package com.untamedrealms.magic.client;

import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.magic.UntamedMagic;
import com.untamedrealms.magic.data.SpellBook;
import com.untamedrealms.magic.data.SpellDef;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Readied spell in a socket left of the hotbar: icon, cooldown sweep, and one pip per quick slot above it. */
public final class SpellHud {
    private static final ResourceLocation SOCKET = ResourceLocation.withDefaultNamespace("hud/hotbar_offhand_left");

    private SpellHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.player.isSpectator()) return;
        SpellBook book = ClientMagic.BOOK;
        ResourceLocation id = book.selectedSpell();
        if (id == null) return;
        SpellDef def = UntamedMagic.SPELLS.getOrNull(id);
        if (def == null) return;

        // A socket left of the hotbar (beyond the off-hand slot), styled like the hotbar itself.
        int x = g.guiWidth() / 2 - 91 - 29 - 31;
        int y = g.guiHeight() - 23;
        g.blitSprite(SOCKET, x, y, 29, 24);
        int ix = x + 3, iy = y + 4;
        UiKit.frame(g, ix - 1, iy - 1, 18, 18, ClientMagic.schoolColor(def.school()));
        g.blit(ClientMagic.icon(id), ix, iy, 0, 0, 16, 16, 16, 16);

        long ready = ClientMagic.READY.getOrDefault(id, 0L);
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        if (ready > now && def.cooldown() > 0) {
            float left = Math.min(1f, (ready - now) / (float) def.cooldown());
            int h = Math.round(16 * left);
            g.fill(ix, iy + 16 - h, ix + 16, iy + 16, 0xA0000000);
        }
        for (int i = 0; i < SpellBook.SLOTS; i++) {
            int px = ix - 3 + i * 3;
            int color = book.slot(i) == null ? 0xFF333333 : i == book.selected() ? UiKit.GOLD : 0xFF8A8A8A;
            g.fill(px, y - 3, px + 2, y - 1, color);
        }
    }
}
