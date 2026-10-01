package com.untamedrealms.magic.network;

import com.untamedrealms.magic.UntamedMagic;
import com.untamedrealms.magic.client.ClientMagic;
import com.untamedrealms.magic.data.SpellBook;
import com.untamedrealms.magic.spell.MagicApi;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
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

@EventBusSubscriber(modid = UntamedMagic.MODID)
public final class MagicNetwork {
    private MagicNetwork() {}

    /** Server -> client: spellbook plus remaining cooldowns (ticks) for the HUD. */
    public record Sync(CompoundTag book, CompoundTag cooldowns) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(UntamedMagic.id("sync"));
        public static final StreamCodec<ByteBuf, Sync> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Sync::book, ByteBufCodecs.COMPOUND_TAG, Sync::cooldowns, Sync::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: cast quick slot (-1 = selected). */
    public record Cast(int slot) implements CustomPacketPayload {
        public static final Type<Cast> TYPE = new Type<>(UntamedMagic.id("cast"));
        public static final StreamCodec<ByteBuf, Cast> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Cast::new, Cast::slot);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: select a slot (absolute) or cycle (relative). */
    public record Select(int value, boolean relative) implements CustomPacketPayload {
        public static final Type<Select> TYPE = new Type<>(UntamedMagic.id("select"));
        public static final StreamCodec<ByteBuf, Select> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Select::value, ByteBufCodecs.BOOL, Select::relative, Select::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: put a known spell into a quick slot (empty = clear). */
    public record SetSlot(int slot, Optional<ResourceLocation> spell) implements CustomPacketPayload {
        public static final Type<SetSlot> TYPE = new Type<>(UntamedMagic.id("set_slot"));
        public static final StreamCodec<ByteBuf, SetSlot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SetSlot::slot, ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), SetSlot::spell, SetSlot::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(Sync.TYPE, Sync.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientMagic.onSync(p.book(), p.cooldowns(), ctx.player().registryAccess())));
        r.playToServer(Cast.TYPE, Cast.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) MagicApi.castSlot(player, p.slot());
        }));
        r.playToServer(Select.TYPE, Select.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            SpellBook book = MagicApi.book(player);
            if (p.relative()) book.cycle(p.value());
            else book.select(p.value());
            sync(player);
        }));
        r.playToServer(SetSlot.TYPE, SetSlot.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            SpellBook book = MagicApi.book(player);
            ResourceLocation spell = p.spell().orElse(null);
            if (spell == null || book.knows(spell)) {
                book.setSlot(p.slot(), spell);
                sync(player);
            }
        }));
    }

    public static void sync(ServerPlayer player) {
        SpellBook book = MagicApi.book(player);
        CompoundTag cooldowns = new CompoundTag();
        long now = player.serverLevel().getGameTime();
        for (ResourceLocation spell : book.known()) {
            long left = book.readyAt(spell) - now;
            if (left > 0) cooldowns.putInt(spell.toString(), (int) left);
        }
        PacketDistributor.sendToPlayer(player, new Sync(book.serializeNBT(player.registryAccess()), cooldowns));
    }
}
