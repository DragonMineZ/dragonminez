package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
@NoArgsConstructor
public class RaceCharacterConfig {
    public static final int CURRENT_VERSION = 2;

    private int configVersion;

    private String raceName;
    private Boolean hasGender = true;
    private Boolean useVanillaSkin = false;
    private String customModel = "";
    private String racialSkill = "human";
	private Float[] defaultModelScaling = {0.9375f, 0.9375f, 0.9375f};
    private Integer defaultBodyType = 0;
    private Integer defaultHairType = 0;
    private Boolean canUseHair = true;
    private Integer defaultEyesType = 0;
    private Integer defaultNoseType = 0;
    private Integer defaultMouthType = 0;
	private Integer defaultTattooType = 0;
	private String defaultBodyColor = null;
    private String defaultBodyColor2 = null;
    private String defaultBodyColor3 = null;
    private String defaultHairColor = null;
    private String defaultEye1Color = null;
    private String defaultEye2Color = null;
    private String defaultAuraColor = null;
    private Map<String, List<Integer>> formSkillsCosts = new HashMap<>();

    public Integer[] getFormSkillTpCosts(String form) {
        List<Integer> list = formSkillsCosts.getOrDefault(form, new ArrayList<>());
        return list.toArray(new Integer[0]);
    }
    public Collection<String> getFormSkills() {
        return formSkillsCosts.keySet();
    }
    public void setFormSkillTpCosts(String form, Integer[] costs) {
        formSkillsCosts.put(form, new ArrayList<>(Arrays.asList(costs)));
    }

    public Boolean hasCustomModel() {
        return this.customModel != null && !this.customModel.isEmpty();
    }
}
