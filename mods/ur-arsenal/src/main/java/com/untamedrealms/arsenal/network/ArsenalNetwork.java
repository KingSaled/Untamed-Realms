package com.untamedrealms.arsenal.network;

import com.untamedrealms.arsenal.UntamedArsenal;
import com.untamedrealms.arsenal.block.Station;
import com.untamedrealms.arsenal.client.ArsenalClient;
import com.untamedrealms.arsenal.station.StationLogic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
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

@EventBusSubscriber(modid = UntamedArsenal.MODID)
public final class ArsenalNetwork {
    private ArsenalNetwork() {}

    /** Server -> client: open a station window. */
    public record OpenStation(BlockPos pos, int station) implements CustomPacketPayload {
        public static final Type<OpenStation> TYPE = new Type<>(UntamedArsenal.id("open_station"));
        public static final StreamCodec<ByteBuf, OpenStation> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenStation::pos, ByteBufCodecs.VAR_INT, OpenStation::station, OpenStation::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: craft a station recipe {@code times} times. */
    public record Craft(BlockPos pos, ResourceLocation recipe, int times) implements CustomPacketPayload {
        public static final Type<Craft> TYPE = new Type<>(UntamedArsenal.id("craft"));
        public static final StreamCodec<ByteBuf, Craft> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Craft::pos, ResourceLocation.STREAM_CODEC, Craft::recipe, ByteBufCodecs.VAR_INT, Craft::times, Craft::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: temper the gear in an inventory slot at a workbench. */
    public record Temper(BlockPos pos, int slot) implements CustomPacketPayload {
        public static final Type<Temper> TYPE = new Type<>(UntamedArsenal.id("temper"));
        public static final StreamCodec<ByteBuf, Temper> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Temper::pos, ByteBufCodecs.VAR_INT, Temper::slot, Temper::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(OpenStation.TYPE, OpenStation.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> ArsenalClient.openStation(p.pos(), Station.values()[p.station()])));
        r.playToServer(Craft.TYPE, Craft.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) StationLogic.craft(player, p.pos(), p.recipe(), p.times());
        }));
        r.playToServer(Temper.TYPE, Temper.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) StationLogic.temper(player, p.pos(), p.slot());
        }));
    }

    public static void openStation(ServerPlayer player, BlockPos pos, Station station) {
        PacketDistributor.sendToPlayer(player, new OpenStation(pos, station.ordinal()));
    }
}
