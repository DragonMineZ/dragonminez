package com.dragonminez.common.init.entities;

import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

import java.util.UUID;

public class ShadowDummyEntity extends DBSagasEntity {

	private static final int Bolita = 1;
	private int kiBlastCooldown = 0;
	private UUID ownerUUID;

	public ShadowDummyEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(700000000);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 300.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.25D)
				.add(Attributes.ATTACK_DAMAGE, 15.0D)
				.add(Attributes.FOLLOW_RANGE, 64.0D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
				.add(Attributes.ARMOR, 10.0D);
	}

	public void setOwner(LivingEntity owner) {
		this.ownerUUID = owner.getUUID();
	}

	public void copyStatsFromPlayer(ServerPlayer player) {
		this.setOwner(player);

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			double playerMaxHP = player.getAttributeValue(Attributes.MAX_HEALTH);
			if (this.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
				this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(playerMaxHP);
				this.setHealth((float) ((float) playerMaxHP * 2 + data.getDefense() * 0.75f));
			}
			if (this instanceof IBattlePower bpEntity) bpEntity.setBattlePower(data.getBattlePower());
			double playerDmg = data.getMeleeDamage() * 0.5;
			if (this.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE))
				this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(playerDmg);
			float calculatedKiDamage = (float) ((float) data.getKiDamage() * 0.5);
			this.setKiBlastDamage(calculatedKiDamage);
		});
		this.getPersistentData().putBoolean("dmz_stats_configured", true);
	}


	@Override
	public void stopCasting() {
		if (getSkillType() == Bolita) {
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

			if (skill == Bolita) {
				return event.setAndContinue(ANIM_KIWAVE);
			}
		}
		event.getController().forceAnimationReset();
		return PlayState.STOP;
	}
}
