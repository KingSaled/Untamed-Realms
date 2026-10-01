package com.untamedrealms.quests.client;

import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.quests.data.QuestDef;
import com.untamedrealms.quests.data.QuestLog;
import com.untamedrealms.quests.data.QuestsData;
import com.untamedrealms.quests.engine.QuestApi;
import com.untamedrealms.quests.engine.Targets;
import com.untamedrealms.quests.network.QuestsNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hand items in at a notice board. Left: your open deliveries. Right: the stacks in your inventory
 * that count for the selected one - left-click hands in the stack, right-click a single item.
 * Nothing is ever taken without you choosing it.
 */
public class DeliveryScreen extends Screen {
    private record Delivery(ResourceLocation quest, int index, QuestDef def, QuestDef.Objective objective, int progress) {}

    private static final int CELL = 20, COLS = 9;
    private final BlockPos board;
    private int left, top, panelW, panelH, listW;
    private int selected;

    public DeliveryScreen(BlockPos board) {
        super(Component.translatable("screen.urquests.deliver"));
        this.board = board;
    }

    /** Every unfinished deliver objective in the current stage of an active quest. */
    static List<Delivery> deliveries() {
        List<Delivery> out = new ArrayList<>();
        for (Map.Entry<ResourceLocation, QuestLog.Active> e : ClientQuests.log().active().entrySet()) {
            QuestDef def = QuestsData.get(e.getKey());
            if (def == null || e.getValue().stage >= def.stages().size()) continue;
            List<QuestDef.Objective> objectives = def.stages().get(e.getValue().stage).objectives();
            for (int i = 0; i < objectives.size(); i++) {
                QuestDef.Objective obj = objectives.get(i);
                int progress = i < e.getValue().progress.length ? e.getValue().progress[i] : 0;
                if (obj.type().equals(QuestApi.DELIVER) && progress < obj.count()) out.add(new Delivery(e.getKey(), i, def, obj, progress));
            }
        }
        return out;
    }

    public static boolean hasDeliveries() {
        return !deliveries().isEmpty();
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 16, 380);
        panelH = Math.min(height - 16, 200);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        listW = panelW - COLS * CELL - 30;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        UiKit.panel(g, left, top, panelW, panelH);
    }

    private List<Integer> matchingSlots(Delivery d) {
        List<Integer> slots = new ArrayList<>();
        var items = minecraft.player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty() && Targets.item(d.objective().target(), items.get(i))) slots.add(i);
        }
        return slots;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        UiKit.heading(g, font, title.copy().withStyle(ChatFormatting.BOLD), left, top + 7, panelW);
        List<Delivery> list = deliveries();
        if (list.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.urquests.deliver_none"), left + panelW / 2, top + panelH / 2, UiKit.TEXT_DIM);
            return;
        }
        selected = Math.min(selected, list.size() - 1);

        // Left: open deliveries
        int y = top + 24;
        for (int i = 0; i < list.size(); i++) {
            Delivery d = list.get(i);
            int x = left + 8;
            boolean hover = UiKit.inside(mouseX, mouseY, x, y, listW, 30);
            g.fill(x, y, x + listW, y + 30, i == selected ? UiKit.BG_SELECTED : hover ? UiKit.BG_HOVER : UiKit.BG_INNER);
            g.drawString(font, font.split(d.def().title(), listW - 8).get(0), x + 4, y + 4, UiKit.GOLD, false);
            Component line = Component.empty().append(QuestApi.describe(d.objective())).append(" " + d.progress() + "/" + d.objective().count());
            g.drawString(font, font.split(line, listW - 8).get(0), x + 4, y + 17, UiKit.TEXT, false);
            y += 33;
        }

        // Right: matching stacks in the inventory
        Delivery d = list.get(selected);
        int gx = left + listW + 18, gy = top + 24;
        g.drawString(font, Component.translatable("screen.urquests.deliver_pick"), gx, gy, UiKit.TEXT_DIM, false);
        gy += 12;
        List<Integer> slots = matchingSlots(d);
        if (slots.isEmpty()) {
            g.drawString(font, Component.translatable("screen.urquests.deliver_nothing"), gx, gy + 4, UiKit.TEXT_DIM, false);
        }
        ItemStack hovered = ItemStack.EMPTY;
        for (int i = 0; i < slots.size(); i++) {
            int x = gx + (i % COLS) * CELL, yy = gy + (i / COLS) * CELL;
            ItemStack stack = minecraft.player.getInventory().items.get(slots.get(i));
            boolean hover = UiKit.inside(mouseX, mouseY, x, yy, CELL - 2, CELL - 2);
            UiKit.inset(g, x, yy, CELL - 2, CELL - 2);
            if (hover) {
                g.fill(x + 1, yy + 1, x + CELL - 3, yy + CELL - 3, UiKit.BG_HOVER);
                hovered = stack;
            }
            g.renderItem(stack, x + 1, yy + 1);
            g.renderItemDecorations(font, stack, x + 1, yy + 1);
        }
        g.drawString(font, Component.translatable("screen.urquests.deliver_hint"), gx, top + panelH - 14, UiKit.TEXT_DIM, false);
        if (!hovered.isEmpty()) g.renderTooltip(font, hovered, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Delivery> list = deliveries();
        int y = top + 24;
        for (int i = 0; i < list.size(); i++) {
            if (UiKit.inside(mouseX, mouseY, left + 8, y, listW, 30)) {
                selected = i;
                return true;
            }
            y += 33;
        }
        if (!list.isEmpty()) {
            Delivery d = list.get(Math.min(selected, list.size() - 1));
            int gx = left + listW + 18, gy = top + 36;
            List<Integer> slots = matchingSlots(d);
            for (int i = 0; i < slots.size(); i++) {
                int x = gx + (i % COLS) * CELL, yy = gy + (i / COLS) * CELL;
                if (UiKit.inside(mouseX, mouseY, x, yy, CELL - 2, CELL - 2)) {
                    int amount = button == 1 ? 1 : minecraft.player.getInventory().items.get(slots.get(i)).getCount();
                    PacketDistributor.sendToServer(new QuestsNetwork.Deliver(board, d.quest(), d.index(), slots.get(i), amount));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
