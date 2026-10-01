package com.untamedrealms.quests.engine;

import com.untamedrealms.quests.QuestsConfig;
import com.untamedrealms.quests.UntamedQuests;
import com.untamedrealms.quests.data.QuestDef;
import com.untamedrealms.quests.data.QuestLog;
import com.untamedrealms.quests.data.QuestsData;
import com.untamedrealms.quests.network.QuestsNetwork;
import com.untamedrealms.skills.api.CharacterLevelUpEvent;
import com.untamedrealms.skills.api.SkillLevelUpEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;

/** Feeds game events into the quest engine. */
@EventBusSubscriber(modid = UntamedQuests.MODID)
public final class QuestEvents {
    private QuestEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        QuestLog log = QuestApi.log(player);
        String starting = QuestsConfig.STARTING_QUEST.get();
        if (!starting.isBlank() && log.flags().add("started_intro")) {
            ResourceLocation id = ResourceLocation.tryParse(starting);
            if (id != null && QuestsData.QUESTS.contains(id)) QuestApi.start(player, id, true);
        }
        QuestsNetwork.syncNow(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuestsNetwork.syncNow(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKill(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        var type = event.getEntity().getType();
        QuestApi.progress(player, QuestApi.KILL, target -> Targets.entity(target, type), 1);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMine(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        var state = event.getState();
        QuestApi.progress(player, QuestApi.MINE, target -> Targets.block(target, state), 1);
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getCrafting();
        QuestApi.progress(player, QuestApi.CRAFT, target -> Targets.item(target, stack), Math.max(1, stack.getCount()));
    }

    @SubscribeEvent
    public static void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getSmelting();
        QuestApi.progress(player, QuestApi.SMELT, target -> Targets.item(target, stack), Math.max(1, stack.getCount()));
    }

    @SubscribeEvent
    public static void onFish(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (ItemStack stack : event.getDrops()) {
            QuestApi.progress(player, QuestApi.FISH, target -> Targets.item(target, stack), stack.getCount());
        }
    }

    /** Custom objective types reported by other modules (e.g. "cast" from ur-magic). */
    @SubscribeEvent
    public static void onProgress(com.untamedrealms.core.api.ProgressEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuestApi.progress(player, event.getType(), target -> Targets.id(target, event.getId()), event.getAmount());
        }
    }

    @SubscribeEvent
    public static void onSkillUp(SkillLevelUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuestApi.evaluateAllStatic(player);
    }

    @SubscribeEvent
    public static void onCharacterLevelUp(CharacterLevelUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuestApi.evaluateAllStatic(player);
    }

    /** Inventory (collect) objectives every second; location (visit / biome) objectives every two. */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        QuestLog log = QuestApi.log(player);
        if (log.active().isEmpty()) return;
        QuestApi.evaluateAllStatic(player);
        if (player.tickCount % 40 != 0) return;
        for (Map.Entry<ResourceLocation, QuestLog.Active> e : List.copyOf(log.active().entrySet())) {
            QuestDef def = QuestsData.get(e.getKey());
            if (def == null || e.getValue().stage >= def.stages().size()) continue;
            for (QuestDef.Objective obj : def.stages().get(e.getValue().stage).objectives()) {
                if (obj.type().equals(QuestApi.VISIT) && Targets.structure(obj.target(), player.serverLevel(), player.blockPosition())) {
                    QuestApi.progress(player, QuestApi.VISIT, t -> t.equals(obj.target()), obj.count());
                } else if (obj.type().equals(QuestApi.BIOME) && Targets.biome(obj.target(), player.serverLevel(), player.blockPosition())) {
                    QuestApi.progress(player, QuestApi.BIOME, t -> t.equals(obj.target()), obj.count());
                }
            }
        }
    }
}
