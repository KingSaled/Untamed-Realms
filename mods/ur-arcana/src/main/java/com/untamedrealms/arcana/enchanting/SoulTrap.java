package com.untamedrealms.arcana.enchanting;

import com.untamedrealms.arcana.UntamedArcana;
import com.untamedrealms.arcana.item.SoulGemItem;
import com.untamedrealms.core.api.UR;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * A creature that dies while Soul Trapped puts its soul into the smallest empty soul gem big enough
 * to hold it, carried by its killer (or the nearest player within 24 blocks).
 */
@EventBusSubscriber(modid = UntamedArcana.MODID)
public final class SoulTrap {
    private SoulTrap() {}

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player || !(victim.level() instanceof ServerLevel level) || !victim.hasEffect(UntamedArcana.SOUL_TRAPPED)) return;
        ServerPlayer player = event.getSource().getEntity() instanceof ServerPlayer killer ? killer : null;
        if (player == null && level.getNearestPlayer(victim, 24) instanceof ServerPlayer near) player = near;
        if (player == null) return;
        int soul = SoulGemItem.soulOf(victim.getMaxHealth());
        if (capture(player, soul)) {
            level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
            level.playSound(null, victim.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2f, 0.8f);
            UR.subtle(player, Component.translatable("message.urarcana.soul_captured", Component.translatable("soul.urarcana." + SoulGemItem.SIZES[soul])));
        }
    }

    /** Fills the smallest empty gem that can hold {@code soul}. Returns whether one was filled. */
    public static boolean capture(Player player, int soul) {
        ItemStack best = ItemStack.EMPTY;
        int bestCapacity = Integer.MAX_VALUE;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof SoulGemItem gem && SoulGemItem.soul(stack) == 0 && gem.capacity >= soul && gem.capacity < bestCapacity) {
                best = stack;
                bestCapacity = gem.capacity;
            }
        }
        if (best.isEmpty()) return false;
        best.set(UntamedArcana.SOUL.get(), soul);
        return true;
    }
}
