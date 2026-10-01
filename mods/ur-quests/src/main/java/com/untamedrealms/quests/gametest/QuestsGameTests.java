package com.untamedrealms.quests.gametest;

import com.untamedrealms.quests.UntamedQuests;
import com.untamedrealms.quests.data.QuestDef;
import com.untamedrealms.quests.data.QuestLog;
import com.untamedrealms.quests.data.QuestsData;
import com.untamedrealms.quests.engine.QuestApi;
import com.untamedrealms.quests.engine.Targets;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

@GameTestHolder(UntamedQuests.MODID)
@PrefixGameTestTemplate(false)
public class QuestsGameTests {
    @GameTest(template = "arena")
    public static void questsLoadAndReferenceEachOther(GameTestHelper helper) {
        helper.assertTrue(QuestsData.QUESTS.entries().size() >= 20, "expected the default quests, got " + QuestsData.QUESTS.entries().size());
        for (Map.Entry<ResourceLocation, QuestDef> e : QuestsData.QUESTS.entries().entrySet()) {
            helper.assertTrue(!e.getValue().stages().isEmpty(), e.getKey() + " has no stages");
            for (ResourceLocation req : e.getValue().requirements().quests()) {
                helper.assertTrue(QuestsData.QUESTS.contains(req), e.getKey() + " requires missing quest " + req);
            }
            for (QuestDef.Stage stage : e.getValue().stages()) {
                helper.assertTrue(!stage.objectives().isEmpty(), e.getKey() + " has a stage without objectives");
            }
        }
        helper.assertTrue(!QuestsData.inPool("bounty").isEmpty(), "bounty pool is empty");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void targetMatching(GameTestHelper helper) {
        helper.assertTrue(Targets.entity("minecraft:zombie", EntityType.ZOMBIE), "exact entity");
        helper.assertTrue(Targets.entity("#minecraft:undead", EntityType.SKELETON), "entity tag");
        helper.assertTrue(Targets.entity("minecraft:spider|minecraft:cave_spider", EntityType.CAVE_SPIDER), "alternatives");
        helper.assertTrue(!Targets.entity("minecraft:zombie", EntityType.CREEPER), "non-match");
        helper.assertTrue(Targets.item("#minecraft:logs", new ItemStack(Items.OAK_LOG)), "item tag");
        helper.assertTrue(Targets.item("*", new ItemStack(Items.DIRT)), "wildcard");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void questLogRoundTrips(GameTestHelper helper) {
        QuestLog log = new QuestLog();
        QuestLog.Active active = new QuestLog.Active(1, 2, 100);
        active.progress[1] = 5;
        log.active().put(UntamedQuests.id("x"), active);
        log.completed().put(UntamedQuests.id("y"), new QuestLog.Completion(3, 500));
        log.setTracked(UntamedQuests.id("x"));
        QuestLog copy = new QuestLog();
        copy.deserializeNBT(helper.getLevel().registryAccess(), log.serializeNBT(helper.getLevel().registryAccess()));
        helper.assertTrue(copy.active().get(UntamedQuests.id("x")).progress[1] == 5, "progress survives");
        helper.assertTrue(copy.completed().get(UntamedQuests.id("y")).times() == 3, "completion survives");
        helper.assertTrue(UntamedQuests.id("x").equals(copy.tracked()), "tracking survives");
        helper.assertTrue(QuestApi.describe(new QuestDef.Objective("kill", "minecraft:zombie", 3, false, java.util.Optional.empty())) != null, "describe works");
        helper.succeed();
    }
}
