package com.untamedrealms.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-player client preferences for the HUD. */
public final class CoreClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_VITALS;
    public static final ModConfigSpec.BooleanValue HIDE_FULL_VITALS;
    public static final ModConfigSpec.BooleanValue SHOW_WALLET_IN_INVENTORY;
    public static final ModConfigSpec.BooleanValue SHOW_NOTIFICATIONS;
    public static final ModConfigSpec.BooleanValue HIDE_VANILLA_POPUPS;
    public static final ModConfigSpec.BooleanValue CUSTOM_TITLE_SCREEN;
    public static final ModConfigSpec.ConfigValue<String> SERVER_ADDRESS;

    static {
        BUILDER.push("hud");
        SHOW_VITALS = BUILDER.comment("Show the Magicka and Stamina bars.").define("showVitals", true);
        HIDE_FULL_VITALS = BUILDER.comment("Fade the bars out when they are full (Skyrim style).").define("hideFullVitals", true);
        SHOW_WALLET_IN_INVENTORY = BUILDER.define("showWalletInInventory", true);
        SHOW_NOTIFICATIONS = BUILDER.comment("Show level-up / quest banners.").define("showNotifications", true);
        HIDE_VANILLA_POPUPS = BUILDER.comment("Hide vanilla tutorial hints, advancement and recipe pop-ups (quests replace them).")
                .define("hideVanillaPopups", true);
        CUSTOM_TITLE_SCREEN = BUILDER.comment("Use the Untamed Realms main menu.").define("customTitleScreen", true);
        SERVER_ADDRESS = BUILDER.comment("Server the main menu's Play button joins.").define("serverAddress", "localhost");
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private CoreClientConfig() {}
}
