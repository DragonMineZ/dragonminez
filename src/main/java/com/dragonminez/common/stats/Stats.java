package com.dragonminez.common.stats;

import net.minecraft.nbt.CompoundTag;

public class Stats {
    private int strength;
    private int strikePower;
    private int resistance;
    private int vitality;
    private int kiPower;
    private int energy;

    public Stats() {
        this.strength = 5;
        this.strikePower = 5;
        this.resistance = 5;
        this.vitality = 5;
        this.kiPower = 5;
        this.energy = 5;
    }

    public int getStrength() { return strength; }
    public int getStrikePower() { return strikePower; }
    public int getResistance() { return resistance; }
    public int getVitality() { return vitality; }
    public int getKiPower() { return kiPower; }
    public int getEnergy() { return energy; }

    public void setStrength(int value) { this.strength = Math.max(5, value); }
    public void setStrikePower(int value) { this.strikePower = Math.max(5, value); }
    public void setResistance(int value) { this.resistance = Math.max(5, value); }
    public void setVitality(int value) { this.vitality = Math.max(5, value); }
    public void setKiPower(int value) { this.kiPower = Math.max(5, value); }
    public void setEnergy(int value) { this.energy = Math.max(5, value); }

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

