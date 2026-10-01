package com.untamedrealms.npcs.command;

import com.untamedrealms.npcs.UntamedNpcs;
import com.untamedrealms.npcs.data.NpcDef;
import com.untamedrealms.npcs.data.NpcsData;
import com.untamedrealms.npcs.data.SettlementDef;
import com.untamedrealms.npcs.entity.NpcEntity;
import com.untamedrealms.npcs.settlement.Settlements;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /ur npc spawn <npc> | /ur npc populate <settlement> - tools for builders placing towns by hand. */
@EventBusSubscriber(modid = UntamedNpcs.MODID)
public final class NpcsCommands {
    private NpcsCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ur").then(Commands.literal("npc").requires(s -> s.hasPermission(2))
                .then(Commands.literal("spawn").then(Commands.argument("npc", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(NpcsData.NPCS.entries().keySet(), b))
                        .executes(ctx -> {
                            ResourceLocation id = ResourceLocationArgument.getId(ctx, "npc");
                            NpcDef def = NpcsData.NPCS.getOrNull(id);
                            if (def == null) { ctx.getSource().sendFailure(Component.translatable("command.urnpcs.unknown", id.toString())); return 0; }
                            ServerLevel level = ctx.getSource().getLevel();
                            Vec3 pos = ctx.getSource().getPosition();
                            NpcEntity npc = UntamedNpcs.NPC.get().create(level);
                            if (npc == null) return 0;
                            npc.moveTo(pos.x, pos.y, pos.z, ctx.getSource().getRotation().y, 0);
                            npc.setup(id, def);
                            npc.finalizeSpawn(level, level.getCurrentDifficultyAt(npc.blockPosition()), MobSpawnType.COMMAND, null);
                            level.addFreshEntity(npc);
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.urnpcs.spawned", npc.getDisplayName(), def.title()), true);
                            return 1;
                        })))
                .then(Commands.literal("populate").then(Commands.argument("settlement", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(NpcsData.SETTLEMENTS.entries().keySet(), b))
                        .executes(ctx -> {
                            SettlementDef def = NpcsData.SETTLEMENTS.getOrNull(ResourceLocationArgument.getId(ctx, "settlement"));
                            if (def == null) return 0;
                            ServerLevel level = ctx.getSource().getLevel();
                            var p = net.minecraft.core.BlockPos.containing(ctx.getSource().getPosition());
                            Settlements.populate(level, new BoundingBox(p), def, level.getRandom());
                            return 1;
                        })))));
    }
}
