package com.untamedrealms.testhub;

import com.untamedrealms.quests.engine.QuestApi;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Registered on the game bus only in test world mode. */
public final class TestHubEvents {
    private static boolean checked;

    private TestHubEvents() {}

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        checked = false;
        MinecraftServer server = event.getServer();
        GameRules rules = server.getGameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        rules.getRule(GameRules.RULE_KEEPINVENTORY).set(true, server);
        rules.getRule(GameRules.RULE_MOBGRIEFING).set(false, server);
        rules.getRule(GameRules.RULE_DOINSOMNIA).set(false, server);
        rules.getRule(GameRules.RULE_SPAWN_RADIUS).set(0, server);
        ServerLevel overworld = server.overworld();
        overworld.setDayTime(6000);
        overworld.setWeatherParameters(1_000_000, 0, false, false);
    }

    /** Builds the hub on the first tick, after every mod's ServerStarted work (e.g. the starting village spawn). */
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (checked) return;
        checked = true;
        ServerLevel overworld = event.getServer().overworld();
        HubData data = HubData.get(overworld);
        if (data.center == null) HubBuilder.buildAndRemember(overworld);
    }

    /** Runs before the starting-village check so that test characters stay at the hub. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        QuestApi.log(player).flags().add("starting_village");
        MinecraftServer server = player.server;
        if (!server.getPlayerList().isOp(player.getGameProfile())) server.getPlayerList().op(player.getGameProfile());
        // the quest log's flags survive death, unlike entity data
        if (QuestApi.log(player).flags().add("urtesthub_book")) player.getInventory().add(ControlBook.create(server));
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        TestCommands.register(event.getDispatcher());
    }
}
