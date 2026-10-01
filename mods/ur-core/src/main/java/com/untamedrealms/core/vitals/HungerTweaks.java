package com.untamedrealms.core.vitals;

import com.untamedrealms.core.CoreConfig;
import com.untamedrealms.core.UntamedCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slower hunger. Food exhaustion added since the last tick is scaled by {@code hunger.rate}, and the
 * vanilla cost of sprinting (0.1 per metre) and jumping (0.05, 0.2 sprint-jumping) is refunded, since
 * stamina already pays for those.
 */
@EventBusSubscriber(modid = UntamedCore.MODID)
public final class HungerTweaks {
    private static final class Track {
        float exhaustion;
        Vec3 pos;
        float refund;
    }

    private static final Map<UUID, Track> TRACKS = new ConcurrentHashMap<>();

    private HungerTweaks() {}

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !CoreConfig.SPRINT_COSTS_HUNGER.get()) {
            Track t = TRACKS.get(player.getUUID());
            if (t != null) t.refund += player.isSprinting() ? 0.2f : 0.05f;
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) return;
        FoodData food = player.getFoodData();
        Track t = TRACKS.computeIfAbsent(player.getUUID(), u -> {
            Track n = new Track();
            n.exhaustion = food.getExhaustionLevel();
            n.pos = player.position();
            return n;
        });
        float now = food.getExhaustionLevel();
        float last = t.exhaustion;
        // Each time exhaustion passes 4 vanilla takes 4 away and removes a point of saturation or food.
        if (now < last) last -= 4f;
        float added = now - last;

        if (!CoreConfig.SPRINT_COSTS_HUNGER.get() && player.isSprinting() && player.getVehicle() == null) {
            double dx = player.getX() - t.pos.x, dz = player.getZ() - t.pos.z;
            t.refund += 0.1f * (float) Math.sqrt(dx * dx + dz * dz);
        }
        if (added > 0) {
            float kept = Math.max(0f, added - t.refund) * CoreConfig.HUNGER_RATE.get().floatValue();
            float target = Math.max(0f, last + kept);
            if (target != now) food.setExhaustion(target);
            now = target;
        }
        t.refund = 0f;
        t.exhaustion = now;
        t.pos = player.position();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TRACKS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        TRACKS.remove(event.getEntity().getUUID());
    }
}
