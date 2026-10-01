package com.untamedrealms.skills.effect;

/**
 * Effect types understood by the suite. Unknown types are kept (another module may read them), so
 * add-on modules can introduce their own types without touching this list.
 */
public final class EffectTypes {
    /** Attribute modifier. Fields: attribute, operation, value. */
    public static final String ATTRIBUTE = "attribute";
    /** Outgoing damage multiplier bonus. key: skill id (one_handed, archery, destruction...) or empty for all. */
    public static final String DAMAGE_BONUS = "damage_bonus";
    /** Incoming damage reduction. key: heavy_armor / light_armor (scaled by pieces worn), block, or empty for all. */
    public static final String DAMAGE_REDUCTION = "damage_reduction";
    /** Skill XP multiplier bonus. key: skill id, category:&lt;combat|magic|stealth|gathering&gt;, or empty for all. */
    public static final String XP_BONUS = "xp_bonus";
    /** Chance of an extra drop when gathering. key: mining / woodcutting / farming / fishing. */
    public static final String EXTRA_DROP = "extra_drop";
    /** Gathering speed bonus. key: mining / woodcutting. */
    public static final String SPEED_BONUS = "speed_bonus";
    /** Added to the sneak-attack damage multiplier. key: melee / archery or empty for both. */
    public static final String SNEAK_ATTACK = "sneak_attack";
    /** Cost reduction. key: magic school id (spell magicka cost) or "stamina". */
    public static final String COST_REDUCTION = "cost_reduction";
    /** Spell magnitude bonus. key: magic school id or empty. */
    public static final String SPELL_POWER = "spell_power";
    /** Better prices with merchants (fraction). */
    public static final String PRICE_BONUS = "price_bonus";
    /** Regeneration multiplier bonus. key: health / magicka / stamina. */
    public static final String REGEN = "regen";
    /** Potion duration bonus (fraction) for potions you drink. */
    public static final String POTION_DURATION = "potion_duration";
    /** Chance for unlooted chests to roll their loot table twice. */
    public static final String LOOT_BONUS = "loot_bonus";
    /** Added to smithing quality roll thresholds. */
    public static final String SMITHING_QUALITY = "smithing_quality";
    /** Extra healing from eating, per point of food. */
    public static final String FOOD_HEALING = "food_healing";
    /** Fall damage reduction (fraction). */
    public static final String FALL_REDUCTION = "fall_reduction";
    /** Detection reduction while sneaking (fraction). */
    public static final String STEALTH = "stealth";
    /** Pickpocket success chance bonus (fraction). */
    public static final String PICKPOCKET = "pickpocket";

    private EffectTypes() {}
}
