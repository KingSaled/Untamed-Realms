package com.untamedrealms.skills.client;

import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.skills.SkillsClientConfig;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.network.SkillsPayloads;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** RuneScape-style XP drops: skill icon + amount drifting upward on the right side of the screen. */
public final class XpDropHud {
    private static final int LIFETIME = 50;
    private static final List<Drop> DROPS = new ArrayList<>();

    private static final class Drop {
        final Skill skill;
        float xp;
        int age;

        Drop(Skill skill, float xp) {
            this.skill = skill;
            this.xp = xp;
        }
    }

    private XpDropHud() {}

    public static void accept(SkillsPayloads.XpDrops payload) {
        if (!SkillsClientConfig.SHOW_XP_DROPS.get()) return;
        for (SkillsPayloads.Drop d : payload.drops()) {
            if (d.skill() < 0 || d.skill() >= Skill.VALUES.size()) continue;
            Skill skill = Skill.VALUES.get(d.skill());
            Drop merged = null;
            for (Drop existing : DROPS) {
                if (existing.skill == skill && existing.age < 8) { merged = existing; break; }
            }
            if (merged != null) merged.xp += d.xp();
            else DROPS.add(new Drop(skill, d.xp()));
        }
        while (DROPS.size() > 8) DROPS.remove(0);
    }

    public static void tick() {
        Iterator<Drop> it = DROPS.iterator();
        while (it.hasNext()) {
            if (++it.next().age > LIFETIME) it.remove();
        }
    }

    public static void clear() {
        DROPS.clear();
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || DROPS.isEmpty()) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        int x = g.guiWidth() - 70;
        int baseY = g.guiHeight() / 2 + 20;
        for (int i = 0; i < DROPS.size(); i++) {
            Drop drop = DROPS.get(i);
            float t = (drop.age + partial) / LIFETIME;
            int y = (int) (baseY - t * 60) - (DROPS.size() - 1 - i) * 2;
            int alpha = (int) (255 * Math.min(1f, (1f - t) * 3f));
            if (alpha < 10) continue;
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(0.75f, 0.75f, 1f);
            g.renderItem(new ItemStack(drop.skill.icon()), 0, 0);
            g.pose().popPose();
            String text = "+" + (drop.xp >= 10 ? String.valueOf(Math.round(drop.xp)) : String.format("%.1f", drop.xp));
            g.drawString(mc.font, text, x + 15, y + 3, (alpha << 24) | (UiKit.GOLD & 0xFFFFFF), true);
        }
    }
}
