package com.dragonminez.common.combat.clash;

import net.minecraft.util.Mth;

public final class ClashMeter {
	public static final int CYCLE_MIN_TICKS = 44;
	public static final int CYCLE_MAX_TICKS = 64;

	private static final float TARGET_PROGRESS_MIN = 0.42f;
	private static final float TARGET_PROGRESS_MAX = 0.86f;
	private static final float HALF_WIDTH_MIN = 0.065f;
	private static final float HALF_WIDTH_MAX = 0.09f;

	public static final float PERFECT_FRACTION = 0.35f;

	public static final float GOOD_MIN_EFFICIENCY = 0.5f;

	private static final int MAX_CYCLES = 64;

	public enum Grade { MISS, GOOD, PERFECT }

	public record Cycle(int index, int startTick, int duration, float center, float halfWidth, boolean reversed) {
		public int endTick() {
			return startTick + duration;
		}

		public float low() {
			return center - halfWidth;
		}

		public float high() {
			return center + halfWidth;
		}

		public float markerAt(float time) {
			float phase = Mth.clamp((time - startTick) / (float) duration, 0.0f, 1.0f);
			return reversed ? 1.0f - phase : phase;
		}
	}

	public record Sample(Cycle cycle, float marker, float distance, Grade grade, float efficiency) {
	}

	private ClashMeter() {
	}

	public static Cycle cycleAt(long seed, float time) {
		int start = 0;
		Cycle cycle = null;
		for (int index = 0; index < MAX_CYCLES; index++) {
			cycle = build(seed, index, start);
			if (time < cycle.endTick()) return cycle;
			start = cycle.endTick();
		}
		return cycle;
	}

	private static Cycle build(long seed, int index, int startTick) {
		int duration = CYCLE_MIN_TICKS + Math.round(hash01(seed, index, 0) * (CYCLE_MAX_TICKS - CYCLE_MIN_TICKS));
		float progress = Mth.lerp(hash01(seed, index, 1), TARGET_PROGRESS_MIN, TARGET_PROGRESS_MAX);
		float halfWidth = Mth.lerp(hash01(seed, index, 2), HALF_WIDTH_MIN, HALF_WIDTH_MAX);
		boolean reversed = (index & 1) == 1;
		float center = reversed ? 1.0f - progress : progress;
		return new Cycle(index, startTick, duration, center, halfWidth, reversed);
	}

	public static Sample sample(long seed, float time) {
		Cycle cycle = cycleAt(seed, time);
		float marker = cycle.markerAt(time);
		float distance = Math.abs(marker - cycle.center()) / cycle.halfWidth();
		Grade grade;
		float efficiency;
		if (distance <= PERFECT_FRACTION) {
			grade = Grade.PERFECT;
			efficiency = 1.0f;
		} else if (distance <= 1.0f) {
			grade = Grade.GOOD;
			float t = (distance - PERFECT_FRACTION) / (1.0f - PERFECT_FRACTION);
			efficiency = 1.0f - (1.0f - GOOD_MIN_EFFICIENCY) * t;
		} else {
			grade = Grade.MISS;
			efficiency = 0.0f;
		}
		return new Sample(cycle, marker, distance, grade, efficiency);
	}

	public static float hash01(long seed, int index, int salt) {
		long z = seed + 0x9E3779B97F4A7C15L * (index + 1L) + 0xD1B54A32D192ED03L * (salt + 1L);
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		z = z ^ (z >>> 31);
		return (z >>> 40) / (float) (1L << 24);
	}
}
