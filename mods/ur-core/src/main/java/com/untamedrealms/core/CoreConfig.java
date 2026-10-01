package com.untamedrealms.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side (per world, synced to clients) balance values for the core systems. */
public final class CoreConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue STAMINA_ENABLED;
    public static final ModConfigSpec.DoubleValue SPRINT_COST_PER_SECOND;
    public static final ModConfigSpec.DoubleValue JUMP_COST;
    public static final ModConfigSpec.DoubleValue ATTACK_COST;
    public static final ModConfigSpec.DoubleValue STAMINA_REGEN_PER_SECOND;
    public static final ModConfigSpec.IntValue STAMINA_REGEN_DELAY_TICKS;
    public static final ModConfigSpec.DoubleValue MAGICKA_REGEN_PER_SECOND;
    public static final ModConfigSpec.IntValue MAGICKA_REGEN_DELAY_TICKS;
    public static final ModConfigSpec.DoubleValue EXHAUSTED_DAMAGE_MULTIPLIER;

    public static final ModConfigSpec.BooleanValue KEEP_WALLET_ON_DEATH;
    public static final ModConfigSpec.DoubleValue DEATH_COIN_LOSS;

    static {
        BUILDER.comment("Stamina: drained by sprinting, jumping and melee attacks, regenerates after a short pause.")
                .push("stamina");
        STAMINA_ENABLED = BUILDER.define("enabled", true);
        SPRINT_COST_PER_SECOND = BUILDER.defineInRange("sprintCostPerSecond", 4.0, 0.0, 1000.0);
        JUMP_COST = BUILDER.defineInRange("jumpCost", 3.0, 0.0, 1000.0);
        ATTACK_COST = BUILDER.defineInRange("attackCost", 2.5, 0.0, 1000.0);
        STAMINA_REGEN_PER_SECOND = BUILDER.defineInRange("regenPerSecond", 9.0, 0.0, 1000.0);
        STAMINA_REGEN_DELAY_TICKS = BUILDER.defineInRange("regenDelayTicks", 25, 0, 1200);
        EXHAUSTED_DAMAGE_MULTIPLIER = BUILDER
                .comment("Melee damage multiplier applied while the attacker has no stamina left.")
                .defineInRange("exhaustedDamageMultiplier", 0.6, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("magicka");
        MAGICKA_REGEN_PER_SECOND = BUILDER.defineInRange("regenPerSecond", 3.0, 0.0, 1000.0);
        MAGICKA_REGEN_DELAY_TICKS = BUILDER.defineInRange("regenDelayTicks", 40, 0, 1200);
        BUILDER.pop();

        BUILDER.comment("The Crown economy (wallet balance is separate from physical coin items).").push("economy");
        KEEP_WALLET_ON_DEATH = BUILDER.define("keepWalletOnDeath", true);
        DEATH_COIN_LOSS = BUILDER
                .comment("Fraction of the wallet lost on death (0 = none). Only applies when keepWalletOnDeath is true.")
                .defineInRange("deathCoinLoss", 0.1, 0.0, 1.0);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private CoreConfig() {}
}
