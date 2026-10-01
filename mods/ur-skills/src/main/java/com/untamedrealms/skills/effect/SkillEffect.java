package com.untamedrealms.skills.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Optional;

/**
 * One data-driven gameplay effect. Perks, classes, birthsigns, gear sets and quest rewards all grant
 * effects in this same shape, and every module reads the aggregate through
 * {@link com.untamedrealms.skills.api.SkillsApi#effect}.
 * <pre>{@code
 * { "type": "damage_bonus", "key": "one_handed", "value": 0.2 }
 * { "type": "attribute", "attribute": "minecraft:generic.max_health", "operation": "add_value", "value": 4 }
 * }</pre>
 * The meaning of {@code key} depends on the type - usually a skill id, a {@code category:<name>},
 * or a resource such as {@code magicka}. See {@link EffectTypes} for the full list.
 */
public record SkillEffect(String type, String key, Optional<ResourceLocation> attribute,
                          AttributeModifier.Operation operation, float value) {
    public static final Codec<SkillEffect> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("type").forGetter(SkillEffect::type),
            Codec.STRING.optionalFieldOf("key", "").forGetter(SkillEffect::key),
            ResourceLocation.CODEC.optionalFieldOf("attribute").forGetter(SkillEffect::attribute),
            AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE).forGetter(SkillEffect::operation),
            Codec.FLOAT.fieldOf("value").forGetter(SkillEffect::value)
    ).apply(inst, SkillEffect::new));

    public static SkillEffect of(String type, String key, float value) {
        return new SkillEffect(type, key, Optional.empty(), AttributeModifier.Operation.ADD_VALUE, value);
    }
}
