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
import software.bernie.geckolib.core.animation.RawAnimation;
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

		double playerMaxHP = player.getAttributeValue(Attributes.MAX_HEALTH);
		if (this.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
			this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(playerMaxHP);
			this.setHealth((float) playerMaxHP * 4);
		}

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (this instanceof IBattlePower bpEntity) bpEntity.setBattlePower(data.getBattlePower());
			double playerDmg = data.getMeleeDamage();
			if (this.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(playerDmg);
			float calculatedKiDamage = (float) data.getKiDamage();
			this.setKiBlastDamage(calculatedKiDamage);
			if (this.getAttributes().hasAttribute(Attributes.ARMOR)) this.getAttribute(Attributes.ARMOR).setBaseValue(data.getDefense());
		});
		this.getPersistentData().putBoolean("dmz_stats_configured", true);
	}

    @Override
    public void tick() {
        super.tick();
        LivingEntity target = this.getTarget();
        handleCommonCombatMovement(target, this.isCasting(), false);

		//if (ownerUUID == null) this.discard();

        if (this.level().isClientSide) {
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(
                        ParticleTypes.LARGE_SMOKE,
                        this.getRandomX(0.5D),
                        this.getRandomY(),
                        this.getRandomZ(0.5D),
                        0.0D, 0.05D, 0.0D
                );
            }
        }

		if (target != null && ownerUUID != null && target.getUUID().equals(ownerUUID)) {
			this.setTarget(null);
			return;
		}

        if (!this.level().isClientSide) {
            if (this.kiBlastCooldown > 0) this.kiBlastCooldown--;

            if (target != null && target.isAlive() && !this.isCasting()) {
                double distSqr = this.distanceToSqr(target);

                if (this.teleportCooldown <= 0 && distSqr > 200.0D) {
                    performTeleport(target);
                    return;
                }

                if (this.kiBlastCooldown <= 0 && distSqr > 100.0D) startCasting(Bolita);
            }

            if (this.isCasting()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.5, 0.5));

                if (target != null && target.isAlive()) {
                    this.castTimer++;

                    if (getSkillType() == Bolita) {
                        if (this.castTimer >= 50) {
                            shootGenericKiBlast(
                                    target,
                                    0.8F,
                                    0xFF3838,
                                    0x241111,
                                    0.8f
                            );
                            stopCasting();
                        }
                    }
                } else {
                    stopCasting();
                }
            }
        }
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
                return event.setAndContinue(RawAnimation.begin().thenPlay("kiwave"));
            }
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}
