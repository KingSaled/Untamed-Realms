package com.untamedrealms.quests.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.untamedrealms.quests.UntamedQuests;
import com.untamedrealms.quests.data.QuestLog;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;
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
    static @Nullable BlockPos marker;
    static Component markerLabel = Component.empty();
    /** Index (in the tracked quest's current stage) of the objective the marker points at, and the NPC's name. */
    static int markerObjective = -1;
    static @Nullable Component markerName;

    private ClientQuests() {}

    public static QuestLog log() {
        return LOG;
    }

    public static void onSync(CompoundTag tag, HolderLookup.Provider registries) {
        LOG.deserializeNBT(registries, tag);
    }

    public static void onMarker(@Nullable BlockPos pos, Component label, int objective, @Nullable Component name) {
        marker = pos;
        markerLabel = label;
        markerObjective = objective;
        markerName = name;
    }

    /** Objective text, naming the actual person when the objective is about one we have located. */
    public static Component describe(com.untamedrealms.quests.data.QuestDef.Objective obj, int index) {
        Component base = com.untamedrealms.quests.engine.QuestApi.describe(obj);
        if (index == markerObjective && markerName != null) {
            return Component.translatable("objective.urquests.named", base, markerName);
        }
        return base;
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
            event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, UntamedQuests.id("compass"), CompassHud::render);
        }

        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_JOURNAL.consumeClick()) {
                if (mc.player != null && mc.screen == null) mc.setScreen(new QuestJournalScreen());
            }
            while (TOGGLE_TRACKER.consumeClick()) trackerVisible = !trackerVisible;
        }

        /** Moves boss bars below the compass (lowest priority so nobody cancels the layer after the push). */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void beforeLayer(RenderGuiLayerEvent.Pre event) {
            if (event.getName().equals(VanillaGuiLayers.BOSS_OVERLAY) && CompassHud.visible()) {
                event.getGuiGraphics().pose().pushPose();
                event.getGuiGraphics().pose().translate(0, CompassHud.BOSS_OFFSET, 0);
            }
        }

        @SubscribeEvent
        public static void afterLayer(RenderGuiLayerEvent.Post event) {
            if (event.getName().equals(VanillaGuiLayers.BOSS_OVERLAY) && CompassHud.visible()) event.getGuiGraphics().pose().popPose();
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            LOG.deserializeNBT(null, new CompoundTag());
            marker = null;
            markerObjective = -1;
            markerName = null;
        }
    }
}
