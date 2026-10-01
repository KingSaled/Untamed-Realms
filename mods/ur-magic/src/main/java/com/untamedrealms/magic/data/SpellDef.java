package com.untamedrealms.magic.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.untamedrealms.skills.api.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * A spell, from {@code data/<ns>/urmagic/spells/<id>.json}. {@code kind} selects the behaviour
 * implemented in code (projectile, cone, lightning, self, area, summon, bound_weapon); the tuning
 * fields (damage, radius, element...) sit in the same JSON object. See docs/CONTENT.md.
 */
public record SpellDef(Component name, Component description, Skill school, int level, float cost, int cooldown,
                       String kind, Tuning tuning, float xp) {
    public static final Codec<SpellDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(SpellDef::name),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(SpellDef::description),
            Skill.CODEC.fieldOf("school").forGetter(SpellDef::school),
            Codec.intRange(1, 99).optionalFieldOf("level", 1).forGetter(SpellDef::level),
            Codec.FLOAT.fieldOf("cost").forGetter(SpellDef::cost),
            Codec.INT.optionalFieldOf("cooldown", 10).forGetter(SpellDef::cooldown),
            Codec.STRING.fieldOf("kind").forGetter(SpellDef::kind),
            Tuning.MAP_CODEC.forGetter(SpellDef::tuning),
            Codec.FLOAT.optionalFieldOf("xp", 8f).forGetter(SpellDef::xp)
    ).apply(inst, SpellDef::new));

    public float damage() { return tuning.damage(); }
    public float heal() { return tuning.heal(); }
    public float radius() { return tuning.radius(); }
    public float range() { return tuning.range(); }
    public float speed() { return tuning.speed(); }
    public int duration() { return tuning.duration(); }
    public String element() { return tuning.element(); }
    public String area() { return tuning.area(); }
    public List<EffectSpec> effects() { return tuning.effects(); }
    public Optional<ResourceLocation> entity() { return tuning.entity(); }

    /** Kind-specific numbers, flattened into the spell's JSON object. */
    public record Tuning(float damage, float heal, float radius, float range, float speed, int duration,
                         String element, String area, List<EffectSpec> effects, Optional<ResourceLocation> entity) {
        public static final MapCodec<Tuning> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.FLOAT.optionalFieldOf("damage", 0f).forGetter(Tuning::damage),
                Codec.FLOAT.optionalFieldOf("heal", 0f).forGetter(Tuning::heal),
                Codec.FLOAT.optionalFieldOf("radius", 6f).forGetter(Tuning::radius),
                Codec.FLOAT.optionalFieldOf("range", 24f).forGetter(Tuning::range),
                Codec.FLOAT.optionalFieldOf("speed", 1.6f).forGetter(Tuning::speed),
                Codec.INT.optionalFieldOf("duration", 200).forGetter(Tuning::duration),
                Codec.STRING.optionalFieldOf("element", "arcane").forGetter(Tuning::element),
                Codec.STRING.optionalFieldOf("area", "").forGetter(Tuning::area),
                EffectSpec.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Tuning::effects),
                ResourceLocation.CODEC.optionalFieldOf("entity").forGetter(Tuning::entity)
        ).apply(inst, Tuning::new));
    }

    /** A mob effect applied by the spell (to the caster for self spells, to targets otherwise). */
    public record EffectSpec(ResourceLocation effect, int duration, int amplifier) {
        public static final Codec<EffectSpec> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ResourceLocation.CODEC.fieldOf("effect").forGetter(EffectSpec::effect),
                Codec.INT.optionalFieldOf("duration", 200).forGetter(EffectSpec::duration),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(EffectSpec::amplifier)
        ).apply(inst, EffectSpec::new));
    }
}
