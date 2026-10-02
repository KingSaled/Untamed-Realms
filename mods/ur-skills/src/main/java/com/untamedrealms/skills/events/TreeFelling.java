package com.untamedrealms.skills.events;

import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.BlockParticleOption;
import com.untamedrealms.skills.SkillsConfig;
import com.untamedrealms.skills.UntamedSkills;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.data.SkillsData;
import com.untamedrealms.skills.data.XpSource;
import com.untamedrealms.skills.registry.SkillsAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tree felling: cutting a natural tree's log with an axe brings the whole tree down. The logs drop
 * at the stump, the leaves fall away a moment later, and Woodcutting XP is awarded per tree rather
 * than per log. Sneak to cut a single log; trees players built from logs are never felled.
 */
@EventBusSubscriber(modid = UntamedSkills.MODID)
public final class TreeFelling {
    private record LeafSweep(ServerLevel level, BoundingBox box, long at) {}

    private static final List<LeafSweep> SWEEPS = new ArrayList<>();

    private TreeFelling() {}

    /** Runs before XpEvents (LOWEST) so the player-placed marker on the stump is still there. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !SkillsConfig.TREE_FELLING.get()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative() || player.isShiftKeyDown()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockState state = event.getState();
        ItemStack tool = player.getMainHandItem();
        if (!state.is(BlockTags.LOGS) || !tool.is(ItemTags.AXES)) return;
        BlockPos origin = event.getPos();
        if (isPlaced(level, origin)) return;

        List<BlockPos> logs = collect(level, origin, state.getBlock(), SkillsConfig.TREE_FELLING_MAX_LOGS.get());
        if (logs.isEmpty() || !hasNaturalLeaves(level, logs, origin)) return;

        // Fell the rest on the next tick, once the stump itself is gone.
        level.getServer().execute(() -> fell(level, player, origin, state, logs));
    }

    /** Connected logs of the same kind above and around {@code origin}, excluding it, highest first. */
    private static List<BlockPos> collect(ServerLevel level, BlockPos origin, Block block, int max) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> found = new ArrayList<>();
        seen.add(origin);
        queue.add(origin);
        while (!queue.isEmpty() && found.size() < max) {
            BlockPos pos = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))) {
                if (next.getY() < origin.getY() || Math.abs(next.getX() - origin.getX()) > 12 || Math.abs(next.getZ() - origin.getZ()) > 12) continue;
                BlockPos immutable = next.immutable();
                if (!seen.add(immutable)) continue;
                if (!level.getBlockState(immutable).is(block) || isPlaced(level, immutable)) continue;
                found.add(immutable);
                queue.add(immutable);
            }
        }
        found.sort(Comparator.comparingInt((BlockPos p) -> p.getY()).reversed());
        return found;
    }

    /** Real trees have leaves that grew there (not placed by players) touching their logs. */
    private static boolean hasNaturalLeaves(ServerLevel level, List<BlockPos> logs, BlockPos origin) {
        int leaves = 0;
        List<BlockPos> all = new ArrayList<>(logs);
        all.add(origin);
        for (BlockPos log : all) {
            for (BlockPos near : BlockPos.betweenClosed(log.offset(-1, -1, -1), log.offset(1, 1, 1))) {
                BlockState s = level.getBlockState(near);
                if (s.is(BlockTags.LEAVES) && s.hasProperty(LeavesBlock.PERSISTENT) && !s.getValue(LeavesBlock.PERSISTENT) && ++leaves >= 3) return true;
            }
        }
        return false;
    }

    private static boolean isPlaced(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        return chunk.hasData(SkillsAttachments.PLACED_BLOCKS.get()) && chunk.getData(SkillsAttachments.PLACED_BLOCKS).contains(pos);
    }

    private static void fell(ServerLevel level, ServerPlayer player, BlockPos origin, BlockState originState, List<BlockPos> logs) {
        ItemStack tool = player.getMainHandItem();
        BoundingBox box = BoundingBox.fromCorners(origin, origin);
        int felled = 0;
        for (BlockPos pos : logs) {
            if (tool.isEmpty() || !tool.is(ItemTags.AXES)) break;
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS)) continue;
            for (ItemStack drop : Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool)) {
                Block.popResource(level, origin, drop);
            }
            // removed quietly (one sound for the whole tree below), with a puff of wood particles
            level.removeBlock(pos, false);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.25, 0.25, 0.25, 0.05);
            // A whole tree costs the axe what three logs would: the stump plus two more.
            if (felled < 2) tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            box.encapsulate(pos);
            felled++;
        }
        if (felled == 0) return;
        level.playSound(null, origin, originState.getSoundType(level, origin, player).getBreakSound(), SoundSource.BLOCKS, 1.0f, 0.8f);

        // Woodcutting XP per tree: the stump already paid one log's worth.
        float perLog = logXp(originState);
        int bonus = Math.round(perLog * (float) Math.sqrt(felled));
        if (bonus > 0) SkillsApi.addXp(player, Skill.WOODCUTTING, bonus);

        // Leaves decay once the game has recalculated their distance to a log; clear them shortly after.
        SWEEPS.add(new LeafSweep(level, box.inflatedBy(5), level.getGameTime() + 12));
    }

    private static float logXp(BlockState state) {
        for (XpSource source : SkillsData.xpSources(XpSource.Trigger.BREAK_BLOCK)) {
            if (source.skill() != Skill.WOODCUTTING) continue;
            XpSource.Entry entry = SkillsData.bestEntry(source, state);
            if (entry != null) return entry.xp();
        }
        return 0f;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (SWEEPS.isEmpty()) return;
        SWEEPS.removeIf(sweep -> {
            if (sweep.level().getGameTime() < sweep.at()) return false;
            BoundingBox b = sweep.box();
            ServerLevel level = sweep.level();
            BlockPos first = null;
            BlockState firstState = null;
            for (BlockPos pos : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
                BlockState s = level.getBlockState(pos);
                if (s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT) && s.getValue(LeavesBlock.DISTANCE) >= LeavesBlock.DECAY_DISTANCE) {
                    // drops (saplings, sticks, apples) without a break sound per leaf
                    Block.dropResources(s, level, pos);
                    level.removeBlock(pos, false);
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.02);
                    if (first == null) { first = pos.immutable(); firstState = s; }
                }
            }
            // one rustle for the whole canopy
            if (first != null) level.playSound(null, first, firstState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0f, 0.9f);
            return true;
        });
    }
}
