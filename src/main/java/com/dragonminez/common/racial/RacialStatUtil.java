package com.dragonminez.common.racial;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;

public final class RacialStatUtil {

	private RacialStatUtil() {
	}

	public static int getStat(StatsData data, String statName) {
		return switch (statName) {
			case "STR" -> data.getStats().getStrength();
			case "SKP" -> data.getStats().getStrikePower();
			case "RES" -> data.getStats().getResistance();
			case "VIT" -> data.getStats().getVitality();
			case "PWR" -> data.getStats().getKiPower();
			case "ENE" -> data.getStats().getEnergy();
			default -> 0;
		};
	}

	public static int applyTechniqueXpBonus(StatsData data, int amount) {
		if (amount <= 0 || !(data.getPlayer() instanceof ServerPlayer serverPlayer)) return amount;
		return RacialRegistry.forPlayer(data)
				.map(ability -> (int) Math.round(ability.modifyTechniqueXpGain(new RacialContext(serverPlayer, data), amount)))
				.orElse(amount);
	}

	public static float applyTechniqueXpBonus(StatsData data, float amount) {
		if (amount <= 0 || !(data.getPlayer() instanceof ServerPlayer serverPlayer)) return amount;
		return RacialRegistry.forPlayer(data)
				.map(ability -> (float) ability.modifyTechniqueXpGain(new RacialContext(serverPlayer, data), amount))
				.orElse(amount);
	}
}
