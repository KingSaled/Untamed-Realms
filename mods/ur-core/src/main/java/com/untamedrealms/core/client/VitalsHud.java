package com.untamedrealms.core.client;

import com.untamedrealms.core.CoreClientConfig;
import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.core.registry.CoreAttributes;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/**
 * Magicka (bottom-left) and Stamina (bottom-right) bars. Like Skyrim they fade out while full and
 * fade back in as soon as they are used. The stamina bar pulses red while exhausted.
 */
public final class VitalsHud {
    private static final int BAR_W = 82;
    private static final int BAR_H = 4;
    private static float magickaAlpha;
    private static float staminaAlpha;

    private VitalsHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || !CoreClientConfig.SHOW_VITALS.get()) return;
        if (player.isSpectator() || ClientCoreState.magicka() < 0) return;

        float maxMagicka = (float) player.getAttributeValue(CoreAttributes.MAX_MAGICKA);
        float maxStamina = (float) player.getAttributeValue(CoreAttributes.MAX_STAMINA);
        float magicka = maxMagicka <= 0 ? 0 : ClientCoreState.magicka() / maxMagicka;
        float stamina = maxStamina <= 0 ? 0 : ClientCoreState.stamina() / maxStamina;
        boolean exhausted = ClientCoreState.exhausted();
        boolean hide = CoreClientConfig.HIDE_FULL_VITALS.get();

        magickaAlpha = approach(magickaAlpha, !hide || magicka < 0.999f ? 1f : 0f);
        staminaAlpha = approach(staminaAlpha, !hide || stamina < 0.999f || exhausted ? 1f : 0f);

        int y = g.guiHeight() - 10;
        if (magickaAlpha > 0.02f) {
            drawBar(g, 8, y, magicka, UiKit.MAGICKA, UiKit.MAGICKA_DARK, magickaAlpha);
        }
        if (staminaAlpha > 0.02f) {
            int color = UiKit.STAMINA;
            int dark = UiKit.STAMINA_DARK;
            if (exhausted && (player.tickCount / 5) % 2 == 0) {
                color = UiKit.HEALTH;
                dark = UiKit.HEALTH_DARK;
            }
            drawBar(g, g.guiWidth() - 8 - BAR_W, y, stamina, color, dark, staminaAlpha);
        }
    }

    private static float approach(float current, float target) {
        float step = 0.06f;
        if (current < target) return Math.min(target, current + step * 2);
        return Math.max(target, current - step);
    }

    private static void drawBar(GuiGraphics g, int x, int y, float fraction, int color, int dark, float alpha) {
        int a = (int) (alpha * 255) << 24;
        g.fill(x - 2, y - 2, x + BAR_W + 2, y + BAR_H + 2, a | 0x000000);
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, a | (UiKit.TRIM_DARK & 0xFFFFFF));
        g.fill(x, y, x + BAR_W, y + BAR_H, a | 0x151515);
        int filled = Math.round(BAR_W * Math.max(0, Math.min(1, fraction)));
        // Skyrim bars shrink toward the centre; ours drain from the outer edge for readability.
        if (filled > 0) {
            g.fill(x, y, x + filled, y + BAR_H, a | (dark & 0xFFFFFF));
            g.fill(x, y, x + filled, y + BAR_H / 2, a | (color & 0xFFFFFF));
        }
    }
}
