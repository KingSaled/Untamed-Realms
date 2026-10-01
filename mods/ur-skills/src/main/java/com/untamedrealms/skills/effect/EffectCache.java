package com.untamedrealms.skills.effect;

import com.untamedrealms.skills.api.Skill;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;

/** Aggregated effects for one player. Rebuilt whenever perks / class / providers change. */
public final class EffectCache {
    private final Object2FloatOpenHashMap<String> totals = new Object2FloatOpenHashMap<>();

    /** Serialises totals for the client mirror ({@link ClientEffectStore}). */
    public net.minecraft.nbt.CompoundTag toTag() {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        totals.object2FloatEntrySet().forEach(e -> tag.putFloat(e.getKey(), e.getFloatValue()));
        return tag;
    }

    void add(SkillEffect effect) {
        totals.addTo(effect.type() + "|" + effect.key(), effect.value());
    }

    /** Raw total for exactly this type and key. */
    public float raw(String type, String key) {
        return totals.getFloat(type + "|" + key);
    }

    /** Total for a type and key including global ("") effects. */
    public float get(String type, String key) {
        float value = raw(type, key);
        if (!key.isEmpty()) value += raw(type, "");
        return value;
    }

    /** Total for a type and skill, including the skill's category and global effects. */
    public float get(String type, Skill skill) {
        return raw(type, skill.id()) + raw(type, "category:" + skill.category().id()) + raw(type, "");
    }
}
