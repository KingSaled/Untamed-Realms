package com.untamedrealms.quests.block;

import com.untamedrealms.quests.QuestsConfig;
import com.untamedrealms.quests.data.QuestsData;
import com.untamedrealms.quests.engine.QuestApi;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Radiant bounties: each notice board offers a few quests from the "bounty" pool, re-rolled every
 * in-game day and different for every board.
 */
public final class Bounties {
    public static final String POOL = "bounty";

    private Bounties() {}

    public static List<ResourceLocation> offers(ServerPlayer player, BlockPos pos) {
        List<ResourceLocation> pool = new ArrayList<>(QuestsData.inPool(POOL));
        long day = player.serverLevel().getDayTime() / 24000L;
        Collections.shuffle(pool, new Random(pos.asLong() * 31 + day));
        List<ResourceLocation> offers = new ArrayList<>();
        for (ResourceLocation id : pool) {
            if (offers.size() >= QuestsConfig.BOUNTIES_PER_BOARD.get()) break;
            if (QuestApi.isActive(player, id) || QuestApi.cannotStart(player, id) == null) offers.add(id);
        }
        return offers;
    }
}
