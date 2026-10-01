package com.untamedrealms.core.registry;

import com.untamedrealms.core.UntamedCore;
import com.untamedrealms.core.economy.Wallet;
import com.untamedrealms.core.vitals.Vitals;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class CoreAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, UntamedCore.MODID);

    public static final Supplier<AttachmentType<Vitals>> VITALS = REGISTER.register("vitals",
            () -> AttachmentType.serializable(Vitals::new).build());

    public static final Supplier<AttachmentType<Wallet>> WALLET = REGISTER.register("wallet",
            () -> AttachmentType.serializable(Wallet::new).copyOnDeath().build());

    private CoreAttachments() {}
}
