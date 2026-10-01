package com.untamedrealms.arsenal;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Light (Hide, Leather, Elven, Glass) and heavy (Iron .. Daedric) armor sets. */
public final class ArsenalArmor {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, UntamedArsenal.MODID);

    /**
     * A set: defense per piece (helmet, chest, legs, boots), toughness, durability multiplier, the
     * repair / forging material, and the Light or Heavy Armor level it needs.
     */
    public enum Set {
        HIDE("hide", true, new int[]{1, 3, 2, 1}, 0f, 8, () -> Items.LEATHER, 1),
        LEATHER("leather", true, new int[]{2, 4, 3, 1}, 0f, 11, () -> Items.LEATHER, 10),
        ELVEN("elven", true, new int[]{2, 5, 4, 2}, 0.5f, 18, () -> ArsenalItems.REFINED_MOONSTONE.get(), 40),
        GLASS("glass", true, new int[]{3, 6, 5, 2}, 1f, 24, () -> ArsenalItems.REFINED_MALACHITE.get(), 55),
        IRON("iron", false, new int[]{2, 6, 5, 2}, 0f, 15, () -> Items.IRON_INGOT, 1),
        STEEL("steel", false, new int[]{3, 6, 5, 2}, 1f, 20, () -> ArsenalItems.STEEL_INGOT.get(), 10),
        ORCISH("orcish", false, new int[]{3, 7, 6, 2}, 1.5f, 24, () -> ArsenalItems.ORICHALCUM_INGOT.get(), 20),
        DWARVEN("dwarven", false, new int[]{3, 7, 6, 3}, 2f, 28, () -> ArsenalItems.DWARVEN_INGOT.get(), 30),
        EBONY("ebony", false, new int[]{3, 8, 6, 3}, 2.5f, 34, () -> ArsenalItems.EBONY_INGOT.get(), 70),
        DAEDRIC("daedric", false, new int[]{4, 8, 7, 3}, 3f, 40, () -> ArsenalItems.EBONY_INGOT.get(), 85);

        public final String id;
        public final boolean light;
        final int[] defense;
        final float toughness;
        final int durability;
        public final Supplier<ItemLike> material;
        public final int level;
        DeferredHolder<ArmorMaterial, ArmorMaterial> holder;

        Set(String id, boolean light, int[] defense, float toughness, int durability, Supplier<ItemLike> material, int level) {
            this.id = id;
            this.light = light;
            this.defense = defense;
            this.toughness = toughness;
            this.durability = durability;
            this.material = material;
            this.level = level;
        }
    }

    public static final ArmorItem.Type[] PIECES = {ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};

    /** "<set>_<piece>" -> armor item. */
    public static final Map<String, DeferredItem<ArmorItem>> ITEMS = new LinkedHashMap<>();

    static {
        for (Set set : Set.values()) {
            set.holder = MATERIALS.register(set.id, () -> material(set));
            Rarity rarity = set.level >= 70 ? Rarity.EPIC : set.level >= 40 ? Rarity.RARE : set.level >= 20 ? Rarity.UNCOMMON : Rarity.COMMON;
            for (ArmorItem.Type piece : PIECES) {
                String name = set.id + "_" + piece.getName();
                ITEMS.put(name, ArsenalItems.ITEMS.register(name, () -> new ArmorItem(set.holder, piece,
                        new Item.Properties().durability(piece.getDurability(set.durability)).rarity(rarity))));
            }
        }
    }

    private ArsenalArmor() {}

    private static ArmorMaterial material(Set set) {
        Map<ArmorItem.Type, Integer> defense = Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
            map.put(ArmorItem.Type.HELMET, set.defense[0]);
            map.put(ArmorItem.Type.CHESTPLATE, set.defense[1]);
            map.put(ArmorItem.Type.LEGGINGS, set.defense[2]);
            map.put(ArmorItem.Type.BOOTS, set.defense[3]);
            map.put(ArmorItem.Type.BODY, set.defense[1]);
        });
        Holder<SoundEvent> sound = set.light ? SoundEvents.ARMOR_EQUIP_LEATHER : SoundEvents.ARMOR_EQUIP_IRON;
        return new ArmorMaterial(defense, 12, sound, () -> Ingredient.of(set.material.get()),
                List.of(new ArmorMaterial.Layer(UntamedArsenal.id(set.id))), set.toughness, set.light ? 0f : 0.05f);
    }
}
