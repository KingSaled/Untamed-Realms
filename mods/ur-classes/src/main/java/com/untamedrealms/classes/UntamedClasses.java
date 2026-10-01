package com.untamedrealms.classes;

import com.mojang.logging.LogUtils;
import com.untamedrealms.classes.data.Birthsign;
import com.untamedrealms.classes.data.ClassData;
import com.untamedrealms.classes.data.ClassDef;
import com.untamedrealms.classes.data.ClassesData;
import com.untamedrealms.skills.api.SkillsApi;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Untamed Realms: Classes. On first join players pick a class (starting skills, gear, coin and
 * lifelong effects) and a birthsign (a permanent blessing). Both feed the skills module's effect
 * system through an {@link com.untamedrealms.skills.effect.EffectProvider}.
 */
@Mod(UntamedClasses.MODID)
public final class UntamedClasses {
    public static final String MODID = "urclasses";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);
    public static final Supplier<AttachmentType<ClassData>> CLASS_DATA = ATTACHMENTS.register("class",
            () -> AttachmentType.serializable(ClassData::new).copyOnDeath().build());

    public UntamedClasses(IEventBus modBus, ModContainer container) {
        ATTACHMENTS.register(modBus);
        ClassesData.init();
        SkillsApi.registerEffectProvider(id("class_and_birthsign"), (player, out) -> {
            ClassData data = player.getData(CLASS_DATA);
            ClassDef def = ClassesData.CLASSES.getOrNull(data.classId());
            if (def != null) def.effects().forEach(out);
            Birthsign sign = ClassesData.BIRTHSIGNS.getOrNull(data.birthsign());
            if (sign != null) sign.effects().forEach(out);
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
