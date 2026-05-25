package com.dragonminez.server.events.players.combat;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.util.ComboManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

public class DashHandler {

	public static void handleDash(ServerPlayer player, float xInput, float zInput, boolean isDoubleDash) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;
			if (player.hasEffect(MainEffects.STUN.get())) return;
			if (data.getStatus().isStrikeLocked()) return;

			if (ComboManager.canTeleport(player.getUUID())) {
				int targetId = ComboManager.getTeleportTarget(player.getUUID());
				Entity target = player.level().getEntity(targetId);

				if (target instanceof LivingEntity livingTarget) {
					Vec3 targetPos = livingTarget.position();
					Vec3 targetLook = livingTarget.getLookAngle();
					Vec3 teleportPos = targetPos.subtract(targetLook.scale(1.5));

					player.teleportTo(teleportPos.x, targetPos.y, teleportPos.z);
					player.setYRot(livingTarget.getYRot());
					player.setYHeadRot(livingTarget.getYRot());
					player.hurtMarked = true;
					player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.TP_SHORT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
					ComboManager.consumeTeleport(player.getUUID());
					return;
				}
			}

			long currentTime = System.currentTimeMillis();
			long lastHurtTime = data.getStatus().getLastHurtTime();
			int evasionWindow = ConfigManager.getCombatConfig().getPerfectEvasionWindowMs();
			boolean isEvasion = (currentTime - lastHurtTime) <= evasionWindow;
			boolean evasionActive = ConfigManager.getCombatConfig().getEnablePerfectEvasion();
			int recentAttackerId = player.getPersistentData().getInt(CombatEvent.DMZ_LAST_ATTACKER_ID_TAG);
			LivingEntity recentAttacker = player.level().getEntity(recentAttackerId) instanceof LivingEntity living ? living : null;
			int vanishWindow = Math.max(50, evasionWindow / 2);
			boolean isVanish = recentAttacker != null && (currentTime - lastHurtTime) <= vanishWindow;

