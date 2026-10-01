package com.untamedrealms.magic.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Spells a player knows, the eight readied quick-slots, the selected slot and cooldowns. */
public final class SpellBook implements INBTSerializable<CompoundTag> {
    public static final int SLOTS = 8;

    private final Set<ResourceLocation> known = new LinkedHashSet<>();
    private final ResourceLocation[] slots = new ResourceLocation[SLOTS];
    private int selected;
    private final Map<ResourceLocation, Long> readyAt = new HashMap<>();

    public Set<ResourceLocation> known() { return known; }
    public boolean knows(ResourceLocation spell) { return known.contains(spell); }
    public @Nullable ResourceLocation slot(int i) { return i >= 0 && i < SLOTS ? slots[i] : null; }
    public int selected() { return selected; }
    public @Nullable ResourceLocation selectedSpell() { return slots[selected]; }

    public boolean learn(ResourceLocation spell) {
        if (!known.add(spell)) return false;
        // auto-ready newly learned spells into the first free slot
        for (int i = 0; i < SLOTS; i++) {
            if (slots[i] == null) { slots[i] = spell; break; }
        }
        return true;
    }

    public void setSlot(int i, @Nullable ResourceLocation spell) {
        if (i < 0 || i >= SLOTS) return;
        if (spell != null) for (int j = 0; j < SLOTS; j++) if (spell.equals(slots[j])) slots[j] = null;
        slots[i] = spell;
    }

    public void select(int i) { selected = Math.floorMod(i, SLOTS); }

    public void cycle(int direction) {
        for (int step = 1; step <= SLOTS; step++) {
            int next = Math.floorMod(selected + step * direction, SLOTS);
            if (slots[next] != null) { selected = next; return; }
        }
    }

    public long readyAt(ResourceLocation spell) { return readyAt.getOrDefault(spell, 0L); }
    public void setReadyAt(ResourceLocation spell, long tick) { readyAt.put(spell, tick); }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        known.forEach(s -> list.add(StringTag.valueOf(s.toString())));
        tag.put("known", list);
        ListTag slotList = new ListTag();
        for (ResourceLocation s : slots) slotList.add(StringTag.valueOf(s == null ? "" : s.toString()));
        tag.put("slots", slotList);
        tag.putInt("selected", selected);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        known.clear();
        Arrays.fill(slots, null);
        ListTag list = tag.getList("known", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) known.add(id);
        }
        ListTag slotList = tag.getList("slots", Tag.TAG_STRING);
        for (int i = 0; i < Math.min(SLOTS, slotList.size()); i++) {
            String s = slotList.getString(i);
            slots[i] = s.isEmpty() ? null : ResourceLocation.tryParse(s);
        }
        selected = Math.floorMod(tag.getInt("selected"), SLOTS);
    }
}
