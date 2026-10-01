package com.untamedrealms.testhub;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/** Where the hub was built (per world, in the overworld's data). */
public final class HubData extends SavedData {
    private static final String NAME = "urtesthub_hub";

    /** The hub's centre on its floor (players stand at y + 1). Null until built. */
    public @Nullable BlockPos center;
    /** The ground spot below (the starting village). */
    public @Nullable BlockPos village;

    public static HubData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(new Factory<>(HubData::new, HubData::load, null), NAME);
    }

    private static HubData load(CompoundTag tag, HolderLookup.Provider registries) {
        HubData data = new HubData();
        data.center = NbtUtils.readBlockPos(tag, "center").orElse(null);
        data.village = NbtUtils.readBlockPos(tag, "village").orElse(null);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (center != null) tag.put("center", NbtUtils.writeBlockPos(center));
        if (village != null) tag.put("village", NbtUtils.writeBlockPos(village));
        return tag;
    }
}
