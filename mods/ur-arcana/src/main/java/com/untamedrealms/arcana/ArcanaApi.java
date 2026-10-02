package com.untamedrealms.arcana;

import com.untamedrealms.arcana.block.ArcanaStationBlock;
import com.untamedrealms.arcana.data.ArcanaKnowledge;
import com.untamedrealms.arcana.network.ArcanaNetwork;
import com.untamedrealms.core.api.UR;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Player knowledge (ingredient effects, enchantments) and station checks. */
public final class ArcanaApi {
    private ArcanaApi() {}

    public static ArcanaKnowledge knowledge(Player player) {
        return player.getData(UntamedArcana.KNOWLEDGE.get());
    }

    /** Sends the player's knowledge to their client (after any change, and on login). */
    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) ArcanaNetwork.syncKnowledge(serverPlayer, knowledge(player));
    }

    // Messages and XP only reach real players; the stations' logic also runs for game-test mock players.

    public static void subtle(Player player, Component text) {
        if (player instanceof ServerPlayer serverPlayer) UR.subtle(serverPlayer, text);
    }

    public static void warn(Player player, Component text) {
        if (player instanceof ServerPlayer serverPlayer) UR.warn(serverPlayer, text);
    }

    public static void xp(Player player, Skill skill, float xp) {
        if (player instanceof ServerPlayer serverPlayer) SkillsApi.addXp(serverPlayer, skill, xp);
    }

    public static boolean atStation(Player player, BlockPos pos, boolean enchanter) {
        return player.distanceToSqr(pos.getCenter()) <= 64
                && player.level().getBlockState(pos).getBlock() instanceof ArcanaStationBlock block && block.enchanter == enchanter;
    }
}
