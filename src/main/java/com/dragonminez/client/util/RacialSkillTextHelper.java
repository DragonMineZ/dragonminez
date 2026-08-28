package com.dragonminez.client.util;

import com.dragonminez.common.config.GeneralServerConfig;
import net.minecraft.network.chat.Component;

public final class RacialSkillTextHelper {

	private static final String RACIAL_PREFIX = "racial_";
	private static final String SKILL_PREFIX = "skill.dragonminez." + RACIAL_PREFIX;

	private RacialSkillTextHelper() {
	}

	public static String getRacialSkillTitle(String racialSkill) {
		return translate(SKILL_PREFIX + normalizeSkill(racialSkill));
	}

	public static String getRacialSkillDescription(String racialSkill, GeneralServerConfig.RacialSkillsConfig config) {
		String skill = normalizeSkill(racialSkill);
		String key = SKILL_PREFIX + skill + ".desc";

		return switch (skill) {
			case "human" -> translate(
					key,
					percentage(config.getHumanKiRegenBoost() - 1.0)
			);

			case "saiyan" -> translate(
					key,
					percentage(config.getSaiyanZenkaiHealthRegen()),
					percentage(config.getSaiyanZenkaiStatBoost()),
					config.getSaiyanZenkaiCooldownSeconds(),
					config.getSaiyanZenkaiAmount(),
					config.getSaiyanZenkaiMinLevel()
			);

			case "namekian" -> translate(
					key,
					percentage(config.getNamekianAssimilationHealthRegen()),
					percentage(config.getNamekianAssimilationStatBoost()),
					config.getNamekianAssimilationAmount()
			);

			case "frostdemon" -> translate(
					key,
					percentage(config.getFrostDemonTPBoost() - 1.0)
			);

			case "bioandroid" -> translate(
					key,
					percentage(config.getBioAndroidDrainRatio()),
					config.getBioAndroidCooldownSeconds()
			);

			case "majin" -> translate(
					key,
					percentage(config.getMajinAbsorptionHealthRegen()),
					percentage(config.getMajinAbsorptionStatCopy()),
					config.getMajinAbsorptionAmount()
			);

			default -> translate(key);
		};
	}

	private static String normalizeSkill(String racialSkill) {
		return racialSkill.startsWith(RACIAL_PREFIX)
				? racialSkill.substring(RACIAL_PREFIX.length())
				: racialSkill;
	}

	private static int percentage(double value) {
		return (int) Math.round(value * 100);
	}

	private static String translate(String key, Object... args) {
		return Component.translatable(key, args).getString();
	}
}
