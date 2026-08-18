package com.dragonminez.common.util;

import com.dragonminez.common.config.GeneralServerConfig;
import net.minecraft.network.chat.Component;

// Shared racial-skill title/description resolution, used by RaceSelectionScreen and SkillsMenuScreen.
public class RacialSkillTextHelper {

	private static final String PREFIX = "racial_";

	private static String stripPrefix(String racialSkill) {
		return racialSkill.startsWith(PREFIX) ? racialSkill.substring(PREFIX.length()) : racialSkill;
	}

	public static String getRacialSkillTitle(String racialSkill) {
		return Component.translatable("skill.dragonminez.racial_" + stripPrefix(racialSkill)).getString();
	}

	public static String getRacialSkillDescription(String racialSkill, GeneralServerConfig.RacialSkillsConfig config) {
		String skill = stripPrefix(racialSkill);
		String descKey = "skill.dragonminez.racial_" + skill + ".desc";

		return switch (skill) {
			case "human" -> {
				int regen = (int) Math.round((config.getHumanKiRegenBoost() - 1.0) * 100);
				yield Component.translatable(descKey, regen).getString();
			}
			case "saiyan" -> {
				int zenkaiHealth = (int) Math.round(config.getSaiyanZenkaiHealthRegen() * 100);
				int zenkaiStat = (int) Math.round(config.getSaiyanZenkaiStatBoost() * 100);
				int cooldown = config.getSaiyanZenkaiCooldownSeconds();
				int maxUses = config.getSaiyanZenkaiAmount();
				int minLevel = config.getSaiyanZenkaiMinLevel();
				yield Component.translatable(descKey, zenkaiHealth, zenkaiStat, cooldown, maxUses, minLevel).getString();
			}
			case "namekian" -> {
				int assimHealth = (int) Math.round(config.getNamekianAssimilationHealthRegen() * 100);
				int assimStat = (int) Math.round(config.getNamekianAssimilationStatBoost() * 100);
				int maxUses = config.getNamekianAssimilationAmount();
				yield Component.translatable(descKey, assimHealth, assimStat, maxUses).getString();
			}
			case "frostdemon" -> {
				int tpBoost = (int) Math.round((config.getFrostDemonTPBoost() - 1.0) * 100);
				yield Component.translatable(descKey, tpBoost).getString();
			}
			case "bioandroid" -> {
				int drainRatio = (int) Math.round(config.getBioAndroidDrainRatio() * 100);
				int cooldown = config.getBioAndroidCooldownSeconds();
				yield Component.translatable(descKey, drainRatio, cooldown).getString();
			}
			case "majin" -> {
				int absHealth = (int) Math.round(config.getMajinAbsorptionHealthRegen() * 100);
				int absStat = (int) Math.round(config.getMajinAbsorptionStatCopy() * 100);
				int maxUses = config.getMajinAbsorptionAmount();
				yield Component.translatable(descKey, absHealth, absStat, maxUses).getString();
			}
			default -> Component.translatable(descKey).getString();
		};
	}
}
