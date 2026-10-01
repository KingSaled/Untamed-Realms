package com.untamedrealms.quests.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.untamedrealms.quests.UntamedQuests;
import com.untamedrealms.quests.data.QuestLog;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Client mirror of the local player's quest log plus key bindings and HUD registration. */
public final class ClientQuests {
    public static final KeyMapping OPEN_JOURNAL = new KeyMapping("key.urquests.journal",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.untamedrealms");
    public static final KeyMapping TOGGLE_TRACKER = new KeyMapping("key.urquests.toggle_tracker",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.untamedrealms");

    private static final QuestLog LOG = new QuestLog();
    static boolean trackerVisible = true;

    private ClientQuests() {}

    public static QuestLog log() {
        return LOG;
    }

    public static void onSync(CompoundTag tag, HolderLookup.Provider registries) {
        LOG.deserializeNBT(registries, tag);
    }

    public static void openBoard(BlockPos pos, List<ResourceLocation> offers) {
        Minecraft.getInstance().setScreen(new NoticeBoardScreen(pos, offers));
    }

    @EventBusSubscriber(modid = UntamedQuests.MODID, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_JOURNAL);
            event.register(TOGGLE_TRACKER);
        }

        @SubscribeEvent
        public static void registerLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(UntamedQuests.id("quest_tracker"), QuestTrackerHud::render);
        }

        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_JOURNAL.consumeClick()) {
                if (mc.player != null && mc.screen == null) mc.setScreen(new QuestJournalScreen());
            }
            while (TOGGLE_TRACKER.consumeClick()) trackerVisible = !trackerVisible;
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            LOG.deserializeNBT(null, new CompoundTag());
        }
    }
}
