package com.untamedrealms.arcana.client;

import com.untamedrealms.arcana.UntamedArcana;
import com.untamedrealms.arcana.alchemy.Alchemy;
import com.untamedrealms.arcana.data.AlchemyEffect;
import com.untamedrealms.arcana.data.ArcanaKnowledge;
import com.untamedrealms.arcana.data.IngredientDef;
import com.untamedrealms.arcana.network.ArcanaNetwork;
import com.untamedrealms.core.client.ui.UiKit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The alchemy lab: your ingredients on the left (click up to three), on the right each chosen
 * ingredient's effects (unknown ones as "?") and the effects the brew will have, as far as you know.
 */
public class AlchemyScreen extends Screen {
    private static final int ROW = 20;
    private final BlockPos pos;
    private int left, top, panelW, panelH, listW, listTop, visibleRows, scroll;
    private final List<ResourceLocation> chosen = new ArrayList<>();
    private ResourceLocation focused;
    private Button taste, brew, brewFive, clear;

    public AlchemyScreen(BlockPos pos) {
        super(Component.translatable("block.urarcana.alchemy_lab"));
        this.pos = pos;
    }

    /** Ingredients in the main inventory, with counts, by name. */
    private List<Map.Entry<ResourceLocation, Integer>> ingredients() {
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        for (ItemStack stack : minecraft.player.getInventory().items) {
            IngredientDef def = Alchemy.ingredient(stack);
            if (def != null) counts.merge(def.item(), stack.getCount(), Integer::sum);
        }
        List<Map.Entry<ResourceLocation, Integer>> out = new ArrayList<>(counts.entrySet());
        out.sort(Comparator.comparing(e -> icon(e.getKey()).getHoverName().getString()));
        return out;
    }

    private static ItemStack icon(ResourceLocation item) {
        return new ItemStack(BuiltInRegistries.ITEM.get(item));
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 16, 400);
        panelH = Math.min(height - 16, 236);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        listW = 170;
        listTop = top + 22;
        visibleRows = (top + panelH - 8 - listTop) / ROW;
        int bx = left + listW + 16, by = top + panelH - 26, w = (panelW - listW - 32) / 4;
        taste = addRenderableWidget(Button.builder(Component.translatable("screen.urarcana.taste"), b -> {
            if (focused != null) PacketDistributor.sendToServer(new ArcanaNetwork.Taste(pos, focused));
        }).bounds(bx, by, w, 18).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.urarcana.taste_hint"))).build());
        brew = addRenderableWidget(Button.builder(Component.translatable("screen.urarcana.brew"), b -> brew(1)).bounds(bx + (w + 4), by, w, 18).build());
        brewFive = addRenderableWidget(Button.builder(Component.translatable("screen.urarcana.brew_five"), b -> brew(5)).bounds(bx + 2 * (w + 4), by, w, 18).build());
        clear = addRenderableWidget(Button.builder(Component.translatable("screen.urarcana.clear"), b -> chosen.clear()).bounds(bx + 3 * (w + 4), by, w, 18).build());
    }

