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
 * Skyrim-style conversation: the NPC's line at the top, the player's responses listed below.
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
        boxW = Math.min(width - 24, 380);
        boxX = (width - boxW) / 2;
        boxY = height / 2 - 10;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // no blur: keep the NPC visible behind the conversation
        g.fillGradient(0, height / 2 - 40, width, height, 0x00000000, 0xC0000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int y = boxY;
        Component header = data.title().getString().isEmpty() ? data.name()
                : Component.empty().append(data.name()).append(Component.literal("  —  ").withStyle(s -> s.withColor(UiKit.TEXT_DIM & 0xFFFFFF))).append(data.title());
        g.drawCenteredString(font, header, width / 2, y, UiKit.GOLD);
        y += 14;
        for (FormattedCharSequence line : font.split(data.text(), boxW)) {
            g.drawCenteredString(font, line, width / 2, y, UiKit.TEXT);
            y += 11;
        }
        y += 8;
        g.fill(boxX + boxW / 4, y, boxX + boxW * 3 / 4, y + 1, UiKit.TRIM);
        y += 8;
        optionsY = y;
        hovered = -1;
        List<Component> options = data.options();
        for (int i = 0; i < options.size(); i++) {
            Component label = Component.literal((i + 1) + ". ").append(options.get(i));
            int w = font.width(label);
            int x = width / 2 - w / 2;
            boolean hover = mouseY >= y - 1 && mouseY < y + 11 && mouseX >= boxX && mouseX < boxX + boxW;
            if (hover) {
                hovered = i;
                g.fill(x - 6, y - 2, x + w + 6, y + 10, 0x40E8C872);
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
