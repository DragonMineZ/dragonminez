package com.dragonminez.common.stats.extras;

public enum DynamicGrowthStat {
	STR, SKP, RES, VIT, PWR, ENE;

	public String key() {
		return name();
	}

	public DynamicGrowthStat linked() {
		return switch (this) {
			case STR -> SKP;
			case SKP -> STR;
			case RES -> VIT;
			case VIT -> RES;
			case PWR -> ENE;
			case ENE -> PWR;
		};
	}
}
