package com.untamedrealms.core.client;

import com.untamedrealms.core.network.CorePayloads;

/** Client-side mirror of the server-authoritative core state for the local player. */
public final class ClientCoreState {
    private static float magicka = -1;
    private static float stamina = -1;
    private static boolean exhausted;
    private static long crowns;

    private ClientCoreState() {}

    public static void onVitals(CorePayloads.VitalsSync payload) {
        magicka = payload.magicka();
        stamina = payload.stamina();
        exhausted = payload.exhausted();
    }

    public static void onWallet(CorePayloads.WalletSync payload) {
        crowns = payload.crowns();
    }

    public static float magicka() { return magicka; }
    public static float stamina() { return stamina; }
    public static boolean exhausted() { return exhausted; }
    public static long crowns() { return crowns; }

    public static void reset() {
        magicka = -1;
        stamina = -1;
        exhausted = false;
        crowns = 0;
    }
}
