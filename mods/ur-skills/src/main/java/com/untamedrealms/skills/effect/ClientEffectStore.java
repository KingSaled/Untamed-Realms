package com.untamedrealms.skills.effect;

import com.untamedrealms.skills.api.Skill;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.nbt.CompoundTag;

/**
 * The local player's aggregated effect totals, mirrored from the server so client-predicted
 * mechanics (block breaking speed) match the server exactly. Holds no client-only types.
 */
public final class ClientEffectStore {
    private static final Object2FloatOpenHashMap<String> TOTALS = new Object2FloatOpenHashMap<>();

    private ClientEffectStore() {}

    public static synchronized void load(CompoundTag tag) {
        TOTALS.clear();
        for (String key : tag.getAllKeys()) TOTALS.put(key, tag.getFloat(key));
    }

    public static synchronized float raw(String type, String key) {
        return TOTALS.getFloat(type + "|" + key);
    }

    public static float get(String type, Skill skill) {
        return raw(type, skill.id()) + raw(type, "category:" + skill.category().id()) + raw(type, "");
    }

    public static float get(String type, String key) {
        float value = raw(type, key);
        if (!key.isEmpty()) value += raw(type, "");
        return value;
    }
}
