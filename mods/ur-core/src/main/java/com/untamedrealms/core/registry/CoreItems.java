package com.untamedrealms.core.registry;

import com.untamedrealms.core.UntamedCore;
import com.untamedrealms.core.economy.CoinItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CoreItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(UntamedCore.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UntamedCore.MODID);

    public static final DeferredItem<CoinItem> CROWN = REGISTER.register("crown",
            () -> new CoinItem(1, new Item.Properties().stacksTo(99)));
    public static final DeferredItem<CoinItem> CROWN_PURSE = REGISTER.register("crown_purse",
            () -> new CoinItem(100, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    /** Shared creative tab. Other modules add their items to it via BuildCreativeModeTabContentsEvent. */
    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, UntamedCore.id("untamed_realms"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("untamed_realms",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.untamedrealms"))
                    .icon(() -> new ItemStack(CROWN_PURSE.get()))
                    .displayItems((params, output) -> {
                        output.accept(CROWN.get());
                        output.accept(CROWN_PURSE.get());
                    })
                    .build());

    private CoreItems() {}
}
