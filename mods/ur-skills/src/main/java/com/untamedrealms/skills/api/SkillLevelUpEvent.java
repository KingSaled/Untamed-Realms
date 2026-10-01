package com.untamedrealms.skills.api;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Fired on the game bus after a skill gains one or more levels. */
public class SkillLevelUpEvent extends PlayerEvent {
    private final Skill skill;
    private final int oldLevel;
    private final int newLevel;

    public SkillLevelUpEvent(ServerPlayer player, Skill skill, int oldLevel, int newLevel) {
        super(player);
        this.skill = skill;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public Skill getSkill() { return skill; }
    public int getOldLevel() { return oldLevel; }
    public int getNewLevel() { return newLevel; }
}
