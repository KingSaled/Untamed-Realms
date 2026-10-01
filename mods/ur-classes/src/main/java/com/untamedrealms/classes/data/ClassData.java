package com.untamedrealms.classes.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/** A player's chosen class and birthsign. Copied on death. */
public final class ClassData implements INBTSerializable<CompoundTag> {
    private @Nullable ResourceLocation classId;
    private @Nullable ResourceLocation birthsign;

    public @Nullable ResourceLocation classId() { return classId; }
    public @Nullable ResourceLocation birthsign() { return birthsign; }
    public boolean chosen() { return classId != null; }

    public void set(@Nullable ResourceLocation classId, @Nullable ResourceLocation birthsign) {
        this.classId = classId;
        this.birthsign = birthsign;
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        if (classId != null) tag.putString("class", classId.toString());
        if (birthsign != null) tag.putString("birthsign", birthsign.toString());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        classId = tag.contains("class") ? ResourceLocation.tryParse(tag.getString("class")) : null;
        birthsign = tag.contains("birthsign") ? ResourceLocation.tryParse(tag.getString("birthsign")) : null;
    }
}
