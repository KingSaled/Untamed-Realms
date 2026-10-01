package com.untamedrealms.quests;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class QuestsClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_COMPASS;

    static {
        BUILDER.push("hud");
        SHOW_COMPASS = BUILDER.comment("Show the compass with quest markers at the top of the screen.")
                .define("showCompass", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private QuestsClientConfig() {}
}
