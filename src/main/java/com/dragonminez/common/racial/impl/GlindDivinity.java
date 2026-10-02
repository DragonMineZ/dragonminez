package com.dragonminez.common.racial.impl;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.racial.RacialAbility;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.FusionTraits;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class GlindDivinity implements RacialAbility {
	public static final String ID = "glind";

	private static final String GOD_FORM_SKILL = "godforms";
	private static final String FROST_DEMON_FAMILY = "frostdemon";
	private static final String MAJIN_FAMILY = "majin";

	@Override
	public String id() {
		return ID;
	}

	public static GeneralServerConfig.GlindRacialConfig config() {
		return ConfigManager.getServerConfig().getRacialSkills().getGlind();
	}

	public static boolean isActive(StatsData data) {
		if (data == null) return false;
		GeneralServerConfig.RacialSkillsConfig racial = ConfigManager.getServerConfig().getRacialSkills();
		if (!Boolean.TRUE.equals(racial.getEnableRacialSkills()) || !Boolean.TRUE.equals(racial.getGlind().getEnabled())) return false;
		RaceCharacterConfig race = ConfigManager.getRaceCharacter(data.getCharacter().getRaceName());
		return race != null && ID.equals(race.getRacialSkill());
	}

	public static boolean hasDivineKi(StatsData data) {
		return isActive(data) && Boolean.TRUE.equals(config().getDivineKi());
	}

	public static boolean canPerceiveDivineKi(Player viewer, StatsData viewerData) {
		if (viewer != null && viewer.hasEffect(MainEffects.KAMI_BLESS.get())) return true;
		return viewerData != null && viewerData.getSkills().getSkillLevel(GOD_FORM_SKILL) >= 1;
	}

	public static boolean isHiddenFrom(StatsData target, StatsData viewer) {
		if (target == null || target == viewer || !hasDivineKi(target)) return false;
		return viewer == null || !canPerceiveDivineKi(viewer.getPlayer(), viewer);
	}

	public static boolean isHiddenFrom(Entity target, Player viewer) {
		if (!(target instanceof Player targetPlayer) || targetPlayer == viewer) return false;
		StatsData targetData = StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).orElse(null);
		if (!hasDivineKi(targetData)) return false;
		StatsData viewerData = viewer != null ? StatsProvider.get(StatsCapability.INSTANCE, viewer).orElse(null) : null;
		return !canPerceiveDivineKi(viewer, viewerData);
	}

	public static double modifyMeditationDodge(StatsData data, double chance) {
		if (chance <= 0.0 || !isActive(data)) return chance;
		return Math.min(1.0, chance * config().getMeditationDodgeMultiplier());
	}

	public static double regenMultiplier(StatsData data) {
		return isActive(data) ? config().getRegenMultiplier() : 1.0;
	}

	public static double applyHealingReduction(StatsData attacker, double stackedReduction) {
		if (!isActive(attacker)) return stackedReduction;
		GeneralServerConfig.GlindRacialConfig config = config();
		return config.getHealingReductionBase() + stackedReduction * config.getHealingReductionStackEfficiency();
	}

	public static double alignmentDamageMultiplier(Player attacker, LivingEntity victim) {
		if (attacker == null || victim == null || attacker == victim) return 1.0;
		StatsData attackerData = StatsProvider.get(StatsCapability.INSTANCE, attacker).orElse(null);
		if (!isActive(attackerData)) return 1.0;
		GeneralServerConfig.GlindRacialConfig config = config();
		if (!(victim instanceof Player victimPlayer)) return 1.0 + config.getNpcDamageBonus();
		StatsData victimData = StatsProvider.get(StatsCapability.INSTANCE, victimPlayer).orElse(null);
		if (victimData == null || !opposesAlignment(attackerData, victimData, config)) return 1.0;
		return 1.0 + playerDamageBonus(victimData, config);
	}

	private static boolean opposesAlignment(StatsData attacker, StatsData victim, GeneralServerConfig.GlindRacialConfig config) {
		int own = attacker.getResources().getAlignment();
		int target = victim.getResources().getAlignment();
		if (own <= config.getEvilAttackerMaxAlignment()) return target >= config.getGoodTargetMinAlignment();
		if (own >= config.getGoodAttackerMinAlignment()) return target <= config.getEvilTargetMaxAlignment();
		return false;
	}

	private static double playerDamageBonus(StatsData victim, GeneralServerConfig.GlindRacialConfig config) {
		String family = FusionTraits.raceFamily(victim.getCharacter().getRaceName());
		if (FROST_DEMON_FAMILY.equals(family)) return config.getFrostDemonDamageBonus();
		if (MAJIN_FAMILY.equals(family)) return config.getMajinDamageBonus();
		return config.getPlayerDamageBonus();
	}
}
