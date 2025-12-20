package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
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

public class SagaRaditzEntity extends DBSagasEntity{

    private int kiBlastCooldown = 0;
    private int castTimer = 0;

    public SagaRaditzEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();

        //Para que el jugador vea a la entidad volar correctamente
        if (target != null && target.isAlive()) {
            if (this.isFlying() || this.isCasting()) {
                rotateBodyToTarget(target);
            }
        }

        if (!this.level().isClientSide) {
            if (this.kiBlastCooldown > 0) this.kiBlastCooldown--;
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

            var distancePlayer = 10.0D;
            if (target != null && target.isAlive() && this.kiBlastCooldown <= 0 &&
                    this.distanceToSqr(target) > distancePlayer && !this.isCasting()) {
                startCasting();
            }

            if (this.isCasting()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.5, 0.5));
                if (target != null && target.isAlive()) {
                    this.castTimer++;
                    if (this.castTimer >= 50) {
                        performKiBlastAttack(target);
                        stopCasting();
                    }
                } else {
                    stopCasting();
                }
            }
        }
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
    private void startCasting() {
        this.setCasting(true);
        this.castTimer = 0;

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);

        this.getNavigation().stop();
        this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
    }

    private void stopCasting() {
        this.setCasting(false);
        this.castTimer = 0;
        this.kiBlastCooldown = 10 * 20;

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25D);
    }
    private void moveTowardsTargetInAir(LivingEntity target) {
        if (this.isCasting()) return;
        double flyspeed = 0.15D;
        double dx = target.getX() - this.getX();
        double dy = (target.getY() + 1.0D) - this.getY();
        double dz = target.getZ() - this.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 1.0) return;
        Vec3 movement = new Vec3(dx / distance * flyspeed, dy / distance * flyspeed, dz / distance * flyspeed);
        double gravityDrag = (dy < -0.5) ? -0.05D : -0.03D;
        this.setDeltaMovement(movement.add(0, gravityDrag, 0));
    }

    private void performKiBlastAttack(LivingEntity target) {
        KiBlastEntity kiBlast = new KiBlastEntity(this.level(), this);

        double sx = this.getX();
        double sy = this.getY() + 1.0D;
        double sz = this.getZ();

        kiBlast.setPos(sx, sy, sz);
        kiBlast.setColors(0xF157FF, 0x850491);
        kiBlast.setSize(2.5f);
        kiBlast.setKiDamage(20.0f);
        kiBlast.setOwner(this);

        double tx = target.getX() - sx;
        double ty = (target.getY() + target.getEyeHeight() * 0.5D) - sy;
        double tz = target.getZ() - sz;

        kiBlast.shoot(tx, ty, tz, 0.6F, 1.0F);

        this.level().addFreshEntity(kiBlast);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "skill_controller", 0, this::skillPredicate));
        super.registerControllers(controllers);
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isCasting()) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("kiwave"));
            return PlayState.CONTINUE;
        }
        event.getController().forceAnimationReset();

        return PlayState.STOP;
    }


}
