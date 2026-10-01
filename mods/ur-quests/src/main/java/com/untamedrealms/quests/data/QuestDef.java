package com.untamedrealms.quests.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.untamedrealms.skills.api.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A quest, from {@code data/<ns>/urquests/quests/<path>.json}. Quests are a list of stages; each
 * stage has objectives that must all be completed to advance. See docs/content/quests.md.
 */
public record QuestDef(Component title, Component description, String category, Optional<ResourceLocation> giver,
                       boolean repeatable, int cooldownMinutes, Optional<String> pool, Requirements requirements,
                       List<Stage> stages, Rewards rewards, boolean abandonable) {

    public static final Codec<QuestDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ComponentSerialization.CODEC.fieldOf("title").forGetter(QuestDef::title),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(QuestDef::description),
            Codec.STRING.optionalFieldOf("category", "side").forGetter(QuestDef::category),
            ResourceLocation.CODEC.optionalFieldOf("giver").forGetter(QuestDef::giver),
            Codec.BOOL.optionalFieldOf("repeatable", false).forGetter(QuestDef::repeatable),
            Codec.INT.optionalFieldOf("cooldown_minutes", 0).forGetter(QuestDef::cooldownMinutes),
            Codec.STRING.optionalFieldOf("pool").forGetter(QuestDef::pool),
            Requirements.CODEC.optionalFieldOf("requirements", Requirements.NONE).forGetter(QuestDef::requirements),
            Stage.CODEC.listOf().fieldOf("stages").forGetter(QuestDef::stages),
            Rewards.CODEC.optionalFieldOf("rewards", Rewards.NONE).forGetter(QuestDef::rewards),
            Codec.BOOL.optionalFieldOf("abandonable", true).forGetter(QuestDef::abandonable)
    ).apply(inst, QuestDef::new));

    public boolean isMain() {
        return "main".equals(category);
    }

    public record Stage(Component description, List<Objective> objectives) {
        public static final Codec<Stage> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ComponentSerialization.CODEC.fieldOf("description").forGetter(Stage::description),
                Objective.CODEC.listOf().fieldOf("objectives").forGetter(Stage::objectives)
        ).apply(inst, Stage::new));
    }

    /**
     * One objective. {@code target} depends on the type: an entity / item / block / structure / biome id
     * or {@code #tag} (several may be joined with {@code |}), an NPC id, or a skill id.
     */
    public record Objective(String type, String target, int count, boolean consume, Optional<Component> text) {
        public static final Codec<Objective> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("type").forGetter(Objective::type),
                Codec.STRING.optionalFieldOf("target", "").forGetter(Objective::target),
                Codec.intRange(1, 100000).optionalFieldOf("count", 1).forGetter(Objective::count),
                Codec.BOOL.optionalFieldOf("consume", false).forGetter(Objective::consume),
                ComponentSerialization.CODEC.optionalFieldOf("text").forGetter(Objective::text)
        ).apply(inst, Objective::new));
    }

    public record Requirements(List<ResourceLocation> quests, Map<Skill, Integer> skills, int level) {
        public static final Requirements NONE = new Requirements(List.of(), Map.of(), 0);
        public static final Codec<Requirements> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ResourceLocation.CODEC.listOf().optionalFieldOf("quests", List.of()).forGetter(Requirements::quests),
                Codec.unboundedMap(Skill.CODEC, Codec.intRange(1, 99)).optionalFieldOf("skills", Map.of()).forGetter(Requirements::skills),
                Codec.INT.optionalFieldOf("level", 0).forGetter(Requirements::level)
        ).apply(inst, Requirements::new));
    }

    /** Rewards; {@code items} are decoded lazily so a missing mod only drops that item. */
    public record Rewards(Map<Skill, Integer> xp, long coins, List<Dynamic<?>> items, int perkPoints) {
        public static final Rewards NONE = new Rewards(Map.of(), 0, List.of(), 0);
        public static final Codec<Rewards> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.unboundedMap(Skill.CODEC, Codec.INT).optionalFieldOf("xp", Map.of()).forGetter(Rewards::xp),
                Codec.LONG.optionalFieldOf("coins", 0L).forGetter(Rewards::coins),
                Codec.PASSTHROUGH.listOf().optionalFieldOf("items", List.of()).forGetter(Rewards::items),
                Codec.INT.optionalFieldOf("perk_points", 0).forGetter(Rewards::perkPoints)
        ).apply(inst, Rewards::new));
    }
}
