package com.dragonminez.common.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsData;

import java.util.*;

public class TransformationHelper {

    public static List<FormConfig.FormData> getUnlockedForms(StatsData statsData, String raceName, String groupName) {
        List<FormConfig.FormData> unlockedForms = new ArrayList<>();

        FormConfig formConfig = ConfigManager.getFormGroup(raceName, groupName);
        if (formConfig == null) {
            return unlockedForms;
        }

        String formType = formConfig.getFormType();
        int requiredLevel = 0;

        switch (formType.toLowerCase()) {
            case "super" -> requiredLevel = statsData.getSkills().getSkillLevel("superform");
            case "god" -> requiredLevel = statsData.getSkills().getSkillLevel("godform");
            case "legendary" -> requiredLevel = statsData.getSkills().getSkillLevel("legendaryforms");
            default -> requiredLevel = 0;
        }

        for (FormConfig.FormData formData : formConfig.getForms().values()) {
            if (formData.getUnlockOnSuperformLevel() <= requiredLevel) {
                unlockedForms.add(formData);
            }
        }

        return unlockedForms;
    }

    public static Map<String, List<FormConfig.FormData>> getAllUnlockedFormsByGroup(StatsData statsData, String raceName) {
        Map<String, List<FormConfig.FormData>> unlockedByGroup = new LinkedHashMap<>();

        Map<String, FormConfig> allForms = ConfigManager.getAllFormsForRace(raceName);

        for (Map.Entry<String, FormConfig> entry : allForms.entrySet()) {
            String groupName = entry.getKey();
            List<FormConfig.FormData> unlockedForms = getUnlockedForms(statsData, raceName, groupName);
            if (!unlockedForms.isEmpty()) {
                unlockedByGroup.put(groupName, unlockedForms);
            }
        }

        return unlockedByGroup;
    }

    public static boolean isFormUnlocked(StatsData statsData, String raceName, String groupName, String formName) {
        FormConfig formConfig = ConfigManager.getFormGroup(raceName, groupName);
        if (formConfig == null) {
            return false;
        }

        FormConfig.FormData formData = formConfig.getForm(formName);
        if (formData == null) {
            return false;
        }

        String formType = formConfig.getFormType();
        int requiredLevel = formData.getUnlockOnSuperformLevel();
        int currentLevel = 0;

        switch (formType.toLowerCase()) {
            case "super" -> currentLevel = statsData.getSkills().getSkillLevel("superform");
            case "god" -> currentLevel = statsData.getSkills().getSkillLevel("godform");
            case "legendary" -> currentLevel = statsData.getSkills().getSkillLevel("legendaryforms");
            default -> currentLevel = 0;
        }

        return currentLevel >= requiredLevel;
    }


    public static String getFormType(String raceName, String groupName) {
        FormConfig formConfig = ConfigManager.getFormGroup(raceName, groupName);
        return formConfig != null ? formConfig.getFormType() : "super";
    }

    public static int getNextUnlockLevel(StatsData statsData, String raceName, String groupName) {
        FormConfig formConfig = ConfigManager.getFormGroup(raceName, groupName);
        if (formConfig == null) {
            return -1;
        }

        String formType = formConfig.getFormType();
        int currentLevel = 0;

        switch (formType.toLowerCase()) {
            case "super" -> currentLevel = statsData.getSkills().getSkillLevel("superform");
            case "god" -> currentLevel = statsData.getSkills().getSkillLevel("godform");
            case "legendary" -> currentLevel = statsData.getSkills().getSkillLevel("legendaryforms");
        }

        int nextUnlock = Integer.MAX_VALUE;
        for (FormConfig.FormData formData : formConfig.getForms().values()) {
            int unlockLevel = formData.getUnlockOnSuperformLevel();
            if (unlockLevel > currentLevel && unlockLevel < nextUnlock) {
                nextUnlock = unlockLevel;
            }
        }

        return nextUnlock == Integer.MAX_VALUE ? -1 : nextUnlock;
    }

