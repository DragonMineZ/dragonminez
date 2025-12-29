package com.dragonminez.common.stats;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class Skill {
    private final String name;
    private int level;
    private boolean isActive;
    private final int maxLevel;

    public Skill(String name, int maxLevel) {
        this.name = name;
        this.level = 0;
        this.isActive = false;
        this.maxLevel = maxLevel;
    }

    public Skill(String name, int level, boolean isActive, int maxLevel) {
        this.name = name;
        this.level = level;
        this.isActive = isActive;
        this.maxLevel = maxLevel;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.min(level, maxLevel);
    }

    public void addLevel(int amount) {
        this.level = Math.min(this.level + amount, maxLevel);
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean isMaxLevel() {
        return level >= maxLevel;
    }

    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Name", name);
        nbt.putInt("Level", level);
        nbt.putBoolean("IsActive", isActive);
        nbt.putInt("MaxLevel", maxLevel);
        return nbt;
    }

    public static Skill load(CompoundTag nbt) {
        String name = nbt.getString("Name");
        int level = nbt.getInt("Level");
        boolean isActive = nbt.getBoolean("IsActive");
        int maxLevel = nbt.getInt("MaxLevel");
        return new Skill(name, level, isActive, maxLevel);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeInt(level);
        buf.writeBoolean(isActive);
        buf.writeInt(maxLevel);
    }

    public static Skill fromBytes(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        int level = buf.readInt();
        boolean isActive = buf.readBoolean();
        int maxLevel = buf.readInt();
        return new Skill(name, level, isActive, maxLevel);
    }

    public void copyFrom(Skill other) {
        this.level = other.level;
        this.isActive = other.isActive;
    }
}

