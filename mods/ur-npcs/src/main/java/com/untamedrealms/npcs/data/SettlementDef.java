package com.untamedrealms.npcs.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Who lives in which structures, from {@code data/<ns>/urnpcs/settlements/<id>.json}. The first time
 * a player walks into a matching structure it is populated: every {@code always} NPC, a random pick
 * of {@code random_count} from {@code random}, and optionally a notice board near the centre.
 */
public record SettlementDef(String structures, List<ResourceLocation> always, List<ResourceLocation> random,
                            int randomCount, boolean noticeBoard, int radius) {
    public static final Codec<SettlementDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("structures").forGetter(SettlementDef::structures),
            ResourceLocation.CODEC.listOf().optionalFieldOf("always", List.of()).forGetter(SettlementDef::always),
            ResourceLocation.CODEC.listOf().optionalFieldOf("random", List.of()).forGetter(SettlementDef::random),
            Codec.INT.optionalFieldOf("random_count", 0).forGetter(SettlementDef::randomCount),
            Codec.BOOL.optionalFieldOf("notice_board", true).forGetter(SettlementDef::noticeBoard),
            Codec.intRange(2, 64).optionalFieldOf("radius", 14).forGetter(SettlementDef::radius)
    ).apply(inst, SettlementDef::new));
}
