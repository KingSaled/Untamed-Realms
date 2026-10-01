package com.untamedrealms.skills.network;

import com.untamedrealms.skills.UntamedSkills;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class SkillsPayloads {
    private SkillsPayloads() {}

    /** Server -> client: full skill data for the local player. */
    public record Sync(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(UntamedSkills.id("sync"));
        public static final StreamCodec<ByteBuf, Sync> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(Sync::new, Sync::data);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Drop(int skill, float xp) {
        public static final StreamCodec<ByteBuf, Drop> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Drop::skill,
                ByteBufCodecs.FLOAT, Drop::xp,
                Drop::new);
    }

    /** Server -> client: batched XP gains for the RuneScape-style XP drop display. */
    public record XpDrops(List<Drop> drops) implements CustomPacketPayload {
        public static final Type<XpDrops> TYPE = new Type<>(UntamedSkills.id("xp_drops"));
        public static final StreamCodec<ByteBuf, XpDrops> STREAM_CODEC =
                Drop.STREAM_CODEC.apply(ByteBufCodecs.list(64)).map(XpDrops::new, XpDrops::drops);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: unlock a perk. */
    public record UnlockPerk(ResourceLocation perk) implements CustomPacketPayload {
        public static final Type<UnlockPerk> TYPE = new Type<>(UntamedSkills.id("unlock_perk"));
        public static final StreamCodec<ByteBuf, UnlockPerk> STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(UnlockPerk::new, UnlockPerk::perk);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: spend an attribute point (0 health, 1 magicka, 2 stamina). */
    public record SpendAttribute(int which) implements CustomPacketPayload {
        public static final Type<SpendAttribute> TYPE = new Type<>(UntamedSkills.id("spend_attribute"));
        public static final StreamCodec<ByteBuf, SpendAttribute> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(SpendAttribute::new, SpendAttribute::which);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
