package com.untamedrealms.skills.gametest;

import com.untamedrealms.skills.UntamedSkills;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillMath;
import com.untamedrealms.skills.data.SkillData;
import com.untamedrealms.skills.data.SkillsData;
import com.untamedrealms.skills.data.XpSource;
import com.untamedrealms.skills.events.XpEvents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(UntamedSkills.MODID)
@PrefixGameTestTemplate(false)
public class SkillsGameTests {
    @GameTest(template = "arena")
    public static void xpCurveMatchesRuneScape(GameTestHelper helper) {
        double scale = SkillMath.scale();
        helper.assertTrue(Math.abs(SkillMath.xpForLevel(2) - 83 * scale) < 1e-6, "level 2 needs 83 (scaled) xp");
        helper.assertTrue(Math.abs(SkillMath.xpForLevel(99) - 13_034_431 * scale) < 1, "level 99 needs 13,034,431 (scaled) xp");
        for (int level : new int[]{1, 2, 10, 50, 98, 99}) {
            helper.assertTrue(SkillMath.levelForXp(SkillMath.xpForLevel(level)) == level, "round trip level " + level);
        }
        helper.assertTrue(SkillMath.levelForXp(SkillMath.xpForLevel(30) - 0.01) == 29, "just below a level");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void everySkillHasPerks(GameTestHelper helper) {
        for (Skill skill : Skill.VALUES) {
            helper.assertTrue(!SkillsData.perks(skill).isEmpty(), "no perks loaded for " + skill.id());
            for (SkillsData.PerkRef ref : SkillsData.perks(skill)) {
                for (var req : ref.requires()) {
                    helper.assertTrue(SkillsData.perk(req) != null, ref.id() + " requires unknown perk " + req);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void xpTablesAndRequirementsLoad(GameTestHelper helper) {
        // other modules add their own mining tables (e.g. ur-arsenal's ores), so look through all of them
        var mining = SkillsData.xpSources(XpSource.Trigger.BREAK_BLOCK).stream()
                .filter(s -> s.skill() == Skill.MINING).toList();
        helper.assertTrue(!mining.isEmpty(), "mining xp table loaded");
        XpSource.Entry diamond = mining.stream().map(s -> SkillsData.bestEntry(s, Blocks.DIAMOND_ORE.defaultBlockState()))
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        helper.assertTrue(diamond != null && diamond.xp() == 50, "diamond ore gives 50 mining xp (most specific entry wins)");
        helper.assertTrue(!SkillsData.requirements(Blocks.DIAMOND_ORE.defaultBlockState()).isEmpty(), "diamond ore has a mining requirement");
        helper.assertTrue(SkillsData.grantsBreakXp(Blocks.OAK_LOG.defaultBlockState()), "oak logs grant woodcutting xp");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void cropsOnlyCountWhenMature(GameTestHelper helper) {
        helper.assertTrue(!XpEvents.isMature(Blocks.WHEAT.defaultBlockState()), "fresh wheat is not mature");
        helper.assertTrue(XpEvents.isMature(Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7)), "age 7 wheat is mature");
        helper.assertTrue(XpEvents.isMature(Blocks.STONE.defaultBlockState()), "blocks without age are always 'mature'");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void skillDataRoundTrips(GameTestHelper helper) {
        SkillData data = new SkillData();
        data.setXp(Skill.MINING, 12345);
        data.setCharacterLevel(7);
        data.addPerkPoints(3);
        data.addPerk(UntamedSkills.id("prospector_1"));
        SkillData copy = new SkillData();
        copy.deserializeNBT(helper.getLevel().registryAccess(), data.serializeNBT(helper.getLevel().registryAccess()));
        helper.assertTrue(copy.xp(Skill.MINING) == 12345, "xp survives");
        helper.assertTrue(copy.characterLevel() == 7 && copy.perkPoints() == 3, "character survives");
        helper.assertTrue(copy.hasPerk(UntamedSkills.id("prospector_1")), "perks survive");
        helper.succeed();
    }
}
