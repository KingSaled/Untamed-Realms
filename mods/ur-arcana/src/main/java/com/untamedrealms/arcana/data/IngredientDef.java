package com.untamedrealms.arcana.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * An alchemy ingredient, from {@code data/<ns>/urarcana/ingredients/<id>.json}: an item and its four
 * hidden effects, in the order they are discovered (tasting reveals the first).
 */
public record IngredientDef(ResourceLocation item, List<ResourceLocation> effects) {
    public static final Codec<IngredientDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(IngredientDef::item),
            ResourceLocation.CODEC.listOf().fieldOf("effects").forGetter(IngredientDef::effects)
    ).apply(inst, IngredientDef::new));
}
