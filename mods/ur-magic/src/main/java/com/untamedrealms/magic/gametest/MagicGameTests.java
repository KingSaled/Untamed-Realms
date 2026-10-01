package com.untamedrealms.magic.gametest;

import com.untamedrealms.magic.UntamedMagic;
import com.untamedrealms.magic.data.SpellBook;
import com.untamedrealms.magic.data.SpellDef;
import com.untamedrealms.magic.item.SpellTomeItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;

@GameTestHolder(UntamedMagic.MODID)
@PrefixGameTestTemplate(false)
public class MagicGameTests {
    private static final Set<String> KINDS = Set.of("projectile", "cone", "lightning", "self", "area", "summon", "bound_weapon");

    @GameTest(template = "arena")
    public static void spellsLoadWithKnownKinds(GameTestHelper helper) {
        helper.assertTrue(UntamedMagic.SPELLS.entries().size() >= 20, "expected the default spells");
        for (Map.Entry<ResourceLocation, SpellDef> e : UntamedMagic.SPELLS.entries().entrySet()) {
            helper.assertTrue(KINDS.contains(e.getValue().kind()), e.getKey() + " has unknown kind " + e.getValue().kind());
            helper.assertTrue(e.getValue().cost() > 0, e.getKey() + " is free");
        }
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void spellbookSlotsAndTomes(GameTestHelper helper) {
        SpellBook book = new SpellBook();
        ResourceLocation a = UntamedMagic.id("firebolt"), b = UntamedMagic.id("healing");
        helper.assertTrue(book.learn(a) && book.learn(b) && !book.learn(a), "learning is idempotent");
        helper.assertTrue(a.equals(book.slot(0)) && b.equals(book.slot(1)), "new spells auto-fill quick slots");
        book.setSlot(3, a);
        helper.assertTrue(book.slot(0) == null && a.equals(book.slot(3)), "a spell occupies one slot at a time");
        book.select(1);
        book.cycle(1);
        helper.assertTrue(book.selected() == 3, "cycling skips empty slots");
        helper.assertTrue(SpellTomeItem.spell(SpellTomeItem.of(a)) != null, "tome resolves its spell");
        helper.succeed();
    }
}
