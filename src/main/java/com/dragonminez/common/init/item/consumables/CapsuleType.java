package com.dragonminez.common.init.item.consumables;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CapsuleType {
	STR("STR", 5),
	SKP("SKP", 5),
	RES("RES", 5),
	VIT("VIT", 5),
    PWR("PWR", 5),
	ENE("ENE", 5);

    private final String statName;
    private final Integer statPoints;

}

