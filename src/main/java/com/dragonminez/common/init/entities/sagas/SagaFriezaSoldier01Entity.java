package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.ki.KiBlastEntity;
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
import software.bernie.geckolib.core.object.PlayState;

public class SagaFriezaSoldier01Entity extends DBSagasEntity{

    private static final int SKILL_KIBLAST = 1;

    private int kiBlastCooldown = 0;

    public SagaFriezaSoldier01Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.setFlySpeed(0.35D);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    public void stopCasting() {
        if (getSkillType() == SKILL_KIBLAST) {
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

            if (skill == SKILL_KIBLAST) {
                return event.setAndContinue(ANIM_KIBLAST);
            }
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}
