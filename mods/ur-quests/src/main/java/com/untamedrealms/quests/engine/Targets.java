package com.untamedrealms.quests.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Matching of objective targets. A target is one or more alternatives joined with {@code |}; each is
 * {@code *}, an exact id, or a {@code #tag}.
 */
public final class Targets {
    private Targets() {}

    /** Entity targets also accept {@code @monster} (any hostile creature). */
    public static boolean entity(String target, EntityType<?> type) {
        for (String alt : target.split("\\|")) {
            alt = alt.trim();
            if (alt.equals("*")) return true;
            if (alt.equals("@monster") && type.getCategory() == MobCategory.MONSTER) return true;
            if (alt.startsWith("#")) {
                ResourceLocation id = ResourceLocation.tryParse(alt.substring(1));
                if (id != null && type.is(TagKey.create(Registries.ENTITY_TYPE, id))) return true;
            } else if (BuiltInRegistries.ENTITY_TYPE.getKey(type).toString().equals(alt)) {
                return true;
            }
        }
        return false;
    }

    public static boolean item(String target, ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (String alt : target.split("\\|")) {
            alt = alt.trim();
            if (alt.equals("*")) return true;
            if (alt.startsWith("#")) {
                ResourceLocation id = ResourceLocation.tryParse(alt.substring(1));
                if (id != null && stack.is(TagKey.create(Registries.ITEM, id))) return true;
            } else if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(alt)) {
                return true;
            }
        }
        return false;
    }

    public static boolean block(String target, BlockState state) {
        for (String alt : target.split("\\|")) {
            alt = alt.trim();
            if (alt.equals("*")) return true;
            if (alt.startsWith("#")) {
                ResourceLocation id = ResourceLocation.tryParse(alt.substring(1));
                if (id != null && state.is(TagKey.create(Registries.BLOCK, id))) return true;
            } else if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(alt)) {
                return true;
            }
        }
        return false;
    }

    public static boolean id(String target, ResourceLocation id) {
        for (String alt : target.split("\\|")) {
            if (alt.trim().equals("*") || alt.trim().equals(id.toString())) return true;
        }
        return false;
    }

    /** Is {@code pos} inside (a piece of) any of the target structures? */
    public static boolean structure(String target, ServerLevel level, BlockPos pos) {
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (String alt : target.split("\\|")) {
            alt = alt.trim();
            if (alt.startsWith("#")) {
                ResourceLocation id = ResourceLocation.tryParse(alt.substring(1));
                if (id == null) continue;
                if (level.structureManager().getStructureWithPieceAt(pos, TagKey.create(Registries.STRUCTURE, id)).isValid()) return true;
            } else {
                ResourceLocation id = ResourceLocation.tryParse(alt);
                if (id == null) continue;
                Structure structure = registry.get(id);
                if (structure != null && level.structureManager().getStructureWithPieceAt(pos, structure).isValid()) return true;
            }
        }
        return false;
    }

    public static boolean biome(String target, ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        for (String alt : target.split("\\|")) {
            alt = alt.trim();
            ResourceLocation id = ResourceLocation.tryParse(alt.startsWith("#") ? alt.substring(1) : alt);
            if (id == null) continue;
            if (alt.startsWith("#") ? biome.is(TagKey.create(Registries.BIOME, id)) : biome.is(ResourceKey.create(Registries.BIOME, id))) return true;
        }
        return false;
    }
}
