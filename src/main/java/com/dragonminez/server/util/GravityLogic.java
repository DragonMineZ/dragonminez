package com.dragonminez.server.util;

import com.dragonminez.common.compat.WorldGuardCompat;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.entities.AllMastersEntity;
import com.dragonminez.common.init.item.WeightItem;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GravityLogic {
	public static final UUID GRAVITY_SPEED_UUID = UUID.fromString("019c3047-cd2f-7af4-a3cd-5bca51dd3588");
	private static final UUID GRAVITY_ATTACK_SPEED_UUID = UUID.fromString("019c3047-4e91-74e1-ac87-d4ea8e463688");

	private static final Map<UUID, Double> NPC_GRAVITY_CACHE = new HashMap<>();
	private static final Map<UUID, Long> NPC_GRAVITY_TICK = new HashMap<>();
	private static final Map<UUID, String> NPC_GRAVITY_DIM = new HashMap<>();

	private static GeneralServerConfig.GravityConfig cfg() {
		return ConfigManager.getServerConfig().getGravity();
	}

	public static double getGravityMultiplier(Player player) {
		GeneralServerConfig.GravityConfig config = cfg();
		if (!config.isEnabled()) return 1.0;

		String dimId = player.level().dimension().location().toString();
		double gravity = config.getWorldGravity(dimId);

		double wgGravity = WorldGuardCompat.getGravity(player.level(), player.blockPosition(), player);
		if (wgGravity > gravity) gravity = wgGravity;

		double npcGravity = getNpcGravity(player);
		if (npcGravity > gravity) gravity = npcGravity;

		double machineGravity = getMachineGravity(player);
		if (machineGravity > gravity) gravity = machineGravity;

		return Math.max(0.0, gravity);
	}

	private static double getResistance(Player player) {
		GeneralServerConfig.GravityConfig config = cfg();
		return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
			double avgEffectiveStats = (
					data.getStats().getStrength() * data.getTotalMultiplier("STR") +
					data.getStats().getStrikePower() * data.getTotalMultiplier("SKP") +
					data.getStats().getResistance() * data.getTotalMultiplier("RES") +
					data.getStats().getVitality() * data.getTotalMultiplier("VIT") +
					data.getStats().getKiPower() * data.getTotalMultiplier("PWR") +
					data.getStats().getEnergy() * data.getTotalMultiplier("ENE")
			) / 6.0;

			int maxStats = ConfigManager.getServerConfig().getGameplay().getMaxValue();
			double div = Math.max(1.0, maxStats * config.getResistanceStatDivisorRatio());
			return (avgEffectiveStats / div) * config.getResistanceScale();
		}).orElse(0.0);
	}

	public static double getNetGravity(Player player) {
		double gravity = getGravityMultiplier(player);
		if (gravity <= 1.0) return 0.0;
		return Math.max(0.0, gravity - getResistance(player));
	}

	public static double getBonusGravity(Player player) {
		double netGravity = getNetGravity(player);
		GeneralServerConfig.GravityConfig config = cfg();

		if (netGravity >= config.getHardStopThreshold()) return 0.0;

		return netGravity;
	}

	public static double getPenalizationGravity(Player player) {
		return getNetGravity(player);
	}

	private static double getNpcGravity(Player player) {
		GeneralServerConfig.GravityConfig config = cfg();
		UUID id = player.getUUID();
		long currentTick = player.level().getGameTime();
		String currentDim = player.level().dimension().location().toString();

		boolean dimChanged = !currentDim.equals(NPC_GRAVITY_DIM.get(id));
		boolean expired = currentTick - NPC_GRAVITY_TICK.getOrDefault(id, 0L) > 100;

		if (dimChanged || expired || !NPC_GRAVITY_CACHE.containsKey(id)) {
			double gravity = 0.0;
			double range = config.getNpcGravityRange();
			AABB searchBox = player.getBoundingBox().inflate(range);
			List<AllMastersEntity.MasterKaiosamaEntity> kais = player.level().getEntitiesOfClass(AllMastersEntity.MasterKaiosamaEntity.class, searchBox);
			if (!kais.isEmpty()) gravity = config.getNpcGravityValue();

			NPC_GRAVITY_CACHE.put(id, gravity);
			NPC_GRAVITY_TICK.put(id, currentTick);
			NPC_GRAVITY_DIM.put(id, currentDim);
			return gravity;
		}

		return NPC_GRAVITY_CACHE.getOrDefault(id, 0.0);
	}

	private static double getMachineGravity(Player player) {
		if (!cfg().getMachineGravityEnabled()) return 0.0;
		return GravityDeviceManager.getGravityFor(player);
	}

	public static int getTotalWeight(Player player) {
		int[] totalWeight = {0};
		CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
			var handler = inv.getCurios().get("weights");
			if (handler != null) {
				for (int i = 0; i < handler.getSlots(); i++) {
					ItemStack stack = handler.getStacks().getStackInSlot(i);
					if (stack.getItem() instanceof WeightItem) {
						totalWeight[0] += WeightItem.getWeight(stack);
					} else if (!stack.isEmpty()) {
						totalWeight[0] += stack.getOrCreateTag().getInt("WeightValue");
					}
				}
			}
		});
		return totalWeight[0];
	}

	public static double getGeneralPenaltyFactor(double pGravity) {
		if (pGravity <= 0) return 0.0;
		double baseCurve = Math.sqrt(pGravity / 100.0);
		return baseCurve * cfg().getPenaltyCurveFactor();
	}

	public static double getJumpFactor(Player player) {
		GeneralServerConfig.GravityConfig config = cfg();
		if (!config.getPhysicalEnabled()) return 1.0;
		double pGravity = getPenalizationGravity(player);
		if (pGravity <= 0) return 1.0;
		if (pGravity >= config.getHardStopThreshold()) return 1.0 - config.getMaxJumpPenalty();
		double penalty = Math.min(config.getMaxJumpPenalty(), getGeneralPenaltyFactor(pGravity));
		return 1.0 - penalty;
	}

	public static double getFlyFactor(Player player) {
		GeneralServerConfig.GravityConfig config = cfg();
		if (!config.getPhysicalEnabled()) return 1.0;
		double pGravity = getPenalizationGravity(player);
		if (pGravity <= 0) return 1.0;
		if (pGravity >= config.getHardStopThreshold()) return 0.0;
		double penalty = Math.min(config.getMaxFlyPenalty(), getGeneralPenaltyFactor(pGravity));
		return 1.0 - penalty;
	}

	public static boolean isFlightHardStopped(Player player) {
		GeneralServerConfig.GravityConfig config = cfg();
		if (!config.getPhysicalEnabled()) return false;
		return getPenalizationGravity(player) >= config.getHardStopThreshold();
	}

	public static double getFallExtra(Player player) {
		GeneralServerConfig.GravityConfig config = cfg();
		if (!config.getPhysicalEnabled()) return 0.0;
		double pGravity = getPenalizationGravity(player);
		if (pGravity <= 0) return 0.0;
		return Math.min(config.getMaxExtraFall(), pGravity * config.getExtraFallPerGravity());
	}

	public static void tick(ServerPlayer player) {
		GeneralServerConfig.GravityConfig config = cfg();
		double pGravity = getPenalizationGravity(player);

		AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);

		if (movementSpeed == null || attackSpeed == null) return;
		movementSpeed.removeModifier(GRAVITY_SPEED_UUID);
		attackSpeed.removeModifier(GRAVITY_ATTACK_SPEED_UUID);

		if (pGravity > 0) {
			double movePenalty;
			double attackPenalty;

			if (pGravity >= config.getHardStopThreshold()) {
				movePenalty = -config.getMaxMovementPenalty();
				attackPenalty = -config.getMaxAttackPenalty();
			} else {
				double generalFactor = getGeneralPenaltyFactor(pGravity);
				movePenalty = -Math.min(config.getMaxMovementPenalty(), generalFactor);
				double attackFactor = Math.sqrt(pGravity / 100.0);
				attackPenalty = -Math.min(config.getMaxAttackPenalty(), attackFactor);
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

		applyStatReduction(player, pGravity, config);
		syncMachineGravity(player);
	}

	private static final Map<UUID, Float> LAST_MACHINE_GRAVITY = new HashMap<>();

	private static void syncMachineGravity(ServerPlayer player) {
		float machineGravity = (float) getMachineGravity(player);
		float last = LAST_MACHINE_GRAVITY.getOrDefault(player.getUUID(), -1.0f);
		if (Math.abs(machineGravity - last) >= 0.5f) {
			LAST_MACHINE_GRAVITY.put(player.getUUID(), machineGravity);
			com.dragonminez.common.network.NetworkHandler.sendToPlayer(
					new com.dragonminez.common.network.S2C.GravityZoneSyncS2C(machineGravity), player);
		}
	}

	private static void applyStatReduction(ServerPlayer player, double pGravity, GeneralServerConfig.GravityConfig config) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			String[] stats = config.getAffectedStats();

			double reduction = 0.0;
			if (config.getStatReductionEnabled() && pGravity > 0) {
				reduction = pGravity * config.getStatReductionPerGravity();
				reduction = Math.max(config.getMinStatReduction(), Math.min(config.getMaxStatReduction(), reduction));
			}

			if (reduction <= 0.0) {
				for (String stat : stats) data.getBonusStats().removeBonusSplit(stat, "Gravity");
				return;
			}

			double multiplier = 1.0 - reduction;
			for (String stat : stats) data.getBonusStats().addBonusSplit(stat, "Gravity", "*", multiplier, false);
		});
	}

	/** Clears all per-player NPC gravity cache entries. Call on player logout. */
	public static void clearNpcGravityCache(UUID playerId) {
		NPC_GRAVITY_CACHE.remove(playerId);
		NPC_GRAVITY_TICK.remove(playerId);
		NPC_GRAVITY_DIM.remove(playerId);
		LAST_MACHINE_GRAVITY.remove(playerId);
	}

	public static double getConsumptionMultiplier(Player player) {
		double pGravity = getPenalizationGravity(player);
		if (pGravity <= 0) return 1.0;
		return 1.0 + (pGravity * cfg().getConsumptionPerGravity());
	}
}
