package com.dragonminez.common.config;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

public class EntitiesConfig {

	@SerializedName("hard_mode_settings")
	private HardModeSettings hardModeSettings = new HardModeSettings();

	@SerializedName("entity_stats")
	private Map<String, EntityStats> entityStats = new HashMap<>();

	public Map<String, EntityStats> getEntityStats() {
		return entityStats;
	}

	public HardModeSettings getHardModeSettings() {
		return hardModeSettings;
	}

	public static class HardModeSettings {
		@SerializedName("hp_multiplier")
		private double hpMultiplier = 3.0;

		@SerializedName("damage_multiplier")
		private double damageMultiplier = 2.0;

		public double getHpMultiplier() { return hpMultiplier; }
		public void setHpMultiplier(double val) { this.hpMultiplier = val; }

		public double getDamageMultiplier() { return damageMultiplier; }
		public void setDamageMultiplier(double val) { this.damageMultiplier = val; }
	}

	public static class EntityStats {
		@SerializedName("health")
		private Double health;
		@SerializedName("melee_damage")
		private Double meleeDamage;
		@SerializedName("ki_damage")
		private Double kiDamage;

		public Double getHealth() { return health; }
		public Double getMeleeDamage() { return meleeDamage; }
		public Double getKiDamage() { return kiDamage; }
		public void setHealth(Double health) { this.health = health; }
		public void setMeleeDamage(Double meleeDamage) { this.meleeDamage = meleeDamage; }
		public void setKiDamage(Double kiDamage) { this.kiDamage = kiDamage; }
	}
}