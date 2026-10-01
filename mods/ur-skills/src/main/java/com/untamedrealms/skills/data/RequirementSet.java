package com.untamedrealms.skills.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.untamedrealms.skills.api.Skill;
import net.minecraft.util.StringRepresentable;

import java.util.List;

/**
 * RuneScape-style skill requirements, from {@code data/<ns>/urskills/requirements/*.json}.
 * <pre>{@code
 * { "requirements": [ { "match": "minecraft:diamond_pickaxe", "kind": "tool", "skill": "mining", "level": 30 } ] }
 * }</pre>
 */
public record RequirementSet(List<Requirement> requirements) {
    public static final Codec<RequirementSet> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Requirement.CODEC.listOf().fieldOf("requirements").forGetter(RequirementSet::requirements)
    ).apply(inst, RequirementSet::new));

    public record Requirement(Matcher match, Kind kind, Skill skill, int level) {
        public static final Codec<Requirement> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Matcher.CODEC.fieldOf("match").forGetter(Requirement::match),
                Kind.CODEC.fieldOf("kind").forGetter(Requirement::kind),
                Skill.CODEC.fieldOf("skill").forGetter(Requirement::skill),
                Codec.intRange(1, 99).fieldOf("level").forGetter(Requirement::level)
        ).apply(inst, Requirement::new));
    }

    public enum Kind implements StringRepresentable {
        /** Item used as a tool to break blocks. */
        TOOL,
        /** Item used as a weapon. */
        WEAPON,
        /** Item worn as armor. */
        ARMOR,
        /** Block that is broken. */
        BLOCK;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
