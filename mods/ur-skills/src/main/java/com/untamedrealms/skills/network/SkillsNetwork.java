package com.untamedrealms.skills.network;

import com.untamedrealms.core.api.UR;
import com.untamedrealms.skills.UntamedSkills;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.client.ClientSkills;
import com.untamedrealms.skills.client.XpDropHud;
import com.untamedrealms.skills.registry.SkillsAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import com.untamedrealms.skills.effect.EffectManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillsNetwork {
    private static final Set<UUID> DIRTY = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, float[]> PENDING_DROPS = new ConcurrentHashMap<>();
    private static final int FLUSH_INTERVAL = 5;

    private SkillsNetwork() {}

    public static void markDirty(ServerPlayer player) {
        DIRTY.add(player.getUUID());
    }

    public static void queueXpDrop(ServerPlayer player, Skill skill, float xp) {
        PENDING_DROPS.computeIfAbsent(player.getUUID(), id -> new float[Skill.VALUES.size()])[skill.ordinal()] += xp;
    }

    public static void syncNow(ServerPlayer player) {
        DIRTY.remove(player.getUUID());
        CompoundTag tag = player.getData(SkillsAttachments.SKILLS).serializeNBT(player.registryAccess());
        tag.put("effects", EffectManager.get(player).toTag());
        PacketDistributor.sendToPlayer(player, new SkillsPayloads.Sync(tag));
    }

    @EventBusSubscriber(modid = UntamedSkills.MODID)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.playToClient(SkillsPayloads.Sync.TYPE, SkillsPayloads.Sync.STREAM_CODEC,
                    (payload, ctx) -> ctx.enqueueWork(() -> ClientSkills.onSync(payload.data(), ctx.player().registryAccess())));
            registrar.playToClient(SkillsPayloads.XpDrops.TYPE, SkillsPayloads.XpDrops.STREAM_CODEC,
                    (payload, ctx) -> ctx.enqueueWork(() -> XpDropHud.accept(payload)));
            registrar.playToServer(SkillsPayloads.UnlockPerk.TYPE, SkillsPayloads.UnlockPerk.STREAM_CODEC,
                    (payload, ctx) -> ctx.enqueueWork(() -> {
                        if (ctx.player() instanceof ServerPlayer player) {
                            Component error = SkillsApi.tryUnlockPerk(player, payload.perk());
                            if (error != null) UR.warn(player, error);
                        }
                    }));
            registrar.playToServer(SkillsPayloads.SpendAttribute.TYPE, SkillsPayloads.SpendAttribute.STREAM_CODEC,
                    (payload, ctx) -> ctx.enqueueWork(() -> {
                        if (ctx.player() instanceof ServerPlayer player) SkillsApi.spendAttributePoint(player, payload.which());
                    }));
        }
    }

    @EventBusSubscriber(modid = UntamedSkills.MODID)
    public static final class Flusher {
        @SubscribeEvent
        public static void onTick(PlayerTickEvent.Post event) {
            if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % FLUSH_INTERVAL != 0) return;
            float[] drops = PENDING_DROPS.remove(player.getUUID());
            if (drops != null) {
                List<SkillsPayloads.Drop> list = new ArrayList<>();
                for (int i = 0; i < drops.length; i++) {
                    if (drops[i] > 0) list.add(new SkillsPayloads.Drop(i, drops[i]));
                }
                if (!list.isEmpty()) PacketDistributor.sendToPlayer(player, new SkillsPayloads.XpDrops(list));
            }
            if (DIRTY.contains(player.getUUID())) syncNow(player);
        }

        @SubscribeEvent
        public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            DIRTY.remove(event.getEntity().getUUID());
            PENDING_DROPS.remove(event.getEntity().getUUID());
        }
    }
}
