package com.untamedrealms.arsenal.gametest;

import com.untamedrealms.arsenal.ArsenalArmor;
import com.untamedrealms.arsenal.ArsenalBlocks;
import com.untamedrealms.arsenal.ArsenalItems;
import com.untamedrealms.arsenal.ArsenalTier;
import com.untamedrealms.arsenal.UntamedArsenal;
import com.untamedrealms.arsenal.WeaponType;
import com.untamedrealms.arsenal.data.StationRecipe;
import com.untamedrealms.arsenal.station.StationLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

@GameTestHolder(UntamedArsenal.MODID)
@PrefixGameTestTemplate(false)
public class ArsenalGameTests {
    @GameTest(template = "arena")
    public static void recipesResolve(GameTestHelper helper) {
        helper.assertTrue(UntamedArsenal.RECIPES.entries().size() >= 100, "expected the default station recipes");
        for (Map.Entry<ResourceLocation, StationRecipe> e : UntamedArsenal.RECIPES.entries().entrySet()) {
            helper.assertTrue(e.getValue().resultItem() != Items.AIR, e.getKey() + ": unknown result " + e.getValue().result());
            for (StationRecipe.Input in : e.getValue().inputs()) {
                helper.assertTrue(!in.icon().isEmpty(), e.getKey() + ": unknown input " + in.item());
            }
        }
        helper.succeed();
    }

    private static double damage(ItemStack stack) {
        double[] total = {0};
        stack.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND, (attr, mod) -> {
            if (attr.value() == Attributes.ATTACK_DAMAGE.value()) total[0] += mod.amount();
        });
        return total[0];
    }

    @GameTest(template = "arena")
    public static void tiersGetStronger(GameTestHelper helper) {
        for (WeaponType type : WeaponType.values()) {
            if (type == WeaponType.BOW) continue;
            double previous = -1;
            for (ArsenalTier tier : ArsenalTier.values()) {
                double d = damage(new ItemStack(ArsenalItems.weapon(tier, type)));
                helper.assertTrue(d > previous, tier.id + " " + type.id + " should out-damage the tier below (" + d + " <= " + previous + ")");
                previous = d;
            }
        }
        int iron = ((ArmorItem) ArsenalArmor.ITEMS.get("iron_chestplate").get()).getDefense();
        int daedric = ((ArmorItem) ArsenalArmor.ITEMS.get("daedric_chestplate").get()).getDefense();
        helper.assertTrue(daedric > iron, "daedric armor should out-protect iron");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void forgeMakesSteel(GameTestHelper helper) {
        BlockPos forge = new BlockPos(1, 1, 1);
        helper.setBlock(forge, ArsenalBlocks.FORGE.get());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(forge.getCenter()).add(1, 0, 0));
        player.getInventory().add(new ItemStack(Items.IRON_INGOT, 2));
        player.getInventory().add(new ItemStack(Items.COAL, 2));
        StationLogic.craft(player, helper.absolutePos(forge), UntamedArsenal.id("steel_ingot"), 2);
        int steel = 0, iron = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(ArsenalItems.STEEL_INGOT.get())) steel += s.getCount();
            if (s.is(Items.IRON_INGOT)) iron += s.getCount();
        }
        helper.assertTrue(steel == 2 && iron == 0, "forge should turn 2 iron + 2 coal into 2 steel (got " + steel + " steel, " + iron + " iron left)");
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(ArsenalItems.STEEL_INGOT.get()).getPath().equals("steel_ingot"), "steel registered");
        helper.succeed();
    }
}
