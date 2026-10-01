package com.untamedrealms.npcs.dialogue;

import com.mojang.serialization.Dynamic;
import com.untamedrealms.core.api.UR;
import com.untamedrealms.core.economy.WalletApi;
import com.untamedrealms.npcs.UntamedNpcs;
import com.untamedrealms.npcs.data.DialogueDef;
import com.untamedrealms.npcs.data.NpcDef;
import com.untamedrealms.npcs.data.NpcsData;
import com.untamedrealms.npcs.entity.NpcEntity;
import com.untamedrealms.npcs.network.NpcsNetwork;
import com.untamedrealms.npcs.shop.ShopManager;
import com.untamedrealms.quests.engine.QuestApi;
import com.untamedrealms.quests.engine.Targets;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.data.SkillData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side dialogue runner. Conditions are evaluated here and only the options the player may pick
 * are sent to the client, so the client never decides anything.
 */
public final class DialogueManager {
    /** Built-in option codes appended after the authored options. */
    private static final int GOODBYE = -1, SHOP = -2, TRAIN = -3;

    private record Session(int entityId, ResourceLocation dialogue, String node, List<Integer> options) {}

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private DialogueManager() {}

    public static void open(ServerPlayer player, NpcEntity npc) {
        NpcDef def = npc.def();
        if (def == null) return;
        QuestApi.onTalk(player, npc.npcId());
        DialogueDef dialogue = def.dialogue().map(NpcsData.DIALOGUES::getOrNull).orElse(null);
        String node = null;
        if (dialogue != null) {
            for (DialogueDef.Entry entry : dialogue.entry()) {
                if (allPass(player, entry.conditions())) { node = entry.node(); break; }
            }
        }
        show(player, npc, def.dialogue().orElse(null), dialogue, node);
    }

    private static void show(ServerPlayer player, NpcEntity npc, ResourceLocation dialogueId, DialogueDef dialogue, String nodeId) {
        NpcDef def = npc.def();
        DialogueDef.Node node = dialogue == null || nodeId == null ? null : dialogue.nodes().get(nodeId);
        Component text;
        List<Component> optionTexts = new ArrayList<>();
        List<Integer> codes = new ArrayList<>();
        if (node != null) {
            runActions(player, npc, node.actions());
            text = node.text();
            for (int i = 0; i < node.options().size(); i++) {
                DialogueDef.Option option = node.options().get(i);
                if (!allPass(player, option.conditions())) continue;
                Component label = option.text();
                if (option.check().isPresent()) {
                    DialogueDef.Check check = option.check().get();
                    Skill skill = Skill.byId(check.skill());
                    label = Component.translatable("dialogue.urnpcs.check", skill == null ? check.skill() : skill.displayName(), check.level())
                            .withStyle(ChatFormatting.GRAY).append(" ").append(label.copy().withStyle(ChatFormatting.WHITE));
                }
                optionTexts.add(label);
                codes.add(i);
            }
        } else {
            text = def.greeting().orElse(Component.translatable("dialogue.urnpcs.default_greeting"));
        }
        if (def.shop().isPresent()) {
            optionTexts.add(Component.translatable("dialogue.urnpcs.shop"));
            codes.add(SHOP);
        }
        if (def.trainer().isPresent()) {
            NpcDef.Trainer trainer = def.trainer().get();
            optionTexts.add(Component.translatable("dialogue.urnpcs.train", trainer.skill().displayName(),
                    trainingCost(SkillsApi.level(player, trainer.skill()))));
            codes.add(TRAIN);
        }
        optionTexts.add(Component.translatable("dialogue.urnpcs.goodbye"));
        codes.add(GOODBYE);

        SESSIONS.put(player.getUUID(), new Session(npc.getId(), dialogueId, nodeId, codes));
        NpcsNetwork.openDialogue(player, npc, text, optionTexts);
    }

