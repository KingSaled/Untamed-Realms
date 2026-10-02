package com.untamedrealms.arcana.enchanting;

import com.untamedrealms.arcana.item.SoulGemItem;
import com.untamedrealms.skills.effect.EffectTypes;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;

/**
 * The rules of enchanting, shared by server and client:
 * <ul>
 *   <li>Disenchanting destroys an enchanted item (or enchanted book) and teaches its enchantments.
 *       Curses can't be learned, and an item with nothing new to teach can't be disenchanted.</li>
 *   <li>Enchanting puts one known enchantment on gear that supports it, spending a filled soul gem.
 *       Gear holds one enchantment you make (two with the {@code enchant_slots} perk effect).</li>
 *   <li>Level: a share of the enchantment's max level from the soul's size (60%) and Enchanting level
 *       (40%), plus the {@code enchant_power} perk effect; at least 1.</li>
 * </ul>
 */
public final class Enchanting {
    public static final String ENCHANT_POWER = EffectTypes.ENCHANT_POWER;
    public static final String ENCHANT_SLOTS = EffectTypes.ENCHANT_SLOTS;

    private Enchanting() {}

    /** The enchantments an item carries (stored ones for enchanted books). */
    public static ItemEnchantments of(ItemStack stack) {
        if (stack.is(Items.ENCHANTED_BOOK)) return stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    /** The learnable (non-curse) enchantments on an item. */
    public static List<Holder<Enchantment>> learnable(ItemStack stack) {
        List<Holder<Enchantment>> out = new ArrayList<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> e : of(stack).entrySet()) {
            if (!e.getKey().is(EnchantmentTags.CURSE)) out.add(e.getKey());
        }
        return out;
    }

    /** Gear you can put an enchantment on at all (not books, potions, stacks or soul gems). */
    public static boolean isGear(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxStackSize() == 1 && !stack.is(Items.BOOK) && !stack.is(Items.ENCHANTED_BOOK)
                && !(stack.getItem() instanceof SoulGemItem) && (stack.isEnchantable() || stack.isDamageableItem());
    }

    public static int slots(float perkBonus) {
        return 1 + Math.round(perkBonus);
    }

    /** Whether {@code enchantment} can go on {@code stack} now, ignoring knowledge and gems. */
    public static boolean canApply(ItemStack stack, Holder<Enchantment> enchantment, int slots) {
        if (!isGear(stack) || enchantment.is(EnchantmentTags.CURSE) || !enchantment.value().canEnchant(stack)) return false;
        ItemEnchantments current = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (current.size() >= slots) return false;
        for (Holder<Enchantment> existing : current.keySet()) {
            if (existing.equals(enchantment) || !Enchantment.areCompatible(existing, enchantment)) return false;
        }
        return true;
    }

    public static int level(Holder<Enchantment> enchantment, int soul, int enchantingLevel, float perkBonus) {
        int max = enchantment.value().getMaxLevel();
        float share = Math.min(1f, soul / 5f * 0.6f + enchantingLevel / 99f * 0.4f + perkBonus);
        return Math.max(1, Math.min(max, (int) Math.ceil(max * share)));
    }

    public static float enchantXp(int soul) {
        return 5 + 12 * soul;
    }

    public static float disenchantXp(int learned) {
        return 10 + 8 * learned;
    }

    public static int soul(ItemStack stack) {
        return stack.getItem() instanceof SoulGemItem ? SoulGemItem.soul(stack) : 0;
    }
}
