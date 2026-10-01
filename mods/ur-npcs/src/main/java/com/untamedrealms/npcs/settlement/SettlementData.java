package com.untamedrealms.npcs.settlement;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-dimension record of which structure starts have already been populated with NPCs. */
public class SettlementData extends SavedData {
    public static final String NAME = "urnpcs_settlements";
    private final LongOpenHashSet populated = new LongOpenHashSet();

    public static Factory<SettlementData> factory() {
        return new Factory<>(SettlementData::new, SettlementData::load, null);
    }

    private static SettlementData load(CompoundTag tag, HolderLookup.Provider registries) {
        SettlementData data = new SettlementData();
        for (long l : tag.getLongArray("populated")) data.populated.add(l);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("populated", populated.toLongArray());
        return tag;
    }

    public boolean isPopulated(long key) {
        return populated.contains(key);
    }

    public void markPopulated(long key) {
        populated.add(key);
        setDirty();
    }
}
