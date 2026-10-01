package com.untamedrealms.core.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Shared look-and-feel for every Untamed Realms screen: dark translucent panels with bronze trim and
 * gold headings, loosely modelled on Skyrim's menus. Drawing is done with flat fills so screens need
 * no texture atlases and scale cleanly at any GUI scale.
 */
public final class UiKit {
    public static final int BG = 0xE6101216;
    public static final int BG_INNER = 0xCC1A1D23;
    public static final int BG_HOVER = 0xCC2A2F38;
    public static final int BG_SELECTED = 0xCC3A3324;
    public static final int TRIM = 0xFF8C7853;
    public static final int TRIM_DARK = 0xFF4A3F2C;
    public static final int GOLD = 0xFFE8C872;
    public static final int TEXT = 0xFFE6E1D3;
    public static final int TEXT_DIM = 0xFF9A958A;
    public static final int GOOD = 0xFF7FD27F;
    public static final int BAD = 0xFFE06A5A;
    public static final int MAGICKA = 0xFF3C7FE0;
    public static final int MAGICKA_DARK = 0xFF1D4280;
    public static final int STAMINA = 0xFF4FC24F;
    public static final int STAMINA_DARK = 0xFF1F6B2A;
    public static final int HEALTH = 0xFFC93C3C;
    public static final int HEALTH_DARK = 0xFF6B1A1A;
    public static final int XP = 0xFFD9B44A;
    public static final int XP_DARK = 0xFF6B5420;

    private UiKit() {}

    /** Main framed panel. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BG);
        frame(g, x, y, w, h, TRIM);
        frame(g, x + 2, y + 2, w - 4, h - 4, TRIM_DARK);
    }

    /** Inset sub-panel. */
    public static void inset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BG_INNER);
        frame(g, x, y, w, h, TRIM_DARK);
    }

    public static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** Horizontal rule with a gold title in the middle, e.g. ——— SKILLS ———. */
    public static void heading(GuiGraphics g, Font font, Component text, int x, int y, int w) {
        int tw = font.width(text);
        int cx = x + w / 2;
        int lineY = y + font.lineHeight / 2;
        g.fill(x + 4, lineY, cx - tw / 2 - 6, lineY + 1, TRIM);
        g.fill(cx + tw / 2 + 6, lineY, x + w - 4, lineY + 1, TRIM);
        g.drawString(font, text, cx - tw / 2, y, GOLD, true);
    }

    /** A two-tone progress bar with a dark frame. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, float fraction, int color, int darkColor) {
        fraction = Math.max(0, Math.min(1, fraction));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, 0xFF202020);
        int filled = Math.round(w * fraction);
        if (filled > 0) {
            g.fill(x, y, x + filled, y + h, darkColor);
            g.fill(x, y, x + filled, y + Math.max(1, h / 2), color);
        }
    }

    /** Draws word-wrapped text and returns the height used. */
    public static int wrapped(GuiGraphics g, Font font, FormattedText text, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = font.split(text, width);
        int yy = y;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, x, yy, color, false);
            yy += font.lineHeight + 1;
        }
        return yy - y;
    }

    public static int wrappedHeight(Font font, FormattedText text, int width) {
        return font.split(text, width).size() * (font.lineHeight + 1);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    /** Formats large numbers compactly: 1234 -> "1,234", 2500000 -> "2.5M". */
    public static String compact(long value) {
        if (value >= 10_000_000) return String.format("%.1fM", value / 1_000_000.0);
        if (value >= 100_000) return String.format("%dK", value / 1000);
        return String.format("%,d", value);
    }
}
