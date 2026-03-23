package com.dragonminez.common.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsData;

import java.util.*;

public class TransformationsHelper {

	public static List<FormConfig.FormData> getUnlockedForms(StatsData statsData, String raceName, String groupName) {
		List<FormConfig.FormData> unlockedForms = new ArrayList<>();
		FormConfig formConfig = ConfigManager.getFormGroup(raceName, groupName);
		if (formConfig == null) {
			return unlockedForms;
		}

		boolean isAndroidGroup = "androidforms".equalsIgnoreCase(groupName);
		boolean isGodGroup = formConfig.getFormType().equalsIgnoreCase("god");
		boolean isAndroidUpgraded = statsData.getStatus().isAndroidUpgraded();
		boolean requiresSaiyanTail = "oozaru".equalsIgnoreCase(formConfig.getGroupName());

		if (isAndroidGroup && !isAndroidUpgraded) {
			return unlockedForms;
		}
		if (isAndroidUpgraded && !isAndroidGroup && !isGodGroup) {
			return unlockedForms;
		}
		if (requiresSaiyanTail && !statsData.getCharacter().isHasSaiyanTail()) {
			return unlockedForms;
		}

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
		if (formConfig == null) {
			return unlockedForms;
		}

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
			default -> statsData.getSkills().getSkillLevel(formType);
		};
	}

	private static int getSkillLevelForStackType(StatsData statsData, String formType) {
		return statsData.getSkills().getSkillLevel(formType);
	}

	public static String getGroupWithFirstAvailableForm(StatsData statsData) {
		String race = statsData.getCharacter().getRaceName();
		Map<String, FormConfig> allGroups = ConfigManager.getAllFormsForRace(race);
		if (allGroups == null || allGroups.isEmpty()) return null;

		List<String> preferredTypes = new ArrayList<>();
		if (statsData.getSkills().getSkillLevel("superform") > 0) preferredTypes.add("super");
		if (statsData.getSkills().getSkillLevel("legendaryforms") > 0) preferredTypes.add("legendary");
		if (statsData.getSkills().getSkillLevel("godform") > 0) preferredTypes.add("god");
		if (statsData.getSkills().getSkillLevel("androidforms") > 0) preferredTypes.add("android");

		if (preferredTypes.isEmpty()) preferredTypes.add("super");

		for (String formType : preferredTypes) {
			String group = findBestGroupByType(statsData, race, allGroups, formType);
			if (group != null) return group;
		}

		return null;
	}

	public static String getFirstAvailableForm(StatsData statsData) {
		String group = getGroupWithFirstAvailableForm(statsData);
		if (group == null) {
			return null;
		}

		FormConfig config = ConfigManager.getFormGroup(statsData.getCharacter().getRaceName(), group);
		if (config == null) {
			return null;
		}

		if (config.getGroupName().contains("oozaru") && !statsData.getCharacter().isHasSaiyanTail()) {
			return null;
		}

		int currentSkillLevel = getSkillLevelForType(statsData, config.getFormType());
		Optional<FormConfig.FormData> firstForm = config.getForms().values().stream()
				.filter(f -> f.getUnlockOnSkillLevel() <= currentSkillLevel)
				.min(Comparator.comparingInt(FormConfig.FormData::getUnlockOnSkillLevel));

		return firstForm.map(FormConfig.FormData::getName).orElse(null);
	}

	public static int getFirstAvailableFormLevel(StatsData statsData) {
		String group = getGroupWithFirstAvailableForm(statsData);
		if (group == null) {
			return -1;
		}

		FormConfig config = ConfigManager.getFormGroup(statsData.getCharacter().getRaceName(), group);
		if (config == null) {
			return -1;
		}

		if (config.getGroupName().contains("oozaru") && !statsData.getCharacter().isHasSaiyanTail()) {
			return -1;
		}

		int currentSkillLevel = getSkillLevelForType(statsData, config.getFormType());
		Optional<FormConfig.FormData> firstForm = config.getForms().values().stream()
				.filter(f -> f.getUnlockOnSkillLevel() <= currentSkillLevel)
				.min(Comparator.comparingInt(FormConfig.FormData::getUnlockOnSkillLevel));

		return firstForm.map(FormConfig.FormData::getUnlockOnSkillLevel).orElse(-1);
	}

	public static String getGroupWithFirstAvailableStackForm(StatsData statsData) {
		Map<String, FormConfig> allGroups = ConfigManager.getAllStackForms();
		if (allGroups == null || allGroups.isEmpty()) return null;

		List<String> preferredTypes = new ArrayList<>();
		for (FormConfig config : allGroups.values()) {
			String formType = config.getFormType();
			if (formType == null || formType.isEmpty()) continue;
			if (statsData.getSkills().getSkillLevel(formType) > 0 && !preferredTypes.contains(formType.toLowerCase())) {
				preferredTypes.add(formType.toLowerCase());
			}
		}

		if (preferredTypes.isEmpty()) preferredTypes.add("kaioken");

		for (String formType : preferredTypes) {
			String group = findBestStackGroupByType(statsData, allGroups, formType);
			if (group != null) return group;
		}

		return null;
	}

	public static String getFirstAvailableStackForm(StatsData statsData) {
		String group = getGroupWithFirstAvailableStackForm(statsData);
		if (group == null) return null;

		FormConfig config = ConfigManager.getStackFormGroup(group);
		if (config == null) return null;

		int currentSkillLevel = getSkillLevelForStackType(statsData, config.getFormType());
		Optional<FormConfig.FormData> firstForm = config.getForms().values().stream()
				.filter(f -> f.getUnlockOnSkillLevel() <= currentSkillLevel)
				.min(Comparator.comparingInt(FormConfig.FormData::getUnlockOnSkillLevel));

		return firstForm.map(FormConfig.FormData::getName).orElse(null);
	}

	public static int getFirstAvailableStackFormLevel(StatsData statsData) {
		String group = getGroupWithFirstAvailableStackForm(statsData);
		if (group == null) return -1;

		FormConfig config = ConfigManager.getStackFormGroup(group);
		if (config == null) return -1;

		int currentSkillLevel = getSkillLevelForStackType(statsData, config.getFormType());
		Optional<FormConfig.FormData> firstForm = config.getForms().values().stream()
				.filter(f -> f.getUnlockOnSkillLevel() <= currentSkillLevel)
				.min(Comparator.comparingInt(FormConfig.FormData::getUnlockOnSkillLevel));

		return firstForm.map(FormConfig.FormData::getUnlockOnSkillLevel).orElse(-1);
	}

	private static String findBestGroupByType(StatsData statsData, String race, Map<String, FormConfig> allGroups, String formType) {
		int lowestReqLevel = Integer.MAX_VALUE;
		String selectedGroup = null;

		for (Map.Entry<String, FormConfig> entry : allGroups.entrySet()) {
			String groupKey = entry.getKey();
			FormConfig config = ConfigManager.getFormGroup(race, groupKey);
			if (config == null || !config.getFormType().equalsIgnoreCase(formType)) continue;
			if (config.getGroupName().contains("oozaru") && !statsData.getCharacter().isHasSaiyanTail()) continue;

			int currentSkillLevel = getSkillLevelForType(statsData, config.getFormType());
			int[] reqLevels = config.getForms().values().stream()
					.mapToInt(FormConfig.FormData::getUnlockOnSkillLevel)
					.filter(req -> req <= currentSkillLevel)
					.sorted()
					.toArray();

			if (reqLevels.length > 0 && reqLevels[0] < lowestReqLevel) {
				lowestReqLevel = reqLevels[0];
				selectedGroup = groupKey;
			}
		}

		return selectedGroup;
	}

	private static String findBestStackGroupByType(StatsData statsData, Map<String, FormConfig> allGroups, String formType) {
		int lowestReqLevel = Integer.MAX_VALUE;
		String selectedGroup = null;

		for (Map.Entry<String, FormConfig> entry : allGroups.entrySet()) {
			String groupKey = entry.getKey();
			FormConfig config = entry.getValue();
			if (config == null || !config.getFormType().equalsIgnoreCase(formType)) continue;

			int currentSkillLevel = getSkillLevelForStackType(statsData, config.getFormType());
			int[] reqLevels = config.getForms().values().stream()
					.mapToInt(FormConfig.FormData::getUnlockOnSkillLevel)
					.filter(req -> req <= currentSkillLevel)
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

		if (group == null || group.isEmpty()) {
			return null;
		}

		FormConfig config = ConfigManager.getFormGroup(race, group);
		if (config == null) {
			return null;
		}

		boolean isAndroidUpgraded = statsData.getStatus().isAndroidUpgraded();
		boolean isAndroidGroup = "androidforms".equalsIgnoreCase(group);
		boolean isGodGroup = config.getFormType().equalsIgnoreCase("god");
		boolean requiresSaiyanTail = "oozaru".equalsIgnoreCase(config.getGroupName());

		if (!isAndroidUpgraded && isAndroidGroup) {
			return null;
		}
		if (isAndroidUpgraded && !isAndroidGroup && !isGodGroup) {
			return null;
		}
		if (requiresSaiyanTail && !statsData.getCharacter().isHasSaiyanTail()) {
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
		if (!statsData.getCharacter().hasActiveForm()) {
			return false;
		}

		String race = statsData.getCharacter().getRaceName();
		String group = statsData.getCharacter().getActiveFormGroup();
		String currentForm = statsData.getCharacter().getActiveForm();

		if ("androidforms".equalsIgnoreCase(group) && "androidbase".equalsIgnoreCase(currentForm)) {
			return false;
		}
		if (isDefaultGroup(race, group)) {
			return !"frostdemon".equals(race) && !"majin".equals(race) && !"bioandroid".equals(race);
		}
		return true;
	}

	public static boolean canStackDescend(StatsData statsData) {
		if (!statsData.getCharacter().hasActiveStackForm()) {
			return false;
		}

		String group = statsData.getCharacter().getActiveStackFormGroup();
		String currentForm = statsData.getCharacter().getActiveStackForm();

		return !group.equalsIgnoreCase("") && !currentForm.equalsIgnoreCase("");
	}

	private static List<String> getSelectableFormNames(StatsData statsData, String race, String groupName) {
		if (groupName == null || groupName.isEmpty()) return Collections.emptyList();
		List<FormConfig.FormData> unlockedForms = getUnlockedForms(statsData, race, groupName);
		return unlockedForms.stream()
				.filter(formData -> formData.getCanAlwaysTransform() ||
						(formData.getDirectTransformation()
								&& statsData.getCharacter().getFormsUsedBefore().getFormGroup(groupName).contains(formData.getName())))
				.map(FormConfig.FormData::getName)
				.toList();
	}

	private static List<String> getSelectableStackFormNames(StatsData statsData, String groupName) {
		if (groupName == null || groupName.isEmpty()) return Collections.emptyList();
		List<FormConfig.FormData> unlockedForms = getUnlockedStackForms(statsData, groupName);
		return unlockedForms.stream()
				.filter(formData -> formData.getCanAlwaysTransform() ||
						(formData.getDirectTransformation()
								&& statsData.getCharacter().getStackFormsUsedBefore().getFormGroup(groupName).contains(formData.getName())))
				.map(FormConfig.FormData::getName)
				.toList();
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
			if (!getSelectableFormNames(statsData, race, groupKey).isEmpty()) {
				unlockedGroups.add(groupKey);
			}
		}

		if (unlockedGroups.isEmpty()) {
			statsData.getCharacter().setSelectedFormGroup("");
			statsData.getCharacter().setSelectedForm("");
			return;
		}

		if (selectedFormGroup == null || selectedFormGroup.isEmpty() || !unlockedGroups.contains(selectedFormGroup)) {
			selectedFormGroup = getGroupWithFirstAvailableForm(statsData);
			statsData.getCharacter().setSelectedFormGroup(selectedFormGroup);
			statsData.getCharacter().setSelectedForm(getFirstAvailableForm(statsData));
		}

		List<String> unlockedFormNames = getSelectableFormNames(statsData, race, selectedFormGroup);
		if (unlockedFormNames.isEmpty()) {
			selectedFormGroup = getGroupWithFirstAvailableForm(statsData);
			statsData.getCharacter().setSelectedFormGroup(selectedFormGroup);
			statsData.getCharacter().setSelectedForm(getFirstAvailableForm(statsData));
			unlockedFormNames = getSelectableFormNames(statsData, race, selectedFormGroup);
			if (unlockedFormNames.isEmpty()) {
				return;
			}
		}

		int groupIndex = unlockedGroups.indexOf(selectedFormGroup);
		if (groupIndex < 0) groupIndex = 0;

		String selectedForm = statsData.getCharacter().getSelectedForm();
		int currentIndex = unlockedFormNames.indexOf(selectedForm);
		if (currentIndex < 0) {
			currentIndex = reverse ? unlockedFormNames.size() : -1;
		}

		int nextFormIndex = currentIndex + offset;
		if (nextFormIndex >= 0 && nextFormIndex < unlockedFormNames.size()) {
			statsData.getCharacter().setSelectedFormGroup(selectedFormGroup);
			statsData.getCharacter().setSelectedForm(unlockedFormNames.get(nextFormIndex));
			return;
		}

		for (int i = 0; i < unlockedGroups.size(); i++) {
			groupIndex = (groupIndex + offset + unlockedGroups.size()) % unlockedGroups.size();
			String nextGroup = unlockedGroups.get(groupIndex);
			List<String> nextGroupForms = getSelectableFormNames(statsData, race, nextGroup);
			if (nextGroupForms.isEmpty()) continue;

			int boundaryIndex = reverse ? nextGroupForms.size() - 1 : 0;
			statsData.getCharacter().setSelectedFormGroup(nextGroup);
			statsData.getCharacter().setSelectedForm(nextGroupForms.get(boundaryIndex));
			return;
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
			if (!getSelectableStackFormNames(statsData, groupKey).isEmpty()) {
				unlockedGroups.add(groupKey);
			}
		}

		if (unlockedGroups.isEmpty()) {
			statsData.getCharacter().setSelectedStackFormGroup("");
			statsData.getCharacter().setSelectedStackForm("");
			return;
		}

		if (selectedStackFormGroup == null || selectedStackFormGroup.isEmpty() || !unlockedGroups.contains(selectedStackFormGroup)) {
			selectedStackFormGroup = getGroupWithFirstAvailableStackForm(statsData);
			statsData.getCharacter().setSelectedStackFormGroup(selectedStackFormGroup);
			statsData.getCharacter().setSelectedStackForm(getFirstAvailableStackForm(statsData));
		}

		List<String> unlockedStackFormNames = getSelectableStackFormNames(statsData, selectedStackFormGroup);
		if (unlockedStackFormNames.isEmpty()) {
			selectedStackFormGroup = getGroupWithFirstAvailableStackForm(statsData);
			statsData.getCharacter().setSelectedStackFormGroup(selectedStackFormGroup);
			statsData.getCharacter().setSelectedStackForm(getFirstAvailableStackForm(statsData));
			unlockedStackFormNames = getSelectableStackFormNames(statsData, selectedStackFormGroup);
			if (unlockedStackFormNames.isEmpty()) {
				return;
			}
		}

		int groupIndex = unlockedGroups.indexOf(selectedStackFormGroup);
		if (groupIndex < 0) groupIndex = 0;

		String selectedStackForm = statsData.getCharacter().getSelectedStackForm();
		int currentIndex = unlockedStackFormNames.indexOf(selectedStackForm);
		if (currentIndex < 0) {
			currentIndex = reverse ? unlockedStackFormNames.size() : -1;
		}

		int nextFormIndex = currentIndex + offset;
		if (nextFormIndex >= 0 && nextFormIndex < unlockedStackFormNames.size()) {
			statsData.getCharacter().setSelectedStackFormGroup(selectedStackFormGroup);
			statsData.getCharacter().setSelectedStackForm(unlockedStackFormNames.get(nextFormIndex));
			return;
		}

		for (int i = 0; i < unlockedGroups.size(); i++) {
			groupIndex = (groupIndex + offset + unlockedGroups.size()) % unlockedGroups.size();
			String nextGroup = unlockedGroups.get(groupIndex);
			List<String> nextGroupForms = getSelectableStackFormNames(statsData, nextGroup);
			if (nextGroupForms.isEmpty()) continue;

			int boundaryIndex = reverse ? nextGroupForms.size() - 1 : 0;
			statsData.getCharacter().setSelectedStackFormGroup(nextGroup);
			statsData.getCharacter().setSelectedStackForm(nextGroupForms.get(boundaryIndex));
			return;
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
		if (config == null) {
			return null;
		}

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
		if (config == null) {
			return null;
		}

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