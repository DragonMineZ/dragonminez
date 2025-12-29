package com.dragonminez.common.init.item;

public enum CapsuleType {
	RED("STR", "red_capsule"),
	PURPLE("SKP", "purple_capsule"),
	YELLOW("RES", "yellow_capsule"),
	GREEN("VIT", "green_capsule"),
    ORANGE("PWR", "orange_capsule"),
	BLUE("ENE", "blue_capsule");

    private final String statName;
    private final String translationKey;

    CapsuleType(String statName, String translationKey) {
        this.statName = statName;
        this.translationKey = translationKey;
    }

    public String getStatName() {
        return statName;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}

