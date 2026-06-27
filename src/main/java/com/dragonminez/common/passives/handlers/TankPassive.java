package com.dragonminez.common.passives.handlers;

import com.dragonminez.common.passives.ClassPassives;
import com.dragonminez.common.passives.IClassPassive;
import com.dragonminez.common.stats.StatsData;

public class TankPassive implements IClassPassive {

	@Override
	public String classKey() { return "tank"; }

	private boolean isLowHp(StatsData data) {
		if (data.getPlayer() == null || data.getPlayer().getMaxHealth() <= 0.0f) return false;
		double ratio = data.getPlayer().getHealth() / data.getPlayer().getMaxHealth();
		return ratio < ClassPassives.value(data, "lowHpThreshold", 0.30);
	}

	@Override
	public double bonusHpRegenFromStamina(StatsData data) {
		double bonus = data.getStaminaRegenPerSecond() * ClassPassives.value(data, "stmToHpRegenRatio", 0.5);
		if (isLowHp(data)) bonus *= ClassPassives.value(data, "lowHpMultiplier", 2.0);
		return Math.max(0.0, bonus);
	}

	@Override
	public double healingReceivedMultiplier(StatsData data) {
		double bonus = ClassPassives.value(data, "healingBonus", 0.25);
		if (isLowHp(data)) bonus *= ClassPassives.value(data, "lowHpMultiplier", 2.0);
		return 1.0 + bonus;
	}
}
