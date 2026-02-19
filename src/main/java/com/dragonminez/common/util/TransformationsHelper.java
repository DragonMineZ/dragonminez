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

	public static List<FormConfig.FormData> getUnlockedStackForms(StatsData statsData, String groupName) {
		List<FormConfig.FormData> unlockedForms = new ArrayList<>();
		FormConfig formConfig = ConfigManager.getStackFormGroup(groupName);
		if (formConfig == null) return unlockedForms;

		String formType = formConfig.getFormType();
		int skillLevel = getSkillLevelForStackType(statsData, formType);

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

	private static int getSkillLevelForStackType(StatsData statsData, String formType) {
		return statsData.getSkills().getSkillLevel(formType);
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

	public static FormConfig.FormData getNextAvailableStackForm(StatsData statsData) {
		String group = statsData.getCharacter().hasActiveStackForm() ?
				statsData.getCharacter().getActiveStackFormGroup() :
				statsData.getCharacter().getSelectedStackFormGroup();

		if (group == null || group.isEmpty()) return null;

		FormConfig config = ConfigManager.getStackFormGroup(group);
		if (config == null) return null;

		String currentFormName = statsData.getCharacter().getActiveStackForm();
		boolean foundCurrent = currentFormName.isEmpty();

		for (Map.Entry<String, FormConfig.FormData> entry : config.getForms().entrySet()) {
			if (!foundCurrent) {
				if (entry.getKey().equalsIgnoreCase(currentFormName)) {
					foundCurrent = true;
				}
				continue;
			}

			int reqLevel = entry.getValue().getUnlockOnSkillLevel();
			int myLevel = getSkillLevelForStackType(statsData, config.getFormType());

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

	public static boolean canStackDescend(StatsData statsData) {
		if (!statsData.getCharacter().hasActiveStackForm()) return false;

		String group = statsData.getCharacter().getActiveStackFormGroup();
		String currentForm = statsData.getCharacter().getActiveStackForm();

		return !group.equalsIgnoreCase("") && !currentForm.equalsIgnoreCase("");
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

	public static void cycleSelectedStackFormGroup(StatsData statsData) {
		Map<String, FormConfig> allGroups = ConfigManager.getAllStackForms();
		if (allGroups.isEmpty()) return;

		List<String> unlockedGroups = new ArrayList<>();
		for (String groupKey : allGroups.keySet()) {
			if (!getUnlockedStackForms(statsData, groupKey).isEmpty()) {
				unlockedGroups.add(groupKey);
			}
		}

		if (unlockedGroups.isEmpty()) return;

		String current = statsData.getCharacter().getSelectedStackFormGroup();
		int index = unlockedGroups.indexOf(current);

		String nextGroup;
		if (index == -1 || index >= unlockedGroups.size() - 1) {
			nextGroup = unlockedGroups.get(0);
		} else {
			nextGroup = unlockedGroups.get(index + 1);
		}

		if(nextGroup != null ) statsData.getCharacter().setSelectedStackFormGroup(nextGroup);

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

	public static FormConfig.FormData getPreviousStackForm(StatsData statsData) {
		if (!statsData.getCharacter().hasActiveStackForm()) return null;

		String group = statsData.getCharacter().getActiveStackFormGroup();
		String current = statsData.getCharacter().getActiveStackForm();

		FormConfig config = ConfigManager.getStackFormGroup(group);
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

	public static String getFirstFormGroup(String groupName, String raceName) {
		FormConfig formConfig = ConfigManager.getFormGroup(raceName, groupName);
		if (formConfig == null) return null;
		if ("androidforms".equalsIgnoreCase(groupName)) return "superandroid";

		Optional<String> firstForm = formConfig.getForms().keySet().stream().findFirst();
		return firstForm.orElse(null);
	}

	public static String getFirstStackFormGroup(String groupName) {
		FormConfig formConfig = ConfigManager.getStackFormGroup(groupName);
		if (formConfig == null) return null;

		Optional<String> firstForm = formConfig.getForms().keySet().stream().findFirst();
		return firstForm.orElse(null);
	}

	public static int getKaiokenPhase(StatsData stats) {
		if ("kaioken".equalsIgnoreCase(stats.getCharacter().getActiveStackFormGroup())) {
			return switch (stats.getCharacter().getActiveStackForm()) {
				case "x2" -> 1;
				case "x3" -> 2;
				case "x4" -> 3;
				case "x10" -> 4;
				case "x20" -> 5;
				default -> 6;
			};
		} else {
			return 0;
		}
	}
}