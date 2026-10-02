package com.untamedrealms.magic.spell;

import com.untamedrealms.magic.UntamedMagic;
import com.untamedrealms.magic.data.SpellDef;
import com.untamedrealms.magic.entity.SpellProjectile;
import com.untamedrealms.magic.fx.SpellFx;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** The coded spell kinds. Each spell's data picks a kind and tunes it. */
public final class SpellEffects {
    /** Summoned creatures and when they unravel (server game time). */
    static final Map<UUID, Long> SUMMONS = new ConcurrentHashMap<>();
    static final String EXPIRES_TAG = "urmagic_summon_expires";

    private SpellEffects() {}

    /** Executes a spell. Returns false if it had nothing to act on (no magicka is spent then). */
    public static boolean cast(ServerPlayer caster, ResourceLocation id, SpellDef def, float power) {
        ServerLevel level = caster.serverLevel();
        switch (def.kind()) {
            case "projectile" -> level.addFreshEntity(SpellProjectile.create(caster, id, def, power));
            case "cone" -> cone(caster, def, power);
            case "lightning" -> { return lightning(caster, def, power); }
            case "self" -> self(caster, def, power);
            case "area" -> area(caster, def, power);
            case "summon" -> { return summon(caster, def, power); }
            case "bound_weapon" -> boundWeapon(caster, def, power);
            default -> {
                UntamedMagic.LOGGER.warn("Unknown spell kind '{}' for {}", def.kind(), id);
                return false;
            }
        }
        SpellFx.cast(caster, def.element(), def.kind());
        return true;
    }

