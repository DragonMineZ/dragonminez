package com.dragonminez.common.stats;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public class Character {
    private int race;
    private String gender;
    private String characterClass;

    public static final int RACE_HUMAN = 0;
    public static final int RACE_SAIYAN = 1;
    public static final int RACE_NAMEKIAN = 2;
    public static final int RACE_FROST_DEMON = 3;
    public static final int RACE_BIO_ANDROID = 4;
    public static final int RACE_MAJIN = 5;

    public static final String GENDER_MALE = "male";
    public static final String GENDER_FEMALE = "female";

    public static final String CLASS_WARRIOR = "warrior";
    public static final String CLASS_SPIRITUALIST = "spiritualist";
    public static final String CLASS_MARTIALARTIST = "martialartist";

    private int hairId;
    private int bodyType;
    private int eyesType;
    private String bodyColor;
    private String bodyColor2;
    private String bodyColor3;
    private String hairColor;
    private String eye1Color;
    private String eye2Color;
    private String auraColor;

    public Character() {
        this.race = RACE_HUMAN;
        this.gender = GENDER_MALE;
        this.characterClass = CLASS_WARRIOR;

        RaceCharacterConfig config = ConfigManager.getRaceCharacter("human");
        this.hairId = config.getDefaultHairType();
        this.bodyType = config.getDefaultBodyType();
        this.eyesType = config.getDefaultEyesType();
        this.bodyColor = config.getDefaultBodyColor();
        this.bodyColor2 = config.getDefaultBodyColor2();
        this.bodyColor3 = config.getDefaultBodyColor3();
        this.hairColor = config.getDefaultHairColor();
        this.eye1Color = config.getDefaultEye1Color();
        this.eye2Color = config.getDefaultEye2Color();
        this.auraColor = config.getDefaultAuraColor();
    }

    public int getRace() { return race; }
    public String getGender() { return gender; }
    public String getCharacterClass() { return characterClass; }
    public int getHairId() { return hairId; }
    public int getBodyType() { return bodyType; }
    public int getEyesType() { return eyesType; }
    public String getBodyColor() { return bodyColor; }
    public String getBodyColor2() { return bodyColor2; }
    public String getBodyColor3() { return bodyColor3; }
    public String getHairColor() { return hairColor; }
    public String getEye1Color() { return eye1Color; }
    public String getEye2Color() { return eye2Color; }
    public String getAuraColor() { return auraColor; }

    public void setRace(int race) {
        int maxRace = ConfigManager.getLoadedRaces().size() - 1;
        this.race = Math.max(0, Math.min(maxRace, race));
        if (!canHaveGender() && !gender.equals(GENDER_MALE)) {
            this.gender = GENDER_MALE;
        }
    }
    public void setGender(String gender) { this.gender = gender; }
    public void setCharacterClass(String characterClass) { this.characterClass = characterClass; }
    public void setHairId(int hairId) { this.hairId = hairId; }
    public void setBodyType(int bodyType) { this.bodyType = bodyType; }
    public void setEyesType(int eyesType) { this.eyesType = eyesType; }
    public void setBodyColor(String bodyColor) { this.bodyColor = bodyColor; }
    public void setBodyColor2(String bodyColor2) { this.bodyColor2 = bodyColor2; }
    public void setBodyColor3(String bodyColor3) { this.bodyColor3 = bodyColor3; }
    public void setHairColor(String hairColor) { this.hairColor = hairColor; }
    public void setEye1Color(String eye1Color) { this.eye1Color = eye1Color; }
    public void setEye2Color(String eye2Color) { this.eye2Color = eye2Color; }
    public void setAuraColor(String auraColor) { this.auraColor = auraColor; }

    public String getRaceName() {
        List<String> raceNames = ConfigManager.getLoadedRaces();
        if (race >= 0 && race < raceNames.size()) {
            return raceNames.get(race);
        }
        return raceNames.isEmpty() ? "human" : raceNames.get(0);
    }

    public static String[] getRaceNames() {
        List<String> raceNames = ConfigManager.getLoadedRaces();
        return raceNames.toArray(new String[0]);
    }

    public boolean canHaveGender() {
        String raceName = getRaceName();
        RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(raceName);
        return raceConfig.hasGender();
    }

    public static int getRaceIdByName(String name) {
        List<String> raceNames = ConfigManager.getLoadedRaces();
        for (int i = 0; i < raceNames.size(); i++) {
            if (raceNames.get(i).equalsIgnoreCase(name)) {
                return i;
            }
        }
        return RACE_HUMAN;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Race", race);
        tag.putString("Gender", gender);
        tag.putString("Class", characterClass);
        tag.putInt("HairId", hairId);
        tag.putInt("BodyType", bodyType);
        tag.putInt("EyesType", eyesType);
        tag.putString("BodyColor", bodyColor);
        tag.putString("BodyColor2", bodyColor2);
        tag.putString("BodyColor3", bodyColor3);
        tag.putString("HairColor", hairColor);
        tag.putString("Eye1Color", eye1Color);
        tag.putString("Eye2Color", eye2Color);
        tag.putString("AuraColor", auraColor);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.race = tag.getInt("Race");
        this.gender = tag.getString("Gender");
        this.characterClass = tag.getString("Class");
        this.hairId = tag.getInt("HairId");
        this.bodyType = tag.getInt("BodyType");
        this.eyesType = tag.getInt("EyesType");
        this.bodyColor = tag.getString("BodyColor");
        this.bodyColor2 = tag.getString("BodyColor2");
        this.bodyColor3 = tag.getString("BodyColor3");
        this.hairColor = tag.getString("HairColor");
        this.eye1Color = tag.getString("Eye1Color");
        this.eye2Color = tag.getString("Eye2Color");
        this.auraColor = tag.getString("AuraColor");
    }

    public void copyFrom(Character other) {
        this.race = other.race;
        this.gender = other.gender;
        this.characterClass = other.characterClass;
        this.hairId = other.hairId;
        this.bodyType = other.bodyType;
        this.eyesType = other.eyesType;
        this.bodyColor = other.bodyColor;
        this.bodyColor2 = other.bodyColor2;
        this.bodyColor3 = other.bodyColor3;
        this.hairColor = other.hairColor;
        this.eye1Color = other.eye1Color;
        this.eye2Color = other.eye2Color;
        this.auraColor = other.auraColor;
    }
}
