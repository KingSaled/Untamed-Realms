package com.untamedrealms.core.vitals;

import com.untamedrealms.core.CoreConfig;
import com.untamedrealms.core.registry.CoreAttachments;
import com.untamedrealms.core.registry.CoreAttributes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Public API for reading and spending Magicka and Stamina. Server-authoritative. */
public final class VitalsApi {
    private VitalsApi() {}

    public static Vitals get(Player player) {
        return player.getData(CoreAttachments.VITALS);
    }

    public static float maxMagicka(Player player) {
        return (float) player.getAttributeValue(CoreAttributes.MAX_MAGICKA);
    }

    public static float maxStamina(Player player) {
        return (float) player.getAttributeValue(CoreAttributes.MAX_STAMINA);
    }

    public static float magicka(Player player) {
        Vitals v = get(player);
        return v.magicka() < 0 ? maxMagicka(player) : v.magicka();
    }

    public static float stamina(Player player) {
        Vitals v = get(player);
        return v.stamina() < 0 ? maxStamina(player) : v.stamina();
    }

    /** Spends magicka if the player has enough. Creative players always succeed for free. */
    public static boolean tryConsumeMagicka(Player player, float amount) {
        if (player.getAbilities().instabuild) return true;
        float current = magicka(player);
        if (current < amount) return false;
        Vitals v = get(player);
        v.setMagicka(current - amount);
        v.setMagickaRegenCooldown(CoreConfig.MAGICKA_REGEN_DELAY_TICKS.get());
        VitalsHandler.markDirty(player);
        return true;
    }

    /**
     * Drains stamina (scaled by the stamina-cost attribute). Never fails: going below zero simply
     * leaves the player exhausted until they recover.
     */
    public static void drainStamina(Player player, float amount) {
        if (player.getAbilities().instabuild || !CoreConfig.STAMINA_ENABLED.get()) return;
        float scaled = amount * (float) player.getAttributeValue(CoreAttributes.STAMINA_COST);
        if (scaled <= 0) return;
        Vitals v = get(player);
        float next = Math.max(0, stamina(player) - scaled);
        v.setStamina(next);
        v.setStaminaRegenCooldown(CoreConfig.STAMINA_REGEN_DELAY_TICKS.get());
        if (next <= 0) v.setExhausted(true);
        VitalsHandler.markDirty(player);
    }

    public static void restoreMagicka(Player player, float amount) {
        Vitals v = get(player);
        v.setMagicka(Mth.clamp(magicka(player) + amount, 0, maxMagicka(player)));
        VitalsHandler.markDirty(player);
    }

    public static void restoreStamina(Player player, float amount) {
        Vitals v = get(player);
        v.setStamina(Mth.clamp(stamina(player) + amount, 0, maxStamina(player)));
        VitalsHandler.markDirty(player);
    }

    public static boolean isExhausted(Player player) {
        return CoreConfig.STAMINA_ENABLED.get() && get(player).exhausted();
    }
}
