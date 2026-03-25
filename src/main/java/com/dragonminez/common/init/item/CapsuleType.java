package com.dragonminez.common.init.item;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CapsuleType {
	RED("STR", 5),
	PURPLE("SKP", 5),
	YELLOW("RES", 5),
	GREEN("VIT", 5),
    ORANGE("PWR", 5),
	BLUE("ENE", 5)
    ;

    private final String statName;
    private final Integer statPoints;

}

