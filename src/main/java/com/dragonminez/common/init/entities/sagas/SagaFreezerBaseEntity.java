package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.IBattlePower;
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

    private static final int SKILL_LASER_COMBO = 1;
    private static final int SKILL_DEATH_BALL = 2;

    private int kiLaserCooldown = 0;
    private int kiBlastCooldown = 0;

    public SagaFreezerBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
		if (this instanceof IBattlePower bp) {
			if (this.getName().toString().contains("fp")) {
				bp.setBattlePower(120000000);
			} else {
				bp.setBattlePower(60000000);
			}
		}
    }
    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();

        handleCommonCombatMovement(target, this.isCasting(), true);

        if (!this.level().isClientSide) {
            if (this.kiLaserCooldown > 0) this.kiLaserCooldown--;
            if (this.kiBlastCooldown > 0) this.kiBlastCooldown--;

            if (target != null && target.isAlive() && !this.isCasting()) {

                if (this.teleportCooldown <= 0) {
                    performTeleport(target);
                    return;
                }

                double distSqr = this.distanceToSqr(target);

                if (distSqr > 120.0D && this.kiBlastCooldown <= 0) {
                    startCasting(SKILL_DEATH_BALL);
                }
                else if (distSqr > 100.0D && this.kiLaserCooldown <= 0) {
                    startCasting(SKILL_LASER_COMBO);
                }
            }

            if (this.isCasting()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.5, 0.5));

                if (target != null && target.isAlive()) {
                    this.castTimer++;

                    int currentSkill = getSkillType();

                    if (currentSkill == SKILL_LASER_COMBO) {
                        if (this.castTimer == 20 || this.castTimer == 40 || this.castTimer == 60) {
                            shootGenericKiLaser(
                                    target,
                                    2.3F,
                                    0xBA1616,
                                    0x850707
                            );
                        }
                        if (this.castTimer >= 60) {
                            stopCasting();
                        }
                    }
                    else if (currentSkill == SKILL_DEATH_BALL) {
                        if (this.castTimer >= 20) {
                            shootGenericKiBlast(
                                    target,
                                    2.5F,
                                    0x8A2FCC,
                                    0x5D1294,
                                    1.5F
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

        if (usedSkill == SKILL_LASER_COMBO) {
            this.kiLaserCooldown = 10 * 20;
        } else if (usedSkill == SKILL_DEATH_BALL) {
            this.kiBlastCooldown = 20 * 20;
        }

        super.stopCasting();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<>(this, "skill_controller", 0, this::skillPredicate));
        controllers.add(new AnimationController<>(this, "tail", 0, this::tailPredicate));
    }

    private <T extends GeoAnimatable> PlayState tailPredicate(AnimationState<T> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("tail"));
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isCasting()) {
            int currentSkill = getSkillType();

            if (currentSkill == SKILL_LASER_COMBO) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("kilaser"));
            } else if (currentSkill == SKILL_DEATH_BALL) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("kiball"));
            }
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}