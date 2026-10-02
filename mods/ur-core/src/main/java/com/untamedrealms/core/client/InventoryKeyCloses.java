package com.untamedrealms.core.client;

import com.untamedrealms.core.UntamedCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * The inventory key (E) closes every Untamed Realms window, like it closes vanilla containers -
 * unless the window can't be closed with Esc either, or you are typing in a text box.
 */
@EventBusSubscriber(modid = UntamedCore.MODID, value = Dist.CLIENT)
public final class InventoryKeyCloses {
    private InventoryKeyCloses() {}

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        Screen screen = event.getScreen();
        if (screen instanceof TitleScreen || !screen.getClass().getName().startsWith("com.untamedrealms.")) return;
        if (!screen.shouldCloseOnEsc() || screen.getFocused() instanceof EditBox) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.keyInventory.matches(event.getKeyCode(), event.getScanCode())) {
            screen.onClose();
            event.setCanceled(true);
        }
    }
}
