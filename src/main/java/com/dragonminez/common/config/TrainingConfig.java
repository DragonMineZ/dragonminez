package com.dragonminez.common.config;

public class TrainingConfig {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;
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


	public static class MinigameSettings {
		protected double rewardMultiplier = 1.0;
		protected float tpsLimitPerGame = 0.0F;
		protected boolean unlockedByDefault = false;
		protected String masterName = "a master";
		protected int difficultyCapLevel = 20;
		protected int challengeTargetLevels = 0;

		public double getRewardMultiplier() {
			return this.rewardMultiplier;
		}

		public float getTpsLimitPerGame() {
			return this.tpsLimitPerGame;
		}

		public boolean isUnlockedByDefault() {
			return this.unlockedByDefault;
		}

		public String getMasterName() {
			return this.masterName;
		}

		public int getDifficultyCapLevel() {
			return this.difficultyCapLevel;
		}

		public int getChallengeTargetLevels() {
			return this.challengeTargetLevels;
		}

		public MinigameSettings() {
		}
	}


	public static class RhythmConfig extends MinigameSettings {
		{
			masterName = "popo";
			difficultyCapLevel = 10;
			challengeTargetLevels = 6;
		}

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
		private SongDifficulty easy = new SongDifficulty(0.2, 0.42, 1700, 1400, 0.8);
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

		public boolean isRequireDiscInInventory() {
			return this.requireDiscInInventory;
		}

		public int getBaseTravelMs() {
			return this.baseTravelMs;
		}

		public int getTravelMsDecreasePerLevel() {
			return this.travelMsDecreasePerLevel;
		}

		public int getMinTravelMs() {
			return this.minTravelMs;
		}

		public int getPerfectWindowMs() {
			return this.perfectWindowMs;
		}

		public int getGoodWindowMs() {
			return this.goodWindowMs;
		}

		public int getHoldReleaseGraceMs() {
			return this.holdReleaseGraceMs;
		}

		public int getDensityTierEveryLevels() {
			return this.densityTierEveryLevels;
		}

		public double getRecommendedMaxNotesPerSecond() {
			return this.recommendedMaxNotesPerSecond;
		}

		public double getLearnSongAccuracy() {
			return this.learnSongAccuracy;
		}

		public SongDifficulty getEasy() {
			return this.easy;
		}

		public SongDifficulty getNormal() {
			return this.normal;
		}

		public SongDifficulty getHard() {
			return this.hard;
		}

		public SongDifficulty getExpert() {
			return this.expert;
		}

		public double getProgressMax() {
			return this.progressMax;
		}

		public double getProgressOnLevelUp() {
			return this.progressOnLevelUp;
		}

		public double getTargetSecondsPerLevel() {
			return this.targetSecondsPerLevel;
		}

		public double getGoodProgressFactor() {
			return this.goodProgressFactor;
		}

		public double getHoldProgressFactor() {
			return this.holdProgressFactor;
		}

		public double getProgressDecayPerTick() {
			return this.progressDecayPerTick;
		}

		public double getProgressLossOnMiss() {
			return this.progressLossOnMiss;
		}

		public double getStartingHealth() {
			return this.startingHealth;
		}

		public double getHealthGainPerfect() {
			return this.healthGainPerfect;
		}

		public double getHealthGainGood() {
			return this.healthGainGood;
		}

		public double getHealthLossOnMiss() {
			return this.healthLossOnMiss;
		}

		public double getHealthLossOnGhost() {
			return this.healthLossOnGhost;
		}

		public double getFreeBaseBpm() {
			return this.freeBaseBpm;
		}

		public double getFreeBpmPerLevel() {
			return this.freeBpmPerLevel;
		}

		public double getFreeMaxBpm() {
			return this.freeMaxBpm;
		}

		public double getFreeHoldChance() {
			return this.freeHoldChance;
		}

		public double getFreeChordChance() {
			return this.freeChordChance;
		}

		public double getSongRewardMultiplier() {
			return this.songRewardMultiplier;
		}

		public RhythmConfig() {
		}
	}


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

		public double getStartIntensity() {
			return this.startIntensity;
		}

		public double getEndIntensity() {
			return this.endIntensity;
		}

		public int getStartTravelMs() {
			return this.startTravelMs;
		}

		public int getEndTravelMs() {
			return this.endTravelMs;
		}

		public double getRewardMultiplier() {
			return this.rewardMultiplier;
		}

