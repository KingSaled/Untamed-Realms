package com.untamedrealms.core.client;

import com.untamedrealms.core.CoreClientConfig;
import com.untamedrealms.core.UntamedCore;
import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.core.registry.CoreItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ToastAddEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class CoreClient {
    private CoreClient() {}

    @EventBusSubscriber(modid = UntamedCore.MODID, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void registerLayers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.HOTBAR, UntamedCore.id("vitals"), VitalsHud::render);
            event.registerAboveAll(UntamedCore.id("notifications"), NotificationHud::render);
        }
    }

    @EventBusSubscriber(modid = UntamedCore.MODID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            NotificationHud.tick();
            preventExhaustedSprint();
        }

        @SubscribeEvent
        public static void onClientTickPre(ClientTickEvent.Pre event) {
            preventExhaustedSprint();
        }

        private static void preventExhaustedSprint() {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player != null && ClientCoreState.exhausted() && !player.isCreative()) {
                if (player.isSprinting()) player.setSprinting(false);
                mc.options.keySprint.setDown(false);
            }
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientCoreState.reset();
            NotificationHud.clear();
        }

        /** Applies the pack's key layout once the game has finished loading (first menu). */
        @SubscribeEvent
        public static void onScreenOpening(ScreenEvent.Opening event) {
            if (event.getNewScreen() instanceof TitleScreen || event.getNewScreen() instanceof AccessibilityOnboardingScreen) {
                KeybindProfile.onFirstMenu();
                if (event.getNewScreen() instanceof TitleScreen && CoreClientConfig.CUSTOM_TITLE_SCREEN.get()) {
                    event.setNewScreen(new UrTitleScreen());
                }
                if (CoreClientConfig.HIDE_VANILLA_POPUPS.get()) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.options.tutorialStep != TutorialSteps.NONE) {
                        mc.getTutorial().setStep(TutorialSteps.NONE);
                        mc.options.save();
                    }
                }
            }
        }

        /** Quests replace vanilla's tutorial hints, advancement and recipe pop-ups. */
        @SubscribeEvent
        public static void onToast(ToastAddEvent event) {
            if (!CoreClientConfig.HIDE_VANILLA_POPUPS.get()) return;
            if (event.getToast() instanceof AdvancementToast || event.getToast() instanceof RecipeToast || event.getToast() instanceof TutorialToast) {
                event.setCanceled(true);
            }
        }

        /** Shows the Crown balance on a small tab above the survival inventory's top-right corner. */
        @SubscribeEvent
        public static void onScreenRender(ScreenEvent.Render.Post event) {
            if (!(event.getScreen() instanceof InventoryScreen screen) || !CoreClientConfig.SHOW_WALLET_IN_INVENTORY.get()) return;
            GuiGraphics g = event.getGuiGraphics();
            Component text = Component.translatable("gui.urcore.wallet", UiKit.compact(ClientCoreState.crowns()));
            int w = Minecraft.getInstance().font.width(text) + 26;
            int x = screen.getGuiLeft() + screen.getXSize() - w;
            int y = screen.getGuiTop() - 21;
            UiKit.inset(g, x, y, w, 20);
            g.renderItem(new ItemStack(CoreItems.CROWN.get()), x + 2, y + 2);
            g.drawString(Minecraft.getInstance().font, text, x + 21, y + 6, UiKit.GOLD, true);
        }
    }
}
