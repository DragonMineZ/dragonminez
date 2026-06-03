package com.dragonminez.common.passives.handlers;

import com.dragonminez.common.passives.ClassPassives;
import com.dragonminez.common.passives.IClassPassive;
import com.dragonminez.common.stats.StatsData;

public class BerserkerPassive implements IClassPassive {

	@Override
	public String classKey() { return "berserker"; }

	private double hpRatio(StatsData data) {
		if (data.getPlayer() == null || data.getPlayer().getMaxHealth() <= 0.0f) return 1.0;
		return data.getPlayer().getHealth() / data.getPlayer().getMaxHealth();
	}

	@Override
	public double healthRegenMultiplier(StatsData data) {
		double ratio = hpRatio(data);
		if (ratio < ClassPassives.value(data, "hpThreshLow", 0.33)) return 1.0 + ClassPassives.value(data, "hpRegenLow", 0.75);
		if (ratio < ClassPassives.value(data, "hpThreshHigh", 0.66)) return 1.0 + ClassPassives.value(data, "hpRegenHigh", 0.25);
		return 1.0;
	}

	@Override
	public double critChanceBonus(StatsData data) {
		double ratio = hpRatio(data);
		if (ratio < ClassPassives.value(data, "hpThreshLow", 0.33)) return ClassPassives.value(data, "critLow", 0.25);
		if (ratio < ClassPassives.value(data, "hpThreshHigh", 0.66)) return ClassPassives.value(data, "critHigh", 0.10);
		return 0.0;
	}
}