    private void brew(int times) {
        if (chosen.size() >= 2) PacketDistributor.sendToServer(new ArcanaNetwork.Brew(pos, List.copyOf(chosen), times));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        UiKit.panel(g, left, top, panelW, panelH);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        List<Map.Entry<ResourceLocation, Integer>> list = ingredients();
        chosen.removeIf(id -> list.stream().noneMatch(e -> e.getKey().equals(id)));
        if (focused != null && list.stream().noneMatch(e -> e.getKey().equals(focused))) focused = null;
        taste.active = focused != null;
        brew.active = brewFive.active = chosen.size() >= 2;
        clear.active = !chosen.isEmpty();
        super.render(g, mouseX, mouseY, partialTick);
        UiKit.heading(g, font, title.copy().withStyle(ChatFormatting.BOLD), left, top + 7, panelW);

        int total = list.size();
        scroll = Mth.clamp(scroll, 0, Math.max(0, total - visibleRows));
        int x = left + 8;
        ItemStack tooltip = ItemStack.EMPTY;
        if (total == 0) {
            UiKit.wrapped(g, font, Component.translatable("screen.urarcana.no_ingredients"), x, listTop + 4, listW, UiKit.TEXT_DIM);
        }
        for (int i = scroll; i < Math.min(total, scroll + visibleRows); i++) {
            ResourceLocation id = list.get(i).getKey();
            ItemStack stack = icon(id);
            int y = listTop + (i - scroll) * ROW;
            boolean hover = UiKit.inside(mouseX, mouseY, x, y, listW, ROW - 2);
            boolean picked = chosen.contains(id);
            g.fill(x, y, x + listW, y + ROW - 2, picked ? UiKit.BG_SELECTED : hover ? UiKit.BG_HOVER : UiKit.BG_INNER);
            if (id.equals(focused)) UiKit.frame(g, x, y, listW, ROW - 2, UiKit.TRIM);
            g.renderItem(stack, x + 2, y + 1);
            String count = "x" + list.get(i).getValue();
            g.drawString(font, font.split(stack.getHoverName(), listW - 30 - font.width(count)).get(0), x + 21, y + 5, picked ? UiKit.GOLD : UiKit.TEXT, false);
            g.drawString(font, count, x + listW - font.width(count) - 4, y + 5, UiKit.TEXT_DIM, false);
            if (hover) tooltip = stack;
        }
        if (total > visibleRows) {
            int barH = Math.max(10, visibleRows * ROW * visibleRows / total);
            int barY = listTop + (visibleRows * ROW - barH) * scroll / Math.max(1, total - visibleRows);
            g.fill(x + listW + 2, barY, x + listW + 4, barY + barH, UiKit.TRIM);
        }
        renderDetails(g);
        if (!tooltip.isEmpty()) g.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    private Component effectName(ResourceLocation id) {
        AlchemyEffect def = UntamedArcana.EFFECTS.getOrNull(id);
        if (def == null) return Component.literal(id.toString());
        return def.name().copy().withStyle(def.harmful() ? ChatFormatting.RED : ChatFormatting.GREEN);
    }

    private void renderDetails(GuiGraphics g) {
        int x = left + listW + 16, y = top + 22, w = panelW - listW - 24;
        ArcanaKnowledge knowledge = ClientArcana.knowledge();
        List<ResourceLocation> show = new ArrayList<>(chosen);
        if (show.isEmpty() && focused != null) show.add(focused);
        if (show.isEmpty()) {
            y = UiKit.wrapped(g, font, Component.translatable("screen.urarcana.alchemy_help"), x, y, w, UiKit.TEXT_DIM);
            return;
        }
        List<IngredientDef> defs = new ArrayList<>();
        for (ResourceLocation id : show) {
            IngredientDef def = Alchemy.ingredient(id);
            if (def == null) continue;
            defs.add(def);
            g.renderItem(icon(id), x, y);
            g.drawString(font, icon(id).getHoverName(), x + 20, y + 4, UiKit.GOLD, false);
            y += 18;
            for (int i = 0; i < def.effects().size(); i++) {
                Component name = knowledge.knows(def.item(), i) ? effectName(def.effects().get(i)) : Component.literal("?").withStyle(ChatFormatting.DARK_GRAY);
                g.drawString(font, Component.literal(i % 2 == 0 ? "  " : "").append(name), x + 4 + (i % 2) * (w / 2), y, UiKit.TEXT, false);
                if (i % 2 == 1) y += 10;
            }
            y += 4;
        }
        if (defs.size() < 2) {
            UiKit.wrapped(g, font, Component.translatable("screen.urarcana.choose_more"), x, y + 2, w, UiKit.TEXT_DIM);
            return;
        }
        // what the player can tell the brew will do: shared effects known on at least two of the chosen ingredients
        List<ResourceLocation> shared = Alchemy.sharedEffects(defs);
        List<ResourceLocation> known = new ArrayList<>();
        for (ResourceLocation effect : shared) {
            int knownOn = 0;
            for (IngredientDef def : defs) {
                int i = def.effects().indexOf(effect);
                if (i >= 0 && knowledge.knows(def.item(), i)) knownOn++;
            }
            if (knownOn >= 2) known.add(effect);
        }
        g.drawString(font, Component.translatable("screen.urarcana.result"), x, y + 2, UiKit.TEXT_DIM, false);
        y += 14;
        if (known.isEmpty()) {
            UiKit.wrapped(g, font, Component.translatable("screen.urarcana.result_unknown"), x, y, w, UiKit.TEXT_DIM);
            return;
        }
        boolean poison = Alchemy.isPoison(known);
        g.drawString(font, Component.translatable(poison ? "screen.urarcana.makes_poison" : "screen.urarcana.makes_potion"), x, y,
                poison ? UiKit.BAD : UiKit.GOOD, false);
        y += 11;
        for (ResourceLocation effect : known) {
            g.drawString(font, Component.literal("  ").append(effectName(effect)), x, y, UiKit.TEXT, false);
            y += 10;
        }
        if (known.size() < shared.size()) g.drawString(font, Component.translatable("screen.urarcana.maybe_more"), x, y, UiKit.TEXT_DIM, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = left + 8;
        if (mouseX >= x && mouseX < x + listW && mouseY >= listTop) {
            List<Map.Entry<ResourceLocation, Integer>> list = ingredients();
            int i = scroll + (int) ((mouseY - listTop) / ROW);
            if (i < list.size() && i < scroll + visibleRows) {
                ResourceLocation id = list.get(i).getKey();
                focused = id;
                if (chosen.contains(id)) chosen.remove(id);
                else if (chosen.size() < 3) chosen.add(id);
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