    public static List<String> getAvailableFormGroups(String raceName) {
        Map<String, FormConfig> allForms = ConfigManager.getAllFormsForRace(raceName);
        return new ArrayList<>(allForms.keySet());
    }

    public static int[] getTransformationCosts(String raceName, String formType) {
        RaceCharacterConfig config = ConfigManager.getRaceCharacter(raceName);
        if (config == null) {
            return new int[]{};
        }

        return switch (formType.toLowerCase()) {
            case "super" -> config.getSuperformTpCost() != null ? config.getSuperformTpCost() : new int[]{};
            case "god" -> config.getGodformTpCost() != null ? config.getGodformTpCost() : new int[]{};
            case "legendary" -> config.getLegendaryformsTpCost() != null ? config.getLegendaryformsTpCost() : new int[]{};
            default -> new int[]{};
        };
    }

    public static int getMaxTransformationLevel(String raceName, String formType) {
        int[] costs = getTransformationCosts(raceName, formType);
        return costs.length;
    }

    public static int getCostForLevel(String raceName, String formType, int level) {
        int[] costs = getTransformationCosts(raceName, formType);
        if (level < 1 || level > costs.length) {
            return -1;
        }
        return costs[level - 1];
    }

    public static boolean canPurchaseLevel(String raceName, String formType, int level) {
        int cost = getCostForLevel(raceName, formType, level);
        return cost >= 0;
    }

    public static FormConfig.FormData getNextForm(StatsData statsData, String raceName, String currentGroupName, String currentFormName) {
        FormConfig formConfig = ConfigManager.getFormGroup(raceName, currentGroupName);
        if (formConfig == null) {
            return null;
        }

        String formType = formConfig.getFormType();
        int currentSkillLevel = switch (formType.toLowerCase()) {
            case "super" -> statsData.getSkills().getSkillLevel("superform");
            case "god" -> statsData.getSkills().getSkillLevel("godform");
            case "legendary" -> statsData.getSkills().getSkillLevel("legendaryforms");
            default -> 0;
        };

        FormConfig.FormData currentForm = null;
        FormConfig.FormData nextForm = null;
        boolean foundCurrent = false;

        for (FormConfig.FormData formData : formConfig.getForms().values()) {
            if (formData.getUnlockOnSuperformLevel() > currentSkillLevel) {
                continue;
            }

            if (foundCurrent) {
                if (nextForm == null || formData.getUnlockOnSuperformLevel() < nextForm.getUnlockOnSuperformLevel()) {
                    nextForm = formData;
                }
            }

            if (formData.getName().equalsIgnoreCase(currentFormName)) {
                currentForm = formData;
                foundCurrent = true;
            }
        }

        if (!foundCurrent || nextForm == null) {
            return null;
        }

        if (nextForm.getUnlockOnSuperformLevel() > currentSkillLevel) {
            return null;
        }

        return nextForm;
    }

    public static boolean hasNextForm(StatsData statsData, String raceName, String currentGroupName, String currentFormName) {
        return getNextForm(statsData, raceName, currentGroupName, currentFormName) != null;
    }

    public static boolean canTransform(StatsData statsData) {
        if (!statsData.getCharacter().hasActiveForm()) {
            String raceName = statsData.getCharacter().getRaceName();
            String selectedGroup = statsData.getCharacter().getSelectedFormGroup();

            if (selectedGroup != null && !selectedGroup.isEmpty()) {
                List<FormConfig.FormData> unlockedForms = getUnlockedForms(statsData, raceName, selectedGroup);
                return !unlockedForms.isEmpty();
            }
            return false;
        }

        String raceName = statsData.getCharacter().getRaceName();
        String currentGroup = statsData.getCharacter().getActiveFormGroup();
        String currentForm = statsData.getCharacter().getActiveFormName();

        return hasNextForm(statsData, raceName, currentGroup, currentForm);
    }

    public static boolean hasKaiokenSelected(StatsData statsData) {
        int kaiokenLevel = statsData.getSkills().getSkillLevel("kaioken");
        return kaiokenLevel > 0 && statsData.getSkills().isSkillActive("kaioken");
    }
}

