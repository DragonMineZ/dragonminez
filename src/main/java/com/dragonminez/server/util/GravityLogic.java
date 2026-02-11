package com.dragonminez.server.util;

import com.dragonminez.common.compat.WorldGuardCompat;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.entities.masters.MasterKaiosamaEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.world.dimension.HTCDimension;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class GravityLogic {
	public static final UUID GRAVITY_SPEED_UUID = UUID.fromString("019c3047-cd2f-7af4-a3cd-5bca51dd3588");
	private static final UUID GRAVITY_ATTACK_SPEED_UUID = UUID.fromString("019c3047-4e91-74e1-ac87-d4ea8e463688");

	public static double getRawGravity(Player player) {
		double maxGravity = 1.0;

		if (player.level().dimension().equals(HTCDimension.HTC_KEY)) maxGravity = Math.max(maxGravity, 10.0);

		double wgGravity = WorldGuardCompat.getGravity(player.level(), player.blockPosition(), player);
		if (wgGravity > 0) maxGravity = Math.max(maxGravity, wgGravity);

		double npcGravity = getNpcGravity(player);
		if (npcGravity > 0) maxGravity = Math.max(maxGravity, npcGravity);

		// double machineGravity = GravityMachineLogic.getNearbyGravity(player);
		// if (machineGravity > 1.0) return machineGravity;
		return maxGravity;

	}

	private static double getNpcGravity(Player player) {
		double gravity = 0.0;
		double range = 100.0;
		AABB searchBox = player.getBoundingBox().inflate(range);
		List<MasterKaiosamaEntity> kais = player.level().getEntitiesOfClass(MasterKaiosamaEntity.class, searchBox);
		if (!kais.isEmpty()) gravity = 10.0;
		return gravity;
	}

	public static double getBonusGravity(Player player) {
		double rawGravity = getRawGravity(player);
		if (rawGravity <= 1.0) return 0.0;

		return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
			double totalStats = data.getStats().getTotalStats();
			double avgStats = totalStats / 6.0;

			int maxStats = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
			double div = Math.max(1.0, maxStats - (maxStats / 10.0));
			double baseResistance = (avgStats / div) * 100.0;

			return Math.max(0.0, rawGravity - baseResistance);
		}).orElse(0.0);
	}

	public static double getPenalizationGravity(Player player) {
		double rawGravity = getRawGravity(player);
		if (rawGravity <= 1.0) return 0.0;
		AtomicReference<Double> penGravity = new AtomicReference<>(0.0);

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			double totalStats = data.getStats().getTotalStats();
			double avgStats = totalStats / 6.0;

			int maxStats = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
			double div = Math.max(1.0, maxStats - (maxStats / 10.0));
			double baseResistance = (avgStats / div) * 100.0;
			double strMult = data.getTotalMultiplier("STR");
			double skpMult = data.getTotalMultiplier("SKP");
			double resMult = data.getTotalMultiplier("RES");
			double vitMult = data.getTotalMultiplier("VIT");
			double pwrMult = data.getTotalMultiplier("PWR");
			double eneMult = data.getTotalMultiplier("ENE");
			double avgBonus = (strMult + skpMult + resMult + vitMult + pwrMult + eneMult) / 6.0;
			double transformFactor = 1.0 + avgBonus;
			double finalResistance = baseResistance * transformFactor;

			penGravity.set(Math.max(0.0, rawGravity - finalResistance));
		});

		return penGravity.get();
	}

	public static double getGeneralPenaltyFactor(double pGravity) {
		if (pGravity <= 0) return 0.0;
		double baseCurve = Math.sqrt(pGravity / 100.0);
		return baseCurve * 1.6;
	}

	public static void tick(ServerPlayer player) {
		double pGravity = getPenalizationGravity(player);

		AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);

		if (movementSpeed == null || attackSpeed == null) return;
		movementSpeed.removeModifier(GRAVITY_SPEED_UUID);
		attackSpeed.removeModifier(GRAVITY_ATTACK_SPEED_UUID);

		if (pGravity > 0) {
			double movePenalty;
			double attackPenalty;

			if (pGravity >= 75.0) {
				movePenalty = -1.0;
				attackPenalty = -1.0;
			} else {
				double generalFactor = getGeneralPenaltyFactor(pGravity);
				movePenalty = -Math.min(0.95, generalFactor);
				double attackFactor = Math.sqrt(pGravity / 100.0);
				attackPenalty = -Math.min(0.9, attackFactor);
			}

			movementSpeed.addTransientModifier(new AttributeModifier(
					GRAVITY_SPEED_UUID,
					"Gravity movement penalty",
					movePenalty,
					AttributeModifier.Operation.MULTIPLY_TOTAL
			));

			attackSpeed.addTransientModifier(new AttributeModifier(
					GRAVITY_ATTACK_SPEED_UUID,
					"Gravity attack speed penalty",
					attackPenalty,
					AttributeModifier.Operation.MULTIPLY_TOTAL
			));
		}
	}

	public static double getConsumptionMultiplier(Player player) {
		double pGravity = getPenalizationGravity(player);
		if (pGravity <= 0) return 1.0;
		return 1.0 + (pGravity / 25.0);
	}
}