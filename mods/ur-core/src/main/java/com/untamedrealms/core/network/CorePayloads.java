package com.untamedrealms.core.network;

import com.untamedrealms.core.UntamedCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** All payloads owned by the core module. */
public final class CorePayloads {
    private CorePayloads() {}

    /** Server -> client: current magicka / stamina pools. */
    public record VitalsSync(float magicka, float stamina, boolean exhausted) implements CustomPacketPayload {
        public static final Type<VitalsSync> TYPE = new Type<>(UntamedCore.id("vitals_sync"));
        public static final StreamCodec<ByteBuf, VitalsSync> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, VitalsSync::magicka,
                ByteBufCodecs.FLOAT, VitalsSync::stamina,
                ByteBufCodecs.BOOL, VitalsSync::exhausted,
                VitalsSync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> client: wallet balance. */
    public record WalletSync(long crowns) implements CustomPacketPayload {
        public static final Type<WalletSync> TYPE = new Type<>(UntamedCore.id("wallet_sync"));
        public static final StreamCodec<ByteBuf, WalletSync> STREAM_CODEC =
                ByteBufCodecs.VAR_LONG.map(WalletSync::new, WalletSync::crowns);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> client: show a banner (level up, quest started, ...). */
    public record Notify(int style, Component title, Component subtitle) implements CustomPacketPayload {
        public static final Type<Notify> TYPE = new Type<>(UntamedCore.id("notify"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Notify> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Notify::style,
                ComponentSerialization.STREAM_CODEC, Notify::title,
                ComponentSerialization.STREAM_CODEC, Notify::subtitle,
                Notify::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> client: the full contents of one data-driven registry (classes, quests, perks, ...). */
    public record DataSync(ResourceLocation registry, CompoundTag data) implements CustomPacketPayload {
        public static final Type<DataSync> TYPE = new Type<>(UntamedCore.id("data_sync"));
        public static final StreamCodec<ByteBuf, DataSync> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, DataSync::registry,
                ByteBufCodecs.TRUSTED_COMPOUND_TAG, DataSync::data,
                DataSync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
