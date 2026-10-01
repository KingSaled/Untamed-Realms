package com.untamedrealms.skills.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A string matcher used throughout skill data: {@code "minecraft:diamond_ore"} (exact id),
 * {@code "#minecraft:logs"} (tag) or {@code "*"} (anything). Works for both items and blocks.
 */
public record Matcher(String raw, ResourceLocation id, boolean tag, boolean any) {
    public static final Codec<Matcher> CODEC = Codec.STRING.comapFlatMap(Matcher::parse, Matcher::raw);

    public static DataResult<Matcher> parse(String raw) {
        if (raw.equals("*")) return DataResult.success(new Matcher(raw, null, false, true));
        boolean tag = raw.startsWith("#");
        ResourceLocation id = ResourceLocation.tryParse(tag ? raw.substring(1) : raw);
        if (id == null) return DataResult.error(() -> "Invalid matcher: " + raw);
        return DataResult.success(new Matcher(raw, id, tag, false));
    }

    public boolean matches(BlockState state) {
        if (any) return true;
        if (tag) return state.is(TagKey.create(Registries.BLOCK, id));
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(id);
    }

    public boolean matches(Block block) {
        return matches(block.defaultBlockState());
    }

    public boolean matches(ItemStack stack) {
        if (any) return !stack.isEmpty();
        if (tag) return stack.is(TagKey.create(Registries.ITEM, id));
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
    }

    public boolean matches(Item item) {
        return matches(new ItemStack(item));
    }

    /** Exact-id matchers win over tag matchers, which win over wildcards. */
    public int specificity() {
        return any ? 0 : tag ? 1 : 2;
    }
}
