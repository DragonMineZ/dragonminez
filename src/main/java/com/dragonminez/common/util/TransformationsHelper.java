package com.dragonminez.common.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsData;

import java.util.*;

public class TransformationsHelper {

	public static List<FormConfig.FormData> getUnlockedForms(StatsData statsData, String raceName, String groupName) {
		List<FormConfig.FormData> unlockedForms = new ArrayList<>();
		FormConfig formConfig = ConfigManager.getFormGroup(raceName, groupName);
		if (formConfig == null) return unlockedForms;

		boolean isAndroidGroup = "androidforms".equalsIgnoreCase(groupName);
		boolean isGodGroup = formConfig.getFormType().equalsIgnoreCase("god");
		boolean isAndroidUpgraded = statsData.getStatus().isAndroidUpgraded();

		if (isAndroidGroup && !isAndroidUpgraded) return unlockedForms;
		if (isAndroidUpgraded && !isAndroidGroup && !isGodGroup && "human".equalsIgnoreCase(raceName)) return unlockedForms;

		String formType = formConfig.getFormType();
		int skillLevel = getSkillLevelForType(statsData, formType);

		for (FormConfig.FormData formData : formConfig.getForms().values()) {
			if (formData.getUnlockOnSkillLevel() <= skillLevel) {
				unlockedForms.add(formData);
			}
		}
		return unlockedForms;
	}

	private static int getSkillLevelForType(StatsData statsData, String formType) {
		return switch (formType.toLowerCase()) {
			case "super" -> statsData.getSkills().getSkillLevel("superform");
			case "god" -> statsData.getSkills().getSkillLevel("godform");
			case "legendary" -> statsData.getSkills().getSkillLevel("legendaryforms");
			case "android" -> statsData.getSkills().getSkillLevel("androidforms");
			default -> 0;
		};
	}

	public static String getGroupWithFirstAvailableForm(StatsData statsData) {
		String race = statsData.getCharacter().getRaceName();
		Map<String, FormConfig> allGroups = ConfigManager.getAllFormsForRace(race);
		int lowestReqLevel = Integer.MAX_VALUE;
		String selectedGroup = null;
		for (String groupKey : allGroups.keySet()) {
			FormConfig config = ConfigManager.getFormGroup(race, groupKey);
			int[] reqLevels = config.getForms().values().stream()
					.mapToInt(FormConfig.FormData::getUnlockOnSkillLevel)
					.sorted()
					.toArray();

			if (reqLevels.length > 0 && reqLevels[0] < lowestReqLevel) {
				lowestReqLevel = reqLevels[0];
				selectedGroup = groupKey;
			}
		}

		return selectedGroup;
	}

	public static FormConfig.FormData getNextAvailableForm(StatsData statsData) {
		String race = statsData.getCharacter().getRaceName();
		String group = statsData.getCharacter().hasActiveForm() ?
				statsData.getCharacter().getActiveFormGroup() :
				statsData.getCharacter().getSelectedFormGroup();

		if (group == null || group.isEmpty()) return null;

		FormConfig config = ConfigManager.getFormGroup(race, group);
		if (config == null) return null;

		boolean isAndroidUpgraded = statsData.getStatus().isAndroidUpgraded();
		boolean isAndroidGroup = "androidforms".equalsIgnoreCase(group);
		boolean isGodGroup = config.getFormType().equalsIgnoreCase("god");

		if (!isAndroidUpgraded && isAndroidGroup) return null;
		if (isAndroidUpgraded && !isAndroidGroup && !isGodGroup && "human".equalsIgnoreCase(race)) return null;

		String currentFormName = statsData.getCharacter().getActiveForm();
		boolean foundCurrent = currentFormName.isEmpty();

		for (Map.Entry<String, FormConfig.FormData> entry : config.getForms().entrySet()) {
			if (!foundCurrent) {
				if (entry.getKey().equalsIgnoreCase(currentFormName)) {
					foundCurrent = true;
				}
				continue;
			}

			int reqLevel = entry.getValue().getUnlockOnSkillLevel();
			int myLevel = getSkillLevelForType(statsData, config.getFormType());

			if (reqLevel <= myLevel) return entry.getValue();
		}
		return null;
	}

	public static boolean canDescend(StatsData statsData) {
		if (!statsData.getCharacter().hasActiveForm()) return false;

		String race = statsData.getCharacter().getRaceName();
		String group = statsData.getCharacter().getActiveFormGroup();
		String currentForm = statsData.getCharacter().getActiveForm();

		if ("androidforms".equalsIgnoreCase(group) && "androidbase".equalsIgnoreCase(currentForm)) return false;
		if (isDefaultGroup(race, group)) {
			return !"frostdemon".equals(race) && !"majin".equals(race) && !"bioandroid".equals(race);
		}
		return true;
	}

