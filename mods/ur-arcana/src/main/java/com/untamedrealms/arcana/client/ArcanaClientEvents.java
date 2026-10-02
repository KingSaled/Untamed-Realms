package com.untamedrealms.arcana.client;

import com.untamedrealms.arcana.UntamedArcana;
import com.untamedrealms.arcana.alchemy.Alchemy;
import com.untamedrealms.arcana.data.AlchemyEffect;
import com.untamedrealms.arcana.data.IngredientDef;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = UntamedArcana.MODID, value = Dist.CLIENT)
public final class ArcanaClientEvents {
    private ArcanaClientEvents() {}

    /** Ingredients list the effects you have discovered, Skyrim style: "Alchemy ingredient: Restore Health, ?, ?, ?". */
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        IngredientDef def = Alchemy.ingredient(event.getItemStack());
        if (def == null) return;
        MutableComponent line = Component.translatable("tooltip.urarcana.ingredient").append(": ").withStyle(ChatFormatting.DARK_PURPLE);
        for (int i = 0; i < def.effects().size(); i++) {
            if (i > 0) line.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            AlchemyEffect effect = UntamedArcana.EFFECTS.getOrNull(def.effects().get(i));
            boolean known = ClientArcana.knowledge().knows(def.item(), i);
            line.append(known && effect != null ? effect.name().copy().withStyle(effect.harmful() ? ChatFormatting.RED : ChatFormatting.GREEN)
                    : Component.literal("?").withStyle(ChatFormatting.DARK_GRAY));
        }
        event.getToolTip().add(Math.min(1, event.getToolTip().size()), line);
    }
}
