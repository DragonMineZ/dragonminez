package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public class EntitiesConfig {
	public static final double CURRENT_VERSION = ConfigManager.CONFIG_VERSION;

	@Setter
	private double configVersion;

	private Map<String, EntityStats> defaultEntityStats = new HashMap<>();

	@Setter
	@Getter
	@NoArgsConstructor
	public static class EntityStats {
		private Double health;
		private Double meleeDamage;
		private Double kiDamage;

		public Double getHealth() {
			return health != null ? Math.max(1, health) : null;
		}

		public Double getMeleeDamage() {
			return meleeDamage != null ? Math.max(1, meleeDamage) : null;
		}

		public Double getKiDamage() {
			return kiDamage != null ? Math.max(1, kiDamage) : null;
		}
	}
}
