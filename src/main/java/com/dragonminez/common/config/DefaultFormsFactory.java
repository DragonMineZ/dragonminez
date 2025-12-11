package com.dragonminez.common.config;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DefaultFormsFactory {

    private final ConfigLoader loader;

    public DefaultFormsFactory(Gson gson, ConfigLoader loader) {
        this.loader = loader;
    }

    private void setDefaultMasteryValues(FormConfig.FormData form) {
        form.setMaxMastery(100.0);
        form.setMasteryPerHit(0.05);
        form.setMasteryPerDamageReceived(0.05);
        form.setStatMultPerMasteryPoint(0.02);
        form.setCostDecreasePerMasteryPoint(0.02);
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

	private static void createDefaultHumanForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
		FormConfig superforms = new FormConfig();
		superforms.setGroupName("superforms");
		superforms.setFormType("super");
		superforms.setForms(new HashMap<>());
		forms.put("superforms", superforms);

		Path humanPath = formsPath.resolve("superforms.json");
		Files.writeString(humanPath, "{\"group_name\":\"superforms\",\"form_type\":\"super\",\"can_stack\":false,\"forms\":{}}", StandardCharsets.UTF_8);
		LogUtil.info(Env.COMMON, "Default Human forms created");
	}

    private void createSaiyanForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
        FormConfig oozaruForms = new FormConfig();
        oozaruForms.setGroupName("oozaru");
        oozaruForms.setFormType("super");

        FormConfig.FormData oozaru = new FormConfig.FormData();
        oozaru.setName("oozaru");
        oozaru.setUnlockOnSuperformLevel(0);
        oozaru.setCustomModel("oozaru");
        oozaru.setModelScaling(2.5f);
        oozaru.setStrMultiplier(1.5);
        oozaru.setSkpMultiplier(1.5);
        oozaru.setDefMultiplier(1.5);
        oozaru.setVitMultiplier(1.5);
        oozaru.setPwrMultiplier(1.5);
        oozaru.setEneMultiplier(1.5);
        oozaru.setSpeedMultiplier(0.8);
        oozaru.setEnergyDrain(1.5);
        oozaru.setStaminaDrain(1.2);
        oozaru.setAttackSpeed(0.9);
        setDefaultMasteryValues(oozaru);

        FormConfig.FormData goldenOozaru = new FormConfig.FormData();
        goldenOozaru.setName("goldenoozaru");
        goldenOozaru.setUnlockOnSuperformLevel(7);
        goldenOozaru.setCustomModel("oozaru");
        goldenOozaru.setHairColor("#FFD700");
        goldenOozaru.setAuraColor("#FFD700");
        goldenOozaru.setModelScaling(2.5f);
        goldenOozaru.setStrMultiplier(1.5);
        goldenOozaru.setSkpMultiplier(1.5);
        goldenOozaru.setDefMultiplier(1.5);
        goldenOozaru.setVitMultiplier(1.5);
        goldenOozaru.setPwrMultiplier(1.5);
        goldenOozaru.setEneMultiplier(1.5);
        goldenOozaru.setSpeedMultiplier(1.5);
        goldenOozaru.setEnergyDrain(1.5);
        goldenOozaru.setStaminaDrain(1.3);
        goldenOozaru.setAttackSpeed(1.1);
        setDefaultMasteryValues(goldenOozaru);

        FormConfig.FormData ssj4 = new FormConfig.FormData();
        ssj4.setName("supersaiyan4");
        ssj4.setUnlockOnSuperformLevel(7);
        ssj4.setHairColor("#000000");
        ssj4.setEye1Color("#FFD700");
        ssj4.setEye2Color("#FFD700");
        ssj4.setAuraColor("#FF0000");
        ssj4.setModelScaling(0.9375f);
        ssj4.setStrMultiplier(1.5);
        ssj4.setSkpMultiplier(1.5);
        ssj4.setDefMultiplier(1.5);
        ssj4.setVitMultiplier(1.5);
        ssj4.setPwrMultiplier(1.5);
        ssj4.setEneMultiplier(1.5);
        ssj4.setSpeedMultiplier(1.5);
        ssj4.setEnergyDrain(1.5);
        ssj4.setStaminaDrain(1.1);
        ssj4.setAttackSpeed(1.2);
        setDefaultMasteryValues(ssj4);

        Map<String, FormConfig.FormData> oozaruFormData = new LinkedHashMap<>();
        oozaruFormData.put("oozaru", oozaru);
        oozaruFormData.put("goldenoozaru", goldenOozaru);
        oozaruFormData.put("ssj4", ssj4);
        oozaruForms.setForms(oozaruFormData);

        FormConfig ssGrades = new FormConfig();
        ssGrades.setGroupName("ssgrades");
        ssGrades.setFormType("super");

        FormConfig.FormData ssj1 = new FormConfig.FormData();
        ssj1.setName("supersaiyan");
        ssj1.setUnlockOnSuperformLevel(1);
        ssj1.setHairColor("#FFD700");
        ssj1.setEye1Color("#00FFFF");
        ssj1.setEye2Color("#00FFFF");
        ssj1.setAuraColor("#FFD700");
        ssj1.setModelScaling(0.9375f);
        ssj1.setStrMultiplier(1.5);
        ssj1.setSkpMultiplier(1.5);
        ssj1.setStmMultiplier(1.5);
        ssj1.setDefMultiplier(1.5);
        ssj1.setVitMultiplier(1.5);
        ssj1.setPwrMultiplier(1.5);
        ssj1.setEneMultiplier(1.5);
        ssj1.setSpeedMultiplier(1.5);
        ssj1.setEnergyDrain(1.5);
        ssj1.setStaminaDrain(1.1);
        ssj1.setAttackSpeed(1.1);
        setDefaultMasteryValues(ssj1);

        FormConfig.FormData ssg2 = new FormConfig.FormData();
        ssg2.setName("supersaiyangrade2");
        ssg2.setUnlockOnSuperformLevel(2);
        ssg2.setHairColor("#FFD700");
        ssg2.setEye1Color("#00FFFF");
        ssg2.setEye2Color("#00FFFF");
        ssg2.setAuraColor("#FFD700");
        ssg2.setModelScaling(1.1f);
        ssg2.setStrMultiplier(1.5);
        ssg2.setSkpMultiplier(1.5);
        ssg2.setDefMultiplier(1.5);
        ssg2.setVitMultiplier(1.5);
        ssg2.setPwrMultiplier(1.5);
        ssg2.setSpeedMultiplier(1.2);
        ssg2.setEnergyDrain(1.5);
        ssg2.setStaminaDrain(1.3);
        ssg2.setAttackSpeed(1.15);
        setDefaultMasteryValues(ssg2);

        FormConfig.FormData ssg3 = new FormConfig.FormData();
        ssg3.setName("supersaiyangrade3");
        ssg3.setUnlockOnSuperformLevel(3);
        ssg3.setHairColor("#FFD700");
        ssg3.setEye1Color("#00FFFF");
        ssg3.setEye2Color("#00FFFF");
        ssg3.setAuraColor("#FFD700");
        ssg3.setModelScaling(1.2f);
        ssg3.setStrMultiplier(1.5);
        ssg3.setSkpMultiplier(1.5);
        ssg3.setDefMultiplier(1.5);
        ssg3.setSpeedMultiplier(1.0);
        ssg3.setEnergyDrain(1.5);
        ssg3.setStaminaDrain(1.5);
        ssg3.setAttackSpeed(1.3);
        setDefaultMasteryValues(ssg3);

        Map<String, FormConfig.FormData> ssGradeForms = new LinkedHashMap<>();
        ssGradeForms.put("ssj1", ssj1);
        ssGradeForms.put("grade2", ssg2);
        ssGradeForms.put("grade3", ssg3);
        ssGrades.setForms(ssGradeForms);

        FormConfig superSaiyan = new FormConfig();
        superSaiyan.setGroupName("supersaiyan");
        superSaiyan.setFormType("super");

        FormConfig.FormData ssj1Mastered = new FormConfig.FormData();
        ssj1Mastered.setName("supersaiyanmastered");
        ssj1Mastered.setUnlockOnSuperformLevel(4);
        ssj1Mastered.setHairColor("#FFD700");
        ssj1Mastered.setEye1Color("#00FFFF");
        ssj1Mastered.setEye2Color("#00FFFF");
        ssj1Mastered.setAuraColor("#FFD700");
        ssj1Mastered.setModelScaling(0.9375f);
        ssj1Mastered.setStrMultiplier(1.5);
        ssj1Mastered.setSkpMultiplier(1.5);
        ssj1Mastered.setStmMultiplier(1.5);
        ssj1Mastered.setDefMultiplier(1.5);
        ssj1Mastered.setVitMultiplier(1.5);
        ssj1Mastered.setPwrMultiplier(1.5);
        ssj1Mastered.setEneMultiplier(1.5);
        ssj1Mastered.setSpeedMultiplier(1.5);
        ssj1Mastered.setEnergyDrain(0.5);
        ssj1Mastered.setStaminaDrain(0.8);
        ssj1Mastered.setAttackSpeed(1.1);
        setDefaultMasteryValues(ssj1Mastered);

        FormConfig.FormData ssj2 = new FormConfig.FormData();
        ssj2.setName("supersaiyan2");
        ssj2.setUnlockOnSuperformLevel(5);
        ssj2.setHairColor("#FFD700");
        ssj2.setEye1Color("#00FFFF");
        ssj2.setEye2Color("#00FFFF");
        ssj2.setAuraColor("#FFD700");
        ssj2.setModelScaling(0.9375f);
        ssj2.setStrMultiplier(1.5);
        ssj2.setSkpMultiplier(1.5);
        ssj2.setStmMultiplier(1.5);
        ssj2.setDefMultiplier(1.5);
        ssj2.setVitMultiplier(1.5);
        ssj2.setPwrMultiplier(1.5);
        ssj2.setEneMultiplier(1.5);
        ssj2.setSpeedMultiplier(1.5);
        ssj2.setEnergyDrain(1.5);
        ssj2.setStaminaDrain(1.1);
        ssj2.setAttackSpeed(1.2);
        setDefaultMasteryValues(ssj2);

        FormConfig.FormData ssj3 = new FormConfig.FormData();
        ssj3.setName("supersaiyan3");
        ssj3.setUnlockOnSuperformLevel(6);
        ssj3.setHairType(1);
        ssj3.setHairColor("#FFD700");
        ssj3.setEye1Color("#00FFFF");
        ssj3.setEye2Color("#00FFFF");
        ssj3.setAuraColor("#FFD700");
        ssj3.setModelScaling(0.9375f);
        ssj3.setStrMultiplier(1.5);
        ssj3.setSkpMultiplier(1.5);
        ssj3.setStmMultiplier(1.5);
        ssj3.setDefMultiplier(1.5);
        ssj3.setVitMultiplier(1.5);
        ssj3.setPwrMultiplier(1.5);
        ssj3.setEneMultiplier(1.5);
        ssj3.setSpeedMultiplier(1.5);
        ssj3.setEnergyDrain(1.5);
        ssj3.setStaminaDrain(1.2);
        ssj3.setAttackSpeed(1.25);
        setDefaultMasteryValues(ssj3);

        Map<String, FormConfig.FormData> superSaiyanForms = new LinkedHashMap<>();
        superSaiyanForms.put("ssj1mastered", ssj1Mastered);
        superSaiyanForms.put("ssj2", ssj2);
        superSaiyanForms.put("ssj3", ssj3);
        superSaiyan.setForms(superSaiyanForms);

        forms.put("oozaru", oozaruForms);
        forms.put("ssgrades", ssGrades);
        forms.put("supersaiyan", superSaiyan);

        Path oozaruPath = formsPath.resolve("oozaru.json");
        loader.saveConfig(oozaruPath, oozaruForms);
        Path ssGradesPath = formsPath.resolve("ssgrades.json");
        loader.saveConfig(ssGradesPath, ssGrades);
        Path superSaiyanPath = formsPath.resolve("supersaiyan.json");
        loader.saveConfig(superSaiyanPath, superSaiyan);
        LogUtil.info(Env.COMMON, "Default Super Saiyan forms created");
    }

    private void createNamekianForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
        FormConfig namekianForms = new FormConfig();
        namekianForms.setGroupName("superforms");
        namekianForms.setFormType("super");

        FormConfig.FormData giantForm = new FormConfig.FormData();
        giantForm.setName("giant");
        giantForm.setUnlockOnSuperformLevel(1);
        giantForm.setModelScaling(3.0f);
        giantForm.setStrMultiplier(1.5);
        giantForm.setSkpMultiplier(1.5);
        giantForm.setDefMultiplier(1.5);
        giantForm.setVitMultiplier(1.5);
        giantForm.setSpeedMultiplier(0.7);
        giantForm.setEnergyDrain(1.5);
        giantForm.setStaminaDrain(1.3);
        giantForm.setAttackSpeed(0.85);
        setDefaultMasteryValues(giantForm);

        FormConfig.FormData superNamekian = new FormConfig.FormData();
        superNamekian.setName("supernamekian");
        superNamekian.setUnlockOnSuperformLevel(2);
        superNamekian.setAuraColor("#7FFF00");
        superNamekian.setModelScaling(1.05f);
        superNamekian.setStrMultiplier(1.5);
        superNamekian.setSkpMultiplier(1.5);
        superNamekian.setDefMultiplier(1.5);
        superNamekian.setVitMultiplier(1.5);
        superNamekian.setSpeedMultiplier(1.5);
        superNamekian.setEnergyDrain(1.5);
        superNamekian.setStaminaDrain(1.1);
        superNamekian.setAttackSpeed(1.1);
        setDefaultMasteryValues(superNamekian);

        Map<String, FormConfig.FormData> namekianFormData = new LinkedHashMap<>();
        namekianFormData.put("giant", giantForm);
        namekianFormData.put("supernamekian", superNamekian);
        namekianForms.setForms(namekianFormData);

        forms.put("superforms", namekianForms);

        Path namekianPath = formsPath.resolve("superforms.json");
        loader.saveConfig(namekianPath, namekianForms);
        LogUtil.info(Env.COMMON, "Default Namekian forms created");
    }

    private void createFrostDemonForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
        FormConfig frostForms = new FormConfig();
        frostForms.setGroupName("evolutionforms");
        frostForms.setFormType("super");
        frostForms.setForms(new LinkedHashMap<>());
        forms.put("evolutionforms", frostForms);

        Path frostPath = formsPath.resolve("evolutionforms.json");
        loader.saveConfig(frostPath, frostForms);
        LogUtil.info(Env.COMMON, "Default Frost Demon forms created");
    }

    private void createMajinForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
        FormConfig majinForms = new FormConfig();
        majinForms.setGroupName("pureforms");
        majinForms.setFormType("super");
        majinForms.setForms(new LinkedHashMap<>());
        forms.put("pureforms", majinForms);

        Path majinPath = formsPath.resolve("pureforms.json");
        loader.saveConfig(majinPath, majinForms);
        LogUtil.info(Env.COMMON, "Default Majin forms created");
    }

    private void createBioAndroidForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
        FormConfig bioForms = new FormConfig();
        bioForms.setGroupName("bioevolution");
        bioForms.setFormType("super");
        bioForms.setForms(new LinkedHashMap<>());
        forms.put("bioevolution", bioForms);

        Path bioPath = formsPath.resolve("bioevolution.json");
        loader.saveConfig(bioPath, bioForms);
        LogUtil.info(Env.COMMON, "Default Bio Android forms created");
    }
}

