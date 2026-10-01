package com.untamedrealms.npcs.client;

import com.untamedrealms.npcs.UntamedNpcs;
import com.untamedrealms.npcs.network.NpcsNetwork;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class ClientNpcs {
    private ClientNpcs() {}

    public static void openDialogue(NpcsNetwork.OpenDialogue payload) {
        Minecraft.getInstance().setScreen(new DialogueScreen(payload));
    }

    public static void closeDialogue() {
        if (Minecraft.getInstance().screen instanceof DialogueScreen) Minecraft.getInstance().setScreen(null);
    }

    public static void openShop(NpcsNetwork.OpenShop payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ShopScreen shop && shop.entityId() == payload.entityId()) shop.update(payload);
        else mc.setScreen(new ShopScreen(payload));
    }

    @EventBusSubscriber(modid = UntamedNpcs.MODID, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(UntamedNpcs.NPC.get(), NpcRenderer::new);
        }
    }
}
