package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class TrainingConfig {
	public static final int CURRENT_VERSION = 4;

	@Setter
	private int configVersion;

	private double rewardBaseCoefficient = 3.4;
	private double rewardCostExponent = 0.6;

	private RhythmConfig rhythm = new RhythmConfig();
	private ControlConfig control = new ControlConfig();
	private MemoryConfig memory = new MemoryConfig();
	private PrecisionConfig precision = new PrecisionConfig();
	private GravityConfig gravity = new GravityConfig();

	public float computeTpsPerLevel(int singleStatCost, MinigameSettings settings) {
		double tpc = Math.max(1.0, singleStatCost);
		double perLevel = rewardBaseCoefficient * Math.pow(tpc, rewardCostExponent);
		double multiplier = settings != null ? settings.getRewardMultiplier() : 1.0;
		return (float) Math.max(1.0, perLevel * multiplier);
	}

	public MinigameSettings getSettings(String minigameId) {
		if (minigameId == null) return rhythm;
		return switch (minigameId.toLowerCase()) {
			case "control" -> control;
			case "memory" -> memory;
			case "precision" -> precision;
			case "gravity" -> gravity;
			default -> rhythm;
		};
	}

	@Getter
	@NoArgsConstructor
	public static class MinigameSettings {
		protected double rewardMultiplier = 1.0;
		protected float tpsLimitPerGame = 50000f;
		protected boolean unlockedByDefault = false;
		protected String masterName = "a master";
	}

	@Getter
	@NoArgsConstructor
	public static class RhythmConfig extends MinigameSettings {
		{ masterName = "popo"; }

		private double baseNoteSpeed = 4.0;
		private double noteSpeedPerLevel = 0.5;
		private int noteTravelDistance = 150;
		private float arrowScale = 2.0f;
		private int baseSpawnIntervalTicks = 20;
		private int minSpawnIntervalTicks = 8;
		private int spawnIntervalDecreasePerLevel = 2;
		private int perfectWindow = 14;
		private int goodWindow = 30;

		private double progressMax = 100.0;
		private double progressOnLevelUp = 30.0;
		private double progressGainPerfect = 18.0;
		private double progressGainGood = 12.0;
		private double progressGainHold = 22.0;
		private double progressDecayPerTick = 0.28;
		private double progressLossOnMiss = 10.0;
		private int loseMissThreshold = 5;
		private int loseMissWindow = 10;

		private double holdNoteChance = 0.18;
		private int holdDurationTicks = 24;

		private double doubleNoteChance = 0.12;
		private int doubleNoteGap = 26;
	}

	@Getter
	@NoArgsConstructor
	public static class ControlConfig extends MinigameSettings {
		{ masterName = "roshi"; }

		private int holdDurationTicks = 100;
		private int levelTimeLimitTicks = 300;
		private int barWidth = 200;
		private int baseZoneWidth = 60;
		private int zoneWidthDecreasePerLevel = 6;
		private int minZoneWidth = 18;
		private double baseZoneSpeed = 1.2;
		private double zoneSpeedPerLevel = 0.35;
		private double markerSpeed = 2.6;
		private double baseProgressLossPerTick = 0.8;
		private double progressLossPerLevel = 0.25;
	}

	@Getter
	@NoArgsConstructor
	public static class MemoryConfig extends MinigameSettings {
		{ masterName = "piccolo"; }

		private int baseSequenceLength = 3;
		private int sequenceLengthPerLevel = 1;
		private int baseShowTicks = 50;
		private int showTicksDecreasePerLevel = 4;
		private int minShowTicks = 16;
	}

	@Getter
	@NoArgsConstructor
	public static class PrecisionConfig extends MinigameSettings {
		{ masterName = "kingkai"; }

		private int outerRingRadius = 40;
		private int targetRadius = 14;
		private double baseRingSpeed = 0.7;
		private double ringSpeedPerLevel = 0.18;
		private int spawnIntervalTicks = 8;
		private int maxCircles = 4;
		private int perfectWindow = 4;
		private int goodWindow = 11;
		private int perfectPoints = 2;
		private int goodPoints = 1;
		private int missPenalty = 2;
		private int fadeOutTicks = 20;
		private int startingScore = 6;
		private double burstChance = 0.5;
		private int levelUpScoreBase = 12;
		private int levelUpScorePerLevel = 8;
		private int loseMissThreshold = 5;
		private int loseMissWindow = 10;
	}

	@Getter
	@NoArgsConstructor
	public static class GravityConfig extends MinigameSettings {
		{ masterName = "vegeta"; }

		private int holdDurationTicks = 200;
		private int barHeight = 180;
		private double controlLineFraction = 0.45;
		private double baseGravity = 1.1;
		private double gravityPerLevel = 0.25;
		private double risePerTap = 9.0;
		private double progressLossPerTick = 2.5;
		private double wrongPressDescentMultiplier = 3.0;
	}
}