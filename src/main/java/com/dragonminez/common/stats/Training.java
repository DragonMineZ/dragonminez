package com.dragonminez.common.stats;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class Training {
	private final Map<String, Long> trainingCooldowns = new HashMap<>();
	private final Map<String, Integer> trainingPointsObtained = new HashMap<>();

	private static final long TRAINING_WINDOW = 3 * 60 * 60 * 1000L;
	private static final int MAX_TRAINING_POINTS = 20;

	public Training() {}

	public boolean canTrain(String stat) {
		long now = System.currentTimeMillis();
		long lastTime = trainingCooldowns.getOrDefault(stat, 0L);

		if (now - lastTime > TRAINING_WINDOW) {
			trainingCooldowns.put(stat, now);
			trainingPointsObtained.put(stat, 0);
			return true;
		}

		return trainingPointsObtained.getOrDefault(stat, 0) < MAX_TRAINING_POINTS;
	}

	public void addTrainingPoints(String stat, int points) {
		long now = System.currentTimeMillis();
		long lastTime = trainingCooldowns.getOrDefault(stat, 0L);

		if (now - lastTime > TRAINING_WINDOW) {
			trainingCooldowns.put(stat, now);
			trainingPointsObtained.put(stat, points);
		} else {
			int current = trainingPointsObtained.getOrDefault(stat, 0);
			trainingPointsObtained.put(stat, current + points);
		}
	}

	public long getTrainingCooldownRemaining(String stat) {
		long now = System.currentTimeMillis();
		long start = trainingCooldowns.getOrDefault(stat, 0L);
		long elapsed = now - start;
		if (elapsed > TRAINING_WINDOW) return 0;

		if (trainingPointsObtained.getOrDefault(stat, 0) >= MAX_TRAINING_POINTS) {
			return TRAINING_WINDOW - elapsed;
		}
		return 0;
	}

	public int getTrainingPoints(String stat) {
		return trainingPointsObtained.getOrDefault(stat, 0);
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		CompoundTag cooldownsTag = new CompoundTag();
		CompoundTag pointsTag = new CompoundTag();

		for (Map.Entry<String, Long> entry : trainingCooldowns.entrySet()) {
			cooldownsTag.putLong(entry.getKey(), entry.getValue());
		}
		for (Map.Entry<String, Integer> entry : trainingPointsObtained.entrySet()) {
			pointsTag.putInt(entry.getKey(), entry.getValue());
		}

		tag.put("TrainingCooldowns", cooldownsTag);
		tag.put("TrainingPoints", pointsTag);
		return tag;
	}

	public void load(CompoundTag tag) {
		CompoundTag cooldownsTag = tag.getCompound("TrainingCooldowns");
		CompoundTag pointsTag = tag.getCompound("TrainingPoints");

		for (String key : cooldownsTag.getAllKeys()) {
			trainingCooldowns.put(key, cooldownsTag.getLong(key));
		}
		for (String key : pointsTag.getAllKeys()) {
			trainingPointsObtained.put(key, pointsTag.getInt(key));
		}
	}

	public void copyFrom(Training other) {
		this.trainingCooldowns.clear();
		this.trainingCooldowns.putAll(other.trainingCooldowns);
		this.trainingPointsObtained.clear();
		this.trainingPointsObtained.putAll(other.trainingPointsObtained);
	}
}
