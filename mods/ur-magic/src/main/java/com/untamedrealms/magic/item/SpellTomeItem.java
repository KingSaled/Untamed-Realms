package com.untamedrealms.magic.item;

import com.untamedrealms.core.api.UR;
import com.untamedrealms.magic.MagicConfig;
import com.untamedrealms.magic.UntamedMagic;
import com.untamedrealms.magic.data.SpellDef;
import com.untamedrealms.magic.spell.MagicApi;
import com.untamedrealms.skills.api.SkillsApi;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

/** Reading a tome teaches its spell (consuming the tome), if your school skill is high enough. */
public class SpellTomeItem extends Item {
    public SpellTomeItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(ResourceLocation spell) {
        ItemStack stack = new ItemStack(UntamedMagic.SPELL_TOME.get());
        stack.set(UntamedMagic.SPELL.get(), spell);
        return stack;
    }

    public static SpellDef spell(ItemStack stack) {
        return UntamedMagic.SPELLS.getOrNull(stack.get(UntamedMagic.SPELL.get()));
    }

    @Override
    public Component getName(ItemStack stack) {
        SpellDef def = spell(stack);
        return def == null ? super.getName(stack) : Component.translatable("item.urmagic.spell_tome.named", def.name());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ResourceLocation id = stack.get(UntamedMagic.SPELL.get());
        SpellDef def = spell(stack);
        if (def == null) return InteractionResultHolder.fail(stack);
        if (player instanceof ServerPlayer sp) {
            if (MagicApi.book(sp).knows(id)) {
                UR.warn(sp, Component.translatable("message.urmagic.already_known", def.name()));
                return InteractionResultHolder.fail(stack);
            }
            if (MagicConfig.REQUIRE_SKILL_TO_LEARN.get() && SkillsApi.level(sp, def.school()) < def.level()) {
                UR.warn(sp, Component.translatable("message.urmagic.too_complex", def.school().displayName(), def.level()));
                return InteractionResultHolder.fail(stack);
            }
            MagicApi.learn(sp, id);
            if (!sp.isCreative()) stack.shrink(1);
        }
        level.playSound(player, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1.2f);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SpellDef def = spell(stack);
        if (def == null) return;
        tooltip.add(Component.translatable("tooltip.urmagic.school", def.school().displayName(), def.level()).withStyle(ChatFormatting.AQUA));
        tooltip.add(def.description().copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.urmagic.cost", Math.round(def.cost())).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.urmagic.read").withStyle(ChatFormatting.DARK_GRAY));
    }
}
