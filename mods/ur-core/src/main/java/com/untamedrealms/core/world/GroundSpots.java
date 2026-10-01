package com.untamedrealms.core.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * Finds places to stand on open ground (streets, grass, sand...) rather than the highest block,
 * which in a town is usually a roof.
 */
public final class GroundSpots {
    private GroundSpots() {}

    /** A random ground spot within {@code radius} of {@code center}, preferring paths; null if none is loaded. */
    public static @Nullable BlockPos random(ServerLevel level, BlockPos center, int radius, RandomSource random) {
        for (int pass = 0; pass < 2; pass++) {
            for (int attempt = 0; attempt < 40; attempt++) {
                int r = Math.max(2, radius * (attempt + 10) / 40);
                int x = center.getX() + random.nextInt(r * 2 + 1) - r;
                int z = center.getZ() + random.nextInt(r * 2 + 1) - r;
                BlockPos pos = check(level, x, z, pass == 0);
                if (pos != null) return pos;
            }
        }
        return null;
    }

    /** The ground spot nearest {@code center} (spiral search), preferring paths; null if none. */
    public static @Nullable BlockPos nearest(ServerLevel level, BlockPos center, int radius) {
        for (int pass = 0; pass < 2; pass++) {
            for (int r = 0; r <= radius; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        BlockPos pos = check(level, center.getX() + dx, center.getZ() + dz, pass == 0);
                        if (pos != null) return pos;
                    }
                }
            }
        }
        return null;
    }

    private static @Nullable BlockPos check(ServerLevel level, int x, int z, boolean pathsOnly) {
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return null;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        BlockState below = level.getBlockState(pos.below());
        if (pathsOnly ? !below.is(Blocks.DIRT_PATH) : !isGround(below)) return null;
        BlockState at = level.getBlockState(pos), above = level.getBlockState(pos.above());
        if (!at.getFluidState().isEmpty() || !(at.isAir() || at.canBeReplaced())) return null;
        if (!(above.isAir() || above.canBeReplaced())) return null;
        return pos;
    }

    /** Natural ground or a street; never planks, stairs, slabs or other building blocks. */
    public static boolean isGround(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.GRAVEL) || state.is(BlockTags.SAND)
                || state.is(Blocks.SNOW_BLOCK) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.TERRACOTTA)
                || state.is(Blocks.MUD) || state.is(Blocks.CLAY);
    }
}
