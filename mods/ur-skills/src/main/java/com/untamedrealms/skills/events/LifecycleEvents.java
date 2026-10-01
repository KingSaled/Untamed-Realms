package com.untamedrealms.skills.events;

import com.untamedrealms.skills.UntamedSkills;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.effect.EffectManager;
import com.untamedrealms.skills.network.SkillsNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Keeps effect caches, attribute modifiers and client copies in step with the player's lifecycle. */
@EventBusSubscriber(modid = UntamedSkills.MODID)
public final class LifecycleEvents {
    private LifecycleEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillsApi.refresh(player);
            SkillsNetwork.syncNow(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillsApi.refresh(player);
            player.setHealth(player.getMaxHealth());
            SkillsNetwork.syncNow(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SkillsApi.refresh(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EffectManager.forget(event.getEntity().getUUID());
    }

    /** Perk data may have changed on /reload: rebuild everyone's effects. */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            event.getPlayerList().getPlayers().forEach(SkillsApi::refresh);
        }
    }
}
