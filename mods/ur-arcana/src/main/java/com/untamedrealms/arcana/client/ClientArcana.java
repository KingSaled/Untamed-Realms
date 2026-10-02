package com.untamedrealms.arcana.client;

import com.untamedrealms.arcana.data.ArcanaKnowledge;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/** The local player's alchemy / enchanting knowledge, as last sent by the server. */
public final class ClientArcana {
    private static final ArcanaKnowledge KNOWLEDGE = new ArcanaKnowledge();

    private ClientArcana() {}

    public static ArcanaKnowledge knowledge() {
        return KNOWLEDGE;
    }

    public static void accept(Map<ResourceLocation, Integer> ingredients, List<ResourceLocation> enchantments) {
        KNOWLEDGE.replaceWith(ingredients, enchantments);
    }

    public static void open(BlockPos pos, boolean enchanter) {
        Minecraft.getInstance().setScreen(enchanter ? new EnchanterScreen(pos) : new AlchemyScreen(pos));
    }
}
