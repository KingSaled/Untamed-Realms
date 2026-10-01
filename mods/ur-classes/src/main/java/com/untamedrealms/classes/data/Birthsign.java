package com.untamedrealms.classes.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.untamedrealms.skills.effect.SkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** The sign you were born under: a permanent blessing. {@code data/<ns>/urclasses/birthsigns/<id>.json}. */
public record Birthsign(Component name, Component description, ResourceLocation icon, int order, List<SkillEffect> effects) {
    public static final Codec<Birthsign> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(Birthsign::name),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(Birthsign::description),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(Birthsign::icon),
            Codec.INT.optionalFieldOf("order", 100).forGetter(Birthsign::order),
            SkillEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Birthsign::effects)
    ).apply(inst, Birthsign::new));
}
