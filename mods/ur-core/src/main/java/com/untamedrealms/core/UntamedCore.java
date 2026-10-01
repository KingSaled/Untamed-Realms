package com.untamedrealms.core;

import com.untamedrealms.core.registry.CoreAttachments;
import com.untamedrealms.core.registry.CoreAttributes;
import com.untamedrealms.core.registry.CoreItems;
import com.untamedrealms.core.registry.CoreLootModifiers;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Untamed Realms: Core.
 * <p>
 * Every other Untamed Realms module depends on this mod. It owns the systems that more than one
 * module needs: the Magicka/Stamina vitals, the Crown wallet, data-driven content syncing,
 * on-screen notifications, chest-loot injection and the shared UI kit.
 */
@Mod(UntamedCore.MODID)
public final class UntamedCore {
    public static final String MODID = "urcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UntamedCore(IEventBus modBus, ModContainer container) {
        CoreAttributes.REGISTER.register(modBus);
        CoreAttachments.REGISTER.register(modBus);
        CoreItems.REGISTER.register(modBus);
        CoreItems.TABS.register(modBus);
        CoreLootModifiers.REGISTER.register(modBus);

        container.registerConfig(ModConfig.Type.SERVER, CoreConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, CoreClientConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
