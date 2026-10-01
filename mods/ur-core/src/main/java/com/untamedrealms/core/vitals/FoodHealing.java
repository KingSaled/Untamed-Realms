package com.untamedrealms.core.vitals;

import com.untamedrealms.core.CoreConfig;
import com.untamedrealms.core.UntamedCore;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Food as healing: every food can be eaten when full and heals over a few seconds, more for more
 * filling (cooked, Farmer's Delight) dishes. Potions stay the way to heal fast.
 */
@EventBusSubscriber(modid = UntamedCore.MODID)
public final class FoodHealing {
    /** Health restored per half-second while healing. */
    private static final float RATE = 1f;
    /** Most pending healing a player can bank by eating several things. */
    private static final float MAX_POOL = 40f;
    private static final Map<UUID, Float> POOL = new ConcurrentHashMap<>();

    private FoodHealing() {}

    /** Health (half-hearts) a food restores over time, or 0 for harmful food. */
    public static float healFor(FoodProperties food) {
        for (FoodProperties.PossibleEffect effect : food.effects()) {
            if (effect.effect().getEffect().value().getCategory() == MobEffectCategory.HARMFUL) return 0f;
        }
        double scale = CoreConfig.SPEC.isLoaded() ? CoreConfig.FOOD_HEALING.get() : CoreConfig.FOOD_HEALING.getDefault();
        return (float) ((0.6 * food.nutrition() + 0.4 * food.saturation()) * scale);
    }

    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) return;
        float heal = healFor(food);
        if (heal > 0) POOL.merge(player.getUUID(), heal, (a, b) -> Math.min(MAX_POOL, a + b));
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) return;
        Float pool = POOL.get(player.getUUID());
        if (pool == null) return;
        if (player.isDeadOrDying()) {
            POOL.remove(player.getUUID());
            return;
        }
        float amount = Math.min(RATE, pool);
        if (player.getHealth() < player.getMaxHealth()) player.heal(amount);
        if (pool - amount <= 0.001f) POOL.remove(player.getUUID());
        else POOL.put(player.getUUID(), pool - amount);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        POOL.remove(event.getEntity().getUUID());
    }

    /** Every food (vanilla and every mod's) can be eaten when full. Mod-bus event, routed automatically. */
    @SubscribeEvent
    public static void alwaysEdible(ModifyDefaultComponentsEvent event) {
        event.getAllItems().forEach(item -> {
            FoodProperties food = item.components().get(DataComponents.FOOD);
            if (food != null && !food.canAlwaysEat()) {
                FoodProperties always = new FoodProperties(food.nutrition(), food.saturation(), true, food.eatSeconds(),
                        food.usingConvertsTo(), food.effects());
                event.modify(item, builder -> builder.set(DataComponents.FOOD, always));
            }
        });
    }
}
