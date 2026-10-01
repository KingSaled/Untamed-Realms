package com.untamedrealms.arsenal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Daedra hearts (for Daedric gear) drop from the Nether's fiends when a player kills them. */
@EventBusSubscriber(modid = UntamedArsenal.MODID)
public final class ArsenalDrops {
    private ArsenalDrops() {}

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player)) return;
        EntityType<?> type = event.getEntity().getType();
        float chance = type == EntityType.WITHER_SKELETON ? 0.06f : type == EntityType.BLAZE ? 0.03f
                : type == EntityType.PIGLIN_BRUTE ? 0.08f : 0f;
        if (chance > 0 && event.getEntity().getRandom().nextFloat() < chance) {
            var e = event.getEntity();
            event.getDrops().add(new ItemEntity(e.level(), e.getX(), e.getY(), e.getZ(), new ItemStack(ArsenalItems.DAEDRA_HEART.get())));
        }
    }
}
