package com.untamedrealms.magic.client;

import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.magic.UntamedMagic;
import com.untamedrealms.magic.data.SpellBook;
import com.untamedrealms.magic.data.SpellDef;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Readied spell next to the hotbar: icon, cooldown sweep, and five slot pips. */
public final class SpellHud {
    private SpellHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.player.isSpectator()) return;
        SpellBook book = ClientMagic.BOOK;
        ResourceLocation id = book.selectedSpell();
        if (id == null) return;
        SpellDef def = UntamedMagic.SPELLS.getOrNull(id);
        if (def == null) return;

        int x = g.guiWidth() / 2 + 91 + 8;
        int y = g.guiHeight() - 23;
        g.fill(x - 1, y - 1, x + 23, y + 23, 0xFF000000);
        g.fill(x, y, x + 22, y + 22, 0xC0101216);
        UiKit.frame(g, x, y, 22, 22, ClientMagic.schoolColor(def.school()));
        g.blit(ClientMagic.icon(id), x + 3, y + 3, 0, 0, 16, 16, 16, 16);

        long ready = ClientMagic.READY.getOrDefault(id, 0L);
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        if (ready > now && def.cooldown() > 0) {
            float left = Math.min(1f, (ready - now) / (float) def.cooldown());
            int h = Math.round(16 * left);
            g.fill(x + 3, y + 3 + 16 - h, x + 19, y + 19, 0xA0000000);
        }
        for (int i = 0; i < SpellBook.SLOTS; i++) {
            int px = x + 1 + i * 4;
            int color = book.slot(i) == null ? 0xFF333333 : i == book.selected() ? UiKit.GOLD : 0xFF8A8A8A;
            g.fill(px, y + 24, px + 3, y + 26, color);
        }
    }
}
