package com.untamedrealms.core.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * A generic "the player did X" signal on the game bus. Modules that cannot depend on each other use it
 * to cooperate: e.g. ur-magic posts {@code ("cast", urmagic:firebolt)} and ur-quests turns it into
 * progress for "cast" objectives.
 */
public class ProgressEvent extends PlayerEvent {
    private final String type;
    private final ResourceLocation id;
    private final int amount;

    public ProgressEvent(ServerPlayer player, String type, ResourceLocation id, int amount) {
        super(player);
        this.type = type;
        this.id = id;
        this.amount = amount;
    }

    public String getType() { return type; }
    public ResourceLocation getId() { return id; }
    public int getAmount() { return amount; }
}
