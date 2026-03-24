package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.IBattlePower;
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
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

public class SagaZarbonEntity extends DBSagasEntity{

    private static final int SKILL_KIBLAST = 1;

    private int kiBlastCooldown = 0;
    private int transformTick = 0;

    public SagaZarbonEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(23000);
		}
    }


    @Override
    protected boolean shouldTriggerTransformationOnDeath() {
        return true;
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
        controllers.add(new AnimationController<>(this, "skill_controller", 3, this::skillPredicate));
        super.registerControllers(controllers);
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isTransforming()) {
            return event.setAndContinue(ANIM_TRANSFORM);
        }

        if (this.isCasting()) {
            int skill = getSkillType();
            if (skill == SKILL_KIBLAST) {
                return event.setAndContinue(ANIM_KIWAVE);
            }
        }

        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}