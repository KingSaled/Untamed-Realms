package com.untamedrealms.skills.client;

import com.untamedrealms.skills.data.SkillData;
import com.untamedrealms.skills.effect.ClientEffectStore;
import com.untamedrealms.skills.registry.SkillsAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * Client mirror of the local player's skill data. The data is also written into the local player's
 * attachment so shared code ({@code SkillsApi.level}) gives the same answers on both sides - this
 * matters for client-predicted mechanics like block-breaking speed.
 */
public final class ClientSkills {
    private static final SkillData DATA = new SkillData();
    private static CompoundTag lastTag;
    private static LocalPlayer appliedTo;

    private ClientSkills() {}

    public static SkillData data() {
        return DATA;
    }

    public static void onSync(CompoundTag tag, HolderLookup.Provider registries) {
        lastTag = tag;
        DATA.deserializeNBT(registries, tag);
        ClientEffectStore.load(tag.getCompound("effects"));
        appliedTo = null;
        applyToPlayer();
    }

    /** Re-applies to a freshly created LocalPlayer (respawn / dimension change). Called every client tick. */
    public static void applyToPlayer() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || lastTag == null || player == appliedTo) return;
        player.getData(SkillsAttachments.SKILLS).deserializeNBT(player.registryAccess(), lastTag);
        appliedTo = player;
    }

    public static void reset() {
        lastTag = null;
        appliedTo = null;
        DATA.reset();
        ClientEffectStore.load(new CompoundTag());
    }
}
