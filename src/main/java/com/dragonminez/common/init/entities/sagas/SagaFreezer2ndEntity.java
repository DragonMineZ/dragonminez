package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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

public class SagaFreezer2ndEntity extends DBSagasEntity{

    private static final int SKILL_GRAB = 1;

    private static final int GRAB_DURATION = 5 * 20;
    private static final double GRAB_RANGE_SQR = 2.5D * 2.5D;

    private int grabCooldown = 0;
    private int transformTick = 0;
    private LivingEntity heldTarget = null;


    public SagaFreezer2ndEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(1200000);
		}
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isTransforming()) {
            if (this.heldTarget != null) releaseTarget();

            this.transformTick++;
            if (handleTransformationLogic(this.transformTick, 100)) { // 100 ticks para Freezer
                DBSagasEntity newForm = (DBSagasEntity) MainEntities.SAGA_FREEZER_THIRD.get().create(this.level());
                finishTransformationSpawn(newForm);
            }
            return;
        }

        if (!this.level().isClientSide && this.getHealth() <= this.getMaxHealth() / 2.0F) {
            startTransformation();
            return;
        }

        LivingEntity target = this.getTarget();

        handleCommonCombatMovement(target, this.isCasting(), true);

        if (!this.level().isClientSide) {
            if (this.grabCooldown > 0) this.grabCooldown--;

            if (target != null && target.isAlive() && !this.isCasting()) {
                double distSqr = this.distanceToSqr(target);

                if (this.grabCooldown <= 0 && distSqr <= GRAB_RANGE_SQR) {
                    performGrabStart(target);
                }
            }

            if (this.isCasting()) {
                if (getSkillType() == SKILL_GRAB) {
                    tickHeldTarget();
                }
            }
        }
    }


    private void performGrabStart(LivingEntity target) {
        this.heldTarget = target;
        startCasting(SKILL_GRAB);
        this.setNoGravity(true);
    }

    private void tickHeldTarget() {
        if (this.heldTarget == null || !this.heldTarget.isAlive()) {
            stopCasting(); // Soltar
            return;
        }

        this.castTimer++;

        this.setDeltaMovement(0, 0, 0);
        this.setYBodyRot(this.getYRot());

        double holdHeight = this.getBbHeight() - 0.2D;
        Vec3 holdPos = this.position().add(0, holdHeight, 0);

        this.heldTarget.setPos(holdPos.x, holdPos.y, holdPos.z);
        this.heldTarget.setDeltaMovement(0, 0, 0);
        this.heldTarget.hurtMarked = true;

        this.heldTarget.setXRot(-90.0F);
        this.heldTarget.xRotO = -90.0F;
        this.heldTarget.setYRot(this.getYRot());
        this.heldTarget.setYHeadRot(this.getYRot());

        if (this.castTimer % 10 == 0) {
            float dmg = this.getKiBlastDamage();
            this.heldTarget.hurt(this.damageSources().mobAttack(this), dmg);
        }

        if (this.castTimer >= GRAB_DURATION) {
            stopCasting();
        }
    }

    private void releaseTarget() {
        if (this.heldTarget != null) {
            Vec3 vec = this.getLookAngle().scale(0.5).add(0, 0.5, 0);
            this.heldTarget.setDeltaMovement(vec);
        }
        this.heldTarget = null;
        this.setNoGravity(false);
    }

    @Override
    public void stopCasting() {
        releaseTarget();

        if (getSkillType() == SKILL_GRAB) {
            this.grabCooldown = 15 * 20;
        }
        super.stopCasting();
    }


    private void startTransformation() {
        this.setTransforming(true);
        this.playSound(MainSounds.KI_CHARGE_LOOP.get(), 1.0F, 1.2F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isCasting() && getSkillType() == SKILL_GRAB && this.heldTarget != null) {
            if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL)) {
                return super.hurt(source, amount);
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "skill_controller", 3, this::skillPredicate));
        controllers.add(new AnimationController<>(this, "tail", 0, this::tailPredicate));
        super.registerControllers(controllers);
    }

    private <T extends GeoAnimatable> PlayState tailPredicate(AnimationState<T> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("tail"));
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isTransforming()) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("transform"));
        }

        if (this.isCasting()) {
            if (getSkillType() == SKILL_GRAB) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("grab"));
            }
        }

        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}
