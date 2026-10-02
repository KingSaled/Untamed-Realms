package com.untamedrealms.arcana;

import com.untamedrealms.arcana.alchemy.Alchemy;
import com.untamedrealms.arcana.alchemy.AlchemyLab;
import com.untamedrealms.arcana.data.IngredientDef;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = UntamedArcana.MODID)
public final class ArcanaEvents {
    private ArcanaEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ArcanaApi.sync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ArcanaApi.sync(player);
    }

    /** Eating an ingredient that is food (spider eyes, berries...) teaches it like tasting at the lab. */
    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        IngredientDef def = Alchemy.ingredient(event.getItem());
        if (def != null) AlchemyLab.taste(player, def, false);
    }
}
