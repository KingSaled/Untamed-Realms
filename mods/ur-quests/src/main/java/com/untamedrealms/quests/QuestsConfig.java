package com.untamedrealms.quests;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class QuestsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> STARTING_QUEST;
    public static final ModConfigSpec.IntValue BOUNTIES_PER_BOARD;
    public static final ModConfigSpec.BooleanValue START_IN_VILLAGE;
    public static final ModConfigSpec.ConfigValue<String> STARTING_STRUCTURES;

    static {
        BUILDER.push("quests");
        STARTING_QUEST = BUILDER.comment("Quest automatically given to every new player (empty to disable).")
                .define("startingQuest", "urquests:main/a_new_beginning");
        BOUNTIES_PER_BOARD = BUILDER.comment("How many bounties a notice board offers each day.")
                .defineInRange("bountiesPerBoard", 3, 1, 9);
        START_IN_VILLAGE = BUILDER.comment("Move the world spawn to the settlement nearest the original spawn (decided once per world).")
                .define("startInVillage", true);
        STARTING_STRUCTURES = BUILDER.comment("Structures that count as a starting settlement (ids or #tags joined with |).")
                .define("startingStructures", "#minecraft:village");
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private QuestsConfig() {}
}
