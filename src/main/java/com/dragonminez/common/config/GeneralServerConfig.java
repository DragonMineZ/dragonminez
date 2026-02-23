package com.dragonminez.common.config;

import java.util.HashMap;
import java.util.Map;

public class GeneralServerConfig {
	public static final int CURRENT_VERSION = 2;
	private int configVersion;
	public int getConfigVersion() { return configVersion; }
	public void setConfigVersion(int configVersion) { this.configVersion = configVersion; }

    private WorldGenConfig worldGen = new WorldGenConfig();
    private GameplayConfig gameplay = new GameplayConfig();
	private CombatConfig combat = new CombatConfig();
	private RacialSkillsConfig racialSkills = new RacialSkillsConfig();
	private StorageConfig storage = new StorageConfig();


    public WorldGenConfig getWorldGen() { return worldGen; }
    public GameplayConfig getGameplay() { return gameplay; }
	public CombatConfig getCombat() { return combat; }
	public RacialSkillsConfig getRacialSkills() { return racialSkills; }
	public StorageConfig getStorage() {return storage; }

    public static class WorldGenConfig {
        private boolean generateCustomStructures = true;
        private boolean generateDragonBalls = true;
		private boolean otherworldActive = true;
		private int dbSpawnRange = 1000;

        public boolean isGenerateCustomStructures() { return generateCustomStructures; }
        public boolean isGenerateDragonBalls() { return generateDragonBalls; }
		public boolean isOtherworldActive() { return otherworldActive; }
		public int getDBSpawnRange() { return Math.max(100, Math.min(dbSpawnRange, 6000)); }

    }

    public static class GameplayConfig {
		private boolean commandOutputOnConsole = true;
		private int reviveCooldownSeconds = 300;
        private double tpGainMultiplier = 1.0;
		private double tpCostMultiplier = 1.0;
		private double tpHealthRatio = 0.10;
		private int tpPerHit = 2;
		private double HTCTpMultiplier = 2.5;
        private int maxStatValue = 10000;
		private boolean storyModeEnabled = true;
		private boolean createDefaultSagas = true;
        private int senzuCooldownTicks = 240;
        private Map<String, float[]> foodRegenerations = createDefaultFoodRegenerations();
        private double mightFruitPower = 1.2;
        private double majinPower = 1.3;
		private double metamoruFusionThreshold = 0.5;
		private String[] fusionBoosts = {"STR", "SKP", "PWR"};
		private int fusionDurationSeconds = 900;
		private int fusionCooldownSeconds = 1800;
		private boolean multiplicationInsteadOfAdditionForMultipliers = false;

		public boolean isCommandOutputOnConsole() { return commandOutputOnConsole; }
		public int getReviveCooldownSeconds() { return Math.max(0, Math.min(reviveCooldownSeconds, Integer.MAX_VALUE)); }
        public double getTpsGainMultiplier() { return Math.max(0, Math.min(tpGainMultiplier, Double.MAX_VALUE)); }
		public double getTpCostMultiplier() { return Math.max(0.01, Math.min(tpCostMultiplier, Double.MAX_VALUE)); }
		public double getTpHealthRatio() { return Math.max(0, Math.min(tpHealthRatio, Double.MAX_VALUE)); }
		public int getTpPerHit() { return Math.max(0, Math.min(tpPerHit, Integer.MAX_VALUE)); }
		public double getHTCTpMultiplier() { return Math.max(1.0, Math.min(HTCTpMultiplier, Double.MAX_VALUE)); }
        public int getMaxStatValue() { return Math.max(1000, Math.min(maxStatValue, Integer.MAX_VALUE)); }
		public boolean isStoryModeEnabled() { return storyModeEnabled; }
		public boolean isCreateDefaultSagas() { return createDefaultSagas; }
        public int getSenzuCooldownTicks() { return Math.max(0, Math.min(senzuCooldownTicks, Integer.MAX_VALUE)); }

        public float[] getFoodRegeneration(String itemId) {
            return foodRegenerations.getOrDefault(itemId, new float[]{0.0f, 0.0f, 0.0f});
        }

