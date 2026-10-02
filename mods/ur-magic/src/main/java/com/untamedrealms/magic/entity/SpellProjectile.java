package com.untamedrealms.magic.entity;

import com.untamedrealms.magic.UntamedMagic;
import com.untamedrealms.magic.data.SpellDef;
import com.untamedrealms.magic.spell.SpellEffects;
import com.untamedrealms.magic.fx.MagicFx;
import com.untamedrealms.magic.fx.SpellFx;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** An invisible bolt drawn entirely with particles (fire, frost, holy light, healing...). */
public class SpellProjectile extends ThrowableProjectile {
    private static final EntityDataAccessor<String> SPELL = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ELEMENT = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.STRING);
    private static final int MAX_AGE = 80;
    private float power = 1f;

    public SpellProjectile(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public static SpellProjectile create(ServerPlayer caster, ResourceLocation spell, SpellDef def, float power) {
        SpellProjectile p = new SpellProjectile(UntamedMagic.SPELL_PROJECTILE.get(), caster.level());
        p.setOwner(caster);
        p.setPos(caster.getEyePosition().add(caster.getLookAngle().scale(0.6)));
        p.entityData.set(SPELL, spell.toString());
        p.entityData.set(ELEMENT, def.element());
        p.power = power;
        p.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0f, def.speed(), 0.5f);
        return p;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SPELL, "");
        builder.define(ELEMENT, "arcane");
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof net.minecraft.server.level.ServerLevel server) {
            if (tickCount > MAX_AGE) {
                discard();
                return;
            }
            SpellFx.trail(server, this, entityData.get(ELEMENT), random);
        }
    }

    public static ParticleOptions particle(String element) {
        return MagicFx.particle(element);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide || !(getOwner() instanceof ServerPlayer caster)) return;
        SpellDef def = UntamedMagic.SPELLS.getOrNull(ResourceLocation.tryParse(entityData.get(SPELL)));
        if (def != null && result.getEntity() instanceof LivingEntity target && target != caster) {
            SpellEffects.applyToTarget(caster, def, target, power, this);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                SpellFx.impact(server, position().add(0, 0.2, 0), entityData.get(ELEMENT), random);
            }
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Spell", entityData.get(SPELL));
        tag.putFloat("Power", power);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SPELL, tag.getString("Spell"));
        power = tag.getFloat("Power");
    }
}
