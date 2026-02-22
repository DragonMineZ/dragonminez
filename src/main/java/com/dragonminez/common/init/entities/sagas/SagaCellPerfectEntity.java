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

public class SagaCellPerfectEntity extends DBSagasEntity {

    private static final int SKILL_KAMEHA = 1;
    private static final int SKILL_KILASER = 2;

    private int kamehaCooldown = 0;
    private int kilaserCooldown = 0;

    public SagaCellPerfectEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.evade(true, 20);
        this.setFlySpeed(0.6D);
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

            if (target != null && target.isAlive() && !this.isCasting()) {
                double distSqr = this.distanceToSqr(target);

                if (this.teleportCooldown <= 0 && distSqr > 256.0D) {
                    performTeleport(target);
                    return;
                }

                if (this.kilaserCooldown <= 0 && distSqr > 25.0D) {
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
                            shootGenericKiLaser(
                                    target,
                                    2.5F,
                                    0xE040FB,
                                    0xAA00FF
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

        if (usedSkill == SKILL_KAMEHA) {
            this.kamehaCooldown = 20 * 20;
        }
        else if (usedSkill == SKILL_KILASER) {
            this.kilaserCooldown = 6 * 20;
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

            if (currentSkill == SKILL_KAMEHA) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("kiwave"));
            }
            else if (currentSkill == SKILL_KILASER) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("kilaser"));
            }

            return PlayState.CONTINUE;
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}