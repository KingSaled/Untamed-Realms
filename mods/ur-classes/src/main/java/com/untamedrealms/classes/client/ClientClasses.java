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

    /** "Warrior · born under The Lord", shown under the skills screen title. */
    public static Component classLine() {
        ClassDef def = ClassesData.CLASSES.getOrNull(classId);
        if (def == null) return null;
        Birthsign sign = ClassesData.BIRTHSIGNS.getOrNull(birthsign);
        return sign == null ? def.name().copy() : Component.translatable("screen.urclasses.class_line", def.name(), sign.name());
    }

    static {
        SkillsScreen.subtitle = ClientClasses::classLine;
    }
}
