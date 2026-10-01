package com.untamedrealms.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-player client preferences for the HUD. */
public final class CoreClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_VITALS;
    public static final ModConfigSpec.BooleanValue HIDE_FULL_VITALS;
    public static final ModConfigSpec.BooleanValue SHOW_WALLET_IN_INVENTORY;
    public static final ModConfigSpec.BooleanValue SHOW_NOTIFICATIONS;

    static {
        BUILDER.push("hud");
        SHOW_VITALS = BUILDER.comment("Show the Magicka and Stamina bars.").define("showVitals", true);
        HIDE_FULL_VITALS = BUILDER.comment("Fade the bars out when they are full (Skyrim style).").define("hideFullVitals", true);
        SHOW_WALLET_IN_INVENTORY = BUILDER.define("showWalletInInventory", true);
        SHOW_NOTIFICATIONS = BUILDER.comment("Show level-up / quest banners.").define("showNotifications", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private CoreClientConfig() {}
}
