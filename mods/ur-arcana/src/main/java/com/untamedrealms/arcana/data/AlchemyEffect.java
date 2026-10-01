package com.untamedrealms.arcana.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

/**
 * An alchemical effect, from {@code data/<ns>/urarcana/alchemy_effects/<id>.json}: which mob effect a
 * potion with it applies, for how long ({@code duration} 0 = instant) and whether it is harmful
 * (harmful-only brews become thrown poisons).
 */
public record AlchemyEffect(Component name, ResourceLocation mobEffect, int duration, int amplifier, boolean harmful) {
    public static final Codec<AlchemyEffect> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(AlchemyEffect::name),
            ResourceLocation.CODEC.fieldOf("mob_effect").forGetter(AlchemyEffect::mobEffect),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(AlchemyEffect::duration),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(AlchemyEffect::amplifier),
            Codec.BOOL.optionalFieldOf("harmful", false).forGetter(AlchemyEffect::harmful)
    ).apply(inst, AlchemyEffect::new));
}
