package com.untamedrealms.quests.engine;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Quest lifecycle events on the game bus, for other modules (NPC reactions, achievements...). */
public abstract class QuestEvent extends PlayerEvent {
    private final ResourceLocation quest;

    protected QuestEvent(ServerPlayer player, ResourceLocation quest) {
        super(player);
        this.quest = quest;
    }

    public ResourceLocation getQuest() { return quest; }

    public static class Started extends QuestEvent {
        public Started(ServerPlayer player, ResourceLocation quest) { super(player, quest); }
    }

    public static class StageCompleted extends QuestEvent {
        private final int stage;
        public StageCompleted(ServerPlayer player, ResourceLocation quest, int stage) { super(player, quest); this.stage = stage; }
        public int getStage() { return stage; }
    }

    public static class Completed extends QuestEvent {
        public Completed(ServerPlayer player, ResourceLocation quest) { super(player, quest); }
    }
}
