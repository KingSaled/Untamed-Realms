package com.untamedrealms.arsenal;

import com.untamedrealms.skills.effect.SkillEffect;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ArsenalComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, UntamedArsenal.MODID);

    /** Bonuses a worn ring or amulet grants. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<SkillEffect>>> JEWEL_EFFECTS =
            COMPONENTS.registerComponentType("jewel_effects", b -> b.persistent(SkillEffect.CODEC.listOf())
                    .networkSynchronized(ByteBufCodecs.fromCodec(SkillEffect.CODEC.listOf())));

    private ArsenalComponents() {}
}
