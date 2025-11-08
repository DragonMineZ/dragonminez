package com.dragonminez.common.stats;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public class StatsHelper {

    public static Optional<StatsData> getStats(Player player) {
        return player.getCapability(StatsCapability.INSTANCE).resolve();
    }

    public static void withStats(Player player, java.util.function.Consumer<StatsData> action) {
        getStats(player).ifPresent(action);
    }

    public static <T> T getOrDefault(Player player, java.util.function.Function<StatsData, T> getter, T defaultValue) {
        return getStats(player).map(getter).orElse(defaultValue);
    }

    public static int getStrength(Player player) {
        return getOrDefault(player, data -> data.getStats().getStrength(), 5);
    }

    public static int getStrikePower(Player player) {
        return getOrDefault(player, data -> data.getStats().getStrikePower(), 5);
    }

    public static int getResistance(Player player) {
        return getOrDefault(player, data -> data.getStats().getResistance(), 5);
    }

    public static int getVitality(Player player) {
        return getOrDefault(player, data -> data.getStats().getVitality(), 5);
    }

    public static int getKiPower(Player player) {
        return getOrDefault(player, data -> data.getStats().getKiPower(), 5);
    }

    public static int getEnergy(Player player) {
        return getOrDefault(player, data -> data.getStats().getEnergy(), 5);
    }

    public static int getLevel(Player player) {
        return getOrDefault(player, StatsData::getLevel, 1);
    }

    public static int getBattlePower(Player player) {
        return getOrDefault(player, StatsData::getBattlePower, 0);
    }

    public static boolean isAlive(Player player) {
        return getOrDefault(player, data -> data.getStatus().isAlive(), true);
    }

    public static boolean hasCreatedCharacter(Player player) {
        return getOrDefault(player, data -> data.getStatus().hasCreatedCharacter(), false);
    }

    public static int getMaxHealth(Player player) {
        return getOrDefault(player, StatsData::getMaxHealth, 20);
    }

    public static int getMaxEnergy(Player player) {
        return getOrDefault(player, StatsData::getMaxEnergy, 100);
    }

    public static int getMaxStamina(Player player) {
        return getOrDefault(player, StatsData::getMaxStamina, 100);
    }

    public static int getCurrentEnergy(Player player) {
        return getOrDefault(player, data -> data.getResources().getCurrentEnergy(), 0);
    }

    public static int getCurrentStamina(Player player) {
        return getOrDefault(player, data -> data.getResources().getCurrentStamina(), 0);
    }

    public static void addStat(Player player, StatType type, int amount) {
        withStats(player, data -> {
            switch (type) {
                case STRENGTH -> data.getStats().addStrength(amount);
                case STRIKE_POWER -> data.getStats().addStrikePower(amount);
                case RESISTANCE -> data.getStats().addResistance(amount);
                case VITALITY -> data.getStats().addVitality(amount);
                case KI_POWER -> data.getStats().addKiPower(amount);
                case ENERGY -> data.getStats().addEnergy(amount);
            }
        });
    }

    public enum StatType {
        STRENGTH,
        STRIKE_POWER,
        RESISTANCE,
        VITALITY,
        KI_POWER,
        ENERGY
    }
}

