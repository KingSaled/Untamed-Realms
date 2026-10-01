package com.untamedrealms.classes.command;

import com.untamedrealms.classes.ClassesApi;
import com.untamedrealms.classes.UntamedClasses;
import com.untamedrealms.classes.data.ClassData;
import com.untamedrealms.classes.network.ClassesNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /ur class [choose | info <player> | reset <player>] */
@EventBusSubscriber(modid = UntamedClasses.MODID)
public final class ClassesCommands {
    private ClassesCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ur").then(Commands.literal("class")
                .then(Commands.literal("choose").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (ClassesApi.hasChosen(player)) {
                        ctx.getSource().sendFailure(Component.translatable("message.urclasses.already_chosen"));
                        return 0;
                    }
                    ClassesNetwork.openSelection(player);
                    return 1;
                }))
                .then(Commands.literal("info").requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            ClassData data = ClassesApi.data(target);
                            ctx.getSource().sendSuccess(() -> Component.literal(target.getScoreboardName() + ": class="
                                    + data.classId() + " birthsign=" + data.birthsign()), false);
                            return 1;
                        })))
                .then(Commands.literal("reset").requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            ClassesApi.reset(target);
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.urclasses.reset", target.getDisplayName()), true);
                            return 1;
                        })))));
    }
}
