package com.untamedrealms.skills.data;

import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillMath;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Everything the skill system persists for one player. Copied on death. */
public final class SkillData implements INBTSerializable<CompoundTag> {
    private final double[] xp = new double[Skill.VALUES.size()];
    private int characterLevel = 1;
    private int characterXp;
    private int perkPoints;
    private int attributePoints;
    private int healthPicks;
    private int magickaPicks;
    private int staminaPicks;
    private int trainedThisLevel;
    private final Set<ResourceLocation> perks = new LinkedHashSet<>();

    public double xp(Skill skill) { return xp[skill.ordinal()]; }
    public int level(Skill skill) { return SkillMath.levelForXp(xp[skill.ordinal()]); }
    public void setXp(Skill skill, double value) { xp[skill.ordinal()] = Math.max(0, value); }

    public int characterLevel() { return characterLevel; }
    public int characterXp() { return characterXp; }
    public int perkPoints() { return perkPoints; }
    public int attributePoints() { return attributePoints; }
    public int healthPicks() { return healthPicks; }
    public int magickaPicks() { return magickaPicks; }
    public int staminaPicks() { return staminaPicks; }
    public int trainedThisLevel() { return trainedThisLevel; }
    public Set<ResourceLocation> perks() { return Collections.unmodifiableSet(perks); }
    public boolean hasPerk(ResourceLocation id) { return perks.contains(id); }

    public void setCharacterLevel(int level) { this.characterLevel = level; }
    public void setCharacterXp(int value) { this.characterXp = value; }
    public void addPerkPoints(int n) { this.perkPoints = Math.max(0, perkPoints + n); }
    public void addAttributePoints(int n) { this.attributePoints = Math.max(0, attributePoints + n); }
    public void addHealthPick() { healthPicks++; }
    public void addMagickaPick() { magickaPicks++; }
    public void addStaminaPick() { staminaPicks++; }
    public void setTrainedThisLevel(int n) { trainedThisLevel = n; }
    public void addPerk(ResourceLocation id) { perks.add(id); }

    /** Wipes the character (used by admin reset / class re-roll). */
    public void reset() {
        java.util.Arrays.fill(xp, 0);
        characterLevel = 1;
        characterXp = 0;
        perkPoints = 0;
        attributePoints = 0;
        healthPicks = magickaPicks = staminaPicks = 0;
        trainedThisLevel = 0;
        perks.clear();
    }

    /** Refunds every perk, returning the points. */
    public int respecPerks() {
        int refunded = perks.size();
        perks.clear();
        perkPoints += refunded;
        return refunded;
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        CompoundTag skills = new CompoundTag();
        for (Skill skill : Skill.VALUES) {
            if (xp[skill.ordinal()] > 0) skills.putDouble(skill.id(), xp[skill.ordinal()]);
        }
        tag.put("xp", skills);
        tag.putInt("level", characterLevel);
        tag.putInt("charXp", characterXp);
        tag.putInt("perkPoints", perkPoints);
        tag.putInt("attributePoints", attributePoints);
        tag.putInt("health", healthPicks);
        tag.putInt("magicka", magickaPicks);
        tag.putInt("stamina", staminaPicks);
        tag.putInt("trained", trainedThisLevel);
        ListTag perkList = new ListTag();
        for (ResourceLocation id : perks) perkList.add(StringTag.valueOf(id.toString()));
        tag.put("perks", perkList);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        CompoundTag skills = tag.getCompound("xp");
        for (Skill skill : Skill.VALUES) xp[skill.ordinal()] = skills.getDouble(skill.id());
        characterLevel = Math.max(1, tag.getInt("level"));
        characterXp = tag.getInt("charXp");
        perkPoints = tag.getInt("perkPoints");
        attributePoints = tag.getInt("attributePoints");
        healthPicks = tag.getInt("health");
        magickaPicks = tag.getInt("magicka");
        staminaPicks = tag.getInt("stamina");
        trainedThisLevel = tag.getInt("trained");
        perks.clear();
        ListTag perkList = tag.getList("perks", Tag.TAG_STRING);
        for (int i = 0; i < perkList.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(perkList.getString(i));
            if (id != null) perks.add(id);
        }
    }
}
