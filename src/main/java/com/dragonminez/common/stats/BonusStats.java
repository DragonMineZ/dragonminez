package com.dragonminez.common.stats;

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
        initializeStat("RES");
        initializeStat("VIT");
        initializeStat("PWR");
        initializeStat("ENE");
    }

    private void initializeStat(String stat) {
        bonuses.put(stat, new ArrayList<>());
    }

    public void addBonus(String stat, String bonusName, String operation, double value) {
        stat = stat.toUpperCase();
        if (!bonuses.containsKey(stat)) {
            return;
        }

        List<StatBonus> statBonuses = bonuses.get(stat);

        statBonuses.removeIf(bonus -> bonus.name.equals(bonusName));

        statBonuses.add(new StatBonus(bonusName, operation, value));
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

    public void clearAll(String stat) {
        stat = stat.toUpperCase();
        if (!bonuses.containsKey(stat)) {
            return;
        }
        bonuses.get(stat).clear();
    }

    public void clearAllStats() {
        for (List<StatBonus> bonusList : bonuses.values()) {
            bonusList.clear();
        }
    }

    public double calculateBonus(String stat, int baseStat) {
        stat = stat.toUpperCase();
        if (!bonuses.containsKey(stat)) {
            return 0;
        }

        double result = 0;
        List<StatBonus> statBonuses = bonuses.get(stat);

        for (StatBonus bonus : statBonuses) {
            switch (bonus.operation) {
                case "+" -> result += bonus.value;
                case "-" -> result -= bonus.value;
                case "*" -> result += (baseStat * bonus.value) - baseStat;
            }
        }

        return result;
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
                    statBonuses.add(new StatBonus(name, operation, value));
                }
            }
        }
    }

    public void copyFrom(BonusStats other) {
        for (Map.Entry<String, List<StatBonus>> entry : other.bonuses.entrySet()) {
            List<StatBonus> thisList = this.bonuses.get(entry.getKey());
            thisList.clear();
            for (StatBonus bonus : entry.getValue()) {
                thisList.add(new StatBonus(bonus.name, bonus.operation, bonus.value));
            }
        }
    }

    public static class StatBonus {
        public final String name;
        public final String operation;
        public final double value;

        public StatBonus(String name, String operation, double value) {
            this.name = name;
            this.operation = operation;
            this.value = value;
        }
    }
}

