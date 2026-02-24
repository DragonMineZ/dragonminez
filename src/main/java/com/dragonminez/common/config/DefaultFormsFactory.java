package com.dragonminez.common.config;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.util.lists.*;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class DefaultFormsFactory {

	private final ConfigLoader loader;

	public DefaultFormsFactory(Gson gson, ConfigLoader loader) {
		this.loader = loader;
	}

	private void setDefaultMasteryValues(FormConfig.FormData form) {
		form.setMaxMastery(100.0);
		form.setMasteryPerHit(0.001);
		form.setMasteryPerDamageReceived(0.001);
		form.setStatMultPerMasteryPoint(0.01);
		form.setCostDecreasePerMasteryPoint(0.025);
		form.setPassiveMastery(0.001);
	}

	public void createDefaultFormsForRace(String raceName, Path formsPath, Map<String, FormConfig> forms) throws IOException {
		switch (raceName.toLowerCase()) {
			case "human" -> createDefaultHumanForms(formsPath, forms);
			case "saiyan" -> createSaiyanForms(formsPath, forms);
			case "namekian" -> createNamekianForms(formsPath, forms);
			case "frostdemon" -> createFrostDemonForms(formsPath, forms);
			case "majin" -> createMajinForms(formsPath, forms);
			case "bioandroid" -> createBioAndroidForms(formsPath, forms);
		}
	}

	public void createDefaultStackForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		createDefaultKaiokenForms(formsPath, forms);
//        createDefaultUltraInstinctForms(formsPath, forms);
//        createDefaultUltraEgoForms(formsPath, forms);
	}

	public void createDefaultKaiokenForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig kaiokenForms = new FormConfig();
		kaiokenForms.setGroupName(StackForms.GROUP_KAIOKEN);
		kaiokenForms.setFormType(StackForms.GROUP_KAIOKEN);

		FormConfig.FormData x2 = new FormConfig.FormData();
		x2.setName(StackForms.X2);
		x2.setUnlockOnSkillLevel(1);
		x2.setStrMultiplier(1.1);
		x2.setSkpMultiplier(1.1);
		x2.setDefMultiplier(1.1);
		x2.setPwrMultiplier(1.1);
		x2.setSpeedMultiplier(1.1);
		x2.setHealthDrain(0.03);
		x2.setAttackSpeed(1.1);
		x2.setAuraColor("#DB182C");
		x2.setHasLightnings(false);
		x2.setHairType("base");
		setDefaultMasteryValues(x2);
		x2.setStackDrainMultiplier(1.0);
		x2.setCanAlwaysTransform(true);

		FormConfig.FormData x3 = new FormConfig.FormData();
		x3.setName(StackForms.X3);
		x3.setUnlockOnSkillLevel(2);
		x3.setStrMultiplier(1.2);
		x3.setSkpMultiplier(1.2);
		x3.setDefMultiplier(1.2);
		x3.setPwrMultiplier(1.2);
		x3.setSpeedMultiplier(1.2);
		x3.setAttackSpeed(1.2);
		x3.setHealthDrain(0.06);
		x3.setAuraColor("#DB182C");
		x3.setHairType("base");
		setDefaultMasteryValues(x3);
		x3.setStackDrainMultiplier(1.0);

		FormConfig.FormData x4 = new FormConfig.FormData();
		x4.setName(StackForms.X4);
		x4.setUnlockOnSkillLevel(3);
		x4.setStrMultiplier(1.35);
		x4.setSkpMultiplier(1.35);
		x4.setDefMultiplier(1.35);
		x4.setPwrMultiplier(1.35);
		x4.setSpeedMultiplier(1.35);
		x4.setAttackSpeed(1.35);
		x4.setHealthDrain(0.095);
		x4.setAuraColor("#DB182C");
		x4.setHairType("base");
		setDefaultMasteryValues(x4);
		x4.setStackDrainMultiplier(1.0);

		FormConfig.FormData x10 = new FormConfig.FormData();
		x10.setName(StackForms.X10);
		x10.setUnlockOnSkillLevel(4);
		x10.setStrMultiplier(1.5);
		x10.setSkpMultiplier(1.5);
		x10.setDefMultiplier(1.5);
		x10.setPwrMultiplier(1.5);
		x10.setSpeedMultiplier(1.5);
		x10.setHealthDrain(0.11);
		x10.setAttackSpeed(1.5);
		x10.setAuraColor("#DB182C");
		x10.setHairType("base");
		setDefaultMasteryValues(x10);
		x10.setStackDrainMultiplier(1.0);

		FormConfig.FormData x20 = new FormConfig.FormData();
		x20.setName(StackForms.X20);
		x20.setUnlockOnSkillLevel(5);
		x20.setStrMultiplier(1.65);
		x20.setSkpMultiplier(1.65);
		x20.setDefMultiplier(1.65);
		x20.setPwrMultiplier(1.65);
		x20.setSpeedMultiplier(1.65);
		x20.setHealthDrain(0.15);
		x20.setAttackSpeed(1.65);
		x20.setAuraColor("#DB182C");
		x20.setHairType("base");
		setDefaultMasteryValues(x20);
		x20.setStackDrainMultiplier(1.0);

		FormConfig.FormData x100 = new FormConfig.FormData();
		x100.setName(StackForms.X100);
		x100.setUnlockOnSkillLevel(6);
		x100.setStrMultiplier(2.0);
		x100.setSkpMultiplier(2.0);
		x100.setDefMultiplier(2.0);
		x100.setPwrMultiplier(2.0);
		x100.setSpeedMultiplier(2.0);
		x100.setHealthDrain(0.20);
		x100.setAttackSpeed(2.0);
		x100.setAuraColor("#DB182C");
		x100.setHairType("base");
		setDefaultMasteryValues(x100);
		x100.setStackDrainMultiplier(1.0);

		Map<String, FormConfig.FormData> stackFormData = new LinkedHashMap<>();
		stackFormData.put(StackForms.X2, x2);
		stackFormData.put(StackForms.X3, x3);
		stackFormData.put(StackForms.X4, x4);
		stackFormData.put(StackForms.X10, x10);
		stackFormData.put(StackForms.X20, x20);
