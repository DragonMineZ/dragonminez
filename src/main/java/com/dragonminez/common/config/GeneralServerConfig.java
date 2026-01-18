package com.dragonminez.common.config;

import java.util.HashMap;
import java.util.Map;

public class GeneralServerConfig {

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
		private int dbSpawnRange = 3000;

        public boolean isGenerateCustomStructures() { return generateCustomStructures; }
        public boolean isGenerateDragonBalls() { return generateDragonBalls; }
		public boolean isOtherworldActive() { return otherworldActive; }
		public int getDBSpawnRange() { return Math.max(500, dbSpawnRange); }

    }

    public static class GameplayConfig {
        private double tpGainMultiplier = 1.0;
		private double tpCostMultiplier = 1.0;
		private double tpHealthRatio = 0.10;
		private int tpPerHit = 2;
		private double HTCTpMultiplier = 2.5;
        private int maxStatValue = 10000;
		private boolean storyModeEnabled = true;
		private boolean createDefaultSagas = true;
        private boolean kaiokenStackable = true;
        private int senzuCooldownTicks = 240;
        private Map<String, float[]> foodRegenerations = createDefaultFoodRegenerations();
        private double mightFruitPower = 1.2;
        private double majinPower = 1.3;

        public double getTpsMultiplier() { return Math.max(0, tpGainMultiplier); }
		public double getTpCostMultiplier() { return Math.max(0.01, tpCostMultiplier); }
		public double getTpHealthRatio() { return Math.max(0, tpHealthRatio); }
		public int getTpPerHit() { return Math.max(0, tpPerHit); }
		public double getHTCTpMultiplier() { return Math.max(1.0, HTCTpMultiplier); }
        public int getMaxStatValue() { return Math.max(1000, maxStatValue); }
		public boolean isStoryModeEnabled() { return storyModeEnabled; }
		public boolean isCreateDefaultSagas() { return createDefaultSagas; }
		public boolean isKaiokenStackable() { return kaiokenStackable; }
        public int getSenzuCooldownTicks() { return Math.max(0, senzuCooldownTicks); }

        public float[] getFoodRegeneration(String itemId) {
            return foodRegenerations.getOrDefault(itemId, new float[]{0.0f, 0.0f, 0.0f});
        }

        public double getMightFruitPower() { return Math.max(0, mightFruitPower); }
        public double getMajinPower() { return Math.max(0, majinPower); }

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
		private double staminaConsumptionRatio = 0.125;
		private boolean respectAttackCooldown = true;
		private boolean enableBlocking = true;
		private boolean enableParrying = true;
		private boolean enablePerfectAttack = true;
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

		public double getStaminaConsumptionRatio() { return Math.max(0, staminaConsumptionRatio); }
		public boolean isRespectAttackCooldown() { return respectAttackCooldown; }
		public boolean isEnableBlocking() { return enableBlocking; }
		public boolean isEnableParrying() { return enableParrying; }
		public boolean isEnablePerfectAttack() { return enablePerfectAttack; }
		public boolean isEnablePerfectEvasion() { return enablePerfectEvasion; }
		public int getParryWindowMs() { return Math.max(0, parryWindowMs); }
		public double getBlockDamageReductionCap() { return Math.max(0, blockDamageReductionCap); }
		public double getBlockDamageReductionMin() { return Math.max(0, blockDamageReductionMin); }
		public double getPoiseDamageMultiplier() { return Math.max(0, poiseDamageMultiplier); }
		public int getPoiseRegenCooldown() { return Math.max(0, poiseRegenCooldown); }
		public int getBlockBreakStunDurationTicks() { return Math.max(0, blockBreakStunDurationTicks); }
		public int getPerfectEvasionWindowMs() { return Math.max(0, perfectEvasionWindowMs); }
		public int getDashCooldownSeconds() { return Math.max(0, dashCooldownSeconds); }
		public int getDoubleDashCooldownSeconds() { return Math.max(0, doubleDashCooldownSeconds); }
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
		private int namekianAsimilationAmount = 4;
		private double namekianAsimilationHealthRegen = 0.35;
		private double namekianAsimilationStatBoost = 0.12;
		private boolean namekianAsimilationOnNpcs = true;
		private boolean frostDemonRacialSkill = true;
		private double frostDemonTPBoost = 1.25;
		private boolean bioAndroidRacialSkill = true;
		private int bioAndroidCooldownSeconds = 900;
		private double bioAndroidDrainRatio = 0.25;
		private boolean majinAbsoprtionSkill = true;
		private boolean majinReviveSkill = true;
		private int majinAbsorptionAmount = 3;
		private double majinAbsorptionHealthRegen = 0.30;
		private double majinAbsorptionStatBoost = 0.10;
		private int majinReviveCooldownSeconds = 3600;
		private double majinReviveHealthRatioPerBlop = 0.25;

		public boolean isEnableRacialSkills() { return enableRacialSkills; }
		public boolean isHumanRacialSkill() { return humanRacialSkill; }
		public double getHumanKiRegenBoost() { return Math.max(0, humanKiRegenBoost); }
		public boolean isSaiyanRacialSkill() { return saiyanRacialSkill; }
		public int getSaiyanZenkaiAmount() { return Math.max(0, saiyanZenkaiAmount); }
		public double getSaiyanZenkaiHealthRegen() { return Math.max(0, saiyanZenkaiHealthRegen); }
		public double getSaiyanZenkaiStatBoost() { return Math.max(0, saiyanZenkaiStatBoost); }
		public String[] getSaiyanZenkaiBoosts() { return saiyanZenkaiBoosts; }
		public int getSaiyanZenkaiCooldownSeconds() { return Math.max(0, saiyanZenkaiCooldownSeconds); }
		public boolean isNamekianRacialSkill() { return namekianRacialSkill; }
		public int getNamekianAsimilationAmount() { return Math.max(0, namekianAsimilationAmount); }
		public double getNamekianAsimilationHealthRegen() { return Math.max(0, namekianAsimilationHealthRegen); }
		public double getNamekianAsimilationStatBoost() { return Math.max(0, namekianAsimilationStatBoost); }
		public boolean isNamekianAsimilationOnNpcs() { return namekianAsimilationOnNpcs; }
		public boolean isFrostDemonRacialSkill() { return frostDemonRacialSkill; }
		public double getFrostDemonTPBoost() { return Math.max(1, frostDemonTPBoost); }
		public boolean isBioAndroidRacialSkill() { return bioAndroidRacialSkill; }
		public int getBioAndroidCooldownSeconds() { return Math.max(0, bioAndroidCooldownSeconds); }
		public double getBioAndroidDrainRatio() { return Math.max(0, bioAndroidDrainRatio); }
		public boolean isMajinAbsoprtionSkill() { return majinAbsoprtionSkill; }
		public boolean isMajinReviveSkill() { return majinReviveSkill; }
		public int getMajinAbsorptionAmount() { return Math.max(0, majinAbsorptionAmount); }
		public double getMajinAbsorptionHealthRegen() { return Math.max(0, majinAbsorptionHealthRegen); }
		public double getMajinAbsorptionStatBoost() { return Math.max(0, majinAbsorptionStatBoost); }
		public int getMajinReviveCooldownSeconds() { return Math.max(0, majinReviveCooldownSeconds); }
		public double getMajinReviveHealthRatioPerBlop() { return Math.max(0, majinReviveHealthRatioPerBlop); }
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
