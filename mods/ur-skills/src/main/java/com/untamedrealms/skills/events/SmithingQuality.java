package com.untamedrealms.skills.events;

import com.untamedrealms.skills.UntamedSkills;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.effect.EffectTypes;
import com.untamedrealms.skills.registry.SkillsComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Skyrim-style tempering at craft time: the higher your Smithing, the more likely crafted weapons and
 * armor come out Fine, Superior, Exquisite or Flawless, each tier adding damage or armor.
 */
@EventBusSubscriber(modid = UntamedSkills.MODID)
public final class SmithingQuality {
    private static final net.minecraft.resources.ResourceLocation WEAPON_ID = UntamedSkills.id("quality_damage");
    private static final net.minecraft.resources.ResourceLocation ARMOR_ID = UntamedSkills.id("quality_armor");

    private SmithingQuality() {}

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        roll(player, event.getCrafting());
    }

    /** Rolls a crafted piece of gear's quality from the crafter's Smithing (also used by ur-arsenal's forge). */
    public static void roll(Player player, ItemStack stack) {
        if (!isGear(stack) || stack.has(SkillsComponents.QUALITY.get())) return;
        int level = SkillsApi.level(player, Skill.SMITHING);
        float bonus = SkillsApi.effect(player, EffectTypes.SMITHING_QUALITY, "");
        float[] thresholds = {
                level * 0.008f + bonus,
                (level - 25) * 0.006f + bonus,
                (level - 50) * 0.005f + bonus,
                (level - 75) * 0.004f + bonus
        };
        int quality = 0;
        for (float t : thresholds) {
            if (player.getRandom().nextFloat() < t) quality++;
            else break;
        }
        if (quality > 0) stack.set(SkillsComponents.QUALITY.get(), quality);
    }

    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        Integer quality = stack.get(SkillsComponents.QUALITY.get());
        if (quality == null || quality <= 0) return;
        if (stack.getItem() instanceof ArmorItem armor) {
            event.addModifier(Attributes.ARMOR,
                    new AttributeModifier(ARMOR_ID, 0.5 * quality, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.bySlot(armor.getEquipmentSlot()));
        } else {
            event.addModifier(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(WEAPON_ID, 0.5 * quality, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
    }

    public static boolean isGear(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return false;
        if (stack.getItem() instanceof ArmorItem) return true;
        ItemAttributeModifiers modifiers = stack.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return modifiers.modifiers().stream().anyMatch(e -> e.attribute().value() == Attributes.ATTACK_DAMAGE.value());
    }
}
