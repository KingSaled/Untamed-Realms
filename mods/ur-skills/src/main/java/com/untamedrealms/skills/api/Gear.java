package com.untamedrealms.skills.api;

import com.untamedrealms.skills.UntamedSkills;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Classifies weapons and armor into skills. Tags first, sensible fallbacks for unknown modded gear. */
public final class Gear {
    public static final TagKey<Item> ONE_HANDED = ItemTags.create(UntamedSkills.id("one_handed"));
    public static final TagKey<Item> TWO_HANDED = ItemTags.create(UntamedSkills.id("two_handed"));
    public static final TagKey<Item> HEAVY_ARMOR = ItemTags.create(UntamedSkills.id("armor/heavy"));
    public static final TagKey<Item> LIGHT_ARMOR = ItemTags.create(UntamedSkills.id("armor/light"));

    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private Gear() {}

    /** Melee weapon skill for an item, or null for fists / non-weapons. */
    public static Skill weaponSkill(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(TWO_HANDED)) return Skill.TWO_HANDED;
        if (stack.is(ONE_HANDED) || stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(Items.TRIDENT)) {
            return Skill.ONE_HANDED;
        }
        return null;
    }

    /** Armor skill for a worn item, or null if it is not armor. */
    public static Skill armorSkill(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(HEAVY_ARMOR)) return Skill.HEAVY_ARMOR;
        if (stack.is(LIGHT_ARMOR)) return Skill.LIGHT_ARMOR;
        if (!(stack.getItem() instanceof ArmorItem armor)) return null;
        // Fallback: anything at least as protective as iron (or with toughness) counts as heavy.
        int ironDefense = switch (armor.getType()) {
            case HELMET -> 2;
            case CHESTPLATE -> 6;
            case LEGGINGS -> 5;
            case BOOTS -> 2;
            case BODY -> 11;
        };
        return armor.getToughness() > 0 || armor.getDefense() >= ironDefense ? Skill.HEAVY_ARMOR : Skill.LIGHT_ARMOR;
    }

    /** Number of worn pieces (0-4) of the given armor skill. */
    public static int piecesWorn(Player player, Skill armorSkill) {
        int count = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (armorSkill(player.getItemBySlot(slot)) == armorSkill) count++;
        }
        return count;
    }

    public static EquipmentSlot[] armorSlots() {
        return ARMOR_SLOTS;
    }
}
