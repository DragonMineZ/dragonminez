package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public class EntitiesConfig {
	public static final int CURRENT_VERSION = 2;

	@Setter
	private int configVersion;

	private HardModeSettings hardModeSettings = new HardModeSettings();
	private Map<String, Map<String, EntityStats>> sagaEntityStats = new HashMap<>();

	@Setter
	@Getter
	@NoArgsConstructor
	public static class HardModeSettings {
		private Double hpMultiplier = 3.0;
		private Double damageMultiplier = 2.0;

		public Double getHpMultiplier() { return Math.max(1, hpMultiplier); }
        public Double getDamageMultiplier() { return Math.max(1, damageMultiplier); }
    }

	@Setter
	@Getter
	@NoArgsConstructor
	public static class EntityStats {
		private Double health;
		private Double meleeDamage;
		private Double kiDamage;

		public Double getHealth() { return health != null ? Math.max(1, health) : null; }
		public Double getMeleeDamage() { return meleeDamage != null ? Math.max(1, meleeDamage) : null; }
		public Double getKiDamage() { return kiDamage != null ? Math.max(1, kiDamage) : null; }
	}
}
