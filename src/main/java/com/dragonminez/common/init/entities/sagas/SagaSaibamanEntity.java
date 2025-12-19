package com.dragonminez.common.init.entities.sagas;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class SagaSaibamanEntity extends DBSagasEntity{

    private static final EntityDataAccessor<Boolean> IS_EXPLODING = SynchedEntityData.defineId(SagaSaibamanEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean isAttacking = false;
    private int fuseTimer = 0;
    private int explodeTimer = 3;

    private boolean hasCheckedExplosionChance = false;

    public SagaSaibamanEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.isAlive()) return;
        //25% health
        float healthThreshold = this.getMaxHealth() * 0.25f;

        if (this.getHealth() <= healthThreshold && !this.hasCheckedExplosionChance && !isExploding()) {

            this.hasCheckedExplosionChance = true;
            //5% explode
            if (this.random.nextFloat() < 0.05F) {
                this.setExploding(true);
            }
        }
        /* Si el saibaman recupera mas del 25% de su vida entonces volvemos a verificar
        if (this.getHealth() > healthThreshold) {
            this.hasCheckedExplosionChance = false;
        }
         */
        if (isExploding()) {
            LivingEntity target = this.getTarget();
            if (target == null) {
                target = this.level().getNearestPlayer(this, 10.0D);
            }
            if (target != null) {
                Vec3 lookAngle = target.getLookAngle();
                double behindX = target.getX() - (lookAngle.x * 0.8D);
                double behindZ = target.getZ() - (lookAngle.z * 0.8D);
                double y = target.getY();

                this.setPos(behindX, y, behindZ);
                this.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                this.setYBodyRot(this.getYRot());
                this.setYHeadRot(this.getYRot());

                this.setDeltaMovement(0, 0, 0);
                this.getNavigation().stop();
                this.setTarget(null);
                this.setAggressive(false);
            }

            fuseTimer++;

            if (fuseTimer >= explodeTimer * 20) {
                explode();
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 5, this::walkPredicate));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, this::attackPredicate));
        controllers.add(new AnimationController<>(this, "explode_controller", 0, this::explodePredicate));
    }

    private <T extends GeoAnimatable> PlayState explodePredicate(AnimationState<T> event) {
        if (this.isExploding()) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("explode"));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    private <T extends GeoAnimatable> PlayState walkPredicate(AnimationState<T> event) {
        DBSagasEntity entity = (DBSagasEntity) event.getAnimatable();
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
        if (entity.swingTime > 0 && !isAttacking) {
            isAttacking = true;
            event.getController().forceAnimationReset();
            event.getController().setAnimation(RawAnimation.begin().thenPlay("attack"));
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

    private void explode() {
        if (!this.level().isClientSide) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 4.0F, Level.ExplosionInteraction.MOB);
            this.discard();
        }
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        if (this.isExploding()) {
            return false;
        }
        return super.doHurtTarget(pEntity);
    }

    @Override
    public boolean canAttack(LivingEntity pTarget) {
        if (this.isExploding()) {
            return false;
        }
        return super.canAttack(pTarget);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_EXPLODING, false);
    }

    public void setExploding(boolean exploding) {
        this.entityData.set(IS_EXPLODING, exploding);
    }

    public boolean isExploding() {
        return this.entityData.get(IS_EXPLODING);
    }
}

