package com.dragonminez.common.config;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.util.lists.*;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class DefaultFormsFactory {
	private void applySequentialMasteryRequisites(String groupName, Map<String, FormConfig.FormData> orderedForms) {
		String previousFormName = null;
		for (FormConfig.FormData form : orderedForms.values()) {
			if (previousFormName != null) {
				form.setFormRequisite(groupName + "." + previousFormName);
				form.setUnlockOnMastery(25.0);
			}
			previousFormName = form.getName();
		}
	}

	private void setDefaultMasteryValues(FormConfig.FormData form) {
		form.setMaxMastery(100.0);
		form.setMasteryPerHitDealt(0.04);
		form.setMasteryPerHitReceived(0.04);
		form.setMaxStatsMultiplier(1.5);
		form.setMaxCostMultiplier(0.75);
		form.setPassiveMasteryEveryFiveSeconds(0.006);
		form.setStackOnMastery(25.0);
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
		x2.setDefMultiplier(1.125);
		x2.setPwrMultiplier(1.1);
		x2.setSpeedMultiplier(1.1);
		x2.setHealthDrain(0.03);
		x2.setAttackSpeed(1.1);
		x2.setTintColor("#FF0000");
		x2.setTintIntensity(0.1);
		x2.setAuraColor("#DB182C");
		x2.setHasLightnings(false);
		x2.setHairType("");
		setDefaultMasteryValues(x2);
		x2.setAuraLayer(1);
		x2.setStackOnMastery(0.0);
		x2.setShareMasteryWith(List.of(StackForms.GROUP_KAIOKEN + "." + StackForms.X3));
		x2.setShareMasteryMultiplier(0.25);
		x2.setStackDrainMultiplier(1.0);
		x2.setAllowFreeTransformOnMastery(0.0);

		FormConfig.FormData x3 = new FormConfig.FormData();
		x3.setName(StackForms.X3);
		x3.setUnlockOnSkillLevel(2);
		x3.setKeepBaseFormHeadBones(true);
		x3.setStrMultiplier(1.2);
		x3.setSkpMultiplier(1.2);
		x3.setDefMultiplier(1.25);
		x3.setPwrMultiplier(1.2);
		x3.setSpeedMultiplier(1.2);
		x3.setAttackSpeed(1.2);
		x3.setHealthDrain(0.06);
		x3.setTintColor("#FF0000");
		x3.setTintIntensity(0.2);
		x3.setAuraColor("#DB182C");
		x3.setHairType("");
		setDefaultMasteryValues(x3);
		x3.setAuraLayer(1);
		x3.setStackOnMastery(0.0);
		x3.setShareMasteryWith(List.of(StackForms.GROUP_KAIOKEN + "." + StackForms.X4));
		x3.setShareMasteryMultiplier(0.25);
		x3.setStackDrainMultiplier(1.0);

		FormConfig.FormData x4 = new FormConfig.FormData();
		x4.setName(StackForms.X4);
		x4.setUnlockOnSkillLevel(3);
		x4.setKeepBaseFormHeadBones(true);
		x4.setStrMultiplier(1.35);
		x4.setSkpMultiplier(1.35);
		x4.setDefMultiplier(1.4375);
		x4.setPwrMultiplier(1.35);
		x4.setSpeedMultiplier(1.35);
		x4.setAttackSpeed(1.35);
		x4.setHealthDrain(0.095);
		x4.setTintColor("#FF0000");
		x4.setTintIntensity(0.3);
		x4.setAuraColor("#DB182C");
		x4.setHairType("");
		setDefaultMasteryValues(x4);
		x4.setAuraLayer(1);
		x4.setStackOnMastery(0.0);
		x4.setShareMasteryWith(List.of(StackForms.GROUP_KAIOKEN + "." + StackForms.X10));
		x4.setShareMasteryMultiplier(0.25);
		x4.setStackDrainMultiplier(1.0);

		FormConfig.FormData x10 = new FormConfig.FormData();
		x10.setName(StackForms.X10);
		x10.setUnlockOnSkillLevel(4);
		x10.setKeepBaseFormHeadBones(true);
		x10.setStrMultiplier(1.5);
		x10.setSkpMultiplier(1.5);
		x10.setDefMultiplier(1.625);
		x10.setPwrMultiplier(1.5);
		x10.setSpeedMultiplier(1.5);
		x10.setHealthDrain(0.11);
		x10.setAttackSpeed(1.5);
		x10.setTintColor("#FF0000");
		x10.setTintIntensity(0.4);
		x10.setAuraColor("#DB182C");
		x10.setHairType("");
		setDefaultMasteryValues(x10);
		x10.setAuraLayer(1);
		x10.setStackOnMastery(0.0);
		x10.setShareMasteryWith(List.of(StackForms.GROUP_KAIOKEN + "." + StackForms.X20));
		x10.setShareMasteryMultiplier(0.25);
		x10.setStackDrainMultiplier(1.0);

		FormConfig.FormData x20 = new FormConfig.FormData();
		x20.setName(StackForms.X20);
		x20.setUnlockOnSkillLevel(5);
		x20.setKeepBaseFormHeadBones(true);
		x20.setStrMultiplier(1.65);
		x20.setSkpMultiplier(1.65);
		x20.setDefMultiplier(1.8125);
		x20.setPwrMultiplier(1.65);
		x20.setSpeedMultiplier(1.65);
		x20.setHealthDrain(0.15);
		x20.setAttackSpeed(1.65);
		x20.setTintColor("#FF0000");
		x20.setTintIntensity(0.5);
		x20.setAuraColor("#DB182C");
		x20.setHairType("");
		setDefaultMasteryValues(x20);
		x20.setAuraLayer(1);
		x20.setStackOnMastery(0.0);
		x20.setShareMasteryWith(List.of(StackForms.GROUP_KAIOKEN + "." + StackForms.X100));
		x20.setShareMasteryMultiplier(0.25);
		x20.setStackDrainMultiplier(1.0);

		FormConfig.FormData x100 = new FormConfig.FormData();
		x100.setName(StackForms.X100);
		x100.setUnlockOnSkillLevel(6);
		x100.setKeepBaseFormHeadBones(true);
		x100.setStrMultiplier(2.0);
		x100.setSkpMultiplier(2.0);
		x100.setDefMultiplier(2.25);
		x100.setPwrMultiplier(2.0);
		x100.setSpeedMultiplier(2.0);
		x100.setHealthDrain(0.20);
		x100.setAttackSpeed(2.0);
		x100.setTintColor("#FF0000");
		x100.setTintIntensity(0.6);
		x100.setAuraColor("#DB182C");
		x100.setHairType("");
		setDefaultMasteryValues(x100);
		x100.setAuraLayer(1);
		x100.setStackOnMastery(0.0);
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
		ultimate.setAuraColor("#FFFFFF");
		ultimate.setKeepBaseFormHeadBones(true);
		ultimate.setCustomModel("");
		ultimate.setStrMultiplier(2.0);
		ultimate.setSkpMultiplier(2.0);
		ultimate.setDefMultiplier(1.1875);
		ultimate.setPwrMultiplier(2.0);
		ultimate.setEnergyDrain(0.0);
		ultimate.setStaminaDrain(0.0);
		ultimate.setHealthDrain(0.0);
		ultimate.setAttackSpeed(1.0);
		ultimate.setHairType("ssj2");
		ultimate.setMaxMastery(0.0);
		ultimate.setMasteryPerHitDealt(0.0);
		ultimate.setMasteryPerHitReceived(0.0);
		ultimate.setMaxStatsMultiplier(1.0);
		ultimate.setPassiveMasteryEveryFiveSeconds(0.0);
		ultimate.setFormStackable(false);
		ultimate.setStackDrainMultiplier(1.0);
		ultimate.setAllowFreeTransformOnMastery(0.0);

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
		sign.setDefMultiplier(1.625);
		sign.setPwrMultiplier(1.5);
		sign.setStaminaDrain(0.03);
		sign.setAuraLayer(1);
		sign.setAuraColor("#E0E0E0");
		sign.setHasLightnings(false);
		sign.setHairType("");
		setDefaultMasteryValues(sign);
		sign.setStackDrainMultiplier(1.0);
		sign.setAllowFreeTransformOnMastery(0.0);

		FormConfig.FormData mastered = new FormConfig.FormData();
		mastered.setName(StackForms.ULTRAINSTINCT_MASTERED);
		mastered.setUnlockOnSkillLevel(2);
		mastered.setStrMultiplier(2.0);
		mastered.setSkpMultiplier(2.0);
		mastered.setDefMultiplier(2.25);
		mastered.setPwrMultiplier(2.0);
		mastered.setStaminaDrain(0.06);
		mastered.setAuraLayer(1);
		mastered.setAuraColor("#E0E0E0");
		mastered.setHairColor("#E0E0E0");
		mastered.setBodyColor2("#E0E0E0");
		mastered.setHairType("");
		setDefaultMasteryValues(mastered);
		mastered.setStackDrainMultiplier(1.0);
		mastered.setAllowFreeTransformOnMastery(0.0);

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
		sign.setDefMultiplier(1.625);
		sign.setPwrMultiplier(1.5);
		sign.setStaminaDrain(0.03);
		sign.setAuraLayer(1);
		sign.setAuraColor("#66023C");
		sign.setHasLightnings(false);
		sign.setHairType("");
		setDefaultMasteryValues(sign);
		sign.setStackDrainMultiplier(1.0);
		sign.setAllowFreeTransformOnMastery(0.0);

		FormConfig.FormData mastered = new FormConfig.FormData();
		mastered.setName(StackForms.ULTRAEGO_MASTERED);
		mastered.setUnlockOnSkillLevel(2);
		mastered.setStrMultiplier(2.0);
		mastered.setSkpMultiplier(2.0);
		mastered.setDefMultiplier(2.25);
		mastered.setPwrMultiplier(2.0);
		mastered.setStaminaDrain(0.06);
		mastered.setAuraLayer(1);
		mastered.setAuraColor("#66023C");
		mastered.setHairColor("#66023C");
		mastered.setBodyColor2("#66023C");
		mastered.setHairType("ssj2");
		setDefaultMasteryValues(mastered);
		mastered.setStackDrainMultiplier(1.0);
		mastered.setAllowFreeTransformOnMastery(0.0);

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
		buffed.setStrMultiplier(1.6);
		buffed.setSkpMultiplier(1.75);
		buffed.setDefMultiplier(1.375);
		buffed.setPwrMultiplier(1.45);
		buffed.setEnergyDrain(0.08);
		buffed.setHairType("base");
		setDefaultMasteryValues(buffed);
		buffed.setStackDrainMultiplier(2.0);
		buffed.setAllowFreeTransformOnMastery(0.0);
		buffed.setIncompatibleWith(List.of(""));

		FormConfig.FormData fullPower = new FormConfig.FormData();
		fullPower.setName(HumanForms.FULLPOWER);
		fullPower.setUnlockOnSkillLevel(2);
		fullPower.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
		fullPower.setStrMultiplier(2.1);
		fullPower.setSkpMultiplier(2.25);
		fullPower.setDefMultiplier(1.8125);
		fullPower.setPwrMultiplier(1.9);
		fullPower.setEnergyDrain(0.16);
		fullPower.setHairType("ssj");
		setDefaultMasteryValues(fullPower);
		fullPower.setStackDrainMultiplier(2.0);
		fullPower.setIncompatibleWith(List.of(""));

		FormConfig.FormData overdrive = new FormConfig.FormData();
		overdrive.setName(HumanForms.OVERDRIVE);
		overdrive.setUnlockOnSkillLevel(3);
		overdrive.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
		overdrive.setStrMultiplier(2.85);
		overdrive.setSkpMultiplier(3.0);
		overdrive.setDefMultiplier(2.4375);
		overdrive.setPwrMultiplier(2.6);
		overdrive.setEnergyDrain(0.34);
		overdrive.setAuraColor("#FFFD99");
		overdrive.setHasLightnings(true);
		overdrive.setLightningColor("#E6F2F5");
		overdrive.setHairType("ssj2");
		setDefaultMasteryValues(overdrive);
		overdrive.setStackDrainMultiplier(2.0);
		overdrive.setIncompatibleWith(List.of(""));

		FormConfig.FormData solaris = new FormConfig.FormData();
		solaris.setName(HumanForms.SOLARIS);
		solaris.setUnlockOnSkillLevel(4);
		solaris.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
		solaris.setStrMultiplier(3.6);
		solaris.setSkpMultiplier(3.75);
		solaris.setDefMultiplier(2.875);
		solaris.setPwrMultiplier(3.25);
		solaris.setEnergyDrain(0.22);
		solaris.setHairType("ssj2");
		setDefaultMasteryValues(solaris);
		solaris.setStackDrainMultiplier(2.0);
		solaris.setIncompatibleWith(List.of(""));

		Map<String, FormConfig.FormData> humanFormData = new LinkedHashMap<>();
		humanFormData.put(HumanForms.BUFFED, buffed);
		humanFormData.put(HumanForms.FULLPOWER, fullPower);
		humanFormData.put(HumanForms.OVERDRIVE, overdrive);
		humanFormData.put(HumanForms.SOLARIS, solaris);
		applySequentialMasteryRequisites(HumanForms.GROUP_SUPERFORMS, humanFormData);
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
		shiyoken.setStrMultiplier(3.4);
		shiyoken.setSkpMultiplier(3.4);
		shiyoken.setDefMultiplier(2.625);
		shiyoken.setPwrMultiplier(3.4);
		shiyoken.setEnergyDrain(0.22);
		shiyoken.setHairType("base");
        shiyoken.setTintColor("#FF0000");
        shiyoken.setTintIntensity(0.25);
        shiyoken.setAuraColor("#A10000");
        shiyoken.setHasLightnings(true);
        shiyoken.setLightningColor("#FF4F4F");
		setDefaultMasteryValues(shiyoken);
		shiyoken.setStackDrainMultiplier(2.0);
		shiyoken.setAllowFreeTransformOnMastery(0.0);
        shiyoken.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});

		FormConfig.FormData shin_shiyoken = new FormConfig.FormData();
		shin_shiyoken.setName(HumanForms.SHIN_SHIYOKEN);
		shin_shiyoken.setUnlockOnSkillLevel(2);
		shin_shiyoken.setCustomModel("buffed");
		shin_shiyoken.setStrMultiplier(4.3);
		shin_shiyoken.setSkpMultiplier(4.3);
		shin_shiyoken.setDefMultiplier(3.25);
		shin_shiyoken.setPwrMultiplier(4.3);
		shin_shiyoken.setEnergyDrain(0.28);
		shin_shiyoken.setHairType("base");
        shin_shiyoken.setTintColor("#FF0000");
        shin_shiyoken.setTintIntensity(0.25);
        shin_shiyoken.setAuraColor("#A10000");
        shin_shiyoken.setHasLightnings(true);
        shin_shiyoken.setLightningColor("#FF4F4F");
		setDefaultMasteryValues(shin_shiyoken);
		shin_shiyoken.setStackDrainMultiplier(2.0);
        shin_shiyoken.setModelScaling(new Float[]{1.3f, 1.3f, 1.3f});

        FormConfig.FormData chou_shiyoken = new FormConfig.FormData();
        FormConfig.FormData.OutlineShaderConfig chou_shiyokenOutline = new FormConfig.FormData.OutlineShaderConfig();
        chou_shiyokenOutline.setEnabled(true);
        chou_shiyokenOutline.setPrimaryColor("#F72D2D");
        chou_shiyokenOutline.setSecondaryColor("#7D0202");
        chou_shiyokenOutline.setOutlineThickness(3.7D);
        chou_shiyoken.setOutlineShader(chou_shiyokenOutline);
        chou_shiyoken.setName(HumanForms.CHOU_SHIYOKEN);
        chou_shiyoken.setUnlockOnSkillLevel(3);
        chou_shiyoken.setCustomModel("4arms");
		chou_shiyoken.setStrMultiplier(5.0);
		chou_shiyoken.setSkpMultiplier(5.0);
		chou_shiyoken.setDefMultiplier(3.625);
		chou_shiyoken.setPwrMultiplier(5.0);
		chou_shiyoken.setEnergyDrain(0.34);
        chou_shiyoken.setHairType("base");
        chou_shiyoken.setTintColor("#FF0000");
        chou_shiyoken.setTintIntensity(0.25);
        chou_shiyoken.setAuraColor("#A10000");
        chou_shiyoken.setHasLightnings(true);
        chou_shiyoken.setLightningColor("#FF4F4F");
        setDefaultMasteryValues(chou_shiyoken);
        chou_shiyoken.setStackDrainMultiplier(2.0);
        chou_shiyoken.setModelScaling(new Float[]{1.5f, 1.5f, 1.5f});

		Map<String, FormConfig.FormData> humanLegendaryData = new LinkedHashMap<>();
		humanLegendaryData.put(HumanForms.SHIYOKEN, shiyoken);
		humanLegendaryData.put(HumanForms.SHIN_SHIYOKEN, shin_shiyoken);
        humanLegendaryData.put(HumanForms.CHOU_SHIYOKEN, chou_shiyoken);
        applySequentialMasteryRequisites(HumanForms.GROUP_LEGENDARYFORMS, humanLegendaryData);
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
		androidBase.setStrMultiplier(2.0);
		androidBase.setSkpMultiplier(2.0);
		androidBase.setDefMultiplier(1.75);
		androidBase.setPwrMultiplier(2.3);
		androidBase.setHairType("base");
		setDefaultMasteryValues(androidBase);
		androidBase.setStackDrainMultiplier(2.0);
		androidBase.setAllowFreeTransformOnMastery(0.0);

		FormConfig.FormData superAndroid = new FormConfig.FormData();
		superAndroid.setName(HumanForms.SUPER_ANDROID);
		superAndroid.setUnlockOnSkillLevel(1);
		superAndroid.setCustomModel("");
		superAndroid.setModelScaling(new Float[]{1.05f, 1.05f, 1.05f});
		superAndroid.setStrMultiplier(2.9);
		superAndroid.setSkpMultiplier(2.9);
		superAndroid.setDefMultiplier(2.375);
		superAndroid.setPwrMultiplier(3.3);
		superAndroid.setHairType("ssj");
		setDefaultMasteryValues(superAndroid);
		superAndroid.setStackDrainMultiplier(2.0);

		FormConfig.FormData fusedAndroid = new FormConfig.FormData();
		fusedAndroid.setName(HumanForms.FUSED_ANDROID);
		fusedAndroid.setUnlockOnSkillLevel(2);
		fusedAndroid.setCustomModel("buffed");
		fusedAndroid.setModelScaling(new Float[]{1.4f, 1.3f, 1.4f});
		fusedAndroid.setStrMultiplier(3.7);
		fusedAndroid.setSkpMultiplier(3.7);
		fusedAndroid.setDefMultiplier(3.0);
		fusedAndroid.setPwrMultiplier(4.2);
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
		applySequentialMasteryRequisites(HumanForms.GROUP_ANDROIDFORMS, androidFormData);
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
		oozaru.setTransformationAnimation("transf.ozaru");
		oozaru.setModelScaling(new Float[]{3.8f, 3.8f, 3.8f});
		oozaru.setStrMultiplier(1.2);
		oozaru.setSkpMultiplier(1.2);
		oozaru.setDefMultiplier(1.125);
		oozaru.setPwrMultiplier(1.2);
		oozaru.setSpeedMultiplier(0.8);
		oozaru.setEnergyDrain(0.05);
		oozaru.setStaminaDrainMultiplier(1.2);
		oozaru.setAttackSpeed(0.25);
		oozaru.setHairType("base");
		setDefaultMasteryValues(oozaru);
		oozaru.setStackDrainMultiplier(2.0);
		oozaru.setAllowFreeTransformOnMastery(0.0);
		oozaru.setIncompatibleWith(List.of(""));

		FormConfig.FormData goldenOozaru = new FormConfig.FormData();
		goldenOozaru.setName(SaiyanForms.GOLDEN_OOZARU);
		goldenOozaru.setUnlockOnSkillLevel(7);
		goldenOozaru.setFormCombo("dragonminez:giant");
		goldenOozaru.setCustomModel("oozaru");
		goldenOozaru.setTransformationAnimation("transf.ozaru");
		goldenOozaru.setHairColor("#FFD700");
		goldenOozaru.setAuraColor("#FFD700");
		goldenOozaru.setBodyColor2("#FFD700");
		goldenOozaru.setModelScaling(new Float[]{3.8f, 3.8f, 3.8f});
		goldenOozaru.setStrMultiplier(2.0);
		goldenOozaru.setSkpMultiplier(2.0);
		goldenOozaru.setDefMultiplier(2.125);
		goldenOozaru.setPwrMultiplier(2.0);
		goldenOozaru.setSpeedMultiplier(0.85);
		goldenOozaru.setEnergyDrain(0.24);
		goldenOozaru.setStaminaDrainMultiplier(1.3);
		goldenOozaru.setAttackSpeed(0.25);
		goldenOozaru.setHairType("base");
		setDefaultMasteryValues(goldenOozaru);
		goldenOozaru.setStackDrainMultiplier(2.0);
		goldenOozaru.setIncompatibleWith(List.of(""));

		FormConfig.FormData ssj4gt = new FormConfig.FormData();
		ssj4gt.setName(SaiyanForms.SUPER_SAIYAN_4);
		ssj4gt.setCustomModel("ssj4gt");
		ssj4gt.setTransformationAnimation("transf.ozaru");
		ssj4gt.setUnlockOnSkillLevel(8);
		ssj4gt.setHairColor("");
		ssj4gt.setBodyColor2("#9d1e31");
		ssj4gt.setEye1Color("#FFD700");
		ssj4gt.setEye2Color("#FFD700");
		ssj4gt.setAuraColor("#FFD700");
		ssj4gt.setModelScaling(new Float[]{0.96f, 0.96f, 0.96f});
		ssj4gt.setStrMultiplier(3.75);
		ssj4gt.setSkpMultiplier(3.75);
		ssj4gt.setDefMultiplier(2.875);
		ssj4gt.setPwrMultiplier(3.75);
		ssj4gt.setEnergyDrain(0.24);
		ssj4gt.setHairType("base");
		ssj4gt.setForcedHairCode("DMZ1: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");
		setDefaultMasteryValues(ssj4gt);
		ssj4gt.setStackDrainMultiplier(2.0);
		ssj4gt.setAllowFreeTransformOnMastery(50.0);
		ssj4gt.setIncompatibleWith(List.of(""));

		Map<String, FormConfig.FormData> oozaruFormData = new LinkedHashMap<>();
		oozaruFormData.put(SaiyanForms.OOZARU, oozaru);
		oozaruFormData.put(SaiyanForms.GOLDEN_OOZARU, goldenOozaru);
		oozaruFormData.put(SaiyanForms.SUPER_SAIYAN_4, ssj4gt);
		applySequentialMasteryRequisites(SaiyanForms.GROUP_OOZARU, oozaruFormData);
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
		ssj1.setDefMultiplier(1.3125);
		ssj1.setPwrMultiplier(1.5);
		ssj1.setEnergyDrain(0.08);
		ssj1.setHairType("ssj");
		setDefaultMasteryValues(ssj1);
		ssj1.setStackDrainMultiplier(2.0);
		ssj1.setAllowFreeTransformOnMastery(0.0);
		ssj1.setIncompatibleWith(List.of(""));

		FormConfig.FormData ssg2 = new FormConfig.FormData();
		ssg2.setName(SaiyanForms.SUPER_SAIYAN_GRADE_2);
		ssg2.setTransformationAnimation("transf.ssg2");
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
		ssg2.setDefMultiplier(1.5);
		ssg2.setPwrMultiplier(1.75);
		ssg2.setSpeedMultiplier(0.9);
		ssg2.setEnergyDrain(0.12);
		ssg2.setStaminaDrainMultiplier(1.3);
		ssg2.setHairType("ssj");
		setDefaultMasteryValues(ssg2);
		ssg2.setStackDrainMultiplier(2.0);
		ssg2.setIncompatibleWith(List.of(""));

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
		ssg3.setDefMultiplier(2.0);
		ssg3.setPwrMultiplier(2.75);
		ssg3.setSpeedMultiplier(0.7);
		ssg3.setEnergyDrain(0.48);
		ssg3.setStaminaDrainMultiplier(3.5);
		ssg3.setAttackSpeed(0.75);
		ssg3.setHairType("ssj");
		setDefaultMasteryValues(ssg3);
		ssg3.setStackDrainMultiplier(2.0);
		ssg3.setIncompatibleWith(List.of(""));

		Map<String, FormConfig.FormData> ssGradeForms = new LinkedHashMap<>();
		ssGradeForms.put(SaiyanForms.SUPER_SAIYAN, ssj1);
		ssGradeForms.put(SaiyanForms.SUPER_SAIYAN_GRADE_2, ssg2);
		ssGradeForms.put(SaiyanForms.SUPER_SAIYAN_GRADE_3, ssg3);
		applySequentialMasteryRequisites(SaiyanForms.GROUP_SSGRADES, ssGradeForms);
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
		ssj1Mastered.setDefMultiplier(1.4375);
		ssj1Mastered.setPwrMultiplier(1.75);
		ssj1Mastered.setEnergyDrain(0.03);
		ssj1Mastered.setHairType("ssj");
		setDefaultMasteryValues(ssj1Mastered);
		ssj1Mastered.setStackDrainMultiplier(2.0);
		ssj1Mastered.setAllowFreeTransformOnMastery(0.0);
		ssj1Mastered.setIncompatibleWith(List.of(""));

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
		ssj2.setDefMultiplier(1.8125);
		ssj2.setPwrMultiplier(2.25);
		ssj2.setEnergyDrain(0.16);
		ssj2.setHairType("ssj2");
		setDefaultMasteryValues(ssj2);
		ssj2.setStackDrainMultiplier(2.0);
		ssj2.setIncompatibleWith(List.of(""));

		FormConfig.FormData ssj3 = new FormConfig.FormData();
		ssj3.setName(SaiyanForms.SUPER_SAIYAN_3);
		ssj3.setUnlockOnSkillLevel(6);
		ssj3.setTransformationAnimation("transf.ssj3");
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
		ssj3.setDefMultiplier(2.4375);
		ssj3.setPwrMultiplier(3.0);
		ssj3.setEnergyDrain(0.34);
		ssj3.setHairType("ssj3");
		setDefaultMasteryValues(ssj3);
		ssj3.setStackDrainMultiplier(2.0);
		ssj3.setIncompatibleWith(List.of(""));

		FormConfig.FormData ssj4d = new FormConfig.FormData();
        FormConfig.FormData.OutlineShaderConfig ssj4dOutline = new FormConfig.FormData.OutlineShaderConfig();
        ssj4dOutline.setEnabled(true);
        ssj4dOutline.setPrimaryColor("#F4FF8A");
        ssj4dOutline.setSecondaryColor("#8AFFFF");
        ssj4dOutline.setOutlineThickness(3.7D);
        ssj4d.setOutlineShader(ssj4dOutline);
        ssj4d.setName(SaiyanForms.SUPER_SAIYAN_4);
		ssj4d.setCustomModel("ssj4d");
		ssj4d.setTransformationAnimation("transf.daima");
		ssj4d.setUnlockOnSkillLevel(8);
		ssj4d.setHairColor("#83073F");
		ssj4d.setBodyColor2("#83073F");
		ssj4d.setEye1Color("#83073F");
		ssj4d.setEye2Color("#83073F");
		ssj4d.setAuraColor("#FFD633");
        ssj4d.setModelScaling(new Float[]{1.2f, 1.2f, 1.2f});
        ssj4d.setStrMultiplier(3.75);
		ssj4d.setSkpMultiplier(3.75);
		ssj4d.setDefMultiplier(2.875);
		ssj4d.setPwrMultiplier(3.75);
		ssj4d.setEnergyDrain(0.24);
		ssj4d.setHairType("base");
		ssj4d.setForcedHairCode("DMZ1:B1TLAOh8hYt61Bp01ing62J1qdSUoM58w9EprdKkP5gOBETJixjMZIaeXqxtpOs9Dr7CsTXfACPhx0OrANlywSLBr6m25mbR9m0xWBlxYscAs1umEkAhkHRQyaWzS8ZGa4Qtr8JWaN7uec4x1DFhUqK5raPsrbqEcIik49QMEuY6bU6NBC7xCdfGlMZQzB0CDRwHGAFPHJ8ynlhlLMzJExgwT2Xt325IfeYMZNQI3m5Jc8ojLec2xIrKZBKO7z5L4aSwYMDIteuP7WBqoh7JnuMmw6bnlRIuKF0MJv1UZBFr93e4vIRiisqaloDW6ctk7lOAuVFjqyGSzK8JWxO0RLqQRtYRxzoDtTj6T3U43JSDHmlITkIGGXOfvsfCrIVLrHCBmqOufXMNYbylpJMu3G61XsbFzndDyC2uT5kKZabPoIL0rujQ9nNeS7HysN50nggDlMiqp2ly08VDzQSxrni9v0LFF4e2nHSaQpc7H2z05oHZWuMNj4t9FfIxMZvisZlnCcSnGDmtGhV8nKEqogARK6nVGk26nQvsosAW61fMnuPeLTrXBBRyg0Yk2BIWAWagibeMvr1QR3dkWoJLGBHsoAAC8iaz01bsQba3Nn7SyqllNTAbI2LL9cvrkI6bqqRsuJXa9QQZ9xlBMgqyJ2rB5AsBaMp6gHWAGAJvjQxNU0sXHZcPiDxeRGTFUXClBIqJOypF08ItGDtyfkCvIYMS9Jxr7InRipRrRwjZOgt23y3IvTDf1lyGFF1XiEqcBdVM8tGOhBVXn6XsHraZnSzJHGWd24in5Di9Oq7kRRmzNsC6yTNnW6Q8am9qhaht7Du26DOow0iXLxao8oeuUAJNiXENUQIUrZuafxutITtfSGLWvvqVejxANpVQk5VBmG8kEGc6GOLIlVQmEF5pA94e3doXSSf7e9mKGT3xQe4aULgGl8yrGFlON0VR1UFB99wmtWkjTvLZwZRAj0JBA2329U0nEwbI4qQ08mRoGZWFJChwoAQqBh7yBvWICbdumlJmuRwMd2mjg2iub6VCaOg4lOlsyESyprDryi78YYSDk43Sz9apzc1Olvo1q3xV22JQ6HgQ6Ii7Nwdmllim2P1AXSRaJUJxG2EFClFXY4UPL2nsXoBcMx3OUMZQlAoShVob8KIhKUHXrLsfSqpqVNhH6r0cBDzqJlmECqcorkgLWwIVNY4UjmCALAn34QtVV40I73cLDUS2PckKSivSFKetPbjTE0pJmk2b0blsxSZ329OmuFQtVzlFoHkFhGdh7Y9QreTTMZFFQlinNwE88DAXthlzsQTUBSON39KPtODrenCzGvWmB9lop4nI2GChGFFvln7yaJ4NZCvXwoCvfsB3gFDGHcGmaMyrODgb7382P3LSVo8rQPEbGQ3MO2FAYf1gwsLFaYqGUPKaG7UHTuTStJOq4WqlFkhMLxW5i5m76KMWA6lvw8gdIlV380Of3SgyjfY8ve9LscFI9geUpsEWDI4U5LGgn1PdCGee3LCDPkOFfuwglhlSzFDk6N1082VaCTjJDNMz75yjwIUkXyteHsdx8skteOSWz8xIfl83VtKDSWm3n43CSJ14dhn7GVTRZsk7VJiDoGRSBzxnEOBi63d0zF3G82zTUfx15f8sRvEbsby0rV2Nw9zb1PZYcz6VMATxgb01AfDyRgvo3KMpBGlzYcOb3lTemvZCfAM1LPUo7MG3YBWWOQnv5Ct6GyvN1VjiAkJ6FTa1gQoqaQb66pcM6WM1UJa1bV2RNQllqZJkoV49NURWpmWMlQlt3sTxRhxU8XtwZ4jxDlg1XZzwpjZd07stsOypKTOSN5ABIvD9DyvJUnoBfa7pM4uahk8sxDurM74NSlKwtTnKYl9lfC9070p8b3hCJY2kLZkyhVwbGJLRZ3Zp2bq2BQWaqPzCgEsqlfE9AHxscwSwOttL4IEg4IqVsn5VQlgU5BdRYGtQDBDG2ZTgEuEZv1yIy3nikfWwP2HVZn107sGvVq5IzBlglzJ3WyhBtHIzFl08Psws3T4HA9OEPMS0RGX6wIzOYlE5rcYWy3f11JuvXT5bZq1NKKPiRDYFvyNu9PXLaoNMLPDTieMATC7tn46FOzo11w6LkLlJzN9S8Jus1eFumLVefL6en2foxZr1sAzig3Z5p8H9eiOcWYUEBS7tGt56uMtlr4NptHRZ1t7Vg");
        ssj4d.setLightningColor("#82C9FF");
        ssj4d.setHasLightnings(true);
        ssj4d.setStackDrainMultiplier(2.0);
        setDefaultMasteryValues(ssj4d);
        ssj4d.setAllowFreeTransformOnMastery(50.0);
		ssj4d.setIncompatibleWith(List.of(""));

		Map<String, FormConfig.FormData> superSaiyanForms = new LinkedHashMap<>();
		superSaiyanForms.put(SaiyanForms.SUPER_SAIYAN_MASTERED, ssj1Mastered);
		superSaiyanForms.put(SaiyanForms.SUPER_SAIYAN_2, ssj2);
		superSaiyanForms.put(SaiyanForms.SUPER_SAIYAN_3, ssj3);
		superSaiyanForms.put(SaiyanForms.SUPER_SAIYAN_4, ssj4d);
		applySequentialMasteryRequisites(SaiyanForms.GROUP_SUPERSAIYAN, superSaiyanForms);
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
        FormConfig.FormData.OutlineShaderConfig ikariOutline = new FormConfig.FormData.OutlineShaderConfig();
        ikariOutline.setEnabled(true);
        ikariOutline.setPrimaryColor("#7FFF7D");
        ikariOutline.setSecondaryColor("#F4FF7D");
        ikariOutline.setOutlineThickness(3.5D);
        ikari.setOutlineShader(ikariOutline);
		ikari.setName(SaiyanForms.IKARI);
		ikari.setUnlockOnSkillLevel(1);
		ikari.setTransformationAnimation("transf.berserker");
		ikari.setCustomModel("buffed");
		ikari.setEye1Color("#FFD700");
		ikari.setEye2Color("#FFD700");
		ikari.setAuraColor("#40FF00");
		ikari.setStrMultiplier(3.4);
		ikari.setSkpMultiplier(3.4);
		ikari.setDefMultiplier(2.625);
		ikari.setPwrMultiplier(3.4);
		ikari.setEnergyDrain(0.1);
        ikari.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
        ikari.setHairType("ssj");
		setDefaultMasteryValues(ikari);
		ikari.setStackDrainMultiplier(2.0);
		ikari.setAllowFreeTransformOnMastery(0.0);

		FormConfig.FormData ssjHybrid = new FormConfig.FormData();
		ssjHybrid.setName(SaiyanForms.SSJ_HYBRID);
		ssjHybrid.setUnlockOnSkillLevel(2);
		ssjHybrid.setTransformationAnimation("transf.berserker");
		ssjHybrid.setCustomModel("buffed");
		ssjHybrid.setBodyColor2("#FFE89E");
		ssjHybrid.setHairColor("#FFE89E");
		ssjHybrid.setEye1Color("#FFFFFF");
		ssjHybrid.setEye2Color("#FFFFFF");
		ssjHybrid.setAuraColor("#40FF00");
		ssjHybrid.setHasLightnings(true);
		ssjHybrid.setLightningColor("#40FF00");
		ssjHybrid.setStrMultiplier(4.3);
		ssjHybrid.setSkpMultiplier(4.3);
		ssjHybrid.setDefMultiplier(3.25);
		ssjHybrid.setPwrMultiplier(4.3);
		ssjHybrid.setEnergyDrain(0.16);
        ssjHybrid.setModelScaling(new Float[]{1.15f, 1.15f, 1.15f});
        ssjHybrid.setHairType("ssj");
		setDefaultMasteryValues(ssjHybrid);
		ssjHybrid.setStackDrainMultiplier(2.0);

		FormConfig.FormData ssjFullPower = new FormConfig.FormData();
		ssjFullPower.setName(SaiyanForms.SSJ_FULL_POWER);
		ssjFullPower.setUnlockOnSkillLevel(3);
		ssjFullPower.setTransformationAnimation("transf.berserker");
		ssjFullPower.setCustomModel("buffed");
		ssjFullPower.setBodyColor2("#9EFE53");
		ssjFullPower.setHairColor("#9EFE53");
		ssjFullPower.setEye1Color("#FFFFFF");
		ssjFullPower.setEye2Color("#FFFFFF");
		ssjFullPower.setAuraColor("#40FF00");
		ssjFullPower.setHasLightnings(true);
		ssjFullPower.setLightningColor("#40FF00");
		ssjFullPower.setStrMultiplier(5.0);
		ssjFullPower.setSkpMultiplier(5.0);
		ssjFullPower.setDefMultiplier(3.625);
		ssjFullPower.setPwrMultiplier(5.0);
		ssjFullPower.setEnergyDrain(0.26);
        ssjFullPower.setModelScaling(new Float[]{1.4f, 1.3f, 1.4f});
        ssjFullPower.setHairType("ssj2");
		setDefaultMasteryValues(ssjFullPower);
		ssjFullPower.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> saiyanLegendaryData = new LinkedHashMap<>();
		saiyanLegendaryData.put(SaiyanForms.IKARI, ikari);
		saiyanLegendaryData.put(SaiyanForms.SSJ_HYBRID, ssjHybrid);
		saiyanLegendaryData.put(SaiyanForms.SSJ_FULL_POWER, ssjFullPower);
		applySequentialMasteryRequisites(SaiyanForms.GROUP_LEGENDARYFORMS, saiyanLegendaryData);
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
		giantForm.setStrMultiplier(2.0);
		giantForm.setSkpMultiplier(2.0);
		giantForm.setDefMultiplier(1.75);
		giantForm.setPwrMultiplier(2.0);
		giantForm.setEnergyDrain(0.09);
		giantForm.setAttackSpeed(0.25);
		giantForm.setHairType("base");
		setDefaultMasteryValues(giantForm);
		giantForm.setStackDrainMultiplier(2.0);
		giantForm.setAllowFreeTransformOnMastery(0.0);
		giantForm.setIncompatibleWith(List.of(""));

		FormConfig.FormData fullPower = new FormConfig.FormData();
		fullPower.setName(NamekianForms.FULLPOWER);
		fullPower.setKeepBaseFormHeadBones(true);
		fullPower.setUnlockOnSkillLevel(2);
		fullPower.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
		fullPower.setStrMultiplier(2.85);
		fullPower.setSkpMultiplier(2.85);
		fullPower.setDefMultiplier(2.3125);
		fullPower.setPwrMultiplier(2.85);
		fullPower.setEnergyDrain(0.18);
		fullPower.setHairType("base");
		setDefaultMasteryValues(fullPower);
		fullPower.setStackDrainMultiplier(2.0);
		fullPower.setIncompatibleWith(List.of(""));

		FormConfig.FormData superNamekian = new FormConfig.FormData();
		superNamekian.setName(NamekianForms.SUPER_NAMEKIAN);
		superNamekian.setUnlockOnSkillLevel(3);
		superNamekian.setCustomModel("namekian_buffed");
		superNamekian.setKeepBaseFormHeadBones(true);
		superNamekian.setAuraColor("#7FFF00");
		superNamekian.setHasLightnings(true);
		superNamekian.setLightningColor("#FFFFFF");
		superNamekian.setModelScaling(new Float[]{1.05f, 1.05f, 1.05f});
		superNamekian.setStrMultiplier(3.75);
		superNamekian.setSkpMultiplier(3.75);
		superNamekian.setDefMultiplier(2.875);
		superNamekian.setPwrMultiplier(3.75);
		superNamekian.setEnergyDrain(0.27);
		superNamekian.setHairType("base");
		setDefaultMasteryValues(superNamekian);
		superNamekian.setStackDrainMultiplier(2.0);
		superNamekian.setIncompatibleWith(List.of(""));

		Map<String, FormConfig.FormData> namekianFormData = new LinkedHashMap<>();
		namekianFormData.put(NamekianForms.GIANT, giantForm);
		namekianFormData.put(NamekianForms.FULLPOWER, fullPower);
		namekianFormData.put(NamekianForms.SUPER_NAMEKIAN, superNamekian);
		applySequentialMasteryRequisites(NamekianForms.GROUP_SUPERFORMS, namekianFormData);
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
		evilNamek.setStrMultiplier(3.4);
		evilNamek.setSkpMultiplier(3.4);
		evilNamek.setDefMultiplier(2.625);
		evilNamek.setPwrMultiplier(3.4);
		evilNamek.setEnergyDrain(0.18);
        evilNamek.setAuraColor("#570B0B");
        evilNamek.setEye1Color("#FFFFFF");
        evilNamek.setEye2Color("#FFFFFF");
        evilNamek.setBodyColor1("#386327");
        evilNamek.setHasLightnings(true);
        evilNamek.setLightningColor("#D9180F");
        evilNamek.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
        evilNamek.setHairType("base");
        evilNamek.setKeepBaseFormHeadBones(true);
		setDefaultMasteryValues(evilNamek);
		evilNamek.setStackDrainMultiplier(2.0);
		evilNamek.setAllowFreeTransformOnMastery(0.0);

		FormConfig.FormData evilGiant = new FormConfig.FormData();
		evilGiant.setName(NamekianForms.EVIL_GIANT_NAMEK);
		evilGiant.setUnlockOnSkillLevel(2);
		evilGiant.setCustomModel("namekian_buffed");
        evilGiant.setFormCombo("dragonminez:giant");
		evilGiant.setStrMultiplier(4.3);
		evilGiant.setSkpMultiplier(4.3);
		evilGiant.setDefMultiplier(3.25);
		evilGiant.setPwrMultiplier(4.3);
		evilGiant.setSpeedMultiplier(0.75);
		evilGiant.setEnergyDrain(0.27);
        evilGiant.setAuraColor("#570B0B");
        evilGiant.setEye1Color("#9E092F");
        evilGiant.setEye2Color("#9E092F");
        evilGiant.setBodyColor1("#274D18");
        evilGiant.setModelScaling(new Float[]{3.8f, 3.8f, 3.8f});
        evilGiant.setAttackSpeed(0.25);
		evilGiant.setEnergyDrain(0.25);
		evilGiant.setStaminaDrainMultiplier(1.5);
		evilGiant.setHairType("base");
        evilGiant.setKeepBaseFormHeadBones(true);
        setDefaultMasteryValues(evilGiant);
		evilGiant.setStackDrainMultiplier(2.0);

		FormConfig.FormData buffedNamek = new FormConfig.FormData();
        FormConfig.FormData.OutlineShaderConfig buffedNamekOutline = new FormConfig.FormData.OutlineShaderConfig();
        buffedNamekOutline.setEnabled(true);
        buffedNamekOutline.setPrimaryColor("#9E2F2F");
        buffedNamekOutline.setSecondaryColor("#470909");
        buffedNamekOutline.setOutlineThickness(3.5D);
        buffedNamek.setOutlineShader(buffedNamekOutline);
		buffedNamek.setName(NamekianForms.BUFFED_NAMEK);
		buffedNamek.setUnlockOnSkillLevel(3);
		buffedNamek.setCustomModel("namekian_buffed");
		buffedNamek.setStrMultiplier(5.0);
		buffedNamek.setSkpMultiplier(5.0);
		buffedNamek.setDefMultiplier(3.625);
		buffedNamek.setPwrMultiplier(5.0);
		buffedNamek.setEnergyDrain(0.24);
        buffedNamek.setModelScaling(new Float[]{1.3f, 1.3f, 1.3f});
        buffedNamek.setAuraColor("#570B0B");
        buffedNamek.setEye1Color("#9E092F");
        buffedNamek.setEye2Color("#9E092F");
        buffedNamek.setBodyColor1("#2C6914");
        buffedNamek.setHasLightnings(true);
        buffedNamek.setLightningColor("#D9180F");
        buffedNamek.setHairType("base");
        buffedNamek.setKeepBaseFormHeadBones(true);
        setDefaultMasteryValues(buffedNamek);
		buffedNamek.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> namekianLegendaryData = new LinkedHashMap<>();
		namekianLegendaryData.put(NamekianForms.EVIL_NAMEK, evilNamek);
		namekianLegendaryData.put(NamekianForms.EVIL_GIANT_NAMEK, evilGiant);
		namekianLegendaryData.put(NamekianForms.BUFFED_NAMEK, buffedNamek);
		applySequentialMasteryRequisites(NamekianForms.GROUP_LEGENDARYFORMS, namekianLegendaryData);
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
		second.setTransformationAnimation("transf.freezer");
		second.setCustomModel("");
		second.setKeepBaseFormHeadBones(true);
		second.setModelScaling(new Float[]{1.3f, 1.3f, 1.3f});
		second.setStrMultiplier(1.65);
		second.setSkpMultiplier(1.65);
		second.setDefMultiplier(1.375);
		second.setPwrMultiplier(1.65);
		second.setHairType("base");
		setDefaultMasteryValues(second);
		second.setStackDrainMultiplier(2.0);
		second.setAllowFreeTransformOnMastery(0.0);
		second.setIncompatibleWith(List.of(""));

		FormConfig.FormData third = new FormConfig.FormData();
		third.setName(FrostDemonForms.THIRD_FORM);
		third.setUnlockOnSkillLevel(2);
		third.setTransformationAnimation("transf.freezer");
		third.setCustomModel("frostdemon_third");
		third.setModelScaling(new Float[]{1.4f, 1.4f, 1.4f});
		third.setStrMultiplier(2.1);
		third.setSkpMultiplier(2.1);
		third.setDefMultiplier(1.8125);
		third.setPwrMultiplier(2.1);
		third.setHairType("base");
		setDefaultMasteryValues(third);
		third.setStackDrainMultiplier(2.0);
		third.setIncompatibleWith(List.of(""));

		FormConfig.FormData finalForm = new FormConfig.FormData();
		finalForm.setName(FrostDemonForms.FINAL_FORM);
		finalForm.setUnlockOnSkillLevel(3);
		second.setTransformationAnimation("transf.freezer2");
		finalForm.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
		finalForm.setStrMultiplier(2.6);
		finalForm.setSkpMultiplier(2.6);
		finalForm.setDefMultiplier(2.1875);
		finalForm.setPwrMultiplier(2.6);
		finalForm.setHairType("base");
		setDefaultMasteryValues(finalForm);
		finalForm.setStackDrainMultiplier(2.0);
		finalForm.setIncompatibleWith(List.of(""));

		FormConfig.FormData fullPower = new FormConfig.FormData();
		fullPower.setName(FrostDemonForms.FULLPOWER);
		fullPower.setUnlockOnSkillLevel(4);
		second.setTransformationAnimation("transf.freezer2");
		fullPower.setCustomModel("frostdemon_fp");
		fullPower.setModelScaling(new Float[]{1.3f, 1.2f, 1.3f});
		fullPower.setStrMultiplier(3.15);
		fullPower.setSkpMultiplier(3.15);
		fullPower.setDefMultiplier(2.5);
		fullPower.setPwrMultiplier(3.15);
		fullPower.setEnergyDrain(0.22);
		fullPower.setStaminaDrainMultiplier(2.5);
		fullPower.setAttackSpeed(0.75);
		fullPower.setLightningColor("#F02B16");
		fullPower.setHairType("base");
		setDefaultMasteryValues(fullPower);
		fullPower.setStackDrainMultiplier(2.0);
		fullPower.setIncompatibleWith(List.of(""));

		FormConfig.FormData fifthForm = new FormConfig.FormData();
		fifthForm.setName(FrostDemonForms.FIFTH_FORM);
		fifthForm.setUnlockOnSkillLevel(5);
		second.setTransformationAnimation("transf.freezer");
		fifthForm.setCustomModel("frostdemon_fifth");
		fifthForm.setModelScaling(new Float[]{1.4f, 1.3f, 1.4f});
		fifthForm.setStrMultiplier(3.9);
		fifthForm.setSkpMultiplier(3.9);
		fifthForm.setDefMultiplier(3.0);
		fifthForm.setPwrMultiplier(3.9);
		fifthForm.setEnergyDrain(0.28);
		fifthForm.setEye1Color("#D91E1E");
		fifthForm.setEye2Color("#D91E1E");
		fifthForm.setHasLightnings(true);
		fifthForm.setLightningColor("#F02B16");
		fifthForm.setHairType("base");
		setDefaultMasteryValues(fifthForm);
		fifthForm.setStackDrainMultiplier(2.0);
		fifthForm.setIncompatibleWith(List.of(""));

		Map<String, FormConfig.FormData> frostFormData = new LinkedHashMap<>();
		frostFormData.put(FrostDemonForms.SECOND_FORM, second);
		frostFormData.put(FrostDemonForms.THIRD_FORM, third);
		frostFormData.put(FrostDemonForms.FINAL_FORM, finalForm);
		frostFormData.put(FrostDemonForms.FULLPOWER, fullPower);
		frostFormData.put(FrostDemonForms.FIFTH_FORM, fifthForm);
		applySequentialMasteryRequisites(FrostDemonForms.GROUP_EVOLUTIONFORMS, frostFormData);
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
		mecha.setCustomModel("frostdemon_mecha");
		mecha.setStrMultiplier(3.5);
		mecha.setSkpMultiplier(3.5);
		mecha.setDefMultiplier(2.6875);
		mecha.setPwrMultiplier(3.5);
		mecha.setHairType("base");
        mecha.setHasLightnings(true);
        mecha.setLightningColor("#FF6052");
		setDefaultMasteryValues(mecha);
		mecha.setStackDrainMultiplier(2.0);
		mecha.setAllowFreeTransformOnMastery(0.0);

		FormConfig.FormData metal = new FormConfig.FormData();
		metal.setName(FrostDemonForms.METAL);
		metal.setUnlockOnSkillLevel(2);
		metal.setCustomModel("frostdemon_fp");
		metal.setStrMultiplier(4.4);
		metal.setSkpMultiplier(4.4);
		metal.setDefMultiplier(3.3125);
		metal.setPwrMultiplier(4.4);
		metal.setEnergyDrain(-0.05);
        metal.setBodyColor1("#B8FFF0");
        metal.setBodyColor2("#CCFDFF");
        metal.setBodyColor3("#B8FFF0");
        metal.setHairColor("#B8FFF0");
        metal.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
        metal.setHasLightnings(true);
        metal.setLightningColor("#B5FDFF");
        metal.setHairType("base");
		setDefaultMasteryValues(metal);
		metal.setStackDrainMultiplier(2.0);

		FormConfig.FormData metalCore = new FormConfig.FormData();
		metalCore.setName(FrostDemonForms.METAL_CORE);
		metalCore.setUnlockOnSkillLevel(3);
		metalCore.setCustomModel("frostdemon_metalcore");
        metalCore.setFormCombo("dragonminez:giant");
		metalCore.setStrMultiplier(5.1);
		metalCore.setSkpMultiplier(5.1);
		metalCore.setDefMultiplier(3.6875);
		metalCore.setPwrMultiplier(5.1);
		metalCore.setEnergyDrain(-0.1);
		metalCore.setHairType("base");
		metalCore.setAttackSpeed(0.25);
        metalCore.setModelScaling(new Float[]{3.8f, 3.8f, 3.8f});
        setDefaultMasteryValues(metalCore);
		metalCore.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> frostLegendaryData = new LinkedHashMap<>();
		frostLegendaryData.put(FrostDemonForms.MECHA, mecha);
		frostLegendaryData.put(FrostDemonForms.METAL, metal);
		frostLegendaryData.put(FrostDemonForms.METAL_CORE, metalCore);
		applySequentialMasteryRequisites(FrostDemonForms.GROUP_LEGENDARYFORMS, frostLegendaryData);
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
		kid.setStrMultiplier(1.75);
		kid.setSkpMultiplier(1.75);
		kid.setDefMultiplier(1.5);
		kid.setPwrMultiplier(1.75);
		kid.setHairType("base");
		setDefaultMasteryValues(kid);
		kid.setStackDrainMultiplier(2.0);
		kid.setAllowFreeTransformOnMastery(0.0);
		kid.setIncompatibleWith(List.of(""));

		FormConfig.FormData evil = new FormConfig.FormData();
		evil.setName(MajinForms.EVIL);
		evil.setUnlockOnSkillLevel(2);
		evil.setCustomModel("majin_evil");
		evil.setKeepBaseFormHeadBones(true);
		evil.setModelScaling(new Float[]{0.9f, 1.0f, 0.9f});
		evil.setStrMultiplier(2.25);
		evil.setSkpMultiplier(2.25);
		evil.setDefMultiplier(2.0);
		evil.setPwrMultiplier(2.25);
		evil.setHairColor("#917979");
		evil.setEye1Color("#F52746");
		evil.setEye2Color("#F52746");
		evil.setBodyColor1("#917979");
		evil.setBodyColor2("#917979");
		evil.setBodyColor3("#917979");
		evil.setHairType("base");
		setDefaultMasteryValues(evil);
		evil.setStackDrainMultiplier(2.0);
		evil.setIncompatibleWith(List.of(""));

		FormConfig.FormData superForm = new FormConfig.FormData();
        superForm.setCustomModel("majin_super");
		superForm.setName(MajinForms.SUPER);
		superForm.setUnlockOnSkillLevel(3);
		superForm.setKeepBaseFormHeadBones(true);
		superForm.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
		superForm.setStrMultiplier(3.0);
		superForm.setSkpMultiplier(3.0);
		superForm.setDefMultiplier(2.5625);
		superForm.setPwrMultiplier(3.0);
		superForm.setHairType("base");
		setDefaultMasteryValues(superForm);
		superForm.setStackDrainMultiplier(2.0);
		superForm.setIncompatibleWith(List.of(""));

		FormConfig.FormData ultra = new FormConfig.FormData();
		ultra.setName(MajinForms.ULTRA);
		ultra.setUnlockOnSkillLevel(4);
		ultra.setCustomModel("majin_ultra");
		ultra.setKeepBaseFormHeadBones(true);
		ultra.setModelScaling(new Float[]{1.3f, 1.2f, 1.3f});
		ultra.setStrMultiplier(3.75);
		ultra.setSkpMultiplier(3.75);
		ultra.setDefMultiplier(3.0);
		ultra.setPwrMultiplier(3.75);
		ultra.setEnergyDrain(0.22);
		ultra.setStaminaDrainMultiplier(3.5);
		ultra.setHasLightnings(true);
		ultra.setLightningColor("#F02B16");
		ultra.setHairType("base");
		setDefaultMasteryValues(ultra);
		ultra.setStackDrainMultiplier(2.0);
		ultra.setIncompatibleWith(List.of(""));

		Map<String, FormConfig.FormData> majinFormData = new LinkedHashMap<>();
		majinFormData.put(MajinForms.KID, kid);
		majinFormData.put(MajinForms.EVIL, evil);
		majinFormData.put(MajinForms.SUPER, superForm);
		majinFormData.put(MajinForms.ULTRA, ultra);
		applySequentialMasteryRequisites(MajinForms.GROUP_PUREFORMS, majinFormData);
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
		innocence.setTransformationAnimation("transf.janemba");
		innocence.setCustomModel("janemba_fat");
		innocence.setStrMultiplier(3.4);
		innocence.setSkpMultiplier(3.4);
		innocence.setDefMultiplier(2.625);
		innocence.setPwrMultiplier(3.4);
		innocence.setHairType("empty");
        innocence.setModelScaling(new Float[]{1.4f, 1.4f, 1.4f});
        setDefaultMasteryValues(innocence);
		innocence.setStackDrainMultiplier(2.0);
		innocence.setAllowFreeTransformOnMastery(0.0);

        FormConfig.FormData giant_innocence_demon = new FormConfig.FormData();
        giant_innocence_demon.setName(MajinForms.GIANT_INNOCENCE_DEMON);
        giant_innocence_demon.setUnlockOnSkillLevel(2);
		giant_innocence_demon.setTransformationAnimation("transf.janemba");
        giant_innocence_demon.setFormCombo("dragonminez:giant");
        giant_innocence_demon.setCustomModel("janemba_fat");
		giant_innocence_demon.setStrMultiplier(4.3);
		giant_innocence_demon.setSkpMultiplier(4.3);
		giant_innocence_demon.setDefMultiplier(3.25);
		giant_innocence_demon.setPwrMultiplier(4.3);
        giant_innocence_demon.setAttackSpeed(0.25);
        giant_innocence_demon.setEnergyDrain(0.25);
        giant_innocence_demon.setHairType("empty");
        giant_innocence_demon.setBodyColor1("#FFFC82");
        giant_innocence_demon.setModelScaling(new Float[]{3.0f, 3.0f, 3.0f});
        setDefaultMasteryValues(giant_innocence_demon);
        giant_innocence_demon.setStackDrainMultiplier(2.0);

		FormConfig.FormData superDemon = new FormConfig.FormData();
		superDemon.setName(MajinForms.SUPER_DEMON);
		superDemon.setUnlockOnSkillLevel(3);
		superDemon.setTransformationAnimation("transf.janemba");
		superDemon.setCustomModel("janemba_super");
		superDemon.setStrMultiplier(5.0);
		superDemon.setSkpMultiplier(5.0);
		superDemon.setDefMultiplier(3.625);
		superDemon.setPwrMultiplier(5.0);
		superDemon.setHairType("empty");
        superDemon.setBodyColor1("#FF6161");
        superDemon.setBodyColor2("#E1A8FF");
        superDemon.setEye1Color("#FFE98A");
        superDemon.setEye2Color("#303030");
        superDemon.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
        setDefaultMasteryValues(superDemon);
		superDemon.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> majinLegendaryData = new LinkedHashMap<>();
		majinLegendaryData.put(MajinForms.INNOCENCE_DEMON, innocence);
        majinLegendaryData.put(MajinForms.GIANT_INNOCENCE_DEMON, giant_innocence_demon);
        majinLegendaryData.put(MajinForms.SUPER_DEMON, superDemon);
		applySequentialMasteryRequisites(MajinForms.GROUP_LEGENDARYFORMS, majinLegendaryData);
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
		semiPerfect.setStrMultiplier(1.75);
		semiPerfect.setSkpMultiplier(1.75);
		semiPerfect.setDefMultiplier(1.5);
		semiPerfect.setPwrMultiplier(1.75);
		semiPerfect.setHairColor("");
		semiPerfect.setEye1Color("#0095FF");
		semiPerfect.setEye2Color("#FFFFFF");
		semiPerfect.setBodyColor1("");
		semiPerfect.setBodyColor2("");
		semiPerfect.setBodyColor3("");
		semiPerfect.setHairType("base");
		setDefaultMasteryValues(semiPerfect);
		semiPerfect.setStackDrainMultiplier(2.0);
		semiPerfect.setAllowFreeTransformOnMastery(0.0);
		semiPerfect.setIncompatibleWith(List.of(""));

		FormConfig.FormData perfect = new FormConfig.FormData();
		perfect.setName(BioAndroidForms.PERFECT);
		perfect.setUnlockOnSkillLevel(2);
		perfect.setCustomModel("bioandroid_perfect");
		perfect.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
		perfect.setStrMultiplier(2.4);
		perfect.setSkpMultiplier(2.4);
		perfect.setDefMultiplier(2.0625);
		perfect.setPwrMultiplier(2.4);
		perfect.setHairColor("");
		perfect.setEye1Color("#F6A6FF");
		perfect.setEye2Color("#FFFFFF");
		perfect.setBodyColor1("");
		perfect.setBodyColor2("#FFFFFF");
		perfect.setBodyColor3("");
		perfect.setHairType("base");
		setDefaultMasteryValues(perfect);
		perfect.setStackDrainMultiplier(2.0);
		perfect.setIncompatibleWith(List.of(""));

		FormConfig.FormData superPerfect = new FormConfig.FormData();
		superPerfect.setName(BioAndroidForms.SUPER_PERFECT);
		superPerfect.setUnlockOnSkillLevel(3);
		superPerfect.setCustomModel("bioandroid_perfect");
		superPerfect.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
		superPerfect.setStrMultiplier(3.05);
		superPerfect.setSkpMultiplier(3.05);
		superPerfect.setDefMultiplier(2.4375);
		superPerfect.setPwrMultiplier(3.05);
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
		superPerfect.setIncompatibleWith(List.of(""));

		FormConfig.FormData ultraperfect = new FormConfig.FormData();
		ultraperfect.setName(BioAndroidForms.ULTRA_PERFECT);
		ultraperfect.setUnlockOnSkillLevel(4);
		ultraperfect.setCustomModel("bioandroid_ultra");
		ultraperfect.setModelScaling(new Float[]{1.3f, 1.3f, 1.3f});
		ultraperfect.setStrMultiplier(3.9);
		ultraperfect.setSkpMultiplier(3.9);
		ultraperfect.setDefMultiplier(3.0);
		ultraperfect.setPwrMultiplier(3.9);
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
		ultraperfect.setIncompatibleWith(List.of(""));

		Map<String, FormConfig.FormData> bioFormData = new LinkedHashMap<>();
		bioFormData.put(BioAndroidForms.SEMI_PERFECT, semiPerfect);
		bioFormData.put(BioAndroidForms.PERFECT, perfect);
		bioFormData.put(BioAndroidForms.SUPER_PERFECT, superPerfect);
		bioFormData.put(BioAndroidForms.ULTRA_PERFECT, ultraperfect);
		applySequentialMasteryRequisites(BioAndroidForms.GROUP_BIOEVOLUTION, bioFormData);
		bioForms.setForms(bioFormData);

		forms.put(BioAndroidForms.GROUP_BIOEVOLUTION, bioForms);
		LogUtil.info(Env.COMMON, "Default Bio Android forms created");

		FormConfig bioLegendaryForms = new FormConfig();
		bioLegendaryForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		bioLegendaryForms.setGroupName(BioAndroidForms.GROUP_LEGENDARYFORMS);
		bioLegendaryForms.setFormType("legendaryforms");

		FormConfig.FormData xeno = new FormConfig.FormData();
		xeno.setName(BioAndroidForms.XENO);
		xeno.setUnlockOnSkillLevel(1);
		xeno.setCustomModel("bioandroid_ultra");
		xeno.setStrMultiplier(3.5);
		xeno.setSkpMultiplier(3.5);
		xeno.setDefMultiplier(2.6875);
		xeno.setPwrMultiplier(3.5);
		xeno.setEnergyDrain(0.06);
        xeno.setAuraColor("#2C0A4A");
        xeno.setHasLightnings(true);
        xeno.setLightningColor("#340063");
        xeno.setEye1Color("#FFFFFF");
        xeno.setEye2Color("#FFFFFF");
        xeno.setBodyColor2("#DBC8C8");
        xeno.setBodyColor3("#4C3554");
        xeno.setHairColor("#4C3554");
        xeno.setHairType("base");
		setDefaultMasteryValues(xeno);
        xeno.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
        xeno.setStackDrainMultiplier(2.0);
		xeno.setAllowFreeTransformOnMastery(0.0);

		FormConfig.FormData xenoFP = new FormConfig.FormData();
		xenoFP.setName(BioAndroidForms.XENO_FP);
		xenoFP.setUnlockOnSkillLevel(2);
		xenoFP.setCustomModel("bioandroid_xeno");
		xenoFP.setStrMultiplier(4.4);
		xenoFP.setSkpMultiplier(4.4);
		xenoFP.setDefMultiplier(3.3125);
		xenoFP.setPwrMultiplier(4.4);
		xenoFP.setEnergyDrain(0.16);
        xenoFP.setAuraColor("#2C0A4A");
        xenoFP.setHasLightnings(true);
        xenoFP.setLightningColor("#340063");
        xenoFP.setEye1Color("#FFFFFF");
        xenoFP.setEye2Color("#FFFFFF");
        xenoFP.setBodyColor2("#DBC8C8");
        xenoFP.setBodyColor3("#4C3554");
        xenoFP.setHairColor("#4C3554");
        xenoFP.setHairType("base");
		setDefaultMasteryValues(xenoFP);
        xenoFP.setModelScaling(new Float[]{1.3f, 1.3f, 1.3f});
        xenoFP.setStackDrainMultiplier(2.0);

		FormConfig.FormData xenoMax = new FormConfig.FormData();
		xenoMax.setName(BioAndroidForms.XENO_MAX);
		xenoMax.setUnlockOnSkillLevel(3);
		xenoMax.setCustomModel("bioandroid_xeno");
		xenoMax.setStrMultiplier(5.1);
		xenoMax.setSkpMultiplier(5.1);
		xenoMax.setDefMultiplier(3.6875);
		xenoMax.setPwrMultiplier(5.1);
		xenoMax.setEnergyDrain(0.22);
        xenoMax.setAuraColor("#2C0A4A");
        xenoMax.setHasLightnings(true);
        xenoMax.setLightningColor("#340063");
        xenoMax.setEye1Color("#FFFFFF");
        xenoMax.setEye2Color("#FFFFFF");
        xenoMax.setBodyColor2("#DBC8C8");
        xenoMax.setBodyColor3("#4C3554");
        xenoMax.setHairColor("#4C3554");
        xenoMax.setHairType("base");
		setDefaultMasteryValues(xenoMax);
        xenoMax.setModelScaling(new Float[]{1.5f, 1.5f, 1.5f});
        xenoMax.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> bioLegendaryData = new LinkedHashMap<>();
		bioLegendaryData.put(BioAndroidForms.XENO, xeno);
		bioLegendaryData.put(BioAndroidForms.XENO_FP, xenoFP);
		bioLegendaryData.put(BioAndroidForms.XENO_MAX, xenoMax);
		applySequentialMasteryRequisites(BioAndroidForms.GROUP_LEGENDARYFORMS, bioLegendaryData);
		bioLegendaryForms.setForms(bioLegendaryData);

		forms.put(BioAndroidForms.GROUP_LEGENDARYFORMS, bioLegendaryForms);
		LogUtil.info(Env.COMMON, "Default Bio Android legendary forms created");
	}
}
