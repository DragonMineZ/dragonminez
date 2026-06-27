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
	public static final double CURRENT_VERSION = ConfigManager.CONFIG_VERSION;

	@Setter
	private double configVersion;

	private final Map<String, ClassStats> classes = new HashMap<>();

	public ClassStats getClassStats(String characterClass) {
		if (!this.classes.containsKey(characterClass)) {
			this.classes.put(characterClass, new ClassStats());
		}
		return this.classes.get(characterClass);
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
		private Double baseHp5 = 5.0;
		private Double hp5VitScaling = 0.05;

		private Double baseEp5 = 10.0;
		private Double ep5EneScaling = 0.1;

		private Double baseSp5 = 10.0;
		private Double sp5StmScaling = 0.1;
		private Double tpCostMultiplier = 1.0;
		private Double tpGainMultiplier = 1.0;
		private Passive passive = new Passive();
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
		@SerializedName("VIT_scaling")
		private Double vitalityScaling = 1.0;
		@SerializedName("PWR_scaling")
		private Double kiPowerScaling = 1.0;
		@SerializedName("ENE_scaling")
		private Double energyScaling = 1.0;
	}
}

