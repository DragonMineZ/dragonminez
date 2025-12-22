package com.dragonminez.common.config;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class GeneralServerConfig {

    private WorldGenConfig worldGen = new WorldGenConfig();
    private GameplayConfig gameplay = new GameplayConfig();
	private StorageConfig storage = new StorageConfig();

    public WorldGenConfig getWorldGen() { return worldGen; }
    public void setWorldGen(WorldGenConfig worldGen) { this.worldGen = worldGen; }

    public GameplayConfig getGameplay() { return gameplay; }
    public void setGameplay(GameplayConfig gameplay) { this.gameplay = gameplay; }

	public StorageConfig getStorage() {return storage; }
	public void setStorage(StorageConfig storage) { this.storage = storage; }

    public static class WorldGenConfig {
        private boolean generateCustomStructures = true;
        private boolean generateDragonBalls = true;

        public boolean isGenerateCustomStructures() { return generateCustomStructures; }
        public void setGenerateCustomStructures(boolean generate) { this.generateCustomStructures = generate; }
        public boolean isGenerateDragonBalls() { return generateDragonBalls; }
        public void setGenerateDragonBalls(boolean generate) { this.generateDragonBalls = generate; }

    }

    public static class GameplayConfig {
        private double tpGainMultiplier = 1.0;
		private double tpCostMultiplier = 1.0;
        private boolean respectAttackCooldown = true;
        private int maxStatValue = 10000;
		private boolean kiDestroyBlocks = true;

        @SerializedName("kaioken_stackable")
        private boolean kaiokenStackable = true;

        @SerializedName("senzu_cooldown_ticks")
        private int senzuCooldownTicks = 240;

        @SerializedName("food_regenerations")
        private Map<String, float[]> foodRegenerations = createDefaultFoodRegenerations();

        @SerializedName("might_fruit_power")
        private double mightFruitPower = 1.2;

        @SerializedName("majin_power")
        private double majinPower = 1.3;

        public double getTpsMultiplier() { return tpGainMultiplier; }
        public void setTpsMultiplier(double multiplier) { this.tpGainMultiplier = multiplier; }
		public double getTpCostMultiplier() { return tpCostMultiplier; }
		public void setTpCostMultiplier(double multiplier) { this.tpCostMultiplier = multiplier; }

        public boolean isRespectAttackCooldown() { return respectAttackCooldown; }
        public void setRespectAttackCooldown(boolean respectAttackCooldown) { this.respectAttackCooldown = respectAttackCooldown; }

        public int getMaxStatValue() { return maxStatValue; }
        public void setMaxStatValue(int maxStatValue) { this.maxStatValue = maxStatValue; }

		public boolean isKiDestroyBlocks() { return kiDestroyBlocks; }
		public void setKiDestroyBlocks(boolean kiDestroyBlocks) { this.kiDestroyBlocks = kiDestroyBlocks; }

        public boolean isKaiokenStackable() { return kaiokenStackable; }
        public void setKaiokenStackable(boolean stackable) { this.kaiokenStackable = stackable; }

        public int getSenzuCooldownTicks() { return senzuCooldownTicks; }
        public void setSenzuCooldownTicks(int ticks) { this.senzuCooldownTicks = ticks; }

        public Map<String, float[]> getFoodRegenerations() { return foodRegenerations; }
        public void setFoodRegenerations(Map<String, float[]> foodRegenerations) { this.foodRegenerations = foodRegenerations; }

        public float[] getFoodRegeneration(String itemId) {
            return foodRegenerations.getOrDefault(itemId, new float[]{0.0f, 0.0f, 0.0f});
        }

        public double getMightFruitPower() { return mightFruitPower; }
        public void setMightFruitPower(double power) { this.mightFruitPower = power; }

        public double getMajinPower() { return majinPower; }
        public void setMajinPower(double power) { this.majinPower = power; }

        private static Map<String, float[]> createDefaultFoodRegenerations() {
            Map<String, float[]> defaults = new HashMap<>();
            defaults.put("dragonminez:raw_dino_meat", new float[]{0.15f, 0.10f, 0.10f});
            defaults.put("dragonminez:cooked_dino_meat", new float[]{0.30f, 0.20f, 0.20f});
            defaults.put("dragonminez:dino_tail_raw", new float[]{0.12f, 0.08f, 0.08f});
            defaults.put("dragonminez:dino_tail_cooked", new float[]{0.25f, 0.15f, 0.15f});
            defaults.put("dragonminez:frog_legs_raw", new float[]{0.08f, 0.12f, 0.08f});
            defaults.put("dragonminez:frog_legs_cooked", new float[]{0.15f, 0.25f, 0.15f});
            defaults.put("dragonminez:senzu_bean", new float[]{1.0f, 1.0f, 1.0f});
			defaults.put("dragonminez:heart_medicine", new float[]{1.0f, 1.0f, 1.0f});
            defaults.put("dragonminez:might_tree_fruit", new float[]{0.05f, 0.35f, 0.0f});
            return defaults;
        }
    }

	public static class StorageConfig {
		public enum StorageType {
			NBT, JSON, DATABASE
		}

		@SerializedName("storage_type")
		private StorageType storageType = StorageType.NBT;

		private String host = "localhost";
		private int port = 3306;
		private String database = "dragonminez";
		private String table = "player_data";
		private String username = "root";
		private String password = "password";

		@SerializedName("pool_size")
		private int poolSize = 10;

		public StorageType getStorageType() { return storageType; }
		public String getHost() { return host; }
		public int getPort() { return port; }
		public String getDatabase() { return database; }
		public String getTable() { return table; }
		public String getUsername() { return username; }
		public String getPassword() { return password; }
		public int getPoolSize() { return poolSize; }
	}
}

