package com.untamedrealms.quests.engine;

import com.mojang.datafixers.util.Pair;
import com.untamedrealms.core.api.UR;
import com.untamedrealms.quests.QuestsConfig;
import com.untamedrealms.quests.UntamedQuests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Players start in a settlement instead of the wilderness: once per world, the village nearest the
 * original spawn becomes the world spawn. Players who joined before that are moved there once.
 */
@EventBusSubscriber(modid = UntamedQuests.MODID)
public final class StartingVillage {
    private static final String NAME = "urquests_starting_village";

    private StartingVillage() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!QuestsConfig.START_IN_VILLAGE.get()) return;
        ServerLevel level = event.getServer().overworld();
        Data data = data(level);
        if (data.decided) return;
        data.decided = true;
        data.setDirty();
        BlockPos from = level.getSharedSpawnPos();
        long t0 = System.currentTimeMillis();
        Pair<BlockPos, Holder<Structure>> found = Locate.structure(level, QuestsConfig.STARTING_STRUCTURES.get(), from, 100);
        if (found == null) {
            UntamedQuests.LOGGER.info("No settlement within 1600 blocks of spawn; keeping the vanilla spawn");
            return;
        }
        BlockPos center = found.getFirst();
        StructureStart start = level.getChunk(center.getX() >> 4, center.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS)
                .getStartForStructure(found.getSecond().value());
        if (start != null && start.isValid()) {
            BoundingBox box = start.getBoundingBox();
            center = new BlockPos(box.getCenter().getX(), 0, box.getCenter().getZ());
        }
        BlockPos spawn = safeSurface(level, center);
        data.spawn = spawn;
        level.setDefaultSpawnPos(spawn, 0f);
        UntamedQuests.LOGGER.info("World spawn moved to the settlement at {} (search took {} ms)", spawn, System.currentTimeMillis() - t0);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !QuestsConfig.START_IN_VILLAGE.get()) return;
        if (!QuestApi.log(player).flags().add("starting_village")) return;
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = data(overworld).spawn;
        // New players already spawn there; only move existing characters who have not done any quest yet.
        if (spawn == null || !QuestApi.log(player).completed().isEmpty()) return;
        if (player.level() == overworld && player.blockPosition().distSqr(spawn) < 64 * 64) return;
        player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot(), player.getXRot());
        UR.subtle(player, Component.translatable("message.urquests.arrived"));
    }

    /** A standable spot near {@code center}: sturdy, dry ground with two air blocks above. */
    private static BlockPos safeSurface(ServerLevel level, BlockPos center) {
        for (int r = 0; r <= 24; r += 2) {
            for (int dx = -r; dx <= r; dx += 2) {
                for (int dz = -r; dz <= r; dz += 2) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int x = center.getX() + dx, z = center.getZ() + dz;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState below = level.getBlockState(pos.below());
                    if (below.isFaceSturdy(level, pos.below(), Direction.UP) && below.getFluidState().isEmpty()
                            && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
                        return pos;
                    }
                }
            }
        }
        return new BlockPos(center.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ()), center.getZ());
    }

    private static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), NAME);
    }

    static final class Data extends SavedData {
        boolean decided;
        @Nullable BlockPos spawn;

        static Data load(CompoundTag tag, HolderLookup.Provider provider) {
            Data d = new Data();
            d.decided = tag.getBoolean("decided");
            d.spawn = NbtUtils.readBlockPos(tag, "spawn").orElse(null);
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            tag.putBoolean("decided", decided);
            if (spawn != null) tag.put("spawn", NbtUtils.writeBlockPos(spawn));
            return tag;
        }
    }
}
