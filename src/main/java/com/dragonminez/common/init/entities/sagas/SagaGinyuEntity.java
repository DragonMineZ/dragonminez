package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
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

public class SagaGinyuEntity extends DBSagasEntity{

    private static final int SKILL_MILKY_CANNON = 1;

    private int kiBlastCooldown = 0;

    public SagaGinyuEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(120000);
		}
    }

    @Override
    public void stopCasting() {
        if (getSkillType() == SKILL_MILKY_CANNON) {
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

            if (skill == SKILL_MILKY_CANNON) {
                return event.setAndContinue(ANIM_KIWAVE);
            }
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}
