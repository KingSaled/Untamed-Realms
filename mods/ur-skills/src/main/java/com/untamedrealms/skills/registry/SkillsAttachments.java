package com.untamedrealms.skills.registry;

import com.untamedrealms.skills.UntamedSkills;
import com.untamedrealms.skills.data.PlacedBlocks;
import com.untamedrealms.skills.data.SkillData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class SkillsAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, UntamedSkills.MODID);

    public static final Supplier<AttachmentType<SkillData>> SKILLS = REGISTER.register("skills",
            () -> AttachmentType.serializable(SkillData::new).copyOnDeath().build());

    /** Per-chunk set of player-placed blocks (anti place-and-break XP farming). */
    public static final Supplier<AttachmentType<PlacedBlocks>> PLACED_BLOCKS = REGISTER.register("placed_blocks",
            () -> AttachmentType.builder(PlacedBlocks::new).serialize(PlacedBlocks.CODEC, PlacedBlocks::shouldSave).build());

    private SkillsAttachments() {}
}
