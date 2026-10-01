package com.untamedrealms.arsenal.client;

import com.untamedrealms.arsenal.UntamedArsenal;
import com.untamedrealms.arsenal.block.Station;
import com.untamedrealms.arsenal.data.StationRecipe;
import com.untamedrealms.arsenal.network.ArsenalNetwork;
import com.untamedrealms.arsenal.station.StationLogic;
import com.untamedrealms.core.client.ui.UiKit;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.client.ClientSkills;
import com.untamedrealms.skills.events.SmithingQuality;
import com.untamedrealms.skills.registry.SkillsComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Forge / tanning rack: recipes on the left (filter by category tab), the selected recipe's
 * materials on the right with what you have. Workbench: your gear on the left, tempering on the right.
 */
public class StationScreen extends Screen {
    private static final int ROW = 20;
    private final BlockPos pos;
    private final Station station;
    private int left, top, panelW, panelH, listW, listTop, visibleRows;
    private int scroll;
    private int selected;
    private String category = "";
    private final List<Button> actionButtons = new ArrayList<>();

    public StationScreen(BlockPos pos, Station station) {
        super(Component.translatable("block.urarsenal." + station.id));
        this.pos = pos;
        this.station = station;
    }

    // ------------------------------------------------------------------ data

    private List<Map.Entry<ResourceLocation, StationRecipe>> recipes() {
        List<Map.Entry<ResourceLocation, StationRecipe>> out = new ArrayList<>();
        for (Map.Entry<ResourceLocation, StationRecipe> e : UntamedArsenal.RECIPES.entries().entrySet()) {
            if (e.getValue().station() == station && (category.isEmpty() || e.getValue().category().equals(category))) out.add(e);
        }
        out.sort(Comparator.comparingInt((Map.Entry<ResourceLocation, StationRecipe> e) -> e.getValue().level())
                .thenComparing(e -> e.getValue().resultStack().getHoverName().getString()));
        return out;
    }

    private Set<String> categories() {
        Set<String> out = new LinkedHashSet<>();
        for (StationRecipe r : UntamedArsenal.RECIPES.values()) if (r.station() == station) out.add(r.category());
        return out;
    }

    /** Workbench: inventory + armor slots holding gear. */
    private List<Integer> gearSlots() {
        List<Integer> out = new ArrayList<>();
        Inventory inv = minecraft.player.getInventory();
        for (int i = 0; i < 40; i++) if (SmithingQuality.isGear(inv.getItem(i))) out.add(i);
        return out;
    }

    private int rows() {
        return station == Station.WORKBENCH ? gearSlots().size() : recipes().size();
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        panelW = Math.min(width - 16, 400);
        panelH = Math.min(height - 16, 236);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        listW = 180;
        listTop = top + (station == Station.WORKBENCH ? 22 : 40);
        visibleRows = (top + panelH - 8 - listTop) / ROW;

        if (station != Station.WORKBENCH) {
            List<String> cats = new ArrayList<>(categories());
            cats.add(0, "");
            int x = left + 8;
            for (String cat : cats) {
                Component label = Component.translatable(cat.isEmpty() ? "screen.urarsenal.all" : "screen.urarsenal.category." + cat);
                int w = font.width(label) + 10;
                addRenderableWidget(Button.builder(label, b -> { category = cat; selected = 0; scroll = 0; }).bounds(x, top + 20, w, 16).build());
                x += w + 2;
            }
        }
        int bx = left + listW + 16, by = top + panelH - 26;
        actionButtons.clear();
        if (station == Station.WORKBENCH) {
            actionButtons.add(addRenderableWidget(Button.builder(Component.translatable("screen.urarsenal.temper"), b -> temper())
                    .bounds(bx, by, panelW - listW - 24, 18).build()));
        } else {
            int w = (panelW - listW - 28) / 2;
            actionButtons.add(addRenderableWidget(Button.builder(Component.translatable("screen.urarsenal.craft"), b -> craft(1))
                    .bounds(bx, by, w, 18).build()));
            actionButtons.add(addRenderableWidget(Button.builder(Component.translatable("screen.urarsenal.craft_five"), b -> craft(5))
                    .bounds(bx + w + 4, by, w, 18).build()));
        }
    }

