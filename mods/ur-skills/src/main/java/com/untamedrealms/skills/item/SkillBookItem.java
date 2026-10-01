package com.untamedrealms.skills.item;

import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillMath;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.registry.SkillsComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

/** Skyrim skill books: reading one raises its skill by a level. Found in structure chests. */
public class SkillBookItem extends Item {
    public SkillBookItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(Item item, Skill skill) {
        ItemStack stack = new ItemStack(item);
        stack.set(SkillsComponents.SKILL.get(), skill);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        Skill skill = stack.get(SkillsComponents.SKILL.get());
        if (skill == null) return super.getName(stack);
        return Component.translatable("item.urskills.skill_book.named", skill.displayName());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Skill skill = stack.get(SkillsComponents.SKILL.get());
        if (skill == null) return InteractionResultHolder.fail(stack);
        if (player instanceof ServerPlayer serverPlayer) {
            if (SkillsApi.level(serverPlayer, skill) >= SkillMath.MAX_LEVEL) {
                serverPlayer.displayClientMessage(Component.translatable("message.urskills.book_maxed").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            SkillsApi.grantLevels(serverPlayer, skill, 1);
            if (!serverPlayer.isCreative()) stack.shrink(1);
        }
        level.playSound(player, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.urskills.skill_book").withStyle(ChatFormatting.GRAY));
    }
}