        public double getMightFruitPower() { return Math.max(0, Math.min(mightFruitPower, Double.MAX_VALUE)); }
        public double getMajinPower() { return Math.max(0, Math.min(majinPower, Double.MAX_VALUE)); }
		public double getMetamoruFusionThreshold() { return Math.max(0, Math.min(metamoruFusionThreshold, Double.MAX_VALUE)); }
		public String[] getFusionBoosts() { return fusionBoosts; }
		public int getFusionDurationSeconds() { return Math.max(0, Math.min(fusionDurationSeconds, Integer.MAX_VALUE)); }
		public int getFusionCooldownSeconds() { return Math.max(0, Math.min(fusionCooldownSeconds, Integer.MAX_VALUE)); }
		public boolean isMultiplicationInsteadOfAdditionForMultipliers() {
			return multiplicationInsteadOfAdditionForMultipliers;
		}

		private static Map<String, float[]> createDefaultFoodRegenerations() {
            Map<String, float[]> defaults = new HashMap<>();
            defaults.put("dragonminez:raw_dino_meat", new float[]{0.10f, 0.10f, 0.10f});
            defaults.put("dragonminez:cooked_dino_meat", new float[]{0.15f, 0.15f, 0.15f});
            defaults.put("dragonminez:dino_tail_raw", new float[]{0.15f, 0.15f, 0.15f});
            defaults.put("dragonminez:dino_tail_cooked", new float[]{0.20f, 0.20f, 0.20f});
            defaults.put("dragonminez:frog_legs_raw", new float[]{0.05f, 0.05f, 0.05f});
            defaults.put("dragonminez:frog_legs_cooked", new float[]{0.10f, 0.10f, 0.10f});
            defaults.put("dragonminez:senzu_bean", new float[]{1.0f, 1.0f, 1.0f});
			defaults.put("dragonminez:heart_medicine", new float[]{1.0f, 1.0f, 1.0f});
            defaults.put("dragonminez:might_tree_fruit", new float[]{0.035f, 0.35f, 0.35f});
            return defaults;
        }
    }

	public static class CombatConfig {
		private boolean killPlayersOnCombatLogout = true;
		private double staminaConsumptionRatio = 0.125;
		private boolean respectAttackCooldown = true;
		private boolean enableBlocking = true;
		private boolean enableParrying = true;
		private boolean enableComboAttacks = true;
		private int comboAttacksCooldownSeconds = 8;
		private boolean enablePerfectEvasion = true;
		private int parryWindowMs = 150;
		private double blockDamageReductionCap = 0.80;
		private double blockDamageReductionMin = 0.40;
		private double poiseDamageMultiplier = 0.25;
		private int poiseRegenCooldown = 100;
		private int blockBreakStunDurationTicks = 60;
		private int perfectEvasionWindowMs = 150;
		private int dashCooldownSeconds = 4;
		private int doubleDashCooldownSeconds = 12;
		private double[] kiBladeConfig = {1.0, 0.05};
		private double[] kiScytheConfig = {1.5, 0.075};
		private double[] kiClawLanceConfig = {2.0, 0.125};

