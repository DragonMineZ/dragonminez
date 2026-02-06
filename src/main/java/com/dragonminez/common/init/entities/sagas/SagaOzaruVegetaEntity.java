package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;

public class SagaOzaruVegetaEntity extends DBSagasEntity{

    private static final int SKILL_KIBLAST = 1;
    private static final int SKILL_ROAR = 2;

    private int kiBlastCooldown = 0;
    private int roarCooldown = 0;

    public SagaOzaruVegetaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.roarCooldown = 400;
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(180000);
		}
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();

        handleCommonCombatMovement(target, this.isCasting(), false);

        if (!this.level().isClientSide) {
            if (!this.isCasting()) {
                if (this.kiBlastCooldown > 0) this.kiBlastCooldown--;
                if (this.roarCooldown > 0) this.roarCooldown--;
            }

            if (target != null && target.isAlive() && !this.isCasting()) {
                double distSqr = this.distanceToSqr(target);

                if (this.roarCooldown <= 0 && distSqr < 225.0D) {
                    startCasting(SKILL_ROAR);
                }
                else if (this.kiBlastCooldown <= 0 && distSqr > 225.0D) {
                    startCasting(SKILL_KIBLAST);
                }
            }

            if (this.isCasting()) {
                this.castTimer++;
                int skill = getSkillType();

                if (skill == SKILL_KIBLAST) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.5, 0.5));

                    if (target != null && target.isAlive()) {
                        if (this.castTimer >= 50) {
                            shootGenericKiBlast(target, 8.5F, 0xEE9EFF, 0xDD3DFF, 0.8f);
                            stopCasting();
                        }
                    } else {
                        stopCasting();
                    }
                }
                else if (skill == SKILL_ROAR) {
                    this.setDeltaMovement(0, this.getDeltaMovement().y, 0);

                    if (this.castTimer < 40) {
                        spawnInwardParticles();
                    }
                    else if (this.castTimer == 40) {
                        performRoarDamage();
                        spawnExplosionParticles();
                        this.playSound(MainSounds.VEGETA_OOZARU_GROWL.get(), 5.0F, 1.0F);
                    }
                    else if (this.castTimer > 40 && this.castTimer < 80) {
                        spawnOutwardParticles();
                    }

                    if (this.castTimer >= 80) {
                        stopCasting();
                    }
                }
            }
        }
    }

    @Override
    public void stopCasting() {
        int currentSkill = getSkillType();

        if (currentSkill == SKILL_KIBLAST) {
            this.kiBlastCooldown = 10 * 20;
        } else if (currentSkill == SKILL_ROAR) {
            this.roarCooldown = 25 * 20;
        }

        super.stopCasting();
    }

    private void spawnInwardParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            double radius = 14.0D;
            for (int i = 0; i < 5; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2;
                double xOffset = Math.cos(angle) * radius;
                double zOffset = Math.sin(angle) * radius;
                double yOffset = (this.random.nextDouble() - 0.5) * 6.0;

                double pX = this.getX() + xOffset;
                double pY = this.getY() + 6.0D + yOffset;
                double pZ = this.getZ() + zOffset;

                double vX = (this.getX() - pX) * 0.15;
                double vY = ((this.getY() + 6.0D) - pY) * 0.15;
                double vZ = (this.getZ() - pZ) * 0.15;

                serverLevel.sendParticles(ParticleTypes.CLOUD, pX, pY, pZ, 0, vX, vY, vZ, 1.0);
            }
        }
    }

    private void performRoarDamage() {
        double range = this.getRoarRange();
        AABB area = this.getBoundingBox().inflate(range, 8.0D, range);
        List<LivingEntity> victims = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity victim : victims) {
            if (victim != this) {
                victim.hurt(this.damageSources().mobAttack(this), (float) this.getRoarDamage());
                double dx = victim.getX() - this.getX();
                double dz = victim.getZ() - this.getZ();
                victim.knockback(0.8F, -dx, -dz);
            }
        }
    }

    private void spawnExplosionParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 6.0D, this.getZ(), 5, 1.0, 1.0, 1.0, 0);
        }
    }

    private void spawnOutwardParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 16; i++) {
                double vX = (this.random.nextDouble() - 0.5) * 3.0;
                double vY = (this.random.nextDouble() - 0.5) * 2.0;
                double vZ = (this.random.nextDouble() - 0.5) * 3.0;
                serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 6.0D, this.getZ(), 0, vX, vY, vZ, 0.5);
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "skill_controller", 0, this::skillPredicate));
        controllers.add(new AnimationController<>(this, "tail", 0, this::tailPredicate));
        super.registerControllers(controllers);
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isCasting()) {
            int skill = getSkillType();

            if (skill == SKILL_KIBLAST) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("kiwave"));
            }
            else if (skill == SKILL_ROAR) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("roar"));
            }
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    private <T extends GeoAnimatable> PlayState tailPredicate(AnimationState<T> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("tail"));
    }
}