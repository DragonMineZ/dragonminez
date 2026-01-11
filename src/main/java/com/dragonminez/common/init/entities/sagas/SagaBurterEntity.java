package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.SPBlueHurricaneEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
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

public class SagaBurterEntity extends DBSagasEntity {

    private int hurricaneCooldown = 0;
    private int hurricaneActiveTimer = 0;

    public SagaBurterEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();

        if (target != null && target.isAlive()) {
            rotateBodyToTarget(target);
        }

        if (!this.level().isClientSide) {
            if (this.hurricaneCooldown > 0) this.hurricaneCooldown--;

            if (this.hurricaneActiveTimer > 0) {
                this.hurricaneActiveTimer--;
            } else {
                if (this.isCasting()) {
                    this.setCasting(false);
                }
            }

            handleFlightLogic(target);

            double distanceToTarget = (target != null) ? this.distanceToSqr(target) : 0;

            if (target != null && target.isAlive() && this.hurricaneCooldown <= 0 && !this.isCasting()) {
                if (distanceToTarget < 225.0D) {
                    performBlueHurricane();
                }
            }
        }
    }

    private void performBlueHurricane() {
        this.setCasting(true);
        this.hurricaneActiveTimer = 140;
        this.hurricaneCooldown = 12 * 20;

        SPBlueHurricaneEntity hurricane = new SPBlueHurricaneEntity(this.level(), this);

        hurricane.setup(this, this.getKiBlastDamage(), 3.0F, 0.0f,0x0000FF, 0x00FFFF);

        this.level().addFreshEntity(hurricane);

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
            }
            else if (yDiff <= 1.0D && this.onGround()) {
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
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isCasting()) {
            pAmount *= 0.5F;
        }
        return super.hurt(pSource, pAmount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "skill_controller", 0, this::skillPredicate));
        super.registerControllers(controllers);
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isCasting()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("kiwave"));
            return PlayState.CONTINUE;
        }

        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}
