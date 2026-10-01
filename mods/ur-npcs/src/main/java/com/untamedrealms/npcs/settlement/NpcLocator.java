package com.untamedrealms.npcs.settlement;

import com.untamedrealms.npcs.UntamedNpcs;
import com.untamedrealms.npcs.data.NpcsData;
import com.untamedrealms.npcs.data.SettlementDef;
import com.untamedrealms.npcs.entity.NpcEntity;
import com.untamedrealms.quests.engine.Locate;
import com.untamedrealms.quests.engine.QuestApi;
import com.untamedrealms.quests.engine.QuestMarkers;
import com.untamedrealms.quests.engine.Targets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Quest markers for "speak with X" / "return to X": the nearest loaded NPC with that id, otherwise
 * the nearest settlement whose roster includes them (settlements fill with NPCs when first entered).
 */
public final class NpcLocator {
    private NpcLocator() {}

    public static void register() {
        QuestMarkers.register(QuestApi.TALK, 60, NpcLocator::locate);
        QuestMarkers.register(QuestApi.TURN_IN, 60, NpcLocator::locate);
    }

    static @Nullable QuestMarkers.Located locate(ServerPlayer player, String target) {
        ServerLevel level = player.serverLevel();
        NpcEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (NpcEntity npc : level.getEntities(UntamedNpcs.NPC.get(), npc -> npc.npcId() != null && Targets.id(target, npc.npcId()))) {
            double d = npc.distanceToSqr(player);
            if (d < bestDist) {
                bestDist = d;
                best = npc;
            }
        }
        if (best != null) return new QuestMarkers.Located(best.blockPosition(), best.getCustomName());

        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (SettlementDef def : NpcsData.SETTLEMENTS.values()) {
            boolean lives = def.always().stream().anyMatch(id -> Targets.id(target, id)) || def.random().stream().anyMatch(id -> Targets.id(target, id));
            if (!lives) continue;
            BlockPos pos = Locate.structurePos(level, def.structures(), player.blockPosition(), 64);
            if (pos != null && pos.distSqr(player.blockPosition()) < nearestDist) {
                nearestDist = pos.distSqr(player.blockPosition());
                nearest = pos;
            }
        }
        return QuestMarkers.Located.at(nearest);
    }
}
