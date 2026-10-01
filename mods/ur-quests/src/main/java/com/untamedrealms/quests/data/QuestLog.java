package com.untamedrealms.quests.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** A player's quest journal: active quests with progress, completion history and flags. */
public final class QuestLog implements INBTSerializable<CompoundTag> {
    public static final class Active {
        public int stage;
        public int[] progress;
        public long startedAt;

        public Active(int stage, int objectives, long startedAt) {
            this.stage = stage;
            this.progress = new int[objectives];
            this.startedAt = startedAt;
        }
    }

    public record Completion(int times, long lastCompletedAt) {}

    private final Map<ResourceLocation, Active> active = new LinkedHashMap<>();
    private final Map<ResourceLocation, Completion> completed = new LinkedHashMap<>();
    private final Set<String> flags = new LinkedHashSet<>();
    private @Nullable ResourceLocation tracked;

    public Map<ResourceLocation, Active> active() { return active; }
    public Map<ResourceLocation, Completion> completed() { return completed; }
    public Set<String> flags() { return flags; }
    public @Nullable ResourceLocation tracked() { return tracked; }
    public void setTracked(@Nullable ResourceLocation id) { tracked = id; }

    public boolean isActive(ResourceLocation id) { return active.containsKey(id); }
    public boolean isCompleted(ResourceLocation id) { return completed.containsKey(id); }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        CompoundTag act = new CompoundTag();
        active.forEach((id, a) -> {
            CompoundTag q = new CompoundTag();
            q.putInt("stage", a.stage);
            q.putIntArray("progress", a.progress);
            q.putLong("started", a.startedAt);
            act.put(id.toString(), q);
        });
        tag.put("active", act);
        CompoundTag done = new CompoundTag();
        completed.forEach((id, c) -> {
            CompoundTag q = new CompoundTag();
            q.putInt("times", c.times());
            q.putLong("last", c.lastCompletedAt());
            done.put(id.toString(), q);
        });
        tag.put("completed", done);
        ListTag flagList = new ListTag();
        flags.forEach(f -> flagList.add(StringTag.valueOf(f)));
        tag.put("flags", flagList);
        if (tracked != null) tag.putString("tracked", tracked.toString());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        active.clear();
        completed.clear();
        flags.clear();
        CompoundTag act = tag.getCompound("active");
        for (String key : act.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null) continue;
            CompoundTag q = act.getCompound(key);
            int[] progress = q.getIntArray("progress");
            Active a = new Active(q.getInt("stage"), progress.length, q.getLong("started"));
            a.progress = progress;
            active.put(id, a);
        }
        CompoundTag done = tag.getCompound("completed");
        for (String key : done.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) completed.put(id, new Completion(done.getCompound(key).getInt("times"), done.getCompound(key).getLong("last")));
        }
        ListTag flagList = tag.getList("flags", Tag.TAG_STRING);
        for (int i = 0; i < flagList.size(); i++) flags.add(flagList.getString(i));
        tracked = tag.contains("tracked") ? ResourceLocation.tryParse(tag.getString("tracked")) : null;
    }
}
