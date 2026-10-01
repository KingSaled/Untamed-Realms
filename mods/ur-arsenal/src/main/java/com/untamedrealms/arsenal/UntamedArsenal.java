package com.untamedrealms.arsenal;

import com.mojang.logging.LogUtils;
import com.untamedrealms.arsenal.data.StationRecipe;
import com.untamedrealms.core.data.DataRegistry;
import com.untamedrealms.core.registry.CoreItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

/**
 * Untamed Realms: Arsenal. Skyrim's material tiers as weapons and armor, the ores and materials to
 * make them, and the stations that do: forge, tanning rack and workbench (tempering).
 */
@Mod(UntamedArsenal.MODID)
public final class UntamedArsenal {
    public static final String MODID = "urarsenal";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DataRegistry<StationRecipe> RECIPES =
            DataRegistry.create(id("station_recipes"), "urarsenal/station_recipes", StationRecipe.CODEC, true);

    public UntamedArsenal(IEventBus modBus, ModContainer container) {
        ArsenalBlocks.BLOCKS.register(modBus);
        ArsenalArmor.MATERIALS.register(modBus);   // also adds the armor items to ArsenalItems.ITEMS
        ArsenalItems.ITEMS.register(modBus);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTabKey().equals(CoreItems.TAB_KEY)) {
                ArsenalItems.ITEMS.getEntries().forEach(item -> event.accept(item.get()));
            }
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
