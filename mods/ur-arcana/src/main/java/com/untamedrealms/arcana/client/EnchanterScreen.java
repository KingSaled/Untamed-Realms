package com.untamedrealms.arcana.client;

import com.untamedrealms.arcana.enchanting.Enchanting;
import com.untamedrealms.arcana.item.SoulGemItem;
import com.untamedrealms.arcana.network.ArcanaNetwork;
import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.client.ClientSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The arcane enchanter. Enchant tab: three columns - your gear, the enchantments you know that fit it,
 * your filled soul gems - and a preview of the result. Disenchant tab: your enchanted items and what
 * destroying each would teach you.
 */
public class EnchanterScreen extends Screen {
    private static final int ROW = 20;
    private final BlockPos pos;
    private boolean disenchantTab;
    private int left, top, panelW, panelH, listTop, visibleRows;
    private final int[] scroll = new int[3];
    private int gearSlot = -1, gemSlot = -1;
    private ResourceLocation enchantment;
    private Button action;

    public EnchanterScreen(BlockPos pos) {
        super(Component.translatable("block.urarcana.arcane_enchanter"));
        this.pos = pos;
    }

    private Inventory inventory() {
        return minecraft.player.getInventory();
    }

    private Registry<Enchantment> registry() {
        return minecraft.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    }

    private int slots() {
        return Enchanting.slots(SkillsApi.effect(minecraft.player, Enchanting.ENCHANT_SLOTS, ""));
    }

    // ------------------------------------------------------------------ columns

