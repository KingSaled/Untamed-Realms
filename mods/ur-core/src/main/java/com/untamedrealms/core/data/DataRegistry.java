package com.untamedrealms.core.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.untamedrealms.core.UntamedCore;
import com.untamedrealms.core.network.CorePayloads;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A data-driven registry of {@code T} loaded from {@code data/<namespace>/<directory>/*.json} with a
 * {@link Codec}, reloaded with {@code /reload} and (optionally) synced to clients.
 * <p>
 * Every content type in the suite - perks, classes, birthsigns, quests, NPCs, dialogues, shops,
 * spells - is one of these, so server owners can add or tweak content with a datapack alone.
 */
public final class DataRegistry<T> {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<ResourceLocation, DataRegistry<?>> ALL = new ConcurrentHashMap<>();

    private final ResourceLocation id;
    private final String directory;
    private final Codec<T> codec;
    private final boolean synced;
    private final Codec<Map<ResourceLocation, T>> mapCodec;
    private final List<Consumer<DataRegistry<T>>> reloadCallbacks = new ArrayList<>();
    private volatile Map<ResourceLocation, T> entries = Collections.emptyMap();

    private DataRegistry(ResourceLocation id, String directory, Codec<T> codec, boolean synced) {
        this.id = id;
        this.directory = directory;
        this.codec = codec;
        this.synced = synced;
        this.mapCodec = Codec.unboundedMap(ResourceLocation.CODEC, codec);
    }

    /**
     * Creates and registers a data registry. Call during mod construction (static init is fine).
     *
     * @param directory path under {@code data/<namespace>/}, e.g. {@code "urquests/quests"}
     * @param synced    whether clients need the contents (for screens, tooltips, ...)
     */
    public static <T> DataRegistry<T> create(ResourceLocation id, String directory, Codec<T> codec, boolean synced) {
        DataRegistry<T> registry = new DataRegistry<>(id, directory, codec, synced);
        if (ALL.putIfAbsent(id, registry) != null) {
            throw new IllegalStateException("Duplicate data registry " + id);
        }
        return registry;
    }

    public ResourceLocation id() { return id; }

    public Optional<T> get(ResourceLocation key) {
        return Optional.ofNullable(entries.get(key));
    }

    public T getOrNull(ResourceLocation key) {
        return key == null ? null : entries.get(key);
    }

    public boolean contains(ResourceLocation key) {
        return entries.containsKey(key);
    }

    public Map<ResourceLocation, T> entries() {
        return entries;
    }

    public Collection<T> values() {
        return entries.values();
    }

    /** Runs after every server reload and every client sync. */
    public void onReload(Consumer<DataRegistry<T>> callback) {
        reloadCallbacks.add(callback);
    }

    private void replace(Map<ResourceLocation, T> next) {
        this.entries = Collections.unmodifiableMap(next);
        for (Consumer<DataRegistry<T>> callback : reloadCallbacks) {
            callback.accept(this);
        }
    }

    // ---------------------------------------------------------------- server loading

    SimpleJsonResourceReloadListener createListener(HolderLookup.Provider registries) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        return new SimpleJsonResourceReloadListener(GSON, directory) {
            @Override
            protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
                Map<ResourceLocation, T> parsed = new LinkedHashMap<>();
                files.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            DataResult<T> result = codec.parse(ops, entry.getValue());
                            result.resultOrPartial(error -> UntamedCore.LOGGER.error(
                                            "[{}] Failed to load {}: {}", id, entry.getKey(), error))
                                    .ifPresent(value -> parsed.put(entry.getKey(), value));
                        });
                UntamedCore.LOGGER.info("[{}] Loaded {} entries", id, parsed.size());
                replace(parsed);
            }
        };
    }

    static Collection<DataRegistry<?>> all() {
        return ALL.values();
    }

    // ---------------------------------------------------------------- syncing

    void sendTo(ServerPlayer player) {
        if (!synced) return;
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, player.registryAccess());
        mapCodec.encodeStart(ops, entries)
                .resultOrPartial(error -> UntamedCore.LOGGER.error("[{}] Failed to encode for sync: {}", id, error))
                .ifPresent(tag -> {
                    CompoundTag wrapper = new CompoundTag();
                    wrapper.put("entries", tag);
                    PacketDistributor.sendToPlayer(player, new CorePayloads.DataSync(id, wrapper));
                });
    }

    /** Client side: replace contents with what the server sent. */
    public static void acceptSync(CorePayloads.DataSync payload, HolderLookup.Provider registries) {
        DataRegistry<?> registry = ALL.get(payload.registry());
        if (registry == null) {
            UntamedCore.LOGGER.warn("Received sync for unknown data registry {}", payload.registry());
            return;
        }
        registry.decodeAndReplace(payload.data().get("entries"), registries);
    }

    private void decodeAndReplace(Tag tag, HolderLookup.Provider registries) {
        if (tag == null) return;
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        mapCodec.parse(ops, tag)
                .resultOrPartial(error -> UntamedCore.LOGGER.error("[{}] Failed to decode sync: {}", id, error))
                .ifPresent(map -> replace(new LinkedHashMap<>(map)));
    }
}
