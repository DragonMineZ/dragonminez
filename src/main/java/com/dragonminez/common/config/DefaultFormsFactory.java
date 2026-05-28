package com.dragonminez.common.config;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.util.lists.*;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@AllArgsConstructor
public class DefaultFormsFactory {
	private void setDefaultMasteryValues(FormConfig.FormData form) {
		form.setMaxMastery(100.0);
		form.setMasteryPerHit(0.025);
		form.setMasteryPerDamageReceived(0.025);
		form.setStatMultPerMasteryPoint(0.0075);
		form.setCostDecreasePerMasteryPoint(0.025);
		form.setPassiveMasteryGainEveryFiveSeconds(0.01);
		form.setAuraType("kakarot");
		form.setAuraLayer(0);
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
 		createDefaultUltimateForms(formsPath, forms);
//        createDefaultUltraInstinctForms(formsPath, forms);
//        createDefaultUltraEgoForms(formsPath, forms);
	}

	public void createDefaultKaiokenForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig kaiokenForms = new FormConfig();
		kaiokenForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		kaiokenForms.setGroupName(StackForms.GROUP_KAIOKEN);
		kaiokenForms.setFormType(StackForms.GROUP_KAIOKEN);

		FormConfig.FormData x2 = new FormConfig.FormData();
		x2.setName(StackForms.X2);
		x2.setUnlockOnSkillLevel(1);
		x2.setKeepBaseFormHeadBones(true);
		x2.setStrMultiplier(1.1);
		x2.setSkpMultiplier(1.1);
		x2.setDefMultiplier(1.1);
		x2.setPwrMultiplier(1.1);
		x2.setSpeedMultiplier(1.1);
		x2.setHealthDrain(0.03);
		x2.setAttackSpeed(1.1);
		x2.setAuraLayer(1);
		x2.setAuraColor("#DB182C");
		x2.setHasLightnings(false);
		x2.setHairType("");
		setDefaultMasteryValues(x2);
		x2.setStackDrainMultiplier(1.0);
		x2.setCanAlwaysTransform(true);

		FormConfig.FormData x3 = new FormConfig.FormData();
		x3.setName(StackForms.X3);
		x3.setUnlockOnSkillLevel(2);
		x3.setKeepBaseFormHeadBones(true);
		x3.setStrMultiplier(1.2);
		x3.setSkpMultiplier(1.2);
		x3.setDefMultiplier(1.2);
		x3.setPwrMultiplier(1.2);
		x3.setSpeedMultiplier(1.2);
		x3.setAttackSpeed(1.2);
		x3.setHealthDrain(0.06);
		x3.setAuraLayer(1);
		x3.setAuraColor("#DB182C");
		x3.setHairType("");
		setDefaultMasteryValues(x3);
		x3.setStackDrainMultiplier(1.0);

		FormConfig.FormData x4 = new FormConfig.FormData();
		x4.setName(StackForms.X4);
		x4.setUnlockOnSkillLevel(3);
		x4.setKeepBaseFormHeadBones(true);
		x4.setStrMultiplier(1.35);
		x4.setSkpMultiplier(1.35);
		x4.setDefMultiplier(1.35);
		x4.setPwrMultiplier(1.35);
		x4.setSpeedMultiplier(1.35);
		x4.setAttackSpeed(1.35);
		x4.setHealthDrain(0.095);
		x4.setAuraLayer(1);
		x4.setAuraColor("#DB182C");
		x4.setHairType("");
		setDefaultMasteryValues(x4);
		x4.setStackDrainMultiplier(1.0);

		FormConfig.FormData x10 = new FormConfig.FormData();
		x10.setName(StackForms.X10);
		x10.setUnlockOnSkillLevel(4);
		x10.setKeepBaseFormHeadBones(true);
		x10.setStrMultiplier(1.5);
		x10.setSkpMultiplier(1.5);
		x10.setDefMultiplier(1.5);
		x10.setPwrMultiplier(1.5);
		x10.setSpeedMultiplier(1.5);
		x10.setHealthDrain(0.11);
		x10.setAttackSpeed(1.5);
		x10.setAuraLayer(1);
		x10.setAuraColor("#DB182C");
		x10.setHairType("");
		setDefaultMasteryValues(x10);
		x10.setStackDrainMultiplier(1.0);

		FormConfig.FormData x20 = new FormConfig.FormData();
		x20.setName(StackForms.X20);
		x20.setUnlockOnSkillLevel(5);
		x20.setKeepBaseFormHeadBones(true);
		x20.setStrMultiplier(1.65);
		x20.setSkpMultiplier(1.65);
		x20.setDefMultiplier(1.65);
		x20.setPwrMultiplier(1.65);
		x20.setSpeedMultiplier(1.65);
		x20.setHealthDrain(0.15);
		x20.setAttackSpeed(1.65);
		x20.setAuraLayer(1);
		x20.setAuraColor("#DB182C");
		x20.setHairType("");
		setDefaultMasteryValues(x20);
		x20.setStackDrainMultiplier(1.0);

