package com.untamedrealms.classes.client;

import com.untamedrealms.classes.data.Birthsign;
import com.untamedrealms.classes.data.ClassDef;
import com.untamedrealms.classes.data.ClassesData;
import com.untamedrealms.classes.network.ClassesNetwork;
import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.skills.api.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Character creation: step 1 picks a class (skills, gear, purse), step 2 a birthsign. Confirming
 * sends the choice to the server, which validates and applies it.
 */
public class ClassSelectionScreen extends Screen {
    private static final int LIST_W = 128;
    private static final int ROW_H = 20;

    private int step;
    private int left, top, panelW, panelH;
    private double scroll;
    private ResourceLocation selectedClass;
    private ResourceLocation selectedSign;
    private Button nextButton, backButton;
    private List<ItemStack> loadoutPreview = List.of();

    public ClassSelectionScreen() {
        super(Component.translatable("screen.urclasses.title"));
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 16, 430);
        panelH = Math.min(height - 16, 270);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        List<Map.Entry<ResourceLocation, ClassDef>> classes = ClassesData.sortedClasses();
        if (selectedClass == null && !classes.isEmpty()) selectClass(classes.get(0).getKey());
        List<Map.Entry<ResourceLocation, Birthsign>> signs = ClassesData.sortedBirthsigns();
        if (selectedSign == null && !signs.isEmpty()) selectedSign = signs.get(0).getKey();

