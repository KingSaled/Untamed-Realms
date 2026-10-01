package com.untamedrealms.npcs.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.untamedrealms.skills.api.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An NPC archetype, from {@code data/<ns>/urnpcs/npcs/<id>.json}. Every spawned NPC references one;
 * a random name from {@code names} is picked when it spawns.
 */
public record NpcDef(Component title, List<String> names, ResourceLocation skin, Optional<ResourceLocation> dialogue,
                     Optional<ResourceLocation> shop, Optional<Trainer> trainer, boolean essential, int wander,
                     Map<String, Dynamic<?>> equipment, Optional<Component> greeting) {
    public static final Codec<NpcDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ComponentSerialization.CODEC.fieldOf("title").forGetter(NpcDef::title),
            Codec.STRING.listOf().optionalFieldOf("names", List.of("Stranger")).forGetter(NpcDef::names),
            ResourceLocation.CODEC.fieldOf("skin").forGetter(NpcDef::skin),
            ResourceLocation.CODEC.optionalFieldOf("dialogue").forGetter(NpcDef::dialogue),
            ResourceLocation.CODEC.optionalFieldOf("shop").forGetter(NpcDef::shop),
            Trainer.CODEC.optionalFieldOf("trainer").forGetter(NpcDef::trainer),
            Codec.BOOL.optionalFieldOf("essential", true).forGetter(NpcDef::essential),
            Codec.intRange(0, 64).optionalFieldOf("wander", 8).forGetter(NpcDef::wander),
            Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH).optionalFieldOf("equipment", Map.of()).forGetter(NpcDef::equipment),
            ComponentSerialization.CODEC.optionalFieldOf("greeting").forGetter(NpcDef::greeting)
    ).apply(inst, NpcDef::new));

    /** Skyrim-style trainer: pay to raise a skill, up to {@code max_level}, five times per character level. */
    public record Trainer(Skill skill, int maxLevel) {
        public static final Codec<Trainer> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Skill.CODEC.fieldOf("skill").forGetter(Trainer::skill),
                Codec.intRange(1, 99).optionalFieldOf("max_level", 50).forGetter(Trainer::maxLevel)
        ).apply(inst, Trainer::new));
    }
}
