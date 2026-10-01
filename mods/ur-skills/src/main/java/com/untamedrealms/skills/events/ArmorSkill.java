package com.untamedrealms.skills.events;

import com.untamedrealms.skills.SkillsConfig;
import com.untamedrealms.skills.UntamedSkills;
import com.untamedrealms.skills.api.Gear;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.data.RequirementSet;
import com.untamedrealms.skills.data.SkillsData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * Armor skill passive: Heavy/Light Armor levels raise total armor rating in proportion to how many
 * pieces of that type are worn. Also applies the SOFT armor-requirement slowdown.
 */
public final class ArmorSkill {
    private static final ResourceLocation ARMOR_BONUS = UntamedSkills.id("armor_skill");
    private static final ResourceLocation UNSKILLED_SLOW = UntamedSkills.id("unskilled_armor");

    private ArmorSkill() {}

    public static void update(ServerPlayer player) {
        double perLevel = SkillsConfig.ARMOR_BONUS_PER_LEVEL.get();
        int heavy = Gear.piecesWorn(player, Skill.HEAVY_ARMOR);
        int light = Gear.piecesWorn(player, Skill.LIGHT_ARMOR);
        double bonus = heavy / 4.0 * SkillsApi.level(player, Skill.HEAVY_ARMOR) * perLevel
                + light / 4.0 * SkillsApi.level(player, Skill.LIGHT_ARMOR) * perLevel;

        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.removeModifier(ARMOR_BONUS);
            if (bonus > 0) armor.addTransientModifier(new AttributeModifier(ARMOR_BONUS, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }

        int unmet = 0;
        if (SkillsConfig.ARMOR_REQUIREMENTS.get() == SkillsConfig.RequirementMode.SOFT) {
            for (EquipmentSlot slot : Gear.armorSlots()) {
                if (firstUnmet(player, player.getItemBySlot(slot)) != null) unmet++;
            }
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(UNSKILLED_SLOW);
            if (unmet > 0) speed.addTransientModifier(new AttributeModifier(UNSKILLED_SLOW, -0.1 * unmet, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** The first armor requirement the player does not meet for this item, or null. */
    public static RequirementSet.Requirement firstUnmet(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (RequirementSet.Requirement req : SkillsData.requirements(RequirementSet.Kind.ARMOR, stack)) {
            if (SkillsApi.level(player, req.skill()) < req.level()) return req;
        }
        return null;
    }
}
