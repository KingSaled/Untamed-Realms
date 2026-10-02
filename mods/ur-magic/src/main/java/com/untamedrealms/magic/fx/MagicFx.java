package com.untamedrealms.magic.fx;

import com.untamedrealms.magic.UntamedMagic;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/** Spell particles (one per element) and synthesised spell sounds (tools/artforge/assets/sounds.py). */
public final class MagicFx {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, UntamedMagic.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, UntamedMagic.MODID);

    public static final String[] PARTICLE_NAMES = {"spark", "ember", "frost", "holy", "heal", "shadow", "arcane"};
    public static final Map<String, DeferredHolder<ParticleType<?>, SimpleParticleType>> PARTICLE = new LinkedHashMap<>();

    public static final String[] SOUND_NAMES = {"fire_cast", "fire_impact", "shock_cast", "shock_impact", "frost_cast", "frost_impact",
            "holy_cast", "holy_impact", "heal_cast", "shadow_cast", "arcane_cast", "arcane_impact", "conjure"};
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> SOUND = new LinkedHashMap<>();

    static {
        // "true": shown even with particles set to Minimal / far away, like vanilla's spell particles
        for (String name : PARTICLE_NAMES) PARTICLE.put(name, PARTICLES.register(name, () -> new SimpleParticleType(true) {}));
        for (String name : SOUND_NAMES) SOUND.put(name, SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(UntamedMagic.id(name))));
    }

    private MagicFx() {}

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
    }

    /** The particle for a spell element. */
    public static SimpleParticleType particle(String element) {
        String name = switch (element) {
            case "fire" -> "ember";
            case "frost" -> "frost";
            case "shock" -> "spark";
            case "holy" -> "holy";
            case "heal" -> "heal";
            case "shadow" -> "shadow";
            default -> "arcane";
        };
        return PARTICLE.get(name).get();
    }

    public static SoundEvent castSound(String element, String kind) {
        if ("summon".equals(kind)) return SOUND.get("conjure").get();
        return SOUND.get(switch (element) {
            case "fire" -> "fire_cast";
            case "frost" -> "frost_cast";
            case "shock" -> "shock_cast";
            case "holy" -> "holy_cast";
            case "heal" -> "heal_cast";
            case "shadow" -> "shadow_cast";
            default -> "arcane_cast";
        }).get();
    }

    public static SoundEvent impactSound(String element) {
        return SOUND.get(switch (element) {
            case "fire" -> "fire_impact";
            case "frost" -> "frost_impact";
            case "shock" -> "shock_impact";
            case "holy", "heal" -> "holy_impact";
            default -> "arcane_impact";
        }).get();
    }
}
