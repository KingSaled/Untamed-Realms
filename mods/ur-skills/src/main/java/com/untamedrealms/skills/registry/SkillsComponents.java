package com.untamedrealms.skills.registry;

import com.mojang.serialization.Codec;
import com.untamedrealms.skills.UntamedSkills;
import com.untamedrealms.skills.api.Skill;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SkillsComponents {
    public static final DeferredRegister.DataComponents REGISTER =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, UntamedSkills.MODID);

    /** The skill a skill book teaches. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Skill>> SKILL = REGISTER.registerComponentType("skill",
            builder -> builder.persistent(Skill.CODEC).networkSynchronized(ByteBufCodecs.idMapper(i -> Skill.VALUES.get(i), Skill::ordinal)));

    /** Smithing quality of crafted gear: 1 Fine, 2 Superior, 3 Exquisite, 4 Flawless. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> QUALITY = REGISTER.registerComponentType("quality",
            builder -> builder.persistent(Codec.intRange(0, 4)).networkSynchronized(ByteBufCodecs.VAR_INT));

    private SkillsComponents() {}
}
