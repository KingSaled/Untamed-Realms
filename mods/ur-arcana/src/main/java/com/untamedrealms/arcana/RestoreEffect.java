package com.untamedrealms.arcana;

import com.untamedrealms.core.vitals.VitalsApi;
import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Instant Restore Magicka / Restore Stamina: 30 points per level. */
public class RestoreEffect extends InstantenousMobEffect {
    private final boolean magicka;

    public RestoreEffect(boolean magicka, int color) {
        super(MobEffectCategory.BENEFICIAL, color);
        this.magicka = magicka;
    }

    @Override
    public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity indirect, LivingEntity target, int amplifier, double health) {
        if (!(target instanceof Player player)) return;
        float amount = (float) (30 * (amplifier + 1) * health);
        if (magicka) VitalsApi.restoreMagicka(player, amount);
        else VitalsApi.restoreStamina(player, amount);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        applyInstantenousEffect(null, null, entity, amplifier, 1.0);
        return true;
    }
}
