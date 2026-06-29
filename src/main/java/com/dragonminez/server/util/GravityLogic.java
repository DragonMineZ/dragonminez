package com.dragonminez.server.util;

import com.dragonminez.common.compat.WorldGuardCompat;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.entities.AllMastersEntity;
import com.dragonminez.common.init.item.WeightItem;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.world.dimension.HTCDimension;
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
		return computeGravity(player, true);
	}

	public static double getTrainingGravityMultiplier(Player player) {
		boolean htc = player.level().dimension().equals(HTCDimension.HTC_KEY);
		return computeGravity(player, !htc);
	}

	private static double computeGravity(Player player, boolean includeDimension) {
		GeneralServerConfig.GravityConfig config = cfg();
		if (!config.isEnabled()) return 1.0;

		String dimId = player.level().dimension().location().toString();
		double gravity = includeDimension ? config.getWorldGravity(dimId) : config.getDefaultWorldGravity();

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

	public static double getTrainingBonusGravity(Player player) {
		double gravity = getTrainingGravityMultiplier(player);
		if (gravity <= 1.0) return 0.0;
		double net = Math.max(0.0, gravity - getResistance(player));
		if (net >= cfg().getHardStopThreshold()) return 0.0;
		return net;
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

	public static double getMachineGravity(Player player) {
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

	private static double computeRelativeLevel(StatsData data) {
		int currentBaseLevel = data.getLevel();
		int totalBaseStats = data.getStats().getTotalStats();
		int initialStats = totalBaseStats - (currentBaseLevel - 1) * 6;

		double boostedTotal =
				data.getStats().getStrength() * data.getTotalMultiplier("STR") +
				data.getStats().getStrikePower() * data.getTotalMultiplier("SKP") +
				data.getStats().getResistance() * data.getTotalMultiplier("RES") +
				data.getStats().getVitality() * data.getTotalMultiplier("VIT") +
				data.getStats().getKiPower() * data.getTotalMultiplier("PWR") +
				data.getStats().getEnergy() * data.getTotalMultiplier("ENE");

		return ((boostedTotal - initialStats) / 6.0) + 1.0;
	}

	public static int getIdealWeight(Player player) {
		GeneralServerConfig.GravityConfig config = cfg();
		if (!config.getTpEnabled()) return 0;
		double gravity = getTrainingGravityMultiplier(player);
		if (gravity <= 0.0) return 0;
		return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
			double relativeLevel = computeRelativeLevel(data);
			double gravityFactor = Math.max(0.0001, 1.0 + (gravity - 1.0) * config.getGravitySensitivity());
			double ideal = relativeLevel / (config.getTpIdealBaseDivisor() * gravityFactor);
			return (int) Math.max(0, Math.round(ideal));
		}).orElse(0);
	}

	public static double getLoadRatio(Player player) {
		int ideal = getIdealWeight(player);
		if (ideal <= 0) return 0.0;
		return (double) getTotalWeight(player) / ideal;
	}

	public static int getTrainingZone(Player player) {
		if (getTotalWeight(player) <= 0) return 0;
		int ideal = getIdealWeight(player);
		if (ideal <= 0) return 0;
		GeneralServerConfig.GravityConfig config = cfg();
		double r = (double) getTotalWeight(player) / ideal;
		if (r < config.getTpIdealRatioLow()) return 1;
		if (r <= config.getTpIdealRatioHigh()) return 2;
		if (r < config.getTpOverloadHardRatio()) return 3;
		return 4;
	}

	public static double getWeightTpMultiplier(Player player) {
		GeneralServerConfig.GravityConfig config = cfg();
		if (!config.getTpEnabled()) return 1.0;
		if (getTotalWeight(player) <= 0) return 1.0;
		int ideal = getIdealWeight(player);
		if (ideal <= 0) return 1.0;
		return weightTpMultiplierForRatio((double) getTotalWeight(player) / ideal, config);
	}

	private static double weightTpMultiplierForRatio(double r, GeneralServerConfig.GravityConfig config) {
		double comfortLow = config.getTpComfortRatioLow();
		double idealLow = config.getTpIdealRatioLow();
		double idealHigh = config.getTpIdealRatioHigh();
		double overload = config.getTpOverloadRatio();
		double overloadHard = config.getTpOverloadHardRatio();
		double comfortMult = config.getTpComfortMultiplier();
		double peak = config.getTpPeakMultiplier();
		double heavyMult = config.getTpHeavyMultiplier();

		if (r <= 0.0) return 1.0;
		if (comfortLow > 0.0 && r < comfortLow) return lerp(1.0, comfortMult, r / comfortLow);
		if (r < idealLow) return lerp(comfortMult, peak, (r - comfortLow) / (idealLow - comfortLow));
		if (r <= idealHigh) return peak;
		if (r <= overload) return lerp(peak, heavyMult, (r - idealHigh) / (overload - idealHigh));
		if (r < overloadHard) return lerp(heavyMult, 1.0, (r - overload) / (overloadHard - overload));
		return 1.0;
	}

	public static double getWeightPenaltyFactor(Player player) {
		GeneralServerConfig.GravityConfig config = cfg();
		if (!config.getTpEnabled()) return 0.0;
		if (getTotalWeight(player) <= 0) return 0.0;
		int ideal = getIdealWeight(player);
		if (ideal <= 0) return 0.0;
		double r = (double) getTotalWeight(player) / ideal;
		double idealHigh = config.getTpIdealRatioHigh();
		double overloadHard = config.getTpOverloadHardRatio();
		if (r <= idealHigh) return 0.0;
		double max = config.getMaxWeightPenalty();
		if (r >= overloadHard) return max;
		return max * (r - idealHigh) / (overloadHard - idealHigh);
	}

	public static double getStatReduction(Player player) {
		GeneralServerConfig.GravityConfig config = cfg();
		if (!config.getStatReductionEnabled()) return 0.0;
		double pGravity = getPenalizationGravity(player);
		double reduction = 0.0;
		if (pGravity > 0) {
			reduction = pGravity * config.getStatReductionPerGravity();
			reduction = Math.max(config.getMinStatReduction(), Math.min(config.getMaxStatReduction(), reduction));
		}
		reduction = Math.min(config.getMaxStatReduction(), reduction + getWeightPenaltyFactor(player));
		return reduction;
	}

	private static double lerp(double a, double b, double t) {
		if (t <= 0.0) return a;
		if (t >= 1.0) return b;
		return a + (b - a) * t;
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

		double weightPenalty = getWeightPenaltyFactor(player);

		double movePenalty = 0.0;
		double attackPenalty = 0.0;
		if (pGravity > 0) {
			if (pGravity >= config.getHardStopThreshold()) {
				movePenalty = config.getMaxMovementPenalty();
				attackPenalty = config.getMaxAttackPenalty();
			} else {
				movePenalty = Math.min(config.getMaxMovementPenalty(), getGeneralPenaltyFactor(pGravity));
				attackPenalty = Math.min(config.getMaxAttackPenalty(), Math.sqrt(pGravity / 100.0));
			}
		}

		movePenalty = Math.min(config.getMaxMovementPenalty(), movePenalty + weightPenalty);
		attackPenalty = Math.min(config.getMaxAttackPenalty(), attackPenalty + weightPenalty);

		if (movePenalty > 0) {
			movementSpeed.addTransientModifier(new AttributeModifier(
					GRAVITY_SPEED_UUID,
					"Gravity movement penalty",
					-movePenalty,
					AttributeModifier.Operation.MULTIPLY_TOTAL
			));
		}
		if (attackPenalty > 0) {
			attackSpeed.addTransientModifier(new AttributeModifier(
					GRAVITY_ATTACK_SPEED_UUID,
					"Gravity attack speed penalty",
					-attackPenalty,
					AttributeModifier.Operation.MULTIPLY_TOTAL
			));
		}

		applyStatReduction(player, config);
	}

	private static void applyStatReduction(ServerPlayer player, GeneralServerConfig.GravityConfig config) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			String[] stats = config.getAffectedStats();
			double reduction = getStatReduction(player);

			if (reduction <= 0.0) {
				for (String stat : stats) data.getBonusStats().removeBonusSplit(stat, "Gravity");
				return;
			}

			double multiplier = 1.0 - reduction;
			for (String stat : stats) data.getBonusStats().addBonusSplit(stat, "Gravity", "*", multiplier, false);
		});
	}

	public static void clearNpcGravityCache(UUID playerId) {
		NPC_GRAVITY_CACHE.remove(playerId);
		NPC_GRAVITY_TICK.remove(playerId);
		NPC_GRAVITY_DIM.remove(playerId);
	}

	public static double getConsumptionMultiplier(Player player) {
		double pGravity = getPenalizationGravity(player);
		if (pGravity <= 0) return 1.0;
		return 1.0 + (pGravity * cfg().getConsumptionPerGravity());
	}
}
