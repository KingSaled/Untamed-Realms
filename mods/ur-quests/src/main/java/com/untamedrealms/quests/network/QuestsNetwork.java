package com.untamedrealms.quests.network;

import com.untamedrealms.core.api.UR;
import com.untamedrealms.quests.UntamedQuests;
import com.untamedrealms.quests.block.Bounties;
import com.untamedrealms.quests.client.ClientQuests;
import com.untamedrealms.quests.engine.QuestApi;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = UntamedQuests.MODID)
public final class QuestsNetwork {
    private static final Set<UUID> DIRTY = ConcurrentHashMap.newKeySet();

    private QuestsNetwork() {}

    public record Sync(CompoundTag log) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(UntamedQuests.id("sync"));
        public static final StreamCodec<ByteBuf, Sync> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(Sync::new, Sync::log);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> client: where the tracked objective is (empty = no marker). */
    public record Marker(Optional<BlockPos> pos, Component label) implements CustomPacketPayload {
        public static final Type<Marker> TYPE = new Type<>(UntamedQuests.id("marker"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Marker> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), Marker::pos,
                ComponentSerialization.TRUSTED_STREAM_CODEC, Marker::label,
                Marker::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: track a quest (empty = untrack). */
    public record Track(Optional<ResourceLocation> quest) implements CustomPacketPayload {
        public static final Type<Track> TYPE = new Type<>(UntamedQuests.id("track"));
        public static final StreamCodec<ByteBuf, Track> STREAM_CODEC = ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).map(Track::new, Track::quest);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Abandon(ResourceLocation quest) implements CustomPacketPayload {
        public static final Type<Abandon> TYPE = new Type<>(UntamedQuests.id("abandon"));
        public static final StreamCodec<ByteBuf, Abandon> STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(Abandon::new, Abandon::quest);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> client: open a notice board offering these bounties. */
    public record OpenBoard(BlockPos pos, List<ResourceLocation> offers) implements CustomPacketPayload {
        public static final Type<OpenBoard> TYPE = new Type<>(UntamedQuests.id("open_board"));
        public static final StreamCodec<ByteBuf, OpenBoard> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenBoard::pos,
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(16)), OpenBoard::offers,
                OpenBoard::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record AcceptBounty(BlockPos pos, ResourceLocation quest) implements CustomPacketPayload {
        public static final Type<AcceptBounty> TYPE = new Type<>(UntamedQuests.id("accept_bounty"));
        public static final StreamCodec<ByteBuf, AcceptBounty> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, AcceptBounty::pos,
                ResourceLocation.STREAM_CODEC, AcceptBounty::quest,
                AcceptBounty::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(Sync.TYPE, Sync.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientQuests.onSync(p.log(), ctx.player().registryAccess())));
        r.playToClient(Marker.TYPE, Marker.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientQuests.onMarker(p.pos().orElse(null), p.label())));
        r.playToClient(OpenBoard.TYPE, OpenBoard.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientQuests.openBoard(p.pos(), p.offers())));
        r.playToServer(Track.TYPE, Track.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ResourceLocation id = p.quest().orElse(null);
            if (id == null || QuestApi.isActive(player, id)) {
                QuestApi.log(player).setTracked(id);
                markDirty(player);
            }
        }));
        r.playToServer(Abandon.TYPE, Abandon.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) QuestApi.abandon(player, p.quest());
        }));
        r.playToServer(AcceptBounty.TYPE, AcceptBounty.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (player.distanceToSqr(p.pos().getCenter()) > 64 || !Bounties.offers(player, p.pos()).contains(p.quest())) return;
            Component error = QuestApi.cannotStart(player, p.quest());
            if (error != null) UR.warn(player, error);
            else QuestApi.start(player, p.quest(), true);
        }));
    }

    public static void markDirty(ServerPlayer player) {
        DIRTY.add(player.getUUID());
    }

    public static void syncNow(ServerPlayer player) {
        DIRTY.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, new Sync(QuestApi.log(player).serializeNBT(player.registryAccess())));
    }

    public static void sendMarker(ServerPlayer player, @Nullable BlockPos pos, Component label) {
        PacketDistributor.sendToPlayer(player, new Marker(Optional.ofNullable(pos), label));
    }

    public static void openBoard(ServerPlayer player, BlockPos pos) {
        PacketDistributor.sendToPlayer(player, new OpenBoard(pos, Bounties.offers(player, pos)));
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 5 == 0 && DIRTY.contains(player.getUUID())) {
            syncNow(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DIRTY.remove(event.getEntity().getUUID());
    }
}
