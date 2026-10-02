package com.untamedrealms.arcana.alchemy;

import com.untamedrealms.arcana.ArcanaApi;
import com.untamedrealms.arcana.UntamedArcana;
import com.untamedrealms.arcana.data.AlchemyEffect;
import com.untamedrealms.arcana.data.ArcanaKnowledge;
import com.untamedrealms.arcana.data.IngredientDef;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.effect.EffectTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Server side of the alchemy lab: tasting and brewing. */
public final class AlchemyLab {
    public static final String ALCHEMY_POWER = EffectTypes.ALCHEMY_POWER;
    public static final String ALCHEMY_REVEAL = EffectTypes.ALCHEMY_REVEAL;

    private AlchemyLab() {}

    public static float power(Player player) {
        return Alchemy.power(SkillsApi.level(player, Skill.ALCHEMY), SkillsApi.effect(player, ALCHEMY_POWER, ""));
    }

    private static ItemStack find(Inventory inventory, ResourceLocation item) {
        for (ItemStack stack : inventory.items) {
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(item)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Learns an ingredient's first effects (how many depends on Alchemy) and feels a weak version of the
     * first one. Used by the lab's Taste button and when an ingredient is eaten. Returns how many effects were new.
     */
    public static int taste(Player player, IngredientDef def, boolean applyFirstEffect) {
        ArcanaKnowledge knowledge = ArcanaApi.knowledge(player);
        int reveal = Alchemy.tasteReveals(SkillsApi.level(player, Skill.ALCHEMY), SkillsApi.effect(player, ALCHEMY_REVEAL, ""));
        int learned = 0;
        for (int i = 0; i < Math.min(reveal, def.effects().size()); i++) if (knowledge.learn(def.item(), i)) learned++;
        if (applyFirstEffect && !def.effects().isEmpty()) {
            AlchemyEffect first = UntamedArcana.EFFECTS.getOrNull(def.effects().get(0));
            MobEffectInstance instance = first == null ? null : Alchemy.instance(first, 0.25f);
            if (instance != null) player.addEffect(instance);
        }
        if (learned > 0) {
            ArcanaApi.xp(player, Skill.ALCHEMY, 2f * learned);
            ArcanaApi.subtle(player, Component.translatable("message.urarcana.tasted", learned));
            ArcanaApi.sync(player);
        }
        return learned;
    }

    /** The lab's Taste button: eats one of the item. */
    public static void taste(ServerPlayer player, BlockPos pos, ResourceLocation item) {
        if (!ArcanaApi.atStation(player, pos, false)) return;
        IngredientDef def = Alchemy.ingredient(item);
        ItemStack stack = find(player.getInventory(), item);
        if (def == null || stack.isEmpty()) return;
        stack.shrink(1);
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.6f, 1.1f);
        if (taste(player, def, true) == 0) ArcanaApi.subtle(player, Component.translatable("message.urarcana.tasted_nothing"));
    }

    /**
     * Brews {@code times} times from one of each ingredient. Returns the number of brews made; 0 also when
     * the ingredients share no effect (they are used up anyway - that's how you learn).
     */
    public static int brew(ServerPlayer player, BlockPos pos, List<ResourceLocation> items, int times) {
        if (!ArcanaApi.atStation(player, pos, false)) return 0;
        List<ResourceLocation> distinct = new ArrayList<>(new LinkedHashSet<>(items));
        if (distinct.size() < 2 || distinct.size() > 3) return 0;
        List<IngredientDef> defs = new ArrayList<>();
        for (ResourceLocation item : distinct) {
            IngredientDef def = Alchemy.ingredient(item);
            if (def == null) return 0;
            defs.add(def);
        }
        return brew(player, defs, times);
    }

    /** Brewing without the station check (game tests, other modules). */
    public static int brew(Player player, List<IngredientDef> defs, int times) {
        List<ResourceLocation> shared = Alchemy.sharedEffects(defs);
        float power = power(player);
        int made = 0;
        for (int n = 0; n < Math.max(1, Math.min(16, times)); n++) {
            List<ItemStack> stacks = new ArrayList<>();
            for (IngredientDef def : defs) stacks.add(find(player.getInventory(), def.item()));
            if (stacks.stream().anyMatch(ItemStack::isEmpty)) break;
            stacks.forEach(s -> s.shrink(1));
            if (shared.isEmpty()) {
                ArcanaApi.warn(player, Component.translatable("message.urarcana.brew_failed"));
                player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.4f);
                return 0;
            }
            ItemStack result = Alchemy.brew(shared, power);
            if (!player.getInventory().add(result)) player.drop(result, false);
            ArcanaApi.xp(player, Skill.ALCHEMY, Alchemy.brewXp(shared));
            made++;
        }
        if (made == 0) {
            ArcanaApi.warn(player, Component.translatable("message.urarcana.missing"));
            return 0;
        }
        if (Alchemy.learnShared(ArcanaApi.knowledge(player), defs, new LinkedHashSet<>(shared)) > 0) {
            ArcanaApi.subtle(player, Component.translatable("message.urarcana.discovered"));
        }
        ArcanaApi.sync(player);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8f, 1.0f);
        return made;
    }
}
