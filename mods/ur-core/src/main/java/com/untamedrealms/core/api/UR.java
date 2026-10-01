package com.untamedrealms.core.api;

import com.untamedrealms.core.network.CoreNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Small convenience facade other modules use for player feedback. */
public final class UR {
    private UR() {}

    /** Big centred banner, e.g. "Mining increased to 12". */
    public static void banner(ServerPlayer player, Component title, Component subtitle) {
        CoreNetwork.notify(player, CoreNetwork.STYLE_BANNER, title, subtitle);
    }

    /** Smaller line shown beneath the banner area, e.g. "Objective complete". */
    public static void subtle(ServerPlayer player, Component text) {
        CoreNetwork.notify(player, CoreNetwork.STYLE_SUBTLE, text, Component.empty());
    }

    /** Red warning text, e.g. "You need Mining 30 to mine this". Rate-limited client side. */
    public static void warn(ServerPlayer player, Component text) {
        CoreNetwork.notify(player, CoreNetwork.STYLE_WARNING, text, Component.empty());
    }
}
