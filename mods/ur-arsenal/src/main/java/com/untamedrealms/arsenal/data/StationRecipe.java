package com.untamedrealms.arsenal.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.untamedrealms.arsenal.block.Station;
import com.untamedrealms.skills.api.Skill;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A forge / tanning rack recipe, from {@code data/<ns>/urarsenal/station_recipes/<id>.json}:
 * inputs (item ids or #tags with counts), a result, the skill level it needs and the XP it gives.
 */
public record StationRecipe(Station station, ResourceLocation result, int count, List<Input> inputs, Skill skill, int level,
                            float xp, String category) {
    public record Input(String item, int count) {
        public static final Codec<Input> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("item").forGetter(Input::item),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Input::count)
        ).apply(inst, Input::new));

        public boolean matches(ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (item.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(item.substring(1));
                return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
            }
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(item);
        }

        /** An item to show for this input in the station window. */
        public ItemStack icon() {
            if (item.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(item.substring(1));
                if (tag != null) {
                    var first = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tag)).flatMap(set -> set.stream().findFirst());
                    if (first.isPresent()) return new ItemStack(first.get().value(), count);
                }
                return ItemStack.EMPTY;
            }
            ResourceLocation id = ResourceLocation.tryParse(item);
            return id == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(id), count);
        }
    }

    public static final Codec<StationRecipe> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Station.CODEC.fieldOf("station").forGetter(StationRecipe::station),
            ResourceLocation.CODEC.fieldOf("result").forGetter(StationRecipe::result),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(StationRecipe::count),
            Input.CODEC.listOf().fieldOf("inputs").forGetter(StationRecipe::inputs),
            Skill.CODEC.optionalFieldOf("skill", Skill.SMITHING).forGetter(StationRecipe::skill),
            Codec.intRange(1, 99).optionalFieldOf("level", 1).forGetter(StationRecipe::level),
            Codec.FLOAT.optionalFieldOf("xp", 10f).forGetter(StationRecipe::xp),
            Codec.STRING.optionalFieldOf("category", "materials").forGetter(StationRecipe::category)
    ).apply(inst, StationRecipe::new));

    public Item resultItem() {
        return BuiltInRegistries.ITEM.get(result);
    }

    public ItemStack resultStack() {
        return new ItemStack(resultItem(), count);
    }
}
