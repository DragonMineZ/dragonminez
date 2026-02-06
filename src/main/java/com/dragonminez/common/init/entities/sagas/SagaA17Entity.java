package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class SagaA17Entity extends DBSagasEntity {

    private static final int SKILL_KI_DISC = 1;
    private static final int SKILL_BARRIER = 2;

    private int discCooldown = 0;
    private int barrierCooldown = 0;

    public SagaA17Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        if (this instanceof IBattlePower bp) {
            bp.setBattlePower(700000000);
        }
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();

        handleCommonCombatMovement(target, this.isCasting(), true);

        if (!this.level().isClientSide) {
            if (this.discCooldown > 0) this.discCooldown--;
            if (this.barrierCooldown > 0) this.barrierCooldown--;

            if (target != null && target.isAlive() && !this.isCasting()) {
                double distSqr = this.distanceToSqr(target);

                if (this.teleportCooldown <= 0 && distSqr > 256.0D) {
                    performTeleport(target);
                    return;
                }

                if (this.barrierCooldown <= 0) {
                    startCasting(SKILL_BARRIER);
                }
                else if (this.discCooldown <= 0 && distSqr > 25.0D) {
                    startCasting(SKILL_KI_DISC);
                }
            }

            if (this.isCasting()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.5, 0.5));

                if (target != null) {
                    this.lookAt(target, 30.0F, 30.0F);
                }

                if (target != null && target.isAlive()) {
                    this.castTimer++;
                    int skill = getSkillType();

                    if (skill == SKILL_KI_DISC) {
                        if (this.castTimer >= 50) {
                            shootGenericKiDisc(10.5F, 0x12C75C, 1.8F);
                            stopCasting();
                        }
                    }
                    else if (skill == SKILL_BARRIER) {

                        if (this.castTimer == 10) {
                            shootKiBarrier(0x13ED6C, 0x12C75C);
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
        int skill = getSkillType();

        if (skill == SKILL_KI_DISC) {
            this.discCooldown = 8 * 20;
        }
        else if (skill == SKILL_BARRIER) {
            this.barrierCooldown = 20 * 20;
        }

        super.stopCasting();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<>(this, "skill_controller", 0, this::skillPredicate));
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isCasting()) {
            int skill = getSkillType();

            if (skill == SKILL_KI_DISC) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("kiwave"));
            }
            else if (skill == SKILL_BARRIER) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("barrier"));
            }
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}