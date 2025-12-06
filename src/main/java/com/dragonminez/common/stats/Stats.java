package com.dragonminez.common.stats;

import com.dragonminez.common.events.DMZEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

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

    public void setPlayer(Player player) {
        this.player = player;
    }

    public int getStrength() { return strength; }
    public int getStrikePower() { return strikePower; }
    public int getResistance() { return resistance; }
    public int getVitality() { return vitality; }
    public int getKiPower() { return kiPower; }
    public int getEnergy() { return energy; }

    public void setStrength(int value) {
        int oldValue = this.strength;
        int newValue = Math.max(5, value);
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
        int newValue = Math.max(5, value);
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
        int newValue = Math.max(5, value);
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
        int newValue = Math.max(5, value);
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
        int newValue = Math.max(5, value);
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
        int newValue = Math.max(5, value);
        if (oldValue != newValue && player != null) {
            DMZEvent.StatChangeEvent event = new DMZEvent.StatChangeEvent(player, DMZEvent.StatChangeEvent.StatType.ENERGY, oldValue, newValue);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                this.energy = newValue;
            }
        } else {
            this.energy = newValue;
        }
    }

    public void addStrength(int amount) { setStrength(strength + amount); }
    public void addStrikePower(int amount) { setStrikePower(strikePower + amount); }
    public void addResistance(int amount) { setResistance(resistance + amount); }
    public void addVitality(int amount) { setVitality(vitality + amount); }
    public void addKiPower(int amount) { setKiPower(kiPower + amount); }
    public void addEnergy(int amount) { setEnergy(energy + amount); }

    public int getTotalStats() {
        return strength + strikePower + resistance + vitality + kiPower + energy;
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
        this.strength = tag.getInt("STR");
        this.strikePower = tag.getInt("SKP");
        this.resistance = tag.getInt("RES");
        this.vitality = tag.getInt("VIT");
        this.kiPower = tag.getInt("PWR");
        this.energy = tag.getInt("ENE");
    }

    public void copyFrom(Stats other) {
        this.strength = other.strength;
        this.strikePower = other.strikePower;
        this.resistance = other.resistance;
        this.vitality = other.vitality;
        this.kiPower = other.kiPower;
        this.energy = other.energy;
    }
}

