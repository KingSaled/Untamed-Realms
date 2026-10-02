package com.untamedrealms.arcana;

import com.untamedrealms.arcana.data.ArcanaKnowledge;
import com.untamedrealms.arcana.data.IngredientDef;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Map;

/** /ur arcana learnall|fillgems|forget <player> (admin): every ingredient effect and enchantment; fill carried soul gems; forget it all. */
@EventBusSubscriber(modid = UntamedArcana.MODID)
public final class ArcanaCommands {
    private ArcanaCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ur").then(Commands.literal("arcana").requires(s -> s.hasPermission(2))
                .then(Commands.literal("learnall").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    ArcanaKnowledge knowledge = ArcanaApi.knowledge(target);
                    for (IngredientDef def : UntamedArcana.INGREDIENTS.values()) {
                        for (int i = 0; i < def.effects().size(); i++) knowledge.learn(def.item(), i);
                    }
                    target.registryAccess().registryOrThrow(Registries.ENCHANTMENT).holders()
                            .forEach(h -> knowledge.enchantments().add(h.key().location()));
                    ArcanaApi.sync(target);
                    ctx.getSource().sendSuccess(() -> Component.literal("Taught every ingredient effect and enchantment to " + target.getScoreboardName()), true);
                    return 1;
                })))
                .then(Commands.literal("fillgems").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    int filled = 0;
                    for (net.minecraft.world.item.ItemStack stack : target.getInventory().items) {
                        if (stack.getItem() instanceof com.untamedrealms.arcana.item.SoulGemItem gem && com.untamedrealms.arcana.item.SoulGemItem.soul(stack) == 0) {
                            stack.set(UntamedArcana.SOUL.get(), gem.capacity);
                            filled++;
                        }
                    }
                    int n = filled;
                    ctx.getSource().sendSuccess(() -> Component.literal("Filled " + n + " soul gem(s)"), true);
                    return n;
                })))
                .then(Commands.literal("forget").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    ArcanaApi.knowledge(target).replaceWith(Map.of(), List.of());
                    ArcanaApi.sync(target);
                    ctx.getSource().sendSuccess(() -> Component.literal("Cleared " + target.getScoreboardName() + "'s alchemy and enchanting knowledge"), true);
                    return 1;
                })))));
    }
}
