package com.untamedrealms.core.economy;

import com.untamedrealms.core.network.CorePayloads;
import com.untamedrealms.core.registry.CoreAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/** Public API for the Crown economy. All mutation is server-side and auto-synced. */
public final class WalletApi {
    private WalletApi() {}

    public static long balance(Player player) {
        return player.getData(CoreAttachments.WALLET).crowns();
    }

    public static void set(Player player, long crowns) {
        player.getData(CoreAttachments.WALLET).set(crowns);
        sync(player);
    }

    public static void deposit(Player player, long crowns) {
        if (crowns <= 0) return;
        set(player, saturatingAdd(balance(player), crowns));
    }

    /** Removes coins if the player can afford it. Returns whether the payment happened. */
    public static boolean tryWithdraw(Player player, long crowns) {
        if (crowns < 0) return false;
        long balance = balance(player);
        if (balance < crowns) return false;
        set(player, balance - crowns);
        return true;
    }

    public static boolean canAfford(Player player, long crowns) {
        return balance(player) >= crowns;
    }

    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new CorePayloads.WalletSync(balance(player)));
        }
    }

    private static long saturatingAdd(long a, long b) {
        long r = a + b;
        return ((a ^ r) & (b ^ r)) < 0 ? Long.MAX_VALUE : r;
    }
}
