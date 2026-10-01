package com.untamedrealms.arsenal.block;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** The crafting stations. Forge and tanning rack craft from recipes; the workbench tempers gear. */
public enum Station implements StringRepresentable {
    FORGE("forge"), TANNING_RACK("tanning_rack"), WORKBENCH("workbench");

    public static final Codec<Station> CODEC = StringRepresentable.fromEnum(Station::values);

    public final String id;

    Station(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static Station byId(String id) {
        for (Station s : values()) if (s.id.equals(id)) return s;
        return null;
    }
}
