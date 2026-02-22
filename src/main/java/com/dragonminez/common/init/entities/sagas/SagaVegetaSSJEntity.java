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

public class SagaVegetaSSJEntity extends DBSagasEntity{

    private static final int SKILL_FINALFLASH = 1;
    private int finalflashCooldown = 0;

    public SagaVegetaSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.useCombo1(true, 20*20);
        this.evade(true, 60);
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(1875000000);
		}
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();

        handleCommonCombatMovement(target, this.isCasting(), true);

        if (!this.level().isClientSide) {
            if (this.finalflashCooldown > 0) this.finalflashCooldown--;

            if (target != null && target.isAlive() && !this.isCasting()) {
                double distSqr = this.distanceToSqr(target);

                if (this.teleportCooldown <= 0) {
                    performTeleport(target);
                    return;
                }

                if (this.finalflashCooldown <= 0 && distSqr > 100.0D) {
                    startCasting(SKILL_FINALFLASH);
                }
            }

            if (this.isCasting()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.5, 0.5));

                if (target != null && target.isAlive()) {
                    this.castTimer++;

                    int currentSkill = getSkillType();

                    if (currentSkill == SKILL_FINALFLASH) {
                        if (this.castTimer >= 50) {
                            shootGenericKiWave(target, 2.5F, 0xFFF282, 0xFFEA3B, 2.0f);
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

        if (usedSkill == SKILL_FINALFLASH) {
            this.finalflashCooldown = 10 * 20;
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
            int currentSkill = getSkillType();

            if (currentSkill == SKILL_FINALFLASH) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("kiwave"));
            }

            return PlayState.CONTINUE;
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}