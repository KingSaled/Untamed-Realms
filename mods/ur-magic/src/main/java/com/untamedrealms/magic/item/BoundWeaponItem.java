package com.untamedrealms.magic.item;

import com.untamedrealms.magic.UntamedMagic;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

/** A conjured weapon: strong, unbreakable, undroppable, and gone when the spell expires. */
public class BoundWeaponItem extends SwordItem {
    public BoundWeaponItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        Long expires = stack.get(UntamedMagic.EXPIRES.get());
        if (expires == null || level.getGameTime() > expires) stack.setCount(0);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        stack.setCount(0);
        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

}
