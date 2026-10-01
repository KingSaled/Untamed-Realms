package com.untamedrealms.skills.api;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Fired on the game bus before skill XP is granted. Amount includes multipliers; modifiable and cancellable. */
public class SkillXpEvent extends PlayerEvent implements ICancellableEvent {
    private final Skill skill;
    private double amount;

    public SkillXpEvent(ServerPlayer player, Skill skill, double amount) {
        super(player);
        this.skill = skill;
        this.amount = amount;
    }

    public Skill getSkill() { return skill; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
