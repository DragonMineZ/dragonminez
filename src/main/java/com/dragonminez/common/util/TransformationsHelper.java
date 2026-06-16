package com.dragonminez.common.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.util.lists.SaiyanForms;

import java.util.*;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TransformationsHelper {

	public static class OrderedFormEntry {
		private final String groupName;
		private final String formType;
		private final FormConfig.FormData formData;

		public OrderedFormEntry(String groupName, String formType, FormConfig.FormData formData) {
			this.groupName = groupName;
			this.formType = formType;
			this.formData = formData;
		}

		public String getGroupName() {
			return groupName;
		}

		public String getFormType() {
			return formType;
		}

		public FormConfig.FormData getFormData() {
			return formData;
		}
	}

	public static List<OrderedFormEntry> getOrderedFormsForRace(String raceName, List<String> formTypeOrder) {
		List<OrderedFormEntry> result = new ArrayList<>();
		Map<String, FormConfig> allGroups = ConfigManager.getAllFormsForRace(raceName);
		if (allGroups == null || allGroups.isEmpty()) return result;

		Map<String, Integer> typeOrderIndex = new HashMap<>();
		if (formTypeOrder != null) {
			for (int i = 0; i < formTypeOrder.size(); i++) {
				typeOrderIndex.put(formTypeOrder.get(i).toLowerCase(Locale.ROOT), i);
			}
		}

		for (Map.Entry<String, FormConfig> entry : allGroups.entrySet()) {
			String groupName = entry.getKey();
			FormConfig formConfig = entry.getValue();
			if (formConfig == null) continue;

			String formType = formConfig.getFormType() != null ? formConfig.getFormType().toLowerCase(Locale.ROOT) : "";
			if (formType.equalsIgnoreCase("android")) continue;
			if (!typeOrderIndex.isEmpty() && !typeOrderIndex.containsKey(formType)) continue;

			for (FormConfig.FormData formData : formConfig.getForms().values()) {
				if (formData == null) continue;
				result.add(new OrderedFormEntry(groupName, formType, formData));
			}
		}

		result.sort(Comparator
				.comparingInt((OrderedFormEntry item) -> typeOrderIndex.getOrDefault(item.getFormType(), Integer.MAX_VALUE))
				.thenComparingInt(item -> item.getFormData().getUnlockOnSkillLevel() != null ? item.getFormData().getUnlockOnSkillLevel() : 0)
				.thenComparing(item -> item.getFormData().getName(), String.CASE_INSENSITIVE_ORDER));

		return result;
	}

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

		for (FormConfig.FormData formData : formConfig.getForms().values()) {
			if (isFormUnlocked(statsData, formType, formData.getUnlockOnSkillLevel()) && meetsMasteryRequisite(statsData, formData)) {
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

		for (FormConfig.FormData formData : formConfig.getForms().values()) {
			if (isStackFormUnlocked(statsData, formType, formData.getUnlockOnSkillLevel()) && meetsMasteryRequisite(statsData, formData)) {
				unlockedForms.add(formData);
			}
		}
		return unlockedForms;
	}

	private static String getSkillNameForType(String formType) {
		String lower = formType.toLowerCase();
		if (lower.contains("super")) return "superforms";
		else if (lower.contains("legendary")) return "legendaryforms";
		else if (lower.contains("god")) return "godforms";
		else if (lower.contains("android")) return "androidforms";
		else return formType;
	}

	private static boolean isFormUnlocked(StatsData statsData, String formType, int requiredLevel) {
		return statsData.getSkills().isUnlockedAtLevel(getSkillNameForType(formType), requiredLevel);
	}

	private static boolean isStackFormUnlocked(StatsData statsData, String formType, int requiredLevel) {
		if (formType == null || formType.isEmpty()) return false;
		return statsData.getSkills().isUnlockedAtLevel(formType.toLowerCase(Locale.ROOT), requiredLevel);
	}

	private static boolean meetsMasteryRequisite(StatsData statsData, FormConfig.FormData formData) {
		if (formData == null) return false;
		if (statsData.getPlayer() != null && statsData.getPlayer().isCreative()) return true;
		String req = formData.getFormRequisite();
		double need = formData.getUnlockOnMastery();
		if (req == null || req.isEmpty() || need <= 0.0) return true;
		int dot = req.indexOf('.');
		if (dot <= 0 || dot >= req.length() - 1) return true;
		String reqGroup = req.substring(0, dot);
		String reqForm = req.substring(dot + 1);
		double have = statsData.getCharacter().getFormMasteries().getMastery(reqGroup, reqForm);
		return have >= need;
	}

	private static boolean isFormSelectable(StatsData statsData, String groupName, FormConfig.FormData formData, boolean stack) {
		double mastery = stack
				? statsData.getCharacter().getStackFormMasteries().getMastery(groupName, formData.getName())
				: statsData.getCharacter().getFormMasteries().getMastery(groupName, formData.getName());

		boolean canAlways = formData.getCanAlwaysTransform()
				&& mastery >= formData.getAllowAlwaysTransformOnMastery();

		boolean usedBefore = stack
				? statsData.getCharacter().getStackFormsUsedBefore().getFormGroup(groupName).contains(formData.getName())
				: statsData.getCharacter().getFormsUsedBefore().getFormGroup(groupName).contains(formData.getName());
		boolean directIfUsed = formData.getDirectTransformationIfUsed()
				&& usedBefore
				&& mastery >= formData.getDirectTransformIfUsedOnMastery();

		return canAlways || directIfUsed;
	}

	public static String getGroupWithFirstAvailableForm(StatsData statsData) {
		String race = statsData.getCharacter().getRaceName();
		Map<String, FormConfig> allGroups = ConfigManager.getAllFormsForRace(race);
		if (allGroups == null || allGroups.isEmpty()) return null;

		List<String> preferredTypes = new ArrayList<>();
		if (statsData.getSkills().getSkillLevel("superforms") > 0) preferredTypes.add("superforms");
		if (statsData.getSkills().getSkillLevel("legendaryforms") > 0) preferredTypes.add("legendaryforms");
		if (statsData.getSkills().getSkillLevel("godforms") > 0) preferredTypes.add("godforms");
		if (statsData.getSkills().getSkillLevel("androidforms") > 0) preferredTypes.add("androidforms");

		if (preferredTypes.isEmpty()) preferredTypes.addAll(Arrays.asList("superforms", "legendaryforms", "godforms", "androidforms"));

		for (String formType : preferredTypes) {
			String group = findBestGroupByType(statsData, race, allGroups, formType);
			if (group != null) return group;
		}

		return null;
	}

	public static String getFirstAvailableForm(StatsData statsData) {
		String group = getGroupWithFirstAvailableForm(statsData);
		if (group == null) return null;
		FormConfig config = ConfigManager.getFormGroup(statsData.getCharacter().getRaceName(), group);
		if (config == null) return null;
		if (config.getGroupName().contains("oozaru") && !statsData.getCharacter().isHasSaiyanTail()) return null;

		Optional<FormConfig.FormData> firstForm = config.getForms().values().stream()
				.filter(f -> isFormUnlocked(statsData, config.getFormType(), f.getUnlockOnSkillLevel()) && meetsMasteryRequisite(statsData, f))
				.min(Comparator.comparingInt(FormConfig.FormData::getUnlockOnSkillLevel));

		return firstForm.map(FormConfig.FormData::getName).orElse(null);
	}

	public static int getFirstAvailableFormLevel(StatsData statsData) {
		String group = getGroupWithFirstAvailableForm(statsData);
		if (group == null) return -1;
		FormConfig config = ConfigManager.getFormGroup(statsData.getCharacter().getRaceName(), group);
		if (config == null) return -1;
		if (config.getGroupName().contains("oozaru") && !statsData.getCharacter().isHasSaiyanTail()) return -1;

		Optional<FormConfig.FormData> firstForm = config.getForms().values().stream()
				.filter(f -> isFormUnlocked(statsData, config.getFormType(), f.getUnlockOnSkillLevel()) && meetsMasteryRequisite(statsData, f))
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

		if (preferredTypes.isEmpty()) {
			for (FormConfig config : allGroups.values()) {
				String formType = config.getFormType();
				if (formType != null && !formType.isEmpty() && !preferredTypes.contains(formType.toLowerCase())) preferredTypes.add(formType.toLowerCase());
			}
		}

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

		Optional<FormConfig.FormData> firstForm = config.getForms().values().stream()
				.filter(f -> isStackFormUnlocked(statsData, config.getFormType(), f.getUnlockOnSkillLevel()) && meetsMasteryRequisite(statsData, f))
				.min(Comparator.comparingInt(FormConfig.FormData::getUnlockOnSkillLevel));

		return firstForm.map(FormConfig.FormData::getName).orElse(null);
	}

	public static int getFirstAvailableStackFormLevel(StatsData statsData) {
		String group = getGroupWithFirstAvailableStackForm(statsData);
		if (group == null) return -1;

		FormConfig config = ConfigManager.getStackFormGroup(group);
		if (config == null) return -1;

		Optional<FormConfig.FormData> firstForm = config.getForms().values().stream()
				.filter(f -> isStackFormUnlocked(statsData, config.getFormType(), f.getUnlockOnSkillLevel()) && meetsMasteryRequisite(statsData, f))
				.min(Comparator.comparingInt(FormConfig.FormData::getUnlockOnSkillLevel));

		return firstForm.map(FormConfig.FormData::getUnlockOnSkillLevel).orElse(-1);
	}

	private static String findBestGroupByType(StatsData statsData, String race, Map<String, FormConfig> allGroups, String formType) {
		int lowestReqLevel = Integer.MAX_VALUE;
		String selectedGroup = null;

		for (Map.Entry<String, FormConfig> entry : allGroups.entrySet()) {
			String groupKey = entry.getKey();
			FormConfig config = ConfigManager.getFormGroup(race, groupKey);
			if (config == null || !config.getFormType().toLowerCase().contains(formType)) continue;
			if (config.getGroupName().contains("oozaru") && !statsData.getCharacter().isHasSaiyanTail()) continue;

			final FormConfig formConfig = config;
			int[] reqLevels = config.getForms().values().stream()
					.filter(f -> meetsMasteryRequisite(statsData, f))
					.mapToInt(FormConfig.FormData::getUnlockOnSkillLevel)
					.filter(req -> isFormUnlocked(statsData, formConfig.getFormType(), req))
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
			if (config == null || !config.getFormType().toLowerCase().contains(formType)) continue;

			final FormConfig formConfig = config;
			int[] reqLevels = config.getForms().values().stream()
					.filter(f -> meetsMasteryRequisite(statsData, f))
					.mapToInt(FormConfig.FormData::getUnlockOnSkillLevel)
					.filter(req -> isStackFormUnlocked(statsData, formConfig.getFormType(), req))
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
		String group = statsData.getCharacter().hasActiveForm() ? statsData.getCharacter().getActiveFormGroup() : statsData.getCharacter().getSelectedFormGroup();
		if (group == null || group.isEmpty()) return null;
		FormConfig config = ConfigManager.getFormGroup(race, group);
		if (config == null) return null;

		boolean isAndroidUpgraded = statsData.getStatus().isAndroidUpgraded();
		boolean isAndroidGroup = "androidforms".equalsIgnoreCase(group);
		boolean isGodGroup = config.getFormType().toLowerCase().contains("god");
		boolean requiresSaiyanTail = "oozaru".equalsIgnoreCase(config.getGroupName());

		if (!isAndroidUpgraded && isAndroidGroup) return null;
		if (isAndroidUpgraded && !isAndroidGroup && !isGodGroup) return null;
		if (requiresSaiyanTail && !statsData.getCharacter().isHasSaiyanTail()) return null;

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
					if (entry.getKey().equalsIgnoreCase(currentFormName)) foundCurrent = true;
					continue;
				}

				nextFormConfig = entry.getValue();
				break;
			}
		}
		if (nextFormConfig != null) {
			return (isFormUnlocked(statsData, config.getFormType(), nextFormConfig.getUnlockOnSkillLevel()) && meetsMasteryRequisite(statsData, nextFormConfig)) ? nextFormConfig : null;
		}
		return nextFormConfig;
	}

	public static boolean isOozaruForm(FormConfig.FormData formData) {
		return formData != null && SaiyanForms.OOZARU.equalsIgnoreCase(formData.getName());
	}

	public static boolean shouldAutoChargeOozaru(Player player, StatsData statsData) {
		if (player == null || statsData == null) return false;
		if (statsData.getStatus().getSelectedAction() != ActionMode.FORM) return false;
		if (statsData.getCharacter().hasActiveForm() || statsData.getCharacter().hasActiveStackForm()) return false;

		FormConfig.FormData nextForm = getNextAvailableForm(statsData);
		if (!isOozaruForm(nextForm)) return false;

		Level playerLevel = player.level();
		boolean realMoon = playerLevel.isNight()
				&& isFullMoon(playerLevel)
				&& isLookingAtMoon(player);
		return realMoon || isLookingAtFakeMoon(player);
	}

	private static boolean isFullMoon(Level level) {
		long day = Math.floorDiv(level.getDayTime(), 24000L);
		return day % 8L == 0L;
	}

	private static boolean isLookingAtMoon(Player player) {
		return player.level().canSeeSky(player.blockPosition()) && player.getLookAngle().y > 0.95D;
	}

	private static boolean isLookingAtFakeMoon(Player player) {
		Level level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		double range = 200.0D;
		Vec3 end = eye.add(look.scale(range));
		AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(3.0D);

		for (KiBlastEntity ki : level.getEntitiesOfClass(KiBlastEntity.class, searchBox)) {
			if (ki.getKiRenderType() != KiBlastEntity.RENDER_FAKE_MOON || !ki.isParked()) continue;
			AABB hitbox = ki.getBoundingBox().inflate(2.0D);
			if (hitbox.clip(eye, end).isPresent()) return true;
		}
		return false;
	}

	public static FormConfig.FormData getNextAvailableStackForm(StatsData statsData) {
		String group = statsData.getCharacter().hasActiveStackForm() ? statsData.getCharacter().getActiveStackFormGroup() : statsData.getCharacter().getSelectedStackFormGroup();

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
					if (entry.getKey().equalsIgnoreCase(currentFormName)) foundCurrent = true;
					continue;
				}

				nextFormConfig = entry.getValue();
				break;
			}
		}
		if (nextFormConfig != null) {
			return (isStackFormUnlocked(statsData, config.getFormType(), nextFormConfig.getUnlockOnSkillLevel()) && meetsMasteryRequisite(statsData, nextFormConfig)) ? nextFormConfig : null;
		}
		return nextFormConfig;
	}

	public static boolean canDescend(StatsData statsData) {
		if (!statsData.getCharacter().hasActiveForm()) return false;

		String race = statsData.getCharacter().getRaceName();
		String group = statsData.getCharacter().getActiveFormGroup();
		String currentForm = statsData.getCharacter().getActiveForm();

		if ("androidforms".equalsIgnoreCase(group) && "androidbase".equalsIgnoreCase(currentForm)) return false;
		if (isDefaultGroup(race, group)) return !"frostdemon".equals(race) && !"majin".equals(race) && !"bioandroid".equals(race);
		return true;
	}

	public static boolean canStackDescend(StatsData statsData) {
		if (!statsData.getCharacter().hasActiveStackForm()) return false;

		String group = statsData.getCharacter().getActiveStackFormGroup();
		String currentForm = statsData.getCharacter().getActiveStackForm();

		return !group.equalsIgnoreCase("") && !currentForm.equalsIgnoreCase("");
	}

	private static List<String> getSelectableFormNames(StatsData statsData, String race, String groupName) {
		if (groupName == null || groupName.isEmpty()) return Collections.emptyList();
		List<FormConfig.FormData> unlockedForms = getUnlockedForms(statsData, race, groupName);
		return unlockedForms.stream()
				.filter(formData -> isFormSelectable(statsData, groupName, formData, false))
				.map(FormConfig.FormData::getName)
				.toList();
	}

	private static List<String> getSelectableStackFormNames(StatsData statsData, String groupName) {
		if (groupName == null || groupName.isEmpty()) return Collections.emptyList();
		List<FormConfig.FormData> unlockedForms = getUnlockedStackForms(statsData, groupName);
		return unlockedForms.stream()
				.filter(formData -> isFormSelectable(statsData, groupName, formData, true))
				.map(FormConfig.FormData::getName)
				.toList();
	}

	public static boolean ensureSelectedFormDefault(StatsData statsData) {
		String form = statsData.getCharacter().getSelectedForm();
		if (form != null && !form.isEmpty()) return false;

		String group = getGroupWithFirstAvailableForm(statsData);
		String firstForm = getFirstAvailableForm(statsData);
		if (group == null || group.isEmpty() || firstForm == null || firstForm.isEmpty()) return false;

		statsData.getCharacter().setSelectedFormGroup(group);
		statsData.getCharacter().setSelectedForm(firstForm);
		return true;
	}

	public static boolean ensureSelectedStackFormDefault(StatsData statsData) {
		String form = statsData.getCharacter().getSelectedStackForm();
		if (form != null && !form.isEmpty()) return false;

		String group = getGroupWithFirstAvailableStackForm(statsData);
		String firstForm = getFirstAvailableStackForm(statsData);
		if (group == null || group.isEmpty() || firstForm == null || firstForm.isEmpty()) return false;

		statsData.getCharacter().setSelectedStackFormGroup(group);
		statsData.getCharacter().setSelectedStackForm(firstForm);
		return true;
	}

	public static void cycleSelectedFormGroup(StatsData statsData, boolean reverse) {
		String race = statsData.getCharacter().getRaceName();
		Map<String, FormConfig> allGroups = ConfigManager.getAllFormsForRace(race);
		if (allGroups.isEmpty()) return;

		String selectedFormGroup = statsData.getCharacter().getSelectedFormGroup();
		int offset = reverse ? -1 : 1;

		List<String> unlockedGroups = new ArrayList<>();
		for (String groupKey : allGroups.keySet()) {
			if (!getSelectableFormNames(statsData, race, groupKey).isEmpty()) unlockedGroups.add(groupKey);
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
			if (unlockedFormNames.isEmpty()) return;
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
		if (allGroups.isEmpty()) return;

		String selectedStackFormGroup = statsData.getCharacter().getSelectedStackFormGroup();
		int offset = reverse ? -1 : 1;

		List<String> unlockedGroups = new ArrayList<>();
		for (String groupKey : allGroups.keySet()) {
			if (!getSelectableStackFormNames(statsData, groupKey).isEmpty()) unlockedGroups.add(groupKey);
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
			if (unlockedStackFormNames.isEmpty()) return;
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
		if (config == null) return null;

		FormConfig.FormData prev = null;
		for (FormConfig.FormData f : config.getForms().values()) {
			if (f.getName().equalsIgnoreCase(current)) return prev;
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
			if (f.getName().equalsIgnoreCase(current)) return prev;
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
		} else return 0;
	}

	public static boolean hasGodFormActive(StatsData statsData) {
		var character = statsData.getCharacter();
		if (!character.hasActiveForm()) return false;
		FormConfig config = ConfigManager.getFormGroup(character.getRaceName(), character.getActiveFormGroup());
		return config != null && config.getFormType() != null && config.getFormType().toLowerCase(Locale.ROOT).contains("god");
	}

	public static boolean isInstantTransmissionBlocked(StatsData requester, StatsData target) {
		if (target.getStatus().isAndroidUpgraded()) return true;
		return hasGodFormActive(target) && requester.getSkills().getSkillLevel("godforms") < 1;
	}
}