    private List<Integer> gear() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            ItemStack stack = inventory().getItem(i);
            if (disenchantTab ? !Enchanting.learnable(stack).isEmpty() : Enchanting.isGear(stack)) out.add(i);
        }
        return out;
    }

    private List<Holder.Reference<Enchantment>> enchantments(ItemStack gear) {
        List<Holder.Reference<Enchantment>> out = new ArrayList<>();
        for (ResourceLocation id : ClientArcana.knowledge().enchantments()) {
            registry().getHolder(ResourceKey.create(Registries.ENCHANTMENT, id)).ifPresent(h -> {
                if (gear.isEmpty() || Enchanting.canApply(gear, h, slots())) out.add(h);
            });
        }
        out.sort(Comparator.comparing(h -> h.value().description().getString()));
        return out;
    }

    private List<Integer> gems() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < 36; i++) if (Enchanting.soul(inventory().getItem(i)) > 0) out.add(i);
        return out;
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        panelW = Math.min(width - 16, 420);
        panelH = Math.min(height - 16, 240);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        listTop = top + 40;
        visibleRows = (top + panelH - 34 - listTop) / ROW;
        addRenderableWidget(Button.builder(Component.translatable("screen.urarcana.tab_enchant"), b -> switchTab(false)).bounds(left + 8, top + 20, 80, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.urarcana.tab_disenchant"), b -> switchTab(true)).bounds(left + 92, top + 20, 80, 16).build());
        action = addRenderableWidget(Button.builder(Component.empty(), b -> act()).bounds(left + panelW - 128, top + panelH - 26, 120, 18).build());
    }

    private void switchTab(boolean disenchant) {
        disenchantTab = disenchant;
        gearSlot = gemSlot = -1;
        enchantment = null;
        scroll[0] = scroll[1] = scroll[2] = 0;
    }

    private void act() {
        if (disenchantTab) {
            if (gearSlot >= 0) PacketDistributor.sendToServer(new ArcanaNetwork.Disenchant(pos, gearSlot));
            gearSlot = -1;
        } else if (gearSlot >= 0 && enchantment != null && gemSlot >= 0) {
            PacketDistributor.sendToServer(new ArcanaNetwork.Enchant(pos, gearSlot, enchantment, gemSlot));
            enchantment = null;
            gemSlot = -1;
        }
    }

    private int columnW() {
        return disenchantTab ? 180 : (panelW - 32) / 3;
    }

    private int columnX(int column) {
        return left + 8 + column * (columnW() + 8);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        UiKit.panel(g, left, top, panelW, panelH);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        List<Integer> gear = gear();
        if (gearSlot >= 0 && !gear.contains(gearSlot)) gearSlot = -1;
        ItemStack selectedGear = gearSlot >= 0 ? inventory().getItem(gearSlot) : ItemStack.EMPTY;
        List<Holder.Reference<Enchantment>> known = enchantments(selectedGear);
        if (enchantment != null && known.stream().noneMatch(h -> h.key().location().equals(enchantment))) enchantment = null;
        List<Integer> gems = gems();
        if (gemSlot >= 0 && !gems.contains(gemSlot)) gemSlot = -1;

        action.setMessage(Component.translatable(disenchantTab ? "screen.urarcana.disenchant" : "screen.urarcana.enchant"));
        action.active = disenchantTab ? gearSlot >= 0 : gearSlot >= 0 && enchantment != null && gemSlot >= 0;
        super.render(g, mouseX, mouseY, partialTick);
        UiKit.heading(g, font, title.copy().withStyle(ChatFormatting.BOLD), left, top + 7, panelW);
        String skill = Component.translatable("screen.urarcana.skill", Skill.ENCHANTING.displayName(), ClientSkills.data().level(Skill.ENCHANTING)).getString();
        g.drawString(font, skill, left + panelW - 8 - font.width(skill), top + 24, UiKit.TEXT_DIM, false);

        ItemStack tooltip = ItemStack.EMPTY;
        int w = columnW();
        // column 0: gear
        header(g, 0, disenchantTab ? "screen.urarcana.col_enchanted" : "screen.urarcana.col_gear");
        tooltip = itemColumn(g, 0, gear, gearSlot, mouseX, mouseY, tooltip,
                gear.isEmpty() ? (disenchantTab ? "screen.urarcana.none_enchanted" : "screen.urarcana.none_gear") : null);
        if (disenchantTab) {
            renderDisenchant(g, selectedGear);
        } else {
            header(g, 1, "screen.urarcana.col_known");
            int x = columnX(1);
            if (known.isEmpty()) {
                UiKit.wrapped(g, font, Component.translatable(ClientArcana.knowledge().enchantments().isEmpty()
                        ? "screen.urarcana.none_known" : "screen.urarcana.none_fit"), x, listTop + 2, w, UiKit.TEXT_DIM);
            }
            scroll[1] = Mth.clamp(scroll[1], 0, Math.max(0, known.size() - visibleRows));
            for (int i = scroll[1]; i < Math.min(known.size(), scroll[1] + visibleRows); i++) {
                Holder.Reference<Enchantment> h = known.get(i);
                int y = listTop + (i - scroll[1]) * ROW;
                boolean picked = h.key().location().equals(enchantment);
                g.fill(x, y, x + w, y + ROW - 2, picked ? UiKit.BG_SELECTED : UiKit.inside(mouseX, mouseY, x, y, w, ROW - 2) ? UiKit.BG_HOVER : UiKit.BG_INNER);
                g.drawString(font, font.split(h.value().description(), w - 8).get(0), x + 4, y + 5, picked ? UiKit.GOLD : UiKit.TEXT, false);
            }
            header(g, 2, "screen.urarcana.col_gems");
            tooltip = itemColumn(g, 2, gems, gemSlot, mouseX, mouseY, tooltip, gems.isEmpty() ? "screen.urarcana.none_gems" : null);
            renderPreview(g, selectedGear);
        }
        if (!tooltip.isEmpty()) g.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    private void header(GuiGraphics g, int column, String key) {
        g.drawString(font, Component.translatable(key), columnX(column), listTop - 11, UiKit.TEXT_DIM, false);
    }

    private ItemStack itemColumn(GuiGraphics g, int column, List<Integer> slots, int selected, int mouseX, int mouseY, ItemStack tooltip, String empty) {
        int x = columnX(column), w = columnW();
        if (empty != null) {
            UiKit.wrapped(g, font, Component.translatable(empty), x, listTop + 2, w, UiKit.TEXT_DIM);
            return tooltip;
        }
        scroll[column] = Mth.clamp(scroll[column], 0, Math.max(0, slots.size() - visibleRows));
        for (int i = scroll[column]; i < Math.min(slots.size(), scroll[column] + visibleRows); i++) {
            ItemStack stack = inventory().getItem(slots.get(i));
            int y = listTop + (i - scroll[column]) * ROW;
            boolean hover = UiKit.inside(mouseX, mouseY, x, y, w, ROW - 2);
            g.fill(x, y, x + w, y + ROW - 2, slots.get(i) == selected ? UiKit.BG_SELECTED : hover ? UiKit.BG_HOVER : UiKit.BG_INNER);
            g.renderItem(stack, x + 2, y + 1);
            Component name = stack.getItem() instanceof SoulGemItem
                    ? Component.translatable("soul.urarcana." + SoulGemItem.SIZES[SoulGemItem.soul(stack)]) : stack.getHoverName();
            g.drawString(font, font.split(name, w - 24).get(0), x + 21, y + 5, UiKit.TEXT, false);
            if (hover) tooltip = stack;
        }
        return tooltip;
    }

    private void renderPreview(GuiGraphics g, ItemStack gear) {
        int y = top + panelH - 24;
        int x = left + 8;
        if (gearSlot < 0 || enchantment == null || gemSlot < 0) {
            g.drawString(font, Component.translatable("screen.urarcana.enchant_help"), x, y + 4, UiKit.TEXT_DIM, false);
            return;
        }
        Holder<Enchantment> h = registry().getHolder(ResourceKey.create(Registries.ENCHANTMENT, enchantment)).orElse(null);
        if (h == null) return;
        int soul = Enchanting.soul(inventory().getItem(gemSlot));
        int level = Enchanting.level(h, soul, ClientSkills.data().level(Skill.ENCHANTING),
                SkillsApi.effect(minecraft.player, Enchanting.ENCHANT_POWER, ""));
        Component result = Component.translatable("screen.urarcana.preview", gear.getHoverName(), Enchantment.getFullname(h, level));
        g.drawString(font, font.split(result, panelW - 150).get(0), x, y + 4, UiKit.GOLD, false);
    }

    private void renderDisenchant(GuiGraphics g, ItemStack stack) {
        int x = columnX(1), y = listTop, w = panelW - 16 - (x - left);
        if (stack.isEmpty()) {
            UiKit.wrapped(g, font, Component.translatable("screen.urarcana.disenchant_help"), x, y, w, UiKit.TEXT_DIM);
            return;
        }
        g.renderItem(stack, x, y);
        g.drawString(font, stack.getHoverName(), x + 20, y + 4, UiKit.GOLD, false);
        y += 22;
        g.drawString(font, Component.translatable("screen.urarcana.teaches"), x, y, UiKit.TEXT_DIM, false);
        y += 12;
        boolean anyNew = false;
        for (Holder<Enchantment> h : Enchanting.learnable(stack)) {
            boolean known = h.unwrapKey().map(k -> ClientArcana.knowledge().enchantments().contains(k.location())).orElse(false);
            anyNew |= !known;
            Component line = h.value().description().copy().append(known ? Component.translatable("screen.urarcana.known") : Component.empty());
            g.drawString(font, line, x + 4, y, known ? UiKit.TEXT_DIM : UiKit.GOOD, false);
            y += 11;
        }
        y += 6;
        UiKit.wrapped(g, font, Component.translatable(anyNew ? "screen.urarcana.disenchant_warning" : "screen.urarcana.nothing_new"), x, y, w,
                anyNew ? UiKit.BAD : UiKit.TEXT_DIM);
        action.active = anyNew;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY >= listTop && mouseY < listTop + visibleRows * ROW) {
            int columns = disenchantTab ? 1 : 3;
            for (int c = 0; c < columns; c++) {
                int x = columnX(c);
                if (mouseX < x || mouseX >= x + columnW()) continue;
                int i = scroll[c] + (int) ((mouseY - listTop) / ROW);
                if (c == 0) {
                    List<Integer> gear = gear();
                    if (i < gear.size()) { gearSlot = gear.get(i); return true; }
                } else if (c == 1) {
                    ItemStack selected = gearSlot >= 0 ? inventory().getItem(gearSlot) : ItemStack.EMPTY;
                    List<Holder.Reference<Enchantment>> known = enchantments(selected);
                    if (i < known.size()) { enchantment = known.get(i).key().location(); return true; }
                } else {
                    List<Integer> gems = gems();
                    if (i < gems.size()) { gemSlot = gems.get(i); return true; }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int columns = disenchantTab ? 1 : 3;
        for (int c = 0; c < columns; c++) {
            if (mouseX >= columnX(c) && mouseX < columnX(c) + columnW()) scroll[c] -= (int) Math.signum(scrollY);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
