package com.untamedrealms.core.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.untamedrealms.core.UntamedCore;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The pack's curated key layout, applied on the first title screen so every mod in the pack starts
 * conflict-free.
 * <p>
 * The layout is {@code assets/urcore/keybinds.json} (overridable with
 * {@code config/untamedrealms/keybinds.json}): {@code "bindings": {"key.urmagic.cast": "key.keyboard.r",
 * "key.jei.bookmark": "none", "key.foo": "ctrl+key.keyboard.b"}}. What was applied is remembered in
 * {@code config/untamedrealms/keybinds-applied.json}, so a binding the player changed afterwards is
 * never overwritten; a binding is (re)applied only when it is new to the layout or the player still
 * has the value we applied last time.
 * <p>
 * {@code UR_KEY_REPORT=1} writes {@code untamedrealms-keys.txt} (every mapping, its default, and
 * conflicts) to the game folder; {@code UR_EXIT_AFTER_REPORT=1} then closes the game (used by CI).
 */
public final class KeybindProfile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean done;

    private KeybindProfile() {}

    /** Called when the first title / onboarding screen opens. */
    public static void onFirstMenu() {
        if (done) return;
        done = true;
        Minecraft mc = Minecraft.getInstance();
        try {
            apply(mc);
        } catch (Exception e) {
            UntamedCore.LOGGER.error("Could not apply the Untamed Realms key layout", e);
        }
        if ("1".equals(System.getenv("UR_KEY_REPORT"))) {
            try {
                report(mc);
            } catch (Exception e) {
                UntamedCore.LOGGER.error("Could not write the key report", e);
            }
            if ("1".equals(System.getenv("UR_EXIT_AFTER_REPORT"))) mc.stop();
        }
    }

    private static void apply(Minecraft mc) throws IOException {
        Map<String, String> layout = loadLayout();
        if (layout.isEmpty()) return;
        Path recordFile = FMLPaths.CONFIGDIR.get().resolve("untamedrealms/keybinds-applied.json");
        Map<String, String> record = new LinkedHashMap<>();
        if (Files.exists(recordFile)) {
            try (Reader r = Files.newBufferedReader(recordFile, StandardCharsets.UTF_8)) {
                JsonParser.parseReader(r).getAsJsonObject().entrySet().forEach(e -> record.put(e.getKey(), e.getValue().getAsString()));
            } catch (Exception e) {
                UntamedCore.LOGGER.warn("Ignoring unreadable {}", recordFile, e);
            }
        }

        int changed = 0;
        for (KeyMapping mapping : mc.options.keyMappings) {
            String wanted = layout.get(mapping.getName());
            if (wanted == null) continue;
            String applied = record.get(mapping.getName());
            String current = spec(mapping);
            // Never applied before, or the layout changed and the player kept our previous value.
            boolean apply = applied == null || (!applied.equals(wanted) && applied.equals(current));
            if (apply && !wanted.equals(current)) {
                setSpec(mapping, wanted);
                changed++;
            }
            if (apply) record.put(mapping.getName(), wanted);
        }
        if (changed > 0) {
            KeyMapping.resetMapping();
            mc.options.save();
            UntamedCore.LOGGER.info("Applied the Untamed Realms key layout ({} keys changed)", changed);
        }
        Files.createDirectories(recordFile.getParent());
        JsonObject out = new JsonObject();
        record.forEach(out::addProperty);
        Files.writeString(recordFile, GSON.toJson(out), StandardCharsets.UTF_8);
    }

    private static Map<String, String> loadLayout() throws IOException {
        Map<String, String> layout = new LinkedHashMap<>();
        Path override = FMLPaths.CONFIGDIR.get().resolve("untamedrealms/keybinds.json");
        try (InputStream in = Files.exists(override) ? Files.newInputStream(override)
                : KeybindProfile.class.getResourceAsStream("/assets/urcore/keybinds.json")) {
            if (in == null) return layout;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            root.getAsJsonObject("bindings").entrySet().forEach(e -> layout.put(e.getKey(), normalise(e.getValue().getAsString())));
        }
        return layout;
    }

    // ---- "ctrl+key.keyboard.b" <-> mapping ----

    static String spec(KeyMapping mapping) {
        if (mapping.isUnbound()) return "none";
        String prefix = switch (mapping.getKeyModifier()) {
            case CONTROL -> "ctrl+";
            case SHIFT -> "shift+";
            case ALT -> "alt+";
            default -> "";
        };
        return prefix + mapping.getKey().getName();
    }

    private static String normalise(String spec) {
        String s = spec.trim().toLowerCase(java.util.Locale.ROOT);
        return s.isEmpty() || s.equals("key.keyboard.unknown") ? "none" : s;
    }

    private static void setSpec(KeyMapping mapping, String spec) {
        if (spec.equals("none")) {
            mapping.setKeyModifierAndCode(KeyModifier.NONE, InputConstants.UNKNOWN);
            return;
        }
        KeyModifier modifier = KeyModifier.NONE;
        String key = spec;
        int plus = spec.indexOf('+');
        if (plus > 0) {
            modifier = switch (spec.substring(0, plus)) {
                case "ctrl", "control" -> KeyModifier.CONTROL;
                case "shift" -> KeyModifier.SHIFT;
                case "alt" -> KeyModifier.ALT;
                default -> KeyModifier.NONE;
            };
            key = spec.substring(plus + 1);
        }
        mapping.setKeyModifierAndCode(modifier, InputConstants.getKey(key));
    }

    // ---- report ----

    private static void report(Minecraft mc) throws IOException {
        List<KeyMapping> all = new ArrayList<>(List.of(mc.options.keyMappings));
        all.sort(Comparator.comparing(KeyMapping::getCategory).thenComparing(KeyMapping::getName));
        List<String> lines = new ArrayList<>();
        int conflicts = 0, overlaps = 0;
        lines.add("# category | name | label | default | current | conflicts (both active at once) | overlaps (different screens, harmless)");
        for (KeyMapping m : all) {
            List<String> real = new ArrayList<>(), soft = new ArrayList<>();
            if (!m.isUnbound()) {
                for (KeyMapping o : all) {
                    if (o == m || o.isUnbound() || !(m.same(o) || o.hasKeyModifierConflict(m) || m.hasKeyModifierConflict(o))) continue;
                    boolean together = activeTogether(m, o);
                    (together && !intended(m, o) ? real : soft).add(o.getName());
                }
            }
            if (!real.isEmpty()) conflicts++;
            if (!soft.isEmpty()) overlaps++;
            String def = (m.getDefaultKeyModifier() == KeyModifier.NONE ? "" : m.getDefaultKeyModifier().name().toLowerCase(java.util.Locale.ROOT) + "+")
                    + m.getDefaultKey().getName();
            lines.add(String.join(" | ", m.getCategory(), m.getName(), I18n.get(m.getName()), def, spec(m), String.join(",", real), String.join(",", soft)));
        }
        lines.add("# overlapping (harmless): " + overlaps);
        lines.add("# conflicting mappings: " + conflicts);
        Path out = mc.gameDirectory.toPath().resolve("untamedrealms-keys.txt");
        Files.write(out, lines, StandardCharsets.UTF_8);
        lines.forEach(l -> UntamedCore.LOGGER.info("UR-KEYS {}", l));
    }

    /**
     * Whether two mappings on the same key can fire at the same moment: both work in the world, or
     * both work in the same kind of screen. An in-world key and a screen-only key (e.g. JEI's
     * "show recipe" while hovering an item) never fire together.
     */
    private static boolean activeTogether(KeyMapping a, KeyMapping b) {
        boolean worldA = inWorld(a), worldB = inWorld(b);
        if (worldA && worldB) return true;
        if (worldA != worldB) return false;
        return a.getKeyConflictContext().conflicts(b.getKeyConflictContext()) || b.getKeyConflictContext().conflicts(a.getKeyConflictContext());
    }

    private static boolean inWorld(KeyMapping m) {
        return m.getKeyConflictContext() == KeyConflictContext.IN_GAME || m.getKeyConflictContext() == KeyConflictContext.UNIVERSAL;
    }

    /**
     * Sharing that is fine: Jade's "show details" is meant to be held with sneak, and a key bound to a
     * bare modifier (e.g. JEI "hold Shift to pause") alongside a modifier combination (Shift+PageDown).
     */
    private static boolean intended(KeyMapping a, KeyMapping b) {
        return isPair(a, b, "key.sneak", "key.jade.show_details") || modifierAlone(a, b) || modifierAlone(b, a);
    }

    private static boolean modifierAlone(KeyMapping bare, KeyMapping combo) {
        return combo.getKeyModifier() != KeyModifier.NONE && combo.getKeyModifier().matches(bare.getKey())
                && !combo.getKey().equals(bare.getKey());
    }

    private static boolean isPair(KeyMapping a, KeyMapping b, String x, String y) {
        return (a.getName().equals(x) && b.getName().equals(y)) || (a.getName().equals(y) && b.getName().equals(x));
    }
}
