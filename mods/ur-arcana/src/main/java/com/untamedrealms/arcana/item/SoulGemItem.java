package com.untamedrealms.arcana.item;

import com.untamedrealms.arcana.UntamedArcana;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A soul gem: holds one soul up to its size (Petty 1 .. Grand 5). Filled by killing a creature while
 * carrying it; spent (emptied) at the arcane enchanter.
 */
public class SoulGemItem extends Item {
    public static final String[] SIZES = {"", "petty", "lesser", "common", "greater", "grand"};

    public final int capacity;

    public SoulGemItem(int capacity, Properties properties) {
        super(properties);
        this.capacity = capacity;
    }

    public static int soul(ItemStack stack) {
        return stack.getOrDefault(UntamedArcana.SOUL.get(), 0);
    }

    /** Soul size of a creature from its max health. */
    public static int soulOf(float maxHealth) {
        return maxHealth <= 10 ? 1 : maxHealth <= 20 ? 2 : maxHealth <= 40 ? 3 : maxHealth <= 100 ? 4 : 5;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return soul(stack) > 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int soul = soul(stack);
        tooltip.add(soul == 0 ? Component.translatable("tooltip.urarcana.soul_empty").withStyle(ChatFormatting.GRAY)
                : Component.translatable("tooltip.urarcana.soul_filled", Component.translatable("soul.urarcana." + SIZES[soul]))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.urarcana.soul_capacity", Component.translatable("soul.urarcana." + SIZES[capacity]))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