	public static void cycleSelectedFormGroup(StatsData statsData) {
		String race = statsData.getCharacter().getRaceName();
		Map<String, FormConfig> allGroups = ConfigManager.getAllFormsForRace(race);
		if (allGroups.isEmpty()) return;

		List<String> unlockedGroups = new ArrayList<>();
		for (String groupKey : allGroups.keySet()) {
			if (!getUnlockedForms(statsData, race, groupKey).isEmpty()) {
				unlockedGroups.add(groupKey);
			}
		}

		if (unlockedGroups.isEmpty()) return;

		String current = statsData.getCharacter().getSelectedFormGroup();
		int index = unlockedGroups.indexOf(current);

		String nextGroup;
		if (index == -1 || index >= unlockedGroups.size() - 1) {
			nextGroup = unlockedGroups.get(0);
		} else {
			nextGroup = unlockedGroups.get(index + 1);
		}

        if(nextGroup != null ) statsData.getCharacter().setSelectedFormGroup(nextGroup);

    }

	private static boolean isDefaultGroup(String race, String group) {
		return switch (race) {
			case "frostdemon" -> "evolutionforms".equals(group);
			case "majin" -> "pureforms".equals(group);
			case "bioandroid" -> "bioevolution".equals(group);
			default -> false;
		};
	}

	public static FormConfig.FormData getPreviousForm(StatsData statsData) {
		if (!statsData.getCharacter().hasActiveForm()) return null;

		String race = statsData.getCharacter().getRaceName();
		String group = statsData.getCharacter().getActiveFormGroup();
		String current = statsData.getCharacter().getActiveForm();

		FormConfig config = ConfigManager.getFormGroup(race, group);
		if (config == null) return null;

		FormConfig.FormData prev = null;
		for (FormConfig.FormData f : config.getForms().values()) {
			if (f.getName().equalsIgnoreCase(current)) {
				return prev;
			}
			prev = f;
		}
		return null;
	}

	public static boolean canStackKaioken(StatsData data) {
		if (!data.getSkills().hasSkill("kaioken") || data.getSkills().getSkillLevel("kaioken") <= 0) return false;
		if (!data.getCharacter().hasActiveForm()) return true;

		boolean globalEnabled = ConfigManager.getServerConfig().getGameplay().isKaiokenStackable();
		if (!globalEnabled) return false;

		FormConfig.FormData currentForm = data.getCharacter().getActiveFormData();
		if (currentForm != null) {
			return currentForm.isKaiokenStackable();
		}

		return false;
	}

	public static int getMaxKaiokenPhase(int skillLevel) {
		if (skillLevel <= 0) return 0;
		return (skillLevel + 1) / 2;
	}

	public static String getKaiokenName(int phase) {
		return switch (phase) {
			case 1 -> "x2";
			case 2 -> "x3";
			case 3 -> "x4";
			case 4 -> "x10";
			case 5 -> "x20";
			default -> "";
		};
	}

	public static float getKaiokenHealthDrain(StatsData data) {
		if (!data.getSkills().isSkillActive("kaioken")) return 0;

		int phase = data.getStatus().getActiveKaiokenPhase();
		
		float finalDrain = 1;
		if (data.getCharacter().hasActiveForm()) {
			FormConfig.FormData form = data.getCharacter().getActiveFormData();
			if (form != null) {
				finalDrain = (float) (data.getMaxHealth() * ConfigManager.getSkillsConfig().getDrainRateForLevel("kaioken", phase) * form.getKaiokenDrainMultiplier());
			}
		} else {
			finalDrain = (float) (data.getMaxHealth() * ConfigManager.getSkillsConfig().getDrainRateForLevel("kaioken", phase));
		}

		float vitReduction = getKaiokenVitReduction(data);
		finalDrain *= (1.0f - vitReduction);

		return finalDrain;
	}

	private static float getKaiokenVitReduction(StatsData data) {
		int maxStatValue = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
		int currentVit = data.getStats().getVitality();

		float vitRatio = Math.min(1.0f, (float) currentVit / maxStatValue);

		float minThreshold = 0.1f;
		if (vitRatio <= minThreshold) return 0.0f;

		float scaledRatio = (vitRatio - minThreshold) / (1.0f - minThreshold);
		float maxReduction = 0.40f;

		return maxReduction * scaledRatio * scaledRatio;
	}

	public static String getFirstFormGroup(String groupName, String raceName) {
		FormConfig formConfig = ConfigManager.getFormGroup(raceName, groupName);
		if (formConfig == null) return null;
		if ("androidforms".equalsIgnoreCase(groupName)) return "superandroid";

		Optional<String> firstForm = formConfig.getForms().keySet().stream().findFirst();
		return firstForm.orElse(null);
	}
}