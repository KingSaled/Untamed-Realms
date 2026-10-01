package com.untamedrealms.core.client;

import com.untamedrealms.core.CoreClientConfig;
import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.core.network.CoreNetwork;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Skyrim-style centred banners ("Mining Increased to 12", "Quest Started: ...") plus short status
 * lines and warnings. Banners queue and play one at a time; status lines stack.
 */
public final class NotificationHud {
    private static final int BANNER_TICKS = 70;
    private static final int LINE_TICKS = 60;
    private static final int WARNING_TICKS = 40;
    private static final int FADE = 10;

    private record Entry(Component title, Component subtitle, int duration) {}

    private static final Deque<Entry> BANNERS = new ArrayDeque<>();
    private static Entry currentBanner;
    private static int bannerAge;

    private static final List<long[]> LINE_AGES = new ArrayList<>();
    private static final List<Component> LINES = new ArrayList<>();

    private static Component warning;
    private static int warningAge;

    private NotificationHud() {}

    public static void push(int style, Component title, Component subtitle) {
        if (!CoreClientConfig.SHOW_NOTIFICATIONS.get() && style != CoreNetwork.STYLE_WARNING) return;
        switch (style) {
            case CoreNetwork.STYLE_BANNER -> {
                if (BANNERS.size() < 8) BANNERS.add(new Entry(title, subtitle, BANNER_TICKS));
            }
            case CoreNetwork.STYLE_SUBTLE -> {
                LINES.add(title);
                LINE_AGES.add(new long[]{0});
                while (LINES.size() > 5) { LINES.remove(0); LINE_AGES.remove(0); }
            }
            default -> {
                if (warning == null || !warning.getString().equals(title.getString()) || warningAge > 20) {
                    warning = title;
                    warningAge = 0;
                }
            }
        }
    }

    /** Called every client tick. */
    public static void tick() {
        if (currentBanner == null && !BANNERS.isEmpty()) {
            currentBanner = BANNERS.poll();
            bannerAge = 0;
        }
        if (currentBanner != null && ++bannerAge > currentBanner.duration()) {
            currentBanner = null;
        }
        Iterator<long[]> ages = LINE_AGES.iterator();
        Iterator<Component> lines = LINES.iterator();
        while (ages.hasNext()) {
            long[] age = ages.next();
            lines.next();
            if (++age[0] > LINE_TICKS) { ages.remove(); lines.remove(); }
        }
        if (warning != null && ++warningAge > WARNING_TICKS) warning = null;
    }

    public static void clear() {
        BANNERS.clear();
        currentBanner = null;
        LINES.clear();
        LINE_AGES.clear();
        warning = null;
    }

    private static int alpha(int age, int duration, float partial) {
        float t = age + partial;
        float a = Math.min(1f, Math.min(t / FADE, (duration - t) / FADE));
        return Mth.clamp((int) (a * 255), 0, 255);
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        Font font = mc.font;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        int width = g.guiWidth();
        int baseY = g.guiHeight() / 5;

        if (currentBanner != null) {
            int a = alpha(bannerAge, currentBanner.duration(), partial);
            if (a > 8) {
                g.pose().pushPose();
                g.pose().translate(width / 2f, baseY, 0);
                g.pose().scale(1.6f, 1.6f, 1f);
                int tw = font.width(currentBanner.title());
                g.drawString(font, currentBanner.title(), -tw / 2, 0, (a << 24) | (UiKit.TEXT & 0xFFFFFF), true);
                g.pose().popPose();
                int lineW = Math.max(tw * 2, 120);
                int lineY = baseY + 17;
                g.fill(width / 2 - lineW / 2, lineY, width / 2 + lineW / 2, lineY + 1, (a << 24) | (UiKit.TRIM & 0xFFFFFF));
                if (!currentBanner.subtitle().getString().isEmpty()) {
                    int sw = font.width(currentBanner.subtitle());
                    g.drawString(font, currentBanner.subtitle(), width / 2 - sw / 2, lineY + 5, (a << 24) | (UiKit.GOLD & 0xFFFFFF), true);
                }
            }
        }

        int y = baseY + 36;
        for (int i = 0; i < LINES.size(); i++) {
            int a = alpha((int) LINE_AGES.get(i)[0], LINE_TICKS, partial);
            if (a > 8) {
                Component line = LINES.get(i);
                int lw = font.width(line);
                g.drawString(font, line, width / 2 - lw / 2, y, (a << 24) | (UiKit.TEXT & 0xFFFFFF), true);
            }
            y += font.lineHeight + 2;
        }

        if (warning != null) {
            int a = alpha(warningAge, WARNING_TICKS, partial);
            if (a > 8) {
                int ww = font.width(warning);
                g.drawString(font, warning, width / 2 - ww / 2, g.guiHeight() - 72, (a << 24) | (UiKit.BAD & 0xFFFFFF), true);
            }
        }
    }
}
