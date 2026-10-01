package com.untamedrealms.quests.client;

import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.quests.data.QuestDef;
import com.untamedrealms.quests.data.QuestLog;
import com.untamedrealms.quests.data.QuestsData;
import com.untamedrealms.quests.engine.QuestApi;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Top-right objective tracker for the tracked quest (Skyrim's quest objective list). */
public final class QuestTrackerHud {
    private static final int WIDTH = 150;

    private QuestTrackerHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !ClientQuests.trackerVisible || mc.screen != null || mc.player == null) return;
        QuestLog log = ClientQuests.log();
        ResourceLocation id = log.tracked();
        if (id == null) return;
        QuestLog.Active active = log.active().get(id);
        QuestDef def = QuestsData.get(id);
        if (active == null || def == null || active.stage >= def.stages().size()) return;

        Font font = mc.font;
        int x = g.guiWidth() - WIDTH - 6;
        int y = 6 + (mc.player.getActiveEffects().isEmpty() ? 0 : 26);
        QuestDef.Stage stage = def.stages().get(active.stage);

        List<FormattedCharSequence> title = font.split(def.title(), WIDTH - 8);
        int height = 6 + title.size() * 10 + 2;
        for (int i = 0; i < stage.objectives().size(); i++) height += font.split(QuestApi.describe(stage.objectives().get(i)), WIDTH - 20).size() * 10;
        g.fill(x, y, x + WIDTH, y + height + 2, 0x70101216);
        g.fill(x, y, x + 1, y + height + 2, UiKit.TRIM);

        int yy = y + 4;
        for (FormattedCharSequence line : title) {
            g.drawString(font, line, x + 5, yy, UiKit.GOLD, true);
            yy += 10;
        }
        yy += 2;
        for (int i = 0; i < stage.objectives().size(); i++) {
            QuestDef.Objective obj = stage.objectives().get(i);
            int progress = i < active.progress.length ? active.progress[i] : 0;
            boolean done = progress >= obj.count();
            g.drawString(font, done ? "✔" : "◇", x + 5, yy, done ? UiKit.GOOD : UiKit.TEXT_DIM, false);
            Component text = QuestApi.describe(obj);
            if (obj.count() > 1) text = Component.empty().append(text).append(Component.literal(" " + progress + "/" + obj.count()));
            for (FormattedCharSequence line : font.split(text, WIDTH - 20)) {
                g.drawString(font, line, x + 15, yy, done ? UiKit.TEXT_DIM : UiKit.TEXT, false);
                yy += 10;
            }
        }
    }
}
