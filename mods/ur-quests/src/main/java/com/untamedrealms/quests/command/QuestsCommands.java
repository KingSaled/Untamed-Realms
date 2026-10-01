package com.untamedrealms.quests.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.untamedrealms.quests.UntamedQuests;
import com.untamedrealms.quests.data.QuestLog;
import com.untamedrealms.quests.data.QuestsData;
import com.untamedrealms.quests.engine.QuestApi;
import com.untamedrealms.quests.network.QuestsNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /ur quest start|abandon|forget|reset ... (admin) and /ur quest list. */
@EventBusSubscriber(modid = UntamedQuests.MODID)
public final class QuestsCommands {
    private QuestsCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        var questArg = Commands.argument("quest", ResourceLocationArgument.id())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(QuestsData.QUESTS.entries().keySet(), b));
        event.getDispatcher().register(Commands.literal("ur").then(Commands.literal("quest")
                .then(Commands.literal("list").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    QuestLog log = QuestApi.log(player);
                    ctx.getSource().sendSuccess(() -> Component.literal("Active: " + log.active().keySet() + "  Completed: " + log.completed().size()), false);
                    return 1;
                }))
                .then(Commands.literal("start").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).then(questArg.executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            ResourceLocation id = ResourceLocationArgument.getId(ctx, "quest");
                            Component error = QuestApi.cannotStart(target, id);
                            if (error != null) { ctx.getSource().sendFailure(error); return 0; }
                            QuestApi.start(target, id, true);
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.urquests.started", id.toString(), target.getDisplayName()), true);
                            return 1;
                        }))))
                .then(Commands.literal("abandon").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("quest", ResourceLocationArgument.id()).executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            ResourceLocation id = ResourceLocationArgument.getId(ctx, "quest");
                            QuestApi.log(target).active().remove(id);
                            QuestsNetwork.markDirty(target);
                            return 1;
                        }))))
                .then(Commands.literal("forget").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("quest", ResourceLocationArgument.id()).executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            ResourceLocation id = ResourceLocationArgument.getId(ctx, "quest");
                            QuestApi.log(target).active().remove(id);
                            QuestApi.log(target).completed().remove(id);
                            QuestsNetwork.markDirty(target);
                            return 1;
                        }))))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            QuestApi.log(target).deserializeNBT(target.registryAccess(), new net.minecraft.nbt.CompoundTag());
                            QuestsNetwork.markDirty(target);
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.urquests.reset", target.getDisplayName()), true);
                            return 1;
                        })))
                .then(Commands.literal("skip").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            // no quest given: the tracked one
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            ResourceLocation id = QuestApi.log(target).tracked();
                            if (id == null || !QuestApi.skipStage(target, id)) {
                                ctx.getSource().sendFailure(Component.translatable("command.urquests.nothing_to_skip"));
                                return 0;
                            }
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.urquests.skipped", id.toString()), true);
                            return 1;
                        }).then(Commands.argument("quest", ResourceLocationArgument.id()).executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            ResourceLocation id = ResourceLocationArgument.getId(ctx, "quest");
                            if (!QuestApi.skipStage(target, id)) {
                                ctx.getSource().sendFailure(Component.translatable("command.urquests.nothing_to_skip"));
                                return 0;
                            }
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.urquests.skipped", id.toString()), true);
                            return 1;
                        }))))
                .then(Commands.literal("talk").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("npc", ResourceLocationArgument.id()).executes(ctx -> {
                            QuestApi.onTalk(EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "npc"));
                            return 1;
                        }))))));
    }
}
