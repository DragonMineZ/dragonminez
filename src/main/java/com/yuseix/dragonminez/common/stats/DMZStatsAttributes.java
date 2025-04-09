package com.yuseix.dragonminez.common.stats;

import com.yuseix.dragonminez.common.config.DMZGeneralConfig;
import com.yuseix.dragonminez.common.stats.forms.FormsData;
import com.yuseix.dragonminez.common.stats.skills.DMZSkill;
import com.yuseix.dragonminez.common.util.DMZDatos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DMZStatsAttributes {

    private Map<String, DMZSkill> DMZSkills = new HashMap<>();
    private Map<String, FormsData> FormsData = new HashMap<>();

    private Map<String, Boolean> DMZPermanentEffects = new HashMap<>();
    private Map<String, Integer> DMZTemporalEffects = new HashMap<>();

    private DMZDatos dmzdatos = new DMZDatos();

    private int races, hairID, bodytype, eyesType;
    private int strength = 5, defense = 5, constitution = 5, KiPower = 5, energy = 5, dmzRelease = 5, FormRelease;
    private int curStam, currentEnergy, dmzAlignment = 100, zpoints;

    private int dmzSenzuDaily = 0, saiyanZenkaiTimer = 0, zenkaiCount = 0, babaCooldown = 0, babaAliveTimer = 0;

    private int bodyColor, bodyColor2, bodyColor3, eye1Color, eye2Color, hairColor = 921617, auraColor = 8388607;

    private String gender = "male", dmzClass = "warrior", dmzKiWeapon = "sword";
    private String dmzForm = "base", dmzGroupForm = "";

    private boolean AcceptCharacter = false, isAuraOn = false, isTurboOn = false, isDmzAlive = true, tailMode = false;
    private boolean isTransforming = false, isDescendkeyon = false, compactMenu = false, isKaioPlanet = false, isPorungaRevive = false, isShenronRevive = false;

    private final Player player;

    public DMZStatsAttributes(Player player) {
        this.player = player;
    }

    public int getStat(String stat) {
        int value = 0;
        switch (stat.toUpperCase(Locale.ROOT)) {
            case "STR" -> value = strength;
            case "DEF" -> value = defense;
            case "CON" -> value = constitution;
            case "PWR" -> value = KiPower;
            case "ENE" -> value = energy;
        }
        return value;
    }

    public void setStat(String stat, int value) {
        switch (stat.toUpperCase(Locale.ROOT)) {
            case "STR" -> {
                this.strength = value;
                if (this.strength >= DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()) this.strength = DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get();
                else this.strength = value;
            }
            case "DEF" -> {
                this.defense = value;
                if (this.defense >= DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()) this.defense = DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get();
                else this.defense = value;
            }
            case "CON" -> {
                this.constitution = value;
                if (this.constitution >= DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()) this.constitution = DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get();
                else this.constitution = value;
            }
            case "PWR" -> {
                this.KiPower = value;
                if (this.KiPower >= DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()) this.KiPower = DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get();
                else this.KiPower = value;
            }
            case "ENE" -> {
                this.energy = value;
                if (this.energy >= DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()) this.energy = DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get();
                else this.energy = value;
            }
        }
        DMZStatsCapabilities.syncStats(player);
    }

    public void addStat(String stat, int points) {
        switch (stat.toUpperCase(Locale.ROOT)) {
            case "STR" -> {
                if (strength <= DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()) strength += points;
                if (this.strength >= DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()) this.strength = DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get();
            }
            case "DEF" -> {
                if (defense <= DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()) defense += points;
                if (this.defense >= DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()) this.defense = DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get();
            }
            case "CON" -> {
                if (constitution <= DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()) constitution += points;
                if (this.constitution >= DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()) this.constitution = DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get();
            }
            case "PWR" -> {
                if (KiPower <= DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()) KiPower += points;
                if (this.KiPower >= DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()) this.KiPower = DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get();
            }
            case "ENE" -> {
                if (energy <= DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()) energy += points;
                if (this.energy >= DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()) this.energy = DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get();
            }
        }
        DMZStatsCapabilities.syncStats(player);
    }

    public void removeStat(String stat, int points) {
        switch (stat.toUpperCase(Locale.ROOT)) {
            case "STR" -> {
                strength -= points;
                if (strength < 5) strength = 5;
            }
            case "DEF" -> {
                defense -= points;
                if (defense < 5) defense = 5;
            }
            case "CON" -> {
                constitution -= points;
                if (constitution < 5) constitution = 5;
            }
            case "PWR" -> {
                KiPower -= points;
                if (KiPower < 5) KiPower = 5;
            }
            case "ENE" -> {
                energy -= points;
                if (energy < 5) energy = 5;
            }
        }
        DMZStatsCapabilities.syncStats(player);
    }

    public boolean getBoolean(String value) {
        boolean result = false;
        switch (value.toLowerCase()) {
            case "aura" -> result = isAuraOn;
            case "turbo" -> result = isTurboOn;
            case "descend" -> result = isDescendkeyon;
            case "transform" -> result = isTransforming;
            case "alive" -> result = isDmzAlive;
            case "tailmode" -> result = tailMode;
            case "kaioplanet" -> result = isKaioPlanet;
            case "porungarevive" -> result = isPorungaRevive;
            case "shenronrevive" -> result = isShenronRevive;
            case "compactmenu" -> result = compactMenu;
            case "dmzuser"  -> result = AcceptCharacter;
            default -> System.out.println("The BOOLEAN value " + value + "could not be found");
        }
        return result;
    }

    public void setBoolean(String value, boolean state) {
        switch (value.toLowerCase()) {
            case "aura" -> isAuraOn = state;
            case "turbo" -> isTurboOn = state;
            case "descend" -> isDescendkeyon = state;
            case "transform" -> isTransforming = state;
            case "alive" -> isDmzAlive = state;
            case "tailmode" -> tailMode = state;
            case "kaioplanet" -> isKaioPlanet = state;
            case "porungarevive" -> isPorungaRevive = state;
            case "shenronrevive" -> isShenronRevive = state;
            case "compactmenu" -> compactMenu = state;
            case "dmzuser" -> AcceptCharacter = state;
            default -> System.out.println("The BOOLEAN value " + value + "could not be found");
        }
        DMZStatsCapabilities.syncStats(player);
    }

    public int getIntValue(String value) {
        int result = 0;
        switch (value.toLowerCase(Locale.ROOT)) {
            case "level" -> result = (getStat("STR") + getStat("DEF") + getStat("CON") + getStat("PWR") + getStat("ENE")) / 5;
            case "race" -> result = races;
            case "formrelease" -> result = FormRelease;
            case "senzutimer" -> result = dmzSenzuDaily;
            case "release" -> result = dmzRelease;
            case "alignment" -> result = dmzAlignment;
            case "tps" -> result = zpoints;
            case "bodycolor" -> result = bodyColor;
            case "bodycolor2" -> result = bodyColor2;
            case "bodycolor3" -> result = bodyColor3;
            case "eye1color" -> result = eye1Color;
            case "eye2color" -> result = eye2Color;
            case "haircolor" -> result = hairColor;
            case "hairid" -> result = hairID;
            case "bodytype" -> result = bodytype;
            case "eyestype" -> result = eyesType;
            case "auracolor" -> result = auraColor;
            case "zenkaicount" -> result = zenkaiCount;
            case "zenkaitimer" -> result = saiyanZenkaiTimer;
            case "babacooldown" -> result = babaCooldown;
            case "babaalivetimer" -> result = babaAliveTimer;
            case "maxhealth" -> result = dmzdatos.calcConstitution(this);
            case "maxenergy" -> result = dmzdatos.calcEnergy(this);
            case "maxstam" -> result = dmzdatos.calcStamina(this);
            case "curstam" -> result = curStam;
            case "curenergy" -> result = currentEnergy;
            default -> System.out.println("The INT value " + value + "could not be found");
        }
        return result;
    }

    public void setIntValue(String value, int points) {
        switch (value.toLowerCase(Locale.ROOT)) {
            case "race" -> races = Math.min(points, 6);
            case "formrelease" -> FormRelease = Math.max(0, Math.min(100, points));
            case "senzutimer" -> dmzSenzuDaily = points;
            case "release" -> dmzRelease = points;
            case "alignment" -> dmzAlignment = points;
            case "tps" -> zpoints = points;
            case "bodycolor" -> bodyColor = points;
            case "bodycolor2" -> bodyColor2 = points;
            case "bodycolor3" -> bodyColor3 = points;
            case "eye1color" -> eye1Color = points;
            case "eye2color" -> eye2Color = points;
            case "haircolor" -> hairColor = points;
            case "hairid" -> hairID = points;
            case "bodytype" -> bodytype = points;
            case "eyestype" -> eyesType = points;
            case "auracolor" -> auraColor = points;
            case "zenkaicount" -> zenkaiCount = points;
            case "zenkaitimer" -> saiyanZenkaiTimer = points;
            case "babacooldown" -> babaCooldown = points;
            case "babaalivetimer" -> babaAliveTimer = points;
            case "curstam" -> {
                if (curStam >= dmzdatos.calcStamina(this)) this.curStam = dmzdatos.calcStamina(this);
                else this.curStam = points;
            }
            case "curenergy" -> {
                if (currentEnergy >= dmzdatos.calcEnergy(this)) this.currentEnergy = dmzdatos.calcEnergy(this);
                else this.currentEnergy = points;
            }
            default -> System.out.println("The INT value " + value + "could not be found");
        }
        DMZStatsCapabilities.syncStats(player);
    }

    public void addIntValue(String value, int points) {
        switch (value.toLowerCase(Locale.ROOT)) {
            case "alignment" -> dmzAlignment = Math.min(dmzAlignment + points, 100);
            case "tps" -> zpoints += points;
            case "curstam" -> {
                this.curStam += points;
                if (this.curStam >= dmzdatos.calcStamina(this)) this.curStam = dmzdatos.calcStamina(this);
            }
            case "curenergy" -> {
                this.currentEnergy += points;
                if (this.currentEnergy >= dmzdatos.calcEnergy(this)) this.currentEnergy = dmzdatos.calcEnergy(this);
            }
            default -> System.out.println("The INT value " + value + "could not be found");
        }
        DMZStatsCapabilities.syncStats(player);
    }

    public void removeIntValue(String value, int points) {
        switch (value.toLowerCase(Locale.ROOT)) {
            case "alignment" -> dmzAlignment = Math.max(dmzAlignment - points, 0);
            case "tps" -> {
                zpoints -= points;
                if (zpoints < 0) zpoints = 0;
            }
            case "curstam" -> {
                this.curStam -= points;
                if (this.curStam < 0) this.curStam = 0;
            }
            case "curenergy" -> {
                this.currentEnergy -= points;
                if (this.currentEnergy < 0) this.currentEnergy = 0;
            }
            default -> System.out.println("The INT value " + value + "could not be found");
        }
        DMZStatsCapabilities.syncStats(player);
    }

    public String getStringValue(String value) {
        String result = "";
        switch (value.toLowerCase(Locale.ROOT)) {
            case "gender" -> result = gender;
            case "groupform" -> result = dmzGroupForm;
            case "form" -> result = dmzForm;
            case "kiweapon" -> result = dmzKiWeapon;
            case "class" -> result = dmzClass;
            default -> System.out.println("The STRING value " + value + "could not be found");
        }
        return result;
    }

    public void setStringValue(String value, String newValue) {
        switch (value.toLowerCase(Locale.ROOT)) {
            case "gender" -> gender = newValue;
            case "groupform" -> dmzGroupForm = newValue;
            case "form" -> dmzForm = newValue;
            case "kiweapon" -> dmzKiWeapon = newValue;
            case "class" -> dmzClass = newValue;
            default -> System.out.println("The STRING value " + value + "could not be found");
        }
        DMZStatsCapabilities.syncStats(player);
    }


    public void addSkill(String name, DMZSkill skill) {
        DMZSkills.put(name, skill);
        DMZStatsCapabilities.syncStats(player);
        DMZStatsCapabilities.syncSkills(player);
    }

    public void addFormSkill(String name, FormsData skill) {
        FormsData.put(name, skill);
        DMZStatsCapabilities.syncStats(player);
        DMZStatsCapabilities.syncFormsSkills(player);
    }

    public DMZSkill getSkill(String name) {
        if (DMZSkills.containsKey(name)) return DMZSkills.get(name);
        return null;
    }

    public FormsData getFormSkill(String name) {
        if (FormsData.containsKey(name)) return FormsData.get(name);
        return null;
    }

    public void setDMZSkills(Map<String, DMZSkill> DMZSkills) {
        this.DMZSkills = DMZSkills;
        DMZStatsCapabilities.syncStats(player);
        DMZStatsCapabilities.syncSkills(player);
    }

    public void setDMZFormSkill(Map<String, FormsData> DMZFormSkill) {
        this.FormsData = DMZFormSkill;
        DMZStatsCapabilities.syncStats(player);
        DMZStatsCapabilities.syncFormsSkills(player);
    }

    public boolean hasSkill(String name) {
        return DMZSkills.containsKey(name);
    }

    public boolean hasFormSkill(String name) {
        return FormsData.containsKey(name);
    }

    public Map<String, DMZSkill> getDMZSkills() {
        return DMZSkills;
    }

    public Map<String, FormsData> getAllDMZForms() {
        return FormsData;
    }

    public void removeSkill(String name) {
        DMZSkill skill = DMZSkills.get(name);
        if(skill != null) DMZSkills.remove(name);
        DMZStatsCapabilities.syncStats(player);
        DMZStatsCapabilities.syncSkills(player);
    }

    public void removeAllSkills() {
        DMZSkills.clear();
        DMZStatsCapabilities.syncStats(player);
        DMZStatsCapabilities.syncSkills(player);
    }

    public void removeFormSkill(String name) {
        FormsData formskill = FormsData.get(name);
        if(formskill != null) FormsData.remove(name);
        DMZStatsCapabilities.syncStats(player);
        DMZStatsCapabilities.syncFormsSkills(player);
    }

    public int getSkillLevel(String name) {
        DMZSkill skill = DMZSkills.get(name);
        return skill != null ? skill.getLevel() : -1;  // Devuelve -1 si no existe la habilidad
    }

    public int getFormSkillLevel(String name) {
        FormsData skill = FormsData.get(name);
        return skill != null ? skill.getLevel() : -1;  // Devuelve -1 si no existe la habilidad
    }

    public boolean isActiveSkill(String name) {
        DMZSkill skill = DMZSkills.get(name);
        if (skill == null) return false;
        return skill.isActive();
    }

    public void setSkillActive(String name, boolean isActive){
        DMZSkill skill = DMZSkills.get(name);
        if(skill != null) skill.setActive(isActive);
        DMZStatsCapabilities.syncStats(player);
        DMZStatsCapabilities.syncSkills(player);

    }
    public void setSkillLvl(String name, int cantidad){
        DMZSkill skill = DMZSkills.get(name);
        if(skill != null) skill.setLevel(cantidad);
        DMZStatsCapabilities.syncStats(player);
        DMZStatsCapabilities.syncSkills(player);

    }
    public void setFormSkillLvl(String skillform, int cantidad){
        FormsData skill = FormsData.get(skillform);
        if(skill != null) skill.setLevel(cantidad);
        DMZStatsCapabilities.syncStats(player);
        DMZStatsCapabilities.syncFormsSkills(player);

    }

    // Métodos para gestionar los estados permanentes wa
    public void addDMZPermanentEffect(String permanentEffect, boolean isActive) {
        DMZPermanentEffects.put(permanentEffect, isActive);
        DMZStatsCapabilities.syncStats(player);
        DMZStatsCapabilities.syncPermanentEffects(player);
    }

    public Boolean getDMZPermaEffect(String permanentEffect) {
        return DMZPermanentEffects.getOrDefault(permanentEffect, false);
    }

    public Map<String, Boolean> getDMZPermanentEffects() {
        return DMZPermanentEffects;
    }

    public boolean hasDMZPermaEffect(String permanentEffect) {
        return DMZPermanentEffects.containsKey(permanentEffect);
    }

    public void setDMZPermanentEffect(String permanentEffect, boolean isActive) {
        if (DMZPermanentEffects.containsKey(permanentEffect)) {
            DMZPermanentEffects.put(permanentEffect, isActive);
            DMZStatsCapabilities.syncStats(player);
            DMZStatsCapabilities.syncPermanentEffects(player);
        }
    }

    public void removePermanentEffect(String permanentEffect) {
        if (DMZPermanentEffects.containsKey(permanentEffect)) {
            DMZPermanentEffects.remove(permanentEffect);
            DMZStatsCapabilities.syncStats(player);
            DMZStatsCapabilities.syncPermanentEffects(player);
        }
    }

    // Métodos para gestionar los estados temporales wa
    public Map<String, Integer> getDMZTemporalEffects() {
        return DMZTemporalEffects;
    }

    public void addDMZTemporalEffect(String temporalEffect, int seconds) {
        DMZTemporalEffects.put(temporalEffect, seconds);
        DMZStatsCapabilities.syncStats(player);
        DMZStatsCapabilities.syncTempEffects(player);
    }


    public Integer getDMZTemporalEffect(String temporaleffect) {
        return DMZTemporalEffects.getOrDefault(temporaleffect, 0);
    }

    public boolean hasDMZTemporalEffect(String temporaleffect) {
        return DMZTemporalEffects.containsKey(temporaleffect);
    }

    public void setDMZTemporalEffect(String permanentEffect, int seconds) {
        if (DMZTemporalEffects.containsKey(permanentEffect)) {
            DMZTemporalEffects.put(permanentEffect, seconds);
            DMZStatsCapabilities.syncStats(player);
            DMZStatsCapabilities.syncTempEffects(player);
        }
    }
    public void removeTemporalEffect(String temporalEffect) {
        if (DMZTemporalEffects.containsKey(temporalEffect)) {
            DMZTemporalEffects.remove(temporalEffect);
            DMZStatsCapabilities.syncStats(player);
            DMZStatsCapabilities.syncTempEffects(player);
        }
    }

    public int getDmzBattlePower() {
        int damage = dmzdatos.calcMultipliedStrength(this);
        int kiDamage = dmzdatos.calcMultipliedKiPower(this);
        int totalDefense = dmzdatos.calcMultipliedDefense(this);
        double release = (double) getIntValue("release") / 100;
        return (int) ((damage + kiDamage + totalDefense + getIntValue("maxhealth")) * release);
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

        nbt.putInt("currentEnergy", currentEnergy);
        nbt.putInt("currentStamina", curStam);
        nbt.putInt("dmzRelease", dmzRelease);

        nbt.putInt("bodyColor", bodyColor);
        nbt.putInt("bodyColor2", bodyColor2);
        nbt.putInt("bodyColor3", bodyColor3);
        nbt.putInt("hairColor", hairColor);
        nbt.putInt("eye1Color", eye1Color);
        nbt.putInt("eye2Color", eye2Color);
        nbt.putInt("auraColor", auraColor);
        nbt.putInt("dmzAlignment",dmzAlignment);
        nbt.putInt("formRelease", FormRelease);

        nbt.putString("gender", gender);
        nbt.putString("dmzClass", dmzClass);
        nbt.putString("dmzskiweapon", dmzKiWeapon);
        nbt.putString("dmzForm", dmzForm);
        nbt.putString("dmzGroupForm", dmzGroupForm);

        nbt.putInt("zpoints", zpoints);
        nbt.putInt("dmzSenzuDaily", dmzSenzuDaily);
        nbt.putBoolean("acceptCharacter", AcceptCharacter);
        nbt.putBoolean("compactMenu", compactMenu);
        nbt.putInt("zenkaiCount", zenkaiCount);
        nbt.putInt("zenkaiTimer", saiyanZenkaiTimer);
        nbt.putBoolean("isAuraOn", isAuraOn);
        nbt.putBoolean("isTurboOn", isTurboOn);
        nbt.putBoolean("isDescendKey", isDescendkeyon);
        nbt.putBoolean("isTransfKey", isTransforming);
        nbt.putBoolean("isDmzAlive", isDmzAlive);
        nbt.putBoolean("tailmode", tailMode);
        nbt.putBoolean("isKaioPlanet", isKaioPlanet);
        nbt.putBoolean("isPorungaRevive", isPorungaRevive);
        nbt.putBoolean("isShenronRevive", isShenronRevive);
        nbt.putInt("babaCooldown", babaCooldown);
        nbt.putInt("babaAliveTimer", babaAliveTimer);

        CompoundTag permanentEffectsTag = new CompoundTag();
        for (Map.Entry<String, Boolean> entry : DMZPermanentEffects.entrySet()) {
            permanentEffectsTag.putBoolean(entry.getKey(), entry.getValue());
        }
        nbt.put("DMZPermanentEffects", permanentEffectsTag);

        CompoundTag temporalEffectTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : DMZTemporalEffects.entrySet()) {
            temporalEffectTag.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("DMZTemporalEffects", temporalEffectTag);

        // Crear un CompoundTag para guardar cada habilidad
        CompoundTag skillsTag = new CompoundTag();

        for (Map.Entry<String, DMZSkill> entry : DMZSkills.entrySet()) {
            String skillName = entry.getKey();
            DMZSkill skill = entry.getValue();

            // Crear un CompoundTag para la habilidad y guardarlo en el map de skills
            CompoundTag skillTag = new CompoundTag();

            // Aquí guardas los datos relevantes de la habilidad, como el nivel y la descripción
            skillTag.putString("name", skill.getName());
            skillTag.putInt("level", skill.getLevel());
            skillTag.putString("description", skill.getDesc());
            skillTag.putBoolean("active", skill.isActive());

            // Guarda la habilidad en el CompoundTag de skills
            skillsTag.put(skillName, skillTag);
        }

        nbt.put("DMZSkills", skillsTag);

        // Crear un CompoundTag para guardar cada habilidad
        CompoundTag formsTag = new CompoundTag();

        for (Map.Entry<String, FormsData> entry : FormsData.entrySet()) {
            String skillName = entry.getKey();
            FormsData forms = entry.getValue();

            // Crear un CompoundTag para la habilidad y guardarlo en el map de skills
            CompoundTag formTag = new CompoundTag();

            // Aquí guardas los datos relevantes de la habilidad, como el nivel y la descripción
            formTag.putString("name", forms.getName());
            formTag.putInt("level", forms.getLevel());

            // Guarda la habilidad en el CompoundTag de skills
            formsTag.put(skillName, formTag);
        }

        nbt.put("DMZFormSkill", formsTag);
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

        zpoints = nbt.getInt("zpoints");
        dmzSenzuDaily = nbt.getInt("dmzSenzuDaily");

        currentEnergy = nbt.getInt("currentEnergy");
        curStam = nbt.getInt("currentStamina");
        dmzRelease = nbt.getInt("dmzRelease");
        FormRelease = nbt.getInt("formRelease");

        bodyColor = nbt.getInt("bodyColor");
        bodyColor2 = nbt.getInt("bodyColor2");
        bodyColor3 = nbt.getInt("bodyColor3");
        hairColor = nbt.getInt("hairColor");
        eye1Color = nbt.getInt("eye1Color");
        eye2Color = nbt.getInt("eye2Color");
        auraColor = nbt.getInt("auraColor");

        gender = nbt.getString("gender");
        dmzKiWeapon = nbt.getString("dmzskiweapon");
        dmzClass = nbt.getString("dmzClass");
        dmzAlignment = nbt.getInt("dmzAlignment");
        dmzForm = nbt.getString("dmzForm");
        dmzGroupForm = nbt.getString("dmzGroupForm");

        AcceptCharacter = nbt.getBoolean("acceptCharacter");
        compactMenu = nbt.getBoolean("compactMenu");
        zenkaiCount = nbt.getInt("zenkaiCount");
        saiyanZenkaiTimer = nbt.getInt("zenkaiTimer");
        isAuraOn = nbt.getBoolean("isAuraOn");
        isTurboOn = nbt.getBoolean("isTurboOn");
        isDescendkeyon = nbt.getBoolean("isDescendKey");
        isTransforming = nbt.getBoolean("isTransfKey");
        tailMode = nbt.getBoolean("tailmode");
        isKaioPlanet = nbt.getBoolean("isKaioPlanet");
        isDmzAlive = nbt.getBoolean("isDmzAlive");
        isPorungaRevive = nbt.getBoolean("isPorungaRevive");
        isShenronRevive = nbt.getBoolean("isShenronRevive");
        babaCooldown = nbt.getInt("babaCooldown");
        babaAliveTimer = nbt.getInt("babaAliveTimer");

        CompoundTag permanentEffects = nbt.getCompound("DMZPermanentEffects");
        for (String effectName : permanentEffects.getAllKeys()) {
            boolean isActive = permanentEffects.getBoolean(effectName);
            DMZPermanentEffects.put(effectName, isActive);
        }

        CompoundTag temporalEffectsTag = nbt.getCompound("DMZTemporalEffects");
        for (String effectName : temporalEffectsTag.getAllKeys()) {
            int seconds = temporalEffectsTag.getInt(effectName);
            DMZTemporalEffects.put(effectName, seconds);
        }

        if (nbt.contains("DMZSkills", 10)) {  // Verifica si "DMZSkills" existe
            //El 10 hace referencia a TAG_COMPOUND

            CompoundTag skillsTag = nbt.getCompound("DMZSkills");

            for (String skillName : skillsTag.getAllKeys()) {
                CompoundTag skillTag = skillsTag.getCompound(skillName);

                // Cargar el nivel y la descripción de la habilidad
                String name = skillTag.getString("name");
                int level = skillTag.getInt("level");
                String description = skillTag.getString("description");
                boolean active = skillTag.getBoolean("active");

                DMZSkill skill = new DMZSkill(name, description, level, active);
                DMZSkills.put(skillName, skill);
            }
        }

        if (nbt.contains("DMZFormSkill", 10)) {  // Verifica si "DMZSkills" existe
            //El 10 hace referencia a TAG_COMPOUND

            CompoundTag formsTag = nbt.getCompound("DMZFormSkill");

            for (String skillName : formsTag.getAllKeys()) {
                CompoundTag formTag = formsTag.getCompound(skillName);

                // Cargar el nivel y la descripción de la habilidad
                String name = formTag.getString("name");
                int level = formTag.getInt("level");

                FormsData form = new FormsData(name, level);
                FormsData.put(skillName, form);
            }
        }

    }
}