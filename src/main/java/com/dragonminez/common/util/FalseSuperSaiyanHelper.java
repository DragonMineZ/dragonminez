package com.dragonminez.common.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import net.minecraft.world.entity.LivingEntity;

public final class FalseSuperSaiyanHelper {
	public static final String RACE = "saiyan";
	public static final String SUPER_FORMS_SKILL = "superforms";
	private static final String MUTANT_EFFECT = "mutant";

	private FalseSuperSaiyanHelper() {}

	public static GeneralServerConfig.FalseSuperSaiyanConfig config() {
		GeneralServerConfig server = ConfigManager.getServerConfig();
		return server != null ? server.getFalseSuperSaiyan() : null;
	}

	public static FormConfig.FormData formData() {
		GeneralServerConfig.FalseSuperSaiyanConfig cfg = config();
		if (cfg == null) return null;
		FormConfig group = ConfigManager.getFormGroup(RACE, cfg.getGroupName());
		return group != null ? group.getForm(cfg.getFormName()) : null;
	}

	public static boolean isActiveForm(StatsData data) {
		GeneralServerConfig.FalseSuperSaiyanConfig cfg = config();
		if (cfg == null || data == null || !data.getCharacter().hasActiveForm()) return false;
		return cfg.getGroupName().equalsIgnoreCase(data.getCharacter().getActiveFormGroup())
				&& cfg.getFormName().equalsIgnoreCase(data.getCharacter().getActiveForm());
	}

	public static boolean meetsBaseRequirements(StatsData data) {
		GeneralServerConfig.FalseSuperSaiyanConfig cfg = config();
		if (cfg == null || !cfg.getEnabled() || data == null) return false;
		if (!RACE.equalsIgnoreCase(data.getCharacter().getRaceName())) return false;
		if (!data.getStatus().isHasCreatedCharacter() || !data.getStatus().isAlive()) return false;
		if (data.getEffects().hasEffect(MUTANT_EFFECT)) return false;
		if (data.getSkills().getSkillLevel(SUPER_FORMS_SKILL) > 0) return false;
		int level = data.getLevel();
		return level >= cfg.getMinLevel() && level < cfg.getMaxLevel();
	}

	public static boolean canCharge(LivingEntity player, StatsData data) {
		GeneralServerConfig.FalseSuperSaiyanConfig cfg = config();
		if (cfg == null || player == null || !meetsBaseRequirements(data)) return false;
		if (data.getCharacter().hasActiveForm() || data.getCharacter().hasActiveStackForm()) return false;
		if (data.getStatus().isKnockedDown()) return false;
		if (healthFraction(player) > cfg.getHealthThreshold()) return false;
		return meetsZenkaiGate(data);
	}

	public static boolean ownsRageBar(StatsData data) {
		return isActiveForm(data) || meetsBaseRequirements(data);
	}

	public static boolean meetsZenkaiGate(StatsData data) {
		GeneralServerConfig.FalseSuperSaiyanConfig cfg = config();
		if (cfg == null || !cfg.getRequireZenkaiSpent()) return true;

		GeneralServerConfig server = ConfigManager.getServerConfig();
		if (server == null) return true;
		GeneralServerConfig.RacialSkillsConfig racial = server.getRacialSkills();
		if (racial == null || !racial.getEnableRacialSkills()) return true;
		GeneralServerConfig.SaiyanRacialConfig saiyan = racial.getSaiyan();
		if (saiyan == null || !saiyan.getEnabled()) return true;

		if (data.getCooldowns().hasCooldown(Cooldowns.ZENKAI)) return true;
		return data.getRacialData().getZenkaiPermanentUses() >= saiyan.getPermanentMaxBuffs();
	}

	public static float healthFraction(LivingEntity player) {
		float max = Math.max(1.0f, player.getMaxHealth());
		return player.getHealth() / max;
	}
}
