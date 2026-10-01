package com.untamedrealms.skills.events;

import com.untamedrealms.core.api.UR;
import com.untamedrealms.skills.SkillsConfig;
import com.untamedrealms.skills.UntamedSkills;
import com.untamedrealms.skills.api.Gear;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.data.RequirementSet;
import com.untamedrealms.skills.data.SkillsData;
import com.untamedrealms.skills.effect.EffectTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

/** What skill levels and effects actually do in play, plus enforcement of skill requirements. */
@EventBusSubscriber(modid = UntamedSkills.MODID)
public final class BonusEvents {
    private BonusEvents() {}

    // ------------------------------------------------------------------ damage dealt / taken

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();

        if (event.getSource().getEntity() instanceof ServerPlayer attacker && target != attacker) {
            boolean ranged = event.getSource().getDirectEntity() instanceof AbstractArrow;
            boolean melee = event.getSource().getDirectEntity() == attacker;
            Skill skill = ranged ? Skill.ARCHERY : melee ? Gear.weaponSkill(attacker.getMainHandItem()) : null;
            if (skill != null) {
                float multiplier = 1f + SkillsApi.level(attacker, skill) * SkillsConfig.WEAPON_DAMAGE_PER_LEVEL.get().floatValue()
                        + SkillsApi.effect(attacker, EffectTypes.DAMAGE_BONUS, skill);
                if (melee) multiplier *= weaponRequirementPenalty(attacker, attacker.getMainHandItem());
                amount *= multiplier;
            }
            // Sneak attacks: crouching and unnoticed.
            if ((ranged || melee) && attacker.isShiftKeyDown() && target instanceof Mob mob && mob.getTarget() != attacker) {
                float sneakMultiplier = SkillsConfig.BASE_SNEAK_ATTACK_MULTIPLIER.get().floatValue()
                        + SkillsApi.level(attacker, Skill.SNEAK) * 0.01f
                        + SkillsApi.effect(attacker, EffectTypes.SNEAK_ATTACK, ranged ? "archery" : "melee");
                amount *= sneakMultiplier;
                SkillsApi.addXp(attacker, Skill.SNEAK, Math.min(amount, target.getMaxHealth()) * 2);
                UR.subtle(attacker, Component.translatable("message.urskills.sneak_attack", String.format("%.1f", sneakMultiplier))
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        if (target instanceof ServerPlayer victim) {
            float reduction = SkillsApi.effect(victim, EffectTypes.DAMAGE_REDUCTION, "");
            int heavy = Gear.piecesWorn(victim, Skill.HEAVY_ARMOR);
            int light = Gear.piecesWorn(victim, Skill.LIGHT_ARMOR);
            reduction += heavy / 4f * SkillsApi.effect(victim, EffectTypes.DAMAGE_REDUCTION, "heavy_armor");
            reduction += light / 4f * SkillsApi.effect(victim, EffectTypes.DAMAGE_REDUCTION, "light_armor");
            if (victim.isBlocking()) reduction += SkillsApi.effect(victim, EffectTypes.DAMAGE_REDUCTION, "block");
            if (reduction > 0) amount *= 1f - Math.min(0.8f, reduction);
        }

        if (amount != event.getAmount()) event.setAmount(amount);
    }

    private static float weaponRequirementPenalty(ServerPlayer player, ItemStack weapon) {
        SkillsConfig.RequirementMode mode = SkillsConfig.WEAPON_REQUIREMENTS.get();
        if (mode == SkillsConfig.RequirementMode.OFF || player.isCreative()) return 1f;
        for (RequirementSet.Requirement req : SkillsData.requirements(RequirementSet.Kind.WEAPON, weapon)) {
            if (SkillsApi.level(player, req.skill()) < req.level()) {
                UR.warn(player, Component.translatable("requirement.urskills.weapon", req.skill().displayName(), req.level()));
                return mode == SkillsConfig.RequirementMode.HARD ? 0f : 0.5f;
            }
        }
        return 1f;
    }

    // ------------------------------------------------------------------ gathering speed, block requirements

    /** Runs on both sides with identical inputs so client prediction matches the server. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.isCreative()) return;
        BlockState state = event.getState();
        float speed = event.getNewSpeed();

        Skill skill = state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? Skill.MINING
                : state.is(BlockTags.MINEABLE_WITH_AXE) ? Skill.WOODCUTTING : null;
        if (skill != null) {
            speed *= 1f + SkillsApi.level(player, skill) * SkillsConfig.GATHER_SPEED_PER_LEVEL.get().floatValue()
                    + SkillsApi.effect(player, EffectTypes.SPEED_BONUS, skill);
        }

        SkillsConfig.RequirementMode toolMode = SkillsConfig.TOOL_REQUIREMENTS.get();
        if (toolMode != SkillsConfig.RequirementMode.OFF) {
            for (RequirementSet.Requirement req : SkillsData.requirements(RequirementSet.Kind.TOOL, player.getMainHandItem())) {
                if (SkillsApi.level(player, req.skill()) < req.level()) {
                    speed *= toolMode == SkillsConfig.RequirementMode.HARD ? 0f : 0.25f;
                    if (player instanceof ServerPlayer sp) {
                        UR.warn(sp, Component.translatable("requirement.urskills.tool", req.skill().displayName(), req.level()));
                    }
                    break;
                }
            }
        }

        SkillsConfig.RequirementMode blockMode = SkillsConfig.BLOCK_REQUIREMENTS.get();
        if (blockMode != SkillsConfig.RequirementMode.OFF) {
            for (RequirementSet.Requirement req : SkillsData.requirements(state)) {
                if (SkillsApi.level(player, req.skill()) < req.level()) {
                    speed *= blockMode == SkillsConfig.RequirementMode.HARD ? 0f : 0.1f;
                    if (player instanceof ServerPlayer sp) {
                        UR.warn(sp, Component.translatable("requirement.urskills.block", req.skill().displayName(), req.level()));
                    }
                    break;
                }
            }
        }
        if (speed != event.getNewSpeed()) event.setNewSpeed(Math.max(0f, speed));
    }

    /** Server-side backstop for HARD block requirements (instant-break tools, other mods' miners). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;
        if (SkillsConfig.BLOCK_REQUIREMENTS.get() != SkillsConfig.RequirementMode.HARD) return;
        for (RequirementSet.Requirement req : SkillsData.requirements(event.getState())) {
            if (SkillsApi.level(player, req.skill()) < req.level()) {
                UR.warn(player, Component.translatable("requirement.urskills.block", req.skill().displayName(), req.level()));
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || event.getDrops().isEmpty()) return;
        Long placed = XpEvents.LAST_PLACED_BREAK.get(player.getUUID());
        if (placed != null && placed == event.getPos().asLong()) return;
        BlockState state = event.getState();
        Skill skill = state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? Skill.MINING
                : state.is(BlockTags.LOGS) ? Skill.WOODCUTTING
                : state.is(BlockTags.CROPS) || state.is(BlockTags.MAINTAINS_FARMLAND) ? Skill.FARMING : null;
        if (skill == null) return;
        if (skill == Skill.FARMING && !XpEvents.isMature(state)) return;
        if (skill == Skill.MINING && SkillsData.xpSources(com.untamedrealms.skills.data.XpSource.Trigger.BREAK_BLOCK).stream()
                .noneMatch(src -> src.skill() == Skill.MINING && SkillsData.bestEntry(src, state) != null && SkillsData.bestEntry(src, state).xp() >= 5)) {
            return; // only ores and other valuable blocks double, not plain stone
        }
        float chance = SkillsApi.level(player, skill) * SkillsConfig.EXTRA_DROP_PER_LEVEL.get().floatValue()
                + SkillsApi.effect(player, EffectTypes.EXTRA_DROP, skill);
        if (player.getRandom().nextFloat() >= chance) return;
        List<ItemEntity> extra = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            ItemEntity copy = new ItemEntity(event.getLevel(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy());
            copy.setDefaultPickUpDelay();
            extra.add(copy);
        }
        event.getDrops().addAll(extra);
    }

    // ------------------------------------------------------------------ stealth, agility, armor requirements

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()) return;
        float stealth = SkillsApi.level(player, Skill.SNEAK) * 0.005f + SkillsApi.effect(player, EffectTypes.STEALTH, "");
        if (stealth > 0) event.modifyVisibility(1.0 - Math.min(0.8, stealth));
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        float reduction = SkillsApi.level(player, Skill.AGILITY) * 0.003f + SkillsApi.effect(player, EffectTypes.FALL_REDUCTION, "");
        if (reduction > 0) event.setDamageMultiplier(event.getDamageMultiplier() * (1f - Math.min(0.75f, reduction)));
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getSlot().isArmor()) return;
        if (SkillsConfig.ARMOR_REQUIREMENTS.get() == SkillsConfig.RequirementMode.HARD && !player.isCreative()) {
            ItemStack worn = event.getTo();
            RequirementSet.Requirement unmet = ArmorSkill.firstUnmet(player, worn);
            if (unmet != null) {
                EquipmentSlot slot = event.getSlot();
                ItemStack removed = worn.copy();
                player.setItemSlot(slot, ItemStack.EMPTY);
                if (!player.getInventory().add(removed)) player.drop(removed, false);
                UR.warn(player, Component.translatable("requirement.urskills.armor", unmet.skill().displayName(), unmet.level()));
            }
        }
        ArmorSkill.update(player);
    }

    // ------------------------------------------------------------------ cooking, alchemy, regen

    @SubscribeEvent
    public static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();

        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null) {
            float heal = food.nutrition() * (SkillsApi.level(player, Skill.COOKING) * 0.01f + SkillsApi.effect(player, EffectTypes.FOOD_HEALING, ""));
            if (heal > 0) player.heal(heal);
        }

        if (stack.getItem() instanceof PotionItem) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            float bonus = SkillsApi.level(player, Skill.ALCHEMY) * 0.004f + SkillsApi.effect(player, EffectTypes.POTION_DURATION, "");
            if (contents != null && bonus > 0) {
                for (MobEffectInstance effect : contents.getAllEffects()) {
                    Holder<MobEffect> type = effect.getEffect();
                    if (type.value().isInstantenous()) continue;
                    MobEffectInstance current = player.getEffect(type);
                    if (current == null) continue;
                    int extended = (int) (current.getDuration() * (1f + bonus));
                    player.addEffect(new MobEffectInstance(type, extended, current.getAmplifier(), current.isAmbient(), current.isVisible(), current.showIcon()));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 100 != 0) return;
        float regen = SkillsApi.effect(player, EffectTypes.REGEN, "health");
        if (regen > 0 && player.getHealth() < player.getMaxHealth() && player.isAlive()) player.heal(regen);
    }

    // ------------------------------------------------------------------ lockpicking (unlooted chests)

    @SubscribeEvent
    public static void onOpenContainer(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RandomizableContainerBlockEntity container) || container.getLootTable() == null) return;
        if (player.isSpectator()) return;

        var tableKey = container.getLootTable();
        long seed = container.getLootTableSeed();
        container.unpackLootTable(player);
        SkillsApi.addXp(player, Skill.LOCKPICKING, 25);

        float chance = SkillsApi.level(player, Skill.LOCKPICKING) * 0.004f + SkillsApi.effect(player, EffectTypes.LOOT_BONUS, "");
        if (player.getRandom().nextFloat() < chance) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(tableKey);
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withLuck(player.getLuck())
                    .create(LootContextParamSets.CHEST);
            table.fill((Container) container, params, seed ^ 0x5DEECE66DL);
            UR.subtle(player, Component.translatable("message.urskills.lockpicking_bonus").withStyle(ChatFormatting.GOLD));
        }
    }
}
