package com.dragonminez.common.stats.extras;

import com.dragonminez.common.config.ConfigManager;

public final class DynamicGrowthMath {
	private DynamicGrowthMath() {}

	public static int requiredXp(int currentStat) {
		if (!ConfigManager.getServerConfig().getDynamicGrowth().isPracticeCurveEnabled()) return 100;
		return 70 + currentStat * 2 + (int) Math.floor(Math.pow(currentStat, 1.35));
	}
}
