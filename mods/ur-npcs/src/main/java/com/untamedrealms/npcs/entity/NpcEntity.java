package com.untamedrealms.npcs.entity;

import com.mojang.serialization.Dynamic;
import com.untamedrealms.npcs.data.NpcDef;
import com.untamedrealms.npcs.data.NpcsData;
import com.untamedrealms.npcs.dialogue.DialogueManager;
import com.untamedrealms.npcs.dialogue.Pickpocket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

/**
 * A townsperson. Its archetype ({@link NpcDef}) decides skin, title, dialogue, shop and trainer; the
 * entity itself only stores which archetype it is, its name and its home. Essential NPCs cannot die.
 * NPCs that carry a weapon (guards) defend themselves and attack nearby monsters.
 */
public class NpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> NPC_ID = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);

    private BlockPos home;
    private int talkingTicks;
    private @Nullable UUID talkingTo;
    private long angryUntil;

    public NpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        if (getNavigation() instanceof GroundPathNavigation nav) nav.setCanOpenDoors(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NPC_ID, "");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new TalkGoal());
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.8, false) {
            @Override public boolean canUse() { return isArmed() && super.canUse(); }
        });
        goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.55));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.45) {
            @Override public boolean canUse() { return talkingTicks <= 0 && super.canUse(); }
        });
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f, 0.4f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override public boolean canUse() { return isArmed() && super.canUse(); }
        });
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, m -> isArmed()));
    }

    private boolean isArmed() {
        return !getMainHandItem().isEmpty();
    }

    // ------------------------------------------------------------------ identity

    public ResourceLocation npcId() {
        return ResourceLocation.tryParse(entityData.get(NPC_ID));
    }

    public NpcDef def() {
        return NpcsData.NPCS.getOrNull(npcId());
    }

    /** Assigns the archetype: picks a name, equips gear and sets home to the current position. */
    public void setup(ResourceLocation id, NpcDef def) {
        entityData.set(NPC_ID, id.toString());
        String name = def.names().get(getRandom().nextInt(def.names().size()));
        setCustomName(Component.literal(name));
        setCustomNameVisible(true);
        setHome(blockPosition(), def.wander());
        for (Map.Entry<String, Dynamic<?>> e : def.equipment().entrySet()) {
            EquipmentSlot slot;
            try {
                slot = EquipmentSlot.byName(e.getKey());
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ItemStack stack = decode(e.getValue());
            if (!stack.isEmpty()) {
                setItemSlot(slot, stack);
                setDropChance(slot, 0f);
            }
        }
    }

    private <T> ItemStack decode(Dynamic<T> raw) {
        return ItemStack.CODEC.parse(RegistryOps.create(raw.getOps(), registryAccess()), raw.getValue()).result().orElse(ItemStack.EMPTY);
    }

    public void setHome(BlockPos pos, int radius) {
        home = pos;
        if (radius > 0) restrictTo(pos, radius);
        else restrictTo(pos, 1);
    }

    public BlockPos home() {
        return home;
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        if (level().getGameTime() < angryUntil) {
            serverPlayer.displayClientMessage(Component.translatable("message.urnpcs.angry", getDisplayName()), true);
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && !player.isCreative()) {
            Pickpocket.attempt(serverPlayer, this);
            return InteractionResult.CONSUME;
        }
        talkingTicks = 1200;
        talkingTo = player.getUUID();
        getNavigation().stop();
        getLookControl().setLookAt(player);
        DialogueManager.open(serverPlayer, this);
        return InteractionResult.CONSUME;
    }

    public void becomeAngry(int ticks) {
        angryUntil = level().getGameTime() + ticks;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (talkingTicks > 0) {
            talkingTicks--;
            Player partner = talkingPartner();
            if (partner == null || partner.distanceToSqr(this) > 64) stopTalking();
        }
    }

    private @Nullable Player talkingPartner() {
        return talkingTo == null ? null : level().getPlayerByUUID(talkingTo);
    }

    /** The conversation ended (goodbye, walked away): resume wandering. */
    public void stopTalking() {
        talkingTicks = 0;
        talkingTo = null;
    }

    /** Stands still and faces the player while a conversation or trade is open. */
    private final class TalkGoal extends Goal {
        TalkGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override public boolean canUse() { return talkingTicks > 0 && talkingPartner() != null; }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            Player partner = talkingPartner();
            if (partner != null) getLookControl().setLookAt(partner, 30f, 30f);
            getNavigation().stop();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        NpcDef def = def();
        boolean essential = def == null || def.essential();
        if (essential && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NpcId", entityData.get(NPC_ID));
        if (home != null) tag.putLong("Home", home.asLong());
        tag.putInt("HomeRadius", (int) getRestrictRadius());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(NPC_ID, tag.getString("NpcId"));
        if (tag.contains("Home")) setHome(BlockPos.of(tag.getLong("Home")), Math.max(1, tag.getInt("HomeRadius")));
    }
}
