package com.untamedrealms.classes.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.effect.SkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * A playable class, from {@code data/<ns>/urclasses/classes/<id>.json}: starting skill levels,
 * lifelong effects, a starting loadout and purse.
 */
public record ClassDef(Component name, Component tagline, Component description, ResourceLocation icon, int order,
                       Map<Skill, Integer> skills, List<SkillEffect> effects, Loadout loadout, long coins) {
    public static final Codec<ClassDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(ClassDef::name),
            ComponentSerialization.CODEC.optionalFieldOf("tagline", Component.empty()).forGetter(ClassDef::tagline),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(ClassDef::description),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(ClassDef::icon),
            Codec.INT.optionalFieldOf("order", 100).forGetter(ClassDef::order),
            Codec.unboundedMap(Skill.CODEC, Codec.intRange(1, 99)).optionalFieldOf("skills", Map.of()).forGetter(ClassDef::skills),
            SkillEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(ClassDef::effects),
            Loadout.CODEC.optionalFieldOf("loadout", Loadout.EMPTY).forGetter(ClassDef::loadout),
            Codec.LONG.optionalFieldOf("coins", 0L).forGetter(ClassDef::coins)
    ).apply(inst, ClassDef::new));
}
