package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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

public class SagaFreezer2ndEntity extends DBSagasEntity{

    private static final EntityDataAccessor<Boolean> IS_GRABBING =  SynchedEntityData.defineId(SagaFreezer2ndEntity.class, EntityDataSerializers.BOOLEAN);

    private int grabCooldown = 0;
    private int grabTimer = 0;
    private LivingEntity heldTarget = null;

    private int transformTick = 0;

    private static final int GRAB_DURATION = 5 * 20;
    private static final int COOLDOWN_TIME = 15 * 20;
    private static final double GRAB_RANGE = 2.5D;

    public SagaFreezer2ndEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(1200000);
		}
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_GRABBING, false);
    }

    @Override
    public void tick() {
        super.tick();

        float[] auraColor = ColorUtils.rgbIntToFloat(0x880FFF);

        if (this.isTransforming()) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, 0, 0);

            if (this.heldTarget != null) {
                this.releaseTarget();
            }

            if (!this.level().isClientSide) {
                this.transformTick++;

                if (this.level() instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 50; i++) {

                        double offsetX = (this.random.nextDouble() - 0.5D) * 1.4D;
                        double offsetZ = (this.random.nextDouble() - 0.5D) * 1.4D;
                        double offsetY = this.random.nextDouble() * 0.8D;

                        serverLevel.sendParticles(
                                MainParticles.AURA.get(),
                                this.getX() + offsetX,
                                this.getY() + offsetY,
                                this.getZ() + offsetZ,
                                0,
                                auraColor[0],
                                auraColor[1],
                                auraColor[2],
                                1.0D
                        );
                    }

                }

                if (this.transformTick >= 100) {
                    finishTransformation();
                }
            }
            return;
        }

        if (!this.level().isClientSide && this.getHealth() <= this.getMaxHealth() / 2.0F) {
            startTransformation();
            return;
        }


        if (this.level().isClientSide) return;

        if (this.heldTarget != null) {
            tickHeldTarget();
            return;
        }

        LivingEntity target = this.getTarget();

        if (target != null && target.isAlive()) {
            double distSqr = this.distanceToSqr(target);
            if (this.grabCooldown > 0 && distSqr < 100.0D) {
                this.grabCooldown--;
            }

            if (this.grabCooldown <= 0 && distSqr <= (GRAB_RANGE * GRAB_RANGE)) {
                startGrab(target);
            }
        }

        handleFlyingMovement(target);
    }

    private void startTransformation() {
        this.setTransforming(true);
        this.releaseTarget();
        this.playSound(MainSounds.KI_CHARGE_LOOP.get(), 1.0F, 1.2F);
    }

    private void finishTransformation() {
        Level level = this.level();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1, this.getZ(), 1, 0, 0, 0, 0);

            DBSagasEntity newFreezer = MainEntities.SAGA_FREEZER_THIRD.get().create(level);

            if (newFreezer != null) {
                newFreezer.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
                newFreezer.setTarget(this.getTarget());
                level.addFreshEntity(newFreezer);
            }
            this.discard();
        }
    }

    private void startGrab(LivingEntity target) {
        this.heldTarget = target;
        this.grabTimer = GRAB_DURATION;

        this.entityData.set(IS_GRABBING, true);

        this.getNavigation().stop();
        this.setDeltaMovement(0, 0, 0);
        this.setNoGravity(true);

    }

    private void tickHeldTarget() {
        if (this.heldTarget == null || !this.heldTarget.isAlive() || this.grabTimer <= 0) {
            releaseTarget();
            return;
        }

        this.grabTimer--;

        this.setDeltaMovement(0, 0, 0);
        this.getNavigation().stop();

        this.setYBodyRot(this.getYRot());

        double holdHeight = this.getBbHeight() - 0.2D;

        Vec3 holdPos = this.position().add(0, holdHeight, 0);

        this.heldTarget.setPos(holdPos.x, holdPos.y, holdPos.z);
        this.heldTarget.setDeltaMovement(0,0,0);
        this.heldTarget.hurtMarked = true;

        this.heldTarget.setXRot(-90.0F);
        this.heldTarget.xRotO = -90.0F;
        this.heldTarget.setYRot(this.getYRot());
        this.heldTarget.setYHeadRot(this.getYRot());

        if (this.grabTimer % 10 == 0) {
            float dmg = this.getKiBlastDamage();
            this.heldTarget.hurt(this.damageSources().mobAttack(this), dmg);
        }
    }

    private void releaseTarget() {
        if (this.heldTarget != null) {
            Vec3 vec = this.getLookAngle().scale(0.5).add(0, 0.5, 0);
            this.heldTarget.setDeltaMovement(vec);
        }

        this.heldTarget = null;
        this.grabCooldown = COOLDOWN_TIME;
        this.setNoGravity(false);

        this.entityData.set(IS_GRABBING, false);
    }

    private void handleFlyingMovement(LivingEntity target) {
        if (target != null && target.isAlive()) {
            rotateBodyToTarget(target);

            double yDiff = target.getY() - this.getY();

            if (yDiff > 2.0D && !isFlying()) {
                setFlying(true);
            } else if (yDiff <= 1.0D && this.onGround() && isFlying()) {
                setFlying(false);
                this.setNoGravity(false);
            }
        } else {
            if (this.onGround() && isFlying()) {
                setFlying(false);
                this.setNoGravity(false);
            }
        }

        if (this.isFlying()) {
            this.setNoGravity(true);
            if (target != null) {
                moveTowardsTargetInAir(target);
            } else {
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.03D, 0));
            }
        } else {
            this.setNoGravity(false);
        }
    }

    private void rotateBodyToTarget(LivingEntity target) {
        double d0 = target.getX() - this.getX();
        double d2 = target.getZ() - this.getZ();
        float targetYaw = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
        this.setYRot(targetYaw);
        this.setYBodyRot(targetYaw);
        this.setYHeadRot(targetYaw);
    }

    private void moveTowardsTargetInAir(LivingEntity target) {
        double flyspeed = this.getFlySpeed();
        double dx = target.getX() - this.getX();
        double dy = (target.getY() + 1.0D) - this.getY();
        double dz = target.getZ() - this.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 1.0) return;

        Vec3 movement = new Vec3(dx / distance * flyspeed, dy / distance * flyspeed, dz / distance * flyspeed);
        double gravityDrag = (dy < -0.5) ? -0.05D : -0.03D;
        this.setDeltaMovement(movement.add(0, gravityDrag, 0));
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<>(this, "skill_controller", 0, this::skillPredicate));
        controllers.add(new AnimationController<>(this, "tail", 0, this::tailPredicate));

    }

    private <T extends GeoAnimatable> PlayState tailPredicate(AnimationState<T> event) {

        event.getController().setAnimation(RawAnimation.begin().thenLoop("tail"));

        return PlayState.CONTINUE;
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {

        if (this.isTransforming()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("transform"));
            return PlayState.CONTINUE;
        }

        if (this.entityData.get(IS_GRABBING)) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("grab"));
            return PlayState.CONTINUE;
        }

        event.getController().forceAnimationReset();

        return PlayState.STOP;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.heldTarget != null) {
            if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL)) {
                return super.hurt(source, amount);
            }
            return false;
        }
        return super.hurt(source, amount);
    }


}
