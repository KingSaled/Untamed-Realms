package com.untamedrealms.skills.effect;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * Supplies extra {@link SkillEffect}s for a player beyond their perks - e.g. their class and
 * birthsign (ur-classes) or active blessings. Call
 * {@link com.untamedrealms.skills.api.SkillsApi#refresh} whenever a provider's output changes.
 */
@FunctionalInterface
public interface EffectProvider {
    void collect(ServerPlayer player, Consumer<SkillEffect> out);
}
