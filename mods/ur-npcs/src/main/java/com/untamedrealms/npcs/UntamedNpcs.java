package com.untamedrealms.npcs;

import com.mojang.logging.LogUtils;
import com.untamedrealms.npcs.data.NpcsData;
import com.untamedrealms.npcs.entity.NpcEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * Untamed Realms: NPCs. Townsfolk with names, branching dialogue (quests, Speech checks), merchants,
 * trainers and pickpocketing, plus automatic population of villages and towns.
 */
@Mod(UntamedNpcs.MODID)
public final class UntamedNpcs {
    public static final String MODID = "urnpcs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<NpcEntity>> NPC = ENTITIES.register("npc",
            () -> EntityType.Builder.<NpcEntity>of(NpcEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .build("npc"));

    public UntamedNpcs(IEventBus modBus, ModContainer container) {
        ENTITIES.register(modBus);
        NpcsData.init();
        com.untamedrealms.npcs.settlement.NpcLocator.register();
        modBus.addListener(EntityAttributeCreationEvent.class, event -> event.put(NPC.get(), NpcEntity.createAttributes().build()));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
