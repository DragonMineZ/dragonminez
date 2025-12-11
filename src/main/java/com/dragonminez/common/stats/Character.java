package com.dragonminez.common.stats;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public class Character {
    private String race;
    private String gender;
    private String characterClass;

    private String selectedFormGroup = "";
    private String currentFormGroup = "";
    private String currentForm = "";
    private final FormMasteries formMasteries = new FormMasteries();

    public static final String GENDER_MALE = "male";
    public static final String GENDER_FEMALE = "female";

    public static final String CLASS_WARRIOR = "warrior";
    public static final String CLASS_SPIRITUALIST = "spiritualist";
    public static final String CLASS_MARTIALARTIST = "martialartist";

    private int hairId;
    private int bodyType;
    private int eyesType;
    private int noseType;
    private int mouthType;
	private int tattooType;
    private String bodyColor;
    private String bodyColor2;
    private String bodyColor3;
    private String hairColor;
    private String eye1Color;
    private String eye2Color;
    private String auraColor;

    public Character() {
        this.race = "human";
        this.gender = GENDER_MALE;
        this.characterClass = CLASS_WARRIOR;

        RaceCharacterConfig config = ConfigManager.getRaceCharacter("human");
        this.hairId = config.getDefaultHairType();
        this.bodyType = config.getDefaultBodyType();
        this.eyesType = config.getDefaultEyesType();
        this.noseType = config.getDefaultNoseType();
        this.mouthType = config.getDefaultMouthType();
		this.tattooType = config.getDefaultTattooType();
        this.bodyColor = config.getDefaultBodyColor() != null ? config.getDefaultBodyColor() : "#F5D5A6";
        this.bodyColor2 = config.getDefaultBodyColor2() != null ? config.getDefaultBodyColor2() : "#F5D5A6";
        this.bodyColor3 = config.getDefaultBodyColor3() != null ? config.getDefaultBodyColor3() : "#F5D5A6";
        this.hairColor = config.getDefaultHairColor() != null ? config.getDefaultHairColor() : "#000000";
        this.eye1Color = config.getDefaultEye1Color() != null ? config.getDefaultEye1Color() : "#000000";
        this.eye2Color = config.getDefaultEye2Color() != null ? config.getDefaultEye2Color() : "#000000";
        this.auraColor = config.getDefaultAuraColor() != null ? config.getDefaultAuraColor() : "#FFFFFF";
    }

    public String getRace() { return race; }
    public String getGender() { return gender; }
    public String getCharacterClass() { return characterClass; }
    public String getSelectedFormGroup() { return selectedFormGroup; }
    public String getActiveFormGroup() { return currentFormGroup; }
    public String getActiveFormName() { return currentForm; }
    public String getCurrentFormGroup() { return currentFormGroup; }
    public String getCurrentForm() { return currentForm; }
    public int getHairId() { return hairId; }
    public int getBodyType() { return bodyType; }
    public int getEyesType() { return eyesType; }
    public int getNoseType() { return noseType; }
    public int getMouthType() { return mouthType; }
	public int getTattooType() { return tattooType; }
    public String getBodyColor() { return bodyColor; }
    public String getBodyColor2() { return bodyColor2; }
    public String getBodyColor3() { return bodyColor3; }
    public String getHairColor() { return hairColor; }
    public String getEye1Color() { return eye1Color; }
    public String getEye2Color() { return eye2Color; }
    public String getAuraColor() { return auraColor; }
    public FormMasteries getFormMasteries() { return formMasteries; }

    public void setRace(String race) {
        if (race != null && ConfigManager.isRaceLoaded(race)) {
            this.race = race.toLowerCase();
        } else {
            this.race = "human";
        }
        if (!canHaveGender() && !gender.equals(GENDER_MALE)) {
            this.gender = GENDER_MALE;
        }
    }
    public void setGender(String gender) { this.gender = gender; }
    public void setCharacterClass(String characterClass) { this.characterClass = characterClass; }
    public void setHairId(int hairId) { this.hairId = hairId; }
    public void setBodyType(int bodyType) { this.bodyType = bodyType; }
    public void setEyesType(int eyesType) { this.eyesType = eyesType; }
    public void setNoseType(int noseType) { this.noseType = noseType; }
    public void setMouthType(int mouthType) { this.mouthType = mouthType; }
	public void setTattooType(int tattooType) { this.tattooType = tattooType; }
    public void setBodyColor(String bodyColor) { this.bodyColor = bodyColor; }
    public void setBodyColor2(String bodyColor2) { this.bodyColor2 = bodyColor2; }
    public void setBodyColor3(String bodyColor3) { this.bodyColor3 = bodyColor3; }
    public void setHairColor(String hairColor) { this.hairColor = hairColor; }
    public void setEye1Color(String eye1Color) { this.eye1Color = eye1Color; }
    public void setEye2Color(String eye2Color) { this.eye2Color = eye2Color; }
    public void setAuraColor(String auraColor) { this.auraColor = auraColor; }
    public void setSelectedFormGroup(String selectedFormGroup) { this.selectedFormGroup = selectedFormGroup; }
    public void setCurrentFormGroup(String currentFormGroup) { this.currentFormGroup = currentFormGroup; }
    public void setCurrentForm(String currentForm) { this.currentForm = currentForm; }

    public String getRaceName() {
        return race != null && !race.isEmpty() ? race : "human";
    }

    public static String[] getRaceNames() {
        List<String> raceNames = ConfigManager.getLoadedRaces();
        return raceNames.toArray(new String[0]);
    }

    public boolean canHaveGender() {
        RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(getRaceName());
        return raceConfig.hasGender();
    }

    public double getModelScaling() {
        RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(getRaceName());
        return raceConfig.getDefaultModelScaling();
    }


    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Race", race);
        tag.putString("Gender", gender);
        tag.putString("Class", characterClass);
        tag.putInt("HairId", hairId);
        tag.putInt("BodyType", bodyType);
        tag.putInt("EyesType", eyesType);
        tag.putInt("NoseType", noseType);
        tag.putInt("MouthType", mouthType);
        tag.putString("BodyColor", bodyColor);
        tag.putString("BodyColor2", bodyColor2);
        tag.putString("BodyColor3", bodyColor3);
        tag.putString("HairColor", hairColor);
        tag.putString("Eye1Color", eye1Color);
        tag.putString("Eye2Color", eye2Color);
        tag.putString("AuraColor", auraColor);
        tag.putString("SelectedFormGroup", selectedFormGroup);
        tag.putString("CurrentFormGroup", currentFormGroup);
        tag.putString("CurrentForm", currentForm);
        tag.put("FormMasteries", formMasteries.save());
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("Race", 8)) {
            this.race = tag.getString("Race");
        } else if (tag.contains("Race", 3)) {
            int oldRaceId = tag.getInt("Race");
            List<String> races = ConfigManager.getLoadedRaces();
            this.race = (oldRaceId >= 0 && oldRaceId < races.size()) ? races.get(oldRaceId) : "human";
        } else {
            this.race = "human";
        }

        this.gender = tag.getString("Gender");
        this.characterClass = tag.getString("Class");
        this.hairId = tag.getInt("HairId");
        this.bodyType = tag.getInt("BodyType");
        this.eyesType = tag.getInt("EyesType");
        this.noseType = tag.getInt("NoseType");
        this.mouthType = tag.getInt("MouthType");
        this.bodyColor = tag.getString("BodyColor");
        this.bodyColor2 = tag.getString("BodyColor2");
        this.bodyColor3 = tag.getString("BodyColor3");
        this.hairColor = tag.getString("HairColor");
        this.eye1Color = tag.getString("Eye1Color");
        this.eye2Color = tag.getString("Eye2Color");
        this.auraColor = tag.getString("AuraColor");
        this.selectedFormGroup = tag.getString("SelectedFormGroup");
        this.currentFormGroup = tag.getString("CurrentFormGroup");
        this.currentForm = tag.getString("CurrentForm");
        if (tag.contains("FormMasteries")) {
            formMasteries.load(tag.getCompound("FormMasteries"));
        }
    }

    public boolean hasActiveForm() {
        return !currentFormGroup.isEmpty() && !currentForm.isEmpty();
    }

    public void setActiveForm(String groupName, String formName) {
        this.currentFormGroup = groupName != null ? groupName : "";
        this.currentForm = formName != null ? formName : "";
    }

    public void clearActiveForm() {
        this.currentFormGroup = "";
        this.currentForm = "";
    }

    public FormConfig.FormData getActiveFormData() {
        if (!hasActiveForm()) {
            return null;
        }
        return ConfigManager.getForm(getRaceName(), currentFormGroup, currentForm);
    }

    public void copyFrom(Character other) {
        this.race = other.race;
        this.gender = other.gender;
        this.characterClass = other.characterClass;
        this.hairId = other.hairId;
        this.bodyType = other.bodyType;
        this.eyesType = other.eyesType;
        this.noseType = other.noseType;
        this.mouthType = other.mouthType;
        this.bodyColor = other.bodyColor;
        this.bodyColor2 = other.bodyColor2;
        this.bodyColor3 = other.bodyColor3;
        this.hairColor = other.hairColor;
        this.eye1Color = other.eye1Color;
        this.eye2Color = other.eye2Color;
        this.auraColor = other.auraColor;
        this.selectedFormGroup = other.selectedFormGroup;
        this.currentFormGroup = other.currentFormGroup;
        this.currentForm = other.currentForm;
        this.formMasteries.copyFrom(other.formMasteries);
    }
}
