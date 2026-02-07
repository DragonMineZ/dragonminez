package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class SagaCellSuperPerfectEntity extends DBSagasEntity {

    private static final int SKILL_KAMEHA = 1;
    private static final int SKILL_KILASER = 2;
    private static final int SKILL_BARRIER = 3;

    private int kamehaCooldown = 0;
    private int kilaserCooldown = 0;
    private int barrierCooldown = 0;

    public SagaCellSuperPerfectEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setFlySpeed(0.6D);
        this.setAuraColor(0xFFF06E);
        this.setKiCharge(true);
        this.setLightning(true);
        this.setLightningColor(0xA1FFFF);

        if (this instanceof IBattlePower bp) {
            bp.setBattlePower(2147483647);
        }
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();

        handleCommonCombatMovement(target, this.isCasting(), true);

        if (!this.level().isClientSide) {
            if (this.kamehaCooldown > 0) this.kamehaCooldown--;
            if (this.kilaserCooldown > 0) this.kilaserCooldown--;
            if (this.barrierCooldown > 0) this.barrierCooldown--;

            if (target != null && target.isAlive() && !this.isCasting()) {
                double distSqr = this.distanceToSqr(target);

                if (this.teleportCooldown <= 0 && distSqr > 256.0D) {
                    performTeleport(target);
                    return;
                }

                if (this.barrierCooldown <= 0 && distSqr < 400.0D) {
                    startCasting(SKILL_BARRIER);
                }
                else if (this.kilaserCooldown <= 0 && distSqr > 25.0D) {
                    startCasting(SKILL_KILASER);
                }
                else if (this.kamehaCooldown <= 0 && distSqr > 100.0D) {
                    startCasting(SKILL_KAMEHA);
                }
            }

            if (this.isCasting()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.5, 0.5));

                if (target != null) {
                    this.lookAt(target, 30.0F, 30.0F);
                }

                if (target != null && target.isAlive()) {
                    this.castTimer++;
                    int currentSkill = getSkillType();

                    if (currentSkill == SKILL_KAMEHA) {
                        if (this.castTimer >= 50) {
                            shootGenericKiWave(target, 2.0F, 0xB0FDFF, 0x40FAFF, 2.0f);
                            stopCasting();
                        }
                    }
                    else if (currentSkill == SKILL_KILASER) {
                        if (this.castTimer >= 25) {
                            shootGenericKiLaser(target, 2.5F, 0xE040FB, 0xAA00FF);
                            stopCasting();
                        }
                    }
                    else if (currentSkill == SKILL_BARRIER) {
                        if (this.castTimer == 10) {
                            shootKiBarrier(0xB246FF, 0xB246FF);
                        }

                        if (this.castTimer >= 40) {
                            stopCasting();
                        }
                    }

                } else {
                    stopCasting();
                }
            }
        }
    }

    @Override
    public void stopCasting() {
        int usedSkill = getSkillType();

        if (usedSkill == SKILL_KAMEHA) {
            this.kamehaCooldown = 20 * 20;
        }
        else if (usedSkill == SKILL_KILASER) {
            this.kilaserCooldown = 6 * 20;
        }
        else if (usedSkill == SKILL_BARRIER) {
            this.barrierCooldown = 15 * 20;
        }

        super.stopCasting();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<>(this, "skill_controller", 0, this::skillPredicate));
    }

    @Override
    public void performTeleport(LivingEntity target) {
        Vec3 targetLook = target.getLookAngle().normalize();

        double distanceBehind = 0.7D;
        double destX = target.getX() - (targetLook.x * distanceBehind);
        double destZ = target.getZ() - (targetLook.z * distanceBehind);
        double destY = target.getY();

        this.teleportTo(destX, destY, destZ);

        this.playSound(MainSounds.TP.get(), 1.0F, 1.0F);

        this.teleportCooldown = 4 * 20;

        this.lookAt(target, 360, 360);
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isCasting()) {
            int currentSkill = getSkillType();

            if (currentSkill == SKILL_KAMEHA) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("kiwave"));
            }
            else if (currentSkill == SKILL_KILASER) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("kilaser"));
            }
            else if (currentSkill == SKILL_BARRIER) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("barrier"));
            }

            return PlayState.CONTINUE;
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}