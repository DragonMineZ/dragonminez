package com.dragonminez.common.stats;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairManager;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public class Character {
    private String race;
    private String gender;
    private String characterClass;

    private String selectedFormGroup = "";
    private String activeFormGroup = "";
	private String selectedForm = "";
    private String activeForm = "";
    private final FormMasteries formMasteries = new FormMasteries();
	private UsedForms formsUsedBefore = new UsedForms();

    private String selectedStackFormGroup = "";
    private String activeStackFormGroup = "";
	private String selectedStackForm = "";
    private String activeStackForm = "";
    private final FormMasteries stackFormMasteries = new FormMasteries();
	private UsedForms stackFormsUsedBefore = new UsedForms();

	private boolean hasSaiyanTail = true;

    public static final String GENDER_MALE = "male";
    public static final String GENDER_FEMALE = "female";

    public static final String CLASS_WARRIOR = "warrior";

    private int hairId;
    private CustomHair hairBase = new CustomHair();
	private CustomHair hairSSJ = new CustomHair();
	private CustomHair hairSSJ2 = new CustomHair();
	private CustomHair hairSSJ3 = new CustomHair();
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

	private Boolean armored;

	public Character() {
		this.race = "human";
		this.gender = GENDER_MALE;
		this.characterClass = CLASS_WARRIOR;
		this.armored = false;

		RaceCharacterConfig config = ConfigManager.getRaceCharacter("human");
		if (config != null) {
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
		} else {
			this.hairId = 0;
			this.bodyType = 0;
			this.eyesType = 0;
			this.noseType = 0;
			this.mouthType = 0;
			this.tattooType = 0;
			this.bodyColor = "#F5D5A6";
			this.bodyColor2 = "#F5D5A6";
			this.bodyColor3 = "#F5D5A6";
			this.hairColor = "#000000";
			this.eye1Color = "#000000";
			this.eye2Color = "#000000";
			this.auraColor = "#FFFFFF";
		}
	}

	public String getRace() { return race; }
	public String getGender() { return gender; }
	public String getCharacterClass() { return characterClass; }
	public String getSelectedFormGroup() { return selectedFormGroup; }
	public String getActiveFormGroup() { return activeFormGroup; }
	public String getSelectedForm() {
		return selectedForm;
	}
	public String getActiveForm() { return activeForm; }
	public String getSelectedStackFormGroup() { return selectedStackFormGroup; }
	public String getActiveStackFormGroup() { return activeStackFormGroup; }
	public String getSelectedStackForm() {
		return selectedStackForm;
	}
	public String getActiveStackForm() { return activeStackForm; }
	public int getHairId() { return hairId; }

	public CustomHair getHairBase() {
		if (this.hairId > 0) return HairManager.getPresetHair(this.hairId, this.hairColor);
		return hairBase;
	}

	public CustomHair getHairSSJ() {
		if (this.hairId > 0) return HairManager.getPresetHairSSJ(this.hairId, this.hairColor);
		if (hairSSJ == null || hairSSJ.isEmpty()) return hairBase;
		return hairSSJ;
	}

	public CustomHair getHairSSJ2() {
		if (this.hairId > 0) return HairManager.getPresetHairSSJ2(this.hairId, this.hairColor);
		if (hairSSJ2 == null || hairSSJ2.isEmpty()) return (hairSSJ != null && !hairSSJ.isEmpty()) ? hairSSJ : hairBase;
		return hairSSJ2;
	}

	public CustomHair getHairSSJ3() {
		if (this.hairId > 0) return HairManager.getPresetHairSSJ3(this.hairId, this.hairColor);
		if (hairSSJ3 == null || hairSSJ3.isEmpty()) return (hairSSJ != null && !hairSSJ.isEmpty()) ? hairSSJ : hairBase;
		return hairSSJ3;
	}

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
	public UsedForms getFormsUsedBefore() {
		return formsUsedBefore;
	}
	public FormMasteries getStackFormMasteries() { return stackFormMasteries; }
	public UsedForms getStackFormsUsedBefore() {
		return stackFormsUsedBefore;
	}
	public boolean hasSaiyanTail() {
		return hasSaiyanTail;
	}
	public Boolean getArmored() {return armored;}

	public void setRace(String race) {
		if (race != null) {
			this.race = race.toLowerCase();
		} else {
			this.race = "human";
		}
		if (!canHaveGender() && !gender.equals(GENDER_MALE)) this.gender = GENDER_MALE;
	}

	public void setGender(String gender) { this.gender = gender; }
	public void setCharacterClass(String characterClass) { this.characterClass = characterClass; }
	public void setHairId(int hairId) { this.hairId = hairId; }
	public void setHairBase(CustomHair hairBase) { this.hairBase = hairBase; }
	public void setHairSSJ(CustomHair hairSSJ) { this.hairSSJ = hairSSJ; }
	public void setHairSSJ2(CustomHair hairSSJ2) { this.hairSSJ2 = hairSSJ2; }
	public void setHairSSJ3(CustomHair hairSSJ3) { this.hairSSJ3 = hairSSJ3; }
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
	public void setSelectedForm(String selectedForm) {
		this.selectedForm = selectedForm;
	}
	public void setSelectedStackFormGroup(String selectedStackFormGroup) { this.selectedStackFormGroup = selectedStackFormGroup; }
	public void setSelectedStackForm(String selectedStackForm) {
		this.selectedStackForm = selectedStackForm;
	}
	public void setSaiyanTail(boolean hasSaiyanTail) {
		this.hasSaiyanTail = hasSaiyanTail;
	}
	public void setArmored(Boolean armored) {this.armored = armored;}

    public String getRaceName() {
        return race != null && !race.isEmpty() ? race : "human";
    }

	public boolean canHaveGender() {
		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(getRaceName());
		return raceConfig != null ? raceConfig.hasGender() : true;
	}

	public float[] getModelScaling() {
		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(getRaceName());
		if (raceConfig != null) return raceConfig.getDefaultModelScaling();
		return new float[]{0.9375f, 0.9375f, 0.9375f};
	}

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Race", race);
        tag.putString("Gender", gender);
        tag.putString("Class", characterClass);
        tag.putInt("HairId", hairId);
		tag.put("HairBase", hairBase.save());
		tag.put("HairSSJ", hairSSJ.save());
		tag.put("HairSSJ2", hairSSJ2.save());
		tag.put("HairSSJ3", hairSSJ3.save());
        tag.putInt("BodyType", bodyType);
        tag.putInt("EyesType", eyesType);
        tag.putInt("NoseType", noseType);
        tag.putInt("MouthType", mouthType);
        tag.putInt("TattooType", tattooType);
        tag.putString("BodyColor", bodyColor);
        tag.putString("BodyColor2", bodyColor2);
        tag.putString("BodyColor3", bodyColor3);
        tag.putString("HairColor", hairColor);
        tag.putString("Eye1Color", eye1Color);
        tag.putString("Eye2Color", eye2Color);
        tag.putString("AuraColor", auraColor);
        tag.putString("SelectedFormGroup", selectedFormGroup);
        tag.putString("CurrentFormGroup", activeFormGroup);
		tag.putString("SelectedForm", selectedForm);
        tag.putString("CurrentForm", activeForm);
        tag.putString("SelectedStackFormGroup", selectedStackFormGroup);
        tag.putString("CurrentStackFormGroup", activeStackFormGroup);
		tag.putString("SelectedStackForm", selectedStackForm);
        tag.putString("CurrentStackForm", activeStackForm);
        tag.put("FormMasteries", formMasteries.save());
        tag.put("StackFormMasteries", stackFormMasteries.save());
		tag.put("FormsUsedBefore", formsUsedBefore.save());
		tag.put("StackFormsUsedBefore", stackFormsUsedBefore.save());
		tag.putBoolean("HasSaiyanTail", hasSaiyanTail);
        tag.putBoolean("isArmored", armored);
        return tag;
    }

	public void load(CompoundTag tag) {
		if (tag.contains("Race", 8)) {
			this.race = tag.getString("Race");
		} else if (tag.contains("Race", 3)) {
			int oldRaceId = tag.getInt("Race");
			List<String> races = ConfigManager.getLoadedRaces();
			this.race = (races != null && oldRaceId >= 0 && oldRaceId < races.size()) ? races.get(oldRaceId) : "human";
		} else this.race = "human";


		this.gender = tag.getString("Gender");
		this.characterClass = tag.getString("Class");
		this.hairId = tag.getInt("HairId");
		if (tag.contains("HairBase")) this.hairBase.load(tag.getCompound("HairBase"));
		if (tag.contains("HairSSJ")) this.hairSSJ.load(tag.getCompound("HairSSJ"));
		if (tag.contains("HairSSJ2")) this.hairSSJ2.load(tag.getCompound("HairSSJ2"));
		if (tag.contains("HairSSJ3")) this.hairSSJ3.load(tag.getCompound("HairSSJ3"));
        this.bodyType = tag.getInt("BodyType");
        this.eyesType = tag.getInt("EyesType");
        this.noseType = tag.getInt("NoseType");
        this.mouthType = tag.getInt("MouthType");
        this.tattooType = tag.getInt("TattooType");
        this.bodyColor = tag.getString("BodyColor");
        this.bodyColor2 = tag.getString("BodyColor2");
        this.bodyColor3 = tag.getString("BodyColor3");
        this.hairColor = tag.getString("HairColor");
        this.eye1Color = tag.getString("Eye1Color");
        this.eye2Color = tag.getString("Eye2Color");
        this.auraColor = tag.getString("AuraColor");
        this.selectedFormGroup = tag.getString("SelectedFormGroup");
        this.activeFormGroup = tag.getString("CurrentFormGroup");
		this.selectedForm = tag.getString("SelectedForm");
        this.activeForm = tag.getString("CurrentForm");
        if (tag.contains("FormMasteries")) formMasteries.load(tag.getCompound("FormMasteries"));
		if (tag.contains("FormsUsedBefore")) formsUsedBefore.load(tag.getCompound("FormsUsedBefore"));
        this.selectedStackFormGroup = tag.getString("SelectedStackFormGroup");
        this.activeStackFormGroup = tag.getString("CurrentStackFormGroup");
		this.selectedStackForm = tag.getString("SelectedStackForm");
        this.activeStackForm = tag.getString("CurrentStackForm");
        if (tag.contains("StackFormMasteries")) stackFormMasteries.load(tag.getCompound("StackFormMasteries"));
		if (tag.contains("StackFormsUsedBefore")) stackFormsUsedBefore.load(tag.getCompound("StackFormsUsedBefore"));
		this.hasSaiyanTail = tag.getBoolean("HasSaiyanTail");
        this.armored = tag.getBoolean("isArmored");
    }

	public boolean hasActiveForm() {
		return !activeFormGroup.isEmpty() && !activeForm.isEmpty();
	}

    public void setActiveForm(String groupName, String formName) {
        this.activeFormGroup = groupName != null ? groupName : "";
        this.activeForm = formName != null ? formName : "";
    }

    public void clearActiveForm() {
        this.activeFormGroup = "";
        this.activeForm = "";
    }

	public void clearSelectedForm() {
		this.selectedFormGroup = "";
		this.selectedForm = "";
	}

    public FormConfig.FormData getActiveFormData() {
        if (!hasActiveForm()) return null;
        return ConfigManager.getForm(getRaceName(), activeFormGroup, activeForm);
    }

    public boolean hasActiveStackForm() {
        return !activeStackFormGroup.isEmpty() && !activeStackForm.isEmpty();
    }

    public void setActiveStackForm(String groupName, String formName) {
        this.activeStackFormGroup = groupName != null ? groupName : "";
        this.activeStackForm = formName != null ? formName : "";
    }

    public void clearActiveStackForm() {
        this.activeStackFormGroup = "";
        this.activeStackForm = "";
    }

    public FormConfig.FormData getActiveStackFormData() {
        if (!hasActiveStackForm()) return null;
        return ConfigManager.getStackForm(activeStackFormGroup, activeStackForm);
    }

	public void saveAppearance(CompoundTag tag) {
		tag.putString("BodyColor", bodyColor);
		tag.putString("BodyColor2", bodyColor2);
		tag.putString("BodyColor3", bodyColor3);
		tag.putString("HairColor", hairColor);
		tag.putString("Eye1Color", eye1Color);
		tag.putString("Eye2Color", eye2Color);
		tag.putString("AuraColor", auraColor);
	}

	public void loadAppearance(CompoundTag tag) {
		if (tag.contains("BodyColor")) this.bodyColor = tag.getString("BodyColor");
		if (tag.contains("BodyColor2")) this.bodyColor2 = tag.getString("BodyColor2");
		if (tag.contains("BodyColor3")) this.bodyColor3 = tag.getString("BodyColor3");
		if (tag.contains("HairColor")) this.hairColor = tag.getString("HairColor");
		if (tag.contains("Eye1Color")) this.eye1Color = tag.getString("Eye1Color");
		if (tag.contains("Eye2Color")) this.eye2Color = tag.getString("Eye2Color");
		if (tag.contains("AuraColor")) this.auraColor = tag.getString("AuraColor");
	}

    public void copyFrom(Character other) {
        this.race = other.race;
        this.gender = other.gender;
        this.characterClass = other.characterClass;
        this.hairId = other.hairId;
		this.hairBase = other.hairBase.copy();
		this.hairSSJ = other.hairSSJ.copy();
		this.hairSSJ2 = other.hairSSJ2.copy();
		this.hairSSJ3 = other.hairSSJ3.copy();
        this.bodyType = other.bodyType;
        this.eyesType = other.eyesType;
        this.noseType = other.noseType;
        this.mouthType = other.mouthType;
        this.tattooType = other.tattooType;
        this.bodyColor = other.bodyColor;
        this.bodyColor2 = other.bodyColor2;
        this.bodyColor3 = other.bodyColor3;
        this.hairColor = other.hairColor;
        this.eye1Color = other.eye1Color;
        this.eye2Color = other.eye2Color;
        this.auraColor = other.auraColor;
        this.selectedFormGroup = other.selectedFormGroup;
        this.activeFormGroup = other.activeFormGroup;
        this.activeForm = other.activeForm;
        this.formMasteries.copyFrom(other.formMasteries);
        this.selectedStackFormGroup = other.selectedStackFormGroup;
        this.activeStackFormGroup = other.activeStackFormGroup;
        this.activeStackForm = other.activeStackForm;
        this.stackFormMasteries.copyFrom(other.stackFormMasteries);
        this.armored = other.armored;
    }
}