		public boolean isKillPlayersOnCombatLogout() { return killPlayersOnCombatLogout; }
		public double getStaminaConsumptionRatio() { return Math.max(0, Math.min(staminaConsumptionRatio, Double.MAX_VALUE)); }
		public boolean isRespectAttackCooldown() { return respectAttackCooldown; }
		public boolean isEnableBlocking() { return enableBlocking; }
		public boolean isEnableParrying() { return enableParrying; }
		public boolean isEnableComboAttacks() { return enableComboAttacks; }
		public boolean isEnablePerfectEvasion() { return enablePerfectEvasion; }
		public int getComboAttacksCooldownSeconds() { return Math.max(0, Math.min(comboAttacksCooldownSeconds, Integer.MAX_VALUE)); }
		public int getParryWindowMs() { return Math.max(0, Math.min(parryWindowMs, Integer.MAX_VALUE)); }
		public double getBlockDamageReductionCap() { return Math.max(0, Math.min(blockDamageReductionCap, Double.MAX_VALUE)); }
		public double getBlockDamageReductionMin() { return Math.max(0, Math.min(blockDamageReductionMin, Double.MAX_VALUE)); }
		public double getPoiseDamageMultiplier() { return Math.max(0, Math.min(poiseDamageMultiplier, Double.MAX_VALUE)); }
		public int getPoiseRegenCooldown() { return Math.max(0, Math.min(poiseRegenCooldown, Integer.MAX_VALUE)); }
		public int getBlockBreakStunDurationTicks() { return Math.max(0, Math.min(blockBreakStunDurationTicks, Integer.MAX_VALUE)); }
		public int getPerfectEvasionWindowMs() { return Math.max(0, Math.min(perfectEvasionWindowMs, Integer.MAX_VALUE)); }
		public int getDashCooldownSeconds() { return Math.max(0, Math.min(dashCooldownSeconds, Integer.MAX_VALUE)); }
		public int getDoubleDashCooldownSeconds() { return Math.max(0, Math.min(doubleDashCooldownSeconds, Integer.MAX_VALUE)); }
		public double[] getKiBladeConfig() {return new double[] {Math.max(0, Math.min(kiBladeConfig[0], Double.MAX_VALUE)), Math.max(0, Math.min(kiBladeConfig[1], Double.MAX_VALUE))};}
		public double[] getKiScytheConfig() {return new double[] {Math.max(0, Math.min(kiScytheConfig[0], Double.MAX_VALUE)), Math.max(0, Math.min(kiScytheConfig[1], Double.MAX_VALUE))};}
		public double[] getKiClawLanceConfig() {return new double[] {Math.max(0, Math.min(kiClawLanceConfig[0], Double.MAX_VALUE)), Math.max(0, Math.min(kiClawLanceConfig[1], Double.MAX_VALUE))};}
	}

	public static class RacialSkillsConfig {
		private boolean enableRacialSkills = true;
		private boolean humanRacialSkill = true;
		private double humanKiRegenBoost = 1.40;
		private boolean saiyanRacialSkill = true;
		private int saiyanZenkaiAmount = 3;
		private double saiyanZenkaiHealthRegen = 0.20;
		private double saiyanZenkaiStatBoost = 0.10;
		private String[] saiyanZenkaiBoosts = {"STR", "SKP", "PWR"};
		private int saiyanZenkaiCooldownSeconds = 900;
		private boolean namekianRacialSkill = true;
		private int namekianAssimilationAmount = 4;
		private double namekianAssimilationHealthRegen = 0.35;
		private double namekianAssimilationStatBoost = 0.15;
		private String[] namekianAssimilationBoosts = {"STR", "SKP", "PWR"};
		private boolean namekianAssimilationOnNamekNpcs = true;
		private boolean frostDemonRacialSkill = true;
		private double frostDemonTPBoost = 1.25;
		private boolean bioAndroidRacialSkill = true;
		private int bioAndroidCooldownSeconds = 180;
		private double bioAndroidDrainRatio = 0.25;
		private boolean majinAbsoprtionSkill = true;
		private boolean majinReviveSkill = true;
		private int majinAbsorptionAmount = 3;
		private double majinAbsorptionHealthRegen = 0.30;
		private double majinAbsorptionStatsCopy = 0.10;
		private String[] majinAbsorptionBoosts = {"STR", "SKP", "PWR"};
		private boolean majinAbsorptionOnMobs = true;
		private int majinReviveCooldownSeconds = 3600;
		private double majinReviveHealthRatioPerBlop = 0.25;

