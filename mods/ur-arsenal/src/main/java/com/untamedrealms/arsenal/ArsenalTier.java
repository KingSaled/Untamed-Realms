package com.untamedrealms.arsenal;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.SimpleTier;

import java.util.function.Supplier;

/** Skyrim's weapon materials, weakest to strongest. {@code level} gates crafting (Smithing) and wielding. */
public enum ArsenalTier {
    IRON("iron", 250, 6f, 2f, 14, () -> Items.IRON_INGOT, 1, 1.0f, BlockTags.INCORRECT_FOR_IRON_TOOL),
    STEEL("steel", 400, 6.5f, 2.5f, 12, () -> ArsenalItems.STEEL_INGOT.get(), 10, 1.1f, BlockTags.INCORRECT_FOR_IRON_TOOL),
    ORCISH("orcish", 520, 7f, 3f, 10, () -> ArsenalItems.ORICHALCUM_INGOT.get(), 20, 1.2f, BlockTags.INCORRECT_FOR_IRON_TOOL),
    DWARVEN("dwarven", 650, 7.5f, 3.5f, 12, () -> ArsenalItems.DWARVEN_INGOT.get(), 30, 1.3f, BlockTags.INCORRECT_FOR_DIAMOND_TOOL),
    ELVEN("elven", 800, 8f, 4f, 18, () -> ArsenalItems.REFINED_MOONSTONE.get(), 40, 1.4f, BlockTags.INCORRECT_FOR_DIAMOND_TOOL),
    GLASS("glass", 1100, 8.5f, 5f, 16, () -> ArsenalItems.REFINED_MALACHITE.get(), 55, 1.55f, BlockTags.INCORRECT_FOR_DIAMOND_TOOL),
    EBONY("ebony", 1600, 9f, 6f, 12, () -> ArsenalItems.EBONY_INGOT.get(), 70, 1.7f, BlockTags.INCORRECT_FOR_NETHERITE_TOOL),
    DAEDRIC("daedric", 2200, 9.5f, 7f, 15, () -> ArsenalItems.EBONY_INGOT.get(), 85, 1.9f, BlockTags.INCORRECT_FOR_NETHERITE_TOOL);

    public final String id;
    public final int uses;
    public final float speed;
    public final float damageBonus;
    public final int enchantability;
    public final Supplier<ItemLike> material;
    /** Smithing level to forge it; the matching weapon skill level to wield it at full strength. */
    public final int level;
    /** Arrow damage multiplier for this tier's bow. */
    public final float bowPower;
    private final TagKey<Block> incorrectForDrops;
    private Tier tier;

    ArsenalTier(String id, int uses, float speed, float damageBonus, int enchantability, Supplier<ItemLike> material, int level,
                float bowPower, TagKey<Block> incorrectForDrops) {
        this.id = id;
        this.uses = uses;
        this.speed = speed;
        this.damageBonus = damageBonus;
        this.enchantability = enchantability;
        this.material = material;
        this.level = level;
        this.bowPower = bowPower;
        this.incorrectForDrops = incorrectForDrops;
    }

    /** The vanilla {@link Tier} (durability, mining speed, damage bonus, repair material). */
    public Tier tier() {
        if (tier == null) {
            tier = new SimpleTier(incorrectForDrops, uses, speed, damageBonus, enchantability, () -> Ingredient.of(material.get()));
        }
        return tier;
    }
}
