package com.dragonminez.common.config;

import com.dragonminez.common.stats.techniques.KiAttackData;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class TechniqueConfig {
	public static final int CURRENT_VERSION = 4;

	@Setter
	private int configVersion;

	@SerializedName("KiAttacks")
	private final Map<String, TechniqueTypeConfig> kiAttacks = new HashMap<>();
	@SerializedName("StrikeAttacks")
	private final Map<String, TechniqueTypeConfig> strikeAttacks = new HashMap<>();

	public TechniqueConfig() {
		createDefaults();
	}

	private void createDefaults() {
		for (KiAttackData.KiType type : KiAttackData.KiType.values()) {
			kiAttacks.put(type.name().toLowerCase(), TechniqueTypeConfig.defaults());
		}
	}

	public TechniqueTypeConfig getKiTypeConfig(KiAttackData.KiType type) {
		if (type == null) return TechniqueTypeConfig.defaults();
		TechniqueTypeConfig config = kiAttacks.get(type.name().toLowerCase());
		return config != null ? config : TechniqueTypeConfig.defaults();
	}

	@Getter
	@Setter
	public static class TechniqueTypeConfig {
		private int minXPCost = 100;
		private int maxXPCost = -1;
		private double xpCostMultiplier = 1.0;
		private double xpGainMultiplier = 1.0;
		private int xpGainPerHit = 1;
		private int xpGainPerKill = 3;
		private double kiCostMultiplier = 1.0;
		private double damageMultiplier = 1.0;

		public static TechniqueTypeConfig defaults() {
			return new TechniqueTypeConfig();
		}
	}
}
