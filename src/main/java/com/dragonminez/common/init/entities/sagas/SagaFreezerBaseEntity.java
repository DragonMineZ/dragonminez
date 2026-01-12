package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import net.minecraft.sounds.SoundEvents;
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

public class SagaFreezerBaseEntity extends DBSagasEntity {

    private int kiLaserCooldown = 0;
    private int kiBlastCooldown = 0;
    private int teleportCooldown = 0;

    private int castTimer = 0;

    public SagaFreezerBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();

        if (target != null && target.isAlive()) {
            if (this.isFlying() || this.isCasting()) {
                rotateBodyToTarget(target);
            }
        }

        if (!this.level().isClientSide) {
            if (this.kiLaserCooldown > 0) this.kiLaserCooldown--;
            if (this.kiBlastCooldown > 0) this.kiBlastCooldown--;
            if (this.teleportCooldown > 0) this.teleportCooldown--;

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

            if (target != null && target.isAlive() && this.teleportCooldown <= 0 && !this.isCasting()) {
                performTeleport(target);
            }

            if (target != null && target.isAlive() && !this.isCasting()) {
                double distSqr = this.distanceToSqr(target);

                // Ki Blast (15 bloques -> 225 sqr)
                if (distSqr > 120.0D && this.kiBlastCooldown <= 0) {
                    startCasting(2); // Tipo 2 = Blast
                }
                //Ki Laser (10 bloques -> 100 sqr)
                else if (distSqr > 100.0D && this.kiLaserCooldown <= 0) {
                    startCasting(1); // Tipo 1 = Laser
                }
            }

            if (this.isCasting()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.5, 0.5));

                if (target != null && target.isAlive()) {
                    this.castTimer++;

                    int currentSkill = getSkillType();

                    if (currentSkill == 1) { // Laser
                        if (this.castTimer == 20) {
                            performKiLaserAttack(target);
                        }else if (this.castTimer == 40) {
                            performKiLaserAttack(target);
                        }else if (this.castTimer == 60) {
                            performKiLaserAttack(target);
                            stopCasting();
                        }
                    } else if (currentSkill == 2) { // Blast
                        if (this.castTimer >= 20) {
                            performKiBlastAttack(target);
                            stopCasting();
                        }
                    }
                } else {
                    stopCasting();
                }
            }
        }
    }

    private void startCasting(int type) {
        this.setCasting(true);
        this.setSkillType(type);
        this.castTimer = 0;

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        this.getNavigation().stop();
        this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
    }

    private void stopCasting() {
        this.setCasting(false);
        this.castTimer = 0;

        int usedSkill = getSkillType();

        if (usedSkill == 1) {
            this.kiLaserCooldown = 10 * 20;
        } else if (usedSkill == 2) {
            this.kiBlastCooldown = 20 * 20;
        }

        this.setSkillType(0);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25D);
    }

    private void performTeleport(LivingEntity target) {
        Vec3 targetLook = target.getLookAngle().normalize();

        double distanceBehind = 0.7D;
        double destX = target.getX() - (targetLook.x * distanceBehind);
        double destZ = target.getZ() - (targetLook.z * distanceBehind);
        double destY = target.getY();

        this.teleportTo(destX, destY, destZ);

        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);

        this.teleportCooldown = 8 * 20;

        this.lookAt(target, 360, 360);
    }

    private void performKiLaserAttack(LivingEntity target) {
        Vec3 startPos = this.getEyePosition();
        Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
        double dX = targetPos.x - startPos.x;
        double dY = targetPos.y - startPos.y;
        double dZ = targetPos.z - startPos.z;
        double dist = Math.sqrt(dX * dX + dZ * dZ);

        float yaw = (float) (Mth.atan2(dZ, dX) * (double) (180F / (float) Math.PI)) - 90.0F;
        float pitch = (float) (-(Mth.atan2(dY, dist) * (double) (180F / (float) Math.PI)));

        this.setYRot(yaw);
        this.setXRot(pitch);
        this.setYHeadRot(yaw);

        KiLaserEntity laserEntity = new KiLaserEntity(this.level(), this);
        laserEntity.setPos(startPos.x, startPos.y, startPos.z);
        laserEntity.setColors(0xBA1616, 0x850707);
        laserEntity.setKiDamage(this.getKiBlastDamage());
        laserEntity.setKiSpeed(2.3f);

        this.level().addFreshEntity(laserEntity);
    }

    private void performKiBlastAttack(LivingEntity target) {
        KiBlastEntity kiBlast = new KiBlastEntity(this.level(), this);

        double sx = this.getX();
        double sy = this.getY() + 1.0D;
        double sz = this.getZ();

        kiBlast.setPos(sx, sy, sz);
        kiBlast.setColors(0x8A2FCC, 0x5D1294);
        kiBlast.setSize(2.5f);
        kiBlast.setKiDamage(this.getKiBlastDamage());
        kiBlast.setOwner(this);

        double tx = target.getX() - sx;
        double ty = (target.getY() + target.getEyeHeight() * 0.5D) - sy;
        double tz = target.getZ() - sz;

        kiBlast.shoot(tx, ty, tz, 1.5F, 1.0F);

        this.level().addFreshEntity(kiBlast);
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
        super.registerControllers(controllers);
        controllers.add(new AnimationController<>(this, "skill_controller", 0, this::skillPredicate));
        controllers.add(new AnimationController<>(this, "tail", 0, this::tailPredicate));
    }

    private <T extends GeoAnimatable> PlayState tailPredicate(AnimationState<T> event) {
        event.getController().setAnimation(RawAnimation.begin().thenLoop("tail"));
        return PlayState.CONTINUE;
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isCasting()) {

            int currentSkill = getSkillType();

            if (currentSkill == 1) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay("kilaser"));
            } else if (currentSkill == 2) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay("kiball"));
            }
            return PlayState.CONTINUE;
        }

        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}