package com.untamedrealms.npcs.network;

import com.untamedrealms.core.economy.WalletApi;
import com.untamedrealms.npcs.UntamedNpcs;
import com.untamedrealms.npcs.client.ClientNpcs;
import com.untamedrealms.npcs.dialogue.DialogueManager;
import com.untamedrealms.npcs.dialogue.Pickpocket;
import com.untamedrealms.npcs.entity.NpcEntity;
import com.untamedrealms.npcs.shop.ShopManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

@EventBusSubscriber(modid = UntamedNpcs.MODID)
public final class NpcsNetwork {
    private NpcsNetwork() {}

    public record OpenDialogue(int entityId, Component name, Component title, Component text, List<Component> options) implements CustomPacketPayload {
        public static final Type<OpenDialogue> TYPE = new Type<>(UntamedNpcs.id("open_dialogue"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenDialogue> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, OpenDialogue::entityId,
                ComponentSerialization.STREAM_CODEC, OpenDialogue::name,
                ComponentSerialization.STREAM_CODEC, OpenDialogue::title,
                ComponentSerialization.STREAM_CODEC, OpenDialogue::text,
                ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list(32)), OpenDialogue::options,
                OpenDialogue::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record CloseDialogue(boolean unused) implements CustomPacketPayload {
        public static final Type<CloseDialogue> TYPE = new Type<>(UntamedNpcs.id("close_dialogue"));
        public static final StreamCodec<ByteBuf, CloseDialogue> STREAM_CODEC = ByteBufCodecs.BOOL.map(CloseDialogue::new, CloseDialogue::unused);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ChooseOption(int entityId, int index) implements CustomPacketPayload {
        public static final Type<ChooseOption> TYPE = new Type<>(UntamedNpcs.id("choose_option"));
        public static final StreamCodec<ByteBuf, ChooseOption> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ChooseOption::entityId,
                ByteBufCodecs.VAR_INT, ChooseOption::index,
                ChooseOption::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Offer(ItemStack stack, int price) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Offer> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, Offer::stack,
                ByteBufCodecs.VAR_INT, Offer::price,
                Offer::new);
    }

    public record OpenShop(int entityId, Component name, List<Offer> sells, List<Offer> buys, long balance) implements CustomPacketPayload {
        public static final Type<OpenShop> TYPE = new Type<>(UntamedNpcs.id("open_shop"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenShop> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, OpenShop::entityId,
                ComponentSerialization.STREAM_CODEC, OpenShop::name,
                Offer.STREAM_CODEC.apply(ByteBufCodecs.list(128)), OpenShop::sells,
                Offer.STREAM_CODEC.apply(ByteBufCodecs.list(128)), OpenShop::buys,
                ByteBufCodecs.VAR_LONG, OpenShop::balance,
                OpenShop::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: buy (quantity > 0) or sell (quantity == 0: one, -1: all) the offer at index. */
    public record Trade(int entityId, boolean buying, int index, int quantity) implements CustomPacketPayload {
        public static final Type<Trade> TYPE = new Type<>(UntamedNpcs.id("trade"));
        public static final StreamCodec<ByteBuf, Trade> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Trade::entityId,
                ByteBufCodecs.BOOL, Trade::buying,
                ByteBufCodecs.VAR_INT, Trade::index,
                ByteBufCodecs.VAR_INT, Trade::quantity,
                Trade::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(OpenDialogue.TYPE, OpenDialogue.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientNpcs.openDialogue(p)));
        r.playToClient(CloseDialogue.TYPE, CloseDialogue.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(ClientNpcs::closeDialogue));
        r.playToClient(OpenShop.TYPE, OpenShop.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientNpcs.openShop(p)));
        r.playToServer(ChooseOption.TYPE, ChooseOption.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) DialogueManager.choose(player, p.entityId(), p.index());
        }));
        r.playToServer(Trade.TYPE, Trade.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (p.buying()) ShopManager.buy(player, p.entityId(), p.index(), p.quantity());
            else ShopManager.sell(player, p.entityId(), p.index(), p.quantity() < 0);
            // refresh the screen with new balance / prices
            if (player.level().getEntity(p.entityId()) instanceof NpcEntity npc) npc.def().shop().ifPresent(id -> ShopManager.open(player, npc, id));
        }));
    }

    public static void openDialogue(ServerPlayer player, NpcEntity npc, Component text, List<Component> options) {
        Component title = npc.def() == null ? Component.empty() : npc.def().title();
        PacketDistributor.sendToPlayer(player, new OpenDialogue(npc.getId(), npc.getDisplayName(), title, text, options));
    }

    public static void closeDialogue(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new CloseDialogue(false));
    }

    public static void openShop(ServerPlayer player, NpcEntity npc, Component name, List<ShopManager.Offer> sells, List<ShopManager.Offer> buys) {
        PacketDistributor.sendToPlayer(player, new OpenShop(npc.getId(), name,
                sells.stream().map(o -> new Offer(o.stack(), o.price())).toList(),
                buys.stream().map(o -> new Offer(o.stack(), o.price())).toList(),
                WalletApi.balance(player)));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DialogueManager.forget(event.getEntity().getUUID());
        ShopManager.forget(event.getEntity().getUUID());
        Pickpocket.forget(event.getEntity().getUUID());
    }
}
