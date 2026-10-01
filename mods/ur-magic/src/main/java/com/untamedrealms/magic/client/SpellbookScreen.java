package com.untamedrealms.magic.client;

import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.magic.UntamedMagic;
import com.untamedrealms.magic.data.SpellBook;
import com.untamedrealms.magic.data.SpellDef;
import com.untamedrealms.magic.network.MagicNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Known spells grouped by school; click a quick slot, then a spell to ready it. Right-click clears a slot. */
public class SpellbookScreen extends Screen {
    private static final int CELL = 24;
    private int left, top, panelW, panelH;
    private int activeSlot;

    public SpellbookScreen() {
        super(Component.translatable("screen.urmagic.spellbook"));
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 16, 300);
        panelH = Math.min(height - 16, 230);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        activeSlot = ClientMagic.BOOK.selected();
    }

    private List<ResourceLocation> spells() {
        List<ResourceLocation> list = new ArrayList<>(ClientMagic.BOOK.known());
        list.removeIf(id -> UntamedMagic.SPELLS.getOrNull(id) == null);
        list.sort(Comparator.comparing((ResourceLocation id) -> UntamedMagic.SPELLS.getOrNull(id).school().ordinal())
                .thenComparing(id -> UntamedMagic.SPELLS.getOrNull(id).level()));
        return list;
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
        SpellBook book = ClientMagic.BOOK;
        List<Component> tooltip = null;

        int sx = left + panelW / 2 - (SpellBook.SLOTS * CELL) / 2;
        int sy = top + 22;
        for (int i = 0; i < SpellBook.SLOTS; i++) {
            int x = sx + i * CELL;
            g.fill(x, sy, x + CELL - 2, sy + CELL - 2, i == activeSlot ? UiKit.BG_SELECTED : UiKit.BG_INNER);
            UiKit.frame(g, x, sy, CELL - 2, CELL - 2, i == activeSlot ? UiKit.GOLD : UiKit.TRIM_DARK);
            ResourceLocation id = book.slot(i);
            if (id != null) g.blit(ClientMagic.icon(id), x + 3, sy + 3, 0, 0, 16, 16, 16, 16);
            g.drawString(font, String.valueOf(i + 1), x + 1, sy + 1, UiKit.TEXT_DIM, false);
        }
        g.drawCenteredString(font, Component.translatable("screen.urmagic.hint"), left + panelW / 2, sy + CELL + 2, UiKit.TEXT_DIM);

        List<ResourceLocation> spells = spells();
        int gx = left + 10, gy = sy + CELL + 16;
        int perRow = (panelW - 20) / CELL;
        for (int i = 0; i < spells.size(); i++) {
            ResourceLocation id = spells.get(i);
            SpellDef def = UntamedMagic.SPELLS.getOrNull(id);
            int x = gx + (i % perRow) * CELL, y = gy + (i / perRow) * CELL;
            boolean hover = UiKit.inside(mouseX, mouseY, x, y, CELL - 2, CELL - 2);
            g.fill(x, y, x + CELL - 2, y + CELL - 2, hover ? UiKit.BG_HOVER : UiKit.BG_INNER);
            UiKit.frame(g, x, y, CELL - 2, CELL - 2, ClientMagic.schoolColor(def.school()));
            g.blit(ClientMagic.icon(id), x + 3, y + 3, 0, 0, 16, 16, 16, 16);
            if (hover) {
                tooltip = List.of(def.name().copy().withStyle(ChatFormatting.GOLD),
                        Component.translatable("tooltip.urmagic.school", def.school().displayName(), def.level()).withStyle(ChatFormatting.AQUA),
                        def.description().copy().withStyle(ChatFormatting.GRAY),
                        Component.translatable("tooltip.urmagic.cost", Math.round(def.cost())).withStyle(ChatFormatting.BLUE));
            }
        }
        if (spells.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.urmagic.none"), left + panelW / 2, gy + 20, UiKit.TEXT_DIM);
        }
        if (tooltip != null) g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int sx = left + panelW / 2 - (SpellBook.SLOTS * CELL) / 2, sy = top + 22;
        for (int i = 0; i < SpellBook.SLOTS; i++) {
            if (UiKit.inside(mouseX, mouseY, sx + i * CELL, sy, CELL - 2, CELL - 2)) {
                if (button == 1) PacketDistributor.sendToServer(new MagicNetwork.SetSlot(i, Optional.empty()));
                else {
                    activeSlot = i;
                    PacketDistributor.sendToServer(new MagicNetwork.Select(i, false));
                }
                return true;
            }
        }
        List<ResourceLocation> spells = spells();
        int gx = left + 10, gy = sy + CELL + 16, perRow = (panelW - 20) / CELL;
        for (int i = 0; i < spells.size(); i++) {
            int x = gx + (i % perRow) * CELL, y = gy + (i / perRow) * CELL;
            if (UiKit.inside(mouseX, mouseY, x, y, CELL - 2, CELL - 2)) {
                PacketDistributor.sendToServer(new MagicNetwork.SetSlot(activeSlot, Optional.of(spells.get(i))));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientMagic.SPELLBOOK.matches(keyCode, scanCode)) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
