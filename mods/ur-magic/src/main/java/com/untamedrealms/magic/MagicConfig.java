package com.untamedrealms.magic;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MagicConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue COST_REDUCTION_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue POWER_PER_LEVEL;
    public static final ModConfigSpec.BooleanValue REQUIRE_SKILL_TO_LEARN;

    static {
        BUILDER.push("magic");
        COST_REDUCTION_PER_LEVEL = BUILDER.comment("Magicka cost reduction per level of the spell's school (0.003 = 30% at 99).")
                .defineInRange("costReductionPerLevel", 0.003, 0.0, 0.01);
        POWER_PER_LEVEL = BUILDER.comment("Spell magnitude bonus per level of the school.").defineInRange("powerPerLevel", 0.005, 0.0, 0.1);
        REQUIRE_SKILL_TO_LEARN = BUILDER.comment("Tomes can only be read with enough skill in their school.").define("requireSkillToLearn", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MagicConfig() {}
}
