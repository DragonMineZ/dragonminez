package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.sounds.SoundEvents;
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

public class SagaCellImperfectEntity extends DBSagasEntity {

    private static final int SKILL_KAMEHA = 1;
    private static final int SKILL_ABSORBER = 2;

    private int kamehaCooldown = 0;
    private int absorberCooldown = 0;

    public SagaCellImperfectEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        if (this instanceof IBattlePower bp) {
            bp.setBattlePower(750000000);
        }
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();

        boolean isAbsorbing = this.isCasting() && this.getSkillType() == SKILL_ABSORBER;
        handleCommonCombatMovement(target, this.isCasting() && !isAbsorbing, true);

        if (!this.level().isClientSide) {
            if (this.kamehaCooldown > 0) this.kamehaCooldown--;
            if (this.absorberCooldown > 0) this.absorberCooldown--;

            if (target != null && target.isAlive() && !this.isCasting()) {
                double distSqr = this.distanceToSqr(target);

                // Iniciar Absorción
                if (this.absorberCooldown <= 0 && distSqr < 9.0D) {
                    startCasting(SKILL_ABSORBER);
                }
                // Iniciar Kamehameha
                else if (this.kamehaCooldown <= 0 && distSqr > 100.0D) {
                    startCasting(SKILL_KAMEHA);
                }
                else if (this.teleportCooldown <= 0 && distSqr > 64.0D) {
                    performTeleport(target);
                }
            }

            if (this.isCasting()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.5, 0.5));

                if (target != null && target.isAlive()) {
                    this.castTimer++;
                    int currentSkill = getSkillType();

                    if (currentSkill == SKILL_KAMEHA) {
                        this.lookAt(target, 30.0F, 30.0F);
                        if (this.castTimer >= 50) {
                            shootGenericKiWave(target, 2.0F, 0xB0FDFF, 0x40FAFF, 1.5f);
                            stopCasting();
                        }
                    }

                    else if (currentSkill == SKILL_ABSORBER) {

                        performTeleportBehind(target);

                        this.lookAt(target, 360.0F, 360.0F);

                        if (this.castTimer % 5 == 0) {
                            drainLifeAndEnergy(target);
                        }

                        if (this.castTimer >= 120) {
                            stopCasting();
                        }
                    }

                } else {
                    stopCasting();
                }
            }
        }
    }

    private void drainLifeAndEnergy(LivingEntity target) {
        float damageToDeal = getKiBlastDamage() * 0.5F;
        if (damageToDeal < 1.0F) damageToDeal = 1.0F;

        target.hurt(this.damageSources().magic(), damageToDeal);

        if (target instanceof Player player) {
            player.getCapability(StatsCapability.INSTANCE).ifPresent(stats -> {
                int maxEnergy = stats.getMaxEnergy();

                int drainPercentage = (int) (maxEnergy * 0.05);
                if (drainPercentage < 1) drainPercentage = 1;

                stats.getResources().removeEnergy(drainPercentage);
            });
        }

        this.heal(damageToDeal);
    }

    private void performTeleportBehind(LivingEntity target) {
        Vec3 look = target.getLookAngle().normalize();

        double distance = 0.3D;

        double destX = target.getX() - (look.x * distance);
        double destZ = target.getZ() - (look.z * distance);

        double destY = target.getY();

        this.setPos(destX, destY, destZ);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isCasting() && this.getSkillType() == SKILL_ABSORBER) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void stopCasting() {
        int usedSkill = getSkillType();

        if (usedSkill == SKILL_KAMEHA) {
            this.kamehaCooldown = 10 * 20;
        }
        else if (usedSkill == SKILL_ABSORBER) {
            this.absorberCooldown = 20 * 20;
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
            int currentSkill = getSkillType();

            if (currentSkill == SKILL_KAMEHA) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("kiwave"));
            }
            else if (currentSkill == SKILL_ABSORBER) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("absorb"));
            }

            return PlayState.CONTINUE;
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    private <T extends GeoAnimatable> PlayState tailPredicate(AnimationState<T> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("tail"));
    }
}