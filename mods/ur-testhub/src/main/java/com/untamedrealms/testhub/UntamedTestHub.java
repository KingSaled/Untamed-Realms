package com.untamedrealms.testhub;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Untamed Realms: Test Hub. Inert in a normal world. When the server runs with
 * {@code -Duntamed.testworld=true} (Start-Test-World.bat), it builds a floating hub above the
 * starting village with every item, station, NPC, spell and quest laid out, ops everyone who joins,
 * hands out a control book and adds the {@code /ur test} commands.
 */
@Mod(UntamedTestHub.MODID)
public final class UntamedTestHub {
    public static final String MODID = "urtesthub";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final boolean ACTIVE = Boolean.getBoolean("untamed.testworld");

    public UntamedTestHub(IEventBus modBus) {
        if (ACTIVE) {
            LOGGER.info("Test world mode: the test hub will be built and /ur test commands are enabled");
            NeoForge.EVENT_BUS.register(TestHubEvents.class);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
