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
import software.bernie.geckolib.core.object.PlayState;

public class SagaA17Entity extends DBSagasEntity {

    private static final int SKILL_KI_DISC = 1;
    private static final int SKILL_BARRIER = 2;

    private int discCooldown = 0;
    private int barrierCooldown = 0;

    public SagaA17Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        if (this instanceof IBattlePower bp) {
            bp.setBattlePower(700000000);
        }
    }

    @Override
    public void stopCasting() {
        int skill = getSkillType();

        if (skill == SKILL_KI_DISC) {
            this.discCooldown = 8 * 20;
        }
        else if (skill == SKILL_BARRIER) {
            this.barrierCooldown = 20 * 20;
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

            if (skill == SKILL_KI_DISC) {
                return event.setAndContinue(ANIM_KIWAVE);
            }
            else if (skill == SKILL_BARRIER) {
                return event.setAndContinue(ANIM_KI_BARRIER);
            }
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}