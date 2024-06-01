package com.yuseix.dragonminez.stats;

import com.yuseix.dragonminez.config.DMCAttrConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class DMZStatsAttributes {

    private int races;
    private int hairID, bodytype, eyesType;
    private int strength = 5;
    private int defense = 5;
    private int constitution = 5, curBody, curStam, stamina = 15;

    private int zpoints;
    private int KiPower = 5;

    private int energy = 5, currentEnergy;

    private String gender = "Male";

    private int bodyColor, bodyColor2,bodyColor3,eye1Color,eye2Color,hairColor,auraColor;

    private boolean AcceptCharacter = false;


    private final Player player;

    public DMZStatsAttributes(Player player) {
        this.player = player;
    }

    public int getZpoints() {
        return zpoints;
    }

    public void setZpoints(int zpoints) {
        this.zpoints = zpoints;
        DMZCapabilities.sync(player);
    }

    public int getBodyColor() {
        return bodyColor;
    }

    public void setBodyColor(int bodyColor) {
        this.bodyColor = bodyColor;
        DMZCapabilities.sync(player);
    }

    public int getBodyColor2() {
        return bodyColor2;
    }

    public void setBodyColor2(int bodyColor2) {
        this.bodyColor2 = bodyColor2;
        DMZCapabilities.sync(player);
    }

    public int getBodyColor3() {
        return bodyColor3;
    }

    public void setBodyColor3(int bodyColor3) {
        this.bodyColor3 = bodyColor3;
        DMZCapabilities.sync(player);
    }

    public int getEye1Color() {
        return eye1Color;

    }

    public void setEye1Color(int eye1Color) {
        this.eye1Color = eye1Color;
        DMZCapabilities.sync(player);
    }

    public int getEye2Color() {
        return eye2Color;
    }

    public void setEye2Color(int eye2Color) {
        this.eye2Color = eye2Color;
        DMZCapabilities.sync(player);
    }

    public int getHairColor() {
        return hairColor;
    }

    public void setHairColor(int hairColor) {
        this.hairColor = hairColor;
        DMZCapabilities.sync(player);
    }

    public int getAuraColor() {
        return auraColor;
    }

    public void setAuraColor(int auraColor) {
        this.auraColor = auraColor;
        DMZCapabilities.sync(player);
    }

    public int addStrength(int points) {

        if (strength <= DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get()) {
            strength += points;
        }
        DMZCapabilities.sync(player);

        return strength;
    }

    public int addDefense(int points) {

        if (defense <= DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get()) {
            defense += points;
        }
        DMZCapabilities.sync(player);

        return defense;
    }

    public int addCon(int points) {

        if (constitution <= DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get()) {
            constitution += points;
        }
        DMZCapabilities.sync(player);

        return constitution;
    }

    public int addStam(int points) {

        if (stamina <= DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get()) {
            stamina += points;
        }
        DMZCapabilities.sync(player);

        return stamina;
    }

    public int addKipwr(int points) {

        if (KiPower <= DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get()) {
            KiPower += points;
        }
        DMZCapabilities.sync(player);

        return KiPower;
    }

    public int addEnergy(int points) {

        if (energy <= DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get()) {
            energy += points;
        }

        DMZCapabilities.sync(player);

        return energy;
    }

    public int addZpoints(int points) {

        zpoints += points;
        DMZCapabilities.sync(player);

        return zpoints;
    }

    public int removeZpoints(int points) {

        zpoints -= points;
        DMZCapabilities.sync(player);

        return zpoints;
    }

    public int removeStrenght(int points) {

        if (this.strength > 3) {
            this.strength -= points;
        } else {
            this.strength = 3;
        }

        DMZCapabilities.sync(player);
        return strength;
    }

    public int removeDefense(int points) {

        if (this.defense > 3) {
            this.defense -= points;
        } else {
            this.defense = 3;
        }
        DMZCapabilities.sync(player);
        return defense;
    }

    public int removeConstitution(int points) {

        if (this.constitution > 5) {
            this.constitution -= points;
        } else {
            this.constitution = 5;
        }

        DMZCapabilities.sync(player);
        return constitution;
    }

    public int removeKiPower(int points) {

        if (this.KiPower > 5) {
            this.KiPower -= points;
        } else {
            this.KiPower = 5;
        }
        DMZCapabilities.sync(player);

        return KiPower;
    }

    public int removeEnergy(int points) {

        if (this.energy > 10) {
            this.energy -= points;
        } else {
            this.energy = 10;
        }
        DMZCapabilities.sync(player);

        return energy;
    }

    public int removeStamina(int points) {

        if (this.stamina > 10) {
            this.stamina -= points;
        } else {
            this.stamina = 10;
        }
        DMZCapabilities.sync(player);

        return stamina;
    }

    public int getRace() {
        return races;
    }

    public void setRace(int races) {
        this.races = races;
        if (races > 6) {
            this.races = 6;
        }
        if(this.races < 0){
            this.races = 0;
        }
        DMZCapabilities.sync(player);

    }

    public int getHairID() {
        return hairID;
    }

    public void setHairID(int hairID) {
        this.hairID = hairID;
        DMZCapabilities.sync(player);

    }

    public int getBodytype() {
        return bodytype;
    }

    public void setBodytype(int bodytype) {
        this.bodytype = bodytype;
        DMZCapabilities.sync(player);
    }

    public int getEyesType() {
        return eyesType;
    }

    public void setEyesType(int eyesType) {
        this.eyesType = eyesType;
        DMZCapabilities.sync(player);

    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {

        this.strength = strength;

        if (this.strength >= DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get()) {
            this.strength = DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get();
        } else {
            this.strength = strength;
        }

        DMZCapabilities.sync(player);

    }

    public int getDefense() {

        return defense;
    }

    public void setDefense(int defense) {

        this.defense = defense;

        if (this.defense >= DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get()) {
            this.defense = DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get();
        } else {
            this.defense = defense;
        }

        DMZCapabilities.sync(player);
    }

    public int getConstitution() {
        return constitution;
    }

    public void setConstitution(int constitution) {

        this.constitution = constitution;

        if (this.constitution >= DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get()) {
            this.constitution = DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get();
        } else {
            this.constitution = constitution;
        }

        DMZCapabilities.sync(player);

    }

    public int getKiPower() {
        return KiPower;
    }

    public void setKiPower(int kiPower) {

        this.KiPower = kiPower;

        if (this.KiPower >= DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get()) {
            KiPower = DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get();
        } else {
            this.KiPower = kiPower;
        }

        DMZCapabilities.sync(player);

    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {

        this.energy = energy;

        if (this.energy >= DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get()) {
            this.energy = DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get();
        } else {
            this.energy = energy;
        }

        DMZCapabilities.sync(player);

    }

    public int getCurrentEnergy() {
        return currentEnergy;
    }

    public void setCurrentEnergy(int currentEnergy) {
        this.currentEnergy = currentEnergy;
        DMZCapabilities.sync(player);

    }

    public void removeCurEnergy(int currentEnergy) {
        this.currentEnergy -= currentEnergy;

        if (this.currentEnergy < 0) {
            this.currentEnergy = 0;
        }

        DMZCapabilities.sync(player);

    }

    public int addCurEnergy(int currentEnergy) {

        if (this.currentEnergy < ((int) (energy * 0.5) * DMCAttrConfig.MULTIPLIER_ENERGY.get())) {
            this.currentEnergy += currentEnergy;
        } else {
            this.currentEnergy += 0;
        }

        DMZCapabilities.sync(player);
        return this.currentEnergy;
    }

    public int getCurStam() {

        return curStam;
    }

    public void setCurStam(int curStam) {
        this.curStam = curStam;
        DMZCapabilities.sync(player);
    }

    public int removeCurStam(int curStam) {
        this.curStam -= curStam;

        if (this.curStam < 0) {
            this.curStam = 0;
        }
        DMZCapabilities.sync(player);

        return this.curStam;
    }

    public int addCurStam(int curStam) {

        if (this.curStam < (stamina + 3)) {
            this.curStam += curStam;
        } else {
            this.curStam += 0;
        }

        DMZCapabilities.sync(player);
        return this.curStam;
    }

    public int getStamina() {
        return stamina;
    }

    public void setStamina(int stamina) {
        if (this.stamina >= DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get()) {
            this.stamina = DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get();
        } else {
            this.stamina = stamina;
        }
        DMZCapabilities.sync(player);
    }

    public boolean isAcceptCharacter() {
        return AcceptCharacter;
    }

    public void setAcceptCharacter(boolean acceptCharacter) {
        AcceptCharacter = acceptCharacter;
        DMZCapabilities.sync(player);
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
        DMZCapabilities.sync(player);
    }

    public CompoundTag saveNBTData() {

        CompoundTag nbt = new CompoundTag();

        nbt.putInt("race", races);

        nbt.putInt("hairID", hairID);
        nbt.putInt("bodyType", bodytype);
        nbt.putInt("eyesType", eyesType);

        nbt.putInt("strength", strength);
        nbt.putInt("defense", defense);
        nbt.putInt("constitution", constitution);
        nbt.putInt("kiPower", KiPower);
        nbt.putInt("energy", energy);
        nbt.putInt("stamina", stamina);

        nbt.putInt("currentEnergy", currentEnergy);
        nbt.putInt("currentBody", curBody);
        nbt.putInt("currentStamina", curStam);

        nbt.putInt("bodyColor", bodyColor);
        nbt.putInt("bodyColor2", bodyColor2);
        nbt.putInt("bodyColor3", bodyColor3);
        nbt.putInt("hairColor", hairColor);
        nbt.putInt("eye1Color", eye1Color);
        nbt.putInt("eye2Color", eye2Color);
        nbt.putInt("auraColor", auraColor);

        nbt.putString("gender", gender);

        nbt.putInt("zpoints", zpoints);
        nbt.putBoolean("acceptCharacter", AcceptCharacter);
        return nbt;
    }

    public void loadNBTData(CompoundTag nbt) {

        races = nbt.getInt("race");

        hairID = nbt.getInt("hairID");
        bodytype = nbt.getInt("bodyType");
        eyesType = nbt.getInt("eyesType");

        strength = nbt.getInt("strength");
        defense = nbt.getInt("defense");
        constitution = nbt.getInt("constitution");
        KiPower = nbt.getInt("kiPower");
        energy = nbt.getInt("energy");
        stamina = nbt.getInt("stamina");

        zpoints = nbt.getInt("zpoints");
        currentEnergy = nbt.getInt("currentEnergy");
        curBody = nbt.getInt("currentBody");
        curStam = nbt.getInt("currentStamina");

        bodyColor = nbt.getInt("bodyColor");
        bodyColor2 = nbt.getInt("bodyColor2");
        bodyColor3 = nbt.getInt("bodyColor3");
        hairColor = nbt.getInt("hairColor");
        eye1Color = nbt.getInt("eye1Color");
        eye2Color = nbt.getInt("eye2Color");
        auraColor = nbt.getInt("auraColor");

        gender = nbt.getString("gender");

        AcceptCharacter = nbt.getBoolean("acceptCharacter");

    }


}