package com.dragonminez.common.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.SkillsConfig;
import com.dragonminez.common.stats.FusedData;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.skills.Skills;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class FusionForms {
	public static final char SEPARATOR = '@';
	public static final String MUTANT_EFFECT = "mutant";

	private FusionForms() {
	}

	public static String qualify(String race, String group) {
		return race.toLowerCase(Locale.ROOT) + SEPARATOR + group;
	}

	public static boolean isQualified(String group) {
		return group != null && group.indexOf(SEPARATOR) > 0;
	}

	public static String baseGroup(String group) {
		if (group == null) return null;
		int index = group.indexOf(SEPARATOR);
		return index > 0 ? group.substring(index + 1) : group;
	}

	public static String qualifierRace(String group) {
		if (group == null) return null;
		int index = group.indexOf(SEPARATOR);
		return index > 0 ? group.substring(0, index) : null;
	}

	public static String raceOf(StatsData data, String group) {
		String race = qualifierRace(group);
		return race != null ? race : data.getCharacter().getRaceName();
	}

	public static FusedData fused(StatsData data) {
		return data != null ? data.getFusedData() : null;
	}

	public static Skills formSkillsFor(StatsData data, String group) {
		FusedData fused = fused(data);
		if (fused != null && isQualified(group)) return fused.getPartnerFormSkills();
		return data.getSkills();
	}

	public static boolean hasMutantFor(StatsData data, String group) {
		if (data == null) return false;
		FusedData fused = fused(data);
		if (fused == null) return data.getEffects().hasEffect(MUTANT_EFFECT);
		if (isQualified(group)) return fused.isPartnerMutant();
		return fused.isLeaderMutant() || (!fused.isCrossRace() && fused.isPartnerMutant());
	}

	public static boolean hasAnyMutant(StatsData data) {
		if (data == null) return false;
		if (data.getEffects().hasEffect(MUTANT_EFFECT)) return true;
		FusedData fused = fused(data);
		return fused != null && fused.isPartnerMutant();
	}

	public static Map<String, FormConfig> allFormGroups(StatsData data) {
		Map<String, FormConfig> own = ConfigManager.getAllFormsForRace(data.getCharacter().getRaceName());
		FusedData fused = fused(data);
		if (fused == null || !fused.isCrossRace()) return own;

		Map<String, FormConfig> all = new LinkedHashMap<>(own);
		for (Map.Entry<String, FormConfig> entry : ConfigManager.getAllFormsForRace(fused.getPartnerRace()).entrySet()) {
			if (!isForeignGroupAllowed(fused, entry.getKey(), entry.getValue())) continue;
			all.put(qualify(fused.getPartnerRace(), entry.getKey()), entry.getValue());
		}
		return all;
	}

	public static boolean isForeignGroupAllowed(StatsData data, String group) {
		FusedData fused = fused(data);
		if (fused == null || !fused.isCrossRace()) return false;
		String race = qualifierRace(group);
		if (race == null || !race.equals(fused.getPartnerRace())) return false;
		return isForeignGroupAllowed(fused, baseGroup(group), ConfigManager.getFormGroup(race, baseGroup(group)));
	}

	private static boolean isForeignGroupAllowed(FusedData fused, String rawGroup, FormConfig config) {
		if (config == null || "androidforms".equalsIgnoreCase(rawGroup)) return false;
		String type = config.getFormType() != null ? config.getFormType().toLowerCase(Locale.ROOT) : "";
		if (TransformationsHelper.isScriptedFormType(type)) return false;
		return !fused.isPartnerAndroid() || type.contains("god");
	}

	public static boolean isSkillAllowed(StatsData data, String skillName) {
		SkillsConfig config = ConfigManager.getSkillsConfig();
		if (config.isSkillAllowedForRace(skillName, data.getCharacter().getRaceName())) return true;
		FusedData fused = fused(data);
		return fused != null && config.isSkillAllowedForRace(skillName, fused.getPartnerRace());
	}

	public static String qualifyLinkedGroup(String ownerGroup, String linkedGroup) {
		String race = qualifierRace(ownerGroup);
		if (race == null || linkedGroup == null || isQualified(linkedGroup)) return linkedGroup;
		if (ConfigManager.getStackFormGroup(linkedGroup) != null) return linkedGroup;
		return qualify(race, linkedGroup);
	}

	public static String formTranslationKey(String race, String group, String form) {
		String qualifier = qualifierRace(group);
		return "race.dragonminez." + (qualifier != null ? qualifier : race) + ".form." + baseGroup(group) + "." + form;
	}
}
