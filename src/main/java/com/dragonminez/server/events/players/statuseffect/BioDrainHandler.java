package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class BioDrainHandler implements IStatusEffectHandler {
	@Override
	public void handleStatusEffects(ServerPlayer player, StatsData data) {

	}

	@Override
	public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {
		handleBioDrainTick(serverPlayer, data);
	}

	@Override
	public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {
		System.out.println("Calling BioDrainHandler onPlayerSecond");
		handleBioDrainSecond(serverPlayer, data);
	}

	private static void handleBioDrainTick(ServerPlayer player, StatsData data) {
		int targetId = data.getStatus().getDrainingTargetId();
		if (targetId == -1) return;

		if (data.getCooldowns().getCooldown(Cooldowns.DRAIN_ACTIVE) > 0) {
			Entity entity = player.level().getEntity(targetId);
			if (entity instanceof LivingEntity target && target.isAlive() && player.distanceTo(target) < 6.0f) {
				float targetYRot = target.getYRot();

				double dist = 0.75;
				double rads = Math.toRadians(targetYRot);
				double xOffset = -Math.sin(rads) * dist;
				double zOffset = Math.cos(rads) * dist;

				double finalX = target.getX() - xOffset;
				double finalZ = target.getZ() - zOffset;
				double finalY = target.getY();

				player.connection.teleport(finalX, finalY, finalZ, targetYRot, 15.0F);

				player.setYRot(targetYRot);
				player.setYHeadRot(targetYRot);

			} else {
				data.getStatus().setDrainingTargetId(-1);
				data.getCooldowns().removeCooldown(Cooldowns.DRAIN_ACTIVE);
				player.removeEffect(MainEffects.STUN.get());
			}

		} else {
			Entity entity = player.level().getEntity(targetId);
			if (entity instanceof LivingEntity target) {
				Vec3 look = player.getLookAngle();
				target.setDeltaMovement(look.scale(1.5).add(0, 0.5, 0));
				target.hurtMarked = true;
				player.setDeltaMovement(look.scale(-1.0).add(0, 0.3, 0));
				player.hurtMarked = true;
				target.playSound(MainSounds.KNOCKBACK_CHARACTER.get());
				player.playSound(MainSounds.KNOCKBACK_CHARACTER.get());
			}
			data.getStatus().setDrainingTargetId(-1);
			player.removeEffect(MainEffects.STUN.get());
		}
	}

	private static void handleBioDrainSecond(ServerPlayer player, StatsData data) {
		int targetId = data.getStatus().getDrainingTargetId();
		if (targetId == -1) return;

		if (data.getCooldowns().getCooldown(Cooldowns.DRAIN_ACTIVE) > 0) {
			Entity entity = player.level().getEntity(targetId);
			if (entity instanceof LivingEntity target && target.isAlive() && player.distanceTo(target) < 6.0f) {
				GeneralServerConfig.RacialSkillsConfig config = ConfigManager.getServerConfig().getRacialSkills();
				double totalDrainRatio = config.getBioAndroidDrainRatio();

				float durationSeconds = 5.0f;
				float totalHealthToDrain = (float) (target.getMaxHealth() * totalDrainRatio);
				float drainPerSecond = Math.round(totalHealthToDrain / durationSeconds);

				target.setHealth(Math.max(1, target.getHealth() - drainPerSecond));
				if (player.getHealth() < player.getMaxHealth()) {
					player.heal(drainPerSecond);
				}
				if (data.getResources().getCurrentEnergy() < data.getMaxEnergy()) {
					data.getResources().addEnergy((int) (drainPerSecond * 5));
				}
				target.playSound(MainSounds.ABSORB1.get());
				player.playSound(MainSounds.ABSORB1.get());

				if (target.getHealth() <= 1.0f) {
					target.hurtMarked = true;
					data.getStatus().setDrainingTargetId(-1);
					data.getCooldowns().removeCooldown(Cooldowns.DRAIN_ACTIVE);
					player.removeEffect(MainEffects.STUN.get());
					target.kill();
				}
			}
		}
	}
}
