package com.untamedrealms.skills.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.untamedrealms.skills.UntamedSkills;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.data.SkillData;
import com.untamedrealms.skills.network.SkillsNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;

/** {@code /ur skills ...} admin commands. */
@EventBusSubscriber(modid = UntamedSkills.MODID)
public final class SkillsCommands {
    private static final SimpleCommandExceptionType UNKNOWN_SKILL =
            new SimpleCommandExceptionType(Component.translatable("command.urskills.unknown_skill"));

    private SkillsCommands() {}

    private static RequiredArgumentBuilder<CommandSourceStack, String> skillArg() {
        return Commands.argument("skill", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Skill.values()).map(Skill::id), builder));
    }

    private static Skill skill(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Skill skill = Skill.byId(StringArgumentType.getString(ctx, "skill"));
        if (skill == null) throw UNKNOWN_SKILL.create();
        return skill;
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ur").then(Commands.literal("skills")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("show").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    SkillData data = SkillsApi.data(target);
                    StringBuilder sb = new StringBuilder();
                    for (Skill s : Skill.VALUES) sb.append(s.id()).append('=').append(data.level(s)).append(' ');
                    ctx.getSource().sendSuccess(() -> Component.literal(target.getScoreboardName() + " L" + data.characterLevel()
                            + " perks:" + data.perkPoints() + " attr:" + data.attributePoints() + " | " + sb), false);
                    return 1;
                })))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player()).then(skillArg()
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 99)).executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            Skill s = skill(ctx);
                            int level = IntegerArgumentType.getInteger(ctx, "level");
                            SkillsApi.setLevel(target, s, level);
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.urskills.set", target.getDisplayName(), s.displayName(), level), true);
                            return 1;
                        })))))
                .then(Commands.literal("addxp").then(Commands.argument("player", EntityArgument.player()).then(skillArg()
                        .then(Commands.argument("xp", DoubleArgumentType.doubleArg(0)).executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            double xp = SkillsApi.addXp(target, skill(ctx), DoubleArgumentType.getDouble(ctx, "xp"));
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.urskills.addxp", String.format("%.1f", xp), target.getDisplayName()), true);
                            return 1;
                        })))))
                .then(Commands.literal("respec").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    int refunded = SkillsApi.data(target).respecPerks();
                    SkillsApi.refresh(target);
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.urskills.respec", target.getDisplayName(), refunded), true);
                    return 1;
                })))
                .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    SkillsApi.data(target).reset();
                    SkillsApi.refresh(target);
                    SkillsNetwork.syncNow(target);
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.urskills.reset", target.getDisplayName()), true);
                    return 1;
                })))));
    }
}
