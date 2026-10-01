package com.untamedrealms.core.registry;

import com.mojang.serialization.MapCodec;
import com.untamedrealms.core.UntamedCore;
import com.untamedrealms.core.loot.InjectChestLootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class CoreLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, UntamedCore.MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<InjectChestLootModifier>> INJECT_CHEST_LOOT =
            REGISTER.register("inject_chest_loot", () -> InjectChestLootModifier.CODEC);

    private CoreLootModifiers() {}
}
