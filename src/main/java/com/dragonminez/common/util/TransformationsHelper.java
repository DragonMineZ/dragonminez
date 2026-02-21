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
		if (isAndroidUpgraded && !isAndroidGroup && !isGodGroup) return unlockedForms;

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

		if (!isAndroidUpgraded && isAndroidGroup) {
			return null;
		}
		if (isAndroidUpgraded && !isAndroidGroup && !isGodGroup) {
			return null;
		}

		String currentFormName = statsData.getCharacter().getActiveForm();
		String nextFormName;
		FormConfig.FormData nextFormConfig = null;
		if (currentFormName == null || currentFormName.isEmpty()) {
			nextFormName = statsData.getCharacter().getSelectedForm();
            nextFormConfig = config.getForm(nextFormName);
        } else {
			boolean foundCurrent = false;
			for (Map.Entry<String, FormConfig.FormData> entry : config.getForms().entrySet()) {
				if (!foundCurrent) {
					if (entry.getKey().equalsIgnoreCase(currentFormName)) {
						foundCurrent = true;
					}
					continue;
				}

				nextFormConfig = entry.getValue();
				break;
			}
		}
		if (nextFormConfig != null) {
			int reqLevel = nextFormConfig.getUnlockOnSkillLevel();
			int myLevel = getSkillLevelForType(statsData, config.getFormType());
			return reqLevel <= myLevel ? nextFormConfig : null;
		}
		return nextFormConfig;
	}

	public static FormConfig.FormData getNextAvailableStackForm(StatsData statsData) {
		String group = statsData.getCharacter().hasActiveStackForm() ?
				statsData.getCharacter().getActiveStackFormGroup() :
				statsData.getCharacter().getSelectedStackFormGroup();

		if (group == null || group.isEmpty()) return null;

		FormConfig config = ConfigManager.getStackFormGroup(group);
		if (config == null) return null;

		String currentFormName = statsData.getCharacter().getActiveStackForm();
		String nextFormName;
		FormConfig.FormData nextFormConfig = null;
		if (currentFormName == null || currentFormName.isEmpty()) {
			nextFormName = statsData.getCharacter().getSelectedStackForm();
			nextFormConfig = config.getForm(nextFormName);
		} else {
			boolean foundCurrent = false;
			for (Map.Entry<String, FormConfig.FormData> entry : config.getForms().entrySet()) {
				if (!foundCurrent) {
					if (entry.getKey().equalsIgnoreCase(currentFormName)) {
						foundCurrent = true;
					}
					continue;
				}

				nextFormConfig = entry.getValue();
				break;
			}
		}
		if (nextFormConfig != null) {
			int reqLevel = nextFormConfig.getUnlockOnSkillLevel();
			int myLevel = getSkillLevelForStackType(statsData, config.getFormType());
			return reqLevel <= myLevel ? nextFormConfig : null;
		}
		return nextFormConfig;
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

	public static void cycleSelectedFormGroup(StatsData statsData, boolean reverse) {
		String race = statsData.getCharacter().getRaceName();
		Map<String, FormConfig> allGroups = ConfigManager.getAllFormsForRace(race);
		if (allGroups.isEmpty()) {
			return;
		}

		String selectedFormGroup = statsData.getCharacter().getSelectedFormGroup();
		int offset = reverse ? -1 : 1;

		List<String> unlockedGroups = new ArrayList<>();
		for (String groupKey : allGroups.keySet()) {
			if (!getUnlockedForms(statsData, race, groupKey).isEmpty()) {
				unlockedGroups.add(groupKey);
			}
		}

		if (unlockedGroups.isEmpty()) {
			return;
		}

		int groupIndex = unlockedGroups.indexOf(selectedFormGroup);

		List<FormConfig.FormData> unlockedForms = getUnlockedForms(statsData, race, selectedFormGroup);
		List<String> unlockedFormNames = unlockedForms.stream()
				.filter(formData -> formData.isCanAlwaysTransform() ||
						(formData.isDirectTransformation()
								&& statsData.getCharacter().getFormsUsedBefore().getFormGroup(selectedFormGroup).contains(formData.getName())))
				.map(FormConfig.FormData::getName)
				.toList();
		String selectedForm = statsData.getCharacter().getSelectedForm();
		int formIndex = unlockedFormNames.indexOf(selectedForm) + offset;
		if (formIndex < 0) {
			groupIndex--;
			if (groupIndex < 0) {
				groupIndex = unlockedGroups.size() - 1;
			}
			String nextGroup = unlockedGroups.get(groupIndex);
			if(nextGroup != null) {
				statsData.getCharacter().setSelectedFormGroup(nextGroup);
			}
			unlockedForms = getUnlockedForms(statsData, race, nextGroup);
			unlockedFormNames = unlockedForms.stream()
					.filter(formData -> formData.isCanAlwaysTransform() ||
							(formData.isDirectTransformation()
									&& statsData.getCharacter().getFormsUsedBefore().getFormGroup(selectedFormGroup).contains(formData.getName())))
					.map(FormConfig.FormData::getName)
					.toList();
			formIndex = unlockedFormNames.size() - 1;
		}
		if (formIndex >= unlockedFormNames.size()) {
			groupIndex++;
			if (groupIndex >= unlockedGroups.size()) {
				groupIndex = 0;
			}
			String nextGroup = unlockedGroups.get(groupIndex);
			unlockedForms = getUnlockedForms(statsData, race, nextGroup);
			if(nextGroup != null) {
				statsData.getCharacter().setSelectedFormGroup(nextGroup);
			}
			unlockedFormNames = unlockedForms.stream()
					.filter(formData -> formData.isCanAlwaysTransform() ||
							(formData.isDirectTransformation()
									&& statsData.getCharacter().getFormsUsedBefore().getFormGroup(selectedFormGroup).contains(formData.getName())))
					.map(FormConfig.FormData::getName)
					.toList();
			formIndex = 0;
		}

		String nextForm = unlockedFormNames.get(formIndex);
		if(nextForm != null ) {
			statsData.getCharacter().setSelectedForm(nextForm);
		}
    }

	public static void cycleSelectedStackFormGroup(StatsData statsData, boolean reverse) {
		Map<String, FormConfig> allGroups = ConfigManager.getAllStackForms();
		if (allGroups.isEmpty()) {
			return;
		}

		String selectedStackFormGroup = statsData.getCharacter().getSelectedStackFormGroup();
		int offset = reverse ? -1 : 1;

		List<String> unlockedGroups = new ArrayList<>();
		for (String groupKey : allGroups.keySet()) {
			if (!getUnlockedStackForms(statsData, groupKey).isEmpty()) {
				unlockedGroups.add(groupKey);
			}
		}

		if (unlockedGroups.isEmpty()) {
			return;
		}

		int groupIndex = unlockedGroups.indexOf(selectedStackFormGroup);

		List<FormConfig.FormData> unlockedStackForms = getUnlockedStackForms(statsData, selectedStackFormGroup);
		List<String> unlockedStackFormNames = unlockedStackForms.stream()
				.filter(formData -> formData.isCanAlwaysTransform() ||
						(formData.isDirectTransformation()
								&& statsData.getCharacter().getStackFormsUsedBefore().getFormGroup(selectedStackFormGroup).contains(formData.getName())))
				.map(FormConfig.FormData::getName)
				.toList();
		String selectedStackForm = statsData.getCharacter().getSelectedStackForm();
		int formIndex = unlockedStackFormNames.indexOf(selectedStackForm) + offset;
		if (formIndex < 0) {
			groupIndex--;
			if (groupIndex < 0) {
				groupIndex = unlockedGroups.size() - 1;
			}
			String nextGroup = unlockedGroups.get(groupIndex);
			if(nextGroup != null) {
				statsData.getCharacter().setSelectedStackFormGroup(nextGroup);
			}
			unlockedStackForms = getUnlockedStackForms(statsData, nextGroup);
			unlockedStackFormNames = unlockedStackForms.stream()
					.filter(formData -> formData.isCanAlwaysTransform() ||
							(formData.isDirectTransformation()
									&& statsData.getCharacter().getStackFormsUsedBefore().getFormGroup(selectedStackFormGroup).contains(formData.getName())))
					.map(FormConfig.FormData::getName)
					.toList();
			formIndex = unlockedStackFormNames.size() - 1;
		}
		if (formIndex >= unlockedStackFormNames.size()) {
			groupIndex++;
			if (groupIndex >= unlockedGroups.size()) {
				groupIndex = 0;
			}
			String nextGroup = unlockedGroups.get(groupIndex);
			unlockedStackForms = getUnlockedStackForms(statsData, nextGroup);
			if(nextGroup != null) {
				statsData.getCharacter().setSelectedStackFormGroup(nextGroup);
			}
			unlockedStackFormNames = unlockedStackForms.stream()
					.filter(formData -> formData.isCanAlwaysTransform() ||
							(formData.isDirectTransformation()
									&& statsData.getCharacter().getStackFormsUsedBefore().getFormGroup(selectedStackFormGroup).contains(formData.getName())))
					.map(FormConfig.FormData::getName)
					.toList();
			formIndex = 0;
		}

		String nextForm = unlockedStackFormNames.get(formIndex);
		if(nextForm != null ) {
			statsData.getCharacter().setSelectedStackForm(nextForm);
		}
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