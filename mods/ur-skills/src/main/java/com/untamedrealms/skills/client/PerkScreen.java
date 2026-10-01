package com.untamedrealms.skills.client;

import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillMath;
import com.untamedrealms.skills.data.SkillData;
import com.untamedrealms.skills.data.SkillsData;
import com.untamedrealms.skills.network.SkillsPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** A single skill's perk tree as a scrollable list, with unlock buttons for available perks. */
public class PerkScreen extends Screen {
    private static final int ROW_H = 40;

    private final Screen parent;
    private final Skill skill;
    private int left, top, panelW, panelH, listTop, listH;
    private double scroll;

    public PerkScreen(Screen parent, Skill skill) {
        super(skill.displayName());
        this.parent = parent;
        this.skill = skill;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 16, 340);
        panelH = Math.min(height - 16, 260);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        listTop = top + 52;
        listH = panelH - 52 - 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(left + panelW / 2 - 40, top + panelH - 24, 80, 18).build());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        UiKit.panel(g, left, top, panelW, panelH);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        SkillData data = ClientSkills.data();
        int level = data.level(skill);

        UiKit.heading(g, font, skill.displayName().copy().withStyle(ChatFormatting.BOLD), left, top + 7, panelW);
        g.renderItem(new ItemStack(skill.icon()), left + 10, top + 20);
        g.drawString(font, Component.translatable("screen.urskills.skill_level", level), left + 30, top + 20, UiKit.TEXT, true);
        UiKit.bar(g, left + 30, top + 31, 120, 3, SkillMath.progress(data.xp(skill)), UiKit.XP, UiKit.XP_DARK);
        g.drawString(font, Component.translatable("screen.urskills.perk_points", data.perkPoints()),
                left + panelW - 10 - font.width(Component.translatable("screen.urskills.perk_points", data.perkPoints())),
                top + 20, data.perkPoints() > 0 ? UiKit.GOLD : UiKit.TEXT_DIM, true);
        UiKit.wrapped(g, font, skill.description(), left + 10, top + 38, panelW - 20, UiKit.TEXT_DIM);

        List<SkillsData.PerkRef> perks = SkillsData.perks(skill);
        int contentH = perks.size() * ROW_H;
        scroll = Mth.clamp(scroll, 0, Math.max(0, contentH - listH));

        g.enableScissor(left + 6, listTop, left + panelW - 6, listTop + listH);
        List<Component> tooltip = null;
        for (int i = 0; i < perks.size(); i++) {
            SkillsData.PerkRef ref = perks.get(i);
            int ry = listTop + i * ROW_H - (int) scroll;
            if (ry + ROW_H < listTop || ry > listTop + listH) continue;
            int rx = left + 8;
            int rw = panelW - 16;
            boolean owned = data.hasPerk(ref.id());
            boolean levelOk = level >= ref.perk().level();
            boolean reqOk = ref.requires().stream().allMatch(data::hasPerk);
            boolean canBuy = !owned && levelOk && reqOk && data.perkPoints() >= ref.perk().cost();

            g.fill(rx, ry, rx + rw, ry + ROW_H - 3, owned ? UiKit.BG_SELECTED : UiKit.BG_INNER);
            if (owned) UiKit.frame(g, rx, ry, rw, ROW_H - 3, UiKit.TRIM);
            int nameColor = owned ? UiKit.GOLD : levelOk && reqOk ? UiKit.TEXT : UiKit.TEXT_DIM;
            g.drawString(font, ref.perk().name(), rx + 5, ry + 4, nameColor, true);
            Component reqText = Component.translatable("screen.urskills.perk_req", ref.perk().level());
            g.drawString(font, reqText, rx + 5 + font.width(ref.perk().name()) + 6, ry + 4, levelOk ? UiKit.GOOD : UiKit.BAD, false);
            UiKit.wrapped(g, font, ref.perk().description(), rx + 5, ry + 15, rw - 70, UiKit.TEXT_DIM);

            int bx = rx + rw - 58, by = ry + 10, bw = 52, bh = 16;
            if (owned) {
                g.drawString(font, Component.translatable("screen.urskills.perk_owned"), bx + 6, by + 4, UiKit.GOLD, false);
            } else if (canBuy) {
                boolean hover = UiKit.inside(mouseX, mouseY, bx, by, bw, bh);
                g.fill(bx, by, bx + bw, by + bh, hover ? 0xFF5A4A28 : 0xFF3A3020);
                UiKit.frame(g, bx, by, bw, bh, UiKit.TRIM);
                Component unlock = Component.translatable("screen.urskills.perk_unlock");
                g.drawString(font, unlock, bx + bw / 2 - font.width(unlock) / 2, by + 4, UiKit.GOLD, false);
            } else if (!reqOk && UiKit.inside(mouseX, mouseY, rx, ry, rw, ROW_H - 3)) {
                tooltip = ref.requires().stream()
                        .map(id -> {
                            SkillsData.PerkRef r = SkillsData.perk(id);
                            Component n = r == null ? Component.literal(id.toString()) : r.perk().name();
                            return (Component) Component.translatable("screen.urskills.perk_needs", n)
                                    .withStyle(data.hasPerk(id) ? ChatFormatting.GREEN : ChatFormatting.RED);
                        })
                        .toList();
            }
        }
        g.disableScissor();
        if (perks.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.urskills.no_perks"), left + panelW / 2, listTop + 10, UiKit.TEXT_DIM);
        }
        if (tooltip != null) g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (!UiKit.inside(mouseX, mouseY, left, listTop, panelW, listH)) return false;
        List<SkillsData.PerkRef> perks = SkillsData.perks(skill);
        for (int i = 0; i < perks.size(); i++) {
            int ry = listTop + i * ROW_H - (int) scroll;
            int bx = left + 8 + panelW - 16 - 58;
            if (UiKit.inside(mouseX, mouseY, bx, ry + 10, 52, 16)) {
                ResourceLocation id = perks.get(i).id();
                if (!ClientSkills.data().hasPerk(id)) {
                    PacketDistributor.sendToServer(new SkillsPayloads.UnlockPerk(id));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= scrollY * 16;
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
