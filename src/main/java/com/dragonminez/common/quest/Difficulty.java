package com.dragonminez.common.quest;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;

public enum Difficulty {
    EASY,
    NORMAL,
    HARD;

    public static Difficulty fromName(String name) {
        if (name == null || name.isBlank()) return NORMAL;
        try {
            return Difficulty.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return NORMAL;
        }
    }

    public static Difficulty fromOrdinal(int ordinal) {
        Difficulty[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NORMAL;
    }

    private static GeneralServerConfig.GameplayConfig gameplay() {
        return ConfigManager.getServerConfig().getGameplay();
    }

    public double hpMultiplier() {
        return switch (this) {
            case EASY -> gameplay().getEasyModeHPMultiplier();
            case HARD -> gameplay().getHardModeHPMultiplier();
            case NORMAL -> 1.0;
        };
    }

    public double damageMultiplier() {
        return switch (this) {
            case EASY -> gameplay().getEasyModeDamageMultiplier();
            case HARD -> gameplay().getHardModeDamageMultiplier();
            case NORMAL -> 1.0;
        };
    }

    public double tpMultiplier() {
        return switch (this) {
            case EASY -> gameplay().getEasyModeTPMultiplier();
            case HARD -> gameplay().getHardModeTPMultiplier();
            case NORMAL -> 1.0;
        };
    }

    public double questRewardMultiplier() {
        return switch (this) {
            case EASY -> gameplay().getEasyModeQuestRewardMultiplier();
            case HARD -> gameplay().getHardModeQuestRewardMultiplier();
            case NORMAL -> 1.0;
        };
    }

    public int aiTierId() {
        return switch (this) {
            case EASY -> 1;
            case NORMAL -> 2;
            case HARD -> 3;
        };
    }
}
