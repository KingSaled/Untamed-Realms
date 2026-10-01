package com.untamedrealms.magic.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.magic.UntamedMagic;
import com.untamedrealms.magic.data.SpellBook;
import com.untamedrealms.magic.data.SpellDef;
import com.untamedrealms.magic.network.MagicNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Hold the wheel key, point the mouse at a spell, let go: that spell is readied. Clicking a spell
 * also readies it. Shows the eight quick slots (assigned in the spellbook).
 */
public class SpellWheelScreen extends Screen {
    private static final int RADIUS = 72;
    private static final int CELL = 26;
    private final KeyMapping holdKey;
    private int hovered = -1;
    private boolean closing;

    public SpellWheelScreen(KeyMapping holdKey) {
        super(Component.translatable("screen.urmagic.wheel"));
        this.holdKey = holdKey;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int slotX(int i) {
        double angle = -Math.PI / 2 + i * 2 * Math.PI / SpellBook.SLOTS;
        return width / 2 + (int) Math.round(Math.cos(angle) * RADIUS);
    }

    private int slotY(int i) {
        double angle = -Math.PI / 2 + i * 2 * Math.PI / SpellBook.SLOTS;
        return height / 2 + (int) Math.round(Math.sin(angle) * RADIUS);
    }

    /** The slot whose direction the mouse points in (anywhere outside the centre). */
    private int slotAt(double mouseX, double mouseY) {
        double dx = mouseX - width / 2.0, dy = mouseY - height / 2.0;
        if (dx * dx + dy * dy < 22 * 22) return -1;
        double angle = Math.atan2(dy, dx) + Math.PI / 2;
        double step = 2 * Math.PI / SpellBook.SLOTS;
        return Math.floorMod((int) Math.round(angle / step), SpellBook.SLOTS);
    }

    @Override
    public void tick() {
        if (!closing && !isHeld()) choose(hovered);
    }

    /** Whether the wheel key is still physically held down. */
    private boolean isHeld() {
        InputConstants.Key key = holdKey.getKey();
        long window = minecraft.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        return key.getValue() != InputConstants.UNKNOWN.getValue() && InputConstants.isKeyDown(window, key.getValue());
    }

    private void choose(int slot) {
        closing = true;
        if (slot >= 0 && ClientMagic.BOOK.slot(slot) != null && slot != ClientMagic.BOOK.selected()) {
            PacketDistributor.sendToServer(new MagicNetwork.Select(slot, false));
        }
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        choose(slotAt(mouseX, mouseY));
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Keep the world visible; only a light vignette behind the wheel.
        g.fillGradient(0, 0, width, height, 0x40000000, 0x60000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        SpellBook book = ClientMagic.BOOK;
        hovered = slotAt(mouseX, mouseY);
        int cx = width / 2, cy = height / 2;

        for (int i = 0; i < SpellBook.SLOTS; i++) {
            int x = slotX(i) - CELL / 2, y = slotY(i) - CELL / 2;
            ResourceLocation id = book.slot(i);
            SpellDef def = id == null ? null : UntamedMagic.SPELLS.getOrNull(id);
            boolean hover = i == hovered;
            g.fill(x - 1, y - 1, x + CELL + 1, y + CELL + 1, 0xFF000000);
            g.fill(x, y, x + CELL, y + CELL, hover ? UiKit.BG_HOVER : i == book.selected() ? UiKit.BG_SELECTED : UiKit.BG_INNER);
            int frame = def == null ? UiKit.TRIM_DARK : hover ? UiKit.GOLD : ClientMagic.schoolColor(def.school());
            UiKit.frame(g, x, y, CELL, CELL, frame);
            if (def != null) {
                g.pose().pushPose();
                g.pose().translate(x + 1, y + 1, 0);
                g.pose().scale(1.5f, 1.5f, 1f);
                g.blit(ClientMagic.icon(id), 0, 0, 0, 0, 16, 16, 16, 16);
                g.pose().popPose();
            }
            String number = String.valueOf(i + 1);
            g.drawString(font, number, x + CELL - font.width(number) - 1, y + CELL - 8, UiKit.TEXT_DIM, true);
        }

        // Centre: the hovered (or readied) spell's details.
        int show = hovered >= 0 ? hovered : book.selected();
        ResourceLocation id = book.slot(show);
        SpellDef def = id == null ? null : UntamedMagic.SPELLS.getOrNull(id);
        if (def != null) {
            g.drawCenteredString(font, def.name().copy().withStyle(ChatFormatting.BOLD), cx, cy - 10, ClientMagic.schoolColor(def.school()));
            g.drawCenteredString(font, Component.translatable("screen.urmagic.wheel.cost", Math.round(def.cost())), cx, cy + 2, UiKit.MAGICKA);
        } else {
            g.drawCenteredString(font, Component.translatable("screen.urmagic.wheel.empty"), cx, cy - 4, UiKit.TEXT_DIM);
        }
        Component hint = Component.translatable("screen.urmagic.wheel.hint", ClientMagic.SPELLBOOK.getTranslatedKeyMessage());
        g.drawCenteredString(font, hint, cx, cy + RADIUS + CELL / 2 + 8, UiKit.TEXT_DIM);
    }
}
