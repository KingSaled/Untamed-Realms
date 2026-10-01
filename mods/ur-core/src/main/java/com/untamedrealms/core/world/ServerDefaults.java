package com.untamedrealms.core.world;

import com.untamedrealms.core.UntamedCore;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** World rules the pack relies on: quests replace advancements, so they are not announced in chat. */
@EventBusSubscriber(modid = UntamedCore.MODID)
public final class ServerDefaults {
    private ServerDefaults() {}

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        event.getServer().getGameRules().getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, event.getServer());
    }
}
