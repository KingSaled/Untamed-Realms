package com.untamedrealms.core.client;

import com.untamedrealms.core.CoreClientConfig;
import com.untamedrealms.core.UntamedCore;
import com.untamedrealms.core.client.ui.UiKit;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.ModListScreen;

/** The Untamed Realms main menu: one Play button that joins the pack's server, plus the usual options. */
public class UrTitleScreen extends Screen {
    private static final int BUTTON_W = 200;

    public UrTitleScreen() {
        super(Component.translatable("menu.urcore.title"));
    }

    @Override
    protected void init() {
        int x = width / 2 - BUTTON_W / 2;
        int y = height / 2 - 10;
        String address = CoreClientConfig.SERVER_ADDRESS.get();
        addRenderableWidget(Button.builder(Component.translatable("menu.urcore.play").withStyle(ChatFormatting.BOLD), b -> join(address))
                .bounds(x, y, BUTTON_W, 24).build());
        y += 30;
        addRenderableWidget(Button.builder(Component.translatable("menu.multiplayer"), b -> minecraft.setScreen(new JoinMultiplayerScreen(this)))
                .bounds(x, y, BUTTON_W / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("menu.singleplayer"), b -> minecraft.setScreen(new SelectWorldScreen(this)))
                .bounds(x + BUTTON_W / 2 + 2, y, BUTTON_W / 2 - 2, 20).build());
        y += 24;
        addRenderableWidget(Button.builder(Component.translatable("menu.options"), b -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)))
                .bounds(x, y, BUTTON_W / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("fml.menu.mods"), b -> minecraft.setScreen(new ModListScreen(this)))
                .bounds(x + BUTTON_W / 2 + 2, y, BUTTON_W / 2 - 2, 20).build());
        y += 24;
        addRenderableWidget(Button.builder(Component.translatable("menu.quit"), b -> minecraft.stop())
                .bounds(x, y, BUTTON_W, 20).build());
    }

    private void join(String address) {
        ServerData data = new ServerData("Untamed Realms", address, ServerData.Type.OTHER);
        ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(address), data, false, null);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPanorama(g, partialTick);
        g.fillGradient(0, 0, width, height, 0x60000000, 0xB0000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int titleY = height / 2 - 78;
        g.pose().pushPose();
        g.pose().translate(cx, titleY, 0);
        g.pose().scale(4f, 4f, 1f);
        Component title = Component.translatable("menu.urcore.title");
        g.drawString(font, title, -font.width(title) / 2, 0, UiKit.GOLD, true);
        g.pose().popPose();
        Component tagline = Component.translatable("menu.urcore.tagline");
        g.drawCenteredString(font, tagline, cx, titleY + 40, UiKit.TEXT);
        g.fill(cx - 90, titleY + 54, cx + 90, titleY + 55, UiKit.TRIM);

        Component server = Component.translatable("menu.urcore.server", CoreClientConfig.SERVER_ADDRESS.get());
        g.drawCenteredString(font, server, cx, height / 2 - 22, UiKit.TEXT_DIM);

        String version = ModList.get().getModContainerById(UntamedCore.MODID).map(c -> c.getModInfo().getVersion().toString()).orElse("?");
        g.drawString(font, "Untamed Realms " + version + "  ·  Minecraft " + SharedConstants.getCurrentVersion().getName(), 4, height - 12, UiKit.TEXT_DIM, true);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
