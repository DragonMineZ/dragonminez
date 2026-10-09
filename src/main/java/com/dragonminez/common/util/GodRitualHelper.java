package com.dragonminez.common.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Status;

import java.util.List;
import java.util.Locale;

public final class GodRitualHelper {
	public static final String RACE = "saiyan";
	private static final String MUTANT_EFFECT = "mutant";

	private GodRitualHelper() {}

	public static GeneralServerConfig.GodRitualConfig config() {
		GeneralServerConfig server = ConfigManager.getServerConfig();
		return server != null ? server.getSuperSaiyanGodRitual() : null;
	}

	public static GeneralServerConfig.KiTransferConfig transferConfig() {
		GeneralServerConfig server = ConfigManager.getServerConfig();
		return server != null ? server.getKiTransfer() : null;
	}

	public static FormConfig.FormData formData() {
		GeneralServerConfig.GodRitualConfig cfg = config();
		if (cfg == null) return null;
		FormConfig group = ConfigManager.getFormGroup(RACE, cfg.getGroupName());
		return group != null ? group.getForm(cfg.getFormName()) : null;
	}

	public static boolean isActiveForm(StatsData data) {
		if (data == null || !data.getCharacter().hasActiveForm()) return false;
		return TransformationsHelper.isGodRitualGroup(data.getCharacter().getRaceName(), data.getCharacter().getActiveFormGroup());
	}

	public static boolean isPureHeart(StatsData data) {
		GeneralServerConfig.GodRitualConfig cfg = config();
		if (cfg == null || data == null) return false;
		int alignment = data.getResources().getAlignment();
		return alignment >= cfg.getPureGoodMinAlignment() || alignment <= cfg.getPureEvilMaxAlignment();
	}

	public static boolean isSaiyan(StatsData data) {
		return data != null && RACE.equalsIgnoreCase(data.getCharacter().getRaceName());
	}

	public static boolean isInAnyForm(StatsData data, List<String> entries) {
		if (data == null || entries == null || !data.getCharacter().hasActiveForm() || data.getCharacter().hasActiveStackForm()) return false;
		String active = (data.getCharacter().getActiveFormGroup() + "." + data.getCharacter().getActiveForm()).toLowerCase(Locale.ROOT);
		for (String entry : entries) {
			if (entry != null && active.equals(entry.trim().toLowerCase(Locale.ROOT))) return true;
		}
		return false;
	}

	public static boolean isChannelingRecipient(StatsData data) {
		return data != null && data.getStatus().getGodRitualRole() == Status.GOD_RITUAL_RECIPIENT;
	}

	public static float shellAlpha(StatsData data, double gameTime) {
		long start = data.getStatus().getGodRitualGlowStart();
		GeneralServerConfig.GodRitualConfig cfg = config();
		if (start <= 0L || cfg == null) return 0.0f;
		double elapsed = gameTime - start;
		if (elapsed <= 0.0) return 0.0f;
		int in = cfg.getGlowInTicks();
		if (elapsed < in) return (float) (elapsed / in);
		double out = (elapsed - in) / cfg.getGlowOutTicks();
		if (out >= 1.0) return 0.0f;
		double fade = 1.0 - out;
		return (float) (fade * fade * (3.0 - 2.0 * fade));
	}

	public static boolean isShellExpired(StatsData data, long gameTime) {
		long start = data.getStatus().getGodRitualGlowStart();
		GeneralServerConfig.GodRitualConfig cfg = config();
		return start > 0L && (cfg == null || gameTime - start > cfg.getGlowInTicks() + cfg.getGlowOutTicks());
	}

	public static float[] shellColor() {
		GeneralServerConfig.GodRitualConfig cfg = config();
		int rgb = Integer.parseInt((cfg != null ? cfg.getShellColor() : "#6FD8FF").substring(1), 16);
		return new float[]{((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f};
	}

	public static double powerBonus(StatsData data) {
		GeneralServerConfig.GodRitualConfig cfg = config();
		if (cfg == null) return 0.30;
		return data.getEffects().hasEffect(MUTANT_EFFECT) ? cfg.getMutantPowerBonus() : cfg.getPowerBonus();
	}

	public static double kiShareMultiplier(StatsData data, String statName) {
		int donors = data.getStatus().getKiTransferDonors();
		if (donors <= 0) return 1.0;
		switch (statName.toUpperCase(Locale.ROOT)) {
			case "STR", "SKP", "DEF", "PWR" -> {
				GeneralServerConfig.KiTransferConfig cfg = transferConfig();
				if (cfg == null) return 1.0;
				return 1.0 + Math.min(donors, cfg.getMaxBoostDonors()) * cfg.getBoostPerDonor();
			}
			default -> {
				return 1.0;
			}
		}
	}

	public static double donorSpeedMultiplier(StatsData data) {
		if (!data.getStatus().isSharingKi()) return 1.0;
		GeneralServerConfig.KiTransferConfig cfg = transferConfig();
		return cfg != null ? cfg.getDonorSpeedMultiplier() : 1.0;
	}
}
