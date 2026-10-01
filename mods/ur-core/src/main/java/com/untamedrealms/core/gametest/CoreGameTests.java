package com.untamedrealms.core.gametest;

import com.untamedrealms.core.UntamedCore;
import com.untamedrealms.core.economy.WalletApi;
import com.untamedrealms.core.registry.CoreAttributes;
import com.untamedrealms.core.vitals.VitalsApi;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Headless server tests run in CI (./gradlew :devenv:runGameTestServer). */
@GameTestHolder(UntamedCore.MODID)
@PrefixGameTestTemplate(false)
public class CoreGameTests {
    @GameTest(template = "arena")
    public static void walletDepositAndWithdraw(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        WalletApi.deposit(player, 150);
        helper.assertTrue(WalletApi.balance(player) == 150, "deposit should add to the balance");
        helper.assertTrue(!WalletApi.tryWithdraw(player, 151), "cannot overspend");
        helper.assertTrue(WalletApi.tryWithdraw(player, 50), "withdraw within balance");
        helper.assertTrue(WalletApi.balance(player) == 100, "balance after withdraw");
        WalletApi.deposit(player, Long.MAX_VALUE);
        helper.assertTrue(WalletApi.balance(player) == Long.MAX_VALUE, "deposit saturates instead of overflowing");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void playerHasVitalAttributes(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(player.getAttribute(CoreAttributes.MAX_MAGICKA) != null, "player has max magicka");
        helper.assertTrue(player.getAttribute(CoreAttributes.MAX_STAMINA) != null, "player has max stamina");
        helper.assertTrue(VitalsApi.maxMagicka(player) == 100f, "default magicka is 100");
        helper.assertTrue(VitalsApi.tryConsumeMagicka(player, 40), "can spend magicka");
        helper.assertTrue(Math.abs(VitalsApi.magicka(player) - 60f) < 0.01f, "magicka after spending");
        helper.assertTrue(!VitalsApi.tryConsumeMagicka(player, 80), "cannot overspend magicka");
        helper.succeed();
    }
}
