package com.untamedrealms.testhub;

import com.untamedrealms.quests.block.Bounties;
import com.untamedrealms.quests.data.QuestDef;
import com.untamedrealms.quests.data.QuestsData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** The test hub's control book: every line runs a shortcut command when clicked. */
public final class ControlBook {
    private static final int QUESTS_PER_PAGE = 8;

    private ControlBook() {}

    public static ItemStack create(MinecraftServer server) {
        List<Component> pages = new ArrayList<>();
        pages.add(page(Component.literal("Untamed Realms\nTEST HUB\n\n").withStyle(ChatFormatting.BOLD),
                Component.literal("Click a line to run it.\n\n"),
                Component.literal("p2 Character\np3 Magic & world\np4 Alchemy & enchanting\np5 Quests\n\n"),
                Component.literal("Everything here is also a command: /ur test ...").withStyle(ChatFormatting.DARK_GRAY)));
        pages.add(page(heading("Character"),
                entry("Reset my character", "/ur test reset", "Class, skills, perks, spells, quests, coins and inventory"),
                entry("Choose class again", "/ur class reset @s", "Opens character creation"),
                entry("All skills 1", "/ur test skills 1", null),
                entry("All skills 25", "/ur test skills 25", null),
                entry("All skills 50", "/ur test skills 50", null),
                entry("All skills 99", "/ur test skills 99", null),
                entry("Refund perks", "/ur skills respec @s", null),
                entry("+1000 Crowns", "/ur wallet give @s 1000", null),
                entry("Refill vitals", "/ur vitals refill @s", "Health, Magicka and Stamina")));
        pages.add(page(heading("Magic & world"),
                entry("Learn every spell", "/ur magic learnall @s", null),
                entry("Forget all spells", "/ur magic forget @s", null),
                entry("Day", "/time set 6000", null),
                entry("Night", "/time set 18000", "Time is frozen in the test world"),
                entry("Next day", "/time add 24000", "New bounties on the notice boards"),
                entry("Clear weather", "/weather clear", null),
                entry("Rain", "/weather rain", null),
                entry("Survival", "/gamemode survival", null),
                entry("Creative", "/gamemode creative", null),
                entry("Back to the hub", "/ur test hub", null),
                entry("Village below", "/ur test village", null),
                entry("Rebuild the hub", "/ur test rebuild", "Restocks chests, regrows trees and ores, respawns NPCs")));
        pages.add(page(heading("Alchemy & enchanting"),
                entry("Know every ingredient & enchantment", "/ur arcana learnall @s", "All ingredient effects and every enchantment"),
                entry("Forget them all", "/ur arcana forget @s", "Start discovering from scratch"),
                entry("Fill my soul gems", "/ur arcana fillgems @s", "Each empty gem you carry gets its biggest soul"),
                entry("Learn Soul Trap", "/ur magic learn @s urarcana:soul_trap", null),
                entry("Alchemy 50", "/ur skills set @s alchemy 50", null),
                entry("Enchanting 50", "/ur skills set @s enchanting 50", null)));

        List<ResourceLocation> quests = new ArrayList<>(QuestsData.QUESTS.entries().keySet());
        Set<ResourceLocation> bounties = Set.copyOf(QuestsData.inPool(Bounties.POOL));
        quests.removeIf(bounties::contains);
        quests.sort(null);
        List<Component> lines = new ArrayList<>();
        lines.add(heading("Quests"));
        lines.add(entry("Skip tracked step", "/ur quest skip @s", "Finishes the current step of the quest you track"));
        lines.add(entry("Reset all quests", "/ur quest reset @s", null));
        lines.add(Component.literal("Start:\n").withStyle(ChatFormatting.DARK_GRAY));
        for (ResourceLocation id : quests) {
            if (lines.size() >= QUESTS_PER_PAGE + 4) {
                pages.add(page(lines.toArray(Component[]::new)));
                lines = new ArrayList<>();
                lines.add(heading("Quests"));
            }
            QuestDef def = QuestsData.get(id);
            String title = def == null ? id.getPath() : def.title().getString();
            lines.add(entry(title, "/ur quest start @s " + id, id.toString()));
        }
        pages.add(page(lines.toArray(Component[]::new)));

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Test Hub controls"),
                "Untamed Realms", 0, pages.stream().map(Filterable::passThrough).toList(), true));
        return book;
    }

    private static Component page(Component... parts) {
        MutableComponent page = Component.empty();
        for (Component part : parts) page.append(part);
        return page;
    }

    private static Component heading(String text) {
        return Component.literal(text + "\n\n").withStyle(ChatFormatting.BOLD);
    }

    private static Component entry(String label, String command, String hover) {
        Style style = Style.EMPTY.withColor(ChatFormatting.DARK_BLUE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal(hover == null ? command : hover + "\n" + command)));
        return Component.literal("> " + label + "\n").withStyle(style);
    }
}
