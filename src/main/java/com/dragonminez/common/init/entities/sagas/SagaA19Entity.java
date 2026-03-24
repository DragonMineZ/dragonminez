package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

public class SagaA19Entity extends DBSagasEntity {

	private static final int SKILL_EYE_LASER = 1;
	private static final int SKILL_ENERGY_DRAIN = 2; // El "Grab"

	private int laserCooldown = 0;
	private int drainCooldown = 0;

	public SagaA19Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(400000000);
		}
	}

	@Override
	public void stopCasting() {
		int usedSkill = getSkillType();

		if (usedSkill == SKILL_EYE_LASER) {
			this.laserCooldown = 8 * 20;
		} else if (usedSkill == SKILL_ENERGY_DRAIN) {
			this.drainCooldown = 15 * 20;
		}

		super.stopCasting();
	}

	@Override
	public boolean hurt(DamageSource pSource, float pAmount) {
		if (this.isCasting() && getSkillType() == SKILL_ENERGY_DRAIN) {
			return false;
		}
		return super.hurt(pSource, pAmount);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		super.registerControllers(controllers);
		controllers.add(new AnimationController<>(this, "skill_controller", 0, this::skillPredicate));
	}

	private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
		if (this.isCasting()) {
			int currentSkill = getSkillType();

			if (currentSkill == SKILL_EYE_LASER) {
			} else if (currentSkill == SKILL_ENERGY_DRAIN) {
				return event.setAndContinue(ANIM_GRAB);
			}
		}
		event.getController().forceAnimationReset();
		return PlayState.STOP;
	}
}