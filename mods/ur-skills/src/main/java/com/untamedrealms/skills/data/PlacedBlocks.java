package com.untamedrealms.skills.data;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;

import java.util.Arrays;

/**
 * Positions in a chunk where a player placed an XP-granting block (log, ore...). Breaking such a
 * block grants no gathering XP, which stops place-and-break farming. Stored as a chunk attachment.
 */
public final class PlacedBlocks {
    public static final Codec<PlacedBlocks> CODEC = Codec.LONG_STREAM.xmap(
            stream -> new PlacedBlocks(stream.toArray()),
            placed -> Arrays.stream(placed.positions.toLongArray()));

    private final LongOpenHashSet positions;

    public PlacedBlocks() {
        this.positions = new LongOpenHashSet();
    }

    private PlacedBlocks(long[] values) {
        this.positions = new LongOpenHashSet(values);
    }

    public void add(BlockPos pos) { positions.add(pos.asLong()); }

    /** Removes the position, returning whether it was player-placed. */
    public boolean remove(BlockPos pos) { return positions.remove(pos.asLong()); }

    public boolean contains(BlockPos pos) { return positions.contains(pos.asLong()); }

    public boolean shouldSave() { return !positions.isEmpty(); }
}
