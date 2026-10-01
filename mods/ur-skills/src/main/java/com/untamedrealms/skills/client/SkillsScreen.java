package com.untamedrealms.skills.client;

import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.skills.SkillsConfig;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillMath;
import com.untamedrealms.skills.data.SkillData;
import com.untamedrealms.skills.network.SkillsPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The character sheet: character level, attribute choices and all 24 skills in four columns.
 * Clicking a skill opens its perk tree.
 */
public class SkillsScreen extends Screen {
    private static final int ROW_H = 24;
    private static final int HEADER_H = 66;
    /** Line under the title (ur-classes sets it to "Class · born under Sign"). */
    public static java.util.function.Supplier<Component> subtitle = () -> null;

    private int left, top, panelW, panelH, colW;
    private final List<Button> attributeButtons = new ArrayList<>();

    public SkillsScreen() {
        super(Component.translatable("screen.urskills.skills"));
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 16, 480);
        panelH = Math.min(height - 16, HEADER_H + 18 + 6 * ROW_H + 10);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        colW = (panelW - 16) / 4;

        attributeButtons.clear();
        int bw = 62;
        int bx = left + panelW - 10 - bw * 3 - 8;
        int by = top + 43;
        String[] keys = {"health", "magicka", "stamina"};
        for (int i = 0; i < 3; i++) {
            int which = i;
            Button button = Button.builder(Component.translatable("screen.urskills.attribute." + keys[i]),
                            b -> PacketDistributor.sendToServer(new SkillsPayloads.SpendAttribute(which)))
                    .bounds(bx + i * (bw + 4), by, bw, 16)
                    .build();
            attributeButtons.add(addRenderableWidget(button));
        }
        updateButtons();
    }

    private void updateButtons() {
        boolean canSpend = ClientSkills.data().attributePoints() > 0;
        for (Button b : attributeButtons) b.visible = canSpend;
        if (canSpend) {
            attributeButtons.get(0).active = ClientSkills.data().healthPicks() < (SkillsConfig.SPEC.isLoaded() ? SkillsConfig.MAX_HEALTH_PICKS.get() : SkillsConfig.MAX_HEALTH_PICKS.getDefault());
        }
    }

    @Override
    public void tick() {
        updateButtons();
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

        UiKit.heading(g, font, Component.translatable("screen.urskills.skills").withStyle(ChatFormatting.BOLD), left, top + 7, panelW);

        Component sub = subtitle.get();
        if (sub != null) g.drawCenteredString(font, sub, left + panelW / 2, top + 20, UiKit.TEXT_DIM);

        // Left: character level, its progress, perk points. Right: attribute choice (when available).
        int cx = left + 10;
        int cy = top + 33;
        g.drawString(font, Component.translatable("screen.urskills.level", data.characterLevel()), cx, cy, UiKit.TEXT, true);
        int need = SkillMath.characterXpToNext(data.characterLevel());
        UiKit.bar(g, cx, cy + 12, 140, 4, data.characterXp() / (float) need, UiKit.XP, UiKit.XP_DARK);
        Component perks = Component.translatable("screen.urskills.perk_points", data.perkPoints());
        g.drawString(font, perks, cx + 150, cy + 6, data.perkPoints() > 0 ? UiKit.GOLD : UiKit.TEXT_DIM, true);
        if (data.attributePoints() > 0) {
            Component choose = Component.translatable("screen.urskills.attribute_points", data.attributePoints());
            g.drawString(font, choose, left + panelW - 10 - font.width(choose), cy, UiKit.GOLD, true);
        }

        // Skill columns.
        Skill hovered = null;
        int gridTop = top + HEADER_H;
        Skill.Category[] categories = Skill.Category.values();
        for (int c = 0; c < categories.length; c++) {
            int colX = left + 8 + c * colW;
            UiKit.heading(g, font, categories[c].displayName(), colX, gridTop, colW);
            List<Skill> skills = Skill.inCategory(categories[c]);
            for (int r = 0; r < skills.size(); r++) {
                Skill skill = skills.get(r);
                int rx = colX + 2;
                int ry = gridTop + 14 + r * ROW_H;
                int rw = colW - 4;
                boolean hover = UiKit.inside(mouseX, mouseY, rx, ry, rw, ROW_H - 2);
                if (hover) hovered = skill;
                g.fill(rx, ry, rx + rw, ry + ROW_H - 2, hover ? UiKit.BG_HOVER : UiKit.BG_INNER);
                g.renderItem(new ItemStack(skill.icon()), rx + 2, ry + 2);
                int level = data.level(skill);
                String levelText = String.valueOf(level);
                int nameMax = rw - 24 - font.width(levelText) - 4;
                FormattedCharSequence name = font.split(skill.displayName(), Math.max(10, nameMax)).get(0);
                g.drawString(font, name, rx + 21, ry + 3, UiKit.TEXT, false);
                g.drawString(font, levelText, rx + rw - font.width(levelText) - 3, ry + 3, level >= SkillMath.MAX_LEVEL ? UiKit.GOLD : UiKit.TEXT, false);
                UiKit.bar(g, rx + 21, ry + 15, rw - 25, 2, SkillMath.progress(data.xp(skill)), UiKit.XP, UiKit.XP_DARK);
                if (hasAvailablePerk(data, skill)) {
                    g.fill(rx + rw - 4, ry + 13, rx + rw - 1, ry + 16, UiKit.GOLD);
                }
            }
        }

        if (hovered != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(hovered.displayName().copy().withStyle(ChatFormatting.GOLD));
            lines.add(hovered.description().copy().withStyle(ChatFormatting.GRAY));
            double xp = data.xp(hovered);
            int level = data.level(hovered);
            lines.add(Component.translatable("screen.urskills.xp", String.format("%,d", (long) xp)).withStyle(ChatFormatting.WHITE));
            if (level < SkillMath.MAX_LEVEL) {
                long remaining = (long) Math.ceil(SkillMath.xpForLevel(level + 1) - xp);
                lines.add(Component.translatable("screen.urskills.xp_remaining", String.format("%,d", remaining), level + 1).withStyle(ChatFormatting.WHITE));
            }
            lines.add(Component.translatable("screen.urskills.click_perks").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private static boolean hasAvailablePerk(SkillData data, Skill skill) {
        if (data.perkPoints() <= 0) return false;
        return com.untamedrealms.skills.data.SkillsData.perks(skill).stream().anyMatch(ref ->
                !data.hasPerk(ref.id()) && data.level(skill) >= ref.perk().level()
                        && ref.requires().stream().allMatch(data::hasPerk));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        int gridTop = top + HEADER_H;
        Skill.Category[] categories = Skill.Category.values();
        for (int c = 0; c < categories.length; c++) {
            int colX = left + 8 + c * colW;
            List<Skill> skills = Skill.inCategory(categories[c]);
            for (int r = 0; r < skills.size(); r++) {
                if (UiKit.inside(mouseX, mouseY, colX + 2, gridTop + 14 + r * ROW_H, colW - 4, ROW_H - 2)) {
                    minecraft.setScreen(new PerkScreen(this, skills.get(r)));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (SkillsKeys.OPEN_SKILLS.matches(keyCode, scanCode)) {
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
