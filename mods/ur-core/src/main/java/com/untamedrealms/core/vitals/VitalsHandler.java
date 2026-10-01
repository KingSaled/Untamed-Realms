package com.untamedrealms.core.vitals;

import com.untamedrealms.core.CoreConfig;
import com.untamedrealms.core.UntamedCore;
import com.untamedrealms.core.network.CorePayloads;
import com.untamedrealms.core.registry.CoreAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side simulation of Magicka and Stamina: regeneration, sprint/jump/attack drain and syncing. */
@EventBusSubscriber(modid = UntamedCore.MODID)
public final class VitalsHandler {
    private static final Set<UUID> DIRTY = ConcurrentHashMap.newKeySet();
    private static final int SYNC_INTERVAL = 4;
    /** Exhaustion ends once stamina recovers to this fraction of max. */
    private static final float EXHAUSTION_RECOVERY = 0.25f;

    private VitalsHandler() {}

    static void markDirty(Player player) {
        DIRTY.add(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Vitals v = VitalsApi.get(player);
        float maxMagicka = VitalsApi.maxMagicka(player);
        float maxStamina = VitalsApi.maxStamina(player);

        if (v.magicka() < 0) { v.setMagicka(maxMagicka); markDirty(player); }
        if (v.stamina() < 0) { v.setStamina(maxStamina); markDirty(player); }

        boolean staminaEnabled = CoreConfig.STAMINA_ENABLED.get();
        boolean freeActions = player.isCreative() || player.isSpectator();

        // Sprinting drains stamina; exhausted players cannot sprint.
        if (staminaEnabled && !freeActions && player.isSprinting() && player.getVehicle() == null) {
            VitalsApi.drainStamina(player, CoreConfig.SPRINT_COST_PER_SECOND.get().floatValue() / 20f);
        }
        if (v.exhausted() && player.isSprinting()) {
            player.setSprinting(false);
        }

        // Stamina regeneration (paused while sprinting).
        if (v.staminaRegenCooldown() > 0) {
            v.setStaminaRegenCooldown(v.staminaRegenCooldown() - 1);
        } else if (v.stamina() < maxStamina && !player.isSprinting()) {
            float regen = CoreConfig.STAMINA_REGEN_PER_SECOND.get().floatValue() / 20f
                    * (float) player.getAttributeValue(CoreAttributes.STAMINA_REGEN);
            v.setStamina(Math.min(maxStamina, v.stamina() + regen));
            markDirty(player);
        }
        if (v.exhausted() && (!staminaEnabled || v.stamina() >= maxStamina * EXHAUSTION_RECOVERY)) {
            v.setExhausted(false);
            markDirty(player);
        }

        // Magicka regeneration.
        if (v.magickaRegenCooldown() > 0) {
            v.setMagickaRegenCooldown(v.magickaRegenCooldown() - 1);
        } else if (v.magicka() < maxMagicka) {
            float regen = CoreConfig.MAGICKA_REGEN_PER_SECOND.get().floatValue() / 20f
                    * (float) player.getAttributeValue(CoreAttributes.MAGICKA_REGEN);
            v.setMagicka(Math.min(maxMagicka, v.magicka() + regen));
            markDirty(player);
        }

        // Maximums can shrink (gear removed, perks reset) - keep pools inside them.
        if (v.magicka() > maxMagicka) { v.setMagicka(maxMagicka); markDirty(player); }
        if (v.stamina() > maxStamina) { v.setStamina(maxStamina); markDirty(player); }

        if (player.tickCount % SYNC_INTERVAL == 0 && DIRTY.remove(player.getUUID())) {
            sync(player);
        }
    }

    public static void sync(ServerPlayer player) {
        Vitals v = VitalsApi.get(player);
        PacketDistributor.sendToPlayer(player, new CorePayloads.VitalsSync(
                Math.max(0, v.magicka()), Math.max(0, v.stamina()), v.exhausted()));
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VitalsApi.drainStamina(player, CoreConfig.JUMP_COST.get().floatValue());
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VitalsApi.drainStamina(player, CoreConfig.ATTACK_COST.get().floatValue());
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && event.getSource().getDirectEntity() == attacker
                && VitalsApi.isExhausted(attacker)) {
            event.setAmount(event.getAmount() * CoreConfig.EXHAUSTED_DAMAGE_MULTIPLIER.get().floatValue());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) markDirty(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) markDirty(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DIRTY.remove(event.getEntity().getUUID());
    }
}