        int by = top + panelH - 26;
        backButton = addRenderableWidget(Button.builder(Component.translatable("screen.urclasses.back"), b -> {
            step = 0;
            scroll = 0;
            updateButtons();
        }).bounds(left + LIST_W + 18, by, 80, 18).build());
        nextButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            if (step == 0) {
                if (ClassesData.BIRTHSIGNS.entries().isEmpty()) confirm();
                else { step = 1; scroll = 0; updateButtons(); }
            } else {
                confirm();
            }
        }).bounds(left + panelW - 150, by, 140, 18).build());
        updateButtons();
    }

    private void selectClass(ResourceLocation id) {
        selectedClass = id;
        ClassDef def = ClassesData.CLASSES.getOrNull(id);
        loadoutPreview = def == null || minecraft == null || minecraft.level == null ? List.of()
                : def.loadout().resolve(minecraft.level.registryAccess());
    }

    private void updateButtons() {
        backButton.visible = step == 1;
        nextButton.setMessage(step == 0 ? Component.translatable("screen.urclasses.next") : Component.translatable("screen.urclasses.confirm"));
        nextButton.active = step == 0 ? selectedClass != null : selectedSign != null;
    }

    private void confirm() {
        if (selectedClass == null) return;
        ResourceLocation sign = selectedSign != null ? selectedSign : ResourceLocation.withDefaultNamespace("none");
        PacketDistributor.sendToServer(new ClassesNetwork.Choose(selectedClass, sign));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        UiKit.panel(g, left, top, panelW, panelH);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (minecraft != null && minecraft.level != null && loadoutPreview.isEmpty() && selectedClass != null) selectClass(selectedClass);
        super.render(g, mouseX, mouseY, partialTick);
        Component heading = step == 0 ? Component.translatable("screen.urclasses.step_class") : Component.translatable("screen.urclasses.step_birthsign");
        UiKit.heading(g, font, heading.copy().withStyle(ChatFormatting.BOLD), left, top + 7, panelW);

        int listX = left + 8, listY = top + 22, listH = panelH - 22 - 8;
        UiKit.inset(g, listX, listY, LIST_W, listH);
        ItemStack hoveredItem = null;

        g.enableScissor(listX + 1, listY + 1, listX + LIST_W - 1, listY + listH - 1);
        if (step == 0) {
            List<Map.Entry<ResourceLocation, ClassDef>> classes = ClassesData.sortedClasses();
            scroll = Mth.clamp(scroll, 0, Math.max(0, classes.size() * ROW_H - listH + 4));
            for (int i = 0; i < classes.size(); i++) {
                int ry = listY + 2 + i * ROW_H - (int) scroll;
                drawRow(g, listX + 2, ry, classes.get(i).getValue().name(), classes.get(i).getValue().icon(),
                        classes.get(i).getKey().equals(selectedClass), UiKit.inside(mouseX, mouseY, listX + 2, ry, LIST_W - 4, ROW_H - 2));
            }
        } else {
            List<Map.Entry<ResourceLocation, Birthsign>> signs = ClassesData.sortedBirthsigns();
            scroll = Mth.clamp(scroll, 0, Math.max(0, signs.size() * ROW_H - listH + 4));
            for (int i = 0; i < signs.size(); i++) {
                int ry = listY + 2 + i * ROW_H - (int) scroll;
                drawRow(g, listX + 2, ry, signs.get(i).getValue().name(), signs.get(i).getValue().icon(),
                        signs.get(i).getKey().equals(selectedSign), UiKit.inside(mouseX, mouseY, listX + 2, ry, LIST_W - 4, ROW_H - 2));
            }
        }
        g.disableScissor();

        int dx = listX + LIST_W + 10, dy = listY + 2, dw = panelW - LIST_W - 28;
        if (step == 0) {
            ClassDef def = ClassesData.CLASSES.getOrNull(selectedClass);
            if (def != null) hoveredItem = drawClassDetails(g, def, dx, dy, dw, mouseX, mouseY);
        } else {
            Birthsign sign = ClassesData.BIRTHSIGNS.getOrNull(selectedSign);
            ClassDef def = ClassesData.CLASSES.getOrNull(selectedClass);
            if (sign != null) {
                g.pose().pushPose();
                g.pose().translate(dx, dy, 0);
                g.pose().scale(1.5f, 1.5f, 1f);
                g.drawString(font, sign.name(), 0, 0, UiKit.GOLD, true);
                g.pose().popPose();
                int y = dy + 18;
                y += UiKit.wrapped(g, font, sign.description(), dx, y, dw, UiKit.TEXT) + 8;
                if (def != null) {
                    g.drawString(font, Component.translatable("screen.urclasses.summary", def.name(), sign.name()), dx, y, UiKit.TEXT_DIM, false);
                }
            }
        }
        if (hoveredItem != null) g.renderTooltip(font, hoveredItem, mouseX, mouseY);
    }

    private ItemStack drawClassDetails(GuiGraphics g, ClassDef def, int x, int y, int w, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(1.5f, 1.5f, 1f);
        g.drawString(font, def.name(), 0, 0, UiKit.GOLD, true);
        g.pose().popPose();
        int yy = y + 15;
        if (!def.tagline().getString().isEmpty()) {
            g.drawString(font, def.tagline().copy().withStyle(ChatFormatting.ITALIC), x, yy, UiKit.TEXT_DIM, false);
            yy += 11;
        }
        yy += UiKit.wrapped(g, font, def.description(), x, yy + 2, w, UiKit.TEXT) + 6;

        g.drawString(font, Component.translatable("screen.urclasses.starting_skills"), x, yy, UiKit.GOLD, false);
        yy += 11;
        MutableComponent skills = Component.empty();
        List<Map.Entry<Skill, Integer>> sorted = def.skills().entrySet().stream()
                .sorted(Map.Entry.<Skill, Integer>comparingByValue(Comparator.reverseOrder())).toList();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) skills.append(Component.literal("  ·  ").withStyle(ChatFormatting.DARK_GRAY));
            skills.append(sorted.get(i).getKey().displayName()).append(" " + sorted.get(i).getValue());
        }
        yy += UiKit.wrapped(g, font, skills, x, yy, w, UiKit.TEXT) + 6;

        g.drawString(font, Component.translatable("screen.urclasses.starting_gear"), x, yy, UiKit.GOLD, false);
        yy += 11;
        ItemStack hovered = null;
        int perRow = Math.max(1, w / 18);
        for (int i = 0; i < loadoutPreview.size(); i++) {
            int ix = x + (i % perRow) * 18, iy = yy + (i / perRow) * 18;
            g.fill(ix, iy, ix + 17, iy + 17, UiKit.BG_INNER);
            g.renderItem(loadoutPreview.get(i), ix, iy);
            g.renderItemDecorations(font, loadoutPreview.get(i), ix, iy);
            if (UiKit.inside(mouseX, mouseY, ix, iy, 17, 17)) hovered = loadoutPreview.get(i);
        }
        yy += ((loadoutPreview.size() + perRow - 1) / perRow) * 18 + 4;
        if (def.coins() > 0) {
            g.drawString(font, Component.translatable("screen.urclasses.purse", def.coins()), x, yy, UiKit.GOLD, false);
        }
        return hovered;
    }

    private void drawRow(GuiGraphics g, int x, int y, Component name, ResourceLocation icon, boolean selected, boolean hover) {
        int w = LIST_W - 4;
        g.fill(x, y, x + w, y + ROW_H - 2, selected ? UiKit.BG_SELECTED : hover ? UiKit.BG_HOVER : 0);
        if (selected) UiKit.frame(g, x, y, w, ROW_H - 2, UiKit.TRIM);
        g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(icon)), x + 1, y + 1);
        g.drawString(font, name, x + 20, y + 5, selected ? UiKit.GOLD : UiKit.TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        int listX = left + 8, listY = top + 22, listH = panelH - 30;
        if (!UiKit.inside(mouseX, mouseY, listX, listY, LIST_W, listH)) return false;
        int index = (int) ((mouseY - listY - 2 + scroll) / ROW_H);
        if (step == 0) {
            List<Map.Entry<ResourceLocation, ClassDef>> classes = ClassesData.sortedClasses();
            if (index >= 0 && index < classes.size()) selectClass(classes.get(index).getKey());
        } else {
            List<Map.Entry<ResourceLocation, Birthsign>> signs = ClassesData.sortedBirthsigns();
            if (index >= 0 && index < signs.size()) selectedSign = signs.get(index).getKey();
        }
        updateButtons();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= scrollY * ROW_H;
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
