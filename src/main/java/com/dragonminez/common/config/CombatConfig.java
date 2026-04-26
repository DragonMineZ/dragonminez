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

	private Boolean respectAttackCooldown = true;
	private Double staminaConsumptionRatio = 0.5;
	private Integer baselineFormDrain = 15;
	private Boolean killPlayersOnCombatLogout = true;

	private Double baseDamageReductionCap = 0.75;
	private Double enchantmentDamageReductionCap = 0.85;
	private Double defenseDecayOnGuardBreak = 0.66;

	private Boolean enableBlocking = true;
	private Boolean enableParrying = true;
	private Boolean enableComboAttacks = true;
	private Integer parryWindowMs = 250;
	private Integer blockBreakStunDurationTicks = 60;
	private Double poiseDamageMultiplier = 1.0;
	private Double blockDamageReductionCap = 0.8;
	private Double blockDamageReductionMin = 0.1;
	private Integer poiseRegenCooldown = 120;

	private Boolean enablePerfectEvasion = true;
	private Integer perfectEvasionWindowMs = 200;
	private Integer dashCooldownSeconds = 1;
	private Integer doubleDashCooldownSeconds = 3;

	private Double[] kiBladeConfig = {1.5, 0.4};
	private Double[] kiScytheConfig = {2.0, 0.6};
	private Double[] kiClawLanceConfig = {1.2, 0.3};
	private String[] allowedCombatItems = {};
	private String[] twoHandedCombatItems = {};

	private Float upswingMultiplier = 0.5F;
	private Boolean allowFastAttacks = true;
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

	public Double[] getKiBladeConfig() {
		if (kiBladeConfig == null || kiBladeConfig.length < 2) return new Double[]{0.0, 0.0};
		return new Double[]{Math.max(0, kiBladeConfig[0]), Math.max(0, kiBladeConfig[1])};
	}

	public Double[] getKiScytheConfig() {
		if (kiScytheConfig == null || kiScytheConfig.length < 2) return new Double[]{0.0, 0.0};
		return new Double[]{Math.max(0, kiScytheConfig[0]), Math.max(0, kiScytheConfig[1])};
	}

	public Double[] getKiClawLanceConfig() {
		if (kiClawLanceConfig == null || kiClawLanceConfig.length < 2) return new Double[]{0.0, 0.0};
		return new Double[]{Math.max(0, kiClawLanceConfig[0]), Math.max(0, kiClawLanceConfig[1])};
	}

	@AllArgsConstructor
	@NoArgsConstructor
	@Getter
	public static class CompatibilitySpecifier {
		private String item_id_regex;
		private String weapon_attributes;
	}
}