    /** What a spell does to a creature it hits (projectiles, cones, lightning). */
    public static void applyToTarget(ServerPlayer caster, SpellDef def, LivingEntity target, float power, Entity direct) {
        if (def.heal() > 0) {
            if (target instanceof Player || target instanceof TamableAnimal || target instanceof IronGolem) target.heal(def.heal() * power);
            if (!(def.damage() > 0)) return;
        }
        float damage = def.damage() * power;
        boolean undead = target.getType().is(EntityTypeTags.UNDEAD);
        if ("holy".equals(def.element())) damage *= undead ? 2.0f : 0.5f;
        if (damage > 0) {
            DamageSource source = caster.level().damageSources().indirectMagic(direct, caster);
            target.hurt(source, damage);
        }
        switch (def.element()) {
            case "fire" -> target.igniteForSeconds(3 + Math.round(2 * power));
            case "frost" -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 60, target.getTicksFrozen() + 120));
            }
            case "shock" -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            default -> { }
        }
        applyEffects(def, target);
    }

    private static void applyEffects(SpellDef def, LivingEntity target) {
        for (SpellDef.EffectSpec spec : def.effects()) {
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(spec.effect()).orElse(null);
            if (effect != null) target.addEffect(new MobEffectInstance(effect, spec.duration(), spec.amplifier()));
        }
    }

    // ------------------------------------------------------------------ kinds

    private static void cone(ServerPlayer caster, SpellDef def, float power) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        ServerLevel level = caster.serverLevel();
        SpellFx.cone(caster, def.element(), def.range());
        boolean shock = "shock".equals(def.element());
        if (shock) {
            // a couple of stray arcs into the air even when nothing is hit
            for (int i = 0; i < 2; i++) {
                Vec3 miss = eye.add(look.add((caster.getRandom().nextDouble() - 0.5) * 0.5, (caster.getRandom().nextDouble() - 0.5) * 0.5,
                        (caster.getRandom().nextDouble() - 0.5) * 0.5).normalize().scale(def.range() * (0.5 + caster.getRandom().nextDouble() * 0.5)));
                SpellFx.arc(level, SpellFx.hand(caster), miss, caster.getRandom());
            }
        }
        AABB box = caster.getBoundingBox().expandTowards(look.scale(def.range())).inflate(def.radius() / 2);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
            Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0).subtract(eye);
            if (to.length() > def.range() + 1 || to.normalize().dot(look) < 0.82) continue;
            if (shock) SpellFx.arc(level, SpellFx.hand(caster), target.position().add(0, target.getBbHeight() / 2, 0), caster.getRandom());
            applyToTarget(caster, def, target, power, caster);
        }
    }

    private static boolean lightning(ServerPlayer caster, SpellDef def, float power) {
        ServerLevel level = caster.serverLevel();
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(def.range()));
        HitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(caster, eye, stop,
                caster.getBoundingBox().expandTowards(stop.subtract(eye)).inflate(1), e -> e instanceof LivingEntity && e != caster, def.range() * def.range());
        Vec3 strike = entityHit != null ? entityHit.getLocation() : stop;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return false;
        bolt.moveTo(strike);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
        SpellFx.arc(level, SpellFx.hand(caster), strike, caster.getRandom());
        SpellFx.impact(level, strike, def.element(), caster.getRandom());
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(strike, strike).inflate(def.radius()), e -> e != caster)) {
            applyToTarget(caster, def, target, power, caster);
        }
        return true;
    }

    private static void self(ServerPlayer caster, SpellDef def, float power) {
        if (def.heal() > 0) caster.heal(def.heal() * power);
        for (SpellDef.EffectSpec spec : def.effects()) {
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(spec.effect()).orElse(null);
            if (effect != null) caster.addEffect(new MobEffectInstance(effect, Math.round(spec.duration() * power), spec.amplifier()));
        }
        if ("cure".equals(def.area())) {
            caster.getActiveEffects().stream().filter(e -> !e.getEffect().value().isBeneficial()).map(MobEffectInstance::getEffect).toList()
                    .forEach(caster::removeEffect);
        }
        SpellFx.spiral(caster, def.element());
    }

    private static void area(ServerPlayer caster, SpellDef def, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = def.radius() * Math.min(2f, power);
        SpellFx.ring(caster, def.element(), radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(radius), e -> e != caster && e.isAlive());
        for (LivingEntity target : targets) {
            switch (def.area()) {
                case "undead" -> {
                    if (target.getType().is(EntityTypeTags.UNDEAD)) {
                        applyToTarget(caster, def, target, power, caster);
                        flee(caster, target, 1.2);
                    }
                }
                case "glow" -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, Math.round(def.duration() * power), 0));
                case "calm" -> {
                    if (target instanceof Mob mob && target instanceof Enemy && mob.getMaxHealth() <= 40 * power) {
                        mob.setTarget(null);
                        mob.setLastHurtByMob(null);
                        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.round(def.duration() * power), 1));
                        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.round(def.duration() * power), 2));
                    }
                }
                case "fear" -> {
                    if (target instanceof Mob mob && target instanceof Enemy && mob.getMaxHealth() <= 40 * power) {
                        mob.setTarget(null);
                        flee(caster, target, 1.5);
                        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.round(def.duration() * power), 1));
                    }
                }
                case "heal" -> {
                    if (target instanceof Player || target instanceof TamableAnimal) target.heal(def.heal() * power);
                }
                default -> {
                    if (target instanceof Enemy) applyToTarget(caster, def, target, power, caster);
                }
            }
        }
    }

    private static void flee(ServerPlayer caster, LivingEntity target, double strength) {
        Vec3 away = target.position().subtract(caster.position()).normalize().scale(strength);
        target.push(away.x, 0.35, away.z);
        target.hurtMarked = true;
    }

    private static boolean summon(ServerPlayer caster, SpellDef def, float power) {
        ServerLevel level = caster.serverLevel();
        EntityType<?> type = def.entity().flatMap(id -> level.registryAccess().registryOrThrow(Registries.ENTITY_TYPE).getOptional(id)).orElse(null);
        if (type == null) return false;
        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob)) return false;
        Vec3 pos = caster.position().add(caster.getLookAngle().multiply(2, 0, 2));
        mob.moveTo(pos.x, caster.getY(), pos.z, caster.getYRot(), 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        if (mob instanceof TamableAnimal pet) pet.tame(caster);
        if (mob instanceof IronGolem golem) golem.setPlayerCreated(true);
        mob.setCustomName(net.minecraft.network.chat.Component.translatable("entity.urmagic.summoned", mob.getType().getDescription(), caster.getDisplayName()));
        mob.setPersistenceRequired();
        // Conjured allies grow with Conjuration skill and perks.
        var health = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(health.getBaseValue() * power);
            mob.setHealth(mob.getMaxHealth());
        }
        level.addFreshEntity(mob);
        SpellFx.pillar(level, mob.position(), def.element());
        long expires = level.getGameTime() + Math.round(def.duration() * power);
        mob.getPersistentData().putLong(EXPIRES_TAG, expires);
        SUMMONS.put(mob.getUUID(), expires);
        return true;
    }

    private static void boundWeapon(ServerPlayer caster, SpellDef def, float power) {
        ItemStack sword = new ItemStack(UntamedMagic.BOUND_SWORD.get());
        sword.set(UntamedMagic.EXPIRES.get(), caster.serverLevel().getGameTime() + Math.round(def.duration() * power));
        sword.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
        SpellFx.spiral(caster, def.element());
        if (caster.getMainHandItem().isEmpty()) caster.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
        else if (!caster.getInventory().add(sword)) caster.drop(sword, false);
    }
}
