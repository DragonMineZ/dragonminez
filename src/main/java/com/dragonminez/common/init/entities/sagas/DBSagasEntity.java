package com.dragonminez.common.init.entities.sagas;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class DBSagasEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_CASTING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_FLYING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SKILL_TYPE = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);    //0; none, 1; kiblast, 2; rugido

    private static final EntityDataAccessor<Integer> BATTLE_POWER = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TRANSFORMING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);

    private double roarDamage = 50.0D;
    private double roarRange = 15.0D;
    private double flySpeed = 0.45D;
    private float kiBlastDamage = 20.0F;
    private float kiBlastSpeed = 0.6F;

    private boolean isAttacking = false;


    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    protected DBSagasEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.8D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 15.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));

    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 5, this::walkPredicate));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, this::attackPredicate));

    }

    private <T extends GeoAnimatable> PlayState walkPredicate(AnimationState<T> event) {
        DBSagasEntity entity = (DBSagasEntity) event.getAnimatable();

        if (isCasting()) {
            return PlayState.STOP;
        }
        if (entity.isFlying()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("fly"));
            return PlayState.CONTINUE;
        }

        if (event.isMoving()) {
            if (entity.isAggressive() || entity.getTarget() != null) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("run"));
            } else {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
            }
            return PlayState.CONTINUE;
        }

        event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    private <T extends GeoAnimatable> PlayState attackPredicate(AnimationState<T> event) {
        DBSagasEntity entity = (DBSagasEntity) event.getAnimatable();

        if (isCasting()) {
            return PlayState.STOP;
        }
        if (isTransforming()) {
            return PlayState.STOP;
        }

        if (entity.swingTime > 0 && !isAttacking) {
            isAttacking = true;
            event.getController().forceAnimationReset();
            if (this.random.nextBoolean()) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay("attack1"));
            } else {
                event.getController().setAnimation(RawAnimation.begin().thenPlay("attack2"));
            }
            return PlayState.CONTINUE;
        }
        if (isAttacking) {
            if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
                isAttacking = false;
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_CASTING, false);
        this.entityData.define(IS_FLYING, false);
        this.entityData.define(SKILL_TYPE, 0);
        this.entityData.define(BATTLE_POWER, 20);
        this.entityData.define(TRANSFORMING, false);
    }

    public void setCasting(boolean casting) { this.entityData.set(IS_CASTING, casting); }
    public boolean isCasting() { return this.entityData.get(IS_CASTING); }

    public void setFlying(boolean flying) { this.entityData.set(IS_FLYING, flying); }
    public boolean isFlying() { return this.entityData.get(IS_FLYING); }

    public int getSkillType() { return this.entityData.get(SKILL_TYPE); }
    public void setSkillType(int type) { this.entityData.set(SKILL_TYPE, type); }

    public int getBattlePower() { return this.entityData.get(BATTLE_POWER); }
    public void setBattlePower(int type) { this.entityData.set(BATTLE_POWER, type); }

    public boolean isTransforming() {return this.entityData.get(TRANSFORMING);}
    public void setTransforming(boolean transforming) {this.entityData.set(TRANSFORMING, transforming);}

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }
    public boolean isBattleDamaged() {
        return this.getHealth() <= this.getMaxHealth() / 2.0F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putDouble("RoarDamage", this.roarDamage);
        pCompound.putDouble("RoarRange", this.roarRange);
        pCompound.putDouble("FlySpeed", this.flySpeed);
        pCompound.putFloat("KiBlastDamage", this.kiBlastDamage);
        pCompound.putFloat("KiBlastSpeed", this.kiBlastSpeed);
    }
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("RoarDamage")) this.roarDamage = pCompound.getDouble("RoarDamage");
        if (pCompound.contains("RoarRange")) this.roarRange = pCompound.getDouble("RoarRange");
        if (pCompound.contains("FlySpeed")) this.flySpeed = pCompound.getDouble("FlySpeed");
        if (pCompound.contains("KiBlastDamage")) this.kiBlastDamage = pCompound.getFloat("KiBlastDamage");
        if (pCompound.contains("KiBlastSpeed")) this.kiBlastSpeed = pCompound.getFloat("KiBlastSpeed");
    }

    public double getRoarDamage() { return roarDamage;}
    public void setRoarDamage(double roarDamage) {this.roarDamage = roarDamage;}

    public double getRoarRange() {return roarRange;}
    public void setRoarRange(double roarRange) {this.roarRange = roarRange;}

    public float getKiBlastDamage() {return kiBlastDamage;}
    public void setKiBlastDamage(float kiBlastDamage) {this.kiBlastDamage = kiBlastDamage;}

    public double getFlySpeed() {return flySpeed;}
    public void setFlySpeed(double flySpeed) {this.flySpeed = flySpeed;}

    public float getKiBlastSpeed() {return kiBlastSpeed;}
    public void setKiBlastSpeed(float kiBlastSpeed) {this.kiBlastSpeed = kiBlastSpeed;}

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isTransforming()) {
            return false;
        }

        return super.hurt(pSource, pAmount);
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        if (this.isTransforming()) {
            return false;
        }

        if (this.isCasting()) {
            return false;
        }

        return super.doHurtTarget(pEntity);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

	@Override
	public boolean checkSpawnRules(LevelAccessor pLevel, MobSpawnType reason) {
		return true;
	}

	public static boolean canSpawnHere(EntityType<? extends DBSagasEntity> entity, ServerLevelAccessor world, MobSpawnType spawn, BlockPos pos, RandomSource random) {
		if (world.getDifficulty() != Difficulty.PEACEFUL) {
			return world.getBlockState(pos.below()).isValidSpawn(world, pos.below(), entity);
		}
		return false;
	}
}
