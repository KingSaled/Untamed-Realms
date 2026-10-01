package com.untamedrealms.magic.item;

import com.untamedrealms.core.vitals.VitalsApi;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** Restore Magicka / Restore Stamina potions. */
public class VitalPotionItem extends Item {
    private final boolean magicka;
    private final float amount;

    public VitalPotionItem(boolean magicka, float amount, Properties properties) {
        super(properties);
        this.magicka = magicka;
        this.amount = amount;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            if (magicka) VitalsApi.restoreMagicka(player, amount);
            else VitalsApi.restoreStamina(player, amount);
        }
        boolean infinite = entity instanceof Player player && player.getAbilities().instabuild;
        if (infinite) return stack;
        stack.shrink(1);
        if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
        if (entity instanceof Player player && !player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
            player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 24;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(magicka ? "tooltip.urmagic.restore_magicka" : "tooltip.urmagic.restore_stamina", Math.round(amount))
                .withStyle(magicka ? ChatFormatting.BLUE : ChatFormatting.GREEN));
    }
}
