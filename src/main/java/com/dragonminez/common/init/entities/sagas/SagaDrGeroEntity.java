package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class SagaDrGeroEntity extends DBSagasEntity {

    private static final int SKILL_EYE_LASER = 1;
    private static final int SKILL_ENERGY_DRAIN = 2; // El "Grab"

    private int laserCooldown = 0;
    private int drainCooldown = 0;

    public SagaDrGeroEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        if (this instanceof IBattlePower bp) {
            bp.setBattlePower(150000000);
        }
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();

        handleCommonCombatMovement(target, this.isCasting(), true);

        if (!this.level().isClientSide) {
            // Cooldowns
            if (this.laserCooldown > 0) this.laserCooldown--;
            if (this.drainCooldown > 0) this.drainCooldown--;

            if (target != null && target.isAlive() && !this.isCasting()) {
                double distSqr = this.distanceToSqr(target);

                if (this.teleportCooldown <= 0 && distSqr > 200.0D) {
                    performTeleport(target);
                    return;
                }

                if (this.drainCooldown <= 0 && distSqr < 9.0D) {
                    startCasting(SKILL_ENERGY_DRAIN);
                }
                else if (this.laserCooldown <= 0 && distSqr > 100.0D) {
                    startCasting(SKILL_EYE_LASER);
                }
            }

            if (this.isCasting()) {
                this.setDeltaMovement(0, 0, 0);
                this.getNavigation().stop();

                if (target != null && target.isAlive()) {
                    this.castTimer++;
                    int currentSkill = getSkillType();

                    if (currentSkill == SKILL_EYE_LASER) {
                        this.lookAt(target, 30.0F, 30.0F);

                        if (this.castTimer == 20) {
                            performDoubleEyeLaser(target);
                            stopCasting();
                        }
                    }

                    else if (currentSkill == SKILL_ENERGY_DRAIN) {
                        performGrabLogic(target);
                    }

                } else {
                    stopCasting();
                }
            }
        }
    }

    private void performGrabLogic(LivingEntity target) {
        Vec3 viewVector = this.getViewVector(1.0F);
        double grabDist = 1.2D;
        double targetX = this.getX() + viewVector.x * grabDist;
        double targetY = this.getY() + 0.5D;
        double targetZ = this.getZ() + viewVector.z * grabDist;

        target.setPos(targetX, targetY, targetZ);
        target.setDeltaMovement(0,0,0);
        target.hurtMarked = true;

        this.lookAt(target, 360, 360);

        if (this.castTimer % 10 == 0) {
            float damageAmount = getKiBlastDamage();

            target.hurt(this.damageSources().mobAttack(this), damageAmount);

            this.heal(damageAmount);

            if (target instanceof Player player) {
                StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {

                    int maxEnergy = stats.getMaxEnergy();
                    int drainPercentage = (int) (maxEnergy * 0.05);

                    if (drainPercentage < 1) drainPercentage = 1;

                    stats.getResources().removeEnergy(drainPercentage);
                });
            }
        }
        if (this.castTimer >= 60) {
            target.setDeltaMovement(viewVector.scale(1.5));
            stopCasting();
        }
    }

    private void performDoubleEyeLaser(LivingEntity target) {
        shootGenericKiLaser(target, 2.0F, 0xFFFF00, 0xFFD700);
        shootGenericKiLaser(target, 2.0F, 0xFFFF00, 0xFFD700);
    }

    @Override
    public void stopCasting() {
        int usedSkill = getSkillType();

        if (usedSkill == SKILL_EYE_LASER) {
            this.laserCooldown = 8 * 20;
        } else if (usedSkill == SKILL_ENERGY_DRAIN) {
            this.drainCooldown = 15 * 20;
        }

        super.stopCasting();
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isCasting() && getSkillType() == SKILL_ENERGY_DRAIN) {
            return false;
        }
        return super.hurt(pSource, pAmount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<>(this, "skill_controller", 0, this::skillPredicate));
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isCasting()) {
            int currentSkill = getSkillType();

            if (currentSkill == SKILL_EYE_LASER) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("kilaser"));
            }
            else if (currentSkill == SKILL_ENERGY_DRAIN) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("grab"));
            }
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}