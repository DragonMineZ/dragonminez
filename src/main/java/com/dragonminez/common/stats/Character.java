package com.dragonminez.common.stats;

import net.minecraft.nbt.CompoundTag;

public class Character {
    private int race;
    private String gender;
    private String characterClass;

    public static final int RACE_HUMAN = 0;
    public static final int RACE_SAIYAN = 1;
    public static final int RACE_NAMEKIAN = 2;
    public static final int RACE_COLD_DEMON = 3;
    public static final int RACE_BIO_ANDROID = 4;
    public static final int RACE_MAJIN = 5;

    public static final String[] RACE_NAMES = {"human", "saiyan", "namekian", "colddemon", "bioandroid", "majin"};
    public static final boolean[] HAS_GENDER = {true, true, false, false, false, true};

    public static final String GENDER_MALE = "male";
    public static final String GENDER_FEMALE = "female";

    public static final String CLASS_WARRIOR = "warrior";
    public static final String CLASS_SPIRITUALIST = "spiritualist";
    public static final String CLASS_MARTIALARTIST = "martialartist";

    private int hairId;
    private int bodyType;
    private int eyesType;
    private int bodyColor;
    private int bodyColor2;
    private int bodyColor3;
    private int hairColor;
    private int eye1Color;
    private int eye2Color;
    private int auraColor;

    public Character() {
        this.race = RACE_HUMAN;
        this.gender = GENDER_MALE;
        this.characterClass = CLASS_WARRIOR;
        this.hairId = 0;
        this.bodyType = 0;
        this.eyesType = 0;
        this.bodyColor = 0;
        this.bodyColor2 = 0;
        this.bodyColor3 = 0;
        this.hairColor = 921617;
        this.eye1Color = 0;
        this.eye2Color = 0;
        this.auraColor = 8388607;
    }

    public int getRace() { return race; }
    public String getGender() { return gender; }
    public String getCharacterClass() { return characterClass; }
    public int getHairId() { return hairId; }
    public int getBodyType() { return bodyType; }
    public int getEyesType() { return eyesType; }
    public int getBodyColor() { return bodyColor; }
    public int getBodyColor2() { return bodyColor2; }
    public int getBodyColor3() { return bodyColor3; }
    public int getHairColor() { return hairColor; }
    public int getEye1Color() { return eye1Color; }
    public int getEye2Color() { return eye2Color; }
    public int getAuraColor() { return auraColor; }

    public void setRace(int race) { this.race = Math.max(0, Math.min(5, race)); }
    public void setGender(String gender) { this.gender = gender; }
    public void setCharacterClass(String characterClass) { this.characterClass = characterClass; }
    public void setHairId(int hairId) { this.hairId = hairId; }
    public void setBodyType(int bodyType) { this.bodyType = bodyType; }
    public void setEyesType(int eyesType) { this.eyesType = eyesType; }
    public void setBodyColor(int bodyColor) { this.bodyColor = bodyColor; }
    public void setBodyColor2(int bodyColor2) { this.bodyColor2 = bodyColor2; }
    public void setBodyColor3(int bodyColor3) { this.bodyColor3 = bodyColor3; }
    public void setHairColor(int hairColor) { this.hairColor = hairColor; }
    public void setEye1Color(int eye1Color) { this.eye1Color = eye1Color; }
    public void setEye2Color(int eye2Color) { this.eye2Color = eye2Color; }
    public void setAuraColor(int auraColor) { this.auraColor = auraColor; }

    public String getRaceName() {
        if (race >= 0 && race < RACE_NAMES.length) {
            return RACE_NAMES[race];
        }
        return RACE_NAMES[0];
    }

    public boolean canHaveGender() {
        if (race >= 0 && race < HAS_GENDER.length) {
            return HAS_GENDER[race];
        }
        return true;
    }

    public static int getRaceIdByName(String name) {
        for (int i = 0; i < RACE_NAMES.length; i++) {
            if (RACE_NAMES[i].equalsIgnoreCase(name)) {
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
        tag.putInt("BodyColor", bodyColor);
        tag.putInt("BodyColor2", bodyColor2);
        tag.putInt("BodyColor3", bodyColor3);
        tag.putInt("HairColor", hairColor);
        tag.putInt("Eye1Color", eye1Color);
        tag.putInt("Eye2Color", eye2Color);
        tag.putInt("AuraColor", auraColor);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.race = tag.getInt("Race");
        this.gender = tag.getString("Gender");
        this.characterClass = tag.getString("Class");
        this.hairId = tag.getInt("HairId");
        this.bodyType = tag.getInt("BodyType");
        this.eyesType = tag.getInt("EyesType");
        this.bodyColor = tag.getInt("BodyColor");
        this.bodyColor2 = tag.getInt("BodyColor2");
        this.bodyColor3 = tag.getInt("BodyColor3");
        this.hairColor = tag.getInt("HairColor");
        this.eye1Color = tag.getInt("Eye1Color");
        this.eye2Color = tag.getInt("Eye2Color");
        this.auraColor = tag.getInt("AuraColor");
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

