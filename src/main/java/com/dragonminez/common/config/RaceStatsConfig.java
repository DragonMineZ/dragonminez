package com.dragonminez.common.config;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public class RaceStatsConfig {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;

	@Setter
	private String configVersion;
	private final Map<String, ClassStats> classes = new HashMap<>();
	private static final String FALLBACK_CLASS = "warrior";
	private static final ClassStats SHARED_FALLBACK = new ClassStats();

	public ClassStats getClassStats(String characterClass) {
		ClassStats existing = characterClass != null ? this.classes.get(characterClass) : null;
		if (existing != null) return existing;
		ClassStats fallback = this.classes.get(FALLBACK_CLASS);
		return fallback != null ? fallback : SHARED_FALLBACK;
	}

	public ClassStats getOrCreateClassStats(String characterClass) {
		return this.classes.computeIfAbsent(characterClass, key -> new ClassStats());
	}

	public boolean hasClass(String characterClass) {
		return characterClass != null && this.classes.containsKey(characterClass);
	}

	public Collection<String> getAllClasses() {
		return this.classes.keySet();
	}

	@Setter
	@Getter
	@NoArgsConstructor
	public static class ClassStats {
		private BaseStats baseStats = new BaseStats();
		private StatScaling statScaling = new StatScaling();
		private Double baseHp5 = 1.25;
		private Double hp5VitScaling = 0.0375;

		private Double baseEp5 = 10.0;
		private Double ep5EneScaling = 0.2;

		private Double baseSp5 = 10.0;
		private Double sp5StmScaling = 0.1;
		private Double sp5ResScaling;
		private Double tpCostMultiplier = 1.0;
		private Double tpGainMultiplier = 1.0;
		private Passive passive = new Passive();

		public double resolveSp5VitScaling() {
			return sp5StmScaling != null ? sp5StmScaling : 0.0;
		}

		public double resolveSp5ResScaling() {
			return sp5ResScaling != null ? sp5ResScaling : 0.0;
		}
	}

	@Setter
	@Getter
	@NoArgsConstructor
	public static class Passive {
		private boolean enabled = true;
		private Map<String, Double> values = new HashMap<>();
	}

	@Setter
	@Getter
	@NoArgsConstructor
	public static class BaseStats {
		@SerializedName("STR")
		private Integer strength = 5;
		@SerializedName("SKP")
		private Integer strikePower = 5;
		@SerializedName("RES")
		private Integer resistance = 5;
		@SerializedName("VIT")
		private Integer vitality = 5;
		@SerializedName("PWR")
		private Integer kiPower = 5;
		@SerializedName("ENE")
		private Integer energy = 5;
	}

	@Setter
	@Getter
	@NoArgsConstructor
	public static class StatScaling {
		@SerializedName("STR_scaling")
		private Double strengthScaling = 1.0;
		@SerializedName("SKP_scaling")
		private Double strikePowerScaling = 1.0;
		@SerializedName("STM_scaling")
		private Double staminaScaling = 1.0;
		@SerializedName("DEF_scaling")
		private Double defenseScaling = 1.0;
		@SerializedName("DEF_scaling_max")
		private Double defenseScalingMax;
		@SerializedName("VIT_scaling")
		private Double vitalityScaling = 1.0;
		@SerializedName("VIT_scaling_max")
		private Double vitalityScalingMax;
		@SerializedName("PWR_scaling")
		private Double kiPowerScaling = 1.0;
		@SerializedName("ENE_scaling")
		private Double energyScaling = 1.0;
	}
}

