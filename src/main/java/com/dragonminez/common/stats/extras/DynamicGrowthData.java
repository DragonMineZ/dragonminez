package com.dragonminez.common.stats.extras;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DynamicGrowthData {
	private static final int MAX_TRACKED_TARGETS = 128;

	private final Map<String, Double> practiceXp = new HashMap<>();
	private final Map<String, TargetHistory> targetHistory = new HashMap<>();
	private long lastCombatMs;

	public double getPracticeXp(DynamicGrowthStat stat) {
		double value = practiceXp.getOrDefault(stat.key(), 0.0);
		return Double.isFinite(value) ? value : 0.0;
	}

	public void addPracticeXp(DynamicGrowthStat stat, double amount) {
		if (!Double.isFinite(amount) || amount <= 0.0) return;
		double updated = getPracticeXp(stat) + amount;
		practiceXp.put(stat.key(), Double.isFinite(updated) ? Math.max(0.0, updated) : 0.0);
	}

	public void consumePracticeXp(DynamicGrowthStat stat, double amount) {
		practiceXp.put(stat.key(), Math.max(0.0, getPracticeXp(stat) - amount));
	}

	public double recordTargetAndGetMultiplier(String targetKey, long nowMs, int windowSeconds,
											   int softCap, int hardCap, double softMultiplier, double hardMultiplier) {
		if (targetKey == null || targetKey.isEmpty()) return 1.0;

		long windowMs = (long) Math.max(1, windowSeconds) * 1000L;
		TargetHistory history = targetHistory.computeIfAbsent(targetKey, key -> new TargetHistory(nowMs));

		if (nowMs - history.windowStartMs > windowMs) {
			history.windowStartMs = nowMs;
			history.count = 0;
		}

		history.count++;
		history.lastSeenMs = nowMs;
		pruneTargets();

		if (hardCap > 0 && history.count > hardCap) return hardMultiplier;
		if (softCap > 0 && history.count > softCap) return softMultiplier;
		return 1.0;
	}

	public void markCombat(long nowMs) {
		this.lastCombatMs = nowMs;
	}

	public boolean isRecentlyInCombat(long nowMs, long windowMs) {
		return lastCombatMs > 0L && nowMs - lastCombatMs <= windowMs;
	}

	public void clearCombatMemory() {
		targetHistory.clear();
		lastCombatMs = 0L;
	}

	public boolean hasProgress() {
		for (double xp : practiceXp.values()) {
			if (xp > 0.0) return true;
		}
		return false;
	}

	private void pruneTargets() {
		if (targetHistory.size() <= MAX_TRACKED_TARGETS) return;

		long oldestSeen = Long.MAX_VALUE;
		String oldestKey = null;
		for (Map.Entry<String, TargetHistory> entry : targetHistory.entrySet()) {
			if (entry.getValue().lastSeenMs < oldestSeen) {
				oldestSeen = entry.getValue().lastSeenMs;
				oldestKey = entry.getKey();
			}
		}
		if (oldestKey != null) targetHistory.remove(oldestKey);

		if (targetHistory.size() > MAX_TRACKED_TARGETS) {
			Iterator<String> iterator = targetHistory.keySet().iterator();
			while (targetHistory.size() > MAX_TRACKED_TARGETS && iterator.hasNext()) {
				iterator.next();
				iterator.remove();
			}
		}
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		CompoundTag xpTag = new CompoundTag();
		for (Map.Entry<String, Double> entry : practiceXp.entrySet()) {
			xpTag.putDouble(entry.getKey(), entry.getValue());
		}
		tag.put("PracticeXp", xpTag);
		return tag;
	}

	public void load(CompoundTag tag) {
		practiceXp.clear();
		CompoundTag xpTag = tag.getCompound("PracticeXp");
		for (String key : xpTag.getAllKeys()) {
			practiceXp.put(key, xpTag.getDouble(key));
		}
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeInt(practiceXp.size());
		for (Map.Entry<String, Double> entry : practiceXp.entrySet()) {
			buf.writeUtf(entry.getKey());
			buf.writeDouble(entry.getValue());
		}
	}

	public void fromBytes(FriendlyByteBuf buf) {
		practiceXp.clear();
		int size = buf.readInt();
		for (int i = 0; i < size; i++) {
			String key = buf.readUtf();
			double value = buf.readDouble();
			practiceXp.put(key, value);
		}
	}

	public void copyFrom(DynamicGrowthData other) {
		if (other == this) return;
		this.practiceXp.clear();
		this.practiceXp.putAll(other.practiceXp);
		this.targetHistory.clear();
		for (Map.Entry<String, TargetHistory> entry : other.targetHistory.entrySet()) {
			this.targetHistory.put(entry.getKey(), entry.getValue().copy());
		}
		this.lastCombatMs = other.lastCombatMs;
	}

	public void clear() {
		practiceXp.clear();
		targetHistory.clear();
		lastCombatMs = 0L;
	}

	private static class TargetHistory {
		private long windowStartMs;
		private long lastSeenMs;
		private int count;

		private TargetHistory(long windowStartMs) {
			this.windowStartMs = windowStartMs;
			this.lastSeenMs = windowStartMs;
		}

		private TargetHistory copy() {
			TargetHistory copy = new TargetHistory(windowStartMs);
			copy.lastSeenMs = lastSeenMs;
			copy.count = count;
			return copy;
		}
	}
}
