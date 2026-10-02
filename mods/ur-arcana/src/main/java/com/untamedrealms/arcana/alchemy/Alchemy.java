package com.untamedrealms.arcana.alchemy;

import com.untamedrealms.arcana.UntamedArcana;
import com.untamedrealms.arcana.data.AlchemyEffect;
import com.untamedrealms.arcana.data.ArcanaKnowledge;
import com.untamedrealms.arcana.data.IngredientDef;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The rules of alchemy, shared by server and client (the lab window previews with them):
 * <ul>
 *   <li>Every ingredient has four effects. Tasting reveals the first (more with Alchemy skill).</li>
 *   <li>Brewing two or three ingredients makes a brew of every effect at least two of them share, and
 *       reveals those effects on each ingredient used. No shared effect: the ingredients are wasted.</li>
 *   <li>The brew is named after its most valuable effect; if that effect is harmful it is a poison,
 *       which is thrown (a splash potion).</li>
 *   <li>Strength: duration and, from power 1.75, one extra amplifier level scale with Alchemy level and
 *       the {@code alchemy_power} perk effect.</li>
 * </ul>
 */
public final class Alchemy {
    private Alchemy() {}

    /** The ingredient definition for an item id, or null. */
    public static IngredientDef ingredient(ResourceLocation item) {
        for (IngredientDef def : UntamedArcana.INGREDIENTS.values()) {
            if (def.item().equals(item)) return def;
        }
        return null;
    }

    public static IngredientDef ingredient(ItemStack stack) {
        return stack.isEmpty() ? null : ingredient(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** How many effects tasting reveals: 1, +1 at Alchemy 25 / 50 / 75, plus the alchemy_reveal perk effect. */
    public static int tasteReveals(int alchemyLevel, float perkBonus) {
        return Math.min(4, 1 + alchemyLevel / 25 + Math.round(perkBonus));
    }

    /** Brew strength multiplier. */
    public static float power(int alchemyLevel, float perkBonus) {
        return 1f + alchemyLevel * 0.015f + perkBonus;
    }

    /** Effects shared by at least two of the given (distinct) ingredients, most valuable first. */
    public static List<ResourceLocation> sharedEffects(List<IngredientDef> ingredients) {
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        for (IngredientDef def : ingredients) {
            for (ResourceLocation effect : new LinkedHashSet<>(def.effects())) counts.merge(effect, 1, Integer::sum);
        }
        List<ResourceLocation> shared = new ArrayList<>();
        counts.forEach((effect, n) -> { if (n >= 2 && UntamedArcana.EFFECTS.contains(effect)) shared.add(effect); });
        shared.sort(Comparator.comparingInt((ResourceLocation id) -> -UntamedArcana.EFFECTS.getOrNull(id).value()));
        return shared;
    }

    /** Learns, on each ingredient, the effects that are in {@code effects}. Returns how many were new. */
    public static int learnShared(ArcanaKnowledge knowledge, List<IngredientDef> ingredients, Set<ResourceLocation> effects) {
        int learned = 0;
        for (IngredientDef def : ingredients) {
            for (int i = 0; i < def.effects().size(); i++) {
                if (effects.contains(def.effects().get(i)) && knowledge.learn(def.item(), i)) learned++;
            }
        }
        return learned;
    }

    /** True if the most valuable effect is harmful. */
    public static boolean isPoison(List<ResourceLocation> effects) {
        if (effects.isEmpty()) return false;
        AlchemyEffect top = UntamedArcana.EFFECTS.getOrNull(effects.get(0));
        return top != null && top.harmful();
    }

    public static MobEffectInstance instance(AlchemyEffect def, float power) {
        Optional<Holder.Reference<MobEffect>> holder = BuiltInRegistries.MOB_EFFECT.getHolder(def.mobEffect());
        if (holder.isEmpty()) return null;
        int amplifier = def.amplifier() + (power >= 1.75f ? 1 : 0);
        int duration = def.instant() ? 1 : Math.round(def.duration() * power);
        return new MobEffectInstance(holder.get(), duration, amplifier);
    }

    /** The potion or poison for a set of shared effects (most valuable first), at a strength. */
    public static ItemStack brew(List<ResourceLocation> effects, float power) {
        boolean poison = isPoison(effects);
        List<MobEffectInstance> instances = new ArrayList<>();
        int r = 0, g = 0, b = 0;
        for (ResourceLocation id : effects) {
            AlchemyEffect def = UntamedArcana.EFFECTS.getOrNull(id);
            MobEffectInstance instance = def == null ? null : instance(def, power);
            if (instance == null) continue;
            instances.add(instance);
            int color = instance.getEffect().value().getColor();
            r += (color >> 16) & 0xFF;
            g += (color >> 8) & 0xFF;
            b += color & 0xFF;
        }
        ItemStack stack = new ItemStack(poison ? Items.SPLASH_POTION : Items.POTION);
        int n = Math.max(1, instances.size());
        int color = ((r / n) << 16) | ((g / n) << 8) | (b / n);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(color), instances));
        AlchemyEffect top = effects.isEmpty() ? null : UntamedArcana.EFFECTS.getOrNull(effects.get(0));
        if (top != null) {
            stack.set(DataComponents.ITEM_NAME, Component.translatable(poison ? "item.urarcana.poison" : "item.urarcana.potion", top.name()));
        }
        return stack;
    }

    /** Alchemy XP for a successful brew. */
    public static float brewXp(List<ResourceLocation> effects) {
        int value = 0;
        for (ResourceLocation id : effects) {
            AlchemyEffect def = UntamedArcana.EFFECTS.getOrNull(id);
            if (def != null) value += def.value();
        }
        return 6 + 3 * effects.size() + value / 10f;
    }
}
