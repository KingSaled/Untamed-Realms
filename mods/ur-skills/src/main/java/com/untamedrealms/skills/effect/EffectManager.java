package com.untamedrealms.skills.effect;

import com.untamedrealms.core.registry.CoreAttributes;
import com.untamedrealms.skills.SkillsConfig;
import com.untamedrealms.skills.UntamedSkills;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.data.SkillData;
import com.untamedrealms.skills.data.SkillsData;
import com.untamedrealms.skills.registry.SkillsAttachments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Builds and caches each player's {@link EffectCache} and applies attribute effects to the entity. */
public final class EffectManager {
    private static final Map<UUID, EffectCache> CACHES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, EffectProvider> PROVIDERS = new LinkedHashMap<>();

    private EffectManager() {}

    public static synchronized void registerProvider(ResourceLocation id, EffectProvider provider) {
        PROVIDERS.put(id, provider);
    }

    public static EffectCache get(ServerPlayer player) {
        EffectCache cache = CACHES.get(player.getUUID());
        if (cache == null) cache = rebuild(player);
        return cache;
    }

    public static void forget(UUID player) {
        CACHES.remove(player);
    }

    /** Collects every effect for the player, aggregates them and (re)applies attribute modifiers. */
    public static EffectCache rebuild(ServerPlayer player) {
        EffectCache cache = new EffectCache();
        List<SkillEffect> effects = new ArrayList<>();
        SkillData data = player.getData(SkillsAttachments.SKILLS);

        for (ResourceLocation perkId : data.perks()) {
            SkillsData.PerkRef ref = SkillsData.perk(perkId);
            if (ref != null) effects.addAll(ref.perk().effects());
        }
        for (EffectProvider provider : PROVIDERS.values()) {
            provider.collect(player, effects::add);
        }

        // Attribute choices from character level-ups.
        effects.add(attr(Attributes.MAX_HEALTH, data.healthPicks() * SkillsConfig.HEALTH_PER_PICK.get(), AttributeModifier.Operation.ADD_VALUE));
        effects.add(attr(CoreAttributes.MAX_MAGICKA, data.magickaPicks() * SkillsConfig.MAGICKA_PER_PICK.get(), AttributeModifier.Operation.ADD_VALUE));
        effects.add(attr(CoreAttributes.MAX_STAMINA, data.staminaPicks() * SkillsConfig.STAMINA_PER_PICK.get(), AttributeModifier.Operation.ADD_VALUE));
        // Agility: passive movement speed.
        effects.add(attr(Attributes.MOVEMENT_SPEED, data.level(Skill.AGILITY) * 0.0008, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        Map<String, Double> attributeTotals = new LinkedHashMap<>();
        for (SkillEffect effect : effects) {
            cache.add(effect);
            if (EffectTypes.ATTRIBUTE.equals(effect.type()) && effect.attribute().isPresent()) {
                attributeTotals.merge(effect.attribute().get() + "|" + effect.operation().getSerializedName(), (double) effect.value(), Double::sum);
            }
        }
        // Regen effects map onto the core regen attributes.
        float magickaRegen = cache.raw(EffectTypes.REGEN, "magicka");
        float staminaRegen = cache.raw(EffectTypes.REGEN, "stamina");
        float staminaCost = cache.raw(EffectTypes.COST_REDUCTION, "stamina");
        if (magickaRegen != 0) attributeTotals.merge(CoreAttributes.MAGICKA_REGEN.getId() + "|add_value", (double) magickaRegen, Double::sum);
        if (staminaRegen != 0) attributeTotals.merge(CoreAttributes.STAMINA_REGEN.getId() + "|add_value", (double) staminaRegen, Double::sum);
        if (staminaCost != 0) attributeTotals.merge(CoreAttributes.STAMINA_COST.getId() + "|add_value", (double) -staminaCost, Double::sum);

        removeOurModifiers(player);

        float healthBefore = player.getHealth();
        for (Map.Entry<String, Double> entry : attributeTotals.entrySet()) {
            if (entry.getValue() == 0) continue;
            String[] parts = entry.getKey().split("\\|");
            ResourceLocation attributeId = ResourceLocation.tryParse(parts[0]);
            Optional<Holder.Reference<Attribute>> holder = attributeId == null ? Optional.empty() : BuiltInRegistries.ATTRIBUTE.getHolder(attributeId);
            if (holder.isEmpty()) {
                UntamedSkills.LOGGER.warn("Unknown attribute in skill effect: {}", parts[0]);
                continue;
            }
            AttributeModifier.Operation op = operation(parts[1]);
            AttributeInstance instance = player.getAttribute(holder.get());
            if (instance == null) continue;
            ResourceLocation modId = UntamedSkills.id("fx/" + attributeId.getNamespace() + "/" + attributeId.getPath() + "/" + parts[1]);
            instance.removeModifier(modId);
            instance.addPermanentModifier(new AttributeModifier(modId, entry.getValue(), op));
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        else if (healthBefore > player.getHealth()) player.setHealth(Math.min(healthBefore, player.getMaxHealth()));

        CACHES.put(player.getUUID(), cache);
        return cache;
    }

    /** Removes every modifier this system ever applied (they are permanent so they survive relogs). */
    private static void removeOurModifiers(ServerPlayer player) {
        BuiltInRegistries.ATTRIBUTE.holders().forEach(holder -> {
            AttributeInstance instance = player.getAttribute(holder);
            if (instance == null) return;
            List<ResourceLocation> stale = new ArrayList<>();
            for (AttributeModifier modifier : instance.getModifiers()) {
                if (modifier.id().getNamespace().equals(UntamedSkills.MODID) && modifier.id().getPath().startsWith("fx/")) {
                    stale.add(modifier.id());
                }
            }
            stale.forEach(instance::removeModifier);
        });
    }

    private static SkillEffect attr(Holder<Attribute> attribute, double value, AttributeModifier.Operation op) {
        ResourceLocation id = attribute.unwrapKey().map(k -> k.location()).orElseThrow();
        return new SkillEffect(EffectTypes.ATTRIBUTE, "", Optional.of(id), op, (float) value);
    }

    private static AttributeModifier.Operation operation(String name) {
        for (AttributeModifier.Operation op : AttributeModifier.Operation.values()) {
            if (op.getSerializedName().equals(name)) return op;
        }
        return AttributeModifier.Operation.ADD_VALUE;
    }
}
