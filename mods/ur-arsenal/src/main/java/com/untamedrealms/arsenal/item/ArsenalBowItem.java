package com.untamedrealms.arsenal.item;

import com.untamedrealms.arsenal.ArsenalTier;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/** A bow whose arrows hit harder with better materials. */
public class ArsenalBowItem extends BowItem {
    private final ArsenalTier tier;

    public ArsenalBowItem(ArsenalTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
        arrow.setBaseDamage(arrow.getBaseDamage() * tier.bowPower);
        return arrow;
    }

    @Override
    public int getEnchantmentValue() {
        return tier.enchantability;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(tier.material.get().asItem());
    }
}
