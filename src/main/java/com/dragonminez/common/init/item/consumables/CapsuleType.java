package com.dragonminez.common.init.item.consumables;

public enum CapsuleType {
	STR("STR", 5), SKP("SKP", 5), RES("RES", 5), VIT("VIT", 5), PWR("PWR", 5), ENE("ENE", 5);
	private final String statName;
	private final Integer statPoints;

	public String getStatName() {
		return this.statName;
	}

	public Integer getStatPoints() {
		return this.statPoints;
	}

	private CapsuleType(final String statName, final Integer statPoints) {
		this.statName = statName;
		this.statPoints = statPoints;
	}
}
