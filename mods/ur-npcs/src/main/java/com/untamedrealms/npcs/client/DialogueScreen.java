package com.untamedrealms.npcs.client;

import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.npcs.network.NpcsNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Skyrim-style conversation in a panel at the bottom of the screen: the NPC's line, then the
 * player's responses.
 * Number keys 1-9 pick responses; Esc says goodbye.
 */
public class DialogueScreen extends Screen {
    private final NpcsNetwork.OpenDialogue data;
    private int boxX, boxY, boxW, optionsY;
    private int hovered = -1;

    public DialogueScreen(NpcsNetwork.OpenDialogue data) {
        super(data.name());
        this.data = data;
    }

    @Override
    protected void init() {
        boxW = Math.min(width - 24, 420);
        boxX = (width - boxW) / 2;
    }

    /** Height of the panel for the current text and options. */
    private int panelHeight() {
        int textLines = font.split(data.text(), boxW - 24).size();
        return 12 + 12 + textLines * 11 + 12 + data.options().size() * 13 + 8;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // no blur: keep the NPC visible above the conversation panel
        g.fillGradient(0, height / 2, width, height, 0x00000000, 0x90000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int panelH = panelHeight();
        boxY = height - panelH - 10;
        UiKit.panel(g, boxX, boxY, boxW, panelH);

        int y = boxY + 8;
        Component header = data.title().getString().isEmpty() ? data.name()
                : Component.empty().append(data.name()).append(Component.literal("  —  ").withStyle(st -> st.withColor(UiKit.TEXT_DIM & 0xFFFFFF))).append(data.title());
        g.drawCenteredString(font, header, width / 2, y, UiKit.GOLD);
        y += 14;
        for (FormattedCharSequence line : font.split(data.text(), boxW - 24)) {
            g.drawCenteredString(font, line, width / 2, y, UiKit.TEXT);
            y += 11;
        }
        y += 4;
        g.fill(boxX + boxW / 4, y, boxX + boxW * 3 / 4, y + 1, UiKit.TRIM);
        y += 7;
        optionsY = y;
        hovered = -1;
        List<Component> options = data.options();
        for (int i = 0; i < options.size(); i++) {
            Component label = Component.literal((i + 1) + ". ").append(options.get(i));
            int w = font.width(label);
            int x = width / 2 - w / 2;
            boolean hover = mouseY >= y - 2 && mouseY < y + 11 && mouseX >= boxX && mouseX < boxX + boxW;
            if (hover) {
                hovered = i;
                g.fill(boxX + 6, y - 2, boxX + boxW - 6, y + 10, UiKit.BG_HOVER);
            }
            g.drawString(font, label, x, y, hover ? UiKit.GOLD : UiKit.TEXT, true);
            y += 13;
        }
    }

    private void choose(int index) {
        if (index >= 0 && index < data.options().size()) {
            PacketDistributor.sendToServer(new NpcsNetwork.ChooseOption(data.entityId(), index));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hovered >= 0) {
            choose(hovered);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            choose(keyCode - GLFW.GLFW_KEY_1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            choose(data.options().size() - 1); // last option is always "Goodbye"
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
