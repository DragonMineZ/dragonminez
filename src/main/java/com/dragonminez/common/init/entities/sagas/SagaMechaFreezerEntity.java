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

public class SagaMechaFreezerEntity extends DBSagasEntity {

    private static final int SKILL_LASER_COMBO = 1;
    private static final int SKILL_DEATH_BALL = 2;

    private int kiLaserCooldown = 0;
    private int kiBlastCooldown = 0;

    public SagaMechaFreezerEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        if (this instanceof IBattlePower bp) {
            bp.setBattlePower(140000000);
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
        return event.setAndContinue(ANIM_TAIL);
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isCasting()) {
            int currentSkill = getSkillType();

            if (currentSkill == SKILL_LASER_COMBO) {

            } else if (currentSkill == SKILL_DEATH_BALL) {
                return event.setAndContinue(ANIM_KIBALL);
            }
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}