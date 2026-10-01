package com.untamedrealms.skills.api;

import com.untamedrealms.core.api.UR;
import com.untamedrealms.skills.SkillsConfig;
import com.untamedrealms.skills.data.SkillData;
import com.untamedrealms.skills.data.SkillsData;
import com.untamedrealms.skills.effect.EffectManager;
import com.untamedrealms.skills.effect.EffectProvider;
import com.untamedrealms.skills.effect.ClientEffectStore;
import com.untamedrealms.skills.events.ArmorSkill;
import com.untamedrealms.skills.effect.EffectTypes;
import com.untamedrealms.skills.network.SkillsNetwork;
import com.untamedrealms.skills.registry.SkillsAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Public API of the skill system. Other modules award XP, query levels and read aggregated
 * {@linkplain com.untamedrealms.skills.effect.SkillEffect effects} through this class.
 */
public final class SkillsApi {
    private SkillsApi() {}

    public static SkillData data(Player player) {
        return player.getData(SkillsAttachments.SKILLS);
    }

    public static int level(Player player, Skill skill) {
        return data(player).level(skill);
    }

    public static int characterLevel(Player player) {
        return data(player).characterLevel();
    }

    /**
     * Awards skill XP (before multipliers). Handles skill level-ups, character level-ups, banners and
     * syncing. Returns the XP actually granted.
     */
    public static double addXp(ServerPlayer player, Skill skill, double baseXp) {
        if (baseXp <= 0 || player.isSpectator()) return 0;
        double multiplier = SkillsConfig.GLOBAL_XP_MULTIPLIER.get() * (1.0 + effect(player, EffectTypes.XP_BONUS, skill));
        SkillXpEvent event = NeoForge.EVENT_BUS.post(new SkillXpEvent(player, skill, baseXp * multiplier));
        double amount = event.getAmount();
        if (event.isCanceled() || amount <= 0) return 0;

        SkillData data = data(player);
        int before = data.level(skill);
        double cap = SkillMath.xpForLevel(SkillMath.MAX_LEVEL) * 4; // allow "overflow" XP like RS, but bounded
        data.setXp(skill, Math.min(cap, data.xp(skill) + amount));
        int after = data.level(skill);

        SkillsNetwork.queueXpDrop(player, skill, (float) amount);
        if (after > before) onSkillLevelUp(player, skill, before, after);
        SkillsNetwork.markDirty(player);
        return amount;
    }

    /** Sets a skill straight to a level (admin / class start). Does not grant character XP. */
    public static void setLevel(ServerPlayer player, Skill skill, int level) {
        data(player).setXp(skill, SkillMath.xpForLevel(Math.max(1, Math.min(SkillMath.MAX_LEVEL, level))));
        refresh(player);
        SkillsNetwork.markDirty(player);
    }

    /** Raises a skill by whole levels as if earned (skill books, trainers). Grants character XP. */
    public static void grantLevels(ServerPlayer player, Skill skill, int levels) {
        SkillData data = data(player);
        int before = data.level(skill);
        int target = Math.min(SkillMath.MAX_LEVEL, before + levels);
        if (target <= before) return;
        data.setXp(skill, Math.max(data.xp(skill), SkillMath.xpForLevel(target)));
        onSkillLevelUp(player, skill, before, target);
        SkillsNetwork.markDirty(player);
    }

    private static void onSkillLevelUp(ServerPlayer player, Skill skill, int before, int after) {
        SkillData data = data(player);
        int gained = 0;
        for (int l = before + 1; l <= after; l++) gained += l;
        UR.banner(player,
                Component.translatable("banner.urskills.skill_up", skill.displayName(), after),
                Component.empty());
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.4f);
        NeoForge.EVENT_BUS.post(new SkillLevelUpEvent(player, skill, before, after));