		public SongDifficulty() {
		}
	}


	public static class ControlConfig extends MinigameSettings {
		{
			masterName = "krillin";
			difficultyCapLevel = 20;
		}

		private int trackHeight = 200;
		private double baseBarHeight = 70.0;
		private double barHeightDecreasePerLevel = 1.3;
		private double minBarHeight = 46.0;
		private double liftAcceleration = 0.45;
		private double fallAcceleration = 0.4;
		private double maxBarSpeed = 6.0;
		private double bounceDamping = 0.35;
		private double orbSize = 12.0;
		private double baseOrbSpeed = 0.9;
		private double orbSpeedPerLevel = 0.06;
		private double orbAcceleration = 0.12;
		private int minRetargetTicks = 14;
		private int maxRetargetTicks = 44;
		private double baseDartChance = 0.003;
		private double dartChancePerLevel = 8.0E-4;
		private double dartSpeedMultiplier = 2.2;
		private double initialProgress = 0.3;
		private double progressOnLevelUp = 0.35;
		private double progressFillPerTick = 0.011;
		private double baseProgressDrainPerTick = 0.005;
		private double progressDrainPerLevel = 1.5E-4;

		public int getTrackHeight() {
			return this.trackHeight;
		}

		public double getBaseBarHeight() {
			return this.baseBarHeight;
		}

		public double getBarHeightDecreasePerLevel() {
			return this.barHeightDecreasePerLevel;
		}

		public double getMinBarHeight() {
			return this.minBarHeight;
		}

		public double getLiftAcceleration() {
			return this.liftAcceleration;
		}

		public double getFallAcceleration() {
			return this.fallAcceleration;
		}

		public double getMaxBarSpeed() {
			return this.maxBarSpeed;
		}

		public double getBounceDamping() {
			return this.bounceDamping;
		}

		public double getOrbSize() {
			return this.orbSize;
		}

		public double getBaseOrbSpeed() {
			return this.baseOrbSpeed;
		}

		public double getOrbSpeedPerLevel() {
			return this.orbSpeedPerLevel;
		}

		public double getOrbAcceleration() {
			return this.orbAcceleration;
		}

		public int getMinRetargetTicks() {
			return this.minRetargetTicks;
		}

		public int getMaxRetargetTicks() {
			return this.maxRetargetTicks;
		}

		public double getBaseDartChance() {
			return this.baseDartChance;
		}

		public double getDartChancePerLevel() {
			return this.dartChancePerLevel;
		}

		public double getDartSpeedMultiplier() {
			return this.dartSpeedMultiplier;
		}

		public double getInitialProgress() {
			return this.initialProgress;
		}

		public double getProgressOnLevelUp() {
			return this.progressOnLevelUp;
		}

		public double getProgressFillPerTick() {
			return this.progressFillPerTick;
		}

		public double getBaseProgressDrainPerTick() {
			return this.baseProgressDrainPerTick;
		}

		public double getProgressDrainPerLevel() {
			return this.progressDrainPerLevel;
		}

		public ControlConfig() {
		}
	}


	public static class MemoryConfig extends MinigameSettings {
		{
			masterName = "gohan";
			difficultyCapLevel = 20;
		}

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

		public int getBaseSequenceLength() {
			return this.baseSequenceLength;
		}

		public int getLengthEveryLevels() {
			return this.lengthEveryLevels;
		}

		public int getMaxSequenceLength() {
			return this.maxSequenceLength;
		}

		public int getBaseTicksPerKey() {
			return this.baseTicksPerKey;
		}

		public double getTicksPerKeyDecreasePerLevel() {
			return this.ticksPerKeyDecreasePerLevel;
		}

		public int getMinTicksPerKey() {
			return this.minTicksPerKey;
		}

		public double getKeyTimeMultiplier() {
			return this.keyTimeMultiplier;
		}

		public int getWrongKeyDrainTicks() {
			return this.wrongKeyDrainTicks;
		}

		public int getFastDrainRate() {
			return this.fastDrainRate;
		}

		public int getFeedbackTicks() {
			return this.feedbackTicks;
		}

		public int getLives() {
			return this.lives;
		}

		public MemoryConfig() {
		}
	}


	public static class PrecisionConfig extends MinigameSettings {
		{
			masterName = "trunks";
			difficultyCapLevel = 20;
		}

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

		public int getOuterRingRadius() {
			return this.outerRingRadius;
		}

		public int getTargetRadius() {
			return this.targetRadius;
		}

		public double getBaseRingSpeed() {
			return this.baseRingSpeed;
		}

		public double getRingSpeedPerLevel() {
			return this.ringSpeedPerLevel;
		}

		public int getSpawnIntervalTicks() {
			return this.spawnIntervalTicks;
		}

		public int getMaxCircles() {
			return this.maxCircles;
		}

		public int getStartingMaxCircles() {
			return this.startingMaxCircles;
		}

		public int getMaxCirclesFullLevel() {
			return this.maxCirclesFullLevel;
		}

		public int getPerfectWindow() {
			return this.perfectWindow;
		}

		public int getGoodWindow() {
			return this.goodWindow;
		}

		public int getPerfectPoints() {
			return this.perfectPoints;
		}

		public int getGoodPoints() {
			return this.goodPoints;
		}

		public int getMissPenalty() {
			return this.missPenalty;
		}

		public int getFadeOutTicks() {
			return this.fadeOutTicks;
		}

		public int getStartingScore() {
			return this.startingScore;
		}

		public double getBurstChance() {
			return this.burstChance;
		}

		public int getScorePerLevel() {
			return this.scorePerLevel;
		}

		public int getLoseMissThreshold() {
			return this.loseMissThreshold;
		}

		public int getLoseMissWindow() {
			return this.loseMissWindow;
		}

		public PrecisionConfig() {
		}
	}


	public static class GravityConfig extends MinigameSettings {
		{
			masterName = "vegeta";
			difficultyCapLevel = 20;
		}

		private double gravityAcceleration = 0.4;
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

		public double getGravityAcceleration() {
			return this.gravityAcceleration;
		}

		public double getFlapVelocity() {
			return this.flapVelocity;
		}

		public double getMaxFallSpeed() {
			return this.maxFallSpeed;
		}

		public double getBaseScrollSpeed() {
			return this.baseScrollSpeed;
		}

		public double getScrollSpeedPerLevel() {
			return this.scrollSpeedPerLevel;
		}

		public double getBaseGap() {
			return this.baseGap;
		}

		public double getGapDecreasePerLevel() {
			return this.gapDecreasePerLevel;
		}

		public double getMinGap() {
			return this.minGap;
		}

		public double getBaseSpacing() {
			return this.baseSpacing;
		}

		public double getSpacingDecreasePerLevel() {
			return this.spacingDecreasePerLevel;
		}

		public double getMinSpacing() {
			return this.minSpacing;
		}

		public double getSpacingRandom() {
			return this.spacingRandom;
		}

		public double getBaseMaxGapShift() {
			return this.baseMaxGapShift;
		}

		public double getMaxGapShiftPerLevel() {
			return this.maxGapShiftPerLevel;
		}

		public int getPillarsPerLevel() {
			return this.pillarsPerLevel;
		}

		public int getLives() {
			return this.lives;
		}

		public int getInvulnerabilityTicks() {
			return this.invulnerabilityTicks;
		}

		public GravityConfig() {
		}
	}

	public String getConfigVersion() {
		return this.configVersion;
	}

	public double getTpPerMinuteCoefficient() {
		return this.tpPerMinuteCoefficient;
	}

	public double getRewardCostExponent() {
		return this.rewardCostExponent;
	}

	public double getMinLevelRewardMultiplier() {
		return this.minLevelRewardMultiplier;
	}

	public double getMinPerformanceRewardMultiplier() {
		return this.minPerformanceRewardMultiplier;
	}

	public double getMaxPerformanceRewardMultiplier() {
		return this.maxPerformanceRewardMultiplier;
	}

	public double getMentorBonus() {
		return this.mentorBonus;
	}

	public int getLearnRequiredLevel() {
		return this.learnRequiredLevel;
	}

	public int getLearnRequiredRuns() {
		return this.learnRequiredRuns;
	}

	public int getChallengeTargetLevels() {
		return this.challengeTargetLevels;
	}

	public RhythmConfig getRhythm() {
		return this.rhythm;
	}

	public ControlConfig getControl() {
		return this.control;
	}

	public MemoryConfig getMemory() {
		return this.memory;
	}

	public PrecisionConfig getPrecision() {
		return this.precision;
	}

	public GravityConfig getGravity() {
		return this.gravity;
	}

	public TrainingConfig() {
	}

	public void setConfigVersion(final String configVersion) {
		this.configVersion = configVersion;
	}
}
