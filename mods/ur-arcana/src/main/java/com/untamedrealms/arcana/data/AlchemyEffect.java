package com.untamedrealms.arcana.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

/**
 * An alchemical effect, from {@code data/<ns>/urarcana/alchemy_effects/<id>.json}: the mob effect a brew
 * with it applies, for how long in ticks ({@code duration} 0 = instant), at which amplifier, and how
 * valuable it is. A brew is named after its most valuable effect; if that one is harmful, the brew is a
 * poison (thrown) rather than a potion.
 */
public record AlchemyEffect(Component name, ResourceLocation mobEffect, int duration, int amplifier, boolean harmful, int value) {
    public static final Codec<AlchemyEffect> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(AlchemyEffect::name),
            ResourceLocation.CODEC.fieldOf("mob_effect").forGetter(AlchemyEffect::mobEffect),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(AlchemyEffect::duration),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(AlchemyEffect::amplifier),
            Codec.BOOL.optionalFieldOf("harmful", false).forGetter(AlchemyEffect::harmful),
            Codec.INT.optionalFieldOf("value", 10).forGetter(AlchemyEffect::value)
    ).apply(inst, AlchemyEffect::new));

    public boolean instant() {
        return duration <= 0;
    }
}
