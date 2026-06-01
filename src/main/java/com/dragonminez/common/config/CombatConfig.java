package com.dragonminez.common.config;

import com.dragonminez.common.combat.logic.player.TargetHelper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class CombatConfig {
	public static final int CURRENT_VERSION = 4;

	@Setter
	private int configVersion;

	private Double staminaConsumptionRatio = 0.083;
	private Double blockStaminaCost = 0.25;
	private Integer knockdownDurationSeconds = 30;
	private Integer baselineFormDrain = 200;
	private Boolean killPlayersOnCombatLogout = true;

	private Double kiProtectionMitigationPerLevel = 0.01;
	private Double kiProtectionCostRatio = 0.5;

	private Double kiInfusionDamagePerLevel = 0.025;
	private Double kiInfusionBaseCostPct = 2.5;
	private Double kiInfusionMaxCostPct = 7.5;

	private Double baseDamageReductionCap = 0.75;
	private Double enchantmentDamageReductionCap = 0.85;
	private Double defenseDecayOnGuardBreak = 0.66;

	private Boolean enableBlocking = true;
	private Boolean enableParrying = true;
	private Integer parryWindowMs = 150;
	private Double parryStaminaCostPenalty = 2.0;
	private Integer blockBreakStunDurationTicks = 60;
	private Double poiseDamageMultiplier = 0.25;
	private Double blockDamageReductionCap = 0.65;
	private Double blockDamageReductionMin = 0.05;
	private Integer poiseRegenCooldown = 100;

	private Boolean enablePerfectEvasion = true;
	private Integer perfectEvasionWindowMs = 200;
	private Integer dashCooldownSeconds = 4;
	private Integer doubleDashCooldownSeconds = 12;

	private Map<String, KiWeaponConfig> kiWeaponsConfig = new HashMap<>() {{
		put("blade", new KiWeaponConfig(0.0, 1.0, 0.0, 0.05, -2.4, "#FFFFFF", "dragonminez:sword"));
		put("scythe", new KiWeaponConfig(0.0, 1.5, 0.0, 0.075, -3.0, "#FFFFFF", "dragonminez:scythe"));
		put("clawlance", new KiWeaponConfig(0.0, 2.0, 0.0, 0.125, -2.6, "#FFFFFF", "dragonminez:trident"));
	}};

	private Float upswingMultiplier = 0.5F;
	private Boolean allowAttackingMount = false;
	private Integer attackIntervalCap = 2;
	private Boolean weaponRegistryLogging = false;
	private Boolean weaponRegistryCompression = true;

	private Map<String, TargetHelper.Relation> playerRelations = new HashMap<>() {{
		put("minecraft:player", TargetHelper.Relation.NEUTRAL);
		put("minecraft:villager", TargetHelper.Relation.NEUTRAL);
		put("minecraft:iron_golem", TargetHelper.Relation.NEUTRAL);
		put("guardvillagers:guard", TargetHelper.Relation.NEUTRAL);
	}};

	private TargetHelper.Relation playerRelationToPassives = TargetHelper.Relation.HOSTILE;
	private TargetHelper.Relation playerRelationToHostiles = TargetHelper.Relation.HOSTILE;
	private TargetHelper.Relation playerRelationToOther = TargetHelper.Relation.HOSTILE;

	private Boolean fallbackCompatibilityEnabled = true;
	private String blacklistItemIdRegex = "pickaxe";

	private List<CompatibilitySpecifier> fallbackCompatibility = new ArrayList<>(Arrays.asList(
			new CompatibilitySpecifier("claymore|great_sword|greatsword", "dragonminez:claymore"),
			new CompatibilitySpecifier("great_hammer|greathammer|war_hammer|warhammer|maul", "dragonminez:hammer"),
			new CompatibilitySpecifier("double_axe|doubleaxe|war_axe|waraxe|great_axe|greataxe", "dragonminez:double_axe"),
			new CompatibilitySpecifier("scythe", "dragonminez:scythe"),
			new CompatibilitySpecifier("halberd|glaive|pike|lance|naginata", "dragonminez:halberd"),
			new CompatibilitySpecifier("spear|trident|pitchfork|javelin", "dragonminez:spear"),
			new CompatibilitySpecifier("battlestaff|staff|quarterstaff|pole", "dragonminez:battlestaff"),
			new CompatibilitySpecifier("katana|uchigatana|nodachi|tachi", "dragonminez:katana"),
			new CompatibilitySpecifier("rapier|foil", "dragonminez:rapier"),
			new CompatibilitySpecifier("dagger|knife|shiv|dirk|kunai|karambit|wakizashi|tanto", "dragonminez:dagger"),
			new CompatibilitySpecifier("sickle|kama", "dragonminez:sickle"),
			new CompatibilitySpecifier("soul_knife", "dragonminez:soul_knife"),
			new CompatibilitySpecifier("claw|katar", "dragonminez:claw"),
			new CompatibilitySpecifier("wand", "dragonminez:wand"),
			new CompatibilitySpecifier("mace|hammer|flail", "dragonminez:mace"),
			new CompatibilitySpecifier("axe", "dragonminez:axe"),
			new CompatibilitySpecifier("coral_blade", "dragonminez:coral_blade"),
			new CompatibilitySpecifier("twin_blade|twinblade", "dragonminez:twin_blade"),
			new CompatibilitySpecifier("cutlass|scimitar|machete", "dragonminez:cutlass"),
			new CompatibilitySpecifier("sword|blade", "dragonminez:sword")
	));

	public float getUpswingMultiplier() {
		return Math.max(0.2F, Math.min(1.0F, upswingMultiplier));
	}

	public KiWeaponConfig getKiWeaponConfig(String type) {
		if (type == null || kiWeaponsConfig == null) return null;
		return kiWeaponsConfig.get(type.toLowerCase());
	}

	/** Sorted list of Ki weapon types defined in the config, used for deterministic cycling. */
	public List<String> getKiWeaponTypes() {
		if (kiWeaponsConfig == null) return new ArrayList<>();
		List<String> keys = new ArrayList<>(kiWeaponsConfig.keySet());
		java.util.Collections.sort(keys);
		return keys;
	}

	@AllArgsConstructor
	@NoArgsConstructor
	@Getter
	public static class CompatibilitySpecifier {
		private String item_id_regex;
		private String weapon_attributes;
	}

	@AllArgsConstructor
	@NoArgsConstructor
	@Getter
	public static class KiWeaponConfig {
		private Double baseDamage = 0.0;
		private Double kiScalingDamage = 1.0;
		private Double baseKiCost = 0.0;
		private Double kiScalingCost = 0.05;
		private Double attackSpeed = -2.4;
		private String forcedColor = "#FFFFFF";
		private String weaponCombo = "";

		public double getBaseDamage() { return baseDamage != null ? Math.max(0.0, baseDamage) : 0.0; }
		public double getKiScalingDamage() { return kiScalingDamage != null ? Math.max(0.0, kiScalingDamage) : 0.0; }
		public double getBaseKiCost() { return baseKiCost != null ? Math.max(0.0, baseKiCost) : 0.0; }
		public double getKiScalingCost() { return kiScalingCost != null ? Math.max(0.0, kiScalingCost) : 0.0; }
		public double getAttackSpeed() { return attackSpeed != null ? attackSpeed : 0.0; }
		public String getForcedColor() { return forcedColor; }
		public String getWeaponCombo() { return weaponCombo; }
	}
}