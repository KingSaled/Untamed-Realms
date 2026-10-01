package com.untamedrealms.core.vitals;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Current Magicka and Stamina of a player. Maximums come from attributes; this only stores the
 * current pool and regeneration timers. Not copied on death - you respawn fully rested.
 */
public final class Vitals implements INBTSerializable<CompoundTag> {
    /** Negative means "not initialised yet": fill to max on first tick. */
    private float magicka = -1;
    private float stamina = -1;
    private int magickaRegenCooldown;
    private int staminaRegenCooldown;
    private boolean exhausted;

    public float magicka() { return magicka; }
    public float stamina() { return stamina; }
    public boolean exhausted() { return exhausted; }

    void setMagicka(float value) { this.magicka = value; }
    void setStamina(float value) { this.stamina = value; }
    void setExhausted(boolean exhausted) { this.exhausted = exhausted; }

    int magickaRegenCooldown() { return magickaRegenCooldown; }
    int staminaRegenCooldown() { return staminaRegenCooldown; }
    void setMagickaRegenCooldown(int ticks) { this.magickaRegenCooldown = ticks; }
    void setStaminaRegenCooldown(int ticks) { this.staminaRegenCooldown = ticks; }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("magicka", magicka);
        tag.putFloat("stamina", stamina);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        magicka = tag.contains("magicka") ? tag.getFloat("magicka") : -1;
        stamina = tag.contains("stamina") ? tag.getFloat("stamina") : -1;
    }
}
