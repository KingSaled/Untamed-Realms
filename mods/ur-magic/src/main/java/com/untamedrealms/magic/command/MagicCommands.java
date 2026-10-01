package com.untamedrealms.magic.command;

import com.untamedrealms.magic.UntamedMagic;
import com.untamedrealms.magic.network.MagicNetwork;
import com.untamedrealms.magic.spell.MagicApi;
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

/** /ur magic learn|forget|learnall <player> [spell] (admin). */
@EventBusSubscriber(modid = UntamedMagic.MODID)
public final class MagicCommands {
    private MagicCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ur").then(Commands.literal("magic").requires(s -> s.hasPermission(2))
                .then(Commands.literal("learn").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("spell", ResourceLocationArgument.id())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(UntamedMagic.SPELLS.entries().keySet(), b))
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "spell");
                                    return MagicApi.learn(target, id) ? 1 : 0;
                                }))))
                .then(Commands.literal("learnall").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    UntamedMagic.SPELLS.entries().keySet().forEach(id -> MagicApi.book(target).learn(id));
                    MagicNetwork.sync(target);
                    ctx.getSource().sendSuccess(() -> Component.literal("Taught every spell to " + target.getScoreboardName()), true);
                    return 1;
                })))
                .then(Commands.literal("forget").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    MagicApi.book(target).deserializeNBT(target.registryAccess(), new net.minecraft.nbt.CompoundTag());
                    MagicNetwork.sync(target);
                    return 1;
                })))));
    }
}
