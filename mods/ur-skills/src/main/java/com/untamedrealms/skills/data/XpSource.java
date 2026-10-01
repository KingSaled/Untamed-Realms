package com.untamedrealms.skills.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.untamedrealms.skills.api.Skill;
import net.minecraft.util.StringRepresentable;

import java.util.List;

/**
 * Data-driven XP table, from {@code data/<ns>/urskills/xp_sources/*.json}.
 * <pre>{@code
 * { "skill": "mining", "trigger": "break_block",
 *   "entries": [ { "match": "#minecraft:iron_ores", "xp": 15 }, { "match": "#minecraft:base_stone_overworld", "xp": 1 } ] }
 * }</pre>
 * For each action the most specific matching entry of each table is used.
 */
public record XpSource(Skill skill, Trigger trigger, List<Entry> entries) {
    public static final Codec<XpSource> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Skill.CODEC.fieldOf("skill").forGetter(XpSource::skill),
            Trigger.CODEC.fieldOf("trigger").forGetter(XpSource::trigger),
            Entry.CODEC.listOf().fieldOf("entries").forGetter(XpSource::entries)
    ).apply(inst, XpSource::new));

    public record Entry(Matcher match, float xp, boolean matureOnly) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Matcher.CODEC.fieldOf("match").forGetter(Entry::match),
                Codec.FLOAT.fieldOf("xp").forGetter(Entry::xp),
                Codec.BOOL.optionalFieldOf("mature_only", false).forGetter(Entry::matureOnly)
        ).apply(inst, Entry::new));
    }

    public enum Trigger implements StringRepresentable {
        /** A block broken by the player. Matches blocks. */
        BREAK_BLOCK,
        /** An item caught while fishing. Matches items. */
        FISH,
        /** An item taken from a furnace/smoker/blast furnace. Matches items. */
        SMELT,
        /** An item crafted at a crafting grid. Matches items. */
        CRAFT,
        /** A potion taken from a brewing stand. Matches items. */
        BREW,
        /** An item eaten / drunk. Matches items. */
        CONSUME;

        public static final Codec<Trigger> CODEC = StringRepresentable.fromEnum(Trigger::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
