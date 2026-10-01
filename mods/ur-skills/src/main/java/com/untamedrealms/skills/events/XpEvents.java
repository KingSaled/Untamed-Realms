package com.untamedrealms.skills.events;

import com.untamedrealms.skills.SkillsConfig;
import com.untamedrealms.skills.UntamedSkills;
import com.untamedrealms.skills.api.Gear;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.data.PlacedBlocks;
import com.untamedrealms.skills.data.SkillsData;
import com.untamedrealms.skills.data.XpSource;
import com.untamedrealms.skills.registry.SkillsAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Every way a player earns skill XP by playing. */
@EventBusSubscriber(modid = UntamedSkills.MODID)
public final class XpEvents {
    /** Position of the last player-placed block each player broke (no bonus drops for it). */
    static final Map<UUID, Long> LAST_PLACED_BREAK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_XP_LEVEL = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> LAST_POS = new ConcurrentHashMap<>();

    private XpEvents() {}

    // ------------------------------------------------------------------ gathering

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        LevelChunk chunk = level.getChunkAt(pos);
        if (chunk.hasData(SkillsAttachments.PLACED_BLOCKS.get())) {
            PlacedBlocks placed = chunk.getData(SkillsAttachments.PLACED_BLOCKS);
            if (placed.remove(pos)) {
                chunk.setUnsaved(true);
                if (SkillsConfig.ANTI_PLACE_EXPLOIT.get()) {
                    LAST_PLACED_BREAK.put(player.getUUID(), pos.asLong());
                    return;
                }
            }
        }
        LAST_PLACED_BREAK.remove(player.getUUID());
        if (state.requiresCorrectToolForDrops() && !player.hasCorrectToolForDrops(state)) return;

