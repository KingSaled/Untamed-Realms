package com.untamedrealms.npcs.client;

import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.npcs.network.NpcsNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Merchant screen: their wares on the left (click to buy, shift-click for 8), what they buy on the
 * right (click to sell one, shift-click to sell all). Prices already include Speech bonuses.
 */
public class ShopScreen extends Screen {
    private static final int ROW_H = 20;
    private NpcsNetwork.OpenShop data;
    private int left, top, panelW, panelH, colW;
    private double scrollBuy, scrollSell;

    public ShopScreen(NpcsNetwork.OpenShop data) {
        super(data.name());
        this.data = data;
    }

    public int entityId() {
        return data.entityId();
    }

    public void update(NpcsNetwork.OpenShop data) {
        this.data = data;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 16, 400);
        panelH = Math.min(height - 16, 240);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        colW = (panelW - 24) / 2;
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
        Component purse = Component.translatable("screen.urnpcs.purse", UiKit.compact(data.balance()));
        g.drawString(font, purse, left + panelW - 10 - font.width(purse), top + panelH - 14, UiKit.GOLD, true);
        g.drawString(font, Component.translatable("screen.urnpcs.hint"), left + 10, top + panelH - 14, UiKit.TEXT_DIM, false);

        ItemStack tooltip = null;
        int listTop = top + 34, listH = panelH - 54;
        int bx = left + 8, sx = left + 16 + colW;
        g.drawString(font, Component.translatable("screen.urnpcs.buy"), bx + 2, top + 22, UiKit.GOLD, false);
        g.drawString(font, Component.translatable("screen.urnpcs.sell"), sx + 2, top + 22, UiKit.GOLD, false);
        UiKit.inset(g, bx, listTop, colW, listH);
        UiKit.inset(g, sx, listTop, colW, listH);

        scrollBuy = Mth.clamp(scrollBuy, 0, Math.max(0, data.sells().size() * ROW_H - listH + 4));
        scrollSell = Mth.clamp(scrollSell, 0, Math.max(0, data.buys().size() * ROW_H - listH + 4));
        ItemStack t1 = drawList(g, data.sells(), bx, listTop, listH, scrollBuy, mouseX, mouseY, true);
        ItemStack t2 = drawList(g, data.buys(), sx, listTop, listH, scrollSell, mouseX, mouseY, false);
        tooltip = t1 != null ? t1 : t2;
        if (tooltip != null) g.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    private ItemStack drawList(GuiGraphics g, List<NpcsNetwork.Offer> offers, int x, int y, int h, double scroll, int mouseX, int mouseY, boolean buying) {
        ItemStack hovered = null;
        g.enableScissor(x + 1, y + 1, x + colW - 1, y + h - 1);
        for (int i = 0; i < offers.size(); i++) {
            NpcsNetwork.Offer offer = offers.get(i);
            int ry = y + 2 + i * ROW_H - (int) scroll;
            boolean hover = UiKit.inside(mouseX, mouseY, x + 2, ry, colW - 4, ROW_H - 2) && mouseY >= y && mouseY < y + h;
            if (hover) { g.fill(x + 2, ry, x + colW - 2, ry + ROW_H - 2, UiKit.BG_HOVER); hovered = offer.stack(); }
            g.renderItem(offer.stack(), x + 4, ry + 1);
            g.renderItemDecorations(font, offer.stack(), x + 4, ry + 1);
            String name = font.plainSubstrByWidth(offer.stack().getHoverName().getString(), colW - 70);
            g.drawString(font, name, x + 24, ry + 5, UiKit.TEXT, false);
            String price = offer.price() + "c";
            boolean affordable = !buying || data.balance() >= offer.price();
            g.drawString(font, price, x + colW - 6 - font.width(price), ry + 5, affordable ? UiKit.GOLD : UiKit.BAD, false);
        }
        g.disableScissor();
        return hovered;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listTop = top + 34, listH = panelH - 54;
        if (mouseY < listTop || mouseY >= listTop + listH) return super.mouseClicked(mouseX, mouseY, button);
        boolean shift = hasShiftDown();
        int bx = left + 8, sx = left + 16 + colW;
        if (UiKit.inside(mouseX, mouseY, bx, listTop, colW, listH)) {
            int i = (int) ((mouseY - listTop - 2 + scrollBuy) / ROW_H);
            if (i >= 0 && i < data.sells().size()) PacketDistributor.sendToServer(new NpcsNetwork.Trade(data.entityId(), true, i, shift ? 8 : 1));
            return true;
        }
        if (UiKit.inside(mouseX, mouseY, sx, listTop, colW, listH)) {
            int i = (int) ((mouseY - listTop - 2 + scrollSell) / ROW_H);
            if (i >= 0 && i < data.buys().size()) PacketDistributor.sendToServer(new NpcsNetwork.Trade(data.entityId(), false, i, shift ? -1 : 0));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < left + 8 + colW) scrollBuy -= scrollY * ROW_H;
        else scrollSell -= scrollY * ROW_H;
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
