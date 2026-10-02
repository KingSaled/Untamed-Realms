package com.untamedrealms.arcana.network;

import com.untamedrealms.arcana.ArcanaApi;
import com.untamedrealms.arcana.UntamedArcana;
import com.untamedrealms.arcana.alchemy.AlchemyLab;
import com.untamedrealms.arcana.client.ClientArcana;
import com.untamedrealms.arcana.data.ArcanaKnowledge;
import com.untamedrealms.arcana.enchanting.ArcaneEnchanter;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = UntamedArcana.MODID)
public final class ArcanaNetwork {
    private ArcanaNetwork() {}

    /** Server -> client: open the alchemy lab ({@code enchanter} false) or the arcane enchanter. */
    public record OpenStation(BlockPos pos, boolean enchanter) implements CustomPacketPayload {
        public static final Type<OpenStation> TYPE = new Type<>(UntamedArcana.id("open_station"));
        public static final StreamCodec<ByteBuf, OpenStation> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenStation::pos, ByteBufCodecs.BOOL, OpenStation::enchanter, OpenStation::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> client: what the player knows. */
    public record Knowledge(Map<ResourceLocation, Integer> ingredients, List<ResourceLocation> enchantments) implements CustomPacketPayload {
        public static final Type<Knowledge> TYPE = new Type<>(UntamedArcana.id("knowledge"));
        public static final StreamCodec<ByteBuf, Knowledge> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT), Knowledge::ingredients,
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), Knowledge::enchantments, Knowledge::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: eat one of an ingredient at the lab to learn its first effects. */
    public record Taste(BlockPos pos, ResourceLocation item) implements CustomPacketPayload {
        public static final Type<Taste> TYPE = new Type<>(UntamedArcana.id("taste"));
        public static final StreamCodec<ByteBuf, Taste> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Taste::pos, ResourceLocation.STREAM_CODEC, Taste::item, Taste::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: brew from one of each of these ingredients, {@code times} times. */
    public record Brew(BlockPos pos, List<ResourceLocation> items, int times) implements CustomPacketPayload {
        public static final Type<Brew> TYPE = new Type<>(UntamedArcana.id("brew"));
        public static final StreamCodec<ByteBuf, Brew> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Brew::pos, ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(3)), Brew::items,
                ByteBufCodecs.VAR_INT, Brew::times, Brew::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: disenchant the item in an inventory slot. */
    public record Disenchant(BlockPos pos, int slot) implements CustomPacketPayload {
        public static final Type<Disenchant> TYPE = new Type<>(UntamedArcana.id("disenchant"));
        public static final StreamCodec<ByteBuf, Disenchant> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Disenchant::pos, ByteBufCodecs.VAR_INT, Disenchant::slot, Disenchant::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: enchant the item in {@code itemSlot} with a known enchantment and the soul gem in {@code gemSlot}. */
    public record Enchant(BlockPos pos, int itemSlot, ResourceLocation enchantment, int gemSlot) implements CustomPacketPayload {
        public static final Type<Enchant> TYPE = new Type<>(UntamedArcana.id("enchant"));
        public static final StreamCodec<ByteBuf, Enchant> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Enchant::pos, ByteBufCodecs.VAR_INT, Enchant::itemSlot,
                ResourceLocation.STREAM_CODEC, Enchant::enchantment, ByteBufCodecs.VAR_INT, Enchant::gemSlot, Enchant::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(OpenStation.TYPE, OpenStation.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientArcana.open(p.pos(), p.enchanter())));
        r.playToClient(Knowledge.TYPE, Knowledge.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientArcana.accept(p.ingredients(), p.enchantments())));
        r.playToServer(Taste.TYPE, Taste.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) AlchemyLab.taste(player, p.pos(), p.item());
        }));
        r.playToServer(Brew.TYPE, Brew.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) AlchemyLab.brew(player, p.pos(), p.items(), p.times());
        }));
        r.playToServer(Disenchant.TYPE, Disenchant.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) ArcaneEnchanter.disenchant(player, p.pos(), p.slot());
        }));
        r.playToServer(Enchant.TYPE, Enchant.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) ArcaneEnchanter.enchant(player, p.pos(), p.itemSlot(), p.enchantment(), p.gemSlot());
        }));
    }

    public static void open(ServerPlayer player, BlockPos pos, boolean enchanter) {
        ArcanaApi.sync(player);
        PacketDistributor.sendToPlayer(player, new OpenStation(pos, enchanter));
    }

    public static void syncKnowledge(ServerPlayer player, ArcanaKnowledge knowledge) {
        if (player.connection == null || !player.connection.hasChannel(Knowledge.TYPE)) return;
        PacketDistributor.sendToPlayer(player, new Knowledge(new HashMap<>(knowledge.ingredients()), new ArrayList<>(knowledge.enchantments())));
    }
}
