package com.dragonminez.common.stats.character;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainAttributes;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

@Setter
@Getter
public class Stats {
    private Player player;

    public Stats() {
    }

    private int clampStatValue(int value) {
        int min = 5;
        int capped = Math.max(min, value);
        if (ConfigManager.getServerConfig() == null || ConfigManager.getServerConfig().getGameplay() == null) {
            return capped;
        }
        if (ConfigManager.getServerConfig().getGameplay().getMaxLevelValueInsteadOfStats()) {
            return capped;
        }
        int max = ConfigManager.getServerConfig().getGameplay().getMaxValue();
        return Math.min(capped, max);
    }

    private int safeAdd(int base, int delta) {
        long result = (long) base + delta;
        if (result > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (result < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) result;
    }

    private int getAttributeBaseValue(Attribute attribute, int fallback) {
        if (player == null) return fallback;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return fallback;
        return (int) Math.round(instance.getBaseValue());
    }

    private void setAttributeBaseValue(Attribute attribute, int value) {
        if (player == null) return;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    public int getStrength() {
        return getAttributeBaseValue(MainAttributes.STRENGTH.get(), 5);
    }

    public int getStrikePower() {
        return getAttributeBaseValue(MainAttributes.STRIKE_POWER.get(), 5);
    }

    public int getResistance() {
        return getAttributeBaseValue(MainAttributes.RESISTANCE.get(), 5);
    }

    public int getVitality() {
        return getAttributeBaseValue(MainAttributes.VITALITY.get(), 5);
    }

    public int getKiPower() {
        return getAttributeBaseValue(MainAttributes.KI_POWER.get(), 5);
    }

    public int getEnergy() {
        return getAttributeBaseValue(MainAttributes.ENERGY.get(), 5);
    }

    public void setStrength(int value) {
        int oldValue = getStrength();
        int newValue = clampStatValue(value);
        if (oldValue != newValue && player != null) {
            DMZEvent.StatChangeEvent event = new DMZEvent.StatChangeEvent(player, DMZEvent.StatChangeEvent.StatType.STRENGTH, oldValue, newValue);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                setAttributeBaseValue(MainAttributes.STRENGTH.get(), newValue);
            }
        } else {
            setAttributeBaseValue(MainAttributes.STRENGTH.get(), newValue);
        }
    }

    public void setStrikePower(int value) {
        int oldValue = getStrikePower();
        int newValue = clampStatValue(value);
        if (oldValue != newValue && player != null) {
            DMZEvent.StatChangeEvent event = new DMZEvent.StatChangeEvent(player, DMZEvent.StatChangeEvent.StatType.STRIKE_POWER, oldValue, newValue);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                setAttributeBaseValue(MainAttributes.STRIKE_POWER.get(), newValue);
            }
        } else {
            setAttributeBaseValue(MainAttributes.STRIKE_POWER.get(), newValue);
        }
    }

    public void setResistance(int value) {
        int oldValue = getResistance();
        int newValue = clampStatValue(value);
        if (oldValue != newValue && player != null) {
            DMZEvent.StatChangeEvent event = new DMZEvent.StatChangeEvent(player, DMZEvent.StatChangeEvent.StatType.RESISTANCE, oldValue, newValue);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                setAttributeBaseValue(MainAttributes.RESISTANCE.get(), newValue);
            }
        } else {
            setAttributeBaseValue(MainAttributes.RESISTANCE.get(), newValue);
        }
    }

    public void setVitality(int value) {
        int oldValue = getVitality();
        int newValue = clampStatValue(value);
        if (oldValue != newValue && player != null) {
            DMZEvent.StatChangeEvent event = new DMZEvent.StatChangeEvent(player, DMZEvent.StatChangeEvent.StatType.VITALITY, oldValue, newValue);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                setAttributeBaseValue(MainAttributes.VITALITY.get(), newValue);
            }
        } else {
            setAttributeBaseValue(MainAttributes.VITALITY.get(), newValue);
        }
    }

    public void setKiPower(int value) {
        int oldValue = getKiPower();
        int newValue = clampStatValue(value);
        if (oldValue != newValue && player != null) {
            DMZEvent.StatChangeEvent event = new DMZEvent.StatChangeEvent(player, DMZEvent.StatChangeEvent.StatType.KI_POWER, oldValue, newValue);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                setAttributeBaseValue(MainAttributes.KI_POWER.get(), newValue);
            }
        } else {
            setAttributeBaseValue(MainAttributes.KI_POWER.get(), newValue);
        }
    }

    public void setEnergy(int value) {
        int oldValue = getEnergy();
        int newValue = clampStatValue(value);
        if (oldValue != newValue && player != null) {
            DMZEvent.StatChangeEvent event = new DMZEvent.StatChangeEvent(player, DMZEvent.StatChangeEvent.StatType.ENERGY, oldValue, newValue);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                setAttributeBaseValue(MainAttributes.ENERGY.get(), newValue);
            }
        } else {
            setAttributeBaseValue(MainAttributes.ENERGY.get(), newValue);
        }
    }

    public void addStrength(int amount) { setStrength(safeAdd(getStrength(), amount)); }
    public void addStrikePower(int amount) { setStrikePower(safeAdd(getStrikePower(), amount)); }
    public void addResistance(int amount) { setResistance(safeAdd(getResistance(), amount)); }
    public void addVitality(int amount) { setVitality(safeAdd(getVitality(), amount)); }
    public void addKiPower(int amount) { setKiPower(safeAdd(getKiPower(), amount)); }
    public void addEnergy(int amount) { setEnergy(safeAdd(getEnergy(), amount)); }

    public void setStat(String statName, int value) {
        switch (statName.toLowerCase()) {
            case "str" -> setStrength(value);
            case "skp" -> setStrikePower(value);
            case "res" -> setResistance(value);
            case "vit" -> setVitality(value);
            case "pwr" -> setKiPower(value);
            case "ene" -> setEnergy(value);
            default -> throw new IllegalArgumentException("Unknown stat: " + statName);
        }
    }

    public void addStat(String statName, int amount) {
        switch (statName.toLowerCase()) {
            case "str" -> addStrength(amount);
            case "skp" -> addStrikePower(amount);
            case "res" -> addResistance(amount);
            case "vit" -> addVitality(amount);
            case "pwr" -> addKiPower(amount);
            case "ene" -> addEnergy(amount);
            default -> throw new IllegalArgumentException("Unknown stat: " + statName);
        }
    }

    public void removeStat(String statName, int amount) {
        addStat(statName, -amount);
    }

    public int getTotalStats() {
        long total = (long) getStrength() + getStrikePower() + getResistance() + getVitality() + getKiPower() + getEnergy();
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("STR", getStrength());
        tag.putInt("SKP", getStrikePower());
        tag.putInt("RES", getResistance());
        tag.putInt("VIT", getVitality());
        tag.putInt("PWR", getKiPower());
        tag.putInt("ENE", getEnergy());
        return tag;
    }

    public void load(CompoundTag tag) {
        setStrength(tag.getInt("STR"));
        setStrikePower(tag.getInt("SKP"));
        setResistance(tag.getInt("RES"));
        setVitality(tag.getInt("VIT"));
        setKiPower(tag.getInt("PWR"));
        setEnergy(tag.getInt("ENE"));
    }

    public void copyFrom(Stats other) {
        setStrength(other.getStrength());
        setStrikePower(other.getStrikePower());
        setResistance(other.getResistance());
        setVitality(other.getVitality());
        setKiPower(other.getKiPower());
        setEnergy(other.getEnergy());
    }
}
