package com.untamedrealms.classes.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.untamedrealms.classes.UntamedClasses;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Starting items, kept as raw data and decoded lazily. A loadout entry that names an item from a mod
 * that isn't installed is skipped with a warning instead of making the whole class fail to load.
 */
public record Loadout(List<Dynamic<?>> entries) {
    public static final Codec<Loadout> CODEC = Codec.PASSTHROUGH.listOf().xmap(Loadout::new, Loadout::entries);
    public static final Loadout EMPTY = new Loadout(List.of());

    public List<ItemStack> resolve(HolderLookup.Provider registries) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Dynamic<?> entry : entries) {
            decode(entry, registries, stacks);
        }
        return stacks;
    }

    private static <T> void decode(Dynamic<T> entry, HolderLookup.Provider registries, List<ItemStack> out) {
        RegistryOps<T> ops = RegistryOps.create(entry.getOps(), registries);
        ItemStack.CODEC.parse(ops, entry.getValue())
                .resultOrPartial(error -> UntamedClasses.LOGGER.warn("Skipping loadout item: {}", error))
                .ifPresent(out::add);
    }
}