        int cap = SkillsConfig.CHARACTER_LEVEL_CAP.get();
        int charXp = data.characterXp() + gained;
        int charLevel = data.characterLevel();
        int levelsGained = 0;
        while (charLevel < cap && charXp >= SkillMath.characterXpToNext(charLevel)) {
            charXp -= SkillMath.characterXpToNext(charLevel);
            charLevel++;
            levelsGained++;
        }
        data.setCharacterXp(charXp);
        if (levelsGained > 0) {
            data.setCharacterLevel(charLevel);
            data.addPerkPoints(levelsGained);
            data.addAttributePoints(levelsGained);
            data.setTrainedThisLevel(0);
            UR.banner(player,
                    Component.translatable("banner.urskills.level_up").withStyle(ChatFormatting.GOLD),
                    Component.translatable("banner.urskills.level_up.sub", charLevel));
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, 1.0f);
            NeoForge.EVENT_BUS.post(new CharacterLevelUpEvent(player, charLevel - levelsGained, charLevel));
        }
        // Passive bonuses scale with level, so rebuild modifiers (agility speed etc.).
        refresh(player);
    }

    /** Aggregated effect total for a type and key (from perks, class, birthsign, ...). */
    public static float effect(ServerPlayer player, String type, String key) {
        return EffectManager.get(player).get(type, key);
    }

    /** Aggregated effect total for a type and skill (includes category-wide and global effects). */
    public static float effect(ServerPlayer player, String type, Skill skill) {
        return EffectManager.get(player).get(type, skill);
    }

    /**
     * Effect lookup usable on either side: the server reads the authoritative cache, the client reads
     * the mirrored totals of the local player (other players read as zero on the client).
     */
    public static float effect(Player player, String type, Skill skill) {
        if (player instanceof ServerPlayer serverPlayer) return effect(serverPlayer, type, skill);
        return player.level().isClientSide ? ClientEffectStore.get(type, skill) : 0f;
    }

    public static float effect(Player player, String type, String key) {
        if (player instanceof ServerPlayer serverPlayer) return effect(serverPlayer, type, key);
        return player.level().isClientSide ? ClientEffectStore.get(type, key) : 0f;
    }

    /** Recomputes effects and attribute modifiers. Call after anything that changes effect sources. */
    public static void refresh(ServerPlayer player) {
        EffectManager.rebuild(player);
        ArmorSkill.update(player);
        SkillsNetwork.markDirty(player);
    }

    public static void registerEffectProvider(ResourceLocation id, EffectProvider provider) {
        EffectManager.registerProvider(id, provider);
    }

    public static boolean hasPerk(Player player, ResourceLocation perk) {
        return data(player).hasPerk(perk);
    }

    /** Validates and unlocks a perk. Returns a failure reason, or null on success. */
    public static Component tryUnlockPerk(ServerPlayer player, ResourceLocation perkId) {
        SkillsData.PerkRef ref = SkillsData.perk(perkId);
        SkillData data = data(player);
        if (ref == null) return Component.translatable("perk.urskills.error.unknown");
        if (data.hasPerk(perkId)) return Component.translatable("perk.urskills.error.owned");
        if (data.level(ref.skill()) < ref.perk().level())
            return Component.translatable("perk.urskills.error.level", ref.skill().displayName(), ref.perk().level());
        for (ResourceLocation req : ref.requires()) {
            if (!data.hasPerk(req)) {
                SkillsData.PerkRef reqRef = SkillsData.perk(req);
                return Component.translatable("perk.urskills.error.requires", reqRef == null ? Component.literal(req.toString()) : reqRef.perk().name());
            }
        }
        if (data.perkPoints() < ref.perk().cost()) return Component.translatable("perk.urskills.error.points");
        data.addPerkPoints(-ref.perk().cost());
        data.addPerk(perkId);
        refresh(player);
        SkillsNetwork.markDirty(player);
        UR.subtle(player, Component.translatable("perk.urskills.unlocked", ref.perk().name()).withStyle(ChatFormatting.GOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7f, 1.2f);
        return null;
    }

    public static final int ATTR_HEALTH = 0, ATTR_MAGICKA = 1, ATTR_STAMINA = 2;

    /** Spends an attribute point from a character level-up. */
    public static boolean spendAttributePoint(ServerPlayer player, int which) {
        SkillData data = data(player);
        if (data.attributePoints() <= 0) return false;
        switch (which) {
            case ATTR_HEALTH -> {
                if (data.healthPicks() >= SkillsConfig.MAX_HEALTH_PICKS.get()) return false;
                data.addHealthPick();
            }
            case ATTR_MAGICKA -> data.addMagickaPick();
            case ATTR_STAMINA -> data.addStaminaPick();
            default -> { return false; }
        }
        data.addAttributePoints(-1);
        refresh(player);
        SkillsNetwork.markDirty(player);
        return true;
    }
}
