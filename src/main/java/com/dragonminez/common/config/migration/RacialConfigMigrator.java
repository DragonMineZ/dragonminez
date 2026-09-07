package com.dragonminez.common.config.migration;

import com.google.gson.JsonObject;

public final class RacialConfigMigrator {

	private RacialConfigMigrator() {
	}

	public static void migrate(JsonObject root) {
		if (root == null || !root.has("racialSkills") || !root.get("racialSkills").isJsonObject()) return;
		JsonObject racial = root.getAsJsonObject("racialSkills");
		if (racial.has("saiyan")) return;

		racial.add("saiyan", buildSection(racial,
				new String[]{"saiyanRacialSkill", "enabled"},
				new String[]{"saiyanZenkaiMinLevel", "minLevel"},
				new String[]{"saiyanZenkaiAmount", "permanentMaxBuffs"},
				new String[]{"saiyanZenkaiHealthRegen", "triggerHealthRegen"},
				new String[]{"saiyanZenkaiStatBoost", "triggerStatBoost"},
				new String[]{"saiyanZenkaiBoosts", "buffStats"},
				new String[]{"saiyanZenkaiCooldownSeconds", "cooldownSeconds"}));

		racial.add("namekian", buildSection(racial,
				new String[]{"namekianRacialSkill", "enabled"},
				new String[]{"namekianAssimilationAmount", "assimilationAmount"},
				new String[]{"namekianAssimilationHealthRegen", "assimilationHealthRegen"},
				new String[]{"namekianAssimilationStatBoost", "assimilationStatBoost"},
				new String[]{"namekianAssimilationBoosts", "assimilationBoosts"},
				new String[]{"namekianAssimilationOnNamekNpcs", "assimilationOnNamekNpcs"}));

		racial.add("majin", buildSection(racial,
				new String[]{"majinAbsoprtionSkill", "enabled"},
				new String[]{"majinReviveSkill", "reviveSkill"},
				new String[]{"majinAbsorptionAmount", "absorptionAmount"},
				new String[]{"majinAbsorptionHealthRegen", "absorptionHealthRegen"},
				new String[]{"majinAbsorptionStatsCopy", "absorptionStatCopy"},
				new String[]{"majinAbsorptionBoosts", "absorptionBoosts"},
				new String[]{"majinAbsorptionOnMobs", "absorptionOnMobs"},
				new String[]{"majinReviveCooldownSeconds", "reviveCooldownSeconds"},
				new String[]{"majinReviveHealthRatioPerBlop", "reviveHealthRatioPerBlop"}));

		racial.add("human", buildSection(racial,
				new String[]{"humanRacialSkill", "enabled"},
				new String[]{"humanKiRegenBoost", "kiRegenBoost"}));

		racial.add("frostdemon", buildSection(racial,
				new String[]{"frostDemonRacialSkill", "enabled"},
				new String[]{"frostDemonTPBoost", "tpBoost"}));

		racial.add("bioandroid", buildSection(racial,
				new String[]{"bioAndroidRacialSkill", "enabled"},
				new String[]{"bioAndroidCooldownSeconds", "cooldownSeconds"},
				new String[]{"bioAndroidDrainRatio", "drainRatio"}));

		for (String[] mapping : ALL_FLAT_KEYS) racial.remove(mapping[0]);
	}

	private static JsonObject buildSection(JsonObject flatSource, String[]... keyMappings) {
		JsonObject section = new JsonObject();
		for (String[] mapping : keyMappings) {
			String oldKey = mapping[0];
			String newKey = mapping[1];
			if (flatSource.has(oldKey) && !flatSource.get(oldKey).isJsonNull()) {
				section.add(newKey, flatSource.get(oldKey));
			}
		}
		return section;
	}

	private static final String[][] ALL_FLAT_KEYS = {
			{"saiyanRacialSkill"}, {"saiyanZenkaiMinLevel"}, {"saiyanZenkaiAmount"},
			{"saiyanZenkaiHealthRegen"}, {"saiyanZenkaiStatBoost"}, {"saiyanZenkaiBoosts"}, {"saiyanZenkaiCooldownSeconds"},
			{"namekianRacialSkill"}, {"namekianAssimilationAmount"}, {"namekianAssimilationHealthRegen"},
			{"namekianAssimilationStatBoost"}, {"namekianAssimilationBoosts"}, {"namekianAssimilationOnNamekNpcs"},
			{"majinAbsoprtionSkill"}, {"majinReviveSkill"}, {"majinAbsorptionAmount"}, {"majinAbsorptionHealthRegen"},
			{"majinAbsorptionStatsCopy"}, {"majinAbsorptionBoosts"}, {"majinAbsorptionOnMobs"},
			{"majinReviveCooldownSeconds"}, {"majinReviveHealthRatioPerBlop"},
			{"humanRacialSkill"}, {"humanKiRegenBoost"},
			{"frostDemonRacialSkill"}, {"frostDemonTPBoost"},
			{"bioAndroidRacialSkill"}, {"bioAndroidCooldownSeconds"}, {"bioAndroidDrainRatio"},
	};
}
