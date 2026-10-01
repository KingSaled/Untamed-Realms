package com.untamedrealms.quests.engine;

import com.mojang.serialization.Dynamic;
import com.untamedrealms.core.api.UR;
import com.untamedrealms.core.economy.WalletApi;
import com.untamedrealms.quests.UntamedQuests;
import com.untamedrealms.quests.data.QuestDef;
import com.untamedrealms.quests.data.QuestLog;
import com.untamedrealms.quests.data.QuestsData;
import com.untamedrealms.quests.network.QuestsNetwork;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The quest engine's public API. Other modules start quests (NPC dialogue), report progress for
 * their own objective types ({@link #progress}) and query state for dialogue conditions.
 */
public final class QuestApi {
    /** Objective types the engine tracks itself. Any other type can be driven via {@link #progress}. */
    public static final String KILL = "kill", COLLECT = "collect", TALK = "talk", TURN_IN = "turn_in", VISIT = "visit",
            BIOME = "biome", MINE = "mine", CRAFT = "craft", SMELT = "smelt", FISH = "fish", SKILL = "skill", LEVEL = "level",
            DELIVER = "deliver";

    private QuestApi() {}

    public static QuestLog log(Player player) {
        return player.getData(UntamedQuests.QUEST_LOG);
    }

    public static boolean isActive(Player player, ResourceLocation quest) { return log(player).isActive(quest); }
    public static boolean isCompleted(Player player, ResourceLocation quest) { return log(player).isCompleted(quest); }

    public static int stage(Player player, ResourceLocation quest) {
        QuestLog.Active a = log(player).active().get(quest);
        return a == null ? -1 : a.stage;
    }

    // ------------------------------------------------------------------ starting / stopping

    /** Why a quest can't be started right now, or null if it can. */
    public static Component cannotStart(ServerPlayer player, ResourceLocation id) {
        QuestDef def = QuestsData.get(id);
        if (def == null) return Component.translatable("message.urquests.unknown");
        QuestLog log = log(player);
        if (log.isActive(id)) return Component.translatable("message.urquests.already_active");
        QuestLog.Completion done = log.completed().get(id);
        if (done != null) {
            if (!def.repeatable()) return Component.translatable("message.urquests.already_done");
            long readyAt = done.lastCompletedAt() + def.cooldownMinutes() * 1200L;
            if (player.serverLevel().getGameTime() < readyAt) return Component.translatable("message.urquests.cooldown");
        }
        for (ResourceLocation req : def.requirements().quests()) {
            if (!log.isCompleted(req)) {
                QuestDef reqDef = QuestsData.get(req);
                return Component.translatable("message.urquests.requires_quest", reqDef == null ? Component.literal(req.toString()) : reqDef.title());
            }
        }
        for (Map.Entry<Skill, Integer> e : def.requirements().skills().entrySet()) {
            if (SkillsApi.level(player, e.getKey()) < e.getValue())
                return Component.translatable("message.urquests.requires_skill", e.getKey().displayName(), e.getValue());
        }
        if (SkillsApi.characterLevel(player) < def.requirements().level())
            return Component.translatable("message.urquests.requires_level", def.requirements().level());
        return null;
    }

    public static boolean start(ServerPlayer player, ResourceLocation id, boolean announce) {
        if (cannotStart(player, id) != null) return false;
        QuestDef def = QuestsData.get(id);
        if (def.stages().isEmpty()) return false;
        QuestLog log = log(player);
        log.active().put(id, new QuestLog.Active(0, def.stages().get(0).objectives().size(), player.serverLevel().getGameTime()));
        if (log.tracked() == null || def.isMain()) log.setTracked(id);
        if (announce) {
            UR.banner(player, Component.translatable("banner.urquests.started", def.title()).withStyle(ChatFormatting.GOLD),
                    def.stages().get(0).description());
            player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.8f);
        }
        NeoForge.EVENT_BUS.post(new QuestEvent.Started(player, id));
        // Some objectives may already be satisfied (items in inventory, skill level...).
        evaluateStatic(player, id);
        QuestsNetwork.markDirty(player);
        return true;
    }

    public static boolean abandon(ServerPlayer player, ResourceLocation id) {
        QuestDef def = QuestsData.get(id);
        QuestLog log = log(player);
        if (!log.isActive(id) || (def != null && !def.abandonable())) return false;
        log.active().remove(id);
        if (id.equals(log.tracked())) log.setTracked(log.active().keySet().stream().findFirst().orElse(null));
        UR.subtle(player, Component.translatable("message.urquests.abandoned", def == null ? Component.literal(id.toString()) : def.title()));
        QuestsNetwork.markDirty(player);
        return true;
    }

    // ------------------------------------------------------------------ progress

    /**
     * Adds progress to every active objective of {@code type} whose target matches. Returns whether
     * anything changed. This is how other modules drive custom objective types (e.g. "cast").
     */
    public static boolean progress(ServerPlayer player, String type, Predicate<String> targetMatches, int amount) {
        QuestLog log = log(player);
        if (log.active().isEmpty()) return false;
        boolean changed = false;
        List<ResourceLocation> touched = new ArrayList<>();
        for (Map.Entry<ResourceLocation, QuestLog.Active> e : log.active().entrySet()) {
            QuestDef def = QuestsData.get(e.getKey());
            if (def == null || e.getValue().stage >= def.stages().size()) continue;
            List<QuestDef.Objective> objectives = def.stages().get(e.getValue().stage).objectives();
            QuestLog.Active active = ensureSize(e.getValue(), objectives.size());
            for (int i = 0; i < objectives.size(); i++) {
                QuestDef.Objective obj = objectives.get(i);
                if (!obj.type().equals(type) || active.progress[i] >= obj.count()) continue;
                if (type.equals(TURN_IN) && !othersDone(objectives, active, i)) continue;
                if (!targetMatches.test(obj.target())) continue;
                active.progress[i] = Math.min(obj.count(), active.progress[i] + amount);
                changed = true;
                touched.add(e.getKey());
                if (active.progress[i] >= obj.count() && objectives.size() > 1) {
                    UR.subtle(player, Component.translatable("message.urquests.objective_done", describe(obj)).withStyle(ChatFormatting.GRAY));
                }
            }
        }
        for (ResourceLocation id : touched) checkStage(player, id);
        if (changed) QuestsNetwork.markDirty(player);
        return changed;
    }

    /** Sets absolute progress (used for "collect" / "skill" style objectives that are re-evaluated). */
    static void setProgress(ServerPlayer player, ResourceLocation id, int index, int value) {
        QuestLog.Active active = log(player).active().get(id);
        if (active == null || index >= active.progress.length || active.progress[index] == value) return;
        active.progress[index] = value;
        QuestsNetwork.markDirty(player);
    }

    /**
     * Hands in {@code amount} items from inventory slot {@code slot} for a "deliver" objective (at a
     * notice board). Only matching items are taken, never more than the objective still needs.
     */
    public static boolean deliver(ServerPlayer player, ResourceLocation id, int index, int slot, int amount) {
        QuestLog.Active active = log(player).active().get(id);
        QuestDef def = QuestsData.get(id);
        if (active == null || def == null || active.stage >= def.stages().size()) return false;
        List<QuestDef.Objective> objectives = def.stages().get(active.stage).objectives();
        if (index < 0 || index >= objectives.size() || slot < 0 || slot >= player.getInventory().items.size()) return false;
        QuestDef.Objective obj = objectives.get(index);
        ensureSize(active, objectives.size());
        int needed = obj.count() - active.progress[index];
        ItemStack stack = player.getInventory().items.get(slot);
        if (!obj.type().equals(DELIVER) || needed <= 0 || !Targets.item(obj.target(), stack)) return false;
        int take = Math.min(Math.min(amount, stack.getCount()), needed);
        if (take <= 0) return false;
        stack.shrink(take);
        active.progress[index] += take;
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 0.8f);
        if (active.progress[index] >= obj.count() && objectives.size() > 1) {
            UR.subtle(player, Component.translatable("message.urquests.objective_done", describe(obj)).withStyle(ChatFormatting.GRAY));
        }
        checkStage(player, id);
        QuestsNetwork.markDirty(player);
        return true;
    }

    /** NPCs call this whenever the player talks to them. Handles both talk and turn-in objectives. */
    public static void onTalk(ServerPlayer player, ResourceLocation npc) {
        evaluateAllStatic(player);
        progress(player, TALK, target -> Targets.id(target, npc), 1);
        progress(player, TURN_IN, target -> Targets.id(target, npc), 1);
    }

    /** Re-evaluates collect / skill / level objectives of one quest from current player state. */
    public static void evaluateStatic(ServerPlayer player, ResourceLocation id) {
        QuestLog.Active active = log(player).active().get(id);
        QuestDef def = QuestsData.get(id);
        if (active == null || def == null || active.stage >= def.stages().size()) return;
        List<QuestDef.Objective> objectives = def.stages().get(active.stage).objectives();
        ensureSize(active, objectives.size());
        for (int i = 0; i < objectives.size(); i++) {
            QuestDef.Objective obj = objectives.get(i);
            switch (obj.type()) {
                case COLLECT -> setProgress(player, id, i, Math.min(obj.count(), countItems(player, obj.target())));
                case SKILL -> {
                    Skill skill = Skill.byId(obj.target());
                    if (skill != null) setProgress(player, id, i, SkillsApi.level(player, skill) >= obj.count() ? obj.count() : 0);
                }
                case LEVEL -> setProgress(player, id, i, SkillsApi.characterLevel(player) >= obj.count() ? obj.count() : 0);
                default -> { }
            }
        }
        checkStage(player, id);
    }

    public static void evaluateAllStatic(ServerPlayer player) {
        for (ResourceLocation id : List.copyOf(log(player).active().keySet())) evaluateStatic(player, id);
    }

    private static QuestLog.Active ensureSize(QuestLog.Active active, int size) {
        if (active.progress.length != size) {
            int[] resized = new int[size];
            System.arraycopy(active.progress, 0, resized, 0, Math.min(size, active.progress.length));
            active.progress = resized;
        }
        return active;
    }

    static boolean othersDone(List<QuestDef.Objective> objectives, QuestLog.Active active, int except) {
        for (int i = 0; i < objectives.size(); i++) {
            if (i != except && !objectives.get(i).type().equals(TURN_IN) && active.progress[i] < objectives.get(i).count()) return false;
        }
        return true;
    }

    static int countItems(ServerPlayer player, String target) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (Targets.item(target, stack)) total += stack.getCount();
        }
        if (Targets.item(target, player.getOffhandItem())) total += player.getOffhandItem().getCount();
        return total;
    }

    private static void consumeItems(ServerPlayer player, String target, int amount) {
        for (ItemStack stack : player.getInventory().items) {
            if (amount <= 0) return;
            if (Targets.item(target, stack)) {
                int take = Math.min(amount, stack.getCount());
                stack.shrink(take);
                amount -= take;
            }
        }
    }

    // ------------------------------------------------------------------ stage / quest completion

    private static void checkStage(ServerPlayer player, ResourceLocation id) {
        QuestLog log = log(player);
        QuestLog.Active active = log.active().get(id);
        QuestDef def = QuestsData.get(id);
        if (active == null || def == null) return;
        if (active.stage >= def.stages().size()) { complete(player, id, def); return; }
        QuestDef.Stage stage = def.stages().get(active.stage);
        ensureSize(active, stage.objectives().size());
        for (int i = 0; i < stage.objectives().size(); i++) {
            QuestDef.Objective obj = stage.objectives().get(i);
            if (obj.type().equals(COLLECT)) {
                // collect objectives must still be satisfied at the moment the stage completes
                if (countItems(player, obj.target()) < obj.count()) return;
            } else if (active.progress[i] < obj.count()) {
                return;
            }
        }
        for (QuestDef.Objective obj : stage.objectives()) {
            if (obj.type().equals(COLLECT) && obj.consume()) consumeItems(player, obj.target(), obj.count());
        }
        advance(player, id, def, active);
    }

    /**
     * Admin / test hub: finishes a quest's current stage whatever its objectives (nothing is taken from
     * the inventory). Returns false if the quest is not active.
     */
    public static boolean skipStage(ServerPlayer player, ResourceLocation id) {
        QuestLog.Active active = log(player).active().get(id);
        QuestDef def = QuestsData.get(id);
        if (active == null || def == null) return false;
        if (active.stage >= def.stages().size()) complete(player, id, def);
        else advance(player, id, def, active);
        QuestsNetwork.markDirty(player);
        return true;
    }

    private static void advance(ServerPlayer player, ResourceLocation id, QuestDef def, QuestLog.Active active) {
        NeoForge.EVENT_BUS.post(new QuestEvent.StageCompleted(player, id, active.stage));
        active.stage++;
        if (active.stage >= def.stages().size()) {
            complete(player, id, def);
            return;
        }
        QuestDef.Stage next = def.stages().get(active.stage);
        active.progress = new int[next.objectives().size()];
        UR.banner(player, Component.translatable("banner.urquests.updated", def.title()), next.description());
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1.2f);
        QuestsNetwork.markDirty(player);
        evaluateStatic(player, id);
    }

    private static void complete(ServerPlayer player, ResourceLocation id, QuestDef def) {
        QuestLog log = log(player);
        log.active().remove(id);
        QuestLog.Completion previous = log.completed().get(id);
        log.completed().put(id, new QuestLog.Completion(previous == null ? 1 : previous.times() + 1, player.serverLevel().getGameTime()));
        if (id.equals(log.tracked())) log.setTracked(log.active().keySet().stream().findFirst().orElse(null));

        QuestDef.Rewards rewards = def.rewards();
        rewards.xp().forEach((skill, xp) -> SkillsApi.addXp(player, skill, xp));
        if (rewards.coins() > 0) WalletApi.deposit(player, rewards.coins());
        for (Dynamic<?> raw : rewards.items()) giveItem(player, raw);
        if (rewards.perkPoints() > 0) {
            SkillsApi.data(player).addPerkPoints(rewards.perkPoints());
            SkillsApi.refresh(player);
        }
        UR.banner(player, Component.translatable("banner.urquests.completed", def.title()).withStyle(ChatFormatting.GOLD),
                rewards.coins() > 0 ? Component.translatable("banner.urquests.reward_coins", rewards.coins()) : Component.empty());
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.9f);
        NeoForge.EVENT_BUS.post(new QuestEvent.Completed(player, id));
        QuestsNetwork.markDirty(player);
    }

    private static <T> void giveItem(ServerPlayer player, Dynamic<T> raw) {
        RegistryOps<T> ops = RegistryOps.create(raw.getOps(), player.registryAccess());
        ItemStack.CODEC.parse(ops, raw.getValue())
                .resultOrPartial(err -> UntamedQuests.LOGGER.warn("Bad quest reward item: {}", err))
                .ifPresent(stack -> {
                    if (!player.getInventory().add(stack)) player.drop(stack, false);
                });
    }

    /** Human-readable objective text: the custom {@code text} if given, otherwise generated. */
    public static Component describe(QuestDef.Objective obj) {
        if (obj.text().isPresent()) return obj.text().get();
        String first = obj.target().split("\\|")[0].trim();
        return Component.translatable("objective.urquests." + obj.type(), prettyTarget(first), obj.count());
    }

    private static Component prettyTarget(String target) {
        if (target.equals("*") || target.isEmpty()) return Component.translatable("objective.urquests.anything");
        boolean tag = target.startsWith("#");
        ResourceLocation id = ResourceLocation.tryParse(tag ? target.substring(1) : target);
        if (id == null) return Component.literal(target);
        Skill skill = Skill.byId(target);
        if (skill != null) return skill.displayName();
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id);
        if (!tag && item.isPresent() && item.get() != net.minecraft.world.item.Items.AIR) return item.get().getDescription();
        var entity = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        if (!tag && entity.isPresent()) return entity.get().getDescription();
        String path = id.getPath();
        path = path.substring(path.lastIndexOf('/') + 1).replace('_', ' ');
        return Component.literal(Character.toUpperCase(path.charAt(0)) + path.substring(1));
    }
}
