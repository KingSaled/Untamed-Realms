package com.untamedrealms.magic.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.untamedrealms.skills.api.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * A spell, from {@code data/<ns>/urmagic/spells/<id>.json}. {@code kind} selects the behaviour
 * implemented in code (projectile, cone, lightning, self, area, summon, bound_weapon); the remaining
 * fields tune it. See docs/content/spells.md.
 */
public record SpellDef(Component name, Component description, Skill school, int level, float cost, int cooldown,
                       String kind, float damage, float heal, float radius, float range, float speed, int duration,
                       String element, String area, List<EffectSpec> effects, Optional<ResourceLocation> entity,
                       float xp) {
    public static final Codec<SpellDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(SpellDef::name),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(SpellDef::description),
            Skill.CODEC.fieldOf("school").forGetter(SpellDef::school),
            Codec.intRange(1, 99).optionalFieldOf("level", 1).forGetter(SpellDef::level),
            Codec.FLOAT.fieldOf("cost").forGetter(SpellDef::cost),
            Codec.INT.optionalFieldOf("cooldown", 10).forGetter(SpellDef::cooldown),
            Codec.STRING.fieldOf("kind").forGetter(SpellDef::kind),
            Codec.FLOAT.optionalFieldOf("damage", 0f).forGetter(SpellDef::damage),
            Codec.FLOAT.optionalFieldOf("heal", 0f).forGetter(SpellDef::heal),
            Codec.FLOAT.optionalFieldOf("radius", 6f).forGetter(SpellDef::radius),
            Codec.FLOAT.optionalFieldOf("range", 24f).forGetter(SpellDef::range),
            Codec.FLOAT.optionalFieldOf("speed", 1.6f).forGetter(SpellDef::speed),
            Codec.INT.optionalFieldOf("duration", 200).forGetter(SpellDef::duration),
            Codec.STRING.optionalFieldOf("element", "arcane").forGetter(SpellDef::element),
            Codec.STRING.optionalFieldOf("area", "").forGetter(SpellDef::area),
            EffectSpec.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(SpellDef::effects),
            ResourceLocation.CODEC.optionalFieldOf("entity").forGetter(SpellDef::entity),
            Codec.FLOAT.optionalFieldOf("xp", 8f).forGetter(SpellDef::xp)
    ).apply(inst, SpellDef::new));

    /** A mob effect applied by the spell (to the caster for self spells, to targets otherwise). */
    public record EffectSpec(ResourceLocation effect, int duration, int amplifier) {
        public static final Codec<EffectSpec> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ResourceLocation.CODEC.fieldOf("effect").forGetter(EffectSpec::effect),
                Codec.INT.optionalFieldOf("duration", 200).forGetter(EffectSpec::duration),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(EffectSpec::amplifier)
        ).apply(inst, EffectSpec::new));
    }
}
