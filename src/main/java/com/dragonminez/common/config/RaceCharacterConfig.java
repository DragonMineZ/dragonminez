package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
@NoArgsConstructor
public class RaceCharacterConfig {
	public static final int CURRENT_VERSION = ConfigManager.CONFIG_VERSION;

	private int configVersion;

	private String raceName;
	private Boolean hasGender = true;
	private Boolean useVanillaSkin = false;
	private String customModel = "";
	private Boolean isLayered = false;
	private String[] headBones = new String[0];
	private String racialSkill = "human";
	private Boolean hasSaiyanTail = false;
	private String auraType = "kakarot";
	private Float[] defaultModelScaling = {0.9375f, 0.9375f, 0.9375f};
	private Integer defaultBodyType = 0;
	private Integer defaultHairType = 0;
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

	public boolean normalizeFormSkillKeys(Collection<String> canonicalFormSkills) {
		if (formSkillsCosts == null || formSkillsCosts.isEmpty() || canonicalFormSkills == null || canonicalFormSkills.isEmpty()) return false;

		Set<String> canonical = new HashSet<>();
		for (String name : canonicalFormSkills) if (name != null) canonical.add(name.toLowerCase());

		Map<String, List<Integer>> normalized = new LinkedHashMap<>();
		boolean changed = false;
		for (Map.Entry<String, List<Integer>> entry : formSkillsCosts.entrySet()) {
			String key = entry.getKey();
			String target = key;
			if (key != null) {
				String lower = key.toLowerCase();
				if (!canonical.contains(lower) && canonical.contains(lower + "s")) {
					target = lower + "s";
					changed = true;
				}
			}
			List<Integer> existing = normalized.get(target);
			if (existing == null || existing.isEmpty()) normalized.put(target, entry.getValue());
		}

		if (changed) {
			formSkillsCosts.clear();
			formSkillsCosts.putAll(normalized);
		}
		return changed;
	}

	public Boolean hasCustomModel() {
		return this.customModel != null && !this.customModel.isEmpty();
	}
}
