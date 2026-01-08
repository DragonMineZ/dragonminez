package com.dragonminez.common.config;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class GeneralServerConfig {

    private WorldGenConfig worldGen = new WorldGenConfig();
    private GameplayConfig gameplay = new GameplayConfig();
	private CombatConfig combat = new CombatConfig();
	private StorageConfig storage = new StorageConfig();

    public WorldGenConfig getWorldGen() { return worldGen; }
    public GameplayConfig getGameplay() { return gameplay; }
	public CombatConfig getCombat() { return combat; }
	public StorageConfig getStorage() {return storage; }

    public static class WorldGenConfig {
        private boolean generateCustomStructures = true;
        private boolean generateDragonBalls = true;
		private boolean otherworldActive = true;
		private int dbSpawnRange = 3000;

        public boolean isGenerateCustomStructures() { return generateCustomStructures; }
        public boolean isGenerateDragonBalls() { return generateDragonBalls; }
		public boolean isOtherworldActive() { return otherworldActive; }
		public int getDBSpawnRange() { return dbSpawnRange; }

    }

    public static class GameplayConfig {
        private double tpGainMultiplier = 1.0;
		private double tpCostMultiplier = 1.0;
		private double tpHealthRatio = 0.10;
		private int tpPerHit = 2;
		private double HTCTpMultiplier = 2.5;
        private int maxStatValue = 10000;
		private boolean kiDestroyBlocks = true;
		private boolean storyModeEnabled = true;
		private boolean createDefaultSagas = true;
        private boolean kaiokenStackable = true;
        private int senzuCooldownTicks = 240;
        private Map<String, float[]> foodRegenerations = createDefaultFoodRegenerations();
        private double mightFruitPower = 1.2;
        private double majinPower = 1.3;

        public double getTpsMultiplier() { return tpGainMultiplier; }
		public double getTpCostMultiplier() { return tpCostMultiplier; }
		public double getTpHealthRatio() { return tpHealthRatio; }
		public int getTpPerHit() { return tpPerHit; }
		public double getHTCTpMultiplier() { return HTCTpMultiplier; }
        public int getMaxStatValue() { return maxStatValue; }
		public boolean isKiDestroyBlocks() { return kiDestroyBlocks; }
        public int getSenzuCooldownTicks() { return senzuCooldownTicks; }

        public float[] getFoodRegeneration(String itemId) {
            return foodRegenerations.getOrDefault(itemId, new float[]{0.0f, 0.0f, 0.0f});
        }

        public double getMightFruitPower() { return mightFruitPower; }
        public double getMajinPower() { return majinPower; }

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
		private int parryWindowMs = 300;
		private double blockDamageReductionCap = 0.80;
		private double blockDamageReductionMin = 0.25;
		private double poiseDamageMultiplier = 0.25;
		private int poiseRegenCooldown = 100;
		private int stunDurationTicks = 60;

		public double getStaminaConsumptionRatio() { return staminaConsumptionRatio; }
		public boolean isRespectAttackCooldown() { return respectAttackCooldown; }
		public boolean isEnableBlocking() { return enableBlocking; }
		public boolean isEnableParrying() { return enableParrying; }
		public boolean isEnablePerfectAttack() { return enablePerfectAttack; }
		public int getParryWindowMs() { return parryWindowMs; }
		public double getBlockDamageReductionCap() { return blockDamageReductionCap; }
		public double getBlockDamageReductionMin() { return blockDamageReductionMin; }
		public double getPoiseDamageMultiplier() { return poiseDamageMultiplier; }
		public int getPoiseRegenCooldown() { return poiseRegenCooldown; }
		public int getStunDurationTicks() { return stunDurationTicks; }
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
		public int getPoolSize() { return poolSize; }
		public int getThreadPoolSize() { return Math.max(1, threadPoolSize); }
	}
}

