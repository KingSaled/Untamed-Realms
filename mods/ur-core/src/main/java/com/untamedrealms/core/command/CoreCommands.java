package com.untamedrealms.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.untamedrealms.core.UntamedCore;
import com.untamedrealms.core.economy.WalletApi;
import com.untamedrealms.core.vitals.VitalsApi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /ur ...} root command. Every module registers its own sub-commands under the same
 * {@code ur} literal; Brigadier merges them into one tree.
 */
@EventBusSubscriber(modid = UntamedCore.MODID)
public final class CoreCommands {
    private CoreCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("ur")
                .then(Commands.literal("wallet")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.urcore.wallet.self", WalletApi.balance(player)), false);
                            return 1;
                        })
                        .then(Commands.literal("set").requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    long amount = LongArgumentType.getLong(ctx, "amount");
                                                    WalletApi.set(target, amount);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.urcore.wallet.set", target.getDisplayName(), amount), true);
                                                    return 1;
                                                }))))
                        .then(Commands.literal("give").requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    long amount = LongArgumentType.getLong(ctx, "amount");
                                                    WalletApi.deposit(target, amount);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.urcore.wallet.give", amount, target.getDisplayName()), true);
                                                    return 1;
                                                })))))
                .then(Commands.literal("vitals").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("refill")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            VitalsApi.restoreMagicka(target, Float.MAX_VALUE / 4);
                                            VitalsApi.restoreStamina(target, Float.MAX_VALUE / 4);
                                            target.setHealth(target.getMaxHealth());
                                            ctx.getSource().sendSuccess(() -> Component.translatable("command.urcore.vitals.refill", target.getDisplayName()), true);
                                            return 1;
                                        })))));
    }
}
