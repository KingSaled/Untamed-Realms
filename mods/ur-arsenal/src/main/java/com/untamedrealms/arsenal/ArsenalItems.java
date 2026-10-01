package com.untamedrealms.arsenal;

import com.untamedrealms.arsenal.item.ArsenalBowItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/** Every arsenal item: tiered weapons, smithing materials and ore blocks. */
public final class ArsenalItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UntamedArsenal.MODID);

    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    public static final DeferredItem<Item> ORICHALCUM_INGOT = ITEMS.registerSimpleItem("orichalcum_ingot");
    public static final DeferredItem<Item> DWARVEN_INGOT = ITEMS.registerSimpleItem("dwarven_ingot");
    public static final DeferredItem<Item> REFINED_MOONSTONE = ITEMS.registerSimpleItem("refined_moonstone");
    public static final DeferredItem<Item> REFINED_MALACHITE = ITEMS.registerSimpleItem("refined_malachite");
    public static final DeferredItem<Item> EBONY_INGOT = ITEMS.registerSimpleItem("ebony_ingot");
    public static final DeferredItem<Item> RAW_ORICHALCUM = ITEMS.registerSimpleItem("raw_orichalcum");
    public static final DeferredItem<Item> RAW_MOONSTONE = ITEMS.registerSimpleItem("raw_moonstone");
    public static final DeferredItem<Item> RAW_MALACHITE = ITEMS.registerSimpleItem("raw_malachite");
    public static final DeferredItem<Item> RAW_EBONY = ITEMS.registerSimpleItem("raw_ebony");
    public static final DeferredItem<Item> DWARVEN_SCRAP = ITEMS.registerSimpleItem("dwarven_scrap");
    public static final DeferredItem<Item> LEATHER_STRIPS = ITEMS.registerSimpleItem("leather_strips");
    public static final DeferredItem<Item> DAEDRA_HEART = ITEMS.registerSimpleItem("daedra_heart", new Item.Properties().rarity(Rarity.RARE));

    public static final DeferredItem<BlockItem> ORICHALCUM_ORE = ITEMS.registerSimpleBlockItem(ArsenalBlocks.ORICHALCUM_ORE);
    public static final DeferredItem<BlockItem> MOONSTONE_ORE = ITEMS.registerSimpleBlockItem(ArsenalBlocks.MOONSTONE_ORE);
    public static final DeferredItem<BlockItem> MALACHITE_ORE = ITEMS.registerSimpleBlockItem(ArsenalBlocks.MALACHITE_ORE);
    public static final DeferredItem<BlockItem> EBONY_ORE = ITEMS.registerSimpleBlockItem(ArsenalBlocks.EBONY_ORE);
    public static final DeferredItem<BlockItem> FORGE = ITEMS.registerSimpleBlockItem(ArsenalBlocks.FORGE);
    public static final DeferredItem<BlockItem> TANNING_RACK = ITEMS.registerSimpleBlockItem(ArsenalBlocks.TANNING_RACK);
    public static final DeferredItem<BlockItem> WORKBENCH = ITEMS.registerSimpleBlockItem(ArsenalBlocks.WORKBENCH);

    /** "<tier>_<type>" -> weapon, e.g. "daedric_greatsword". */
    public static final Map<String, DeferredItem<Item>> WEAPONS = new LinkedHashMap<>();

    static {
        for (ArsenalTier tier : ArsenalTier.values()) {
            for (WeaponType type : WeaponType.values()) {
                String name = tier.id + "_" + type.id;
                Rarity rarity = tier.level >= 70 ? Rarity.EPIC : tier.level >= 40 ? Rarity.RARE : tier.level >= 20 ? Rarity.UNCOMMON : Rarity.COMMON;
                WEAPONS.put(name, ITEMS.register(name, () -> weapon(tier, type, new Item.Properties().rarity(rarity))));
            }
        }
    }

    private ArsenalItems() {}

    private static Item weapon(ArsenalTier tier, WeaponType type, Item.Properties props) {
        if (type == WeaponType.BOW) return new ArsenalBowItem(tier, props.durability(tier.uses));
        ItemAttributeModifiers attributes = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_damage"),
                        type.base + tier.damageBonus, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_speed"),
                        type.speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
        props = props.attributes(attributes);
        if (type == WeaponType.WAR_AXE || type == WeaponType.BATTLEAXE) return new AxeItem(tier.tier(), props);
        return new SwordItem(tier.tier(), props);
    }

    public static Item weapon(ArsenalTier tier, WeaponType type) {
        return WEAPONS.get(tier.id + "_" + type.id).get();
    }
}
