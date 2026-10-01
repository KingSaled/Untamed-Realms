package com.untamedrealms.arsenal;

/**
 * Weapon shapes. Damage is {@code 1 + base + tier bonus}; speed is the vanilla attack-speed modifier
 * (4 + speed attacks per second). {@code ingots}/{@code strips} are the forge cost.
 */
public enum WeaponType {
    DAGGER("dagger", 1.5f, -1.6f, Hand.ONE, "bettercombat:dagger", 1, 1),
    SWORD("sword", 3f, -2.4f, Hand.ONE, "bettercombat:sword", 2, 1),
    WAR_AXE("war_axe", 4f, -2.8f, Hand.ONE, "bettercombat:axe", 2, 1),
    MACE("mace", 4.5f, -3.0f, Hand.ONE, "bettercombat:mace", 3, 1),
    GREATSWORD("greatsword", 6f, -3.0f, Hand.TWO, "bettercombat:claymore", 4, 2),
    BATTLEAXE("battleaxe", 7f, -3.2f, Hand.TWO, "bettercombat:double_axe", 4, 2),
    WARHAMMER("warhammer", 8f, -3.4f, Hand.TWO, "bettercombat:hammer", 5, 2),
    BOW("bow", 0f, 0f, Hand.BOW, "", 2, 2);

    public enum Hand { ONE, TWO, BOW }

    public final String id;
    public final float base;
    public final float speed;
    public final Hand hand;
    public final String betterCombatParent;
    public final int ingots;
    public final int strips;

    WeaponType(String id, float base, float speed, Hand hand, String betterCombatParent, int ingots, int strips) {
        this.id = id;
        this.base = base;
        this.speed = speed;
        this.hand = hand;
        this.betterCombatParent = betterCombatParent;
        this.ingots = ingots;
        this.strips = strips;
    }
}
