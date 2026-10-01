package com.untamedrealms.quests.client;

import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.quests.QuestsClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Skyrim-style compass across the top of the screen: cardinal directions scroll with the camera and
 * the tracked quest objective shows as a gold marker with its distance. Boss bars are pushed below it.
 */
public final class CompassHud {
    /** Half the bar width in pixels; one pixel per degree, so the bar spans 180 degrees of view. */
    private static final int HALF = 90;
    private static final int TOP = 3;
    private static final int HEIGHT = 11;
    /** How far boss bars are moved down to make room. */
    public static final int BOSS_OFFSET = 14;
    private static final String[] LABELS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    private CompassHud() {}

    public static boolean visible() {
        Minecraft mc = Minecraft.getInstance();
        return QuestsClientConfig.SHOW_COMPASS.get() && mc.player != null && !mc.options.hideGui;
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        if (!visible()) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Font font = mc.font;
        float yaw = player.getViewYRot(delta.getGameTimeDeltaPartialTick(true));
        int cx = g.guiWidth() / 2;

        g.fill(cx - HALF - 3, TOP, cx + HALF + 3, TOP + HEIGHT, 0x90101216);
        g.fill(cx - HALF - 3, TOP - 1, cx + HALF + 3, TOP, 0xA08C7853);
        g.fill(cx - HALF - 3, TOP + HEIGHT, cx + HALF + 3, TOP + HEIGHT + 1, 0xA08C7853);

        // Ticks every 15 degrees, letters every 45.
        for (int deg = 0; deg < 360; deg += 15) {
            float rel = Mth.wrapDegrees(deg - yaw);
            if (Math.abs(rel) > HALF) continue;
            int x = cx + Math.round(rel);
            int alpha = (int) (255 * (1f - Math.abs(rel) / (HALF + 10f)));
            if (deg % 45 == 0) {
                String label = LABELS[deg / 45];
                boolean cardinal = label.length() == 1;
                int color = label.equals("N") ? 0xE06A5A : cardinal ? 0xE6E1D3 : 0x9A958A;
                g.drawString(font, label, x - font.width(label) / 2, TOP + 2, (alpha << 24) | color, true);
            } else {
                g.fill(x, TOP + 4, x + 1, TOP + HEIGHT - 4, (alpha / 2 << 24) | 0x9A958A);
            }
        }

        BlockPos marker = ClientQuests.marker;
        if (marker != null) {
            double dx = marker.getX() + 0.5 - player.getX();
            double dz = marker.getZ() + 0.5 - player.getZ();
            float bearing = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float rel = Mth.wrapDegrees(bearing - yaw);
            boolean ahead = Math.abs(rel) <= HALF;
            int x = cx + Math.round(Mth.clamp(rel, -HALF, HALF));
            diamond(g, x, TOP + HEIGHT / 2, ahead ? UiKit.GOLD : 0xFF8C7853);
            // Distance sits just right of the bar, so the compass stays one line tall (Jade sits below it).
            int distance = (int) Math.sqrt(dx * dx + dz * dz);
            String text = distance < 1000 ? distance + "m" : String.format("%.1fkm", distance / 1000f);
            g.drawString(font, text, cx + HALF + 7, TOP + 2, ahead ? UiKit.GOLD : UiKit.TEXT_DIM, true);
        }
    }

    /** A 9px quest-marker diamond with a dark outline. */
    private static void diamond(GuiGraphics g, int x, int y, int color) {
        for (int i = -4; i <= 4; i++) {
            int w = 4 - Math.abs(i);
            g.fill(x - w - 1, y + i, x + w + 2, y + i + 1, 0xFF1A1D23);
        }
        for (int i = -3; i <= 3; i++) {
            int w = 3 - Math.abs(i);
            g.fill(x - w, y + i, x + w + 1, y + i + 1, color);
        }
    }
}
