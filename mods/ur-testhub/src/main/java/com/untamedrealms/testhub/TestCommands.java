package com.untamedrealms.testhub;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.untamedrealms.classes.ClassesApi;
import com.untamedrealms.core.economy.WalletApi;
import com.untamedrealms.magic.network.MagicNetwork;
import com.untamedrealms.magic.spell.MagicApi;
import com.untamedrealms.quests.QuestsConfig;
import com.untamedrealms.quests.data.QuestLog;
import com.untamedrealms.quests.data.QuestsData;
import com.untamedrealms.quests.engine.QuestApi;
import com.untamedrealms.quests.network.QuestsNetwork;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.network.SkillsNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** {@code /ur test ...}: the control book's shortcuts. Only registered in test world mode. */
public final class TestCommands {
    private TestCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ur").then(Commands.literal("test").requires(s -> s.hasPermission(2))
                .then(Commands.literal("book").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.getInventory().add(ControlBook.create(player.server));
                    return 1;
                }))
                .then(Commands.literal("hub").executes(ctx -> teleport(ctx.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("village").executes(ctx -> teleport(ctx.getSource().getPlayerOrException(), false)))
                .then(Commands.literal("rebuild").executes(ctx -> {
                    HubBuilder.buildAndRemember(ctx.getSource().getServer().overworld());
                    ctx.getSource().sendSuccess(() -> Component.literal("Test hub rebuilt."), true);
                    return 1;
                }))
                .then(Commands.literal("dummies").executes(ctx -> {
                    ServerLevel overworld = ctx.getSource().getServer().overworld();
                    BlockPos center = HubData.get(overworld).center;
                    if (center == null) return 0;
                    HubBuilder.respawnDummies(overworld, center);
                    return 1;
                }))
                .then(Commands.literal("skills").then(Commands.argument("level", IntegerArgumentType.integer(1, 99)).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    int level = IntegerArgumentType.getInteger(ctx, "level");
                    for (Skill skill : Skill.VALUES) SkillsApi.setLevel(player, skill, level);
                    SkillsNetwork.syncNow(player);
                    ctx.getSource().sendSuccess(() -> Component.literal("Every skill set to " + level + "."), false);
                    return 1;
                })))
                .then(Commands.literal("reset").executes(ctx -> {
                    reset(ctx.getSource().getPlayerOrException());
                    ctx.getSource().sendSuccess(() -> Component.literal("Character reset. Pick a class to start again."), false);
                    return 1;
                }))));
    }

    private static int teleport(ServerPlayer player, boolean hub) {
        ServerLevel overworld = player.server.overworld();
        HubData data = HubData.get(overworld);
        BlockPos target = hub ? data.center : data.village;
        if (target == null) return 0;
        player.teleportTo(overworld, target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5, player.getYRot(), player.getXRot());
        return 1;
    }

    /** Wipes the character as if new: skills, perks, spells, quests, coins and inventory; then character creation. */
    private static void reset(ServerPlayer player) {
        SkillsApi.data(player).reset();
        SkillsApi.refresh(player);
        SkillsNetwork.syncNow(player);

        MagicApi.book(player).deserializeNBT(player.registryAccess(), new CompoundTag());
        MagicNetwork.sync(player);
        com.untamedrealms.arcana.ArcanaApi.knowledge(player).replaceWith(java.util.Map.of(), java.util.List.of());
        com.untamedrealms.arcana.ArcanaApi.sync(player);

        QuestLog log = QuestApi.log(player);
        log.deserializeNBT(player.registryAccess(), new CompoundTag());
        log.flags().add("starting_village");
        log.flags().add("urtesthub_book");
        log.flags().add("started_intro");
        ResourceLocation intro = ResourceLocation.tryParse(QuestsConfig.STARTING_QUEST.get());
        if (intro != null && QuestsData.QUESTS.contains(intro)) QuestApi.start(player, intro, true);
        QuestsNetwork.markDirty(player);

        WalletApi.set(player, 0);
        player.getInventory().clearContent();
        player.getInventory().add(ControlBook.create(player.server));
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        ClassesApi.reset(player);
    }
}
