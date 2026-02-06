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

public class SagaA17Entity extends DBSagasEntity{

    private static final int SKILL_MILKY_CANNON = 1;

    private int kiBlastCooldown = 0;

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
            if (this.kiBlastCooldown > 0) this.kiBlastCooldown--;

            if (target != null && target.isAlive() && !this.isCasting()) {
                double distSqr = this.distanceToSqr(target);

                if (this.teleportCooldown <= 0 && distSqr > 200.0D) {
                    performTeleport(target);
                    return;
                }

                if (this.kiBlastCooldown <= 0 && distSqr > 100.0D) {
                    startCasting(SKILL_MILKY_CANNON);
                }
            }

            if (this.isCasting()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.5, 0.5));

                if (target != null && target.isAlive()) {
                    this.castTimer++;

                    if (getSkillType() == SKILL_MILKY_CANNON) {
                        if (this.castTimer >= 50) {
                            shootGenericKiBlast(
                                    target,
                                    2.1F,
                                    0xF3A3FF,
                                    0xEB57FF,
                                    2.0f
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
        if (getSkillType() == SKILL_MILKY_CANNON) {
            this.kiBlastCooldown = 10 * 20;
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

            if (skill == SKILL_MILKY_CANNON) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("kiwave"));
            }
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}