		FormConfig.FormData x100 = new FormConfig.FormData();
		x100.setName(StackForms.X100);
		x100.setUnlockOnSkillLevel(6);
		x100.setKeepBaseFormHeadBones(true);
		x100.setStrMultiplier(2.0);
		x100.setSkpMultiplier(2.0);
		x100.setDefMultiplier(2.0);
		x100.setPwrMultiplier(2.0);
		x100.setSpeedMultiplier(2.0);
		x100.setHealthDrain(0.20);
		x100.setAttackSpeed(2.0);
		x100.setAuraLayer(1);
		x100.setAuraColor("#DB182C");
		x100.setHairType("");
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
		LogUtil.info(Env.COMMON, "Default Kaioken forms created");
	}

	public void createDefaultUltimateForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig ultimateForms = new FormConfig();
		ultimateForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		ultimateForms.setGroupName(StackForms.GROUP_ULTIMATE);
		ultimateForms.setFormType(StackForms.GROUP_ULTIMATE);

		FormConfig.FormData ultimate = new FormConfig.FormData();
		ultimate.setName(StackForms.ULTIMATE);
		ultimate.setUnlockOnSkillLevel(1);
		ultimate.setKeepBaseFormHeadBones(true);
		ultimate.setCustomModel("");
		ultimate.setStrMultiplier(1.25);
		ultimate.setSkpMultiplier(1.25);
		ultimate.setDefMultiplier(1.15);
		ultimate.setPwrMultiplier(1.25);
		ultimate.setEnergyDrain(0.0);
		ultimate.setStaminaDrain(0.0);
		ultimate.setHealthDrain(0.0);
		ultimate.setAttackSpeed(1.0);
		ultimate.setHairType("base");
		ultimate.setMaxMastery(0.0);
		ultimate.setMasteryPerHit(0.0);
		ultimate.setMasteryPerDamageReceived(0.0);
		ultimate.setStatMultPerMasteryPoint(0.0);
		ultimate.setPassiveMasteryGainEveryFiveSeconds(0.0);
		ultimate.setFormStackable(false);
		ultimate.setStackDrainMultiplier(1.0);
		ultimate.setCanAlwaysTransform(true);

		Map<String, FormConfig.FormData> stackFormData = new LinkedHashMap<>();
		stackFormData.put(StackForms.ULTIMATE, ultimate);
		ultimateForms.setForms(stackFormData);

		forms.put(StackForms.GROUP_ULTIMATE, ultimateForms);
		LogUtil.info(Env.COMMON, "Default Ultimate stack form created");
	}

	public void createDefaultUltraInstinctForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig ultraInstinctForms = new FormConfig();
		ultraInstinctForms.setConfigVersion(FormConfig.CURRENT_VERSION);
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
		sign.setAuraLayer(1);
		sign.setAuraColor("#E0E0E0");
		sign.setHasLightnings(false);
		sign.setHairType("");
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
		mastered.setAuraLayer(1);
		mastered.setAuraColor("#E0E0E0");
		mastered.setHairColor("#E0E0E0");
		mastered.setBodyColor2("#E0E0E0");
		mastered.setHairType("");
		setDefaultMasteryValues(mastered);
		mastered.setStackDrainMultiplier(1.0);
		mastered.setDirectTransformation(true);

		Map<String, FormConfig.FormData> stackFormData = new LinkedHashMap<>();
		stackFormData.put(StackForms.ULTRAINSTINCT_SIGN, sign);
		stackFormData.put(StackForms.ULTRAINSTINCT_MASTERED, mastered);
		ultraInstinctForms.setForms(stackFormData);

		forms.put(StackForms.GROUP_ULTRAINSTINCT, ultraInstinctForms);
		LogUtil.info(Env.COMMON, "Default Ultra Instict forms created");
	}

	public void createDefaultUltraEgoForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig ultraEgoForms = new FormConfig();
		ultraEgoForms.setConfigVersion(FormConfig.CURRENT_VERSION);
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
		sign.setAuraLayer(1);
		sign.setAuraColor("#66023C");
		sign.setHasLightnings(false);
		sign.setHairType("");
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
		mastered.setAuraLayer(1);
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
		LogUtil.info(Env.COMMON, "Default Ultra Ego forms created");
	}

	private void createDefaultHumanForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig humanForms = new FormConfig();
		humanForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		humanForms.setGroupName(HumanForms.GROUP_SUPERFORMS);
		humanForms.setFormType("superforms");

		FormConfig.FormData buffed = new FormConfig.FormData();
		buffed.setName(HumanForms.BUFFED);
		buffed.setUnlockOnSkillLevel(1);
		buffed.setCustomModel("buffed");
		buffed.setModelScaling(new Float[]{1.2f, 1.1f, 1.2f});
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
		fullPower.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
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
		overdrive.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
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
		solaris.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
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
		LogUtil.info(Env.COMMON, "Default Human forms created");

		FormConfig humanLegendaryForms = new FormConfig();
		humanLegendaryForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		humanLegendaryForms.setGroupName(HumanForms.GROUP_LEGENDARYFORMS);
		humanLegendaryForms.setFormType("legendaryforms");

		FormConfig.FormData shiyoken = new FormConfig.FormData();
		shiyoken.setName(HumanForms.SHIYOKEN);
		shiyoken.setUnlockOnSkillLevel(1);
		shiyoken.setCustomModel("");
		shiyoken.setStrMultiplier(4.25);
		shiyoken.setSkpMultiplier(4.25);
		shiyoken.setDefMultiplier(4.25);
		shiyoken.setPwrMultiplier(4.25);
		shiyoken.setHairType("base");
		setDefaultMasteryValues(shiyoken);
		shiyoken.setStackDrainMultiplier(2.0);
		shiyoken.setCanAlwaysTransform(true);

		FormConfig.FormData lunaris = new FormConfig.FormData();
		lunaris.setName(HumanForms.LUNARIS);
		lunaris.setUnlockOnSkillLevel(2);
		lunaris.setCustomModel("");
		lunaris.setStrMultiplier(5.5);
		lunaris.setSkpMultiplier(5.5);
		lunaris.setDefMultiplier(5.5);
		lunaris.setPwrMultiplier(5.5);
		lunaris.setHairType("base");
		setDefaultMasteryValues(lunaris);
		lunaris.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> humanLegendaryData = new LinkedHashMap<>();
		humanLegendaryData.put(HumanForms.SHIYOKEN, shiyoken);
		humanLegendaryData.put(HumanForms.LUNARIS, lunaris);
		humanLegendaryForms.setForms(humanLegendaryData);

		forms.put(HumanForms.GROUP_LEGENDARYFORMS, humanLegendaryForms);
		LogUtil.info(Env.COMMON, "Default Human legendary forms created");

		createAndroidForms(formsPath, forms);
	}

	private void createAndroidForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig androidForms = new FormConfig();
		androidForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		androidForms.setGroupName(HumanForms.GROUP_ANDROIDFORMS);
		androidForms.setFormType("androidforms");

		FormConfig.FormData androidBase = new FormConfig.FormData();
		androidBase.setName(HumanForms.ANDROID_BASE);
		androidBase.setUnlockOnSkillLevel(0);
		androidBase.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
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
		superAndroid.setModelScaling(new Float[]{1.05f, 1.05f, 1.05f});
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
		fusedAndroid.setCustomModel("buffed");
		fusedAndroid.setModelScaling(new Float[]{1.4f, 1.3f, 1.4f});
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
		LogUtil.info(Env.COMMON, "Default Android forms created for Humans");
	}

	private void createSaiyanForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig oozaruForms = new FormConfig();
		oozaruForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		oozaruForms.setGroupName(SaiyanForms.GROUP_OOZARU);
		oozaruForms.setFormType("superforms");

		FormConfig.FormData oozaru = new FormConfig.FormData();
		oozaru.setName(SaiyanForms.OOZARU);
		oozaru.setUnlockOnSkillLevel(0);
		oozaru.setFormCombo("dragonminez:giant");
		oozaru.setCustomModel("oozaru");
		oozaru.setModelScaling(new Float[]{3.8f, 3.8f, 3.8f});
		oozaru.setStrMultiplier(1.2);
		oozaru.setSkpMultiplier(1.2);
		oozaru.setDefMultiplier(1.1);
		oozaru.setPwrMultiplier(1.2);
		oozaru.setSpeedMultiplier(0.8);
		oozaru.setEnergyDrain(0.05);
		oozaru.setStaminaDrainMultiplier(1.2);
		oozaru.setAttackSpeed(0.25);
		oozaru.setAttackSpeed(0.9);
		oozaru.setHairType("base");
		setDefaultMasteryValues(oozaru);
		oozaru.setStackDrainMultiplier(2.0);
		oozaru.setCanAlwaysTransform(true);

		FormConfig.FormData goldenOozaru = new FormConfig.FormData();
		goldenOozaru.setName(SaiyanForms.GOLDEN_OOZARU);
		goldenOozaru.setUnlockOnSkillLevel(7);
		goldenOozaru.setFormCombo("dragonminez:giant");
		goldenOozaru.setCustomModel("oozaru");
		goldenOozaru.setHairColor("#FFD700");
		goldenOozaru.setAuraColor("#FFD700");
		goldenOozaru.setBodyColor2("#FFD700");
		goldenOozaru.setModelScaling(new Float[]{3.8f, 3.8f, 3.8f});
		goldenOozaru.setStrMultiplier(2.0);
		goldenOozaru.setSkpMultiplier(2.0);
		goldenOozaru.setDefMultiplier(1.9);
		goldenOozaru.setPwrMultiplier(2.0);
		goldenOozaru.setSpeedMultiplier(0.85);
		goldenOozaru.setEnergyDrain(0.24);
		goldenOozaru.setStaminaDrainMultiplier(1.3);
		goldenOozaru.setAttackSpeed(0.25);
		goldenOozaru.setHairType("base");
		setDefaultMasteryValues(goldenOozaru);
		goldenOozaru.setStackDrainMultiplier(2.0);

		FormConfig.FormData ssj4gt = new FormConfig.FormData();
		ssj4gt.setName(SaiyanForms.SUPER_SAIYAN_4);
		ssj4gt.setCustomModel("ssj4gt");
		ssj4gt.setUnlockOnSkillLevel(8);
		ssj4gt.setHairColor("#000000");
		ssj4gt.setBodyColor2("#B22E4D");
		ssj4gt.setEye1Color("#FFD700");
		ssj4gt.setEye2Color("#FFD700");
		ssj4gt.setAuraColor("#FFD700");
		ssj4gt.setModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		ssj4gt.setStrMultiplier(3.75);
		ssj4gt.setSkpMultiplier(3.75);
		ssj4gt.setDefMultiplier(2.5);
		ssj4gt.setPwrMultiplier(3.75);
		ssj4gt.setEnergyDrain(0.24);
		ssj4gt.setHairType("base");
		ssj4gt.setForcedHairCode("");
		setDefaultMasteryValues(ssj4gt);
		ssj4gt.setStackDrainMultiplier(2.0);
		ssj4gt.setDirectTransformation(true);

		Map<String, FormConfig.FormData> oozaruFormData = new LinkedHashMap<>();
		oozaruFormData.put(SaiyanForms.OOZARU, oozaru);
		oozaruFormData.put(SaiyanForms.GOLDEN_OOZARU, goldenOozaru);
		oozaruFormData.put(SaiyanForms.SUPER_SAIYAN_4, ssj4gt);
		oozaruForms.setForms(oozaruFormData);

		FormConfig ssGrades = new FormConfig();
		ssGrades.setConfigVersion(FormConfig.CURRENT_VERSION);
		ssGrades.setGroupName(SaiyanForms.GROUP_SSGRADES);
		ssGrades.setFormType("superforms");

		FormConfig.FormData ssj1 = new FormConfig.FormData();
		ssj1.setName(SaiyanForms.SUPER_SAIYAN);
		ssj1.setUnlockOnSkillLevel(1);
		ssj1.setHairColor("#FFEDB3");
		ssj1.setBodyColor2("#FFEDB3");
		ssj1.setEye1Color("#00FFFF");
		ssj1.setEye2Color("#00FFFF");
		ssj1.setAuraColor("#FFD700");
		ssj1.setModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
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
		ssg2.setCustomModel("buffed");
		ssg2.setHairColor("#FFEDB3");
		ssg2.setBodyColor2("#FFEDB3");
		ssg2.setEye1Color("#00FFFF");
		ssg2.setEye2Color("#00FFFF");
		ssg2.setAuraColor("#FFD700");
		ssg2.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
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
		ssg3.setCustomModel("buffed");
		ssg3.setHairColor("#FFEDB3");
		ssg3.setBodyColor2("#FFEDB3");
		ssg3.setEye1Color("#00FFFF");
		ssg3.setEye2Color("#00FFFF");
		ssg3.setAuraColor("#FFD700");
		ssg3.setModelScaling(new Float[]{1.2f, 1.2f, 1.2f});
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
		superSaiyan.setConfigVersion(FormConfig.CURRENT_VERSION);
		superSaiyan.setGroupName(SaiyanForms.GROUP_SUPERSAIYAN);
		superSaiyan.setFormType("superforms");

		FormConfig.FormData ssj1Mastered = new FormConfig.FormData();
		ssj1Mastered.setName(SaiyanForms.SUPER_SAIYAN_MASTERED);
		ssj1Mastered.setUnlockOnSkillLevel(4);
		ssj1Mastered.setHairColor("#FFE89E");
		ssj1Mastered.setBodyColor2("#FFE89E");
		ssj1Mastered.setEye1Color("#00FFFF");
		ssj1Mastered.setEye2Color("#00FFFF");
		ssj1Mastered.setAuraColor("#FFD700");
		ssj1Mastered.setModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
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
		ssj2.setModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
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
		ssj3.setModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		ssj3.setStrMultiplier(3.0);
		ssj3.setSkpMultiplier(3.0);
		ssj3.setDefMultiplier(2.15);
		ssj3.setPwrMultiplier(3.0);
		ssj3.setEnergyDrain(0.34);
		ssj3.setHairType("ssj3");
		setDefaultMasteryValues(ssj3);
		ssj3.setStackDrainMultiplier(2.0);

		FormConfig.FormData ssj4d = new FormConfig.FormData();
		ssj4d.setName(SaiyanForms.SUPER_SAIYAN_4);
		ssj4d.setCustomModel("ssj4d");
		ssj4d.setUnlockOnSkillLevel(8);
		ssj4d.setHairColor("#83073F");
		ssj4d.setBodyColor2("#83073F");
		ssj4d.setEye1Color("#83073F");
		ssj4d.setEye2Color("#83073F");
		ssj4d.setAuraColor("#83073F");
		ssj4d.setModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		ssj4d.setStrMultiplier(3.75);
		ssj4d.setSkpMultiplier(3.75);
		ssj4d.setDefMultiplier(2.5);
		ssj4d.setPwrMultiplier(3.75);
		ssj4d.setEnergyDrain(0.24);
		ssj4d.setHairType("base");
		ssj4d.setForcedHairCode("");
		setDefaultMasteryValues(ssj4d);
		ssj4d.setStackDrainMultiplier(2.0);
		ssj4d.setDirectTransformation(true);

		Map<String, FormConfig.FormData> superSaiyanForms = new LinkedHashMap<>();
		superSaiyanForms.put(SaiyanForms.SUPER_SAIYAN_MASTERED, ssj1Mastered);
		superSaiyanForms.put(SaiyanForms.SUPER_SAIYAN_2, ssj2);
		superSaiyanForms.put(SaiyanForms.SUPER_SAIYAN_3, ssj3);
		superSaiyanForms.put(SaiyanForms.SUPER_SAIYAN_4, ssj4d);
		superSaiyan.setForms(superSaiyanForms);

		forms.put(SaiyanForms.OOZARU, oozaruForms);
		forms.put(SaiyanForms.GROUP_SSGRADES, ssGrades);
		forms.put(SaiyanForms.SUPER_SAIYAN, superSaiyan);
		LogUtil.info(Env.COMMON, "Default Super Saiyan forms created");

		FormConfig saiyanLegendaryForms = new FormConfig();
		saiyanLegendaryForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		saiyanLegendaryForms.setGroupName(SaiyanForms.GROUP_LEGENDARYFORMS);
		saiyanLegendaryForms.setFormType("legendaryforms");

		FormConfig.FormData ikari = new FormConfig.FormData();
		ikari.setName(SaiyanForms.IKARI);
		ikari.setUnlockOnSkillLevel(1);
		ikari.setCustomModel("buffed");
		ikari.setHairColor("#000000");
		ikari.setEye1Color("#FFD700");
		ikari.setEye2Color("#FFD700");
		ikari.setAuraColor("#40FF00");
		ikari.setStrMultiplier(4.25);
		ikari.setSkpMultiplier(4.25);
		ikari.setDefMultiplier(4.25);
		ikari.setPwrMultiplier(4.25);
		ikari.setHairType("base");
		setDefaultMasteryValues(ikari);
		ikari.setStackDrainMultiplier(2.0);
		ikari.setCanAlwaysTransform(true);

		FormConfig.FormData ssjHybrid = new FormConfig.FormData();
		ssjHybrid.setName(SaiyanForms.SSJ_HYBRID);
		ssjHybrid.setUnlockOnSkillLevel(2);
		ssjHybrid.setCustomModel("buffed");
		ssjHybrid.setHairColor("#FFE89E");
		ssjHybrid.setEye1Color("#FFFFFF");
		ssjHybrid.setEye2Color("#FFFFFF");
		ssjHybrid.setAuraColor("#40FF00");
		ssjHybrid.setHasLightnings(true);
		ssjHybrid.setLightningColor("#40FF00");
		ssjHybrid.setStrMultiplier(4.75);
		ssjHybrid.setSkpMultiplier(4.75);
		ssjHybrid.setDefMultiplier(4.75);
		ssjHybrid.setPwrMultiplier(4.75);
		ssjHybrid.setHairType("ssj");
		setDefaultMasteryValues(ssjHybrid);
		ssjHybrid.setStackDrainMultiplier(2.0);

		FormConfig.FormData ssjFullPower = new FormConfig.FormData();
		ssjFullPower.setName(SaiyanForms.SSJ_FULL_POWER);
		ssjFullPower.setUnlockOnSkillLevel(3);
		ssjFullPower.setCustomModel("buffed");
		ssjFullPower.setHairColor("#9EFE53");
		ssjFullPower.setEye1Color("#FFFFFF");
		ssjFullPower.setEye2Color("#FFFFFF");
		ssjFullPower.setAuraColor("#40FF00");
		ssjFullPower.setHasLightnings(true);
		ssjFullPower.setLightningColor("#40FF00");
		ssjFullPower.setStrMultiplier(5.5);
		ssjFullPower.setSkpMultiplier(5.5);
		ssjFullPower.setDefMultiplier(5.5);
		ssjFullPower.setPwrMultiplier(5.5);
		ssjFullPower.setHairType("ssj2");
		setDefaultMasteryValues(ssjFullPower);
		ssjFullPower.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> saiyanLegendaryData = new LinkedHashMap<>();
		saiyanLegendaryData.put(SaiyanForms.IKARI, ikari);
		saiyanLegendaryData.put(SaiyanForms.SSJ_HYBRID, ssjHybrid);
		saiyanLegendaryData.put(SaiyanForms.SSJ_FULL_POWER, ssjFullPower);
		saiyanLegendaryForms.setForms(saiyanLegendaryData);

		forms.put(SaiyanForms.GROUP_LEGENDARYFORMS, saiyanLegendaryForms);
		LogUtil.info(Env.COMMON, "Default Saiyan legendary forms created");
	}

	private void createNamekianForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig namekianForms = new FormConfig();
		namekianForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		namekianForms.setGroupName(NamekianForms.GROUP_SUPERFORMS);
		namekianForms.setFormType("superforms");

		FormConfig.FormData giantForm = new FormConfig.FormData();
		giantForm.setName(NamekianForms.GIANT);
		giantForm.setUnlockOnSkillLevel(1);
		giantForm.setFormCombo("dragonminez:giant");
		giantForm.setKeepBaseFormHeadBones(true);
		giantForm.setModelScaling(new Float[]{3.6f, 3.6f, 3.6f});
		giantForm.setStrMultiplier(1.5);
		giantForm.setSkpMultiplier(1.5);
		giantForm.setDefMultiplier(1.4);
		giantForm.setPwrMultiplier(1.5);
		giantForm.setEnergyDrain(0.09);
		giantForm.setAttackSpeed(0.25);
		giantForm.setHairType("base");
		setDefaultMasteryValues(giantForm);
		giantForm.setStackDrainMultiplier(2.0);
		giantForm.setCanAlwaysTransform(true);

		FormConfig.FormData fullPower = new FormConfig.FormData();
		fullPower.setName(NamekianForms.FULLPOWER);
		fullPower.setKeepBaseFormHeadBones(true);
		fullPower.setUnlockOnSkillLevel(2);
		fullPower.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
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
		superNamekian.setCustomModel("namekian_orange");
		superNamekian.setKeepBaseFormHeadBones(true);
		superNamekian.setAuraColor("#7FFF00");
		superNamekian.setHasLightnings(true);
		superNamekian.setLightningColor("#FFFFFF");
		superNamekian.setModelScaling(new Float[]{1.05f, 1.05f, 1.05f});
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
		LogUtil.info(Env.COMMON, "Default Namekian forms created");

		FormConfig namekianLegendaryForms = new FormConfig();
		namekianLegendaryForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		namekianLegendaryForms.setGroupName(NamekianForms.GROUP_LEGENDARYFORMS);
		namekianLegendaryForms.setFormType("legendaryforms");

		FormConfig.FormData evilNamek = new FormConfig.FormData();
		evilNamek.setName(NamekianForms.EVIL_NAMEK);
		evilNamek.setUnlockOnSkillLevel(1);
		evilNamek.setCustomModel("");
		evilNamek.setStrMultiplier(4.25);
		evilNamek.setSkpMultiplier(4.25);
		evilNamek.setDefMultiplier(4.25);
		evilNamek.setPwrMultiplier(4.25);
		evilNamek.setHairType("base");
		setDefaultMasteryValues(evilNamek);
		evilNamek.setStackDrainMultiplier(2.0);
		evilNamek.setCanAlwaysTransform(true);

		FormConfig.FormData evilGiant = new FormConfig.FormData();
		evilGiant.setName(NamekianForms.EVIL_GIANT_NAMEK);
		evilGiant.setUnlockOnSkillLevel(2);
		evilGiant.setCustomModel("");
		evilGiant.setStrMultiplier(4.875);
		evilGiant.setSkpMultiplier(4.875);
		evilGiant.setDefMultiplier(4.875);
		evilGiant.setPwrMultiplier(4.875);
		evilGiant.setSpeedMultiplier(0.75);
		evilGiant.setAttackSpeed(0.85);
		evilGiant.setEnergyDrain(0.25);
		evilGiant.setStaminaDrainMultiplier(1.5);
		evilGiant.setHairType("base");
		setDefaultMasteryValues(evilGiant);
		evilGiant.setStackDrainMultiplier(2.0);

		FormConfig.FormData buffedNamek = new FormConfig.FormData();
		buffedNamek.setName(NamekianForms.BUFFED_NAMEK);
		buffedNamek.setUnlockOnSkillLevel(3);
		buffedNamek.setCustomModel("");
		buffedNamek.setStrMultiplier(5.5);
		buffedNamek.setSkpMultiplier(5.5);
		buffedNamek.setDefMultiplier(5.5);
		buffedNamek.setPwrMultiplier(5.5);
		buffedNamek.setHairType("base");
		setDefaultMasteryValues(buffedNamek);
		buffedNamek.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> namekianLegendaryData = new LinkedHashMap<>();
		namekianLegendaryData.put(NamekianForms.EVIL_NAMEK, evilNamek);
		namekianLegendaryData.put(NamekianForms.EVIL_GIANT_NAMEK, evilGiant);
		namekianLegendaryData.put(NamekianForms.BUFFED_NAMEK, buffedNamek);
		namekianLegendaryForms.setForms(namekianLegendaryData);

		forms.put(NamekianForms.GROUP_LEGENDARYFORMS, namekianLegendaryForms);
		LogUtil.info(Env.COMMON, "Default Namekian legendary forms created");
	}

	private void createFrostDemonForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig frostForms = new FormConfig();
		frostForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		frostForms.setGroupName(FrostDemonForms.GROUP_EVOLUTIONFORMS);
		frostForms.setFormType("superforms");

		FormConfig.FormData second = new FormConfig.FormData();
		second.setName(FrostDemonForms.SECOND_FORM);
		second.setUnlockOnSkillLevel(1);
		second.setCustomModel("");
		second.setKeepBaseFormHeadBones(true);
		second.setModelScaling(new Float[]{1.3f, 1.3f, 1.3f});
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
		third.setModelScaling(new Float[]{1.4f, 1.4f, 1.4f});
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
		finalForm.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
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
		fullPower.setCustomModel("frostdemon_fp");
		fullPower.setModelScaling(new Float[]{1.3f, 1.2f, 1.3f});
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
		fifthForm.setModelScaling(new Float[]{1.4f, 1.3f, 1.4f});
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
		LogUtil.info(Env.COMMON, "Default Frost Demon forms created");

		FormConfig frostLegendaryForms = new FormConfig();
		frostLegendaryForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		frostLegendaryForms.setGroupName(FrostDemonForms.GROUP_LEGENDARYFORMS);
		frostLegendaryForms.setFormType("legendaryforms");

		FormConfig.FormData mecha = new FormConfig.FormData();
		mecha.setName(FrostDemonForms.MECHA);
		mecha.setUnlockOnSkillLevel(1);
		mecha.setCustomModel("");
		mecha.setStrMultiplier(4.25);
		mecha.setSkpMultiplier(4.25);
		mecha.setDefMultiplier(4.25);
		mecha.setPwrMultiplier(4.25);
		mecha.setHairType("base");
		setDefaultMasteryValues(mecha);
		mecha.setStackDrainMultiplier(2.0);
		mecha.setCanAlwaysTransform(true);

		FormConfig.FormData metal = new FormConfig.FormData();
		metal.setName(FrostDemonForms.METAL);
		metal.setUnlockOnSkillLevel(2);
		metal.setCustomModel("");
		metal.setStrMultiplier(4.875);
		metal.setSkpMultiplier(4.875);
		metal.setDefMultiplier(4.875);
		metal.setPwrMultiplier(4.875);
		metal.setHairType("base");
		setDefaultMasteryValues(metal);
		metal.setStackDrainMultiplier(2.0);

		FormConfig.FormData metalCore = new FormConfig.FormData();
		metalCore.setName(FrostDemonForms.METAL_CORE);
		metalCore.setUnlockOnSkillLevel(3);
		metalCore.setCustomModel("");
		metalCore.setStrMultiplier(5.5);
		metalCore.setSkpMultiplier(5.5);
		metalCore.setDefMultiplier(5.5);
		metalCore.setPwrMultiplier(5.5);
		metalCore.setHairType("base");
		setDefaultMasteryValues(metalCore);
		metalCore.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> frostLegendaryData = new LinkedHashMap<>();
		frostLegendaryData.put(FrostDemonForms.MECHA, mecha);
		frostLegendaryData.put(FrostDemonForms.METAL, metal);
		frostLegendaryData.put(FrostDemonForms.METAL_CORE, metalCore);
		frostLegendaryForms.setForms(frostLegendaryData);

		forms.put(FrostDemonForms.GROUP_LEGENDARYFORMS, frostLegendaryForms);
		LogUtil.info(Env.COMMON, "Default Frost Demon legendary forms created");
	}

	private void createMajinForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig majinForms = new FormConfig();
		majinForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		majinForms.setGroupName(MajinForms.GROUP_PUREFORMS);
		majinForms.setFormType("superforms");

		FormConfig.FormData kid = new FormConfig.FormData();
		kid.setName(MajinForms.KID);
		kid.setUnlockOnSkillLevel(1);
		kid.setCustomModel("majin_kid");
		kid.setKeepBaseFormHeadBones(true);
		kid.setModelScaling(new Float[]{0.7f, 0.7f, 0.7f});
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
		evil.setKeepBaseFormHeadBones(true);
		evil.setModelScaling(new Float[]{0.9f, 1.0f, 0.9f});
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

		FormConfig.FormData superForms = new FormConfig.FormData();
		superForms.setName(MajinForms.SUPER);
		superForms.setUnlockOnSkillLevel(3);
		superForms.setKeepBaseFormHeadBones(true);
		superForms.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
		superForms.setStrMultiplier(2.25);
		superForms.setSkpMultiplier(2.25);
		superForms.setDefMultiplier(2.15);
		superForms.setPwrMultiplier(2.25);
		superForms.setHairType("base");
		setDefaultMasteryValues(superForms);
		superForms.setStackDrainMultiplier(2.0);

		FormConfig.FormData ultra = new FormConfig.FormData();
		ultra.setName(MajinForms.ULTRA);
		ultra.setUnlockOnSkillLevel(4);
		ultra.setCustomModel("majin_ultra");
		ultra.setKeepBaseFormHeadBones(true);
		ultra.setModelScaling(new Float[]{1.3f, 1.2f, 1.3f});
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
		majinFormData.put(MajinForms.SUPER, superForms);
		majinFormData.put(MajinForms.ULTRA, ultra);
		majinForms.setForms(majinFormData);

		forms.put(MajinForms.GROUP_PUREFORMS, majinForms);
		LogUtil.info(Env.COMMON, "Default Majin forms created");

		FormConfig majinLegendaryForms = new FormConfig();
		majinLegendaryForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		majinLegendaryForms.setGroupName(MajinForms.GROUP_LEGENDARYFORMS);
		majinLegendaryForms.setFormType("legendaryforms");

		FormConfig.FormData innocence = new FormConfig.FormData();
		innocence.setName(MajinForms.INNOCENCE_DEMON);
		innocence.setUnlockOnSkillLevel(1);
		innocence.setCustomModel("");
		innocence.setStrMultiplier(4.25);
		innocence.setSkpMultiplier(4.25);
		innocence.setDefMultiplier(4.25);
		innocence.setPwrMultiplier(4.25);
		innocence.setHairType("base");
		setDefaultMasteryValues(innocence);
		innocence.setStackDrainMultiplier(2.0);
		innocence.setCanAlwaysTransform(true);

		FormConfig.FormData superDemon = new FormConfig.FormData();
		superDemon.setName(MajinForms.SUPER_DEMON);
		superDemon.setUnlockOnSkillLevel(2);
		superDemon.setCustomModel("");
		superDemon.setStrMultiplier(5.5);
		superDemon.setSkpMultiplier(5.5);
		superDemon.setDefMultiplier(5.5);
		superDemon.setPwrMultiplier(5.5);
		superDemon.setHairType("base");
		setDefaultMasteryValues(superDemon);
		superDemon.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> majinLegendaryData = new LinkedHashMap<>();
		majinLegendaryData.put(MajinForms.INNOCENCE_DEMON, innocence);
		majinLegendaryData.put(MajinForms.SUPER_DEMON, superDemon);
		majinLegendaryForms.setForms(majinLegendaryData);

		forms.put(MajinForms.GROUP_LEGENDARYFORMS, majinLegendaryForms);
		LogUtil.info(Env.COMMON, "Default Majin legendary forms created");
	}

	private void createBioAndroidForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig bioForms = new FormConfig();
		bioForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		bioForms.setGroupName(BioAndroidForms.GROUP_BIOEVOLUTION);
		bioForms.setFormType("superforms");

		FormConfig.FormData semiPerfect = new FormConfig.FormData();
		semiPerfect.setName(BioAndroidForms.SEMI_PERFECT);
		semiPerfect.setUnlockOnSkillLevel(1);
		semiPerfect.setCustomModel("bioandroid_semi");
		semiPerfect.setModelScaling(new Float[]{1.3f, 1.3f, 1.3f});
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
		perfect.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
		perfect.setStrMultiplier(2.25);
		perfect.setSkpMultiplier(2.25);
		perfect.setDefMultiplier(1.85);
		perfect.setPwrMultiplier(2.25);
		perfect.setHairColor("");
		perfect.setEye1Color("#F6A6FF");
		perfect.setEye2Color("#FFFFFF");
		perfect.setBodyColor1("");
		perfect.setBodyColor2("#FFFFFF");
		perfect.setBodyColor3("");
		perfect.setHairType("base");
		setDefaultMasteryValues(perfect);
		perfect.setStackDrainMultiplier(2.0);

		FormConfig.FormData superPerfect = new FormConfig.FormData();
		superPerfect.setName(BioAndroidForms.SUPER_PERFECT);
		superPerfect.setUnlockOnSkillLevel(3);
		superPerfect.setCustomModel("bioandroid_perfect");
		superPerfect.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
		superPerfect.setStrMultiplier(2.85);
		superPerfect.setSkpMultiplier(2.85);
		superPerfect.setDefMultiplier(2.15);
		superPerfect.setPwrMultiplier(2.85);
		superPerfect.setEnergyDrain(0.16);
		superPerfect.setEye1Color("#F6A6FF");
		superPerfect.setEye2Color("#FFFFFF");
		superPerfect.setBodyColor1("");
		superPerfect.setBodyColor2("#FFFFFF");
		superPerfect.setBodyColor3("");
		superPerfect.setAuraColor("#FFFF69");
		superPerfect.setHasLightnings(true);
		superPerfect.setLightningColor("#1AA1C7");
		superPerfect.setHairType("base");
		setDefaultMasteryValues(superPerfect);
		superPerfect.setStackDrainMultiplier(2.0);

		FormConfig.FormData ultraperfect = new FormConfig.FormData();
		ultraperfect.setName(BioAndroidForms.ULTRA_PERFECT);
		ultraperfect.setUnlockOnSkillLevel(4);
		ultraperfect.setCustomModel("bioandroid_ultra");
		ultraperfect.setModelScaling(new Float[]{1.3f, 1.3f, 1.3f});
		ultraperfect.setStrMultiplier(3.25);
		ultraperfect.setSkpMultiplier(3.25);
		ultraperfect.setDefMultiplier(2.0);
		ultraperfect.setPwrMultiplier(3.25);
		ultraperfect.setSpeedMultiplier(0.6);
		ultraperfect.setEnergyDrain(0.28);
		ultraperfect.setStaminaDrainMultiplier(3.5);
		ultraperfect.setAttackSpeed(0.55);
		ultraperfect.setEye1Color("#F6A6FF");
		ultraperfect.setEye2Color("#FFFFFF");
		ultraperfect.setBodyColor1("");
		ultraperfect.setBodyColor2("#FFFFFF");
		ultraperfect.setBodyColor3("");
		ultraperfect.setAuraColor("#FFFF69");
		ultraperfect.setHasLightnings(true);
		ultraperfect.setLightningColor("#1AA1C7");
		ultraperfect.setHairType("base");
		setDefaultMasteryValues(ultraperfect);
		ultraperfect.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> bioFormData = new LinkedHashMap<>();
		bioFormData.put(BioAndroidForms.SEMI_PERFECT, semiPerfect);
		bioFormData.put(BioAndroidForms.PERFECT, perfect);
		bioFormData.put(BioAndroidForms.SUPER_PERFECT, superPerfect);
		bioFormData.put(BioAndroidForms.ULTRA_PERFECT, ultraperfect);
		bioForms.setForms(bioFormData);

		forms.put(BioAndroidForms.GROUP_BIOEVOLUTION, bioForms);
		LogUtil.info(Env.COMMON, "Default Bio Android forms created");

		FormConfig bioLegendaryForms = new FormConfig();
		bioLegendaryForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		bioLegendaryForms.setGroupName(BioAndroidForms.GROUP_LEGENDARYFORMS);
		bioLegendaryForms.setFormType("legendaryforms");

		FormConfig.FormData imperfectMax = new FormConfig.FormData();
		imperfectMax.setName(BioAndroidForms.IMPERFECT_MAX);
		imperfectMax.setUnlockOnSkillLevel(1);
		imperfectMax.setCustomModel("");
		imperfectMax.setStrMultiplier(4.25);
		imperfectMax.setSkpMultiplier(4.25);
		imperfectMax.setDefMultiplier(4.25);
		imperfectMax.setPwrMultiplier(4.25);
		imperfectMax.setHairType("base");
		setDefaultMasteryValues(imperfectMax);
		imperfectMax.setStackDrainMultiplier(2.0);
		imperfectMax.setCanAlwaysTransform(true);

		FormConfig.FormData semiPerfectMax = new FormConfig.FormData();
		semiPerfectMax.setName(BioAndroidForms.SEMI_PERFECT_MAX);
		semiPerfectMax.setUnlockOnSkillLevel(2);
		semiPerfectMax.setCustomModel("");
		semiPerfectMax.setStrMultiplier(4.67);
		semiPerfectMax.setSkpMultiplier(4.67);
		semiPerfectMax.setDefMultiplier(4.67);
		semiPerfectMax.setPwrMultiplier(4.67);
		semiPerfectMax.setHairType("base");
		setDefaultMasteryValues(semiPerfectMax);
		semiPerfectMax.setStackDrainMultiplier(2.0);

		FormConfig.FormData giantMax = new FormConfig.FormData();
		giantMax.setName(BioAndroidForms.GIANT_MAX);
		giantMax.setUnlockOnSkillLevel(3);
		giantMax.setCustomModel("");
		giantMax.setStrMultiplier(5.08);
		giantMax.setSkpMultiplier(5.08);
		giantMax.setDefMultiplier(5.08);
		giantMax.setPwrMultiplier(5.08);
		giantMax.setSpeedMultiplier(0.7);
		giantMax.setAttackSpeed(0.85);
		giantMax.setEnergyDrain(0.25);
		giantMax.setHairType("base");
		setDefaultMasteryValues(giantMax);
		giantMax.setStackDrainMultiplier(2.0);

		FormConfig.FormData perfectMax = new FormConfig.FormData();
		perfectMax.setName(BioAndroidForms.PERFECT_MAX);
		perfectMax.setUnlockOnSkillLevel(4);
		perfectMax.setCustomModel("");
		perfectMax.setStrMultiplier(5.5);
		perfectMax.setSkpMultiplier(5.5);
		perfectMax.setDefMultiplier(5.5);
		perfectMax.setPwrMultiplier(5.5);
		perfectMax.setHairType("base");
		setDefaultMasteryValues(perfectMax);
		perfectMax.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> bioLegendaryData = new LinkedHashMap<>();
		bioLegendaryData.put(BioAndroidForms.IMPERFECT_MAX, imperfectMax);
		bioLegendaryData.put(BioAndroidForms.SEMI_PERFECT_MAX, semiPerfectMax);
		bioLegendaryData.put(BioAndroidForms.GIANT_MAX, giantMax);
		bioLegendaryData.put(BioAndroidForms.PERFECT_MAX, perfectMax);
		bioLegendaryForms.setForms(bioLegendaryData);

		forms.put(BioAndroidForms.GROUP_LEGENDARYFORMS, bioLegendaryForms);
		LogUtil.info(Env.COMMON, "Default Bio Android legendary forms created");
	}
}
