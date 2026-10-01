package com.untamedrealms.skills;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server (per-world, synced) balance values for the skill system. */
public final class SkillsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue XP_CURVE_SCALE;
    public static final ModConfigSpec.DoubleValue GLOBAL_XP_MULTIPLIER;
    public static final ModConfigSpec.IntValue CHARACTER_LEVEL_CAP;

    public static final ModConfigSpec.DoubleValue HEALTH_PER_PICK;
    public static final ModConfigSpec.DoubleValue MAGICKA_PER_PICK;
    public static final ModConfigSpec.DoubleValue STAMINA_PER_PICK;
    public static final ModConfigSpec.IntValue MAX_HEALTH_PICKS;

    public static final ModConfigSpec.DoubleValue COMBAT_XP_PER_DAMAGE;
    public static final ModConfigSpec.DoubleValue ARMOR_XP_PER_DAMAGE;
    public static final ModConfigSpec.DoubleValue BLOCK_XP_PER_DAMAGE;

    public static final ModConfigSpec.DoubleValue WEAPON_DAMAGE_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue ARMOR_BONUS_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue GATHER_SPEED_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue EXTRA_DROP_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue BASE_SNEAK_ATTACK_MULTIPLIER;

    public static final ModConfigSpec.EnumValue<RequirementMode> TOOL_REQUIREMENTS;
    public static final ModConfigSpec.EnumValue<RequirementMode> WEAPON_REQUIREMENTS;
    public static final ModConfigSpec.EnumValue<RequirementMode> ARMOR_REQUIREMENTS;
    public static final ModConfigSpec.EnumValue<RequirementMode> BLOCK_REQUIREMENTS;
    public static final ModConfigSpec.BooleanValue ANTI_PLACE_EXPLOIT;
    public static final ModConfigSpec.BooleanValue TREE_FELLING;
    public static final ModConfigSpec.IntValue TREE_FELLING_MAX_LOGS;

    public enum RequirementMode { OFF, SOFT, HARD }

    static {
        BUILDER.comment("Experience & levelling").push("progression");
        XP_CURVE_SCALE = BUILDER
                .comment("Scale on the RuneScape XP curve. 1.0 = authentic (level 99 at 13M xp); 0.25 = level 99 at ~3.3M.")
                .defineInRange("xpCurveScale", 0.25, 0.01, 10.0);
        GLOBAL_XP_MULTIPLIER = BUILDER.comment("Multiplier on all skill XP gained.").defineInRange("globalXpMultiplier", 1.0, 0.0, 100.0);
        CHARACTER_LEVEL_CAP = BUILDER.defineInRange("characterLevelCap", 100, 1, 1000);
        BUILDER.pop();

        BUILDER.comment("What each attribute choice on character level-up grants").push("attributes");
        HEALTH_PER_PICK = BUILDER.comment("Max health per Health pick (2 = one heart).").defineInRange("healthPerPick", 1.0, 0.0, 100.0);
        MAGICKA_PER_PICK = BUILDER.defineInRange("magickaPerPick", 10.0, 0.0, 1000.0);
        STAMINA_PER_PICK = BUILDER.defineInRange("staminaPerPick", 10.0, 0.0, 1000.0);
        MAX_HEALTH_PICKS = BUILDER.comment("Cap on Health picks so health does not run away.").defineInRange("maxHealthPicks", 40, 0, 1000);
        BUILDER.pop();

        BUILDER.comment("XP from combat").push("combat");
        COMBAT_XP_PER_DAMAGE = BUILDER.defineInRange("weaponXpPerDamage", 4.0, 0.0, 1000.0);
        ARMOR_XP_PER_DAMAGE = BUILDER.defineInRange("armorXpPerDamage", 3.0, 0.0, 1000.0);
        BLOCK_XP_PER_DAMAGE = BUILDER.defineInRange("blockXpPerDamage", 4.0, 0.0, 1000.0);
        BUILDER.pop();

        BUILDER.comment("Passive bonuses granted by skill level").push("bonuses");
        WEAPON_DAMAGE_PER_LEVEL = BUILDER.comment("One-Handed / Two-Handed / Archery damage bonus per level.").defineInRange("weaponDamagePerLevel", 0.004, 0.0, 1.0);
        ARMOR_BONUS_PER_LEVEL = BUILDER.comment("Armor rating bonus per Heavy/Light Armor level, at a full set of that type.").defineInRange("armorBonusPerLevel", 0.003, 0.0, 1.0);
        GATHER_SPEED_PER_LEVEL = BUILDER.comment("Mining / Woodcutting speed per level.").defineInRange("gatherSpeedPerLevel", 0.005, 0.0, 1.0);
        EXTRA_DROP_PER_LEVEL = BUILDER.comment("Chance per level of a double drop when gathering.").defineInRange("extraDropPerLevel", 0.002, 0.0, 1.0);
        BASE_SNEAK_ATTACK_MULTIPLIER = BUILDER.comment("Damage multiplier for attacks on mobs that have not noticed you.").defineInRange("baseSneakAttackMultiplier", 2.0, 1.0, 100.0);
        BUILDER.pop();

        BUILDER.comment("Woodcutting").push("woodcutting");
        TREE_FELLING = BUILDER.comment("Chopping a natural tree with an axe fells the whole tree (sneak to cut a single log).",
                        "Woodcutting XP is per tree: a log's XP x (1 + sqrt(logs - 1)), so big trees are worth more but not per log.")
                .define("treeFelling", true);
        TREE_FELLING_MAX_LOGS = BUILDER.defineInRange("maxLogs", 256, 1, 2048);
        BUILDER.pop();

        BUILDER.comment("Skill requirements (RuneScape style). SOFT = penalty, HARD = blocked.").push("requirements");
        TOOL_REQUIREMENTS = BUILDER.comment("Tools without the skill mine at 25% speed (SOFT) or not at all (HARD).").defineEnum("tools", RequirementMode.SOFT);
        WEAPON_REQUIREMENTS = BUILDER.comment("Weapons without the skill deal 50% damage (SOFT) or none (HARD).").defineEnum("weapons", RequirementMode.SOFT);
        ARMOR_REQUIREMENTS = BUILDER.comment("Armor without the skill cannot be worn (HARD) or slows you (SOFT).").defineEnum("armor", RequirementMode.HARD);
        BLOCK_REQUIREMENTS = BUILDER.comment("Blocks (ores) without the skill cannot be mined (HARD) or mine very slowly (SOFT).").defineEnum("blocks", RequirementMode.HARD);
        ANTI_PLACE_EXPLOIT = BUILDER.comment("Blocks placed by players give no gathering XP when broken.").define("antiPlaceExploit", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private SkillsConfig() {}

    /** Config values are only readable once the world's server config is loaded. */
    public static double get(ModConfigSpec.DoubleValue value, double fallback) {
        return SPEC.isLoaded() ? value.get() : fallback;
    }
}