		public boolean isEnableRacialSkills() { return enableRacialSkills; }
		public boolean isHumanRacialSkill() { return humanRacialSkill; }
		public double getHumanKiRegenBoost() { return Math.max(0, Math.min(humanKiRegenBoost, Double.MAX_VALUE)); }
		public boolean isSaiyanRacialSkill() { return saiyanRacialSkill; }
		public int getSaiyanZenkaiAmount() { return Math.max(0, Math.min(saiyanZenkaiAmount, Integer.MAX_VALUE)); }
		public double getSaiyanZenkaiHealthRegen() { return Math.max(0, Math.min(saiyanZenkaiHealthRegen, Double.MAX_VALUE)); }
		public double getSaiyanZenkaiStatBoost() { return Math.max(0, Math.min(saiyanZenkaiStatBoost, Double.MAX_VALUE)); }
		public String[] getSaiyanZenkaiBoosts() { return saiyanZenkaiBoosts; }
		public int getSaiyanZenkaiCooldownSeconds() { return Math.max(0, Math.min(saiyanZenkaiCooldownSeconds, Integer.MAX_VALUE)); }
		public boolean isNamekianRacialSkill() { return namekianRacialSkill; }
		public int getNamekianAssimilationAmount() { return Math.max(0, Math.min(namekianAssimilationAmount, Integer.MAX_VALUE)); }
		public double getNamekianAssimilationHealthRegen() { return Math.max(0, Math.min(namekianAssimilationHealthRegen, Double.MAX_VALUE)); }
		public double getNamekianAssimilationStatBoost() { return Math.max(0, Math.min(namekianAssimilationStatBoost, Double.MAX_VALUE)); }
		public String[] getNamekianAssimilationBoosts() { return namekianAssimilationBoosts; }
		public boolean isNamekianAssimilationOnNamekNpcs() { return namekianAssimilationOnNamekNpcs; }
		public boolean isFrostDemonRacialSkill() { return frostDemonRacialSkill; }
		public double getFrostDemonTPBoost() { return Math.max(1, Math.min(frostDemonTPBoost, Double.MAX_VALUE)); }
		public boolean isBioAndroidRacialSkill() { return bioAndroidRacialSkill; }
		public int getBioAndroidCooldownSeconds() { return Math.max(0, Math.min(bioAndroidCooldownSeconds, Integer.MAX_VALUE)); }
		public double getBioAndroidDrainRatio() { return Math.max(0, Math.min(bioAndroidDrainRatio, Double.MAX_VALUE)); }
		public boolean isMajinAbsoprtionSkill() { return majinAbsoprtionSkill; }
		public boolean isMajinReviveSkill() { return majinReviveSkill; }
		public int getMajinAbsorptionAmount() { return Math.max(0, Math.min(majinAbsorptionAmount, Integer.MAX_VALUE)); }
		public double getMajinAbsorptionHealthRegen() { return Math.max(0, Math.min(majinAbsorptionHealthRegen, Double.MAX_VALUE)); }
		public double getMajinAbsorptionStatCopy() { return Math.max(0, Math.min(majinAbsorptionStatsCopy, Double.MAX_VALUE)); }
		public String[] getMajinAbsorptionBoosts() { return majinAbsorptionBoosts; }
		public boolean isMajinAbsorptionOnMobs() { return majinAbsorptionOnMobs; }
		public int getMajinReviveCooldownSeconds() { return Math.max(0, Math.min(majinReviveCooldownSeconds, Integer.MAX_VALUE)); }
		public double getMajinReviveHealthRatioPerBlop() { return Math.max(0, Math.min(majinReviveHealthRatioPerBlop, Double.MAX_VALUE)); }
	}

	public static class StorageConfig {
		public enum StorageType {
			NBT, JSON, DATABASE
		}

		private StorageType storageType = StorageType.NBT;

		private String host = "localhost";
		private int port = 3306;
		private String database = "dragonminez";
		private String table = "player_data";
		private String username = "root";
		private String password = "password";

		private int poolSize = 10;
		private int threadPoolSize = 4;

		public StorageType getStorageType() { return storageType; }
		public String getHost() { return host; }
		public int getPort() { return port; }
		public String getDatabase() { return database; }
		public String getTable() { return table; }
		public String getUsername() { return username; }
		public String getPassword() { return password; }
		public int getPoolSize() { return Math.max(1, poolSize); }
		public int getThreadPoolSize() { return Math.max(1, threadPoolSize); }
	}
}