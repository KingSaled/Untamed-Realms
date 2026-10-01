package com.untamedrealms.quests.client;

import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.quests.data.QuestDef;
import com.untamedrealms.quests.data.QuestLog;
import com.untamedrealms.quests.data.QuestsData;
import com.untamedrealms.quests.engine.QuestApi;
import com.untamedrealms.quests.network.QuestsNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The journal (J): active and completed quests on the left, the selected quest's details on the right. */
public class QuestJournalScreen extends Screen {
    private static final int LIST_W = 140;
    private static final int ROW_H = 14;

    private int left, top, panelW, panelH;
    private boolean showCompleted;
    private ResourceLocation selected;
    private double listScroll, detailScroll;
    private Button activeTab, completedTab, trackButton, abandonButton;

    public QuestJournalScreen() {
        super(Component.translatable("screen.urquests.journal"));
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 16, 440);
        panelH = Math.min(height - 16, 270);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        activeTab = addRenderableWidget(Button.builder(Component.translatable("screen.urquests.tab.active"), b -> { showCompleted = false; listScroll = 0; selectFirst(); })
                .bounds(left + 8, top + 20, LIST_W / 2 - 1, 16).build());
        completedTab = addRenderableWidget(Button.builder(Component.translatable("screen.urquests.tab.completed"), b -> { showCompleted = true; listScroll = 0; selectFirst(); })
                .bounds(left + 8 + LIST_W / 2 + 1, top + 20, LIST_W / 2 - 1, 16).build());
        trackButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            boolean tracked = selected != null && selected.equals(ClientQuests.log().tracked());
            PacketDistributor.sendToServer(new QuestsNetwork.Track(tracked ? Optional.empty() : Optional.ofNullable(selected)));
        }).bounds(left + panelW - 178, top + panelH - 24, 80, 18).build());
        abandonButton = addRenderableWidget(Button.builder(Component.translatable("screen.urquests.abandon"), b -> {
            if (selected != null) PacketDistributor.sendToServer(new QuestsNetwork.Abandon(selected));
        }).bounds(left + panelW - 92, top + panelH - 24, 84, 18).build());
        selected = ClientQuests.log().tracked();
        if (selected == null) selectFirst();
    }

    private List<ResourceLocation> entries() {
        QuestLog log = ClientQuests.log();
        List<ResourceLocation> list = new ArrayList<>(showCompleted ? log.completed().keySet() : log.active().keySet());
        // main quests first, then alphabetical by title
        list.sort((a, b) -> {
            QuestDef qa = QuestsData.get(a), qb = QuestsData.get(b);
            int ma = qa != null && qa.isMain() ? 0 : 1, mb = qb != null && qb.isMain() ? 0 : 1;
            if (ma != mb) return ma - mb;
            return a.compareTo(b);
        });
        return list;
    }

    private void selectFirst() {
        List<ResourceLocation> list = entries();
        selected = list.isEmpty() ? null : list.get(0);
        detailScroll = 0;
    }

    @Override
    public void tick() {
        activeTab.active = showCompleted;
        completedTab.active = !showCompleted;
        boolean isActive = selected != null && ClientQuests.log().isActive(selected);
        trackButton.visible = isActive;
        boolean tracked = selected != null && selected.equals(ClientQuests.log().tracked());
        trackButton.setMessage(Component.translatable(tracked ? "screen.urquests.untrack" : "screen.urquests.track"));
        QuestDef def = selected == null ? null : QuestsData.get(selected);
        abandonButton.visible = isActive && def != null && def.abandonable();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        UiKit.panel(g, left, top, panelW, panelH);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        UiKit.heading(g, font, title.copy().withStyle(ChatFormatting.BOLD), left, top + 7, panelW);
        QuestLog log = ClientQuests.log();

        // ---- list
        int lx = left + 8, ly = top + 40, lh = panelH - 48;
        UiKit.inset(g, lx, ly, LIST_W, lh);
        List<ResourceLocation> list = entries();
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, list.size() * ROW_H - lh + 4));
        g.enableScissor(lx + 1, ly + 1, lx + LIST_W - 1, ly + lh - 1);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = list.get(i);
            QuestDef def = QuestsData.get(id);
            int ry = ly + 2 + i * ROW_H - (int) listScroll;
            boolean sel = id.equals(selected);
            if (sel) g.fill(lx + 2, ry, lx + LIST_W - 2, ry + ROW_H - 1, UiKit.BG_SELECTED);
            else if (UiKit.inside(mouseX, mouseY, lx + 2, ry, LIST_W - 4, ROW_H - 1)) g.fill(lx + 2, ry, lx + LIST_W - 2, ry + ROW_H - 1, UiKit.BG_HOVER);
            Component name = def == null ? Component.literal(id.toString()) : def.title();
            int color = def != null && def.isMain() ? UiKit.GOLD : UiKit.TEXT;
            if (id.equals(log.tracked())) g.drawString(font, "▶", lx + 4, ry + 3, UiKit.GOLD, false);
            g.drawString(font, font.plainSubstrByWidth(name.getString(), LIST_W - 20), lx + 14, ry + 3, color, false);
        }
        g.disableScissor();
        if (list.isEmpty()) {
            g.drawCenteredString(font, Component.translatable(showCompleted ? "screen.urquests.none_completed" : "screen.urquests.none_active"),
                    lx + LIST_W / 2, ly + 10, UiKit.TEXT_DIM);
        }

        // ---- details
        int dx = lx + LIST_W + 10, dy = top + 22, dw = panelW - LIST_W - 28, dh = panelH - 22 - 30;
        QuestDef def = selected == null ? null : QuestsData.get(selected);
        if (def == null) return;
        g.enableScissor(dx, dy, dx + dw, dy + dh);
        int y = dy - (int) detailScroll;
        g.pose().pushPose();
        g.pose().translate(dx, y, 0);
        g.pose().scale(1.3f, 1.3f, 1f);
        g.drawString(font, def.title(), 0, 0, UiKit.GOLD, true);
        g.pose().popPose();
        y += 14;
        g.drawString(font, Component.translatable("quest_category.urquests." + def.category()), dx, y, UiKit.TEXT_DIM, false);
        y += 12;
        y += UiKit.wrapped(g, font, def.description(), dx, y, dw, UiKit.TEXT) + 6;

        QuestLog.Active active = log.active().get(selected);
        int shownStages = active == null ? def.stages().size() : Math.min(def.stages().size(), active.stage + 1);
        for (int s = 0; s < shownStages; s++) {
            QuestDef.Stage stage = def.stages().get(s);
            boolean current = active != null && s == active.stage;
            y += UiKit.wrapped(g, font, stage.description(), dx, y, dw, current ? UiKit.TEXT : UiKit.TEXT_DIM) + 2;
            if (!current) continue;
            for (int i = 0; i < stage.objectives().size(); i++) {
                QuestDef.Objective obj = stage.objectives().get(i);
                int progress = i < active.progress.length ? active.progress[i] : 0;
                boolean done = progress >= obj.count();
                Component text = QuestApi.describe(obj);
                if (obj.count() > 1) text = Component.empty().append(text).append(" (" + progress + "/" + obj.count() + ")");
                g.drawString(font, done ? "✔" : "◇", dx + 4, y, done ? UiKit.GOOD : UiKit.GOLD, false);
                y += UiKit.wrapped(g, font, text, dx + 14, y, dw - 14, done ? UiKit.TEXT_DIM : UiKit.TEXT);
            }
            y += 4;
        }

        QuestDef.Rewards rewards = def.rewards();
        if (rewards.coins() > 0 || !rewards.xp().isEmpty() || !rewards.items().isEmpty()) {
            y += 4;
            g.drawString(font, Component.translatable("screen.urquests.rewards"), dx, y, UiKit.GOLD, false);
            y += 11;
            if (rewards.coins() > 0) {
                g.drawString(font, Component.translatable("screen.urquests.reward_coins", rewards.coins()), dx + 4, y, UiKit.TEXT, false);
                y += 10;
            }
            for (var e : rewards.xp().entrySet()) {
                g.drawString(font, Component.translatable("screen.urquests.reward_xp", e.getValue(), e.getKey().displayName()), dx + 4, y, UiKit.TEXT, false);
                y += 10;
            }
            if (!rewards.items().isEmpty()) {
                g.drawString(font, Component.translatable("screen.urquests.reward_items", rewards.items().size()), dx + 4, y, UiKit.TEXT, false);
                y += 10;
            }
        }
        g.disableScissor();
        int contentH = y + (int) detailScroll - dy;
        detailScroll = Mth.clamp(detailScroll, 0, Math.max(0, contentH - dh));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        int lx = left + 8, ly = top + 40, lh = panelH - 48;
        if (UiKit.inside(mouseX, mouseY, lx, ly, LIST_W, lh)) {
            int index = (int) ((mouseY - ly - 2 + listScroll) / ROW_H);
            List<ResourceLocation> list = entries();
            if (index >= 0 && index < list.size()) {
                selected = list.get(index);
                detailScroll = 0;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < left + 8 + LIST_W) listScroll -= scrollY * ROW_H;
        else detailScroll -= scrollY * 12;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientQuests.OPEN_JOURNAL.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
