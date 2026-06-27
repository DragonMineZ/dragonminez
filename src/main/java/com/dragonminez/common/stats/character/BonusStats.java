package com.dragonminez.common.stats.character;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BonusStats {
    private final Map<String, List<StatBonus>> bonuses = new HashMap<>();

    public BonusStats() {
        initializeStat("STR");
        initializeStat("SKP");
        initializeStat("DEF");
        initializeStat("STM");
        initializeStat("VIT");
        initializeStat("PWR");
        initializeStat("ENE");
    }

    private void initializeStat(String stat) {
        bonuses.put(stat, new ArrayList<>());
    }

    public void addBonusSplit(String stat, String bonusName, String operation, double value, boolean applyMultipliers) {
        if (stat.equalsIgnoreCase("RES")) {
            addBonus("DEF", bonusName, operation, value, applyMultipliers);
            addBonus("STM", bonusName, operation, value, applyMultipliers);
        } else addBonus(stat, bonusName, operation, value, applyMultipliers);
    }

    public void removeBonusSplit(String stat, String bonusName) {
        if (stat.equalsIgnoreCase("RES")) {
            removeBonus("DEF", bonusName);
            removeBonus("STM", bonusName);
        } else removeBonus(stat, bonusName);
    }

    public void clearAllSplit(String stat) {
        if (stat.equalsIgnoreCase("RES")) {
            clearAll("DEF");
            clearAll("STM");
        } else clearAll(stat);
    }

    public void addBonus(String stat, String bonusName, String operation, double value) {
        addBonus(stat, bonusName, operation, value, false);
    }

    public void addBonus(String stat, String bonusName, String operation, double value, boolean applyMultipliers) {
        stat = stat.toUpperCase();
        if (!bonuses.containsKey(stat)) {
            return;
        }

        List<StatBonus> statBonuses = bonuses.get(stat);
        statBonuses.removeIf(bonus -> bonus.name.equals(bonusName));
        statBonuses.add(new StatBonus(bonusName, operation, value, applyMultipliers));
    }

    public void removeBonus(String stat, String bonusName) {
        stat = stat.toUpperCase();
        if (!bonuses.containsKey(stat)) {
            return;
        }

        List<StatBonus> statBonuses = bonuses.get(stat);
        statBonuses.removeIf(bonus -> bonus.name.equals(bonusName));
    }

    public void removeAllBonuses(String bonusName) {
        for (List<StatBonus> statBonuses : bonuses.values()) {
            statBonuses.removeIf(bonus -> bonus.name.equals(bonusName));
        }
    }

    public void clearBonus(String stat, String bonusName) {
        stat = stat.toUpperCase();
        if (!bonuses.containsKey(stat)) return;

        List<StatBonus> statBonuses = bonuses.get(stat);
        statBonuses.removeIf(bonus -> bonus.name.contains(bonusName));
    }

    public void clearAll(String stat) {
        stat = stat.toUpperCase();
        if (!bonuses.containsKey(stat)) return;
        bonuses.get(stat).clear();
    }

    public void clearAllStats() {
        for (List<StatBonus> bonusList : bonuses.values()) bonusList.clear();
    }

    public double calculateBonus(String stat, int baseStat, boolean getMultiplicable) {
        stat = stat.toUpperCase();
        if (!bonuses.containsKey(stat)) {
            return 0;
        }

        double flatResult = 0;
        double multiplierProduct = 1.0;
        List<StatBonus> statBonuses = bonuses.get(stat);

        for (StatBonus bonus : statBonuses) {
            if (bonus.applyMultipliers == getMultiplicable) {
                switch (bonus.operation) {
                    case "+" -> flatResult += bonus.value;
                    case "-" -> flatResult -= bonus.value;
                    case "*" -> multiplierProduct *= bonus.value;
                }
            }
        }

        // Apply all * bonuses as a single combined multiplier (multiplicative chaining)
        // then add flat +/- bonuses on top
        return (baseStat * multiplierProduct - baseStat) + flatResult;
    }

    public List<StatBonus> getBonuses(String stat) {
        stat = stat.toUpperCase();
        if (!bonuses.containsKey(stat)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(bonuses.get(stat));
    }

    public boolean hasBonus(String stat, String bonusName) {
        stat = stat.toUpperCase();
        if (!bonuses.containsKey(stat)) {
            return false;
        }
        return bonuses.get(stat).stream().anyMatch(bonus -> bonus.name.equals(bonusName));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        for (Map.Entry<String, List<StatBonus>> entry : bonuses.entrySet()) {
            ListTag bonusList = new ListTag();
            for (StatBonus bonus : entry.getValue()) {
                CompoundTag bonusTag = new CompoundTag();
                bonusTag.putString("Name", bonus.name);
                bonusTag.putString("Operation", bonus.operation);
                bonusTag.putDouble("Value", bonus.value);
                bonusTag.putBoolean("ApplyMultipliers", bonus.applyMultipliers);
                bonusList.add(bonusTag);
            }
            tag.put(entry.getKey(), bonusList);
        }

        return tag;
    }

    public void load(CompoundTag tag) {
        for (String stat : bonuses.keySet()) {
            if (tag.contains(stat)) {
                List<StatBonus> statBonuses = bonuses.get(stat);
                statBonuses.clear();

                ListTag bonusList = tag.getList(stat, Tag.TAG_COMPOUND);
                for (int i = 0; i < bonusList.size(); i++) {
                    CompoundTag bonusTag = bonusList.getCompound(i);
                    String name = bonusTag.getString("Name");
                    String operation = bonusTag.getString("Operation");
                    double value = bonusTag.getDouble("Value");
                    boolean applyMultipliers = bonusTag.getBoolean("ApplyMultipliers");
                    statBonuses.add(new StatBonus(name, operation, value, applyMultipliers));
                }
            }
        }
    }

    public void copyFrom(BonusStats other) {
        for (Map.Entry<String, List<StatBonus>> entry : other.bonuses.entrySet()) {
            List<StatBonus> thisList = this.bonuses.get(entry.getKey());
            thisList.clear();
            for (StatBonus bonus : entry.getValue()) {
                thisList.add(new StatBonus(bonus.name, bonus.operation, bonus.value, bonus.applyMultipliers));
            }
        }
    }

    public static class StatBonus {
        public final String name;
        public final String operation;
        public final double value;
        public final boolean applyMultipliers;

        public StatBonus(String name, String operation, double value, boolean applyMultipliers) {
            this.name = name;
            this.operation = operation;
            this.value = value;
            this.applyMultipliers = applyMultipliers;
        }
    }
}