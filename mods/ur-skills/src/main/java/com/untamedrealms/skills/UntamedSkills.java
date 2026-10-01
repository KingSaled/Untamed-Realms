package com.untamedrealms.skills;

import com.mojang.logging.LogUtils;
import com.untamedrealms.skills.data.SkillsData;
import com.untamedrealms.skills.registry.SkillsAttachments;
import com.untamedrealms.skills.registry.SkillsComponents;
import com.untamedrealms.skills.registry.SkillsItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Untamed Realms: Skills. Learn-by-doing skills with a RuneScape XP curve, Skyrim character levels,
 * data-driven perk trees, skill requirements and skill books. Exposes {@link com.untamedrealms.skills.api.SkillsApi}
 * to the rest of the suite.
 */
@Mod(UntamedSkills.MODID)
public final class UntamedSkills {
    public static final String MODID = "urskills";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UntamedSkills(IEventBus modBus, ModContainer container) {
        SkillsAttachments.REGISTER.register(modBus);
        SkillsComponents.REGISTER.register(modBus);
        SkillsItems.REGISTER.register(modBus);
        SkillsData.init();
        container.registerConfig(ModConfig.Type.SERVER, SkillsConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, SkillsClientConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
