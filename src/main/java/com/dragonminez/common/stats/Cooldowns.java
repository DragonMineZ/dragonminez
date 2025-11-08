package com.dragonminez.common.stats;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class Cooldowns {
    private final Map<String, Integer> cooldowns;

    public static final String SENZU_DAILY = "SenzuDaily";
    public static final String BABA = "Baba";
    public static final String BABA_ALIVE = "BabaAlive";
    public static final String ZENKAI = "Zenkai";

    public Cooldowns() {
        this.cooldowns = new HashMap<>();
    }

    public int getCooldown(String key) {
        return cooldowns.getOrDefault(key, 0);
    }

    public void setCooldown(String key, int value) {
        if (value <= 0) {
            cooldowns.remove(key);
        } else {
            cooldowns.put(key, value);
        }
    }

    public void addCooldown(String key, int amount) {
        int current = getCooldown(key);
        setCooldown(key, current + amount);
    }

    public void reduceCooldown(String key, int amount) {
        int current = getCooldown(key);
        setCooldown(key, Math.max(0, current - amount));
    }

    public boolean hasCooldown(String key) {
        return getCooldown(key) > 0;
    }

    public void tick() {
        cooldowns.replaceAll((key, value) -> Math.max(0, value - 1));
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= 0);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        cooldowns.forEach(tag::putInt);
        return tag;
    }

    public void load(CompoundTag tag) {
        cooldowns.clear();
        for (String key : tag.getAllKeys()) {
            cooldowns.put(key, tag.getInt(key));
        }
    }

    public void copyFrom(Cooldowns other) {
        this.cooldowns.clear();
        this.cooldowns.putAll(other.cooldowns);
    }

    public Map<String, Integer> getAllCooldowns() {
        return new HashMap<>(cooldowns);
    }
}

