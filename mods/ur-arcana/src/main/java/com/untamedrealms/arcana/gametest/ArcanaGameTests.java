package com.untamedrealms.arcana.gametest;

import com.untamedrealms.arcana.ArcanaApi;
import com.untamedrealms.arcana.UntamedArcana;
import com.untamedrealms.arcana.alchemy.Alchemy;
import com.untamedrealms.arcana.alchemy.AlchemyLab;
import com.untamedrealms.arcana.data.AlchemyEffect;
import com.untamedrealms.arcana.data.IngredientDef;
import com.untamedrealms.arcana.enchanting.ArcaneEnchanter;
import com.untamedrealms.arcana.enchanting.SoulTrap;
import com.untamedrealms.arcana.item.SoulGemItem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(UntamedArcana.MODID)
@PrefixGameTestTemplate(false)
public class ArcanaGameTests {
    /** A plain mock player: no connection, so the stations skip messages and XP (see ArcanaApi). */
    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().clearContent();
        return player;
    }

    private static ItemStack item(ResourceLocation id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(id));
    }

    /** Two ingredients that share an effect (sharing == true) or share none. */
    private static List<IngredientDef> pair(boolean sharing) {
        List<IngredientDef> all = new ArrayList<>(UntamedArcana.INGREDIENTS.values());
        for (IngredientDef a : all) {
            for (IngredientDef b : all) {
                if (a == b) continue;
                if (Alchemy.sharedEffects(List.of(a, b)).isEmpty() != sharing) return List.of(a, b);
            }
        }
        return List.of();
    }

    @GameTest(template = "arena")
    public static void alchemyDataResolves(GameTestHelper helper) {
        helper.assertTrue(UntamedArcana.EFFECTS.entries().size() >= 20, "expected the default alchemy effects");
        helper.assertTrue(UntamedArcana.INGREDIENTS.entries().size() >= 25, "expected the default ingredients");
        for (Map.Entry<ResourceLocation, AlchemyEffect> e : UntamedArcana.EFFECTS.entries().entrySet()) {
            helper.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(e.getValue().mobEffect()), e.getKey() + ": unknown mob effect " + e.getValue().mobEffect());
        }
        Map<ResourceLocation, Integer> uses = new HashMap<>();
        for (Map.Entry<ResourceLocation, IngredientDef> e : UntamedArcana.INGREDIENTS.entries().entrySet()) {
            IngredientDef def = e.getValue();
            helper.assertTrue(item(def.item()).getItem() != Items.AIR, e.getKey() + ": unknown item " + def.item());
            helper.assertTrue(def.effects().size() == 4, e.getKey() + ": needs four effects");
            for (ResourceLocation effect : def.effects()) {
                helper.assertTrue(UntamedArcana.EFFECTS.contains(effect), e.getKey() + ": unknown effect " + effect);
                uses.merge(effect, 1, Integer::sum);
            }
        }
        for (ResourceLocation effect : UntamedArcana.EFFECTS.entries().keySet()) {
            helper.assertTrue(uses.getOrDefault(effect, 0) >= 2, effect + " is on fewer than two ingredients, so it can never be brewed");
        }
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void brewingMakesSharedEffects(GameTestHelper helper) {
        Player player = player(helper);
        List<IngredientDef> pair = pair(true);
        helper.assertTrue(pair.size() == 2, "no two ingredients share an effect");
        pair.forEach(def -> player.getInventory().add(item(def.item())));
        List<ResourceLocation> shared = Alchemy.sharedEffects(pair);
        helper.assertTrue(AlchemyLab.brew(player, pair, 1) == 1, "brew should succeed");
        ItemStack brew = ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) if (stack.has(DataComponents.POTION_CONTENTS)) brew = stack;
        helper.assertTrue(!brew.isEmpty(), "no potion made");
        PotionContents contents = brew.get(DataComponents.POTION_CONTENTS);
        ResourceLocation expected = UntamedArcana.EFFECTS.getOrNull(shared.get(0)).mobEffect();
        boolean found = false;
        for (MobEffectInstance instance : contents.getAllEffects()) found |= BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value()).equals(expected);
        helper.assertTrue(found, "potion lacks the shared effect " + expected);
        for (IngredientDef def : pair) {
            helper.assertTrue(ArcanaApi.knowledge(player).knows(def.item(), def.effects().indexOf(shared.get(0))), "brewing should reveal the shared effect");
            helper.assertTrue(player.getInventory().countItem(item(def.item()).getItem()) == 0, "ingredients should be used up");
        }
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void noSharedEffectWastesIngredients(GameTestHelper helper) {
        Player player = player(helper);
        List<IngredientDef> pair = pair(false);
        helper.assertTrue(pair.size() == 2, "every ingredient pair shares an effect?");
        pair.forEach(def -> player.getInventory().add(item(def.item())));
        helper.assertTrue(AlchemyLab.brew(player, pair, 1) == 0, "nothing should brew");
        for (IngredientDef def : pair) helper.assertTrue(player.getInventory().countItem(item(def.item()).getItem()) == 0, "ingredients are spent");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void disenchantThenEnchant(GameTestHelper helper) {
        Player player = player(helper);
        Holder<Enchantment> sharpness = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS);
        player.getInventory().setItem(0, EnchantedBookItem.createForEnchantment(new EnchantmentInstance(sharpness, 3)));
        helper.assertTrue(ArcaneEnchanter.disenchant(player, 0) == 1, "disenchanting should teach Sharpness");
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "the book is destroyed");
        helper.assertTrue(ArcanaApi.knowledge(player).enchantments().contains(Enchantments.SHARPNESS.location()), "Sharpness known");

        player.getInventory().setItem(1, new ItemStack(Items.IRON_SWORD));
        ItemStack gem = new ItemStack(UntamedArcana.SOUL_GEMS.get(2).get());
        gem.set(UntamedArcana.SOUL.get(), 3);
        player.getInventory().setItem(2, gem);
        helper.assertTrue(ArcaneEnchanter.enchant(player, 1, Enchantments.SHARPNESS.location(), 2), "enchanting should succeed");
        helper.assertTrue(player.getInventory().getItem(1).getEnchantments().getLevel(sharpness) >= 1, "sword has Sharpness");
        helper.assertTrue(SoulGemItem.soul(player.getInventory().getItem(2)) == 0, "the gem is emptied");
        helper.assertTrue(!ArcaneEnchanter.enchant(player, 1, Enchantments.SHARPNESS.location(), 2), "an empty gem can't enchant");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void soulTrapFillsSmallestGem(GameTestHelper helper) {
        Player player = player(helper);
        player.getInventory().add(new ItemStack(UntamedArcana.SOUL_GEMS.get(4).get()));   // grand
        player.getInventory().add(new ItemStack(UntamedArcana.SOUL_GEMS.get(1).get()));   // lesser
        player.getInventory().add(new ItemStack(UntamedArcana.SOUL_GEMS.get(0).get()));   // petty: too small for a zombie
        int zombieSoul = SoulGemItem.soulOf(EntityType.ZOMBIE.create(helper.getLevel()).getMaxHealth());
        helper.assertTrue(zombieSoul == 2, "a zombie has a lesser soul, got " + zombieSoul);
        helper.assertTrue(SoulTrap.capture(player, zombieSoul), "a gem should take the soul");
        int lesser = -1, grand = -1, petty = -1;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof SoulGemItem g) {
                if (g.capacity == 2) lesser = SoulGemItem.soul(stack);
                if (g.capacity == 5) grand = SoulGemItem.soul(stack);
                if (g.capacity == 1) petty = SoulGemItem.soul(stack);
            }
        }
        helper.assertTrue(lesser == 2, "the lesser gem should hold it, has " + lesser);
        helper.assertTrue(grand == 0 && petty == 0, "the other gems stay empty");
        helper.assertTrue(SoulTrap.capture(player, 5), "a grand soul fills the grand gem");
        helper.assertTrue(SoulTrap.capture(player, 1) && !SoulTrap.capture(player, 1), "a petty soul fills the petty gem; then no gem is left");
        helper.succeed();
    }
}
