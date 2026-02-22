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

public class SagaCellSemiPerfectEntity extends DBSagasEntity {

    private static final int SKILL_BIGBANG = 1;

    private int kiVolleyCooldown = 0;

    public SagaCellSemiPerfectEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.evade(true, 40);
        if (this instanceof IBattlePower bp) {
            bp.setBattlePower(1450000000);
		}
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();

        handleCommonCombatMovement(target, this.isCasting(), true);

        if (!this.level().isClientSide) {
            if (this.kiVolleyCooldown > 0) this.kiVolleyCooldown--;

            if (target != null && target.isAlive() && !this.isCasting()) {
                double distSqr = this.distanceToSqr(target);

                if (this.teleportCooldown <= 0 && distSqr > 200.0D) {
                    performTeleport(target);
                    return;
                }

                if (this.kiVolleyCooldown <= 0 && distSqr > 100.0D) {
                    startCasting(SKILL_BIGBANG);
                }
            }

            if (this.isCasting()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.5, 0.5));

                if (target != null && target.isAlive()) {
                    this.castTimer++;

                    if (getSkillType() == SKILL_BIGBANG) {
                        if (this.castTimer >= 50) {
                            shootGenericKiBlast(
                                    target,
                                    8.0f,
                                    0xD9234D,
                                    0x850925,
                                    3.0f
                            );

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

        if (usedSkill == SKILL_BIGBANG) {
            this.kiVolleyCooldown = 10 * 20;
        }
        super.stopCasting();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<>(this, "skill_controller", 0, this::skillPredicate));
        controllers.add(new AnimationController<>(this, "tail", 0, this::tailPredicate));

    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isCasting()) {
            int skill = getSkillType();

            if (skill == SKILL_BIGBANG) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("kiwave"));
            }
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    private <T extends GeoAnimatable> PlayState tailPredicate(AnimationState<T> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("tail"));
    }
}