//        stackFormData.put(StackForms.X100, x100);
		kaiokenForms.setForms(stackFormData);

		forms.put(StackForms.GROUP_KAIOKEN, kaiokenForms);

		Path kaiokenPath = formsPath.resolve(StackForms.GROUP_KAIOKEN + ".json");
		loader.saveConfig(kaiokenPath, kaiokenForms);
		LogUtil.info(Env.COMMON, "Default Kaioken forms created");
	}

	public void createDefaultUltraInstinctForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig ultraInstinctForms = new FormConfig();
		ultraInstinctForms.setGroupName(StackForms.GROUP_ULTRAINSTINCT);
		ultraInstinctForms.setFormType(StackForms.GROUP_ULTRAINSTINCT);

		FormConfig.FormData sign = new FormConfig.FormData();
		sign.setName(StackForms.ULTRAINSTINCT_SIGN);
		sign.setUnlockOnSkillLevel(1);
		sign.setStrMultiplier(1.5);
		sign.setSkpMultiplier(1.5);
		sign.setDefMultiplier(1.5);
		sign.setPwrMultiplier(1.5);
		sign.setStaminaDrain(0.03);
		sign.setAuraColor("#E0E0E0");
		sign.setHasLightnings(false);
		sign.setHairType("base");
		setDefaultMasteryValues(sign);
		sign.setStackDrainMultiplier(1.0);
		sign.setCanAlwaysTransform(true);

		FormConfig.FormData mastered = new FormConfig.FormData();
		mastered.setName(StackForms.ULTRAINSTINCT_MASTERED);
		mastered.setUnlockOnSkillLevel(2);
		mastered.setStrMultiplier(2.0);
		mastered.setSkpMultiplier(2.0);
		mastered.setDefMultiplier(2.0);
		mastered.setPwrMultiplier(2.0);
		mastered.setStaminaDrain(0.06);
		mastered.setAuraColor("#E0E0E0");
		mastered.setHairColor("#E0E0E0");
		mastered.setBodyColor2("#E0E0E0");
		mastered.setHairType("base");
		setDefaultMasteryValues(mastered);
		mastered.setStackDrainMultiplier(1.0);
		mastered.setDirectTransformation(true);

		Map<String, FormConfig.FormData> stackFormData = new LinkedHashMap<>();
		stackFormData.put(StackForms.ULTRAINSTINCT_SIGN, sign);
		stackFormData.put(StackForms.ULTRAINSTINCT_MASTERED, mastered);
		ultraInstinctForms.setForms(stackFormData);

		forms.put(StackForms.GROUP_ULTRAINSTINCT, ultraInstinctForms);

		Path ultraInstinctPath = formsPath.resolve(StackForms.GROUP_ULTRAINSTINCT + ".json");
		loader.saveConfig(ultraInstinctPath, ultraInstinctForms);
		LogUtil.info(Env.COMMON, "Default Ultra Instict forms created");
	}

	public void createDefaultUltraEgoForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig ultraEgoForms = new FormConfig();
		ultraEgoForms.setGroupName(StackForms.GROUP_ULTRAEGO);
		ultraEgoForms.setFormType(StackForms.GROUP_ULTRAEGO);

		FormConfig.FormData sign = new FormConfig.FormData();
		sign.setName(StackForms.ULTRAEGO_SIGN);
		sign.setUnlockOnSkillLevel(1);
		sign.setStrMultiplier(1.5);
		sign.setSkpMultiplier(1.5);
		sign.setDefMultiplier(1.5);
		sign.setPwrMultiplier(1.5);
		sign.setStaminaDrain(0.03);
		sign.setAuraColor("#66023C");
		sign.setHasLightnings(false);
		sign.setHairType("base");
		setDefaultMasteryValues(sign);
		sign.setStackDrainMultiplier(1.0);
		sign.setCanAlwaysTransform(true);

		FormConfig.FormData mastered = new FormConfig.FormData();
		mastered.setName(StackForms.ULTRAEGO_MASTERED);
		mastered.setUnlockOnSkillLevel(2);
		mastered.setStrMultiplier(2.0);
		mastered.setSkpMultiplier(2.0);
		mastered.setDefMultiplier(2.0);
		mastered.setPwrMultiplier(2.0);
		mastered.setStaminaDrain(0.06);
		mastered.setAuraColor("#66023C");
		mastered.setHairColor("#66023C");
		mastered.setBodyColor2("#66023C");
		mastered.setHairType("ssj2");
		setDefaultMasteryValues(mastered);
		mastered.setStackDrainMultiplier(1.0);
		mastered.setDirectTransformation(true);

		Map<String, FormConfig.FormData> stackFormData = new LinkedHashMap<>();
		stackFormData.put(StackForms.ULTRAEGO_SIGN, sign);
		stackFormData.put(StackForms.ULTRAEGO_MASTERED, mastered);
		ultraEgoForms.setForms(stackFormData);

		forms.put(StackForms.GROUP_ULTRAEGO, ultraEgoForms);

		Path ultraEgoPath = formsPath.resolve(StackForms.GROUP_ULTRAEGO + ".json");
		loader.saveConfig(ultraEgoPath, ultraEgoForms);
		LogUtil.info(Env.COMMON, "Default Ultra Ego forms created");
	}

	private void createDefaultHumanForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig humanForms = new FormConfig();
		humanForms.setGroupName(HumanForms.GROUP_SUPERFORMS);
		humanForms.setFormType("super");

		FormConfig.FormData buffed = new FormConfig.FormData();
		buffed.setName(HumanForms.BUFFED);
		buffed.setUnlockOnSkillLevel(1);
		buffed.setModelScaling(new float[]{1.2f, 1.1f, 1.2f});
		buffed.setStrMultiplier(1.5);
		buffed.setSkpMultiplier(1.65);
		buffed.setDefMultiplier(1.25);
		buffed.setPwrMultiplier(1.35);
		buffed.setEnergyDrain(0.08);
		buffed.setHairType("base");
		setDefaultMasteryValues(buffed);
		buffed.setStackDrainMultiplier(2.0);
		buffed.setCanAlwaysTransform(true);

		FormConfig.FormData fullPower = new FormConfig.FormData();
		fullPower.setName(HumanForms.FULLPOWER);
		fullPower.setUnlockOnSkillLevel(2);
		fullPower.setModelScaling(new float[]{1.0f, 1.0f, 1.0f});
		fullPower.setStrMultiplier(1.75);
		fullPower.setSkpMultiplier(2.0);
		fullPower.setDefMultiplier(1.65);
		fullPower.setPwrMultiplier(1.5);
		fullPower.setEnergyDrain(0.16);
		fullPower.setHairType("ssj");
		setDefaultMasteryValues(fullPower);
		fullPower.setStackDrainMultiplier(2.0);

		FormConfig.FormData overdrive = new FormConfig.FormData();
		overdrive.setName(HumanForms.OVERDRIVE);
		overdrive.setUnlockOnSkillLevel(3);
		overdrive.setModelScaling(new float[]{1.1f, 1.1f, 1.1f});
		overdrive.setStrMultiplier(3.0);
		overdrive.setSkpMultiplier(3.35);
		overdrive.setDefMultiplier(2.15);
		overdrive.setPwrMultiplier(2.65);
		overdrive.setEnergyDrain(0.34);
		overdrive.setAuraColor("#FFFD99");
		overdrive.setHasLightnings(true);
		overdrive.setLightningColor("#E6F2F5");
		overdrive.setHairType("ssj2");
		setDefaultMasteryValues(overdrive);
		overdrive.setStackDrainMultiplier(2.0);

		FormConfig.FormData solaris = new FormConfig.FormData();
		solaris.setName(HumanForms.SOLARIS);
		solaris.setUnlockOnSkillLevel(4);
		solaris.setModelScaling(new float[]{1.0f, 1.0f, 1.0f});
		solaris.setStrMultiplier(3.0);
		solaris.setSkpMultiplier(3.35);
		solaris.setDefMultiplier(2.5);
		solaris.setPwrMultiplier(2.65);
		solaris.setEnergyDrain(0.22);
		solaris.setHairType("ssj2");
		setDefaultMasteryValues(solaris);
		solaris.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> humanFormData = new LinkedHashMap<>();
		humanFormData.put(HumanForms.BUFFED, buffed);
		humanFormData.put(HumanForms.FULLPOWER, fullPower);
		humanFormData.put(HumanForms.OVERDRIVE, overdrive);
		//humanFormData.put(HumanForms.SOLARIS, solaris);
		humanForms.setForms(humanFormData);

		forms.put(HumanForms.GROUP_SUPERFORMS, humanForms);

		Path humanPath = formsPath.resolve(HumanForms.GROUP_SUPERFORMS + ".json");
		loader.saveConfig(humanPath, humanForms);
		LogUtil.info(Env.COMMON, "Default Human forms created");

		createAndroidForms(formsPath, forms);
	}

	private void createAndroidForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig androidForms = new FormConfig();
		androidForms.setGroupName(HumanForms.GROUP_ANDROIDFORMS);
		androidForms.setFormType("android");

		FormConfig.FormData androidBase = new FormConfig.FormData();
		androidBase.setName(HumanForms.ANDROID_BASE);
		androidBase.setUnlockOnSkillLevel(0);
		androidBase.setModelScaling(new float[]{1.0f, 1.0f, 1.0f});
		androidBase.setStrMultiplier(1.45);
		androidBase.setSkpMultiplier(1.35);
		androidBase.setDefMultiplier(1.25);
		androidBase.setPwrMultiplier(1.75);
		androidBase.setHairType("base");
		setDefaultMasteryValues(androidBase);
		androidBase.setStackDrainMultiplier(2.0);

		FormConfig.FormData superAndroid = new FormConfig.FormData();
		superAndroid.setName(HumanForms.SUPER_ANDROID);
		superAndroid.setUnlockOnSkillLevel(1);
		superAndroid.setCustomModel("");
		superAndroid.setModelScaling(new float[]{1.05f, 1.05f, 1.05f});
		superAndroid.setStrMultiplier(2.15);
		superAndroid.setSkpMultiplier(2.0);
		superAndroid.setDefMultiplier(1.65);
		superAndroid.setPwrMultiplier(2.65);
		superAndroid.setHairType("ssj");
		setDefaultMasteryValues(superAndroid);
		superAndroid.setStackDrainMultiplier(2.0);

		FormConfig.FormData fusedAndroid = new FormConfig.FormData();
		fusedAndroid.setName(HumanForms.FUSED_ANDROID);
		fusedAndroid.setUnlockOnSkillLevel(2);
		fusedAndroid.setModelScaling(new float[]{1.4f, 1.3f, 1.4f});
		fusedAndroid.setStrMultiplier(2.85);
		fusedAndroid.setSkpMultiplier(2.65);
		fusedAndroid.setDefMultiplier(2.15);
		fusedAndroid.setPwrMultiplier(3.5);
		fusedAndroid.setStaminaDrainMultiplier(2.5);
		fusedAndroid.setAttackSpeed(0.85);
		fusedAndroid.setHairColor("#E65332");
		fusedAndroid.setEye1Color("#FFFFFF");
		fusedAndroid.setEye2Color("#FFFFFF");
		fusedAndroid.setHasLightnings(true);
		fusedAndroid.setLightningColor("#E63232");
		fusedAndroid.setBodyColor1("#4D9AE8");
		fusedAndroid.setHairType("ssj2");
		fusedAndroid.setForcedHairCode("");
		setDefaultMasteryValues(fusedAndroid);
		fusedAndroid.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> androidFormData = new LinkedHashMap<>();
		androidFormData.put(HumanForms.ANDROID_BASE, androidBase);
		androidFormData.put(HumanForms.SUPER_ANDROID, superAndroid);
		androidFormData.put(HumanForms.FUSED_ANDROID, fusedAndroid);
		androidForms.setForms(androidFormData);

		forms.put(HumanForms.GROUP_ANDROIDFORMS, androidForms);

		Path androidPath = formsPath.resolve(HumanForms.GROUP_ANDROIDFORMS + ".json");
		loader.saveConfig(androidPath, androidForms);
		LogUtil.info(Env.COMMON, "Default Android forms created for Humans");
	}

	private void createSaiyanForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig oozaruForms = new FormConfig();
		oozaruForms.setGroupName(SaiyanForms.GROUP_OOZARU);
		oozaruForms.setFormType("super");

		FormConfig.FormData oozaru = new FormConfig.FormData();
		oozaru.setName(SaiyanForms.OOZARU);
		oozaru.setUnlockOnSkillLevel(0);
		oozaru.setCustomModel("oozaru");
		oozaru.setBodyColor2("#572117");
		oozaru.setModelScaling(new float[]{3.8f, 3.8f, 3.8f});
		oozaru.setStrMultiplier(1.2);
		oozaru.setSkpMultiplier(1.2);
		oozaru.setDefMultiplier(1.1);
		oozaru.setPwrMultiplier(1.2);
		oozaru.setSpeedMultiplier(0.8);
		oozaru.setEnergyDrain(0.05);
		oozaru.setStaminaDrainMultiplier(1.2);
		oozaru.setAttackSpeed(0.9);
		oozaru.setHairType("base");
		setDefaultMasteryValues(oozaru);
		oozaru.setStackDrainMultiplier(2.0);
		oozaru.setCanAlwaysTransform(true);

		FormConfig.FormData goldenOozaru = new FormConfig.FormData();
		goldenOozaru.setName(SaiyanForms.GOLDEN_OOZARU);
		goldenOozaru.setUnlockOnSkillLevel(7);
		goldenOozaru.setCustomModel("oozaru");
		goldenOozaru.setHairColor("#FFD700");
		goldenOozaru.setAuraColor("#FFD700");
		goldenOozaru.setBodyColor2("#FFD700");
		goldenOozaru.setModelScaling(new float[]{3.8f, 3.8f, 3.8f});
		goldenOozaru.setStrMultiplier(3.0);
		goldenOozaru.setSkpMultiplier(3.0);
		goldenOozaru.setDefMultiplier(1.9);
		goldenOozaru.setPwrMultiplier(3.0);
		goldenOozaru.setSpeedMultiplier(0.85);
		goldenOozaru.setEnergyDrain(0.24);
		goldenOozaru.setStaminaDrainMultiplier(1.3);
		goldenOozaru.setHairType("base");
		setDefaultMasteryValues(goldenOozaru);
		goldenOozaru.setStackDrainMultiplier(2.0);

		FormConfig.FormData ssj4 = new FormConfig.FormData();
		ssj4.setName(SaiyanForms.SUPER_SAIYAN_4);
		ssj4.setUnlockOnSkillLevel(7);
		ssj4.setHairColor("#000000");
		ssj4.setBodyColor2("#C21E56");
		ssj4.setEye1Color("#FFD700");
		ssj4.setEye2Color("#FFD700");
		ssj4.setAuraColor("#FF0000");
		ssj4.setModelScaling(new float[]{0.9375f, 0.9375f, 0.9375f});
		ssj4.setStrMultiplier(4.5);
		ssj4.setSkpMultiplier(4.5);
		ssj4.setDefMultiplier(2.5);
		ssj4.setPwrMultiplier(4.5);
		ssj4.setEnergyDrain(0.22);
		ssj4.setHairType("base");
		setDefaultMasteryValues(ssj4);
		ssj4.setStackDrainMultiplier(2.0);
		ssj4.setDirectTransformation(true);

		Map<String, FormConfig.FormData> oozaruFormData = new LinkedHashMap<>();
		oozaruFormData.put(SaiyanForms.OOZARU, oozaru);
		oozaruFormData.put(SaiyanForms.GOLDEN_OOZARU, goldenOozaru);
		//oozaruFormData.put(SaiyanForms.SUPER_SAIYAN_4, ssj4);
		oozaruForms.setForms(oozaruFormData);

		FormConfig ssGrades = new FormConfig();
		ssGrades.setGroupName(SaiyanForms.GROUP_SSGRADES);
		ssGrades.setFormType("super");

		FormConfig.FormData ssj1 = new FormConfig.FormData();
		ssj1.setName(SaiyanForms.SUPER_SAIYAN);
		ssj1.setUnlockOnSkillLevel(1);
		ssj1.setHairColor("#FFEDB3");
		ssj1.setBodyColor2("#FFEDB3");
		ssj1.setEye1Color("#00FFFF");
		ssj1.setEye2Color("#00FFFF");
		ssj1.setAuraColor("#FFD700");
		ssj1.setModelScaling(new float[]{0.9375f, 0.9375f, 0.9375f});
		ssj1.setStrMultiplier(1.5);
		ssj1.setSkpMultiplier(1.5);
		ssj1.setDefMultiplier(1.25);
		ssj1.setPwrMultiplier(1.5);
		ssj1.setEnergyDrain(0.08);
		ssj1.setHairType("ssj");
		setDefaultMasteryValues(ssj1);
		ssj1.setStackDrainMultiplier(2.0);
		ssj1.setCanAlwaysTransform(true);

		FormConfig.FormData ssg2 = new FormConfig.FormData();
		ssg2.setName(SaiyanForms.SUPER_SAIYAN_GRADE_2);
		ssg2.setUnlockOnSkillLevel(2);
		ssg2.setCustomModel("");
		ssg2.setHairColor("#FFEDB3");
		ssg2.setBodyColor2("#FFEDB3");
		ssg2.setEye1Color("#00FFFF");
		ssg2.setEye2Color("#00FFFF");
		ssg2.setAuraColor("#FFD700");
		ssg2.setModelScaling(new float[]{1.0f, 1.0f, 1.0f});
		ssg2.setStrMultiplier(1.75);
		ssg2.setSkpMultiplier(1.75);
		ssg2.setDefMultiplier(1.4);
		ssg2.setPwrMultiplier(1.75);
		ssg2.setSpeedMultiplier(0.9);
		ssg2.setEnergyDrain(0.12);
		ssg2.setStaminaDrainMultiplier(1.3);
		ssg2.setHairType("ssj");
		setDefaultMasteryValues(ssg2);
		ssg2.setStackDrainMultiplier(2.0);

		FormConfig.FormData ssg3 = new FormConfig.FormData();
		ssg3.setName(SaiyanForms.SUPER_SAIYAN_GRADE_3);
		ssg3.setUnlockOnSkillLevel(3);
		ssg3.setHairColor("#FFEDB3");
		ssg3.setBodyColor2("#FFEDB3");
		ssg3.setEye1Color("#00FFFF");
		ssg3.setEye2Color("#00FFFF");
		ssg3.setAuraColor("#FFD700");
		ssg3.setModelScaling(new float[]{1.2f, 1.1f, 1.2f});
		ssg3.setStrMultiplier(2.75);
		ssg3.setSkpMultiplier(2.75);
		ssg3.setDefMultiplier(1.8);
		ssg3.setPwrMultiplier(2.75);
		ssg3.setSpeedMultiplier(0.7);
		ssg3.setEnergyDrain(0.28);
		ssg3.setStaminaDrainMultiplier(3.5);
		ssg3.setAttackSpeed(0.75);
		ssg3.setHairType("ssj");
		setDefaultMasteryValues(ssg3);
		ssg3.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> ssGradeForms = new LinkedHashMap<>();
		ssGradeForms.put(SaiyanForms.SUPER_SAIYAN, ssj1);
		ssGradeForms.put(SaiyanForms.SUPER_SAIYAN_GRADE_2, ssg2);
		ssGradeForms.put(SaiyanForms.SUPER_SAIYAN_GRADE_3, ssg3);
		ssGrades.setForms(ssGradeForms);

		FormConfig superSaiyan = new FormConfig();
		superSaiyan.setGroupName(SaiyanForms.GROUP_SUPERSAIYAN);
		superSaiyan.setFormType("super");

		FormConfig.FormData ssj1Mastered = new FormConfig.FormData();
		ssj1Mastered.setName(SaiyanForms.SUPER_SAIYAN_MASTERED);
		ssj1Mastered.setUnlockOnSkillLevel(4);
		ssj1Mastered.setHairColor("#FFE89E");
		ssj1Mastered.setBodyColor2("#FFE89E");
		ssj1Mastered.setEye1Color("#00FFFF");
		ssj1Mastered.setEye2Color("#00FFFF");
		ssj1Mastered.setAuraColor("#FFD700");
		ssj1Mastered.setModelScaling(new float[]{0.9375f, 0.9375f, 0.9375f});
		ssj1Mastered.setStrMultiplier(1.75);
		ssj1Mastered.setSkpMultiplier(1.75);
		ssj1Mastered.setDefMultiplier(1.35);
		ssj1Mastered.setPwrMultiplier(1.75);
		ssj1Mastered.setEnergyDrain(0.03);
		ssj1Mastered.setHairType("ssj");
		setDefaultMasteryValues(ssj1Mastered);
		ssj1Mastered.setStackDrainMultiplier(2.0);
		ssj1Mastered.setCanAlwaysTransform(true);

		FormConfig.FormData ssj2 = new FormConfig.FormData();
		ssj2.setName(SaiyanForms.SUPER_SAIYAN_2);
		ssj2.setUnlockOnSkillLevel(5);
		ssj2.setHairColor("#FFE89E");
		ssj2.setBodyColor2("#FFE89E");
		ssj2.setEye1Color("#00FFFF");
		ssj2.setEye2Color("#00FFFF");
		ssj2.setAuraColor("#FFD700");
		ssj2.setHasLightnings(true);
		ssj2.setLightningColor("#A1FFF9");
		ssj2.setModelScaling(new float[]{0.9375f, 0.9375f, 0.9375f});
		ssj2.setStrMultiplier(2.25);
		ssj2.setSkpMultiplier(2.25);
		ssj2.setDefMultiplier(1.65);
		ssj2.setPwrMultiplier(2.25);
		ssj2.setEnergyDrain(0.16);
		ssj2.setHairType("ssj2");
		setDefaultMasteryValues(ssj2);
		ssj2.setStackDrainMultiplier(2.0);

		FormConfig.FormData ssj3 = new FormConfig.FormData();
		ssj3.setName(SaiyanForms.SUPER_SAIYAN_3);
		ssj3.setUnlockOnSkillLevel(6);
		ssj3.setHairColor("#FFE89E");
		ssj3.setBodyColor2("#FFE89E");
		ssj3.setEye1Color("#00FFFF");
		ssj3.setEye2Color("#00FFFF");
		ssj3.setAuraColor("#FFD700");
		ssj3.setHasLightnings(true);
		ssj3.setLightningColor("#A1FFF9");
		ssj3.setModelScaling(new float[]{0.9375f, 0.9375f, 0.9375f});
		ssj3.setStrMultiplier(3.0);
		ssj3.setSkpMultiplier(3.0);
		ssj3.setDefMultiplier(2.15);
		ssj3.setPwrMultiplier(3.0);
		ssj3.setEnergyDrain(0.34);
		ssj3.setHairType("ssj3");
		setDefaultMasteryValues(ssj3);
		ssj3.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> superSaiyanForms = new LinkedHashMap<>();
		superSaiyanForms.put(SaiyanForms.SUPER_SAIYAN_MASTERED, ssj1Mastered);
		superSaiyanForms.put(SaiyanForms.SUPER_SAIYAN_2, ssj2);
		superSaiyanForms.put(SaiyanForms.SUPER_SAIYAN_3, ssj3);
		superSaiyan.setForms(superSaiyanForms);

		forms.put(SaiyanForms.OOZARU, oozaruForms);
		forms.put(SaiyanForms.GROUP_SSGRADES, ssGrades);
		forms.put(SaiyanForms.SUPER_SAIYAN, superSaiyan);

		Path oozaruPath = formsPath.resolve(SaiyanForms.OOZARU + ".json");
		loader.saveConfig(oozaruPath, oozaruForms);
		Path ssGradesPath = formsPath.resolve(SaiyanForms.GROUP_SSGRADES + ".json");
		loader.saveConfig(ssGradesPath, ssGrades);
		Path superSaiyanPath = formsPath.resolve(SaiyanForms.SUPER_SAIYAN + ".json");
		loader.saveConfig(superSaiyanPath, superSaiyan);
		LogUtil.info(Env.COMMON, "Default Super Saiyan forms created");
	}

	private void createNamekianForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig namekianForms = new FormConfig();
		namekianForms.setGroupName(NamekianForms.GROUP_SUPERFORMS);
		namekianForms.setFormType("super");

		FormConfig.FormData giantForm = new FormConfig.FormData();
		giantForm.setName(NamekianForms.GIANT);
		giantForm.setUnlockOnSkillLevel(1);
		giantForm.setModelScaling(new float[]{3.6f, 3.6f, 3.6f});
		giantForm.setStrMultiplier(1.5);
		giantForm.setSkpMultiplier(1.5);
		giantForm.setDefMultiplier(1.4);
		giantForm.setPwrMultiplier(1.5);
		giantForm.setEnergyDrain(0.09);
		giantForm.setHairType("base");
		setDefaultMasteryValues(giantForm);
		giantForm.setStackDrainMultiplier(2.0);
		giantForm.setCanAlwaysTransform(true);

		FormConfig.FormData fullPower = new FormConfig.FormData();
		fullPower.setName(NamekianForms.FULLPOWER);
		fullPower.setUnlockOnSkillLevel(2);
		fullPower.setModelScaling(new float[]{1.0f, 1.0f, 1.0f});
		fullPower.setStrMultiplier(2.25);
		fullPower.setSkpMultiplier(2.25);
		fullPower.setDefMultiplier(1.85);
		fullPower.setPwrMultiplier(2.25);
		fullPower.setEnergyDrain(0.18);
		fullPower.setHairType("base");
		setDefaultMasteryValues(fullPower);
		fullPower.setStackDrainMultiplier(2.0);

		FormConfig.FormData superNamekian = new FormConfig.FormData();
		superNamekian.setName(NamekianForms.SUPER_NAMEKIAN);
		superNamekian.setUnlockOnSkillLevel(3);
		superNamekian.setAuraColor("#7FFF00");
		superNamekian.setHasLightnings(true);
		superNamekian.setLightningColor("#FFFFFF");
		superNamekian.setModelScaling(new float[]{1.05f, 1.05f, 1.05f});
		superNamekian.setStrMultiplier(3.0);
		superNamekian.setSkpMultiplier(3.0);
		superNamekian.setDefMultiplier(2.35);
		superNamekian.setPwrMultiplier(3.0);
		superNamekian.setEnergyDrain(0.27);
		superNamekian.setHairType("base");
		setDefaultMasteryValues(superNamekian);
		superNamekian.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> namekianFormData = new LinkedHashMap<>();
		namekianFormData.put(NamekianForms.GIANT, giantForm);
		namekianFormData.put(NamekianForms.FULLPOWER, fullPower);
		namekianFormData.put(NamekianForms.SUPER_NAMEKIAN, superNamekian);
		namekianForms.setForms(namekianFormData);

		forms.put(NamekianForms.GROUP_SUPERFORMS, namekianForms);

		Path namekianPath = formsPath.resolve(NamekianForms.GROUP_SUPERFORMS + ".json");
		loader.saveConfig(namekianPath, namekianForms);
		LogUtil.info(Env.COMMON, "Default Namekian forms created");
	}

	private void createFrostDemonForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig frostForms = new FormConfig();
		frostForms.setGroupName(FrostDemonForms.GROUP_EVOLUTIONFORMS);
		frostForms.setFormType("super");

		FormConfig.FormData second = new FormConfig.FormData();
		second.setName(FrostDemonForms.SECOND_FORM);
		second.setUnlockOnSkillLevel(1);
		second.setCustomModel("");
		second.setModelScaling(new float[]{1.3f, 1.3f, 1.3f});
		second.setStrMultiplier(1.5);
		second.setSkpMultiplier(1.5);
		second.setDefMultiplier(1.25);
		second.setPwrMultiplier(1.5);
		second.setHairType("base");
		setDefaultMasteryValues(second);
		second.setStackDrainMultiplier(2.0);
		second.setCanAlwaysTransform(true);

		FormConfig.FormData third = new FormConfig.FormData();
		third.setName(FrostDemonForms.THIRD_FORM);
		third.setUnlockOnSkillLevel(2);
		third.setCustomModel("frostdemon_third");
		third.setModelScaling(new float[]{1.4f, 1.4f, 1.4f});
		third.setStrMultiplier(1.75);
		third.setSkpMultiplier(1.75);
		third.setDefMultiplier(1.65);
		third.setPwrMultiplier(1.75);
		third.setHairType("base");
		setDefaultMasteryValues(third);
		third.setStackDrainMultiplier(2.0);

		FormConfig.FormData finalForm = new FormConfig.FormData();
		finalForm.setName(FrostDemonForms.FINAL_FORM);
		finalForm.setUnlockOnSkillLevel(3);
		finalForm.setModelScaling(new float[]{1.0f, 1.0f, 1.0f});
		finalForm.setStrMultiplier(2.25);
		finalForm.setSkpMultiplier(2.25);
		finalForm.setDefMultiplier(1.85);
		finalForm.setPwrMultiplier(2.25);
		finalForm.setHairType("base");
		setDefaultMasteryValues(finalForm);
		finalForm.setStackDrainMultiplier(2.0);

		FormConfig.FormData fullPower = new FormConfig.FormData();
		fullPower.setName(FrostDemonForms.FULLPOWER);
		fullPower.setUnlockOnSkillLevel(4);
		fullPower.setModelScaling(new float[]{1.3f, 1.2f, 1.3f});
		fullPower.setStrMultiplier(2.75);
		fullPower.setSkpMultiplier(2.75);
		fullPower.setDefMultiplier(2.15);
		fullPower.setPwrMultiplier(2.75);
		fullPower.setEnergyDrain(0.22);
		fullPower.setStaminaDrainMultiplier(2.5);
		fullPower.setAttackSpeed(0.75);
		fullPower.setLightningColor("#F02B16");
		fullPower.setHairType("base");
		setDefaultMasteryValues(fullPower);
		fullPower.setStackDrainMultiplier(2.0);

		FormConfig.FormData fifthForm = new FormConfig.FormData();
		fifthForm.setName(FrostDemonForms.FIFTH_FORM);
		fifthForm.setUnlockOnSkillLevel(5);
		fifthForm.setCustomModel("frostdemon_fifth");
		fifthForm.setModelScaling(new float[]{1.4f, 1.3f, 1.4f});
		fifthForm.setStrMultiplier(3.0);
		fifthForm.setSkpMultiplier(3.0);
		fifthForm.setDefMultiplier(2.35);
		fifthForm.setPwrMultiplier(3.0);
		fifthForm.setEnergyDrain(0.28);
		fifthForm.setEye1Color("#D91E1E");
		fifthForm.setEye2Color("#D91E1E");
		fifthForm.setHasLightnings(true);
		fifthForm.setLightningColor("#F02B16");
		fifthForm.setHairType("base");
		setDefaultMasteryValues(fifthForm);
		fifthForm.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> frostFormData = new LinkedHashMap<>();
		frostFormData.put(FrostDemonForms.SECOND_FORM, second);
		frostFormData.put(FrostDemonForms.THIRD_FORM, third);
		frostFormData.put(FrostDemonForms.FINAL_FORM, finalForm);
		frostFormData.put(FrostDemonForms.FULLPOWER, fullPower);
		frostFormData.put(FrostDemonForms.FIFTH_FORM, fifthForm);
		frostForms.setForms(frostFormData);

		forms.put(FrostDemonForms.GROUP_EVOLUTIONFORMS, frostForms);

		Path frostPath = formsPath.resolve(FrostDemonForms.GROUP_EVOLUTIONFORMS + ".json");
		loader.saveConfig(frostPath, frostForms);
		LogUtil.info(Env.COMMON, "Default Frost Demon forms created");
	}

	private void createMajinForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig majinForms = new FormConfig();
		majinForms.setGroupName(MajinForms.GROUP_PUREFORMS);
		majinForms.setFormType("super");

		FormConfig.FormData kid = new FormConfig.FormData();
		kid.setName(MajinForms.KID);
		kid.setUnlockOnSkillLevel(1);
		kid.setCustomModel("majin_kid");
		kid.setModelScaling(new float[]{0.7f, 0.7f, 0.7f});
		kid.setStrMultiplier(1.5);
		kid.setSkpMultiplier(1.5);
		kid.setDefMultiplier(1.25);
		kid.setPwrMultiplier(1.5);
		kid.setHairType("base");
		setDefaultMasteryValues(kid);
		kid.setStackDrainMultiplier(2.0);
		kid.setCanAlwaysTransform(true);

		FormConfig.FormData evil = new FormConfig.FormData();
		evil.setName(MajinForms.EVIL);
		evil.setUnlockOnSkillLevel(2);
		evil.setCustomModel("majin_evil");
		evil.setModelScaling(new float[]{0.9f, 1.0f, 0.9f});
		evil.setStrMultiplier(1.75);
		evil.setSkpMultiplier(1.75);
		evil.setDefMultiplier(1.65);
		evil.setPwrMultiplier(1.75);
		evil.setHairColor("#917979");
		evil.setEye1Color("#F52746");
		evil.setEye2Color("#F52746");
		evil.setBodyColor1("#917979");
		evil.setBodyColor2("#917979");
		evil.setBodyColor3("#917979");
		evil.setHairType("base");
		setDefaultMasteryValues(evil);
		evil.setStackDrainMultiplier(2.0);

		FormConfig.FormData superForm = new FormConfig.FormData();
		superForm.setName(MajinForms.SUPER);
		superForm.setUnlockOnSkillLevel(3);
		superForm.setModelScaling(new float[]{1.0f, 1.0f, 1.0f});
		superForm.setStrMultiplier(2.25);
		superForm.setSkpMultiplier(2.25);
		superForm.setDefMultiplier(2.15);
		superForm.setPwrMultiplier(2.25);
		superForm.setHairType("base");
		setDefaultMasteryValues(superForm);
		superForm.setStackDrainMultiplier(2.0);

		FormConfig.FormData ultra = new FormConfig.FormData();
		ultra.setName(MajinForms.ULTRA);
		ultra.setUnlockOnSkillLevel(4);
		ultra.setModelScaling(new float[]{1.3f, 1.2f, 1.3f});
		ultra.setStrMultiplier(3.0);
		ultra.setSkpMultiplier(3.0);
		ultra.setDefMultiplier(2.5);
		ultra.setPwrMultiplier(3.0);
		ultra.setEnergyDrain(0.22);
		ultra.setStaminaDrainMultiplier(3.5);
		ultra.setHasLightnings(true);
		ultra.setLightningColor("#F02B16");
		ultra.setHairType("base");
		setDefaultMasteryValues(ultra);
		ultra.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> majinFormData = new LinkedHashMap<>();
		majinFormData.put(MajinForms.KID, kid);
		majinFormData.put(MajinForms.EVIL, evil);
		majinFormData.put(MajinForms.SUPER, superForm);
		majinFormData.put(MajinForms.ULTRA, ultra);
		majinForms.setForms(majinFormData);

		forms.put(MajinForms.GROUP_PUREFORMS, majinForms);

		Path majinPath = formsPath.resolve(MajinForms.GROUP_PUREFORMS + ".json");
		loader.saveConfig(majinPath, majinForms);
		LogUtil.info(Env.COMMON, "Default Majin forms created");
	}

	private void createBioAndroidForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig bioForms = new FormConfig();
		bioForms.setGroupName(BioAndroidForms.GROUP_BIOEVOLUTION);
		bioForms.setFormType("super");

		FormConfig.FormData semiPerfect = new FormConfig.FormData();
		semiPerfect.setName(BioAndroidForms.SEMI_PERFECT);
		semiPerfect.setUnlockOnSkillLevel(1);
		semiPerfect.setCustomModel("bioandroid_semi");
		semiPerfect.setModelScaling(new float[]{1.3f, 1.3f, 1.3f});
		semiPerfect.setStrMultiplier(1.5);
		semiPerfect.setSkpMultiplier(1.5);
		semiPerfect.setDefMultiplier(1.4);
		semiPerfect.setPwrMultiplier(1.5);
        semiPerfect.setHairColor("");
        semiPerfect.setEye1Color("#0095FF");
        semiPerfect.setEye2Color("#FFFFFF");
        semiPerfect.setBodyColor1("");
        semiPerfect.setBodyColor2("");
        semiPerfect.setBodyColor3("");
		semiPerfect.setHairType("base");
		setDefaultMasteryValues(semiPerfect);
		semiPerfect.setStackDrainMultiplier(2.0);
		semiPerfect.setCanAlwaysTransform(true);

		FormConfig.FormData perfect = new FormConfig.FormData();
		perfect.setName(BioAndroidForms.PERFECT);
		perfect.setUnlockOnSkillLevel(2);
		perfect.setCustomModel("bioandroid_perfect");
		perfect.setModelScaling(new float[]{1.1f, 1.1f, 1.1f});
		perfect.setStrMultiplier(2.25);
		perfect.setSkpMultiplier(2.25);
		perfect.setDefMultiplier(1.85);
		perfect.setPwrMultiplier(2.25);
        perfect.setHairColor("");
        perfect.setEye1Color("#F6A6FF");
        perfect.setEye2Color("#FFFFFF");
        perfect.setBodyColor1("");
        perfect.setBodyColor2("");
        perfect.setBodyColor3("");
		perfect.setHairType("base");
		setDefaultMasteryValues(perfect);
		perfect.setStackDrainMultiplier(2.0);

		FormConfig.FormData superPerfect = new FormConfig.FormData();
		superPerfect.setName(BioAndroidForms.SUPER_PERFECT);
		superPerfect.setUnlockOnSkillLevel(3);
		superPerfect.setCustomModel("bioandroid_perfect");
		superPerfect.setModelScaling(new float[]{1.1f, 1.1f, 1.1f});
		superPerfect.setStrMultiplier(3.0);
		superPerfect.setSkpMultiplier(3.0);
		superPerfect.setDefMultiplier(2.35);
		superPerfect.setPwrMultiplier(3.0);
		superPerfect.setEnergyDrain(0.16);
        superPerfect.setEye1Color("#F6A6FF");
        superPerfect.setEye2Color("#FFFFFF");
        superPerfect.setBodyColor1("");
        superPerfect.setBodyColor2("");
        superPerfect.setBodyColor3("");
		superPerfect.setAuraColor("FFFF69");
		superPerfect.setHasLightnings(true);
		superPerfect.setLightningColor("#1AA1C7");
		superPerfect.setHairType("base");
		setDefaultMasteryValues(superPerfect);
		superPerfect.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> bioFormData = new LinkedHashMap<>();
		bioFormData.put(BioAndroidForms.SEMI_PERFECT, semiPerfect);
		bioFormData.put(BioAndroidForms.PERFECT, perfect);
		bioFormData.put(BioAndroidForms.SUPER_PERFECT, superPerfect);
		bioForms.setForms(bioFormData);

		forms.put(BioAndroidForms.GROUP_BIOEVOLUTION, bioForms);

		Path bioAndroidPath = formsPath.resolve(BioAndroidForms.GROUP_BIOEVOLUTION + ".json");
		loader.saveConfig(bioAndroidPath, bioForms);
		LogUtil.info(Env.COMMON, "Default Bio Android forms created");
	}
}