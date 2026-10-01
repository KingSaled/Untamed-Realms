package com.untamedrealms.core.economy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

/** A player's Crown balance. Physical coins can be deposited into it by using them. */
public final class Wallet implements INBTSerializable<CompoundTag> {
    private long crowns;

    public long crowns() {
        return crowns;
    }

    void set(long crowns) {
        this.crowns = Math.max(0, crowns);
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("crowns", crowns);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        crowns = tag.getLong("crowns");
    }
}
