package com.untamedrealms.core.client;

import com.untamedrealms.core.UntamedCore;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The pack's video settings, applied once per {@link #VERSION} on the first title screen. Afterwards the
 * player's own choices are left alone; bumping VERSION re-applies them (e.g. after a pack-wide retune).
 * <p>
 * Vanilla renders the near terrain and Distant Horizons everything past it, so the vanilla distance only
 * needs to match what the server sends (view-distance=10 in server.properties); more costs frames and
 * shows nothing extra.
 */
public final class GraphicsDefaults {
    private static final int VERSION = 1;
    private static final int RENDER_DISTANCE = 10;
    private static final int SIMULATION_DISTANCE = 8;
    private static boolean done;

    private GraphicsDefaults() {}

    public static void onFirstMenu() {
        if (done) return;
        done = true;
        Path record = FMLPaths.CONFIGDIR.get().resolve("untamedrealms/graphics-applied.txt");
        try {
            if (Files.exists(record) && Files.readString(record).trim().equals(String.valueOf(VERSION))) return;
            Minecraft mc = Minecraft.getInstance();
            mc.options.renderDistance().set(RENDER_DISTANCE);
            mc.options.simulationDistance().set(SIMULATION_DISTANCE);
            mc.options.save();
            Files.createDirectories(record.getParent());
            Files.writeString(record, String.valueOf(VERSION));
            UntamedCore.LOGGER.info("Applied Untamed Realms video defaults (render distance {})", RENDER_DISTANCE);
        } catch (Exception e) {
            UntamedCore.LOGGER.warn("Could not apply the pack's video defaults", e);
        }
    }
}
