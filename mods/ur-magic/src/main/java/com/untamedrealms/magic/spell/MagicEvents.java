package com.untamedrealms.magic.spell;

import com.untamedrealms.magic.UntamedMagic;
import com.untamedrealms.magic.network.MagicNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = UntamedMagic.MODID)
public final class MagicEvents {
    private MagicEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) MagicNetwork.sync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) MagicNetwork.sync(player);
    }

    /** Conjured creatures unravel when their time is up. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0 || SpellEffects.SUMMONS.isEmpty()) return;
        long now = event.getServer().overworld().getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = SpellEffects.SUMMONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            if (now < e.getValue()) continue;
            for (ServerLevel level : event.getServer().getAllLevels()) {
                Entity entity = level.getEntity(e.getKey());
                if (entity != null) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, entity.getX(), entity.getY() + 0.5, entity.getZ(), 30, 0.3, 0.5, 0.3, 0.2);
                    entity.discard();
                    break;
                }
            }
            it.remove();
        }
    }

    /** Re-track summons after a restart or chunk reload so they still unravel. */
    @SubscribeEvent
    public static void onJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity().getPersistentData().contains(SpellEffects.EXPIRES_TAG)) {
            SpellEffects.SUMMONS.put(event.getEntity().getUUID(), event.getEntity().getPersistentData().getLong(SpellEffects.EXPIRES_TAG));
        }
    }

    /** Muffle: monsters struggle to notice you. */
    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity().hasEffect(UntamedMagic.MUFFLED)) event.modifyVisibility(0.35);
    }
}
