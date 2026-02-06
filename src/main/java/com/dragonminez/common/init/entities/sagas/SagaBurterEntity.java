package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.SPBlueHurricaneEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
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

public class SagaBurterEntity extends DBSagasEntity {

    private static final int SKILL_BLUE_HURRICANE = 1;
    private int hurricaneCooldown = 0;

    public SagaBurterEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        if (this instanceof IBattlePower bp) {
            bp.setBattlePower(40000);
        }
        this.setFlySpeed(0.55D);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();

        boolean shouldStopMoving = this.isCasting() && getSkillType() != SKILL_BLUE_HURRICANE;
        handleCommonCombatMovement(target, shouldStopMoving, true);

        if (!this.level().isClientSide) {
            if (this.hurricaneCooldown > 0) this.hurricaneCooldown--;

            if (target != null && target.isAlive() && !this.isCasting()) {
                double distSqr = this.distanceToSqr(target);

                if (this.hurricaneCooldown <= 0 && distSqr < 225.0D) {
                    startCasting(SKILL_BLUE_HURRICANE);

                    this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.30D);
                }
            }

            if (this.isCasting()) {
                if (getSkillType() == SKILL_BLUE_HURRICANE) {
                    this.castTimer++;

                    if (this.castTimer == 1) {
                        performBlueHurricaneEffect();
                    }

                    if (target != null) {
                        this.lookAt(target, 30.0F, 30.0F);
                    }

                    if (this.castTimer >= 140) {
                        stopCasting();
                    }
                }
            }
        }
    }

    @Override
    public void moveTowardsTargetInAir(LivingEntity target) {
        if (this.isCasting() && getSkillType() != SKILL_BLUE_HURRICANE) return;

        double flyspeed = this.getFlySpeed();
        double dx = target.getX() - this.getX();
        double dy = (target.getY() + 1.0D) - this.getY();
        double dz = target.getZ() - this.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 1.0) return;
        Vec3 movement = new Vec3(dx / distance * flyspeed, dy / distance * flyspeed, dz / distance * flyspeed);
        double gravityDrag = (dy < -0.5) ? -0.05D : -0.03D;
        this.setDeltaMovement(movement.add(0, gravityDrag, 0));
    }

    private void performBlueHurricaneEffect() {
        SPBlueHurricaneEntity hurricane = new SPBlueHurricaneEntity(this.level(), this);
        hurricane.setup(
                this,
                this.getKiBlastDamage(),
                3.0F,
                0.0f,
                0x0000FF,
                0x00FFFF
        );
        this.level().addFreshEntity(hurricane);
    }

    @Override
    public void stopCasting() {
        if (getSkillType() == SKILL_BLUE_HURRICANE) {
            this.hurricaneCooldown = 12 * 20;
        }
        super.stopCasting();
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isCasting() && getSkillType() == SKILL_BLUE_HURRICANE) {
            pAmount *= 0.5F;
        }
        return super.hurt(pSource, pAmount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "skill_controller", 3, this::skillPredicate));
        super.registerControllers(controllers);
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isCasting()) {
            if (getSkillType() == SKILL_BLUE_HURRICANE) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("kiwave"));
            }
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}