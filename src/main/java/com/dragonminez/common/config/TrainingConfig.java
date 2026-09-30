package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class TrainingConfig {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;

	@Setter
	private String configVersion;

	private double tpPerMinuteCoefficient = 62.0;
	private double rewardCostExponent = 0.6;
	private double minLevelRewardMultiplier = 0.5;
	private double minPerformanceRewardMultiplier = 0.75;
	private double maxPerformanceRewardMultiplier = 1.25;
	private double mentorBonus = 0.25;
	private int learnRequiredLevel = 5;
	private int learnRequiredRuns = 3;
	private int challengeTargetLevels = 10;

	private RhythmConfig rhythm = new RhythmConfig();
	private ControlConfig control = new ControlConfig();
	private MemoryConfig memory = new MemoryConfig();
	private PrecisionConfig precision = new PrecisionConfig();
	private GravityConfig gravity = new GravityConfig();

	public double computeTpsPerMinute(int singleStatCost, MinigameSettings settings) {
		double tpc = Math.max(1.0, singleStatCost);
		double perMinute = tpPerMinuteCoefficient * Math.pow(tpc, rewardCostExponent);
		double multiplier = settings != null ? settings.getRewardMultiplier() : 1.0;
		return Math.max(0.0, perMinute * multiplier);
	}

	public int getChallengeTargetLevels(String minigameId) {
		MinigameSettings settings = getSettings(minigameId);
		return settings.getChallengeTargetLevels() > 0 ? settings.getChallengeTargetLevels() : challengeTargetLevels;
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
		protected float tpsLimitPerGame = 0f;
		protected boolean unlockedByDefault = false;
		protected String masterName = "a master";
		protected int difficultyCapLevel = 20;
		protected int challengeTargetLevels = 0;
	}

	@Getter
	@NoArgsConstructor
	public static class RhythmConfig extends MinigameSettings {
		{ masterName = "popo"; difficultyCapLevel = 10; challengeTargetLevels = 6; }

		private boolean requireDiscInInventory = true;
		private int baseTravelMs = 1700;
		private int travelMsDecreasePerLevel = 90;
		private int minTravelMs = 900;
		private int perfectWindowMs = 50;
		private int goodWindowMs = 115;
		private int holdReleaseGraceMs = 140;
		private int densityTierEveryLevels = 2;
		private double recommendedMaxNotesPerSecond = 3.0;
		private double learnSongAccuracy = 0.75;
		private SongDifficulty easy = new SongDifficulty(0.20, 0.42, 1700, 1400, 0.8);
		private SongDifficulty normal = new SongDifficulty(0.35, 0.65, 1550, 1150, 1.1);
		private SongDifficulty hard = new SongDifficulty(0.55, 0.88, 1350, 950, 1.25);
		private SongDifficulty expert = new SongDifficulty(0.75, 1.0, 1150, 780, 1.45);

		private double progressMax = 100.0;
		private double progressOnLevelUp = 20.0;
		private double targetSecondsPerLevel = 12.0;
		private double goodProgressFactor = 0.7;
		private double holdProgressFactor = 0.5;
		private double progressDecayPerTick = 0.02;
		private double progressLossOnMiss = 4.0;

		private double startingHealth = 0.6;
		private double healthGainPerfect = 0.03;
		private double healthGainGood = 0.015;
		private double healthLossOnMiss = 0.08;
		private double healthLossOnGhost = 0.02;

		private double freeBaseBpm = 110.0;
		private double freeBpmPerLevel = 5.0;
		private double freeMaxBpm = 160.0;
		private double freeHoldChance = 0.12;
		private double freeChordChance = 0.06;
		private double songRewardMultiplier = 1.05;

		public SongDifficulty getDifficulty(int index) {
			return switch (index) {
				case 0 -> easy;
				case 2 -> hard;
				case 3 -> expert;
				default -> normal;
			};
		}
	}

	@Getter
	@NoArgsConstructor
	public static class SongDifficulty {
		private double startIntensity = 0.35;
		private double endIntensity = 0.65;
		private int startTravelMs = 1550;
		private int endTravelMs = 1150;
		private double rewardMultiplier = 1.0;

		public SongDifficulty(double startIntensity, double endIntensity, int startTravelMs, int endTravelMs, double rewardMultiplier) {
			this.startIntensity = startIntensity;
			this.endIntensity = endIntensity;
			this.startTravelMs = startTravelMs;
			this.endTravelMs = endTravelMs;
			this.rewardMultiplier = rewardMultiplier;
		}
	}

	@Getter
	@NoArgsConstructor
	public static class ControlConfig extends MinigameSettings {
		{ masterName = "krillin"; difficultyCapLevel = 20; }

		private int trackHeight = 200;
		private double baseBarHeight = 70.0;
		private double barHeightDecreasePerLevel = 1.3;
		private double minBarHeight = 46.0;
		private double liftAcceleration = 0.45;
		private double fallAcceleration = 0.40;
		private double maxBarSpeed = 6.0;
		private double bounceDamping = 0.35;
		private double orbSize = 12.0;
		private double baseOrbSpeed = 0.9;
		private double orbSpeedPerLevel = 0.06;
		private double orbAcceleration = 0.12;
		private int minRetargetTicks = 14;
		private int maxRetargetTicks = 44;
		private double baseDartChance = 0.003;
		private double dartChancePerLevel = 0.0008;
		private double dartSpeedMultiplier = 2.2;
		private double initialProgress = 0.30;
		private double progressOnLevelUp = 0.35;
		private double progressFillPerTick = 0.011;
		private double baseProgressDrainPerTick = 0.005;
		private double progressDrainPerLevel = 0.00015;
	}

	@Getter
	@NoArgsConstructor
	public static class MemoryConfig extends MinigameSettings {
		{ masterName = "gohan"; difficultyCapLevel = 20; }

		private int baseSequenceLength = 3;
		private int lengthEveryLevels = 2;
		private int maxSequenceLength = 8;
		private int baseTicksPerKey = 30;
		private double ticksPerKeyDecreasePerLevel = 0.9;
		private int minTicksPerKey = 13;
		private double keyTimeMultiplier = 2.2;
		private int wrongKeyDrainTicks = 8;
		private int fastDrainRate = 3;
		private int feedbackTicks = 10;
		private int lives = 3;
	}

	@Getter
	@NoArgsConstructor
	public static class PrecisionConfig extends MinigameSettings {
		{ masterName = "trunks"; difficultyCapLevel = 20; }

		private int outerRingRadius = 40;
		private int targetRadius = 14;
		private double baseRingSpeed = 0.42;
		private double ringSpeedPerLevel = 0.03;
		private int spawnIntervalTicks = 16;
		private int maxCircles = 3;
		private int startingMaxCircles = 2;
		private int maxCirclesFullLevel = 8;
		private int perfectWindow = 5;
		private int goodWindow = 13;
		private int perfectPoints = 2;
		private int goodPoints = 1;
		private int missPenalty = 2;
		private int fadeOutTicks = 20;
		private int startingScore = 6;
		private double burstChance = 0.5;
		private int scorePerLevel = 12;
		private int loseMissThreshold = 5;
		private int loseMissWindow = 10;
	}

	@Getter
	@NoArgsConstructor
	public static class GravityConfig extends MinigameSettings {
		{ masterName = "vegeta"; difficultyCapLevel = 20; }

		private double gravityAcceleration = 0.40;
		private double flapVelocity = 4.9;
		private double maxFallSpeed = 8.0;
		private double baseScrollSpeed = 3.25;
		private double scrollSpeedPerLevel = 0.035;
		private double baseGap = 72.0;
		private double gapDecreasePerLevel = 0.95;
		private double minGap = 54.0;
		private double baseSpacing = 150.0;
		private double spacingDecreasePerLevel = 2.0;
		private double minSpacing = 116.0;
		private double spacingRandom = 20.0;
		private double baseMaxGapShift = 50.0;
		private double maxGapShiftPerLevel = 1.5;
		private int pillarsPerLevel = 3;
		private int lives = 3;
		private int invulnerabilityTicks = 30;
	}
}
