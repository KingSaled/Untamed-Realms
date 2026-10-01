package com.untamedrealms.classes.network;

import com.untamedrealms.classes.ClassesApi;
import com.untamedrealms.classes.UntamedClasses;
import com.untamedrealms.classes.client.ClientClasses;
import com.untamedrealms.classes.data.ClassData;
import com.untamedrealms.core.api.UR;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Optional;

@EventBusSubscriber(modid = UntamedClasses.MODID)
public final class ClassesNetwork {
    private ClassesNetwork() {}

    /** Server -> client: open the class selection screen. */
    public record OpenSelection(boolean first) implements CustomPacketPayload {
        public static final Type<OpenSelection> TYPE = new Type<>(UntamedClasses.id("open_selection"));
        public static final StreamCodec<ByteBuf, OpenSelection> STREAM_CODEC = ByteBufCodecs.BOOL.map(OpenSelection::new, OpenSelection::first);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> client: the local player's class and birthsign. */
    public record Sync(Optional<ResourceLocation> classId, Optional<ResourceLocation> birthsign) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(UntamedClasses.id("sync"));
        public static final StreamCodec<ByteBuf, Sync> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), Sync::classId,
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), Sync::birthsign,
                Sync::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: the player's choice. */
    public record Choose(ResourceLocation classId, ResourceLocation birthsign) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(UntamedClasses.id("choose"));
        public static final StreamCodec<ByteBuf, Choose> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Choose::classId,
                ResourceLocation.STREAM_CODEC, Choose::birthsign,
                Choose::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenSelection.TYPE, OpenSelection.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(ClientClasses::requestOpenSelection));
        registrar.playToClient(Sync.TYPE, Sync.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientClasses.onSync(payload.classId().orElse(null), payload.birthsign().orElse(null))));
        registrar.playToServer(Choose.TYPE, Choose.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                Component error = ClassesApi.choose(player, payload.classId(), payload.birthsign());
                if (error != null) UR.warn(player, error);
            }
        }));
    }

    public static void sync(ServerPlayer player) {
        ClassData data = ClassesApi.data(player);
        PacketDistributor.sendToPlayer(player, new Sync(Optional.ofNullable(data.classId()), Optional.ofNullable(data.birthsign())));
    }

    public static void openSelection(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new OpenSelection(true));
    }
}