        for (XpSource source : SkillsData.xpSources(XpSource.Trigger.BREAK_BLOCK)) {
            XpSource.Entry entry = SkillsData.bestEntry(source, state);
            if (entry == null) continue;
            if (entry.matureOnly() && !isMature(state)) continue;
            SkillsApi.addXp(player, source.skill(), entry.xp());
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!SkillsData.grantsBreakXp(event.getPlacedBlock())) return;
        LevelChunk chunk = level.getChunkAt(event.getPos());
        chunk.getData(SkillsAttachments.PLACED_BLOCKS).add(event.getPos());
        chunk.setUnsaved(true);
    }

    /** Crops and other ageing plants only grant XP when fully grown. */
    public static boolean isMature(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && property.getName().equals("age")) {
                int max = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
                return state.getValue(age) >= max;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (ItemStack stack : event.getDrops()) awardItem(player, XpSource.Trigger.FISH, stack);
        // Fishing passive + perks: chance of an extra catch.
        float chance = SkillsApi.level(player, Skill.FISHING) * 0.003f
                + SkillsApi.effect(player, com.untamedrealms.skills.effect.EffectTypes.EXTRA_DROP, Skill.FISHING);
        if (!event.getDrops().isEmpty() && player.getRandom().nextFloat() < chance) {
            net.neoforged.neoforge.items.ItemHandlerHelper.giveItemToPlayer(player, event.getDrops().get(0).copy());
        }
    }

    @SubscribeEvent
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) awardItem(player, XpSource.Trigger.SMELT, event.getSmelting());
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) awardItem(player, XpSource.Trigger.CRAFT, event.getCrafting());
    }

    @SubscribeEvent
    public static void onBrewed(PlayerBrewedPotionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) awardItem(player, XpSource.Trigger.BREW, event.getStack());
    }

    @SubscribeEvent
    public static void onConsumed(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) awardItem(player, XpSource.Trigger.CONSUME, event.getItem());
    }

    private static void awardItem(ServerPlayer player, XpSource.Trigger trigger, ItemStack stack) {
        if (stack.isEmpty()) return;
        int count = Math.max(1, stack.getCount());
        for (XpSource source : SkillsData.xpSources(trigger)) {
            XpSource.Entry entry = SkillsData.bestEntry(source, stack);
            if (entry != null) SkillsApi.addXp(player, source.skill(), entry.xp() * count);
        }
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer player) SkillsApi.addXp(player, Skill.FARMING, 12);
    }

    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (event.getTamer() instanceof ServerPlayer player) SkillsApi.addXp(player, Skill.FARMING, 25);
    }

    // ------------------------------------------------------------------ combat

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        float damage = Math.min(event.getNewDamage(), target.getMaxHealth());
        if (damage <= 0) return;

        // Offence: weapon skills.
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && target != attacker && !(target instanceof ArmorStand)) {
            Skill skill = null;
            if (event.getSource().getDirectEntity() instanceof AbstractArrow) {
                skill = Skill.ARCHERY;
            } else if (event.getSource().getDirectEntity() == attacker) {
                skill = Gear.weaponSkill(attacker.getMainHandItem());
            }
            if (skill != null) {
                SkillsApi.addXp(attacker, skill, damage * SkillsConfig.COMBAT_XP_PER_DAMAGE.get());
            }
        }

        // Defence: armor skills, only for hits from creatures (no fall / lava farming).
        if (target instanceof ServerPlayer victim && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != victim) {
            float xp = Math.max(damage, 0.5f) * SkillsConfig.ARMOR_XP_PER_DAMAGE.get().floatValue();
            int heavy = Gear.piecesWorn(victim, Skill.HEAVY_ARMOR);
            int light = Gear.piecesWorn(victim, Skill.LIGHT_ARMOR);
            if (heavy > 0) SkillsApi.addXp(victim, Skill.HEAVY_ARMOR, xp * heavy / 4f);
            if (light > 0) SkillsApi.addXp(victim, Skill.LIGHT_ARMOR, xp * light / 4f);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!event.getBlocked() || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getDamageSource().getEntity() == null) return;
        SkillsApi.addXp(player, Skill.BLOCK, event.getBlockedDamage() * SkillsConfig.BLOCK_XP_PER_DAMAGE.get());

        // Block passive: chance to shove the attacker back.
        int level = SkillsApi.level(player, Skill.BLOCK);
        if (event.getDamageSource().getDirectEntity() instanceof LivingEntity attacker
                && player.getRandom().nextFloat() < Math.min(0.5f, level * 0.006f)) {
            attacker.knockback(0.6, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
        }
    }

    // ------------------------------------------------------------------ speech, enchanting, smithing

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillsApi.addXp(player, Skill.SPEECH, 6 + event.getMerchantOffer().getXp() * 2);
        }
    }

    @SubscribeEvent
    public static void onAnvil(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getRight().is(Items.ENCHANTED_BOOK)) {
            SkillsApi.addXp(player, Skill.ENCHANTING, 30);
        } else if (!event.getRight().isEmpty()) {
            SkillsApi.addXp(player, Skill.SMITHING, 15);
        }
    }

    // ------------------------------------------------------------------ per-tick: enchanting, sneak, agility

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) return;
        UUID id = player.getUUID();

        // Enchanting: levels spent at an enchanting table.
        if (player.containerMenu instanceof EnchantmentMenu) {
            Integer before = LAST_XP_LEVEL.get(id);
            if (before != null && player.experienceLevel < before && !player.isCreative()) {
                SkillsApi.addXp(player, Skill.ENCHANTING, (before - player.experienceLevel) * 35);
            }
            LAST_XP_LEVEL.put(id, player.experienceLevel);
        } else {
            LAST_XP_LEVEL.remove(id);
        }

        if (player.tickCount % 20 != 0) return;

        // Sneak: staying hidden near hostiles.
        if (player.isShiftKeyDown()) {
            List<Mob> nearby = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(12),
                    mob -> mob instanceof Enemy && mob.isAlive() && mob.getTarget() != player);
            if (!nearby.isEmpty()) SkillsApi.addXp(player, Skill.SNEAK, Math.min(3, nearby.size()));
        }

        // Agility: ground covered while sprinting, swimming and climbing.
        Vec3 now = player.position();
        Vec3 last = LAST_POS.put(id, now);
        if (last != null && player.getVehicle() == null && !player.getAbilities().flying) {
            double horizontal = Math.sqrt((now.x - last.x) * (now.x - last.x) + (now.z - last.z) * (now.z - last.z));
            double vertical = now.y - last.y;
            if (horizontal < 40) {
                if (player.isSprinting()) SkillsApi.addXp(player, Skill.AGILITY, horizontal * 0.12);
                else if (player.isSwimming()) SkillsApi.addXp(player, Skill.AGILITY, horizontal * 0.15);
            }
            if (player.onClimbable() && vertical > 0 && vertical < 20) SkillsApi.addXp(player, Skill.AGILITY, vertical * 0.5);
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isSprinting()) {
            SkillsApi.addXp(player, Skill.AGILITY, 0.25);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFall(LivingFallEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        float distance = event.getDistance();
        if (distance > 4 && distance < 40) SkillsApi.addXp(player, Skill.AGILITY, (distance - 3) * 1.5);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        LAST_PLACED_BREAK.remove(id);
        LAST_XP_LEVEL.remove(id);
        LAST_POS.remove(id);
    }
}
