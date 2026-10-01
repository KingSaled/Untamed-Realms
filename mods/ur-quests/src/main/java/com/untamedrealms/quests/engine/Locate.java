package com.untamedrealms.quests.engine;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Finding the nearest structure / biome named by an objective target ({@code id|#tag|...}). */
public final class Locate {
    private Locate() {}

    /** Every structure the target names, or an empty list. */
    public static List<Holder<Structure>> structures(ServerLevel level, String target) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<Holder<Structure>> out = new ArrayList<>();
        for (String alt : target.split("\\|")) {
            alt = alt.trim();
            boolean tag = alt.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? alt.substring(1) : alt);
            if (id == null) continue;
            if (tag) registry.getTag(TagKey.create(Registries.STRUCTURE, id)).ifPresent(set -> set.forEach(out::add));
            else registry.getHolder(ResourceKey.create(Registries.STRUCTURE, id)).ifPresent(out::add);
        }
        return out;
    }

    /** Nearest matching structure within {@code radiusChunks}, like {@code /locate structure}. */
    public static @Nullable Pair<BlockPos, Holder<Structure>> structure(ServerLevel level, String target, BlockPos from, int radiusChunks) {
        List<Holder<Structure>> holders = structures(level, target);
        if (holders.isEmpty()) return null;
        return level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holders), from, radiusChunks, false);
    }

    public static @Nullable BlockPos structurePos(ServerLevel level, String target, BlockPos from, int radiusChunks) {
        Pair<BlockPos, Holder<Structure>> found = structure(level, target, from, radiusChunks);
        return found == null ? null : found.getFirst();
    }

    /** Nearest matching biome within {@code radius} blocks, like {@code /locate biome}. */
    public static @Nullable BlockPos biome(ServerLevel level, String target, BlockPos from, int radius) {
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(holder -> {
            for (String alt : target.split("\\|")) {
                alt = alt.trim();
                ResourceLocation id = ResourceLocation.tryParse(alt.startsWith("#") ? alt.substring(1) : alt);
                if (id == null) continue;
                if (alt.startsWith("#") ? holder.is(TagKey.create(Registries.BIOME, id)) : holder.is(ResourceKey.create(Registries.BIOME, id))) return true;
            }
            return false;
        }, from, radius, 32, 64);
        return found == null ? null : found.getFirst();
    }
}
