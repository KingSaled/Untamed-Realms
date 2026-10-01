package com.untamedrealms.quests.client;

import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.quests.data.QuestDef;
import com.untamedrealms.quests.data.QuestsData;
import com.untamedrealms.quests.engine.QuestApi;
import com.untamedrealms.quests.network.QuestsNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Today's bounties pinned to a notice board, each as a parchment card with an Accept button. */
public class NoticeBoardScreen extends Screen {
    private final BlockPos pos;
    private final List<ResourceLocation> offers;
    private int left, top, panelW, panelH;

    public NoticeBoardScreen(BlockPos pos, List<ResourceLocation> offers) {
        super(Component.translatable("screen.urquests.board"));
        this.pos = pos;
        this.offers = offers;
    }

    @Override
    protected void init() {
        int cards = Math.max(1, offers.size());
        panelW = Math.min(width - 16, 24 + cards * 128);
        panelH = Math.min(height - 16, 200);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        int cardW = (panelW - 16 - (cards - 1) * 8) / cards;
        if (DeliveryScreen.hasDeliveries()) {
            addRenderableWidget(Button.builder(Component.translatable("screen.urquests.hand_in"), b -> minecraft.setScreen(new DeliveryScreen(pos)))
                    .bounds(left + panelW - 78, top + 4, 70, 14).build());
        }
        for (int i = 0; i < offers.size(); i++) {
            ResourceLocation id = offers.get(i);
            int cx = left + 8 + i * (cardW + 8);
            Button accept = Button.builder(Component.translatable("screen.urquests.accept"), b -> {
                PacketDistributor.sendToServer(new QuestsNetwork.AcceptBounty(pos, id));
                onClose();
            }).bounds(cx + 8, top + panelH - 30, cardW - 16, 18).build();
            accept.active = !ClientQuests.log().isActive(id);
            addRenderableWidget(accept);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        UiKit.panel(g, left, top, panelW, panelH);
        int cards = Math.max(1, offers.size());
        int cardW = (panelW - 16 - (cards - 1) * 8) / cards;
        for (int i = 0; i < offers.size(); i++) {
            int cx = left + 8 + i * (cardW + 8), cy = top + 22;
            g.fill(cx, cy, cx + cardW, top + panelH - 8, 0xFFE3D3A8);
            UiKit.frame(g, cx, cy, cardW, panelH - 30, 0xFF8A6F45);
            g.fill(cx + cardW / 2 - 2, cy + 2, cx + cardW / 2 + 2, cy + 6, 0xFF8C1D1D); // pin
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        UiKit.heading(g, font, title.copy().withStyle(ChatFormatting.BOLD), left, top + 7, panelW);
        if (offers.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.urquests.board_empty"), left + panelW / 2, top + panelH / 2, UiKit.TEXT_DIM);
            return;
        }
        int cards = offers.size();
        int cardW = (panelW - 16 - (cards - 1) * 8) / cards;
        int ink = 0xFF3A2A1E;
        for (int i = 0; i < offers.size(); i++) {
            QuestDef def = QuestsData.get(offers.get(i));
            if (def == null) continue;
            int cx = left + 8 + i * (cardW + 8) + 6, cy = top + 32, w = cardW - 12;
            cy += UiKit.wrapped(g, font, def.title().copy().withStyle(ChatFormatting.BOLD), cx, cy, w, ink) + 4;
            if (!def.stages().isEmpty()) {
                for (QuestDef.Objective obj : def.stages().get(0).objectives()) {
                    Component text = QuestApi.describe(obj);
                    if (obj.count() > 1) text = Component.empty().append(text).append(" x" + obj.count());
                    cy += UiKit.wrapped(g, font, Component.literal("- ").append(text), cx, cy, w, ink);
                }
            }
            cy += 4;
            if (def.rewards().coins() > 0) {
                g.drawString(font, Component.translatable("screen.urquests.reward_coins", def.rewards().coins()), cx, cy, 0xFF6B4A06, false);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
