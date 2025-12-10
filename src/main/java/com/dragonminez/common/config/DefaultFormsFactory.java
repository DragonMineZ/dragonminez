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
		superforms.setForms(new HashMap<>());
		forms.put("superforms", superforms);

		Path humanPath = formsPath.resolve("superforms.json");
		Files.writeString(humanPath, "{\"group_name\":\"superforms\",\"forms\":{}}", StandardCharsets.UTF_8);
		LogUtil.info(Env.COMMON, "Default Human forms created");
	}

    private void createSaiyanForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
        FormConfig ssGrades = new FormConfig();
        ssGrades.setGroupName("ssgrades");

        FormConfig.FormData ssj1 = new FormConfig.FormData();
        ssj1.setName("supersaiyan");
        ssj1.setHairColor("#FFD700");
        ssj1.setEye1Color("#00FFFF");
        ssj1.setEye2Color("#00FFFF");
        ssj1.setAuraColor("#FFD700");
		ssj1.setModelScaling(0.9375);
        ssj1.setStrMultiplier(1.5);
        ssj1.setSkpMultiplier(1.5);
        ssj1.setStmMultiplier(1.5);
        ssj1.setDefMultiplier(1.5);
        ssj1.setVitMultiplier(1.5);
        ssj1.setPwrMultiplier(1.5);
        ssj1.setEneMultiplier(1.5);
        ssj1.setSpeedMultiplier(1.2);
        ssj1.setEnergyDrain(1.5);

        FormConfig.FormData ssg2 = new FormConfig.FormData();
        ssg2.setName("supersaiyangrade2");
        ssg2.setHairColor("#FFD700");
        ssg2.setEye1Color("#00FFFF");
        ssg2.setEye2Color("#00FFFF");
        ssg2.setAuraColor("#FFD700");
        ssg2.setModelScaling(1.1);
        ssg2.setStrMultiplier(2.0);
        ssg2.setSkpMultiplier(2.0);
        ssg2.setDefMultiplier(1.8);
        ssg2.setVitMultiplier(1.8);
        ssg2.setPwrMultiplier(1.8);
        ssg2.setSpeedMultiplier(1.0);
        ssg2.setEnergyDrain(2.5);

        FormConfig.FormData ssg3 = new FormConfig.FormData();
        ssg3.setName("supersaiyangrade3");
        ssg3.setHairColor("#FFD700");
        ssg3.setEye1Color("#00FFFF");
        ssg3.setEye2Color("#00FFFF");
        ssg3.setAuraColor("#FFD700");
        ssg3.setModelScaling(1.2);
        ssg3.setStrMultiplier(2.5);
        ssg3.setSkpMultiplier(2.5);
        ssg3.setDefMultiplier(2.0);
        ssg3.setSpeedMultiplier(0.8);
        ssg3.setEnergyDrain(4.0);

        Map<String, FormConfig.FormData> ssGradeForms = new LinkedHashMap<>();
        ssGradeForms.put("form1", ssj1);
        ssGradeForms.put("form2", ssg2);
        ssGradeForms.put("form3", ssg3);
        ssGrades.setForms(ssGradeForms);

        forms.put("ssgrades", ssGrades);

        Path ssGradesPath = formsPath.resolve("ssgrades.json");
        loader.saveConfig(ssGradesPath, ssGrades);
        LogUtil.info(Env.COMMON, "Default Super Saiyan Grades forms created");
    }

    private void createNamekianForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
        FormConfig namekianForms = new FormConfig();
        namekianForms.setGroupName("superforms");

        FormConfig.FormData giantForm = new FormConfig.FormData();
        giantForm.setName("giant");
        giantForm.setModelScaling(3.0);
        giantForm.setStrMultiplier(2.5);
        giantForm.setSkpMultiplier(2.5);
        giantForm.setDefMultiplier(2.0);
        giantForm.setVitMultiplier(3.0);
        giantForm.setSpeedMultiplier(0.7);
        giantForm.setEnergyDrain(3.0);

        Map<String, FormConfig.FormData> namekianFormData = new LinkedHashMap<>();
        namekianFormData.put("form1", giantForm);
        namekianForms.setForms(namekianFormData);

        forms.put("superforms", namekianForms);

        Path namekianPath = formsPath.resolve("superforms.json");
        loader.saveConfig(namekianPath, namekianForms);
        LogUtil.info(Env.COMMON, "Default Namekian forms created");
    }

    private void createFrostDemonForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
        FormConfig frostForms = new FormConfig();
        frostForms.setGroupName("evolutionforms");
        frostForms.setForms(new LinkedHashMap<>());
        forms.put("evolutionforms", frostForms);

        Path frostPath = formsPath.resolve("evolutionforms.json");
        loader.saveConfig(frostPath, frostForms);
        LogUtil.info(Env.COMMON, "Default Frost Demon forms created");
    }

    private void createMajinForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
        FormConfig majinForms = new FormConfig();
        majinForms.setGroupName("pureforms");
        majinForms.setForms(new LinkedHashMap<>());
        forms.put("pureforms", majinForms);

        Path majinPath = formsPath.resolve("pureforms.json");
        loader.saveConfig(majinPath, majinForms);
        LogUtil.info(Env.COMMON, "Default Majin forms created");
    }

    private void createBioAndroidForms(Path formsPath, Map<String, FormConfig> forms) throws IOException {
        FormConfig bioForms = new FormConfig();
        bioForms.setGroupName("bioevolution");
        bioForms.setForms(new LinkedHashMap<>());
        forms.put("bioevolution", bioForms);

        Path bioPath = formsPath.resolve("bioevolution.json");
        loader.saveConfig(bioPath, bioForms);
        LogUtil.info(Env.COMMON, "Default Bio Android forms created");
    }
}

