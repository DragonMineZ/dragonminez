package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;

public class SagaOzaruVegetaEntity extends DBSagasEntity{

    private int kiBlastCooldown = 0;
    private int roarCooldown = 0;
    private int castTimer = 0;

    public SagaOzaruVegetaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.roarCooldown = 400;
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

        if (!this.level().isClientSide) {

            if (!this.isCasting()) {
                if (this.kiBlastCooldown > 0) this.kiBlastCooldown--;
                if (this.roarCooldown > 0) this.roarCooldown--;
            }

            LivingEntity target = this.getTarget();
            double distSqr = (target != null) ? this.distanceToSqr(target) : 0;

            if (target != null && target.isAlive() && !this.isCasting()) {

                // Jugador cerca a menos de 15 bloques
                if (this.roarCooldown <= 0 && distSqr < (15.0 * 15.0)) {
                    startRoar();
                }
                // Jugador a mas de 15 bloques y no hay cooldown
                else if (this.kiBlastCooldown <= 0 && distSqr > (15.0 * 15.0)) {
                    startKiBlast();
                }
            }
            if (this.isCasting()) {
                this.castTimer++;
                int skill = getSkillType();

                // KiBlast
                if (skill == 1) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.5, 0.5)); // Freno suave
                    if (target != null && target.isAlive()) {
                        if (this.castTimer >= 50) {
                            performKiBlastAttack(target);
                            stopCasting();
                        }
                    } else {
                        stopCasting();
                    }
                }
                else if (skill == 2) {
                    // Se detiene para tirar la skill
                    this.setDeltaMovement(0, this.getDeltaMovement().y, 0);

                    // Particulas dentro del personaje
                    if (this.castTimer < 40) {
                        spawnInwardParticles();
                    }
                    // explosion
                    else if (this.castTimer == 40) {
                        performRoarDamage();
                        spawnExplosionParticles();
                    }
                    // Saca las particulas
                    else if (this.castTimer > 40 && this.castTimer < 80) {
                        spawnOutwardParticles();
                    }
                    // Termina el cast del rugido
                    if (this.castTimer >= 80) {
                        stopCasting();
                    }
                }
            }
        }
    }
    private void startKiBlast() {
        this.setCasting(true);
        this.setSkillType(1);
        this.castTimer = 0;

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        this.getNavigation().stop();
        this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
    }

    private void startRoar() {
        this.setCasting(true);
        this.setSkillType(2);
        this.castTimer = 0;

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        this.getNavigation().stop();
        this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
    }

    private void stopCasting() {
        int currentSkill = getSkillType();

        this.setCasting(false);
        this.setSkillType(0);
        this.castTimer = 0;

        if (currentSkill == 1) {
            this.kiBlastCooldown = 10 * 20;
        } else if (currentSkill == 2) {
            this.roarCooldown = 25 * 20; //rugido
        }

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25D);
    }

    private void performKiBlastAttack(LivingEntity target) {
        KiBlastEntity kiBlast = new KiBlastEntity(this.level(), this);

        double sx = this.getX();
        double sy = this.getY() + 1.0D;
        double sz = this.getZ();

        kiBlast.setPos(sx, sy, sz);
        kiBlast.setColors(0xEE9EFF, 0xDD3DFF);
        kiBlast.setSize(8.5f);
        kiBlast.setKiDamage(20.0f);
        kiBlast.setOwner(this);

        double tx = target.getX() - sx;
        double ty = (target.getY() + target.getEyeHeight() * 0.5D) - sy;
        double tz = target.getZ() - sz;

        kiBlast.shoot(tx, ty, tz, 0.98F, 1.0F);

        this.level().addFreshEntity(kiBlast);
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
        double range = 25.0D;
        AABB area = this.getBoundingBox().inflate(range, 8.0D, range);
        List<LivingEntity> victims = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity victim : victims) {
            if (victim != this) {
                victim.hurt(this.damageSources().mobAttack(this), 20.0F); //damage rugido
                // Empujar
                double dx = victim.getX() - this.getX();
                double dz = victim.getZ() - this.getZ();
                victim.knockback(1.2F, -dx, -dz);
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
            if (skill == 1) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay("kiwave"));
                return PlayState.CONTINUE;
            }
            else if (skill == 2) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay("roar"));
                return PlayState.CONTINUE;
            }
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    private <T extends GeoAnimatable> PlayState tailPredicate(AnimationState<T> event) {

        event.getController().setAnimation(RawAnimation.begin().thenLoop("tail"));

        return PlayState.CONTINUE;
    }

}
