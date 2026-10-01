package com.untamedrealms.npcs.dialogue;

import com.untamedrealms.core.api.UR;
import com.untamedrealms.core.economy.WalletApi;
import com.untamedrealms.npcs.entity.NpcEntity;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.effect.EffectTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sneak + use on an NPC to lift their purse. Success chance scales with Pickpocket level and perks,
 * and is much better from behind. Getting caught angers the NPC and costs a fine.
 */
public final class Pickpocket {
    private static final long COOLDOWN_TICKS = 6000; // 5 minutes per NPC per player
    private static final Map<String, Long> LAST_ATTEMPT = new ConcurrentHashMap<>();

    private Pickpocket() {}

    public static void attempt(ServerPlayer player, NpcEntity npc) {
        String key = player.getUUID() + ":" + npc.getUUID();
        long now = player.serverLevel().getGameTime();
        Long last = LAST_ATTEMPT.get(key);
        if (last != null && now - last < COOLDOWN_TICKS) {
            player.displayClientMessage(Component.translatable("message.urnpcs.pickpocket_wary", npc.getDisplayName()), true);
            return;
        }
        LAST_ATTEMPT.put(key, now);

        int level = SkillsApi.level(player, Skill.PICKPOCKET);
        double chance = 0.2 + level * 0.006 + SkillsApi.effect(player, EffectTypes.PICKPOCKET, "");
        Vec3 facing = npc.getViewVector(1f);
        Vec3 toPlayer = player.position().subtract(npc.position()).normalize();
        if (facing.dot(toPlayer) < -0.3) chance += 0.25; // behind them
        chance = Math.min(0.9, chance);

        if (player.getRandom().nextDouble() < chance) {
            int coins = 3 + player.getRandom().nextInt(8 + level / 3);
            WalletApi.deposit(player, coins);
            SkillsApi.addXp(player, Skill.PICKPOCKET, 25 + coins * 2);
            player.displayClientMessage(Component.translatable("message.urnpcs.pickpocket_success", coins).withStyle(ChatFormatting.GOLD), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.4f, 1.6f);
        } else {
            long fine = Math.min(WalletApi.balance(player), 20);
            WalletApi.tryWithdraw(player, fine);
            npc.becomeAngry(2400);
            npc.getLookControl().setLookAt(player);
            SkillsApi.addXp(player, Skill.PICKPOCKET, 5);
            UR.warn(player, Component.translatable("message.urnpcs.pickpocket_caught", npc.getDisplayName(), fine));
            player.level().playSound(null, npc.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1f, 0.8f);
        }
    }

    public static void forget(UUID player) {
        LAST_ATTEMPT.keySet().removeIf(k -> k.startsWith(player.toString()));
    }
}
