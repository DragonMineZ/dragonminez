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
		form.setMasteryPerHitDealt(0.025);
		form.setMasteryPerHitReceived(0.025);
		form.setMaxStatsMultiplier(1.5);
		form.setMaxCostMultiplier(0.75);
		form.setPassiveMasteryEveryFiveSeconds(0.004);
		form.setStackOnMastery(25.0);
		form.setAuraType("kakarot");
		form.setAuraType3D(FormConfig.AURA_3D_USER_PREFERENCE);
		form.setAuraLayer(0);
	}

	private static final double[] TIER_1 = {1.5, 1.1};
	private static final double[] GRADE_2 = {1.75, 1.2};
	private static final double[] GRADE_3 = {2.75, 2.0};
	private static final double[] TIER_2 = {2.0, 1.25};
	private static final double[] TIER_3 = {3.0, 2.5};
	private static final double[] TIER_4 = {3.0, 1.5};
	private static final double[] MASTERED_SSJ = {1.3, 1.0};
	private static final double[] FALSE_SSJ = {1.25, 1.1};

	private static void otherworldDrain(FormConfig.FormData form, double[] drain) {
		form.setOtherworldTimeDrain(drain[0]);
		form.setOtherworldTimeDrainMastered(drain[1]);
	}

	private static FormConfig.Aura3DStyle aura3D() {
		return new FormConfig.Aura3DStyle();
	}

	private static FormConfig.Aura3DStyle kaiokenAura(float amplitude) {
		return aura3D().size(1.18f, 1.20f, 1.12f).waves(2.1f, 5.0f, amplitude).turbulence(1.0f, 0.51f).rim(0.02f, 1.0f, 5.0f, 0.01f).colors("#FF0000", "#FF0000", "#FF0066");
	}

	private static FormConfig.Aura3DStyle darkAura(String noise) {
		return aura3D().waves(2.1f, 5.0f, 0.76f).turbulence(0.5f, 0.54f).rim(0.04f, 1.0f, 4.5f, 0.06f).colors("", "", noise);
	}

	public void createDefaultFormsForRace(String raceName, Path formsPath, Map<String, FormConfig> forms) throws IOException {
		switch (raceName.toLowerCase()) {
			case "human" -> createDefaultHumanForms(formsPath, forms);
			case "saiyan" -> createSaiyanForms(formsPath, forms);
			case "namekian" -> createNamekianForms(formsPath, forms);
			case "frostdemon" -> createFrostDemonForms(formsPath, forms);
			case "majin" -> createMajinForms(formsPath, forms);
			case "bioandroid" -> createBioAndroidForms(formsPath, forms);
			case "glind" -> createGlindForms(formsPath, forms);
		}
		forceGroupAura3D(forms, LEGENDARY_FORM_TYPE, FormConfig.AURA_3D_SPARKING);
	}

	private static final String LEGENDARY_FORM_TYPE = "legendaryforms";
	private static final String FALSE_FORM_TYPE = "falseform";

	private static void forceGroupAura3D(Map<String, FormConfig> forms, String formType, String auraType) {
		for (FormConfig config : forms.values()) {
			if (config == null || !formType.equalsIgnoreCase(config.getFormType())) continue;
			for (FormConfig.FormData form : config.getForms().values()) {
				if (form == null) continue;
				form.setAuraType3D(auraType);
				form.setExtraAuraType3D(auraType);
			}
		}
	}

	public void createDefaultStackForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		createDefaultKaiokenForms(formsPath, forms);
 		createDefaultUltimateForms(formsPath, forms);
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
		x2.setAura3DStyle(kaiokenAura(0.67f));
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
		x3.setAura3DStyle(kaiokenAura(0.70f));
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
		x4.setAura3DStyle(kaiokenAura(0.72f));
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
		x10.setAura3DStyle(kaiokenAura(0.76f));
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
		x20.setAura3DStyle(kaiokenAura(0.80f));
		x20.setAuraLayer(1);
		x20.setStackOnMastery(0.0);
		x20.setStackDrainMultiplier(1.0);

		Map<String, FormConfig.FormData> stackFormData = new LinkedHashMap<>();
		stackFormData.put(StackForms.X2, x2);
		stackFormData.put(StackForms.X3, x3);
		stackFormData.put(StackForms.X4, x4);
		stackFormData.put(StackForms.X10, x10);
		stackFormData.put(StackForms.X20, x20);
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
		ultimate.setAuraType3D(FormConfig.AURA_3D_SMOOTH);
		ultimate.setCustomModel("finalbase");
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
		ultimate.setAura3DStyle(aura3D().waves(1.8f, 3.6f, 0.5f).turbulence(0.05f, 0.5f).rim(0.03f, 0.9f, 3.0f, 0.08f));

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
		sign.setAura3DStyle(aura3D().waves(1.7f, 3.2f, 0.45f).turbulence(0.06f, 0.48f).rim(0.02f, 0.9f, 2.6f, 0.09f).colors("", "", "#8FA8FF"));
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
		mastered.setAura3DStyle(aura3D().waves(1.7f, 3.0f, 0.42f).turbulence(0.04f, 0.46f).rim(0.02f, 0.85f, 2.4f, 0.10f).colors("", "", "#A8BFFF"));
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
		sign.setAura3DStyle(aura3D().waves(2.2f, 5.4f, 0.8f).turbulence(0.35f, 0.55f).rim(0.04f, 1.0f, 3.65f, 0.06f).colors("", "", "#FF00EB"));
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
		mastered.setAura3DStyle(aura3D().size(1.12f, 1.2f, 1.04f).waves(1.85f, 5.0f, 0.7f).turbulence(0.45f, 0.54f).rim(0.02f, 1.0f, 3.65f, 0.06f).colors("", "", "#FF00EB"));
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
		buffed.setSagaRequisite("classic_saga:46");
		buffed.setCustomModel("buffedg3");
		buffed.setModelScaling(new Float[]{1.2f, 1.1f, 1.2f});
		buffed.setStrMultiplier(1.7);
		buffed.setSkpMultiplier(1.85);
		buffed.setDefMultiplier(1.425);
		buffed.setPwrMultiplier(1.5);
		buffed.setEnergyDrain(0.08);
		buffed.setHairType("base");
		setDefaultMasteryValues(buffed);
		buffed.setAura3DStyle(aura3D().waves(2.0f, 5.0f, 0.78f).turbulence(0.12f, 0.54f));
		buffed.setStackDrainMultiplier(2.0);
		buffed.setAllowFreeTransformOnMastery(0.0);
		buffed.setIncompatibleWith(List.of(""));

		FormConfig.FormData fullPower = new FormConfig.FormData();
		fullPower.setName(HumanForms.FULLPOWER);
		fullPower.setUnlockOnSkillLevel(2);
		fullPower.setSagaRequisite("saiyan_saga:12");
		fullPower.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
		fullPower.setStrMultiplier(2.25);
		fullPower.setSkpMultiplier(2.45);
		fullPower.setDefMultiplier(1.9375);
		fullPower.setPwrMultiplier(2.05);
		fullPower.setEnergyDrain(0.16);
		fullPower.setHairType("ssj");
		fullPower.setAura3DStyle(aura3D().waves(2.0f, 5.3f, 0.8f).turbulence(0.15f, 0.54f));
		setDefaultMasteryValues(fullPower);
		fullPower.setStackDrainMultiplier(2.0);
		fullPower.setIncompatibleWith(List.of(""));

		FormConfig.FormData overdrive = new FormConfig.FormData();
		overdrive.setName(HumanForms.OVERDRIVE);
		overdrive.setUnlockOnSkillLevel(3);
		overdrive.setSagaRequisite("frieza_saga:10");
		overdrive.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
		overdrive.setStrMultiplier(3.15);
		overdrive.setSkpMultiplier(3.3);
		overdrive.setDefMultiplier(2.65);
		overdrive.setPwrMultiplier(2.85);
		overdrive.setEnergyDrain(0.34);
		overdrive.setAuraColor("#FFFD99");
		overdrive.setHasLightnings(true);
		overdrive.setLightningColor("#E6F2F5");
		overdrive.setHairType("ssj2");
		setDefaultMasteryValues(overdrive);
		overdrive.setAura3DStyle(aura3D().size(1.12f, 1.2f, 1.04f).waves(2.1f, 5.8f, 0.82f).turbulence(0.2f, 0.55f));
		overdrive.setStackDrainMultiplier(2.0);
		overdrive.setIncompatibleWith(List.of(""));

		FormConfig.FormData solaris = new FormConfig.FormData();
		solaris.setName(HumanForms.SOLARIS);
		solaris.setUnlockOnSkillLevel(4);
		solaris.setSagaRequisite("buu_saga:35");
		solaris.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
		solaris.setStrMultiplier(4.6);
		solaris.setSkpMultiplier(4.75);
		solaris.setDefMultiplier(3.6);
		solaris.setPwrMultiplier(4.25);
		solaris.setEnergyDrain(0.22);
		solaris.setHairType("ssj2");
		setDefaultMasteryValues(solaris);
		solaris.setAura3DStyle(aura3D().waves(1.9f, 4.6f, 0.66f).turbulence(0.10f, 0.52f).rim(0.04f, 1.0f, 3.2f, 0.07f).colors("", "", "#FFB300"));
		solaris.setStackDrainMultiplier(2.0);
		solaris.setIncompatibleWith(List.of("ultimate.ultimate"));

		Map<String, FormConfig.FormData> humanFormData = new LinkedHashMap<>();
		otherworldDrain(buffed, TIER_1);
		otherworldDrain(fullPower, TIER_2);
		otherworldDrain(overdrive, TIER_3);
		otherworldDrain(solaris, TIER_4);
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
		shiyoken.setStrMultiplier(3.75);
		shiyoken.setSkpMultiplier(3.75);
		shiyoken.setDefMultiplier(2.8625);
		shiyoken.setPwrMultiplier(3.75);
		shiyoken.setEnergyDrain(0.22);
		shiyoken.setHairType("base");
        shiyoken.setTintColor("#FF0000");
        shiyoken.setTintIntensity(0.25);
        shiyoken.setAuraColor("#A10000");
        shiyoken.setHasLightnings(true);
        shiyoken.setLightningColor("#FF4F4F");
		setDefaultMasteryValues(shiyoken);
		shiyoken.setAura3DStyle(darkAura("#FF1A1A").waves(2.1f, 5.2f, 0.78f).turbulence(0.45f, 0.54f));
		shiyoken.setStackDrainMultiplier(2.0);
		shiyoken.setAllowFreeTransformOnMastery(0.0);
        shiyoken.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});

		FormConfig.FormData shin_shiyoken = new FormConfig.FormData();
		shin_shiyoken.setName(HumanForms.SHIN_SHIYOKEN);
		shin_shiyoken.setUnlockOnSkillLevel(2);
		shin_shiyoken.setCustomModel("buffed");
		shin_shiyoken.setStrMultiplier(4.8);
		shin_shiyoken.setSkpMultiplier(4.8);
		shin_shiyoken.setDefMultiplier(3.5875);
		shin_shiyoken.setPwrMultiplier(4.8);
		shin_shiyoken.setEnergyDrain(0.28);
		shin_shiyoken.setHairType("base");
        shin_shiyoken.setTintColor("#FF0000");
        shin_shiyoken.setTintIntensity(0.25);
        shin_shiyoken.setAuraColor("#A10000");
        shin_shiyoken.setHasLightnings(true);
        shin_shiyoken.setLightningColor("#FF4F4F");
		setDefaultMasteryValues(shin_shiyoken);
		shin_shiyoken.setAura3DStyle(darkAura("#FF1A1A").size(1.12f, 1.2f, 1.03f).waves(2.1f, 5.2f, 0.8f).turbulence(0.45f, 0.54f));
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
		chou_shiyoken.setStrMultiplier(5.6);
		chou_shiyoken.setSkpMultiplier(5.6);
		chou_shiyoken.setDefMultiplier(4.0125);
		chou_shiyoken.setPwrMultiplier(5.6);
		chou_shiyoken.setEnergyDrain(0.34);
        chou_shiyoken.setHairType("base");
        chou_shiyoken.setTintColor("#FF0000");
        chou_shiyoken.setTintIntensity(0.25);
        chou_shiyoken.setAuraColor("#A10000");
        chou_shiyoken.setHasLightnings(true);
        chou_shiyoken.setLightningColor("#FF4F4F");
        setDefaultMasteryValues(chou_shiyoken);
        chou_shiyoken.setAura3DStyle(darkAura("#FF1A1A").size(1.18f, 1.24f, 1.08f).waves(2.1f, 5.4f, 0.82f).turbulence(0.5f, 0.55f));
        chou_shiyoken.setStackDrainMultiplier(2.0);
        chou_shiyoken.setModelScaling(new Float[]{1.5f, 1.5f, 1.5f});

		Map<String, FormConfig.FormData> humanLegendaryData = new LinkedHashMap<>();
		otherworldDrain(shiyoken, TIER_1);
		otherworldDrain(shin_shiyoken, TIER_2);
		otherworldDrain(chou_shiyoken, TIER_3);
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
		androidBase.setStrMultiplier(2.15);
		androidBase.setSkpMultiplier(2.15);
		androidBase.setDefMultiplier(1.8625);
		androidBase.setPwrMultiplier(2.5);
		androidBase.setHairType("base");
		setDefaultMasteryValues(androidBase);
		androidBase.setAura3DStyle(aura3D().waves(1.9f, 4.5f, 0.6f).turbulence(0.05f, 0.52f));
		androidBase.setStackDrainMultiplier(2.0);
		androidBase.setAllowFreeTransformOnMastery(0.0);

		FormConfig.FormData superAndroid = new FormConfig.FormData();
		superAndroid.setName(HumanForms.SUPER_ANDROID);
		superAndroid.setUnlockOnSkillLevel(1);
		superAndroid.setSagaRequisite("frieza_saga:10");
		superAndroid.setCustomModel("buffed");
		superAndroid.setModelScaling(new Float[]{1.05f, 1.05f, 1.05f});
		superAndroid.setStrMultiplier(3.2);
		superAndroid.setSkpMultiplier(3.2);
		superAndroid.setDefMultiplier(2.575);
		superAndroid.setPwrMultiplier(3.65);
		superAndroid.setHairType("ssj");
		setDefaultMasteryValues(superAndroid);
		superAndroid.setAura3DStyle(aura3D().waves(2.0f, 5.0f, 0.72f).turbulence(0.1f, 0.54f));
		superAndroid.setStackDrainMultiplier(2.0);

		FormConfig.FormData fusedAndroid = new FormConfig.FormData();
		fusedAndroid.setName(HumanForms.FUSED_ANDROID);
		fusedAndroid.setUnlockOnSkillLevel(2);
		fusedAndroid.setSagaRequisite("buu_saga:1");
		fusedAndroid.setCustomModel("buffedg3");
		fusedAndroid.setModelScaling(new Float[]{1.4f, 1.3f, 1.4f});
		fusedAndroid.setStrMultiplier(4.1);
		fusedAndroid.setSkpMultiplier(4.1);
		fusedAndroid.setDefMultiplier(3.3);
		fusedAndroid.setPwrMultiplier(4.7);
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
		fusedAndroid.setAura3DStyle(aura3D().size(1.14f, 1.2f, 1.06f).waves(2.1f, 5.4f, 0.8f).turbulence(0.3f, 0.55f));
		fusedAndroid.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> androidFormData = new LinkedHashMap<>();
		otherworldDrain(superAndroid, TIER_1);
		otherworldDrain(fusedAndroid, TIER_2);
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
		oozaru.setStrMultiplier(1.25);
		oozaru.setSkpMultiplier(1.25);
		oozaru.setDefMultiplier(1.15);
		oozaru.setPwrMultiplier(1.25);
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
		goldenOozaru.setSagaRequisite("buu_saga:1");
		goldenOozaru.setFormCombo("dragonminez:giant");
		goldenOozaru.setCustomModel("oozaru");
		goldenOozaru.setTransformationAnimation("transf.ozaru");
		goldenOozaru.setHairColor("#FFD700");
		goldenOozaru.setAuraColor("#FFD700");
		goldenOozaru.setBodyColor2("#FFD700");
		goldenOozaru.setModelScaling(new Float[]{3.8f, 3.8f, 3.8f});
		goldenOozaru.setStrMultiplier(2.15);
		goldenOozaru.setSkpMultiplier(2.15);
		goldenOozaru.setDefMultiplier(2.3);
		goldenOozaru.setPwrMultiplier(2.15);
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
		ssj4gt.setSagaRequisite("buu_saga:35");
		ssj4gt.setHairColor("");
		ssj4gt.setBodyColor2("#9d1e31");
		ssj4gt.setEye1Color("#FFD700");
		ssj4gt.setEye2Color("#FFD700");
		ssj4gt.setAuraColor("#FFD700");
		ssj4gt.setModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		ssj4gt.setStrMultiplier(4.75);
		ssj4gt.setSkpMultiplier(4.75);
		ssj4gt.setDefMultiplier(3.5625);
		ssj4gt.setPwrMultiplier(4.75);
		ssj4gt.setEnergyDrain(0.24);
		ssj4gt.setHairType("ssj4");
		ssj4gt.setForcedHairCode("");
		setDefaultMasteryValues(ssj4gt);
		ssj4gt.setAura3DStyle(aura3D().waves(2.0f, 5.0f, 0.78f).turbulence(0.13f, 0.54f).colors("", "", "#C8102E"));
		ssj4gt.setStackDrainMultiplier(2.0);
		ssj4gt.setAllowFreeTransformOnMastery(50.0);
		ssj4gt.setIncompatibleWith(List.of("ultimate.ultimate"));

		Map<String, FormConfig.FormData> oozaruFormData = new LinkedHashMap<>();
		otherworldDrain(oozaru, TIER_1);
		otherworldDrain(goldenOozaru, TIER_3);
		otherworldDrain(ssj4gt, TIER_4);
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
		ssj1.setSagaRequisite("classic_saga:46");
		ssj1.setHairColor("#FFEDB3");
		ssj1.setBodyColor2("#FFEDB3");
		ssj1.setEye1Color("#00FFFF");
		ssj1.setEye2Color("#00FFFF");
		ssj1.setAuraColor("#FFD700");
		ssj1.setModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		ssj1.setStrMultiplier(1.6);
		ssj1.setSkpMultiplier(1.6);
		ssj1.setDefMultiplier(1.375);
		ssj1.setPwrMultiplier(1.6);
		ssj1.setEnergyDrain(0.08);
		ssj1.setHairType("ssj");
		setDefaultMasteryValues(ssj1);
		ssj1.setAura3DStyle(aura3D().waves(2.0f, 5.0f, 0.77f).turbulence(0.14f, 0.54f));
		ssj1.setStackDrainMultiplier(2.0);
		ssj1.setAllowFreeTransformOnMastery(0.0);
		ssj1.setIncompatibleWith(List.of(""));

		FormConfig.FormData ssg2 = new FormConfig.FormData();
		ssg2.setName(SaiyanForms.SUPER_SAIYAN_GRADE_2);
		ssg2.setTransformationAnimation("transf.ssg2");
		ssg2.setUnlockOnSkillLevel(2);
		ssg2.setSagaRequisite("saiyan_saga:12");
		ssg2.setCustomModel("buffed");
		ssg2.setHairColor("#FFEDB3");
		ssg2.setBodyColor2("#FFEDB3");
		ssg2.setEye1Color("#00FFFF");
		ssg2.setEye2Color("#00FFFF");
		ssg2.setAuraColor("#FFD700");
		ssg2.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
		ssg2.setStrMultiplier(1.85);
		ssg2.setSkpMultiplier(1.85);
		ssg2.setDefMultiplier(1.575);
		ssg2.setPwrMultiplier(1.85);
		ssg2.setSpeedMultiplier(0.9);
		ssg2.setEnergyDrain(0.12);
		ssg2.setStaminaDrainMultiplier(1.3);
		ssg2.setHairType("ssj");
		setDefaultMasteryValues(ssg2);
		ssg2.setAura3DStyle(aura3D().size(1.14f, 1.18f, 1.06f).waves(2.0f, 5.2f, 0.8f).turbulence(0.16f, 0.54f));
		ssg2.setStackDrainMultiplier(2.0);
		ssg2.setIncompatibleWith(List.of(""));

		FormConfig.FormData ssg3 = new FormConfig.FormData();
		ssg3.setName(SaiyanForms.SUPER_SAIYAN_GRADE_3);
		ssg3.setUnlockOnSkillLevel(3);
		ssg3.setSagaRequisite("frieza_saga:5");
		ssg3.setCustomModel("buffedg3");
		ssg3.setHairColor("#FFEDB3");
		ssg3.setBodyColor2("#FFEDB3");
		ssg3.setEye1Color("#00FFFF");
		ssg3.setEye2Color("#00FFFF");
		ssg3.setAuraColor("#FFD700");
		ssg3.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
		ssg3.setStrMultiplier(3.0);
		ssg3.setSkpMultiplier(3.0);
		ssg3.setDefMultiplier(2.15);
		ssg3.setPwrMultiplier(3.0);
		ssg3.setSpeedMultiplier(0.7);
		ssg3.setEnergyDrain(0.48);
		ssg3.setStaminaDrainMultiplier(3.5);
		ssg3.setAttackSpeed(0.75);
		ssg3.setHairType("ssj");
		setDefaultMasteryValues(ssg3);
		ssg3.setAura3DStyle(aura3D().size(1.22f, 1.22f, 1.12f).waves(2.0f, 4.4f, 0.86f).turbulence(0.2f, 0.5f));
		ssg3.setStackDrainMultiplier(2.0);
		ssg3.setIncompatibleWith(List.of(""));

		Map<String, FormConfig.FormData> ssGradeForms = new LinkedHashMap<>();
		otherworldDrain(ssj1, TIER_1);
		otherworldDrain(ssg2, GRADE_2);
		otherworldDrain(ssg3, GRADE_3);
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
		ssj1Mastered.setSagaRequisite("android_saga:4");
		ssj1Mastered.setHairColor("#FFE89E");
		ssj1Mastered.setBodyColor2("#FFE89E");
		ssj1Mastered.setEye1Color("#00FFFF");
		ssj1Mastered.setEye2Color("#00FFFF");
		ssj1Mastered.setAuraColor("#FFD700");
		ssj1Mastered.setModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		ssj1Mastered.setStrMultiplier(1.85);
		ssj1Mastered.setSkpMultiplier(1.85);
		ssj1Mastered.setDefMultiplier(1.5);
		ssj1Mastered.setPwrMultiplier(1.85);
		ssj1Mastered.setEnergyDrain(0.03);
		ssj1Mastered.setHairType("ssj");
		setDefaultMasteryValues(ssj1Mastered);
		ssj1Mastered.setAura3DStyle(aura3D().waves(1.9f, 4.6f, 0.7f).turbulence(0.08f, 0.54f));
		ssj1Mastered.setStackDrainMultiplier(2.0);
		ssj1Mastered.setAllowFreeTransformOnMastery(0.0);
		ssj1Mastered.setIncompatibleWith(List.of(""));

		FormConfig.FormData ssj2 = new FormConfig.FormData();
		ssj2.setName(SaiyanForms.SUPER_SAIYAN_2);
		ssj2.setUnlockOnSkillLevel(5);
		ssj2.setSagaRequisite("android_saga:15");
		ssj2.setHairColor("#FFE89E");
		ssj2.setBodyColor2("#FFE89E");
		ssj2.setEye1Color("#00FFFF");
		ssj2.setEye2Color("#00FFFF");
		ssj2.setAuraColor("#FFD700");
		ssj2.setHasLightnings(true);
		ssj2.setLightningColor("#A1FFF9");
		ssj2.setModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		ssj2.setStrMultiplier(2.45);
		ssj2.setSkpMultiplier(2.45);
		ssj2.setDefMultiplier(1.9375);
		ssj2.setPwrMultiplier(2.45);
		ssj2.setEnergyDrain(0.16);
		ssj2.setHairType("ssj2");
		setDefaultMasteryValues(ssj2);
		ssj2.setAura3DStyle(aura3D().waves(2.1f, 5.6f, 0.8f).turbulence(0.18f, 0.55f).colors("", "", "#FFF6C8"));
		ssj2.setStackDrainMultiplier(2.0);
		ssj2.setIncompatibleWith(List.of(""));

		FormConfig.FormData ssj3 = new FormConfig.FormData();
		ssj3.setName(SaiyanForms.SUPER_SAIYAN_3);
		ssj3.setUnlockOnSkillLevel(6);
		ssj3.setSagaRequisite("buu_saga:1");
		ssj3.setTransformationAnimation("transf.ssj3");
		ssj3.setHairColor("#FFE89E");
		ssj3.setBodyColor2("#FFE89E");
		ssj3.setEye1Color("#00FFFF");
		ssj3.setEye2Color("#00FFFF");
		ssj3.setAuraColor("#FFD700");
		ssj3.setHasLightnings(true);
		ssj3.setLightningColor("#A1FFF9");
		ssj3.setModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		ssj3.setStrMultiplier(3.3);
		ssj3.setSkpMultiplier(3.3);
		ssj3.setDefMultiplier(2.65);
		ssj3.setPwrMultiplier(3.3);
		ssj3.setEnergyDrain(0.34);
		ssj3.setHairType("ssj3");
		setDefaultMasteryValues(ssj3);
		ssj3.setAura3DStyle(aura3D().size(1.12f, 1.24f, 1.05f).waves(2.1f, 5.4f, 0.84f).turbulence(0.2f, 0.56f).colors("", "", "#FFF6C8"));
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
		ssj4d.setSagaRequisite("buu_saga:35");
		ssj4d.setHairColor("#83073F");
		ssj4d.setBodyColor2("#83073F");
		ssj4d.setEye1Color("#83073F");
		ssj4d.setEye2Color("#83073F");
		ssj4d.setAuraColor("#FFD633");
        ssj4d.setModelScaling(new Float[]{1.2f, 1.2f, 1.2f});
        ssj4d.setStrMultiplier(4.75);
		ssj4d.setSkpMultiplier(4.75);
		ssj4d.setDefMultiplier(3.5625);
		ssj4d.setPwrMultiplier(4.75);
		ssj4d.setEnergyDrain(0.24);
		ssj4d.setHairType("ssj4");
		ssj4d.setForcedHairCode("");
        ssj4d.setLightningColor("#82C9FF");
        ssj4d.setHasLightnings(true);
        ssj4d.setStackDrainMultiplier(2.0);
        setDefaultMasteryValues(ssj4d);
        ssj4d.setAura3DStyle(aura3D().waves(2.0f, 5.0f, 0.78f).turbulence(0.13f, 0.54f).colors("", "", "#C8102E"));
        ssj4d.setAllowFreeTransformOnMastery(50.0);
		ssj4d.setIncompatibleWith(List.of("ultimate.ultimate"));

		Map<String, FormConfig.FormData> superSaiyanForms = new LinkedHashMap<>();
		otherworldDrain(ssj1Mastered, MASTERED_SSJ);
		otherworldDrain(ssj2, TIER_2);
		otherworldDrain(ssj3, TIER_3);
		otherworldDrain(ssj4d, TIER_4);
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

		FormConfig falseForms = new FormConfig();
		falseForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		falseForms.setGroupName(SaiyanForms.GROUP_FALSEFORM);
		falseForms.setFormType(FALSE_FORM_TYPE);

		FormConfig.FormData falseSuperSaiyan = new FormConfig.FormData();
		falseSuperSaiyan.setName(SaiyanForms.FALSE_SUPER_SAIYAN);
		falseSuperSaiyan.setUnlockOnSkillLevel(99);
		falseSuperSaiyan.setTransformationAnimation("transf.berserker");
		falseSuperSaiyan.setHairType("ssj");
		falseSuperSaiyan.setHairColor("#6B1208");
		falseSuperSaiyan.setBodyColor2("#6B1208");
		falseSuperSaiyan.setEye1Color("#FFFFFF");
		falseSuperSaiyan.setEye2Color("#FFFFFF");
		falseSuperSaiyan.setAuraColor("#FFD84D");
		falseSuperSaiyan.setTintColor("#FFD21F");
		falseSuperSaiyan.setTintIntensity(0.55);
		falseSuperSaiyan.setModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		falseSuperSaiyan.setStrMultiplier(1.45);
		falseSuperSaiyan.setSkpMultiplier(1.45);
		falseSuperSaiyan.setDefMultiplier(1.2875);
		falseSuperSaiyan.setPwrMultiplier(1.45);
		falseSuperSaiyan.setVitMultiplier(1.0);
		falseSuperSaiyan.setEnergyDrain(0.0);
		setDefaultMasteryValues(falseSuperSaiyan);
		falseSuperSaiyan.setAura3DStyle(aura3D().waves(2.1f, 5.2f, 0.82f).turbulence(0.22f, 0.55f).colors("", "", "#FFF0A0"));
		falseSuperSaiyan.setFormStackable(false);
		falseSuperSaiyan.setStackDrainMultiplier(2.0);
		falseSuperSaiyan.setAllowFreeTransformOnMastery(0.0);
		falseSuperSaiyan.setIncompatibleWith(List.of(""));

		Map<String, FormConfig.FormData> falseFormData = new LinkedHashMap<>();
		otherworldDrain(falseSuperSaiyan, FALSE_SSJ);
		falseFormData.put(SaiyanForms.FALSE_SUPER_SAIYAN, falseSuperSaiyan);
		falseForms.setForms(falseFormData);

		forms.put(SaiyanForms.GROUP_FALSEFORM, falseForms);

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
		ikari.setStrMultiplier(3.75);
		ikari.setSkpMultiplier(3.75);
		ikari.setDefMultiplier(2.8625);
		ikari.setPwrMultiplier(3.75);
		ikari.setEnergyDrain(0.1);
        ikari.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
        ikari.setHairType("ssj");
		setDefaultMasteryValues(ikari);
		ikari.setAura3DStyle(aura3D().waves(2.2f, 5.6f, 0.8f).turbulence(0.3f, 0.55f).colors("", "", "#1B8C00"));
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
		ssjHybrid.setStrMultiplier(4.8);
		ssjHybrid.setSkpMultiplier(4.8);
		ssjHybrid.setDefMultiplier(3.5875);
		ssjHybrid.setPwrMultiplier(4.8);
		ssjHybrid.setEnergyDrain(0.16);
        ssjHybrid.setModelScaling(new Float[]{1.15f, 1.15f, 1.15f});
        ssjHybrid.setHairType("ssj");
		setDefaultMasteryValues(ssjHybrid);
		ssjHybrid.setAura3DStyle(aura3D().size(1.14f, 1.2f, 1.06f).waves(2.2f, 5.8f, 0.84f).turbulence(0.35f, 0.56f).colors("", "", "#1B8C00"));
		ssjHybrid.setStackDrainMultiplier(2.0);

		FormConfig.FormData ssjFullPower = new FormConfig.FormData();
		ssjFullPower.setName(SaiyanForms.SSJ_FULL_POWER);
		ssjFullPower.setUnlockOnSkillLevel(3);
		ssjFullPower.setTransformationAnimation("transf.berserker");
		ssjFullPower.setCustomModel("buffedg3");
		ssjFullPower.setBodyColor2("#9EFE53");
		ssjFullPower.setHairColor("#9EFE53");
		ssjFullPower.setEye1Color("#FFFFFF");
		ssjFullPower.setEye2Color("#FFFFFF");
		ssjFullPower.setAuraColor("#40FF00");
		ssjFullPower.setHasLightnings(true);
		ssjFullPower.setLightningColor("#40FF00");
		ssjFullPower.setStrMultiplier(5.6);
		ssjFullPower.setSkpMultiplier(5.6);
		ssjFullPower.setDefMultiplier(4.0125);
		ssjFullPower.setPwrMultiplier(5.6);
		ssjFullPower.setEnergyDrain(0.26);
        ssjFullPower.setModelScaling(new Float[]{1.4f, 1.3f, 1.4f});
        ssjFullPower.setHairType("ssj2");
		setDefaultMasteryValues(ssjFullPower);
		ssjFullPower.setAura3DStyle(aura3D().size(1.22f, 1.26f, 1.12f).waves(2.2f, 6.0f, 0.9f).turbulence(0.42f, 0.56f).colors("", "", "#0F5C00"));
		ssjFullPower.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> saiyanLegendaryData = new LinkedHashMap<>();
		otherworldDrain(ikari, TIER_1);
		otherworldDrain(ssjHybrid, TIER_2);
		otherworldDrain(ssjFullPower, TIER_3);
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
		giantForm.setSagaRequisite("classic_saga:46");
		giantForm.setFormCombo("dragonminez:giant");
		giantForm.setKeepBaseFormHeadBones(true);
		giantForm.setModelScaling(new Float[]{3.6f, 3.6f, 3.6f});
		giantForm.setStrMultiplier(2.15);
		giantForm.setSkpMultiplier(2.15);
		giantForm.setDefMultiplier(1.8625);
		giantForm.setPwrMultiplier(2.15);
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
		fullPower.setSagaRequisite("frieza_saga:10");
		fullPower.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
		fullPower.setStrMultiplier(3.15);
		fullPower.setSkpMultiplier(3.15);
		fullPower.setDefMultiplier(2.5125);
		fullPower.setPwrMultiplier(3.15);
		fullPower.setEnergyDrain(0.18);
		fullPower.setAura3DStyle(aura3D().waves(2.0f, 5.0f, 0.8f).turbulence(0.15f, 0.54f));
		fullPower.setHairType("base");
		setDefaultMasteryValues(fullPower);
		fullPower.setStackDrainMultiplier(2.0);
		fullPower.setIncompatibleWith(List.of(""));

		FormConfig.FormData superNamekian = new FormConfig.FormData();
		superNamekian.setName(NamekianForms.SUPER_NAMEKIAN);
		superNamekian.setUnlockOnSkillLevel(3);
		superNamekian.setSagaRequisite("buu_saga:35");
		superNamekian.setCustomModel("namekian_buffed");
		superNamekian.setKeepBaseFormHeadBones(true);
		superNamekian.setAuraColor("#7FFF00");
		superNamekian.setHasLightnings(true);
		superNamekian.setLightningColor("#FFFFFF");
		superNamekian.setModelScaling(new Float[]{1.05f, 1.05f, 1.05f});
		superNamekian.setStrMultiplier(4.75);
		superNamekian.setSkpMultiplier(4.75);
		superNamekian.setDefMultiplier(3.5625);
		superNamekian.setPwrMultiplier(4.75);
		superNamekian.setEnergyDrain(0.27);
		superNamekian.setHairType("base");
		setDefaultMasteryValues(superNamekian);
		superNamekian.setAura3DStyle(aura3D().waves(1.9f, 4.8f, 0.7f).turbulence(0.12f, 0.52f).rim(0.04f, 1.0f, 3.2f, 0.07f).colors("", "", "#E8FFB0"));
		superNamekian.setStackDrainMultiplier(2.0);
		superNamekian.setIncompatibleWith(List.of("ultimate.ultimate"));

		Map<String, FormConfig.FormData> namekianFormData = new LinkedHashMap<>();
		otherworldDrain(giantForm, TIER_1);
		otherworldDrain(fullPower, TIER_2);
		otherworldDrain(superNamekian, TIER_3);
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
		evilNamek.setStrMultiplier(3.75);
		evilNamek.setSkpMultiplier(3.75);
		evilNamek.setDefMultiplier(2.8625);
		evilNamek.setPwrMultiplier(3.75);
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
		evilNamek.setAura3DStyle(darkAura("#FF0000").size(1.12f, 1.2f, 1.03f));
		evilNamek.setStackDrainMultiplier(2.0);
		evilNamek.setAllowFreeTransformOnMastery(0.0);

		FormConfig.FormData evilGiant = new FormConfig.FormData();
		evilGiant.setName(NamekianForms.EVIL_GIANT_NAMEK);
		evilGiant.setUnlockOnSkillLevel(2);
		evilGiant.setCustomModel("namekian_buffed");
        evilGiant.setFormCombo("dragonminez:giant");
		evilGiant.setStrMultiplier(4.8);
		evilGiant.setSkpMultiplier(4.8);
		evilGiant.setDefMultiplier(3.5875);
		evilGiant.setPwrMultiplier(4.8);
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
		evilGiant.setAura3DStyle(darkAura("#FF0000"));
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
		buffedNamek.setStrMultiplier(5.6);
		buffedNamek.setSkpMultiplier(5.6);
		buffedNamek.setDefMultiplier(4.0125);
		buffedNamek.setPwrMultiplier(5.6);
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
		buffedNamek.setAura3DStyle(darkAura("#FF0000").size(1.14f, 1.2f, 1.05f).waves(2.1f, 5.2f, 0.8f));
		buffedNamek.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> namekianLegendaryData = new LinkedHashMap<>();
		otherworldDrain(evilNamek, TIER_1);
		otherworldDrain(evilGiant, TIER_2);
		otherworldDrain(buffedNamek, TIER_3);
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
		second.setSagaRequisite("classic_saga:46");
		second.setTransformationAnimation("transf.freezer");
		second.setCustomModel("frostdemon_second");
		second.setKeepBaseFormHeadBones(true);
		second.setModelScaling(new Float[]{1.3f, 1.3f, 1.3f});
		second.setStrMultiplier(1.75);
		second.setSkpMultiplier(1.75);
		second.setDefMultiplier(1.425);
		second.setPwrMultiplier(1.75);
		second.setHairType("base");
		setDefaultMasteryValues(second);
		second.setStackDrainMultiplier(2.0);
		second.setAllowFreeTransformOnMastery(0.0);
		second.setIncompatibleWith(List.of(""));

		FormConfig.FormData third = new FormConfig.FormData();
		third.setName(FrostDemonForms.THIRD_FORM);
		third.setUnlockOnSkillLevel(2);
		third.setSagaRequisite("saiyan_saga:12");
		third.setTransformationAnimation("transf.freezer");
		third.setCustomModel("frostdemon_third");
		third.setModelScaling(new Float[]{1.4f, 1.4f, 1.4f});
		third.setStrMultiplier(2.25);
		third.setSkpMultiplier(2.25);
		third.setDefMultiplier(1.9375);
		third.setPwrMultiplier(2.25);
		third.setHairType("base");
		setDefaultMasteryValues(third);
		third.setStackDrainMultiplier(2.0);
		third.setIncompatibleWith(List.of(""));

		FormConfig.FormData finalForm = new FormConfig.FormData();
		finalForm.setName(FrostDemonForms.FINAL_FORM);
		finalForm.setUnlockOnSkillLevel(3);
		finalForm.setSagaRequisite("frieza_saga:10");
		second.setTransformationAnimation("transf.freezer2");
		finalForm.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
		finalForm.setStrMultiplier(2.85);
		finalForm.setSkpMultiplier(2.85);
		finalForm.setDefMultiplier(2.3625);
		finalForm.setPwrMultiplier(2.85);
		finalForm.setHairType("base");
		setDefaultMasteryValues(finalForm);
		finalForm.setStackDrainMultiplier(2.0);
		finalForm.setIncompatibleWith(List.of(""));

		FormConfig.FormData fullPower = new FormConfig.FormData();
		fullPower.setName(FrostDemonForms.FULLPOWER);
		fullPower.setUnlockOnSkillLevel(4);
		fullPower.setSagaRequisite("buu_saga:1");
		second.setTransformationAnimation("transf.freezer2");
		fullPower.setCustomModel("frostdemon_fp");
		fullPower.setModelScaling(new Float[]{1.3f, 1.2f, 1.3f});
		fullPower.setStrMultiplier(3.45);
		fullPower.setSkpMultiplier(3.45);
		fullPower.setDefMultiplier(2.725);
		fullPower.setPwrMultiplier(3.45);
		fullPower.setEnergyDrain(0.22);
		fullPower.setAura3DStyle(aura3D().waves(2.1f, 5.6f, 0.82f).turbulence(0.2f, 0.55f));
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
		fifthForm.setSagaRequisite("buu_saga:35");
		second.setTransformationAnimation("transf.freezer");
		fifthForm.setCustomModel("frostdemon_fifth");
		fifthForm.setModelScaling(new Float[]{1.4f, 1.3f, 1.4f});
		fifthForm.setStrMultiplier(4.9);
		fifthForm.setSkpMultiplier(4.9);
		fifthForm.setDefMultiplier(3.6875);
		fifthForm.setPwrMultiplier(4.9);
		fifthForm.setEnergyDrain(0.28);
		fifthForm.setEye1Color("#D91E1E");
		fifthForm.setEye2Color("#D91E1E");
		fifthForm.setHasLightnings(true);
		fifthForm.setLightningColor("#F02B16");
		fifthForm.setHairType("base");
		setDefaultMasteryValues(fifthForm);
		fifthForm.setAura3DStyle(aura3D().size(1.12f, 1.2f, 1.04f).waves(2.1f, 5.6f, 0.84f).turbulence(0.25f, 0.55f));
		fifthForm.setStackDrainMultiplier(2.0);
		fifthForm.setIncompatibleWith(List.of("ultimate.ultimate"));

		Map<String, FormConfig.FormData> frostFormData = new LinkedHashMap<>();
		otherworldDrain(second, TIER_1);
		otherworldDrain(third, GRADE_2);
		otherworldDrain(finalForm, TIER_2);
		otherworldDrain(fullPower, TIER_3);
		otherworldDrain(fifthForm, TIER_4);
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
		mecha.setStrMultiplier(3.9);
		mecha.setSkpMultiplier(3.9);
		mecha.setDefMultiplier(2.9375);
		mecha.setPwrMultiplier(3.9);
		mecha.setHairType("base");
        mecha.setHasLightnings(true);
        mecha.setLightningColor("#FF6052");
		setDefaultMasteryValues(mecha);
		mecha.setAura3DStyle(aura3D().waves(1.9f, 4.4f, 0.6f).turbulence(0.05f, 0.52f));
		mecha.setStackDrainMultiplier(2.0);
		mecha.setAllowFreeTransformOnMastery(0.0);

		FormConfig.FormData metal = new FormConfig.FormData();
		metal.setName(FrostDemonForms.METAL);
		metal.setUnlockOnSkillLevel(2);
		metal.setCustomModel("frostdemon_final");
		metal.setStrMultiplier(4.9);
		metal.setSkpMultiplier(4.9);
		metal.setDefMultiplier(3.6625);
		metal.setPwrMultiplier(4.9);
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
		metal.setAura3DStyle(aura3D().waves(1.9f, 4.5f, 0.65f).turbulence(0.1f, 0.52f).rim(0.03f, 1.0f, 3.0f, 0.07f).colors("", "", "#B5FDFF"));
		metal.setStackDrainMultiplier(2.0);

		FormConfig.FormData metalCore = new FormConfig.FormData();
		metalCore.setName(FrostDemonForms.METAL_CORE);
		metalCore.setUnlockOnSkillLevel(3);
		metalCore.setCustomModel("frostdemon_metalcore");
        metalCore.setFormCombo("dragonminez:giant");
		metalCore.setStrMultiplier(5.7);
		metalCore.setSkpMultiplier(5.7);
		metalCore.setDefMultiplier(4.0875);
		metalCore.setPwrMultiplier(5.7);
		metalCore.setEnergyDrain(-0.1);
		metalCore.setHairType("base");
		metalCore.setAttackSpeed(0.25);
        metalCore.setModelScaling(new Float[]{3.8f, 3.8f, 3.8f});
        setDefaultMasteryValues(metalCore);
		metalCore.setAura3DStyle(aura3D().waves(1.9f, 4.5f, 0.65f).turbulence(0.1f, 0.52f).rim(0.03f, 1.0f, 3.0f, 0.07f).colors("", "", "#B5FDFF"));
		metalCore.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> frostLegendaryData = new LinkedHashMap<>();
		otherworldDrain(mecha, TIER_1);
		otherworldDrain(metal, TIER_2);
		otherworldDrain(metalCore, TIER_3);
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
		kid.setSagaRequisite("classic_saga:46");
		kid.setCustomModel("majin_kid");
		kid.setKeepBaseFormHeadBones(true);
		kid.setModelScaling(new Float[]{0.7f, 0.7f, 0.7f});
		kid.setStrMultiplier(1.85);
		kid.setSkpMultiplier(1.85);
		kid.setDefMultiplier(1.575);
		kid.setPwrMultiplier(1.85);
		kid.setHairType("base");
		setDefaultMasteryValues(kid);
		kid.setAura3DStyle(aura3D().waves(2.3f, 6.2f, 0.82f).turbulence(0.3f, 0.56f));
		kid.setStackDrainMultiplier(2.0);
		kid.setAllowFreeTransformOnMastery(0.0);
		kid.setIncompatibleWith(List.of(""));

		FormConfig.FormData evil = new FormConfig.FormData();
		evil.setName(MajinForms.EVIL);
		evil.setUnlockOnSkillLevel(2);
		evil.setSagaRequisite("frieza_saga:5");
		evil.setCustomModel("majin_evil");
		evil.setKeepBaseFormHeadBones(true);
		evil.setModelScaling(new Float[]{0.9f, 1.0f, 0.9f});
		evil.setStrMultiplier(2.45);
		evil.setSkpMultiplier(2.45);
		evil.setDefMultiplier(2.15);
		evil.setPwrMultiplier(2.45);
		evil.setHairColor("#917979");
		evil.setEye1Color("#F52746");
		evil.setEye2Color("#F52746");
		evil.setBodyColor1("#917979");
		evil.setBodyColor2("#917979");
		evil.setBodyColor3("#917979");
		evil.setHairType("base");
		setDefaultMasteryValues(evil);
		evil.setAura3DStyle(aura3D().waves(2.1f, 5.4f, 0.8f).turbulence(0.4f, 0.55f).colors("", "", "#FF2A6D"));
		evil.setStackDrainMultiplier(2.0);
		evil.setIncompatibleWith(List.of(""));

		FormConfig.FormData superForm = new FormConfig.FormData();
        superForm.setCustomModel("majin_super");
		superForm.setName(MajinForms.SUPER);
		superForm.setUnlockOnSkillLevel(3);
		superForm.setSagaRequisite("buu_saga:1");
		superForm.setKeepBaseFormHeadBones(true);
		superForm.setModelScaling(new Float[]{1.0f, 1.0f, 1.0f});
		superForm.setStrMultiplier(3.3);
		superForm.setSkpMultiplier(3.3);
		superForm.setDefMultiplier(2.8);
		superForm.setPwrMultiplier(3.3);
		superForm.setHairType("base");
		setDefaultMasteryValues(superForm);
		superForm.setStackDrainMultiplier(2.0);
		superForm.setIncompatibleWith(List.of(""));

		FormConfig.FormData ultra = new FormConfig.FormData();
		ultra.setName(MajinForms.ULTRA);
		ultra.setUnlockOnSkillLevel(4);
		ultra.setSagaRequisite("buu_saga:35");
		ultra.setCustomModel("majin_ultra");
		ultra.setKeepBaseFormHeadBones(true);
		ultra.setModelScaling(new Float[]{1.3f, 1.2f, 1.3f});
		ultra.setStrMultiplier(4.75);
		ultra.setSkpMultiplier(4.75);
		ultra.setDefMultiplier(3.725);
		ultra.setPwrMultiplier(4.75);
		ultra.setEnergyDrain(0.22);
		ultra.setStaminaDrainMultiplier(3.5);
		ultra.setHasLightnings(true);
		ultra.setLightningColor("#F02B16");
		ultra.setHairType("base");
		setDefaultMasteryValues(ultra);
		ultra.setAura3DStyle(aura3D().size(1.14f, 1.2f, 1.06f).waves(2.1f, 5.4f, 0.85f).turbulence(0.25f, 0.55f));
		ultra.setStackDrainMultiplier(2.0);
		ultra.setIncompatibleWith(List.of("ultimate.ultimate"));

		Map<String, FormConfig.FormData> majinFormData = new LinkedHashMap<>();
		otherworldDrain(kid, TIER_1);
		otherworldDrain(evil, TIER_2);
		otherworldDrain(superForm, TIER_3);
		otherworldDrain(ultra, TIER_4);
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
		innocence.setCustomModel("janemba_imperfect");
		innocence.setStrMultiplier(3.75);
		innocence.setSkpMultiplier(3.75);
		innocence.setDefMultiplier(2.8625);
		innocence.setPwrMultiplier(3.75);
		innocence.setHairType("empty");
        innocence.setModelScaling(new Float[]{1.4f, 1.4f, 1.4f});
        setDefaultMasteryValues(innocence);
		innocence.setAura3DStyle(aura3D().waves(2.2f, 5.6f, 0.8f).turbulence(0.35f, 0.55f).colors("", "", "#FF61D8"));
		innocence.setStackDrainMultiplier(2.0);
		innocence.setAllowFreeTransformOnMastery(0.0);

        FormConfig.FormData giant_innocence_demon = new FormConfig.FormData();
        giant_innocence_demon.setName(MajinForms.GIANT_INNOCENCE_DEMON);
        giant_innocence_demon.setUnlockOnSkillLevel(2);
		giant_innocence_demon.setTransformationAnimation("transf.janemba");
        giant_innocence_demon.setFormCombo("dragonminez:giant");
        giant_innocence_demon.setCustomModel("janemba_fat");
		giant_innocence_demon.setStrMultiplier(4.8);
		giant_innocence_demon.setSkpMultiplier(4.8);
		giant_innocence_demon.setDefMultiplier(3.5875);
		giant_innocence_demon.setPwrMultiplier(4.8);
        giant_innocence_demon.setAttackSpeed(0.25);
        giant_innocence_demon.setEnergyDrain(0.25);
        giant_innocence_demon.setHairType("empty");
        giant_innocence_demon.setBodyColor1("#FFFC82");
        giant_innocence_demon.setBodyColor2("#FFB8FD");
        giant_innocence_demon.setEye1Color("#303030");
        giant_innocence_demon.setEye2Color("#303030");
        giant_innocence_demon.setModelScaling(new Float[]{3.8f, 3.8f, 3.8f});
        setDefaultMasteryValues(giant_innocence_demon);
        giant_innocence_demon.setAura3DStyle(aura3D().waves(2.2f, 5.2f, 0.8f).turbulence(0.35f, 0.55f).colors("", "", "#FF61D8"));
        giant_innocence_demon.setStackDrainMultiplier(2.0);

		FormConfig.FormData superDemon = new FormConfig.FormData();
		superDemon.setName(MajinForms.SUPER_DEMON);
		superDemon.setUnlockOnSkillLevel(3);
		superDemon.setTransformationAnimation("transf.janemba");
		superDemon.setCustomModel("janemba_super");
		superDemon.setStrMultiplier(5.6);
		superDemon.setSkpMultiplier(5.6);
		superDemon.setDefMultiplier(4.0125);
		superDemon.setPwrMultiplier(5.6);
		superDemon.setHairType("empty");
        superDemon.setBodyColor1("#FF6161");
        superDemon.setBodyColor2("#E1A8FF");
        superDemon.setEye1Color("#FFE98A");
        superDemon.setEye2Color("#303030");
        superDemon.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
        setDefaultMasteryValues(superDemon);
        superDemon.setAura3DStyle(aura3D().size(1.12f, 1.2f, 1.04f).waves(2.2f, 5.6f, 0.84f).turbulence(0.4f, 0.55f).colors("", "", "#FF61D8"));
		superDemon.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> majinLegendaryData = new LinkedHashMap<>();
		otherworldDrain(innocence, TIER_1);
		otherworldDrain(giant_innocence_demon, TIER_2);
		otherworldDrain(superDemon, TIER_3);
		majinLegendaryData.put(MajinForms.INNOCENCE_DEMON, innocence);
        majinLegendaryData.put(MajinForms.GIANT_INNOCENCE_DEMON, giant_innocence_demon);
        majinLegendaryData.put(MajinForms.SUPER_DEMON, superDemon);
		applySequentialMasteryRequisites(MajinForms.GROUP_LEGENDARYFORMS, majinLegendaryData);
		majinLegendaryForms.setForms(majinLegendaryData);

		forms.put(MajinForms.GROUP_LEGENDARYFORMS, majinLegendaryForms);
		LogUtil.info(Env.COMMON, "Default Majin legendary forms created");
	}

	private static FormConfig.FormData.OutlineShaderConfig outline(String primary, String secondary, double thickness) {
		FormConfig.FormData.OutlineShaderConfig outline = new FormConfig.FormData.OutlineShaderConfig();
		outline.setEnabled(true);
		outline.setPrimaryColor(primary);
		outline.setSecondaryColor(secondary);
		outline.setOutlineThickness(thickness);
		return outline;
	}

	private void createGlindForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig glindForms = new FormConfig();
		glindForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		glindForms.setGroupName(GlindForms.GROUP_SUPERFORMS);
		glindForms.setFormType("superforms");

		FormConfig.FormData buffed = new FormConfig.FormData();
		buffed.setName(GlindForms.BUFFED);
		buffed.setUnlockOnSkillLevel(1);
		buffed.setSagaRequisite("classic_saga:46");
		buffed.setCustomModel("buffed");
		buffed.setModelScaling(new Float[]{1.05f, 1.05f, 1.05f});
		buffed.setStrMultiplier(1.75);
		buffed.setSkpMultiplier(1.75);
		buffed.setDefMultiplier(1.425);
		buffed.setPwrMultiplier(1.75);
		buffed.setEnergyDrain(0.08);
		buffed.setHairType("base");
		setDefaultMasteryValues(buffed);
		buffed.setAura3DStyle(aura3D().waves(2.0f, 5.0f, 0.78f).turbulence(0.12f, 0.54f));
		buffed.setStackDrainMultiplier(2.0);
		buffed.setAllowFreeTransformOnMastery(0.0);
		buffed.setIncompatibleWith(List.of(""));

		FormConfig.FormData outOfControl = new FormConfig.FormData();
		outOfControl.setOutlineShader(outline("#FF2B2B", "#7D0202", 3.5));
		outOfControl.setName(GlindForms.OUT_OF_CONTROL);
		outOfControl.setUnlockOnSkillLevel(2);
		outOfControl.setSagaRequisite("saiyan_saga:12");
		outOfControl.setTransformationAnimation("transf.berserker");
		outOfControl.setCustomModel("buffed");
		outOfControl.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
		outOfControl.setStrMultiplier(2.25);
		outOfControl.setSkpMultiplier(2.25);
		outOfControl.setDefMultiplier(1.9375);
		outOfControl.setPwrMultiplier(2.25);
		outOfControl.setEnergyDrain(0.16);
		outOfControl.setEye1Color("#E01414");
		outOfControl.setEye2Color("#E01414");
		outOfControl.setAuraColor("#E01B1B");
		outOfControl.setHairType("base");
		setDefaultMasteryValues(outOfControl);
		outOfControl.setAura3DStyle(aura3D().waves(2.1f, 5.4f, 0.8f).turbulence(0.35f, 0.55f).colors("", "", "#FF3A3A"));
		outOfControl.setStackDrainMultiplier(2.0);
		outOfControl.setIncompatibleWith(List.of(""));

		FormConfig.FormData superForm = new FormConfig.FormData();
		superForm.setOutlineShader(outline("#5AA9FF", "#1238C9", 3.5));
		superForm.setName(GlindForms.SUPER);
		superForm.setUnlockOnSkillLevel(3);
		superForm.setSagaRequisite("frieza_saga:10");
		superForm.setTransformationAnimation("transf.berserker");
		superForm.setCustomModel("buffed");
		superForm.setModelScaling(new Float[]{1.2f, 1.1f, 1.2f});
		superForm.setStrMultiplier(2.85);
		superForm.setSkpMultiplier(2.85);
		superForm.setDefMultiplier(2.3625);
		superForm.setPwrMultiplier(2.85);
		superForm.setEnergyDrain(0.22);
		superForm.setEye1Color("#E01414");
		superForm.setEye2Color("#E01414");
		superForm.setAuraColor("#2F6BFF");
		superForm.setHairType("base");
		setDefaultMasteryValues(superForm);
		superForm.setAura3DStyle(aura3D().size(1.12f, 1.2f, 1.04f).waves(2.1f, 5.6f, 0.82f).turbulence(0.2f, 0.55f).colors("", "", "#9CC8FF"));
		superForm.setStackDrainMultiplier(2.0);
		superForm.setIncompatibleWith(List.of(""));

		FormConfig.FormData absorption = new FormConfig.FormData();
		absorption.setName(GlindForms.ABSORPTION);
		absorption.setUnlockOnSkillLevel(4);
		absorption.setSagaRequisite("buu_saga:1");
		absorption.setCustomModel("ssj4gt");
		absorption.setHideTail(true);
		absorption.setFurUsesHairColor(true);
		absorption.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
		absorption.setStrMultiplier(3.45);
		absorption.setSkpMultiplier(3.45);
		absorption.setDefMultiplier(2.725);
		absorption.setPwrMultiplier(3.45);
		absorption.setEnergyDrain(0.24);
		absorption.setAuraColor("#2F6BFF");
		absorption.setHairType("ssj4");
		setDefaultMasteryValues(absorption);
		absorption.setAura3DStyle(aura3D().waves(2.0f, 5.0f, 0.78f).turbulence(0.13f, 0.54f).colors("", "", "#9CC8FF"));
		absorption.setStackDrainMultiplier(2.0);
		absorption.setIncompatibleWith(List.of(""));

		FormConfig.FormData trueForm = new FormConfig.FormData();
		trueForm.setName(GlindForms.TRUE_FORM);
		trueForm.setUnlockOnSkillLevel(5);
		trueForm.setSagaRequisite("buu_saga:35");
		trueForm.setCustomModel("glindtrueform");
		trueForm.setModelScaling(new Float[]{1.95f, 2.1f, 1.95f});
		trueForm.setStrMultiplier(4.9);
		trueForm.setSkpMultiplier(4.9);
		trueForm.setDefMultiplier(3.6875);
		trueForm.setPwrMultiplier(4.9);
		trueForm.setEnergyDrain(0.28);
		trueForm.setHairColor("#BE3C48");
		trueForm.setBodyColor1("#247FA2");
		trueForm.setBodyColor2("#D2D2C6");
		trueForm.setAuraColor("#7B2FBE");
        trueForm.setEye1Color("#AB0F0F");
        trueForm.setEye2Color("#AB0F0F");
        trueForm.setHairType("ssj3");
		setDefaultMasteryValues(trueForm);
		trueForm.setAura3DStyle(darkAura("#B05CFF").size(1.12f, 1.2f, 1.04f).waves(2.1f, 5.2f, 0.8f).turbulence(0.45f, 0.54f));
		trueForm.setStackDrainMultiplier(2.0);
		trueForm.setIncompatibleWith(List.of("ultimate.ultimate"));

		Map<String, FormConfig.FormData> glindFormData = new LinkedHashMap<>();
		otherworldDrain(buffed, TIER_1);
		otherworldDrain(outOfControl, GRADE_2);
		otherworldDrain(superForm, TIER_2);
		otherworldDrain(absorption, TIER_3);
		otherworldDrain(trueForm, TIER_4);
		glindFormData.put(GlindForms.BUFFED, buffed);
		glindFormData.put(GlindForms.OUT_OF_CONTROL, outOfControl);
		glindFormData.put(GlindForms.SUPER, superForm);
		glindFormData.put(GlindForms.ABSORPTION, absorption);
		glindFormData.put(GlindForms.TRUE_FORM, trueForm);
		applySequentialMasteryRequisites(GlindForms.GROUP_SUPERFORMS, glindFormData);
		glindForms.setForms(glindFormData);

		forms.put(GlindForms.GROUP_SUPERFORMS, glindForms);
		LogUtil.info(Env.COMMON, "Default Glind forms created");

		FormConfig glindLegendaryForms = new FormConfig();
		glindLegendaryForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		glindLegendaryForms.setGroupName(GlindForms.GROUP_LEGENDARYFORMS);
		glindLegendaryForms.setFormType("legendaryforms");

		FormConfig.FormData transcended = new FormConfig.FormData();
		transcended.setName(GlindForms.TRANSCENDED);
		transcended.setUnlockOnSkillLevel(1);
		transcended.setCustomModel("trascended");
		transcended.setModelScaling(new Float[]{1.4f, 1.4f, 1.4f});
		transcended.setStrMultiplier(3.75);
		transcended.setSkpMultiplier(3.75);
		transcended.setDefMultiplier(2.8625);
		transcended.setPwrMultiplier(3.75);
		transcended.setEnergyDrain(0.22);
		transcended.setHairColor("#BE3C48");
		transcended.setBodyColor1("#D2D2C6");
		transcended.setBodyColor2("#247FA2");
		transcended.setEye1Color("#FFD700");
		transcended.setEye2Color("#FFD700");
		transcended.setAuraColor("#7B2FBE");
		transcended.setHairType("ssj2");
		transcended.setKeepBaseFormHeadBones(true);
		setDefaultMasteryValues(transcended);
		transcended.setAura3DStyle(darkAura("#B05CFF").size(1.12f, 1.2f, 1.03f).waves(2.1f, 5.2f, 0.8f).turbulence(0.45f, 0.54f));
		transcended.setStackDrainMultiplier(2.0);
		transcended.setAllowFreeTransformOnMastery(0.0);

		FormConfig.FormData darkKing = new FormConfig.FormData();
		darkKing.setOutlineShader(outline("#FF1A1A", "#8A0000", 3.5));
		darkKing.setName(GlindForms.DARK_KING);
		darkKing.setUnlockOnSkillLevel(2);
		darkKing.setCustomModel("buffedg3");
		darkKing.setKeepBaseFormHeadBones(true);
		darkKing.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
		darkKing.setStrMultiplier(4.8);
		darkKing.setSkpMultiplier(4.8);
		darkKing.setDefMultiplier(3.5875);
		darkKing.setPwrMultiplier(4.8);
		darkKing.setEnergyDrain(0.28);
		darkKing.setHairColor("#F2234B");
		darkKing.setBodyColor1("#D2D2C6");
		darkKing.setBodyColor2("#3C3A3F");
		darkKing.setEye1Color("#E01414");
		darkKing.setEye2Color("#E01414");
		darkKing.setAuraColor("#B00000");
		darkKing.setHairType("ssj2");
		setDefaultMasteryValues(darkKing);
		darkKing.setAura3DStyle(aura3D().size(1.14f, 1.2f, 1.05f).waves(2.1f, 5.4f, 0.82f).turbulence(0.5f, 0.55f).rim(0.3f, 1.0f, 4.5f, 0.06f).colors("#050000", "#E01414", "#FF1A1A"));
		darkKing.setStackDrainMultiplier(2.0);

		FormConfig.FormData timePowerDarkKing = new FormConfig.FormData();
		timePowerDarkKing.setOutlineShader(outline("#000000", "#000000", 4.5));
		timePowerDarkKing.setName(GlindForms.TIME_POWER_DARK_KING);
		timePowerDarkKing.setUnlockOnSkillLevel(3);
		timePowerDarkKing.setCustomModel("glinddark");
		timePowerDarkKing.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
		timePowerDarkKing.setStrMultiplier(5.6);
		timePowerDarkKing.setSkpMultiplier(5.6);
		timePowerDarkKing.setDefMultiplier(4.0125);
		timePowerDarkKing.setPwrMultiplier(5.6);
		timePowerDarkKing.setEnergyDrain(0.34);
		timePowerDarkKing.setHairColor("#F2234B");
		timePowerDarkKing.setBodyColor1("#D2D2C6");
		timePowerDarkKing.setEye1Color("#E01414");
		timePowerDarkKing.setEye2Color("#E01414");
		timePowerDarkKing.setBodyColor2("#463E4A");
		timePowerDarkKing.setBodyColor3("#E82C3E");
		timePowerDarkKing.setAuraColor("#8E4A4A");
		timePowerDarkKing.setHairType("base");
		setDefaultMasteryValues(timePowerDarkKing);
		timePowerDarkKing.setAura3DStyle(darkAura("#C9A3A3").size(1.18f, 1.24f, 1.08f).waves(2.1f, 5.4f, 0.82f).turbulence(0.5f, 0.55f));
		timePowerDarkKing.setStackDrainMultiplier(2.0);
		timePowerDarkKing.setShareMasteryWith(List.of(GlindForms.GROUP_LEGENDARYFORMS + "." + GlindForms.TIME_POWER_REALM_OF_LIGHT));
		timePowerDarkKing.setShareMasteryMultiplier(0.5);

		FormConfig.FormData timePowerRealmOfLight = new FormConfig.FormData();
		timePowerRealmOfLight.setOutlineShader(outline("#FFFFFF", "#FFF3C4", 3.5));
		timePowerRealmOfLight.setName(GlindForms.TIME_POWER_REALM_OF_LIGHT);
		timePowerRealmOfLight.setUnlockOnSkillLevel(3);
		timePowerRealmOfLight.setCustomModel("");
		timePowerRealmOfLight.setModelScaling(new Float[]{1.35f, 1.45f, 1.35f});
		timePowerRealmOfLight.setStrMultiplier(5.6);
		timePowerRealmOfLight.setSkpMultiplier(5.6);
		timePowerRealmOfLight.setDefMultiplier(4.0125);
		timePowerRealmOfLight.setPwrMultiplier(5.6);
		timePowerRealmOfLight.setEnergyDrain(0.34);
		timePowerRealmOfLight.setAuraColor("#FFF3C4");
		timePowerRealmOfLight.setHairType("base");
		setDefaultMasteryValues(timePowerRealmOfLight);
		timePowerRealmOfLight.setAura3DStyle(aura3D().size(1.18f, 1.24f, 1.08f).waves(1.9f, 4.6f, 0.7f).turbulence(0.1f, 0.52f).rim(0.03f, 0.9f, 3.0f, 0.08f).colors("", "", "#FFFFFF"));
		timePowerRealmOfLight.setStackDrainMultiplier(2.0);
		timePowerRealmOfLight.setShareMasteryWith(List.of(GlindForms.GROUP_LEGENDARYFORMS + "." + GlindForms.TIME_POWER_DARK_KING));
		timePowerRealmOfLight.setShareMasteryMultiplier(0.5);

		Map<String, FormConfig.FormData> glindLegendaryData = new LinkedHashMap<>();
		otherworldDrain(transcended, TIER_1);
		otherworldDrain(darkKing, TIER_2);
		otherworldDrain(timePowerDarkKing, TIER_3);
		otherworldDrain(timePowerRealmOfLight, TIER_3);
		glindLegendaryData.put(GlindForms.TRANSCENDED, transcended);
		glindLegendaryData.put(GlindForms.DARK_KING, darkKing);
		glindLegendaryData.put(GlindForms.TIME_POWER_DARK_KING, timePowerDarkKing);
		glindLegendaryData.put(GlindForms.TIME_POWER_REALM_OF_LIGHT, timePowerRealmOfLight);
		applySequentialMasteryRequisites(GlindForms.GROUP_LEGENDARYFORMS, glindLegendaryData);
		timePowerRealmOfLight.setFormRequisite(GlindForms.GROUP_LEGENDARYFORMS + "." + GlindForms.DARK_KING);
		glindLegendaryForms.setForms(glindLegendaryData);

		forms.put(GlindForms.GROUP_LEGENDARYFORMS, glindLegendaryForms);
		LogUtil.info(Env.COMMON, "Default Glind legendary forms created");
	}

	private void createBioAndroidForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig bioForms = new FormConfig();
		bioForms.setConfigVersion(FormConfig.CURRENT_VERSION);
		bioForms.setGroupName(BioAndroidForms.GROUP_BIOEVOLUTION);
		bioForms.setFormType("superforms");

		FormConfig.FormData semiPerfect = new FormConfig.FormData();
		semiPerfect.setName(BioAndroidForms.SEMI_PERFECT);
		semiPerfect.setUnlockOnSkillLevel(1);
		semiPerfect.setSagaRequisite("classic_saga:46");
		semiPerfect.setCustomModel("bioandroid_semi");
		semiPerfect.setModelScaling(new Float[]{1.3f, 1.3f, 1.3f});
		semiPerfect.setStrMultiplier(1.85);
		semiPerfect.setSkpMultiplier(1.85);
		semiPerfect.setDefMultiplier(1.575);
		semiPerfect.setPwrMultiplier(1.85);
		semiPerfect.setHairColor("");
		semiPerfect.setEye1Color("#BA1414");
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
		perfect.setSagaRequisite("frieza_saga:5");
		perfect.setCustomModel("bioandroid_perfect");
		perfect.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
		perfect.setStrMultiplier(2.6);
		perfect.setSkpMultiplier(2.6);
		perfect.setDefMultiplier(2.225);
		perfect.setPwrMultiplier(2.6);
		perfect.setEye1Color("#BA1414");
        perfect.setEye2Color("#FFFFFF");
        perfect.setBodyColor2("#FFFFFF");
        perfect.setBodyColor3("#FFED61");
        perfect.setAuraColor("#FFFF69");
        perfect.setHairColor("#7C28B0");
		perfect.setHairType("base");
		setDefaultMasteryValues(perfect);
		perfect.setAura3DStyle(aura3D().waves(1.9f, 4.6f, 0.68f).turbulence(0.1f, 0.52f).rim(0.04f, 1.0f, 3.4f, 0.07f));
		perfect.setStackDrainMultiplier(2.0);
		perfect.setIncompatibleWith(List.of(""));

		FormConfig.FormData superPerfect = new FormConfig.FormData();
		superPerfect.setName(BioAndroidForms.SUPER_PERFECT);
		superPerfect.setUnlockOnSkillLevel(3);
		superPerfect.setSagaRequisite("buu_saga:1");
		superPerfect.setCustomModel("bioandroid_perfect");
		superPerfect.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
		superPerfect.setStrMultiplier(3.35);
		superPerfect.setSkpMultiplier(3.35);
		superPerfect.setDefMultiplier(2.65);
		superPerfect.setPwrMultiplier(3.35);
		superPerfect.setEnergyDrain(0.16);
		superPerfect.setEye1Color("#BA1414");
		superPerfect.setEye2Color("#FFFFFF");
		superPerfect.setBodyColor1("");
		superPerfect.setBodyColor2("#FFFFFF");
		superPerfect.setBodyColor3("#FFED61");
		superPerfect.setAuraColor("#FFFF69");
        superPerfect.setHairColor("#7C28B0");
		superPerfect.setHasLightnings(true);
		superPerfect.setLightningColor("#1AA1C7");
		superPerfect.setHairType("base");
		setDefaultMasteryValues(superPerfect);
		superPerfect.setAura3DStyle(aura3D().waves(2.0f, 5.2f, 0.78f).turbulence(0.16f, 0.54f));
		superPerfect.setStackDrainMultiplier(2.0);
		superPerfect.setIncompatibleWith(List.of(""));

		FormConfig.FormData ultraperfect = new FormConfig.FormData();
		ultraperfect.setName(BioAndroidForms.ULTRA_PERFECT);
		ultraperfect.setUnlockOnSkillLevel(4);
		ultraperfect.setSagaRequisite("buu_saga:35");
		ultraperfect.setCustomModel("bioandroid_ultra");
		ultraperfect.setModelScaling(new Float[]{1.3f, 1.3f, 1.3f});
		ultraperfect.setStrMultiplier(4.9);
		ultraperfect.setSkpMultiplier(4.9);
		ultraperfect.setDefMultiplier(3.6875);
		ultraperfect.setPwrMultiplier(4.9);
		ultraperfect.setSpeedMultiplier(0.6);
		ultraperfect.setEnergyDrain(0.28);
		ultraperfect.setStaminaDrainMultiplier(3.5);
		ultraperfect.setAttackSpeed(0.55);
		ultraperfect.setEye1Color("#BA1414");
		ultraperfect.setEye2Color("#FFFFFF");
		ultraperfect.setBodyColor1("");
		ultraperfect.setBodyColor2("#FFFFFF");
		ultraperfect.setBodyColor3("#FFED61");
		ultraperfect.setAuraColor("#FFFF69");
        ultraperfect.setHairColor("#7C28B0");
        ultraperfect.setHasLightnings(true);
		ultraperfect.setLightningColor("#1AA1C7");
		ultraperfect.setHairType("base");
		setDefaultMasteryValues(ultraperfect);
		ultraperfect.setAura3DStyle(aura3D().size(1.18f, 1.22f, 1.08f).waves(2.0f, 4.6f, 0.86f).turbulence(0.22f, 0.54f));
		ultraperfect.setStackDrainMultiplier(2.0);
		ultraperfect.setIncompatibleWith(List.of("ultimate.ultimate"));

		Map<String, FormConfig.FormData> bioFormData = new LinkedHashMap<>();
		otherworldDrain(semiPerfect, TIER_1);
		otherworldDrain(perfect, TIER_2);
		otherworldDrain(superPerfect, TIER_3);
		otherworldDrain(ultraperfect, TIER_4);
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
		xeno.setStrMultiplier(3.9);
		xeno.setSkpMultiplier(3.9);
		xeno.setDefMultiplier(2.9375);
		xeno.setPwrMultiplier(3.9);
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
		xeno.setAura3DStyle(darkAura("#9D00FF"));
        xeno.setModelScaling(new Float[]{1.1f, 1.1f, 1.1f});
        xeno.setStackDrainMultiplier(2.0);
		xeno.setAllowFreeTransformOnMastery(0.0);

		FormConfig.FormData xenoFP = new FormConfig.FormData();
		xenoFP.setName(BioAndroidForms.XENO_FP);
		xenoFP.setUnlockOnSkillLevel(2);
		xenoFP.setCustomModel("bioandroid_xeno");
		xenoFP.setStrMultiplier(4.9);
		xenoFP.setSkpMultiplier(4.9);
		xenoFP.setDefMultiplier(3.6625);
		xenoFP.setPwrMultiplier(4.9);
		xenoFP.setEnergyDrain(0.16);
        xenoFP.setAuraColor("#2C0A4A");
        xenoFP.setHasLightnings(true);
        xenoFP.setLightningColor("#340063");
        xenoFP.setEye1Color("#BA1414");
        xenoFP.setEye2Color("#BA1414");
        xenoFP.setBodyColor2("#DBC8C8");
        xenoFP.setBodyColor3("#4C3554");
        xenoFP.setHairColor("#4C3554");
        xenoFP.setHairType("base");
		setDefaultMasteryValues(xenoFP);
		xenoFP.setAura3DStyle(darkAura("#9D00FF").size(1.12f, 1.2f, 1.03f).waves(2.1f, 5.2f, 0.8f));
        xenoFP.setModelScaling(new Float[]{1.3f, 1.3f, 1.3f});
        xenoFP.setStackDrainMultiplier(2.0);

		FormConfig.FormData xenoMax = new FormConfig.FormData();
		xenoMax.setName(BioAndroidForms.XENO_MAX);
		xenoMax.setUnlockOnSkillLevel(3);
		xenoMax.setCustomModel("bioandroid_xenofp");
		xenoMax.setStrMultiplier(5.7);
		xenoMax.setSkpMultiplier(5.7);
		xenoMax.setDefMultiplier(4.0875);
		xenoMax.setPwrMultiplier(5.7);
		xenoMax.setEnergyDrain(0.22);
        xenoMax.setAuraColor("#2C0A4A");
        xenoMax.setHasLightnings(true);
        xenoMax.setLightningColor("#340063");
        xenoMax.setEye1Color("#BA1414");
        xenoMax.setEye2Color("#BA1414");
        xenoMax.setBodyColor2("#DBC8C8");
        xenoMax.setBodyColor3("#4C3554");
        xenoMax.setHairColor("#4C3554");
        xenoMax.setHairType("base");
		setDefaultMasteryValues(xenoMax);
		xenoMax.setAura3DStyle(darkAura("#9D00FF"));
        xenoMax.setModelScaling(new Float[]{3.8f, 3.8f, 3.8f});
        xenoMax.setStackDrainMultiplier(2.0);

		Map<String, FormConfig.FormData> bioLegendaryData = new LinkedHashMap<>();
		otherworldDrain(xeno, TIER_1);
		otherworldDrain(xenoFP, TIER_2);
		otherworldDrain(xenoMax, TIER_3);
		bioLegendaryData.put(BioAndroidForms.XENO, xeno);
		bioLegendaryData.put(BioAndroidForms.XENO_FP, xenoFP);
		bioLegendaryData.put(BioAndroidForms.XENO_MAX, xenoMax);
		applySequentialMasteryRequisites(BioAndroidForms.GROUP_LEGENDARYFORMS, bioLegendaryData);
		bioLegendaryForms.setForms(bioLegendaryData);

		forms.put(BioAndroidForms.GROUP_LEGENDARYFORMS, bioLegendaryForms);
		LogUtil.info(Env.COMMON, "Default Bio Android legendary forms created");
	}
}
