package com.untamedrealms.arcana.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** What a player has learned: each ingredient's discovered effects (bitmask) and known enchantments. */
public final class ArcanaKnowledge implements INBTSerializable<CompoundTag> {
    private final Map<ResourceLocation, Integer> ingredients = new LinkedHashMap<>();
    private final Set<ResourceLocation> enchantments = new LinkedHashSet<>();

    public boolean knows(ResourceLocation item, int effectIndex) {
        return (ingredients.getOrDefault(item, 0) & (1 << effectIndex)) != 0;
    }

    /** Returns whether this was new. */
    public boolean learn(ResourceLocation item, int effectIndex) {
        int before = ingredients.getOrDefault(item, 0);
        int after = before | (1 << effectIndex);
        ingredients.put(item, after);
        return after != before;
    }

    public Set<ResourceLocation> enchantments() {
        return enchantments;
    }

    /** Discovered effects per ingredient item, as bitmasks (bit i = effect i). */
    public Map<ResourceLocation, Integer> ingredients() {
        return ingredients;
    }

    public void replaceWith(Map<ResourceLocation, Integer> ingredients, java.util.Collection<ResourceLocation> enchantments) {
        this.ingredients.clear();
        this.ingredients.putAll(ingredients);
        this.enchantments.clear();
        this.enchantments.addAll(enchantments);
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        CompoundTag ing = new CompoundTag();
        ingredients.forEach((k, v) -> ing.putInt(k.toString(), v));
        tag.put("ingredients", ing);
        ListTag ench = new ListTag();
        enchantments.forEach(e -> ench.add(StringTag.valueOf(e.toString())));
        tag.put("enchantments", ench);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        ingredients.clear();
        enchantments.clear();
        CompoundTag ing = tag.getCompound("ingredients");
        for (String key : ing.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) ingredients.put(id, ing.getInt(key));
        }
        ListTag ench = tag.getList("enchantments", Tag.TAG_STRING);
        for (int i = 0; i < ench.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(ench.getString(i));
            if (id != null) enchantments.add(id);
        }
    }
}