			if (isEvasion && evasionActive) {
				float baseDrain = ConfigManager.getCombatConfig().getBaselineFormDrain();
				int kiCost = (int) Math.ceil(baseDrain * 0.65);

				DMZEvent.PlayerEvasionEvent evasionEvent = new DMZEvent.PlayerEvasionEvent(player, recentAttacker, 0, kiCost);
				MinecraftForge.EVENT_BUS.post(evasionEvent);

				if (evasionEvent.isCanceled()) return;

				kiCost = evasionEvent.getKiCost();
				float currentEnergy = data.getResources().getCurrentEnergy();

				if (currentEnergy >= kiCost) {
					data.getResources().addEnergy(-kiCost);
					data.getStatus().setLastHurtTime(0);

					if (recentAttacker != null) {
						if (isVanish) {
							Vec3 attackerLook = recentAttacker.getLookAngle();
							Vec3 behind = recentAttacker.position().subtract(attackerLook.scale(1.3));
							player.teleportTo(behind.x, recentAttacker.getY(), behind.z);
							player.setYRot(recentAttacker.getYRot());
							player.setYHeadRot(recentAttacker.getYRot());
							player.hurtMarked = true;

							recentAttacker.knockback(1.1D, recentAttacker.getX() - player.getX(), recentAttacker.getZ() - player.getZ());
							float vanishDamage = (float) Math.max(1.0, data.getMeleeDamage() * 0.2);
							recentAttacker.hurt(player.damageSources().playerAttack(player), vanishDamage);
						} else {
							recentAttacker.knockback(1.35D, recentAttacker.getX() - player.getX(), recentAttacker.getZ() - player.getZ());
							recentAttacker.setDeltaMovement(recentAttacker.getDeltaMovement().scale(0.6));
						}
					}

					player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
							isVanish ? MainSounds.TP_SHORT.get() : MainSounds.EVASION1.get(),
							SoundSource.PLAYERS, 1.0F, isVanish ? 1.0F : 1.2F + player.getRandom().nextFloat() * 0.2F);
					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.EVASION, 0), player);
					NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
					return;
				}
			}

			boolean isFlyingSkillActive = data.getSkills().isSkillActive("fly");
			boolean canDoubleDash = !isFlyingSkillActive && isDoubleDash && data.getCooldowns().hasCooldown(Cooldowns.DASH_ACTIVE) && !data.getCooldowns().hasCooldown(Cooldowns.DOUBLEDASH_CD);
			boolean canNormalDash = !isDoubleDash && !data.getCooldowns().hasCooldown(Cooldowns.DASH_CD);

			if (!canDoubleDash && !canNormalDash) return;

			double baseDistance = 4.0;
			double speedMultiplier = player.getAttributeValue(Attributes.MOVEMENT_SPEED) / 0.1;
			double distance = baseDistance * speedMultiplier;

			int baseDrain = ConfigManager.getCombatConfig().getBaselineFormDrain();
			int kiCost;
			DMZEvent.PlayerDashEvent.DashType dashType;

			if (canDoubleDash) {
				distance = distance * 1.5;
				kiCost = (int) Math.ceil(baseDrain * 0.25);
				dashType = DMZEvent.PlayerDashEvent.DashType.DOUBLE;
			} else {
				kiCost = (int) Math.ceil(baseDrain * 0.12);
				dashType = DMZEvent.PlayerDashEvent.DashType.NORMAL;
			}

			DMZEvent.PlayerDashEvent dashEvent = new DMZEvent.PlayerDashEvent(player, dashType, distance, kiCost);
			MinecraftForge.EVENT_BUS.post(dashEvent);
			if (dashEvent.isCanceled()) return;

			distance = dashEvent.getDistance();
			kiCost = dashEvent.getKiCost();
			float currentEnergy = data.getResources().getCurrentEnergy();

			if (player.isCreative() || player.isSpectator()) kiCost = 0;
			if (currentEnergy < kiCost) return;
			if (player.getFoodData().getFoodLevel() <= 3) return;
			data.getResources().addEnergy(-kiCost);

			Vec3 forward = Vec3.directionFromRotation(0, player.getYRot()).normalize();
			Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
			Vec3 direction = forward.scale(zInput).add(right.scale(xInput)).normalize();

			Vec3 currentMotion = player.getDeltaMovement();
			Vec3 velocity;
			if (isFlyingSkillActive) {
				double trackedFlightSpeed = player.getPersistentData().getDouble("dmz_server_speed");
				double sampledSpeed = Math.max(currentMotion.length(), trackedFlightSpeed);
				double flightImpulse = Math.min(3.8, Math.max(0.55, (distance * 0.30) + (sampledSpeed * 2.5)));
				velocity = direction.scale(flightImpulse);
				Vec3 horizontalBoost = new Vec3(currentMotion.x + velocity.x, 0.0, currentMotion.z + velocity.z);
				player.setDeltaMovement(horizontalBoost.x, 0.0, horizontalBoost.z);
				player.fallDistance = 0.0F;
			} else {
				double yVel = player.onGround() ? 0.35 : 0.2;
				velocity = direction.scale(distance * 0.3);
				player.setDeltaMovement(currentMotion.add(velocity.x, yVel, velocity.z));
				tryStepUpDuringDash(player, direction, canDoubleDash ? 2 : 1);
			}
			player.hurtMarked = true;

			if (player.level() instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			}

			int dashCdSeconds = ConfigManager.getCombatConfig().getDashCooldownSeconds();
			int doubleDashCdSeconds = ConfigManager.getCombatConfig().getDoubleDashCooldownSeconds();
			int dashCdTicks = dashCdSeconds * 20;
			int doubleDashCdTicks = doubleDashCdSeconds * 20;

			if (canDoubleDash) {
				data.getCooldowns().setCooldown(Cooldowns.DASH_CD, dashCdTicks);
				data.getCooldowns().setCooldown(Cooldowns.DOUBLEDASH_CD, doubleDashCdTicks);
				data.getCooldowns().removeCooldown(Cooldowns.DASH_ACTIVE);
				player.addEffect(new MobEffectInstance(MainEffects.DASH_CD.get(), dashCdTicks, 0, false, false, true));
				player.addEffect(new MobEffectInstance(MainEffects.DOUBLEDASH_CD.get(), doubleDashCdTicks, 0, false, false, true));
			} else {
				data.getCooldowns().setCooldown(Cooldowns.DASH_CD, dashCdTicks);
				data.getCooldowns().setCooldown(Cooldowns.DASH_ACTIVE, 15);
				player.addEffect(new MobEffectInstance(MainEffects.DASH_CD.get(), dashCdTicks, 0, false, false, true));
			}

			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 1.5F + player.getRandom().nextFloat() * 0.3F);

			if (!isFlyingSkillActive) {
				int dashDirection = getDashDirectionFromInput(xInput, zInput);
				if (canDoubleDash) dashDirection += 4;
				NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.DASH, dashDirection, player.getId()), player);
			}
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
		});
	}

	private static int getDashDirectionFromInput(float xInput, float zInput) {
		if (zInput > 0 && xInput == 0) return 1;
		if (zInput < 0 && xInput == 0) return 2;
		if (xInput < 0 && zInput == 0) return 4;
		if (xInput > 0 && zInput == 0) return 3;
		if (zInput > 0) return 1;
		if (zInput < 0) return 2;
		return 1;
	}

	private static void tryStepUpDuringDash(ServerPlayer player, Vec3 direction, int maxStepHeight) {
		if (maxStepHeight <= 0 || direction.lengthSqr() < 1.0E-6) return;

		Level level = player.level();
		Vec3 dir = new Vec3(direction.x, 0.0, direction.z).normalize();
		double probeDistance = 0.9;
		double targetX = player.getX() + (dir.x * probeDistance);
		double targetZ = player.getZ() + (dir.z * probeDistance);
		BlockPos frontBase = BlockPos.containing(targetX, player.getY(), targetZ);

		if (level.getBlockState(frontBase).getCollisionShape(level, frontBase).isEmpty()) return;

		for (int step = 1; step <= maxStepHeight; step++) {
			BlockPos feetPos = frontBase.above(step);
			BlockPos headPos = feetPos.above();
			BlockPos belowPos = feetPos.below();

			boolean feetFree = level.getBlockState(feetPos).getCollisionShape(level, feetPos).isEmpty();
			boolean headFree = level.getBlockState(headPos).getCollisionShape(level, headPos).isEmpty();
			boolean support = !level.getBlockState(belowPos).getCollisionShape(level, belowPos).isEmpty();

			if (feetFree && headFree && support) {
				player.teleportTo(targetX, feetPos.getY(), targetZ);
				player.hurtMarked = true;
				return;
			}
		}
	}
}