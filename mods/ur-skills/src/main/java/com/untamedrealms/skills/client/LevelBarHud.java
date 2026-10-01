package com.untamedrealms.skills.client;

import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.skills.api.SkillMath;
import com.untamedrealms.skills.data.SkillData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Replaces vanilla's experience bar with the character level: progress to the next level and the
 * level number above it. Vanilla experience (spent at the enchanting table) shows as a small purple
 * number at the bar's right end.
 */
public final class LevelBarHud {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("urcore", "hud/level_bar_background");
    private static final ResourceLocation PROGRESS = ResourceLocation.fromNamespaceAndPath("urcore", "hud/level_bar_progress");
    private static final int ENCHANT_COLOR = 0xFFB98AF0;

    private LevelBarHud() {}

    /** Our bar shows whenever vanilla's would. */
    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.gameMode != null && mc.gameMode.hasExperience() && !mc.options.hideGui
                && mc.player.jumpableVehicle() == null;
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        if (!active()) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        SkillData data = ClientSkills.data();
        int x = g.guiWidth() / 2 - 91;
        int y = g.guiHeight() - 32 + 3;
        g.blitSprite(BACKGROUND, x, y, 182, 5);
        float progress = Math.min(1f, data.characterXp() / (float) Math.max(1, SkillMath.characterXpToNext(data.characterLevel())));
        int filled = Math.round(progress * 182);
        if (filled > 0) g.blitSprite(PROGRESS, 182, 5, 0, 0, x, y, filled, 5);

        String level = String.valueOf(data.characterLevel());
        int lx = g.guiWidth() / 2 - font.width(level) / 2;
        int ly = g.guiHeight() - 31 - 4;
        g.drawString(font, level, lx + 1, ly, 0xFF000000, false);
        g.drawString(font, level, lx - 1, ly, 0xFF000000, false);
        g.drawString(font, level, lx, ly + 1, 0xFF000000, false);
        g.drawString(font, level, lx, ly - 1, 0xFF000000, false);
        g.drawString(font, level, lx, ly, UiKit.GOLD, false);

        int enchant = mc.player.experienceLevel;
        if (enchant > 0) {
            String text = "✦" + enchant;
            g.drawString(font, text, x + 182 - font.width(text), ly, ENCHANT_COLOR, true);
        }
    }
}
