package com.untamedrealms.quests.engine;

import com.untamedrealms.quests.UntamedQuests;
import com.untamedrealms.quests.data.QuestDef;
import com.untamedrealms.quests.data.QuestLog;
import com.untamedrealms.quests.data.QuestsData;
import com.untamedrealms.quests.network.QuestsNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quest markers: for the tracked quest, the first unfinished objective that has a place in the world
 * (a structure to find, a biome to reach, an NPC to talk to) is located server-side and sent to the
 * client, which shows it on the compass. Other modules add locators for their objective types
 * (ur-npcs locates NPCs for {@code talk} / {@code turn_in}).
 */
@EventBusSubscriber(modid = UntamedQuests.MODID)
public final class QuestMarkers {
    /** Finds where an objective's target is, or null if unknown / too far. */
    @FunctionalInterface
    public interface Locator {
        @Nullable Located locate(ServerPlayer player, String target);
    }

    /** A located target: where it is and, for people, their name ("Helga"). */
    public record Located(BlockPos pos, @Nullable Component name) {
        public static @Nullable Located at(@Nullable BlockPos pos) {
            return pos == null ? null : new Located(pos, null);
        }
    }

    /** {@code refreshTicks}: how often to re-run while the objective is unchanged (0 = only after moving far). */
    private record Entry(Locator locator, int refreshTicks) {}

    private static final Map<String, Entry> LOCATORS = new ConcurrentHashMap<>();
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();
    private static final int MOVE_REFRESH = 192;

    private static final class State {
        String key = "";
        @Nullable BlockPos origin;
        @Nullable Located target;
        long at;
    }

    static {
        register(QuestApi.VISIT, 0, (player, target) -> Located.at(Locate.structurePos(player.serverLevel(), target, player.blockPosition(), 64)));
        register(QuestApi.DELIVER, 0, (player, target) -> Located.at(Locate.structurePos(player.serverLevel(), "#minecraft:village", player.blockPosition(), 64)));
        register(QuestApi.BIOME, 0, (player, target) -> Located.at(Locate.biome(player.serverLevel(), target, player.blockPosition(), 2400)));
    }

    private QuestMarkers() {}

    public static void register(String objectiveType, int refreshTicks, Locator locator) {
        LOCATORS.put(objectiveType, new Entry(locator, refreshTicks));
    }

    private record Current(String key, QuestDef.Objective objective, int index) {}

    /** The tracked quest's first unfinished objective that can be located. */
    private static @Nullable Current current(ServerPlayer player) {
        QuestLog log = QuestApi.log(player);
        ResourceLocation id = log.tracked();
        if (id == null) return null;
        QuestLog.Active active = log.active().get(id);
        QuestDef def = QuestsData.get(id);
        if (active == null || def == null || active.stage >= def.stages().size()) return null;
        List<QuestDef.Objective> objectives = def.stages().get(active.stage).objectives();
        for (int i = 0; i < objectives.size() && i < active.progress.length; i++) {
            QuestDef.Objective obj = objectives.get(i);
            if (active.progress[i] >= obj.count() || !LOCATORS.containsKey(obj.type())) continue;
            if (obj.type().equals(QuestApi.TURN_IN) && !QuestApi.othersDone(objectives, active, i)) continue;
            return new Current(player.level().dimension().location() + "|" + id + "|" + active.stage + "|" + i, obj, i);
        }
        return null;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 7) return;
        State state = STATES.computeIfAbsent(player.getUUID(), u -> new State());
        Current cur = current(player);
        if (cur == null) {
            if (!state.key.isEmpty() || state.origin == null) {
                state.key = "";
                state.target = null;
                state.origin = player.blockPosition();
                QuestsNetwork.sendMarker(player, null, Component.empty(), -1, null);
            }
            return;
        }
        Entry entry = LOCATORS.get(cur.objective().type());
        long now = player.serverLevel().getGameTime();
        boolean refresh = !cur.key().equals(state.key) || state.origin == null
                || state.origin.distSqr(player.blockPosition()) > MOVE_REFRESH * MOVE_REFRESH
                || (entry.refreshTicks() > 0 && now - state.at >= entry.refreshTicks());
        if (!refresh) return;
        Located target = null;
        try {
            target = entry.locator().locate(player, cur.objective().target());
        } catch (Exception e) {
            UntamedQuests.LOGGER.warn("Quest marker lookup failed for {}", cur.objective().target(), e);
        }
        boolean changed = !cur.key().equals(state.key) || !Objects.equals(target, state.target);
        state.key = cur.key();
        state.origin = player.blockPosition();
        state.at = now;
        state.target = target;
        if (changed) {
            QuestsNetwork.sendMarker(player, target == null ? null : target.pos(), QuestApi.describe(cur.objective()), cur.index(),
                    target == null ? null : target.name());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }
}
