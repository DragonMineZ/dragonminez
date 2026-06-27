package com.dragonminez.common.stats.extras;

public enum DynamicGrowthStat {
	STR, SKP, RES, VIT, PWR, ENE;

	public String key() {
		return name();
	}
}
