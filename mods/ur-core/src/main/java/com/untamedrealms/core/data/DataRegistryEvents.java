package com.untamedrealms.core.data;

import com.untamedrealms.core.UntamedCore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/** Wires every {@link DataRegistry} into datapack reloading and login/reload syncing. */
@EventBusSubscriber(modid = UntamedCore.MODID)
public final class DataRegistryEvents {
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        for (DataRegistry<?> registry : DataRegistry.all()) {
            event.addListener(registry.createListener(event.getRegistryAccess()));
        }
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(player -> {
            for (DataRegistry<?> registry : DataRegistry.all()) {
                registry.sendTo(player);
            }
        });
    }

    private DataRegistryEvents() {}
}
