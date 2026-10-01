package com.untamedrealms.core.network;

import com.untamedrealms.core.UntamedCore;
import com.untamedrealms.core.client.ClientCoreState;
import com.untamedrealms.core.client.NotificationHud;
import com.untamedrealms.core.data.DataRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration for the core module. Client-bound handlers delegate through lambdas so that
 * client-only classes are never resolved on a dedicated server.
 */
@EventBusSubscriber(modid = UntamedCore.MODID)
public final class CoreNetwork {
    public static final String PROTOCOL = "1";

    /** Notification styles understood by {@link NotificationHud}. */
    public static final int STYLE_BANNER = 0;   // big centred banner (level up, quest complete)
    public static final int STYLE_SUBTLE = 1;   // smaller line under the banner area
    public static final int STYLE_WARNING = 2;  // red action-bar style warning

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(CorePayloads.VitalsSync.TYPE, CorePayloads.VitalsSync.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientCoreState.onVitals(payload)));
        registrar.playToClient(CorePayloads.WalletSync.TYPE, CorePayloads.WalletSync.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientCoreState.onWallet(payload)));
        registrar.playToClient(CorePayloads.Notify.TYPE, CorePayloads.Notify.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> NotificationHud.push(payload.style(), payload.title(), payload.subtitle())));
        registrar.playToClient(CorePayloads.DataSync.TYPE, CorePayloads.DataSync.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> DataRegistry.acceptSync(payload, ctx.player().registryAccess())));
    }

    public static void notify(ServerPlayer player, int style, Component title, Component subtitle) {
        PacketDistributor.sendToPlayer(player, new CorePayloads.Notify(style, title, subtitle));
    }

    private CoreNetwork() {}
}
