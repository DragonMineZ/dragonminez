package com.dragonminez.common.stats.character;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

@Setter
@Getter
public class Stats {
    private int strength;
    private int strikePower;
    private int resistance;
    private int vitality;
    private int kiPower;
    private int energy;

    private Player player;

    public Stats() {
        this.strength = 5;
        this.strikePower = 5;
        this.resistance = 5;
        this.vitality = 5;
        this.kiPower = 5;
        this.energy = 5;
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

    public void setStrength(int value) {
        int oldValue = this.strength;
        int newValue = clampStatValue(value);
        if (oldValue != newValue && player != null) {
            DMZEvent.StatChangeEvent event = new DMZEvent.StatChangeEvent(player, DMZEvent.StatChangeEvent.StatType.STRENGTH, oldValue, newValue);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                this.strength = newValue;
            }
        } else {
            this.strength = newValue;
        }
    }

    public void setStrikePower(int value) {
        int oldValue = this.strikePower;
        int newValue = clampStatValue(value);
        if (oldValue != newValue && player != null) {
            DMZEvent.StatChangeEvent event = new DMZEvent.StatChangeEvent(player, DMZEvent.StatChangeEvent.StatType.STRIKE_POWER, oldValue, newValue);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                this.strikePower = newValue;
            }
        } else {
            this.strikePower = newValue;
        }
    }

    public void setResistance(int value) {
        int oldValue = this.resistance;
        int newValue = clampStatValue(value);
        if (oldValue != newValue && player != null) {
            DMZEvent.StatChangeEvent event = new DMZEvent.StatChangeEvent(player, DMZEvent.StatChangeEvent.StatType.RESISTANCE, oldValue, newValue);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                this.resistance = newValue;
            }
        } else {
            this.resistance = newValue;
        }
    }

    public void setVitality(int value) {
        int oldValue = this.vitality;
        int newValue = clampStatValue(value);
        if (oldValue != newValue && player != null) {
            DMZEvent.StatChangeEvent event = new DMZEvent.StatChangeEvent(player, DMZEvent.StatChangeEvent.StatType.VITALITY, oldValue, newValue);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                this.vitality = newValue;
            }
        } else {
            this.vitality = newValue;
        }
    }

    public void setKiPower(int value) {
        int oldValue = this.kiPower;
        int newValue = clampStatValue(value);
        if (oldValue != newValue && player != null) {
            DMZEvent.StatChangeEvent event = new DMZEvent.StatChangeEvent(player, DMZEvent.StatChangeEvent.StatType.KI_POWER, oldValue, newValue);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                this.kiPower = newValue;
            }
        } else {
            this.kiPower = newValue;
        }
    }

    public void setEnergy(int value) {
        int oldValue = this.energy;
        int newValue = clampStatValue(value);
        if (oldValue != newValue && player != null) {
            DMZEvent.StatChangeEvent event = new DMZEvent.StatChangeEvent(player, DMZEvent.StatChangeEvent.StatType.ENERGY, oldValue, newValue);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                this.energy = newValue;
            }
        } else {
            this.energy = newValue;
        }
    }

      public void addStrength(int amount) { setStrength(safeAdd(strength, amount)); }
      public void addStrikePower(int amount) { setStrikePower(safeAdd(strikePower, amount)); }
      public void addResistance(int amount) { setResistance(safeAdd(resistance, amount)); }
      public void addVitality(int amount) { setVitality(safeAdd(vitality, amount)); }
      public void addKiPower(int amount) { setKiPower(safeAdd(kiPower, amount)); }
      public void addEnergy(int amount) { setEnergy(safeAdd(energy, amount)); }

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
        long total = (long) strength + strikePower + resistance + vitality + kiPower + energy;
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("STR", strength);
        tag.putInt("SKP", strikePower);
        tag.putInt("RES", resistance);
        tag.putInt("VIT", vitality);
        tag.putInt("PWR", kiPower);
        tag.putInt("ENE", energy);
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
        setStrength(other.strength);
        setStrikePower(other.strikePower);
        setResistance(other.resistance);
        setVitality(other.vitality);
        setKiPower(other.kiPower);
        setEnergy(other.energy);
    }
}

