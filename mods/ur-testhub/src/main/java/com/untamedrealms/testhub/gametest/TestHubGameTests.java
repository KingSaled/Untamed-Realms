package com.untamedrealms.testhub.gametest;

import com.untamedrealms.arsenal.ArsenalArmor;
import com.untamedrealms.arsenal.ArsenalBlocks;
import com.untamedrealms.npcs.entity.NpcEntity;
import com.untamedrealms.testhub.ControlBook;
import com.untamedrealms.testhub.HubBuilder;
import com.untamedrealms.testhub.UntamedTestHub;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(UntamedTestHub.MODID)
@PrefixGameTestTemplate(false)
public final class TestHubGameTests {
    private TestHubGameTests() {}

    /** Builds the whole hub far from the other tests and checks that each quarter is there. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void hubBuilds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos c = new BlockPos(4000, -30, 4000);
        HubBuilder.build(level, c);
        helper.assertTrue(level.getBlockState(c.offset(-6, 1, -6)).is(ArsenalBlocks.FORGE.get()), "forge in the forge yard");
        helper.assertTrue(level.getBlockState(c.offset(26, 1, -12)).is(com.untamedrealms.arcana.UntamedArcana.ARCANE_ENCHANTER.get()), "arcane enchanter in the magic quarter");
        helper.assertTrue(level.getBlockState(c.offset(22, 1, -12)).is(com.untamedrealms.arcana.UntamedArcana.ALCHEMY_LAB.get()), "alchemy lab in the magic quarter");
        helper.assertTrue(level.getBlockEntity(c.offset(16, 1, -8)) instanceof ChestBlockEntity chest && !chest.isEmpty(), "ingredient chest stocked");
        helper.assertTrue(level.getBlockState(c.offset(-8, 1, 30)).getBlock().getDescriptionId().contains("notice_board"), "notice board in town");
        helper.assertTrue(level.getBlockEntity(c.offset(-6, 1, -14)) instanceof ChestBlockEntity chest && !chest.isEmpty(), "weapon chest stocked");
        helper.assertTrue(level.getBlockEntity(c.offset(6, 1, -6)) instanceof ChestBlockEntity chest && !chest.isEmpty(), "magic chest stocked");
        helper.assertTrue(level.getBlockState(c.offset(28, 1, 26)).is(ArsenalBlocks.EBONY_ORE.get()), "ebony ore on the ore wall");
        helper.assertTrue(ControlBook.create(level.getServer()).has(DataComponents.WRITTEN_BOOK_CONTENT), "control book");
        AABB box = new AABB(c.getX() - 41, c.getY() - 4, c.getZ() - 41, c.getX() + 41, c.getY() + 23, c.getZ() + 41);
        helper.succeedWhen(() -> {
            int npcs = level.getEntitiesOfClass(NpcEntity.class, box).size();
            helper.assertTrue(npcs == HubBuilder.npcCount(), "every NPC in town (" + npcs + "/" + HubBuilder.npcCount() + ")");
            int stands = level.getEntitiesOfClass(ArmorStand.class, box).size();
            helper.assertTrue(stands == ArsenalArmor.Set.values().length, "an armor stand per set (" + stands + ")");
            helper.assertTrue(level.getEntitiesOfClass(Husk.class, box).size() == 3, "three training dummies");
        });
    }
}
