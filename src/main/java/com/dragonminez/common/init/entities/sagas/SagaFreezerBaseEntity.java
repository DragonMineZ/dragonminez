package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import net.minecraft.sounds.SoundEvents;
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

public class SagaFreezerBaseEntity extends DBSagasEntity {

    private static final int SKILL_LASER_COMBO = 1;
    private static final int SKILL_DEATH_BALL = 2;

    private int kiLaserCooldown = 0;
    private int kiBlastCooldown = 0;

    public SagaFreezerBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        if (this instanceof IBattlePower bp) {
            if (this.getName().toString().contains("fp")) {
                bp.setBattlePower(120000000);
            } else {
                bp.setBattlePower(60000000);
            }
        }
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

        if (!this.isCasting()) {
            event.getController().forceAnimationReset();
        }
        return PlayState.STOP;
    }
}