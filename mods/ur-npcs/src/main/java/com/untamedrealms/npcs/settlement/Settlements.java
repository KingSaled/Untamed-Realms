package com.untamedrealms.npcs.settlement;

import com.untamedrealms.npcs.UntamedNpcs;
import com.untamedrealms.npcs.data.NpcDef;
import com.untamedrealms.npcs.data.NpcsData;
import com.untamedrealms.npcs.data.SettlementDef;
import com.untamedrealms.npcs.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Lazily brings settlements to life: the first time a player stands inside a matching structure
 * (e.g. any village, Towns & Towers town...), its townsfolk and a notice board are spawned.
 */
@EventBusSubscriber(modid = UntamedNpcs.MODID)
public final class Settlements {
    private Settlements() {}

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 100 != 0 || player.isSpectator()) return;
        ServerLevel level = player.serverLevel();
        for (SettlementDef def : NpcsData.SETTLEMENTS.values()) {
            StructureStart start = find(level, player.blockPosition(), def.structures());
            if (start == null || !start.isValid()) continue;
            long key = start.getChunkPos().toLong();
            SettlementData data = level.getDataStorage().computeIfAbsent(SettlementData.factory(), SettlementData.NAME);
            if (data.isPopulated(key)) return;
            data.markPopulated(key);
            populate(level, start.getBoundingBox(), def, level.getRandom());
            return;
        }
    }

    private static StructureStart find(ServerLevel level, BlockPos pos, String structures) {
        for (String alt : structures.split("\\|")) {
            alt = alt.trim();
            boolean tag = alt.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? alt.substring(1) : alt);
            if (id == null) continue;
            StructureStart start;
            if (tag) {
                start = level.structureManager().getStructureWithPieceAt(pos, TagKey.create(Registries.STRUCTURE, id));
            } else {
                Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
                if (structure == null) continue;
                start = level.structureManager().getStructureWithPieceAt(pos, structure);
            }
            if (start.isValid()) return start;
        }
        return null;
    }

    public static void populate(ServerLevel level, BoundingBox box, SettlementDef def, RandomSource random) {
        BlockPos center = box.getCenter();
        List<ResourceLocation> roster = new ArrayList<>(def.always());
        List<ResourceLocation> pool = new ArrayList<>(def.random());
        for (int i = 0; i < def.randomCount() && !pool.isEmpty(); i++) {
            roster.add(pool.remove(random.nextInt(pool.size())));
        }
        int spawned = 0;
        for (ResourceLocation npcId : roster) {
            NpcDef npcDef = NpcsData.NPCS.getOrNull(npcId);
            if (npcDef == null) continue;
            BlockPos pos = findSpot(level, center, def.radius(), random);
            if (pos == null) continue;
            NpcEntity npc = UntamedNpcs.NPC.get().create(level);
            if (npc == null) continue;
            npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360f, 0f);
            npc.setup(npcId, npcDef);
            npc.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
            level.addFreshEntity(npc);
            spawned++;
        }
        if (def.noticeBoard()) {
            BlockPos pos = findSpot(level, center, 6, random);
            if (pos != null) {
                BlockState board = com.untamedrealms.quests.UntamedQuests.NOTICE_BOARD.get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
                level.setBlock(pos, board, 3);
            }
        }
        UntamedNpcs.LOGGER.info("Populated settlement at {} with {} NPCs", center, spawned);
    }

    /** A standable surface spot near {@code center}: solid below, two air blocks, not water. */
    private static BlockPos findSpot(ServerLevel level, BlockPos center, int radius, RandomSource random) {
        for (int attempt = 0; attempt < 24; attempt++) {
            int x = center.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = center.getZ() + random.nextInt(radius * 2 + 1) - radius;
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            BlockState below = level.getBlockState(pos.below());
            if (below.isFaceSturdy(level, pos.below(), Direction.UP) && level.getBlockState(pos).isAir()
                    && level.getBlockState(pos.above()).isAir() && !below.is(Blocks.WATER)) {
                return pos;
            }
        }
        return null;
    }
}
