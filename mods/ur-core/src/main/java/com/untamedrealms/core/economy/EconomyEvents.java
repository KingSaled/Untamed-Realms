package com.untamedrealms.core.economy;

import com.untamedrealms.core.CoreConfig;
import com.untamedrealms.core.UntamedCore;
import com.untamedrealms.core.registry.CoreItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = UntamedCore.MODID)
public final class EconomyEvents {
    private EconomyEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        WalletApi.sync(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        WalletApi.sync(event.getEntity());
    }

    /**
     * On death a share of the wallet spills onto the ground as coins (so grave/corpse mods pick
     * them up like any other drop). High priority so we run before grave mods collect drops.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) return;
        long balance = WalletApi.balance(player);
        if (balance <= 0) return;
        double fraction = CoreConfig.KEEP_WALLET_ON_DEATH.get() ? CoreConfig.DEATH_COIN_LOSS.get() : 1.0;
        long lost = (long) Math.floor(balance * fraction);
        if (lost <= 0) return;
        WalletApi.set(player, balance - lost);

        long purses = lost / 100;
        long crowns = lost % 100;
        while (purses > 0) {
            int n = (int) Math.min(purses, CoreItems.CROWN_PURSE.get().getDefaultMaxStackSize());
            event.getDrops().add(drop(player, new ItemStack(CoreItems.CROWN_PURSE.get(), n)));
            purses -= n;
        }
        if (crowns > 0) {
            event.getDrops().add(drop(player, new ItemStack(CoreItems.CROWN.get(), (int) crowns)));
        }
    }

    private static ItemEntity drop(ServerPlayer player, ItemStack stack) {
        ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), stack);
        entity.setDefaultPickUpDelay();
        return entity;
    }
}
