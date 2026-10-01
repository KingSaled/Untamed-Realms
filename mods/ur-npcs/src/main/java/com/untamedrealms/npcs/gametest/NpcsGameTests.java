package com.untamedrealms.npcs.gametest;

import com.untamedrealms.npcs.UntamedNpcs;
import com.untamedrealms.npcs.data.DialogueDef;
import com.untamedrealms.npcs.data.NpcDef;
import com.untamedrealms.npcs.data.NpcsData;
import com.untamedrealms.npcs.data.SettlementDef;
import com.untamedrealms.npcs.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

@GameTestHolder(UntamedNpcs.MODID)
@PrefixGameTestTemplate(false)
public class NpcsGameTests {
    @GameTest(template = "arena")
    public static void contentIsConsistent(GameTestHelper helper) {
        helper.assertTrue(NpcsData.NPCS.entries().size() >= 12, "expected the default NPCs");
        for (Map.Entry<ResourceLocation, NpcDef> e : NpcsData.NPCS.entries().entrySet()) {
            NpcDef def = e.getValue();
            def.dialogue().ifPresent(d -> helper.assertTrue(NpcsData.DIALOGUES.contains(d), e.getKey() + " -> missing dialogue " + d));
            def.shop().ifPresent(s -> helper.assertTrue(NpcsData.SHOPS.contains(s), e.getKey() + " -> missing shop " + s));
        }
        for (Map.Entry<ResourceLocation, DialogueDef> e : NpcsData.DIALOGUES.entries().entrySet()) {
            DialogueDef d = e.getValue();
            for (DialogueDef.Entry entry : d.entry()) helper.assertTrue(d.nodes().containsKey(entry.node()), e.getKey() + " entry -> missing node " + entry.node());
            for (DialogueDef.Node node : d.nodes().values()) {
                for (DialogueDef.Option option : node.options()) {
                    option.next().ifPresent(n -> helper.assertTrue(d.nodes().containsKey(n), e.getKey() + " -> missing node " + n));
                    option.check().ifPresent(c -> helper.assertTrue(d.nodes().containsKey(c.fail()), e.getKey() + " -> missing fail node " + c.fail()));
                }
            }
        }
        for (SettlementDef s : NpcsData.SETTLEMENTS.values()) {
            for (ResourceLocation id : s.always()) helper.assertTrue(NpcsData.NPCS.contains(id), "settlement references missing NPC " + id);
            for (ResourceLocation id : s.random()) helper.assertTrue(NpcsData.NPCS.contains(id), "settlement references missing NPC " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void essentialNpcsSurviveDamage(GameTestHelper helper) {
        NpcEntity npc = helper.spawn(UntamedNpcs.NPC.get(), new BlockPos(2, 1, 2));
        ResourceLocation elder = UntamedNpcs.id("village_elder");
        npc.setup(elder, NpcsData.NPCS.getOrNull(elder));
        float before = npc.getHealth();
        npc.hurt(helper.getLevel().damageSources().generic(), 1000f);
        helper.assertTrue(npc.isAlive() && npc.getHealth() == before, "essential NPC took damage");
        helper.assertTrue(npc.getCustomName() != null, "NPC was named");
        helper.succeed();
    }
}
