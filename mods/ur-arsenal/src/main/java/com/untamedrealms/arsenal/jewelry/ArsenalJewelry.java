package com.untamedrealms.arsenal.jewelry;

import com.untamedrealms.arsenal.ArsenalComponents;
import com.untamedrealms.arsenal.ArsenalItems;
import com.untamedrealms.core.registry.CoreAttributes;
import com.untamedrealms.skills.effect.EffectTypes;
import com.untamedrealms.skills.effect.SkillEffect;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The rings and amulets, with their default bonuses. */
public final class ArsenalJewelry {
    public static final Map<String, DeferredItem<JewelryItem>> ITEMS = new LinkedHashMap<>();

    static {
        jewel("ring_of_strength", fx(EffectTypes.DAMAGE_BONUS, "one_handed", 0.10f), fx(EffectTypes.DAMAGE_BONUS, "two_handed", 0.10f));
        jewel("ring_of_the_archer", fx(EffectTypes.DAMAGE_BONUS, "archery", 0.15f));
        jewel("ring_of_magicka", attr(CoreAttributes.MAX_MAGICKA, 30));
        jewel("ring_of_stamina", attr(CoreAttributes.MAX_STAMINA, 30));
        jewel("ring_of_vitality", attr(Attributes.MAX_HEALTH, 4));
        jewel("ring_of_the_thief", fx(EffectTypes.PICKPOCKET, "", 0.15f), fx(EffectTypes.STEALTH, "", 0.10f));
        jewel("amulet_of_destruction", fx(EffectTypes.SPELL_POWER, "destruction", 0.15f), fx(EffectTypes.COST_REDUCTION, "destruction", 0.10f));
        jewel("amulet_of_restoration", fx(EffectTypes.SPELL_POWER, "restoration", 0.15f), fx(EffectTypes.COST_REDUCTION, "restoration", 0.10f));
        jewel("amulet_of_the_merchant", fx(EffectTypes.PRICE_BONUS, "", 0.10f));
        jewel("amulet_of_warding", fx(EffectTypes.DAMAGE_REDUCTION, "", 0.08f));
        jewel("amulet_of_learning", fx(EffectTypes.XP_BONUS, "", 0.10f));
        jewel("amulet_of_vigor", fx(EffectTypes.REGEN, "magicka", 0.25f), fx(EffectTypes.REGEN, "stamina", 0.25f));
    }

    private ArsenalJewelry() {}

    /** Forces class loading (the static block registers the items). */
    public static void init() {}

    private static void jewel(String name, SkillEffect... effects) {
        ITEMS.put(name, ArsenalItems.ITEMS.register(name, () -> new JewelryItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                .component(ArsenalComponents.JEWEL_EFFECTS.get(), List.of(effects)))));
    }

    private static SkillEffect fx(String type, String key, float value) {
        return SkillEffect.of(type, key, value);
    }

    private static SkillEffect attr(Holder<Attribute> attribute, float value) {
        return new SkillEffect(EffectTypes.ATTRIBUTE, "", Optional.of(attribute.unwrapKey().orElseThrow().location()),
                AttributeModifier.Operation.ADD_VALUE, value);
    }
}
