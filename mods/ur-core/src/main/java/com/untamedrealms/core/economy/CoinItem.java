package com.untamedrealms.core.economy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Physical Crown coins. Using a stack deposits all of it into the wallet. */
public class CoinItem extends Item {
    private final long value;

    public CoinItem(long value, Properties properties) {
        super(properties);
        this.value = value;
    }

    public long value() {
        return value;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            long total = value * stack.getCount();
            WalletApi.deposit(player, total);
            player.displayClientMessage(Component.translatable("message.urcore.deposit", total, WalletApi.balance(player))
                    .withStyle(ChatFormatting.GOLD), true);
            stack.setCount(0);
        }
        level.playSound(player, player.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.6f, 1.6f);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.urcore.coin_value", value).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.urcore.coin_use").withStyle(ChatFormatting.GRAY));
    }
}