    public static void choose(ServerPlayer player, int entityId, int index) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.entityId() != entityId || index < 0 || index >= session.options().size()) return;
        if (!(player.level().getEntity(entityId) instanceof NpcEntity npc) || npc.distanceToSqr(player) > 100) {
            close(player);
            return;
        }
        int code = session.options().get(index);
        switch (code) {
            case GOODBYE -> { close(player); return; }
            case SHOP -> { npc.def().shop().ifPresent(shop -> ShopManager.open(player, npc, shop)); SESSIONS.remove(player.getUUID()); return; }
            case TRAIN -> { train(player, npc); return; }
            default -> { }
        }
        DialogueDef dialogue = NpcsData.DIALOGUES.getOrNull(session.dialogue());
        DialogueDef.Node node = dialogue == null ? null : dialogue.nodes().get(session.node());
        if (node == null || code >= node.options().size()) { close(player); return; }
        DialogueDef.Option option = node.options().get(code);
        if (!allPass(player, option.conditions())) { close(player); return; }

        String next = option.next().orElse(null);
        if (option.check().isPresent()) {
            DialogueDef.Check check = option.check().get();
            Skill skill = Skill.byId(check.skill());
            boolean pass = skill != null && SkillsApi.level(player, skill) >= check.level();
            if (pass) {
                SkillsApi.addXp(player, skill, 20 + check.level());
                UR.subtle(player, Component.translatable("dialogue.urnpcs.check_passed").withStyle(ChatFormatting.GREEN));
            } else {
                UR.subtle(player, Component.translatable("dialogue.urnpcs.check_failed").withStyle(ChatFormatting.RED));
                next = check.fail();
            }
            if (!pass) {
                show(player, npc, session.dialogue(), dialogue, next);
                return;
            }
        }
        if (runActions(player, npc, option.actions())) return; // an action took over (e.g. opened the shop)
        if (next == null) close(player);
        else show(player, npc, session.dialogue(), dialogue, next);
    }

    public static void close(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
        NpcsNetwork.closeDialogue(player);
    }

    public static void forget(UUID player) {
        SESSIONS.remove(player);
    }

    // ------------------------------------------------------------------ conditions

    public static boolean allPass(ServerPlayer player, List<DialogueDef.Condition> conditions) {
        for (DialogueDef.Condition c : conditions) {
            if (test(player, c) == c.negate()) return false;
        }
        return true;
    }

    private static boolean test(ServerPlayer player, DialogueDef.Condition c) {
        ResourceLocation id = ResourceLocation.tryParse(c.target());
        return switch (c.type()) {
            case "quest_active" -> id != null && QuestApi.isActive(player, id);
            case "quest_completed" -> id != null && QuestApi.isCompleted(player, id);
            case "quest_not_started" -> id != null && !QuestApi.isActive(player, id) && !QuestApi.isCompleted(player, id);
            case "quest_available" -> id != null && QuestApi.cannotStart(player, id) == null;
            case "quest_stage" -> id != null && QuestApi.stage(player, id) == c.value();
            case "skill" -> {
                Skill skill = Skill.byId(c.target());
                yield skill != null && SkillsApi.level(player, skill) >= c.value();
            }
            case "level" -> SkillsApi.characterLevel(player) >= c.value();
            case "has_item" -> countItems(player, c.target()) >= Math.max(1, c.value());
            case "coins" -> WalletApi.canAfford(player, c.value());
            case "flag" -> QuestApi.log(player).flags().contains(c.target());
            case "not_flag" -> !QuestApi.log(player).flags().contains(c.target());
            default -> {
                UntamedNpcs.LOGGER.warn("Unknown dialogue condition type '{}'", c.type());
                yield false;
            }
        };
    }

    private static int countItems(ServerPlayer player, String target) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) if (Targets.item(target, stack)) total += stack.getCount();
        return total;
    }

    // ------------------------------------------------------------------ actions

    /** Runs actions; returns true if one of them replaced the dialogue screen. */
    private static boolean runActions(ServerPlayer player, NpcEntity npc, List<DialogueDef.Action> actions) {
        boolean tookOver = false;
        for (DialogueDef.Action a : actions) {
            ResourceLocation id = ResourceLocation.tryParse(a.target());
            switch (a.type()) {
                case "start_quest" -> { if (id != null) QuestApi.start(player, id, true); }
                case "give_item" -> a.item().ifPresent(raw -> give(player, raw));
                case "take_item" -> take(player, a.target(), Math.max(1, a.value()));
                case "give_coins" -> WalletApi.deposit(player, a.value());
                case "take_coins" -> WalletApi.tryWithdraw(player, a.value());
                case "set_flag" -> QuestApi.log(player).flags().add(a.target());
                case "clear_flag" -> QuestApi.log(player).flags().remove(a.target());
                case "give_xp" -> {
                    Skill skill = Skill.byId(a.target());
                    if (skill != null) SkillsApi.addXp(player, skill, a.value());
                }
                case "open_shop" -> {
                    ResourceLocation shop = id != null ? id : npc.def().shop().orElse(null);
                    if (shop != null) { ShopManager.open(player, npc, shop); tookOver = true; }
                }
                case "train" -> { train(player, npc); tookOver = true; }
                case "end" -> { close(player); tookOver = true; }
                default -> UntamedNpcs.LOGGER.warn("Unknown dialogue action type '{}'", a.type());
            }
        }
        return tookOver;
    }

    private static <T> void give(ServerPlayer player, Dynamic<T> raw) {
        ItemStack.CODEC.parse(RegistryOps.create(raw.getOps(), player.registryAccess()), raw.getValue()).result().ifPresent(stack -> {
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        });
    }

    private static void take(ServerPlayer player, String target, int amount) {
        for (ItemStack stack : player.getInventory().items) {
            if (amount <= 0) return;
            if (Targets.item(target, stack)) {
                int n = Math.min(amount, stack.getCount());
                stack.shrink(n);
                amount -= n;
            }
        }
    }

    // ------------------------------------------------------------------ training

    public static int trainingCost(int currentLevel) {
        return 15 * (currentLevel + 1);
    }

    private static void train(ServerPlayer player, NpcEntity npc) {
        NpcDef.Trainer trainer = npc.def().trainer().orElse(null);
        if (trainer == null) return;
        SkillData data = SkillsApi.data(player);
        int level = SkillsApi.level(player, trainer.skill());
        if (level >= trainer.maxLevel()) {
            UR.warn(player, Component.translatable("dialogue.urnpcs.train_maxed", npc.getDisplayName()));
        } else if (data.trainedThisLevel() >= 5) {
            UR.warn(player, Component.translatable("dialogue.urnpcs.train_limit"));
        } else if (!WalletApi.tryWithdraw(player, trainingCost(level))) {
            UR.warn(player, Component.translatable("dialogue.urnpcs.train_cost", trainingCost(level)));
        } else {
            data.setTrainedThisLevel(data.trainedThisLevel() + 1);
            SkillsApi.grantLevels(player, trainer.skill(), 1);
        }
        open(player, npc);
    }
}
