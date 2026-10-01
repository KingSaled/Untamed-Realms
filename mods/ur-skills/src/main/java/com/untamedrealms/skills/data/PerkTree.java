package com.untamedrealms.skills.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.effect.SkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.List;

/**
 * A skill's perk tree, loaded from {@code data/<ns>/urskills/perks/<file>.json}.
 * Perk ids are global: {@code <ns>:<perk id>} where ns is the file's namespace.
 */
public record PerkTree(Skill skill, List<Perk> perks) {
    public static final Codec<PerkTree> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Skill.CODEC.fieldOf("skill").forGetter(PerkTree::skill),
            Perk.CODEC.listOf().fieldOf("perks").forGetter(PerkTree::perks)
    ).apply(inst, PerkTree::new));

    public record Perk(String id, Component name, Component description, int level, List<String> requires,
                       int cost, List<SkillEffect> effects) {
        public static final Codec<Perk> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("id").forGetter(Perk::id),
                ComponentSerialization.CODEC.fieldOf("name").forGetter(Perk::name),
                ComponentSerialization.CODEC.fieldOf("description").forGetter(Perk::description),
                Codec.intRange(1, 99).fieldOf("level").forGetter(Perk::level),
                Codec.STRING.listOf().optionalFieldOf("requires", List.of()).forGetter(Perk::requires),
                Codec.intRange(0, 100).optionalFieldOf("cost", 1).forGetter(Perk::cost),
                SkillEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Perk::effects)
        ).apply(inst, Perk::new));
    }
}
