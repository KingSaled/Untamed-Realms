package com.untamedrealms.arsenal.client;

import com.untamedrealms.arsenal.ArsenalItems;
import com.untamedrealms.arsenal.UntamedArsenal;
import com.untamedrealms.arsenal.block.Station;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BowItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public final class ArsenalClient {
    private ArsenalClient() {}

    public static void openStation(BlockPos pos, Station station) {
        Minecraft.getInstance().setScreen(new StationScreen(pos, station));
    }

    @EventBusSubscriber(modid = UntamedArsenal.MODID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {}

        /** Our bows animate like the vanilla bow (pull / pulling item properties). */
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ArsenalItems.WEAPONS.values().forEach(holder -> {
                if (!(holder.get() instanceof BowItem bow)) return;
                ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
                        entity == null || entity.getUseItem() != stack ? 0f
                                : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20f);
                ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1f : 0f);
            }));
        }
    }
}
