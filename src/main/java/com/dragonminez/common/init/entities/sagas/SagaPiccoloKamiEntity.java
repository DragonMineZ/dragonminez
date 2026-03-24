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

public class SagaPiccoloKamiEntity extends DBSagasEntity {

    private static final int SKILL_VOLLEY = 1;

    private int kiVolleyCooldown = 0;

    public SagaPiccoloKamiEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setKiCharge(true);
		if (this instanceof IBattlePower bp) {
            bp.setBattlePower(705000000);
		}
    }

    @Override
    public void stopCasting() {
        int usedSkill = getSkillType();

        if (usedSkill == SKILL_VOLLEY) {
            this.kiVolleyCooldown = 10 * 20;
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

            if (skill == SKILL_VOLLEY) {
                return event.setAndContinue(ANIM_KIWAVE);
            }
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}