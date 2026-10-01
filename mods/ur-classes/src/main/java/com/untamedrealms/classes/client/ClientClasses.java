package com.untamedrealms.classes.client;

import com.untamedrealms.classes.UntamedClasses;
import com.untamedrealms.classes.data.Birthsign;
import com.untamedrealms.classes.data.ClassDef;
import com.untamedrealms.classes.data.ClassesData;
import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.skills.client.SkillsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = UntamedClasses.MODID, value = Dist.CLIENT)
public final class ClientClasses {
    private static ResourceLocation classId;
    private static ResourceLocation birthsign;
    private static boolean pendingOpen;

    private ClientClasses() {}

    public static void onSync(ResourceLocation newClass, ResourceLocation newSign) {
        classId = newClass;
        birthsign = newSign;
        if (classId != null && Minecraft.getInstance().screen instanceof ClassSelectionScreen screen) screen.onClose();
    }

    public static void requestOpenSelection() {
        pendingOpen = true;
    }

    public static ResourceLocation classId() { return classId; }
    public static ResourceLocation birthsign() { return birthsign; }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        // Wait until the loading screen is gone, otherwise it would replace our screen.
        if (pendingOpen && mc.player != null && mc.screen == null) {
            pendingOpen = false;
            mc.setScreen(new ClassSelectionScreen());
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        classId = null;
        birthsign = null;
        pendingOpen = false;
    }

    /** Adds "Warrior · born under The Lord" to the skills screen header. */
    @SubscribeEvent
    public static void onSkillsScreen(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof SkillsScreen screen)) return;
        ClassDef def = ClassesData.CLASSES.getOrNull(classId);
        if (def == null) return;
        Birthsign sign = ClassesData.BIRTHSIGNS.getOrNull(birthsign);
        Component text = sign == null ? def.name().copy()
                : Component.translatable("screen.urclasses.class_line", def.name(), sign.name());
        GuiGraphics g = event.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        int panelW = Math.min(screen.width - 16, 440);
        int panelH = Math.min(screen.height - 16, 46 + 18 + 6 * 24 + 14);
        int left = (screen.width - panelW) / 2;
        int top = (screen.height - panelH) / 2;
        g.drawString(font, text, left + panelW - 10 - font.width(text), top + 9, UiKit.TEXT_DIM, false);
    }
}
