package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiExplosionEntity;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
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

public class SagaRecoomeEntity extends DBSagasEntity {

    private static final int SKILL_EXPLOSION = 1;
    private int skillCooldown = 0;


    public SagaRecoomeEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(40000);
		}
    }
    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();

        handleCommonCombatMovement(target, this.isCasting(), true);

        if (!this.level().isClientSide) {
            if (this.skillCooldown > 0) this.skillCooldown--;

            if (target != null && target.isAlive() && !this.isCasting()) {
                double distSqr = this.distanceToSqr(target);

                if (this.skillCooldown <= 0 && distSqr < 225.0D) {
                    startCasting(SKILL_EXPLOSION);
                }
            }

            if (this.isCasting()) {
                this.setDeltaMovement(0, 0, 0);

                if (getSkillType() == SKILL_EXPLOSION) {
                    this.castTimer++;

                    if (this.castTimer == 1) {
                        performKiExplosion(
                                this.getKiBlastDamage(),
                                0xE58FFF,
                                0xFF00FF
                        );
                    }

                    if (this.castTimer >= KiExplosionEntity.DURATION) {
                        stopCasting();
                    }
                }
            }
        }
    }

    @Override
    public void stopCasting() {
        if (getSkillType() == SKILL_EXPLOSION) {
            this.skillCooldown = 15 * 20;
        }
        super.stopCasting();
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isCasting()) return false;
        return super.hurt(pSource, pAmount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "skill_controller", 3, this::skillPredicate));
        super.registerControllers(controllers);
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isCasting()) {
            if (getSkillType() == SKILL_EXPLOSION) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("kiexplosion"));
            }
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}