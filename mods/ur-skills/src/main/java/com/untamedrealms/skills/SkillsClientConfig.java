package com.untamedrealms.skills;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SkillsClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_XP_DROPS;
    public static final ModConfigSpec.BooleanValue SHOW_REQUIREMENT_TOOLTIPS;

    static {
        BUILDER.push("hud");
        SHOW_XP_DROPS = BUILDER.comment("RuneScape-style floating XP drops on the right of the screen.").define("showXpDrops", true);
        SHOW_REQUIREMENT_TOOLTIPS = BUILDER.define("showRequirementTooltips", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private SkillsClientConfig() {}
}
