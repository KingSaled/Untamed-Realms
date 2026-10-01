package com.untamedrealms.core.world;

import com.untamedrealms.core.UntamedCore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Keeps the player who last hurt a creature as its killer while it burns, bleeds or falls to death
 * (vanilla forgets after five seconds). Without this a mob set on fire by a Firebolt that dies from
 * the flames gives no quest progress, loot or experience.
 */
@EventBusSubscriber(modid = UntamedCore.MODID)
public final class KillCredit {
    private static final String KEY = "urcore_credit";
    /** How long (ticks) a player keeps the credit after their last hit. */
    private static final long WINDOW = 20 * 30;

    private KillCredit() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || target instanceof Player) return;
        if (event.getSource().getEntity() instanceof Player player) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("player", player.getUUID());
            tag.putLong("time", target.level().getGameTime());
            target.getPersistentData().put(KEY, tag);
            return;
        }
        Player player = creditedPlayer(target);
        if (player != null) target.setLastHurtByPlayer(player);
    }

    /** The player who hit {@code target} within the last 30 seconds, if they are still around. */
    public static @Nullable Player creditedPlayer(LivingEntity target) {
        CompoundTag tag = target.getPersistentData().getCompound(KEY);
        if (!tag.hasUUID("player") || target.level().getGameTime() - tag.getLong("time") > WINDOW) return null;
        UUID id = tag.getUUID("player");
        return target.level().getPlayerByUUID(id);
    }
}
