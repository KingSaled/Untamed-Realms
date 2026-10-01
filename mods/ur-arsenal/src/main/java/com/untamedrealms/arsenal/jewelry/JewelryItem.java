package com.untamedrealms.arsenal.jewelry;

import com.untamedrealms.arsenal.ArsenalComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A ring or amulet worn in a Curios slot. Its bonuses are {@link com.untamedrealms.skills.effect.SkillEffect}s
 * in the {@code urarsenal:jewel_effects} component (datapacks can change them per item).
 */
public class JewelryItem extends Item {
    public JewelryItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.urarsenal.wear_" + (stack.is(net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("curios", "ring")))
                ? "ring" : "amulet")).withStyle(ChatFormatting.DARK_GRAY));
        if (!stack.has(ArsenalComponents.JEWEL_EFFECTS.get())) tooltip.add(Component.literal("?").withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
