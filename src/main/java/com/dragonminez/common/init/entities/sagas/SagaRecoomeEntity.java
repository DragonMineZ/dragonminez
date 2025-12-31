package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiExplosionEntity;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;

public class SagaRecoomeEntity extends DBSagasEntity {

    private int skillCooldown = 0;
    private int skillTimer = 0;
    private static final int COOLDOWN_TIME = 300;

    public SagaRecoomeEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isCasting()) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);

            if (!this.level().isClientSide) {
                this.skillTimer++;

                if (this.skillTimer == 1) {
                    KiExplosionEntity explosion = new KiExplosionEntity(this.level(), this);

                    explosion.setupExplosion(
                            this,
                            explosion.getKiDamage(),
                            0xE58FFF,
                            0xFF00FF
                    );

                    this.level().addFreshEntity(explosion);
                }

                if (this.skillTimer >= KiExplosionEntity.DURATION) {
                    stopCasting();
                }
            }
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.isFlying()) {
            rotateBodyToTarget(target);
        }

        if (!this.level().isClientSide) {
            if (this.skillCooldown > 0) this.skillCooldown--;
            handleFlightLogic(target);

            double distanceToTargetSqr = (target != null) ? this.distanceToSqr(target) : 0;
            if (target != null && target.isAlive() && this.skillCooldown <= 0 && !this.isCasting()) {
                if (distanceToTargetSqr < 225.0D) {
                    startCasting();
                }
            }
        }
    }

    private void startCasting() {
        this.setCasting(true);
        this.skillTimer = 0;
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
    }

    private void stopCasting() {
        this.setCasting(false);
        this.skillTimer = 0;
        this.skillCooldown = COOLDOWN_TIME;
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.20D);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isCasting()) return false;
        return super.hurt(pSource, pAmount);
    }

    private void rotateBodyToTarget(LivingEntity target) {
        double d0 = target.getX() - this.getX();
        double d2 = target.getZ() - this.getZ();
        float targetYaw = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
        this.setYRot(targetYaw);
        this.setYBodyRot(targetYaw);
        this.setYHeadRot(targetYaw);
        this.yRotO = targetYaw;
        this.yBodyRotO = targetYaw;
        this.yHeadRotO = targetYaw;
    }

    private void handleFlightLogic(LivingEntity target) {
        if (target != null && target.isAlive()) {
            double yDiff = target.getY() - this.getY();
            if (yDiff > 2.0D) {
                if (!isFlying()) setFlying(true);
            } else if (yDiff <= 1.0D && this.onGround()) {
                if (isFlying()) {
                    setFlying(false);
                    this.setNoGravity(false);
                }
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

    private void moveTowardsTargetInAir(LivingEntity target) {
        if (this.isCasting()) return;
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
        controllers.add(new AnimationController<>(this, "skill_controller", 3, this::skillPredicate));
        super.registerControllers(controllers);
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isCasting()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("kiexplosion"));
            return PlayState.CONTINUE;
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}