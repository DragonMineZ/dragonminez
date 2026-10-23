package com.dragonminez.common.config;

import com.google.gson.annotations.SerializedName;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RaceStatsConfig {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;
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

		public void setBaseStats(final BaseStats baseStats) {
			this.baseStats = baseStats;
		}

		public void setStatScaling(final StatScaling statScaling) {
			this.statScaling = statScaling;
		}

		public void setBaseHp5(final Double baseHp5) {
			this.baseHp5 = baseHp5;
		}

		public void setHp5VitScaling(final Double hp5VitScaling) {
			this.hp5VitScaling = hp5VitScaling;
		}

		public void setBaseEp5(final Double baseEp5) {
			this.baseEp5 = baseEp5;
		}

		public void setEp5EneScaling(final Double ep5EneScaling) {
			this.ep5EneScaling = ep5EneScaling;
		}

		public void setBaseSp5(final Double baseSp5) {
			this.baseSp5 = baseSp5;
		}

		public void setSp5StmScaling(final Double sp5StmScaling) {
			this.sp5StmScaling = sp5StmScaling;
		}

		public void setSp5ResScaling(final Double sp5ResScaling) {
			this.sp5ResScaling = sp5ResScaling;
		}

		public void setTpCostMultiplier(final Double tpCostMultiplier) {
			this.tpCostMultiplier = tpCostMultiplier;
		}

		public void setTpGainMultiplier(final Double tpGainMultiplier) {
			this.tpGainMultiplier = tpGainMultiplier;
		}

		public void setPassive(final Passive passive) {
			this.passive = passive;
		}

		public BaseStats getBaseStats() {
			return this.baseStats;
		}

		public StatScaling getStatScaling() {
			return this.statScaling;
		}

		public Double getBaseHp5() {
			return this.baseHp5;
		}

		public Double getHp5VitScaling() {
			return this.hp5VitScaling;
		}

		public Double getBaseEp5() {
			return this.baseEp5;
		}

		public Double getEp5EneScaling() {
			return this.ep5EneScaling;
		}

		public Double getBaseSp5() {
			return this.baseSp5;
		}

		public Double getSp5StmScaling() {
			return this.sp5StmScaling;
		}

		public Double getSp5ResScaling() {
			return this.sp5ResScaling;
		}

		public Double getTpCostMultiplier() {
			return this.tpCostMultiplier;
		}

		public Double getTpGainMultiplier() {
			return this.tpGainMultiplier;
		}

		public Passive getPassive() {
			return this.passive;
		}

		public ClassStats() {
		}
	}


	public static class Passive {
		private boolean enabled = true;
		private Map<String, Double> values = new HashMap<>();

		public void setEnabled(final boolean enabled) {
			this.enabled = enabled;
		}

		public void setValues(final Map<String, Double> values) {
			this.values = values;
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public Map<String, Double> getValues() {
			return this.values;
		}

		public Passive() {
		}
	}


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

		public void setStrength(final Integer strength) {
			this.strength = strength;
		}

		public void setStrikePower(final Integer strikePower) {
			this.strikePower = strikePower;
		}

		public void setResistance(final Integer resistance) {
			this.resistance = resistance;
		}

		public void setVitality(final Integer vitality) {
			this.vitality = vitality;
		}

		public void setKiPower(final Integer kiPower) {
			this.kiPower = kiPower;
		}

		public void setEnergy(final Integer energy) {
			this.energy = energy;
		}

		public Integer getStrength() {
			return this.strength;
		}

		public Integer getStrikePower() {
			return this.strikePower;
		}

		public Integer getResistance() {
			return this.resistance;
		}

		public Integer getVitality() {
			return this.vitality;
		}

		public Integer getKiPower() {
			return this.kiPower;
		}

		public Integer getEnergy() {
			return this.energy;
		}

		public BaseStats() {
		}
	}


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

		public void setStrengthScaling(final Double strengthScaling) {
			this.strengthScaling = strengthScaling;
		}

		public void setStrikePowerScaling(final Double strikePowerScaling) {
			this.strikePowerScaling = strikePowerScaling;
		}

		public void setStaminaScaling(final Double staminaScaling) {
			this.staminaScaling = staminaScaling;
		}

		public void setDefenseScaling(final Double defenseScaling) {
			this.defenseScaling = defenseScaling;
		}

		public void setDefenseScalingMax(final Double defenseScalingMax) {
			this.defenseScalingMax = defenseScalingMax;
		}

		public void setVitalityScaling(final Double vitalityScaling) {
			this.vitalityScaling = vitalityScaling;
		}

		public void setVitalityScalingMax(final Double vitalityScalingMax) {
			this.vitalityScalingMax = vitalityScalingMax;
		}

		public void setKiPowerScaling(final Double kiPowerScaling) {
			this.kiPowerScaling = kiPowerScaling;
		}

		public void setEnergyScaling(final Double energyScaling) {
			this.energyScaling = energyScaling;
		}

		public Double getStrengthScaling() {
			return this.strengthScaling;
		}

		public Double getStrikePowerScaling() {
			return this.strikePowerScaling;
		}

		public Double getStaminaScaling() {
			return this.staminaScaling;
		}

		public Double getDefenseScaling() {
			return this.defenseScaling;
		}

		public Double getDefenseScalingMax() {
			return this.defenseScalingMax;
		}

		public Double getVitalityScaling() {
			return this.vitalityScaling;
		}

		public Double getVitalityScalingMax() {
			return this.vitalityScalingMax;
		}

		public Double getKiPowerScaling() {
			return this.kiPowerScaling;
		}

		public Double getEnergyScaling() {
			return this.energyScaling;
		}

		public StatScaling() {
		}
	}

	public String getConfigVersion() {
		return this.configVersion;
	}

	public Map<String, ClassStats> getClasses() {
		return this.classes;
	}

	public RaceStatsConfig() {
	}

	public void setConfigVersion(final String configVersion) {
		this.configVersion = configVersion;
	}
}
