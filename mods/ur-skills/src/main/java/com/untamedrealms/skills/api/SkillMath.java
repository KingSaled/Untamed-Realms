package com.untamedrealms.skills.api;

import com.untamedrealms.skills.SkillsConfig;

/**
 * Level curves.
 * <ul>
 *   <li>Skills use the RuneScape curve (1-99), scaled by {@code xpCurveScale}.</li>
 *   <li>Character level uses Skyrim's rule: each skill level gained adds that level as character XP;
 *       reaching the next character level needs {@code 25 * (level + 3)}.</li>
 * </ul>
 */
public final class SkillMath {
    public static final int MAX_LEVEL = 99;

    /** Unscaled RuneScape table: RS_TABLE[L] = total xp needed to reach level L. */
    private static final double[] RS_TABLE = new double[MAX_LEVEL + 2];

    static {
        double points = 0;
        RS_TABLE[1] = 0;
        for (int level = 1; level <= MAX_LEVEL; level++) {
            points += Math.floor(level + 300.0 * Math.pow(2.0, level / 7.0));
            RS_TABLE[level + 1] = Math.floor(points / 4.0);
        }
    }

    private SkillMath() {}

    public static double scale() {
        return SkillsConfig.get(SkillsConfig.XP_CURVE_SCALE, 0.25);
    }

    /** Total XP needed to reach {@code level}. */
    public static double xpForLevel(int level) {
        if (level <= 1) return 0;
        if (level > MAX_LEVEL) level = MAX_LEVEL;
        return RS_TABLE[level] * scale();
    }

    public static int levelForXp(double xp) {
        double scale = scale();
        int level = 1;
        while (level < MAX_LEVEL && xp >= RS_TABLE[level + 1] * scale) level++;
        return level;
    }

    /** 0..1 progress from the current level to the next. */
    public static float progress(double xp) {
        int level = levelForXp(xp);
        if (level >= MAX_LEVEL) return 1f;
        double from = xpForLevel(level);
        double to = xpForLevel(level + 1);
        return (float) ((xp - from) / Math.max(1e-9, to - from));
    }

    /** Character XP needed to go from {@code level} to {@code level + 1}. */
    public static int characterXpToNext(int level) {
        return 25 * (level + 3);
    }
}