    private void craft(int times) {
        List<Map.Entry<ResourceLocation, StationRecipe>> list = recipes();
        if (selected < list.size()) PacketDistributor.sendToServer(new ArsenalNetwork.Craft(pos, list.get(selected).getKey(), times));
    }

    private void temper() {
        List<Integer> slots = gearSlots();
        if (selected < slots.size()) PacketDistributor.sendToServer(new ArsenalNetwork.Temper(pos, slots.get(selected)));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        UiKit.panel(g, left, top, panelW, panelH);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        UiKit.heading(g, font, title.copy().withStyle(ChatFormatting.BOLD), left, top + 7, panelW);
        int total = rows();
        selected = Mth.clamp(selected, 0, Math.max(0, total - 1));
        scroll = Mth.clamp(scroll, 0, Math.max(0, total - visibleRows));
        if (total == 0) {
            g.drawCenteredString(font, Component.translatable(station == Station.WORKBENCH ? "screen.urarsenal.no_gear" : "screen.urarsenal.no_recipes"),
                    left + panelW / 2, top + panelH / 2, UiKit.TEXT_DIM);
            actionButtons.forEach(b -> b.active = false);
            return;
        }
        ItemStack tooltip = ItemStack.EMPTY;
        int x = left + 8;
        if (station == Station.WORKBENCH) {
            List<Integer> slots = gearSlots();
            for (int i = scroll; i < Math.min(total, scroll + visibleRows); i++) {
                ItemStack stack = minecraft.player.getInventory().getItem(slots.get(i));
                int y = listTop + (i - scroll) * ROW;
                boolean hover = UiKit.inside(mouseX, mouseY, x, y, listW, ROW - 2);
                g.fill(x, y, x + listW, y + ROW - 2, i == selected ? UiKit.BG_SELECTED : hover ? UiKit.BG_HOVER : UiKit.BG_INNER);
                g.renderItem(stack, x + 2, y + 1);
                g.drawString(font, font.split(stack.getHoverName(), listW - 24).get(0), x + 21, y + 5, UiKit.TEXT, false);
                if (hover) tooltip = stack;
            }
            renderTemper(g, minecraft.player.getInventory().getItem(slots.get(selected)));
        } else {
            List<Map.Entry<ResourceLocation, StationRecipe>> list = recipes();
            for (int i = scroll; i < Math.min(total, scroll + visibleRows); i++) {
                StationRecipe r = list.get(i).getValue();
                ItemStack result = r.resultStack();
                int y = listTop + (i - scroll) * ROW;
                boolean hover = UiKit.inside(mouseX, mouseY, x, y, listW, ROW - 2);
                g.fill(x, y, x + listW, y + ROW - 2, i == selected ? UiKit.BG_SELECTED : hover ? UiKit.BG_HOVER : UiKit.BG_INNER);
                g.renderItem(result, x + 2, y + 1);
                boolean able = ClientSkills.data().level(r.skill()) >= r.level();
                String lvl = String.valueOf(r.level());
                g.drawString(font, font.split(result.getHoverName(), listW - 30 - font.width(lvl)).get(0), x + 21, y + 5, able ? UiKit.TEXT : UiKit.TEXT_DIM, false);
                g.drawString(font, lvl, x + listW - font.width(lvl) - 4, y + 5, able ? UiKit.GOLD : UiKit.BAD, false);
                if (hover) tooltip = result;
            }
            tooltip = renderRecipe(g, list.get(selected).getValue(), mouseX, mouseY, tooltip);
        }
        if (total > visibleRows) {
            int barH = Math.max(10, visibleRows * ROW * visibleRows / total);
            int barY = listTop + (visibleRows * ROW - barH) * scroll / Math.max(1, total - visibleRows);
            g.fill(x + listW + 2, barY, x + listW + 4, barY + barH, UiKit.TRIM);
        }
        if (!tooltip.isEmpty()) g.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    private ItemStack renderRecipe(GuiGraphics g, StationRecipe r, int mouseX, int mouseY, ItemStack tooltip) {
        int x = left + listW + 16, y = top + 22;
        ItemStack result = r.resultStack();
        g.renderItem(result, x, y);
        g.drawString(font, result.getHoverName(), x + 20, y + 4, UiKit.GOLD, true);
        y += 22;
        int have = ClientSkills.data().level(r.skill());
        g.drawString(font, Component.translatable("screen.urarsenal.requires", r.skill().displayName(), r.level()), x, y,
                have >= r.level() ? UiKit.GOOD : UiKit.BAD, false);
        y += 14;
        g.drawString(font, Component.translatable("screen.urarsenal.materials"), x, y, UiKit.TEXT_DIM, false);
        y += 12;
        boolean all = have >= r.level();
        for (StationRecipe.Input in : r.inputs()) {
            ItemStack icon = in.icon();
            int count = StationLogic.count(minecraft.player.getInventory(), in);
            g.renderItem(icon, x, y);
            if (UiKit.inside(mouseX, mouseY, x, y, 16, 16)) tooltip = icon;
            Component name = in.item().startsWith("#") ? Component.translatable("screen.urarsenal.any", icon.getHoverName()) : icon.getHoverName();
            g.drawString(font, font.split(name, panelW - listW - 80).get(0), x + 20, y + 4, UiKit.TEXT, false);
            String amount = count + "/" + in.count();
            g.drawString(font, amount, left + panelW - 12 - font.width(amount), y + 4, count >= in.count() ? UiKit.GOOD : UiKit.BAD, false);
            all &= count >= in.count();
            y += 18;
        }
        for (Button b : actionButtons) b.active = all;
        return tooltip;
    }

    private void renderTemper(GuiGraphics g, ItemStack gear) {
        int x = left + listW + 16, y = top + 22;
        g.renderItem(gear, x, y);
        g.drawString(font, gear.getHoverName(), x + 20, y + 4, UiKit.GOLD, true);
        y += 24;
        int quality = gear.getOrDefault(SkillsComponents.QUALITY.get(), 0);
        int max = StationLogic.maxTemper(ClientSkills.data().level(Skill.SMITHING));
        Component current = quality == 0 ? Component.translatable("screen.urarsenal.quality_none") : Component.translatable("quality.urskills." + quality);
        g.drawString(font, Component.translatable("screen.urarsenal.quality", current), x, y, UiKit.TEXT, false);
        y += 12;
        Component cap = Component.translatable("quality.urskills." + max);
        g.drawString(font, Component.translatable("screen.urarsenal.quality_max", cap), x, y, UiKit.TEXT_DIM, false);
        y += 16;
        boolean hasMaterial = false;
        for (ItemStack stack : minecraft.player.getInventory().items) {
            if (stack != gear && !stack.isEmpty() && gear.getItem().isValidRepairItem(gear, stack)) {
                hasMaterial = true;
                g.renderItem(stack, x, y);
                g.drawString(font, Component.translatable("screen.urarsenal.temper_cost", stack.getHoverName()), x + 20, y + 4, UiKit.GOOD, false);
                break;
            }
        }
        if (!hasMaterial) {
            for (FormattedLine line : FormattedLine.of(font, Component.translatable("screen.urarsenal.temper_need"), panelW - listW - 28)) {
                g.drawString(font, line.text(), x, y + 4, UiKit.BAD, false);
                y += 10;
            }
        }
        for (Button b : actionButtons) b.active = hasMaterial && quality < max;
    }

    private record FormattedLine(net.minecraft.util.FormattedCharSequence text) {
        static List<FormattedLine> of(net.minecraft.client.gui.Font font, Component text, int width) {
            return font.split(text, width).stream().map(FormattedLine::new).toList();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = left + 8;
        if (mouseX >= x && mouseX < x + listW && mouseY >= listTop) {
            int i = scroll + (int) ((mouseY - listTop) / ROW);
            if (i < rows() && i < scroll + visibleRows) {
                selected = i;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= (int) Math.signum(scrollY);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
