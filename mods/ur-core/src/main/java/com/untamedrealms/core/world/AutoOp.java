package com.untamedrealms.core.world;

import com.untamedrealms.core.CoreConfig;
import com.untamedrealms.core.UntamedCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Makes the usernames in the {@code admin.autoOp} server config operators when they join. */
@EventBusSubscriber(modid = UntamedCore.MODID)
public final class AutoOp {
    private AutoOp() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String name = player.getGameProfile().getName();
        if (CoreConfig.AUTO_OP.get().stream().noneMatch(name::equalsIgnoreCase)) return;
        PlayerList players = player.server.getPlayerList();
        if (players.isOp(player.getGameProfile())) return;
        players.op(player.getGameProfile());
        UntamedCore.LOGGER.info("Made {} an operator (admin.autoOp)", name);
    }
}
