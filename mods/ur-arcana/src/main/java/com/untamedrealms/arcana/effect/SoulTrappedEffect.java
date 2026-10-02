package com.untamedrealms.arcana.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marks a creature whose soul goes into a soul gem if it dies while the effect lasts (see SoulTrap). */
public class SoulTrappedEffect extends MobEffect {
    public SoulTrappedEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A4FD8);
    }
}
