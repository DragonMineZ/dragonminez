package com.dragonminez.common.training;

import com.dragonminez.common.config.TrainingConfig;

import java.util.Random;

public abstract class MinigameLogic {
	public static final int TICKS_PER_MINUTE = 1200;

	protected final TrainingConfig config;
	protected final TrainingConfig.MinigameSettings settings;
	protected final Random random;

	private int tick;
	private int levelsCleared;
	private boolean finished;
	private boolean targetReached;
	private int targetLevelsCleared = -1;
	private long heldMask;
	private double performance = 0.5;
	private double rewardUnits;

	protected MinigameLogic(TrainingConfig config, TrainingConfig.MinigameSettings settings, long seed) {
		this.config = config;
		this.settings = settings;
		this.random = new Random(seed);
	}

	public abstract String id();

	public void setTargetLevelsCleared(int target) {
		this.targetLevelsCleared = target;
	}

	public final void applyEvent(MinigameEvent event) {
		if (finished) return;
		switch (event.type) {
			case MinigameEvent.KEY_DOWN -> {
				long bit = 1L << event.key;
				if ((heldMask & bit) != 0) return;
				heldMask |= bit;
				onKeyDown(event.key, event.fraction());
			}
			case MinigameEvent.KEY_UP -> {
				long bit = 1L << event.key;
				if ((heldMask & bit) == 0) return;
				heldMask &= ~bit;
				onKeyUp(event.key, event.fraction());
			}
			case MinigameEvent.CLICK -> onClick(event.fieldX(), event.fieldY(), event.fraction());
			default -> {}
		}
	}

	public final void tick() {
		if (finished) return;
		tick++;
		tickGame();
		if (!finished) rewardUnits += rewardRate() / TICKS_PER_MINUTE;
	}

	public double rewardRate() {
		int cap = Math.max(2, settings.getDifficultyCapLevel());
		double levelProgress = Math.min(1.0, levelsCleared / (double) (cap - 1));
		double minLevel = config.getMinLevelRewardMultiplier();
		double levelMult = minLevel + (1.0 - minLevel) * levelProgress;
		double minPerf = config.getMinPerformanceRewardMultiplier();
		double maxPerf = config.getMaxPerformanceRewardMultiplier();
		double perfMult = minPerf + (maxPerf - minPerf) * performance;
		return Math.max(0.0, levelMult * perfMult);
	}

	protected final int difficulty() {
		return Math.min(levelsCleared, Math.max(0, settings.getDifficultyCapLevel() - 1));
	}

	protected final void levelUp() {
		levelsCleared++;
		if (targetLevelsCleared > 0 && levelsCleared >= targetLevelsCleared) {
			targetReached = true;
			finished = true;
			return;
		}
		onLevelUp();
	}

	protected final void fail() {
		finished = true;
	}

	protected final void complete() {
		finished = true;
	}

	public final void forceFinish() {
		finished = true;
	}

	protected final void samplePerformance(double sample, double weight) {
		performance += weight * (Math.max(0.0, Math.min(1.0, sample)) - performance);
	}

	protected final boolean isHeld(int key) {
		return (heldMask & (1L << key)) != 0;
	}

	public final boolean isInputHeld(int key) {
		return isHeld(key);
	}

	public int tickCount() {
		return tick;
	}

	public int levelsCleared() {
		return levelsCleared;
	}

	public int level() {
		return levelsCleared + 1;
	}

	public boolean isFinished() {
		return finished;
	}

	public boolean isTargetReached() {
		return targetReached;
	}

	public double performance() {
		return performance;
	}

	public double rewardUnits() {
		return rewardUnits;
	}

	public long checksum() {
		long h = 1125899906842597L;
		h = 31 * h + tick;
		h = 31 * h + levelsCleared;
		h = 31 * h + (finished ? 1 : 0);
		h = 31 * h + Math.round(rewardUnits * 1_000_000.0);
		h = 31 * h + gameChecksum();
		return h;
	}

	protected long gameChecksum() {
		return 0L;
	}

	protected abstract void tickGame();

	protected void onKeyDown(int key, float frac) {}

	protected void onKeyUp(int key, float frac) {}

	protected void onClick(float x, float y, float frac) {}

	protected void onLevelUp() {}
}
