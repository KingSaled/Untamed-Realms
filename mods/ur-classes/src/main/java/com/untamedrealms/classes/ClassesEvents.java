package com.untamedrealms.classes;

import com.untamedrealms.classes.network.ClassesNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = UntamedClasses.MODID)
public final class ClassesEvents {
    /** Players who still need to pick a class, with ticks until we prompt them. */
    private static final Map<UUID, Integer> PROMPT = new ConcurrentHashMap<>();

    private ClassesEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ClassesNetwork.sync(player);
        if (!ClassesApi.hasChosen(player)) PROMPT.put(player.getUUID(), 40);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ClassesNetwork.sync(player);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Integer ticks = PROMPT.get(player.getUUID());
        if (ticks == null) return;
        if (ticks > 0) {
            PROMPT.put(player.getUUID(), ticks - 1);
            return;
        }
        PROMPT.remove(player.getUUID());
        if (!ClassesApi.hasChosen(player)) {
            ClassesNetwork.openSelection(player);
            player.sendSystemMessage(Component.translatable("message.urclasses.choose_hint").withStyle(ChatFormatting.GOLD));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PROMPT.remove(event.getEntity().getUUID());
    }
}
