package com.untamedrealms.arcana.effect;

import com.untamedrealms.core.vitals.VitalsApi;
import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Instant Restore / Ravage Magicka or Stamina: 30 points per level, added or taken away. */
public class VitalEffect extends InstantenousMobEffect {
    private final boolean magicka;
    private final float sign;

    public VitalEffect(boolean magicka, boolean restore, int color) {
        super(restore ? MobEffectCategory.BENEFICIAL : MobEffectCategory.HARMFUL, color);
        this.magicka = magicka;
        this.sign = restore ? 1f : -1f;
    }

    @Override
    public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity indirect, LivingEntity target, int amplifier, double health) {
        if (!(target instanceof Player player)) return;
        float amount = sign * (float) (30 * (amplifier + 1) * health);
        if (magicka) VitalsApi.restoreMagicka(player, amount);
        else VitalsApi.restoreStamina(player, amount);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        applyInstantenousEffect(null, null, entity, amplifier, 1.0);
        return true;
    }
}
