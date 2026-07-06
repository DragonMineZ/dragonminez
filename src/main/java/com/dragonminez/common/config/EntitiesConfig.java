package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public class EntitiesConfig {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;

	@Setter
	private String configVersion;

	private Map<String, EntityStats> defaultEntityStats = new HashMap<>();

	/**
	 * Global defaults applied when a saga enemy transforms into its next form.
	 * Per-quest KILL objectives may override these individually; when neither a
	 * quest override nor a config value is present, the {@code ...Or(...)} helpers
	 * fall back to the historical hard-coded behaviour (1.5x stats, transform at 50% HP).
	 */
	private TransformSettings transformDefaults = new TransformSettings();

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

	@Setter
	@NoArgsConstructor
	public static class TransformSettings {
		/** Multiplier applied to the previous form's max health when transforming. */
		private Double healthMultiplier;
		/** Multiplier applied to the previous form's melee (attack) damage when transforming. */
		private Double meleeMultiplier;
		/** Multiplier applied to the previous form's ki blast damage when transforming. */
		private Double kiMultiplier;
		/** Fraction of max health (0..1) at which the enemy triggers its transformation. */
		private Double triggerHealthPercent;

		public double healthMultiplierOr(double fallback) {
			return healthMultiplier != null ? Math.max(0.1D, healthMultiplier) : fallback;
		}

		public double meleeMultiplierOr(double fallback) {
			return meleeMultiplier != null ? Math.max(0.1D, meleeMultiplier) : fallback;
		}

		public double kiMultiplierOr(double fallback) {
			return kiMultiplier != null ? Math.max(0.1D, kiMultiplier) : fallback;
		}

		public double triggerHealthFractionOr(double fallback) {
			return triggerHealthPercent != null ? Mth.clamp(triggerHealthPercent, 0.0D, 1.0D) : fallback;
		}
	}
}
