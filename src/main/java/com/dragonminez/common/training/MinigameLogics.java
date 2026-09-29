package com.dragonminez.common.training;

import com.dragonminez.common.config.TrainingConfig;

import java.util.List;

public final class MinigameLogics {
	public static final List<String> IDS = List.of(RhythmLogic.ID, KiControlLogic.ID, MemoryLogic.ID, PrecisionLogic.ID, GravityLogic.ID);
	public static final List<String> CHALLENGE_STAGES = List.of(KiControlLogic.ID, GravityLogic.ID, MemoryLogic.ID, PrecisionLogic.ID, RhythmLogic.ID);

	private MinigameLogics() {}

	public static boolean isValidId(String id) {
		return id != null && IDS.contains(id);
	}

	public static MinigameLogic create(String id, TrainingConfig config, long seed) {
		return create(id, config, seed, null);
	}

	public static MinigameLogic create(String id, TrainingConfig config, long seed, RhythmChart chart) {
		return switch (id) {
			case RhythmLogic.ID -> new RhythmLogic(config, seed, chart);
			case KiControlLogic.ID -> new KiControlLogic(config, seed);
			case MemoryLogic.ID -> new MemoryLogic(config, seed);
			case PrecisionLogic.ID -> new PrecisionLogic(config, seed);
			case GravityLogic.ID -> new GravityLogic(config, seed);
			default -> throw new IllegalArgumentException("Unknown minigame " + id);
		};
	}
}
