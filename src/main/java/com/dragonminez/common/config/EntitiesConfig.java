package com.dragonminez.common.config;

import java.util.HashMap;
import java.util.Map;

public class EntitiesConfig {
	private HardModeSettings hardModeSettings = new HardModeSettings();
	private Map<String, EntityStats> entityStats = new HashMap<>();

	public Map<String, EntityStats> getEntityStats() {
		return entityStats;
	}

	public HardModeSettings getHardModeSettings() {
		return hardModeSettings;
	}

	public static class HardModeSettings {
		private double hpMultiplier = 3.0;
		private double damageMultiplier = 2.0;

		public double getHpMultiplier() { return Math.max(1, hpMultiplier); }
		public void setHpMultiplier(double hpMultiplier) { this.hpMultiplier = hpMultiplier; }
		public double getDamageMultiplier() { return Math.max(1, damageMultiplier); }
		public void setDamageMultiplier(double damageMultiplier) { this.damageMultiplier = damageMultiplier; }
	}

	public static class EntityStats {
		private Double health;
		private Double meleeDamage;
		private Double kiDamage;

		public Double getHealth() { return health != null ? Math.max(1, health) : null; }
		public void setHealth(Double health) { this.health = health; }
		public Double getMeleeDamage() { return meleeDamage != null ? Math.max(1, meleeDamage) : null; }
		public void setMeleeDamage(Double meleeDamage) { this.meleeDamage = meleeDamage; }
		public Double getKiDamage() { return kiDamage != null ? Math.max(1, kiDamage) : null; }
		public void setKiDamage(Double kiDamage) { this.kiDamage = kiDamage; }
	}
}
