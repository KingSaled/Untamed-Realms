package com.untamedrealms.skills.api;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Fired on the game bus after the player's character level increases. */
public class CharacterLevelUpEvent extends PlayerEvent {
    private final int oldLevel;
    private final int newLevel;

    public CharacterLevelUpEvent(ServerPlayer player, int oldLevel, int newLevel) {
        super(player);
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public int getOldLevel() { return oldLevel; }
    public int getNewLevel() { return newLevel; }
}
