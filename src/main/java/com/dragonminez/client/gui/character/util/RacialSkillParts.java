package com.dragonminez.client.gui.character.util;

import com.dragonminez.client.util.NumberFormattingUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.racial.impl.BioAndroidEvolution;
import com.dragonminez.common.util.lists.FrostDemonForms;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RacialSkillParts {
	public static final String ENTRY_PREFIX = "racial_";

	public record Part(String id, MutableComponent name, MutableComponent description) {}

	private RacialSkillParts() {}

	public static List<Part> forPlayer(String racialSkill, boolean androidUpgraded) {
		return build(racialSkill, androidUpgraded, false);
	}

	public static List<Part> forRace(String racialSkill) {
		return build(racialSkill, false, true);
	}

	public static Part find(List<Part> parts, String id) {
		for (Part part : parts) if (part.id().equals(id)) return part;
		return null;
	}

	private static List<Part> build(String racialSkill, boolean android, boolean allVariants) {
		List<Part> parts = new ArrayList<>();
		if (racialSkill == null || racialSkill.isEmpty()) return parts;
		GeneralServerConfig.RacialSkillsConfig config = ConfigManager.getServerConfig().getRacialSkills();
		String skill = racialSkill.toLowerCase(Locale.ROOT);
		switch (skill) {
			case "human" -> human(parts, config.getHuman(), android, allVariants);
			case "saiyan" -> saiyan(parts, config.getSaiyan());
			case "namekian" -> namekian(parts, config.getNamekian());
			case "frostdemon" -> frostDemon(parts, config.getFrostdemon());
			case "bioandroid" -> bioAndroid(parts, config.getBioandroid());
			case "majin" -> majin(parts, config.getMajin());
			case "glind" -> glind(parts, config.getGlind());
			default -> {
				String key = "skill.dragonminez." + ENTRY_PREFIX + skill;
				parts.add(new Part(ENTRY_PREFIX + skill, Component.translatable(key), Component.translatable(key + ".desc")));
			}
		}
		return parts;
	}

	private static void human(List<Part> parts, GeneralServerConfig.HumanRacialConfig c, boolean android, boolean allVariants) {
		if (!android || allVariants) add(parts, "human", "blood_fueled_ki");
		add(parts, "human", "fast_learner", pct(c.getTechniqueXpBonus()));
		add(parts, "human", "adrenaline", pct(c.getAdrenalineThreshold()), c.getAdrenalineSeconds(), pct(c.getAdrenalineDamageReduction()),
				pct(c.getAdrenalineAttackSpeed()), pct(c.getAdrenalineMoveSpeed()), c.getAdrenalineCooldownSeconds());
		if (!android && !allVariants) return;
		add(parts, "human", "android_core", NumberFormattingUtil.formatUpToOneDecimal(c.getAndroidKiRegenMultiplier()));
		if (Boolean.TRUE.equals(c.getAndroidBarrierEnabled())) {
			add(parts, "human", "absorption_barrier", c.getAndroidBarrierMaxSeconds(), pct(c.getAndroidBarrierKiConversion()),
					pct(c.getAndroidBarrierSurgeOverflow()), pct(c.getAndroidBarrierBreakOverflow()),
					c.getAndroidBarrierCooldownSeconds(), c.getAndroidBarrierBrokenCooldownSeconds());
		}
	}

	private static void saiyan(List<Part> parts, GeneralServerConfig.SaiyanRacialConfig c) {
		add(parts, "saiyan", "zenkai", c.getMinLevel(), pct(c.getOverkillThreshold()), c.getKnockoutSeconds(),
				pct(c.getKnockoutHealthRegen()), pct(c.getKnockoutEnergyRegen()), pct(c.getKnockoutStaminaRegen()),
				stats(c.getBuffStats()), pct(c.getMinBuffPct()), pct(c.getMaxBuffPct()), c.getPeakWindowSeconds(),
				pct(c.getMaxBuffHitRatio()), c.getPermanentMaxBuffs(), c.getTempBuffMinSeconds(), c.getTempBuffMaxSeconds(),
				c.getCooldownSeconds());
		add(parts, "saiyan", "limitless_power", pct(c.getReleaseBonusPerZenkai()), pct(c.getReleaseBonusCap()));
	}

	private static void namekian(List<Part> parts, GeneralServerConfig.NamekianRacialConfig c) {
		MutableComponent npcs = Boolean.TRUE.equals(c.getAssimilationOnNamekNpcs())
				? Component.translatable("skill.dragonminez.racial_namekian.assimilation.npcs")
				: Component.empty();
		add(parts, "namekian", "assimilation", npcs, pct(c.getAssimilationHealthRegen()), pct(c.getAssimilationStatBoost()),
				stats(c.getAssimilationBoosts()), c.getAssimilationAmount(), c.getAssimilationCooldownSeconds());
		add(parts, "namekian", "regeneration", pct(c.getRegenHealthRatio()), c.getRegenChannelSeconds(), pct(c.getRegenEnergyCost()),
				pct(c.getRegenStaminaCost()), c.getRegenCooldownSeconds());
		add(parts, "namekian", "water_affinity", pct(c.getWaterRegenBonus()), c.getWaterRegenSeconds());
	}

	private static void frostDemon(List<Part> parts, GeneralServerConfig.FrostDemonRacialConfig c) {
		add(parts, "frostdemon", "prodigious_strength", pct(c.getTpBoost() - 1.0));
		add(parts, "frostdemon", "evolutionary_mastery", pct(c.getMasteryGainBonus()));
		add(parts, "frostdemon", "hardened_body", pct(c.getKiTechniqueResistance()), pct(c.getStrikeTechniqueResistance()));
		add(parts, "frostdemon", "power_reserve", frostForms(c.getReserveChargeForms()), c.getReserveChargeReleaseThreshold(),
				pct(c.getReserveMaxRatio()), c.getReserveFullChargeSeconds(), c.getReserveDurationSeconds(), pct(c.getReserveFormBonus()));
	}

	private static void bioAndroid(List<Part> parts, GeneralServerConfig.BioAndroidRacialConfig c) {
		add(parts, "bioandroid", "evolution");
		add(parts, "bioandroid", "vital_drain", BioAndroidEvolution.STUN_DURATION_TICKS / 20, pct(c.getDrainRatio() / BioAndroidEvolution.CHANNEL_SECONDS),
				c.getCooldownSeconds());
		MutableComponent outcome = Boolean.TRUE.equals(c.getExplodeKillsUser())
				? Component.translatable("skill.dragonminez.racial_bioandroid.self_destruct.dies")
				: Component.translatable("skill.dragonminez.racial_bioandroid.self_destruct.survives",
						pct(BioAndroidEvolution.EXPLODE_SURVIVAL_HEALTH_RATIO), c.getExplodeKnockdownSeconds());
		add(parts, "bioandroid", "self_destruct", c.getExplodeChargeSeconds(), c.getExplodeRadius(), pct(c.getExplodeDamageRatio()),
				outcome, c.getExplodeCooldownSeconds());
		add(parts, "bioandroid", "cell_jr", BioAndroidEvolution.CELL_JR_CHARGE_SECONDS, pct(c.getCellJrStatRatio()), c.getCellJrMax(), pct(c.getCellJrOwnerPenalty()),
				c.getCellJrChargeCooldownSeconds());
	}

	private static void majin(List<Part> parts, GeneralServerConfig.MajinRacialConfig c) {
		MutableComponent targets = Component.translatable(Boolean.TRUE.equals(c.getAbsorptionOnMobs())
				? "skill.dragonminez.racial_majin.absorption.mobs"
				: "skill.dragonminez.racial_majin.absorption.players");
		add(parts, "majin", "absorption", targets, pct(c.getAbsorptionHealthRegen()), pct(c.getAbsorptionStatCopy()),
				stats(c.getAbsorptionBoosts()), c.getAbsorptionAmount(), c.getAbsorptionCooldownSeconds(), c.getSlotEjectCooldownSeconds());
		add(parts, "majin", "sweet_tooth", pct(c.getFoodHealBonus()));
		add(parts, "majin", "healing_magic", pct(c.getKiHealDealtBonus()), pct(c.getKiHealReceivedBonus()),
				pct(c.getHealTechniqueCostReduction()), pct(c.getHealTechniqueCooldownReduction()));
	}

	private static void glind(List<Part> parts, GeneralServerConfig.GlindRacialConfig c) {
		if (Boolean.TRUE.equals(c.getDivineKi())) add(parts, "glind", "divine_ki");
		add(parts, "glind", "celestial_body", pct(c.getRegenMultiplier() - 1.0), pct(c.getMeditationDodgeMultiplier() - 1.0));
		add(parts, "glind", "divine_punishment", pct(c.getHealingReductionBase()), pct(c.getHealingReductionStackEfficiency()));
		add(parts, "glind", "light_and_darkness", pct(c.getPlayerDamageBonus()), pct(c.getFrostDemonDamageBonus()), pct(c.getMajinDamageBonus()),
				c.getEvilAttackerMaxAlignment(), c.getGoodTargetMinAlignment(), c.getGoodAttackerMinAlignment(), c.getEvilTargetMaxAlignment(),
				pct(c.getNpcDamageBonus()));
	}

	private static void add(List<Part> parts, String skill, String part, Object... args) {
		String key = "skill.dragonminez." + ENTRY_PREFIX + skill + "." + part;
		parts.add(new Part(ENTRY_PREFIX + skill + "." + part, Component.translatable(key), Component.translatable(key + ".desc", args)));
	}

	private static String pct(double ratio) {
		return NumberFormattingUtil.formatUpToOneDecimal(ratio * 100.0);
	}

	private static MutableComponent stats(String[] codes) {
		MutableComponent out = Component.empty();
		if (codes == null) return out;
		for (int i = 0; i < codes.length; i++) {
			if (i > 0) out.append(Component.literal(", "));
			String code = codes[i].toLowerCase(Locale.ROOT);
			out.append(Component.translatableWithFallback("gui.dragonminez.customization.stat." + code + ".title", codes[i]));
		}
		return out;
	}

	private static MutableComponent frostForms(String[] forms) {
		MutableComponent out = Component.empty();
		if (forms == null) return out;
		for (int i = 0; i < forms.length; i++) {
			if (i > 0) out.append(Component.literal(", "));
			out.append(Component.translatableWithFallback("race.dragonminez.frostdemon.form." + FrostDemonForms.GROUP_EVOLUTIONFORMS + "." + forms[i], forms[i]));
		}
		return out;
	}
}
