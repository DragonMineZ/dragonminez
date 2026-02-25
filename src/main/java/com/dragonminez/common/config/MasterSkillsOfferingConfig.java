package com.dragonminez.common.config;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class MasterSkillsOfferingConfig {
	public static final int CURRENT_VERSION = 2;

	@Setter
	private int configVersion;

	private final Map<String, List<String>> skillOfferings = new HashMap<>();

    public MasterSkillsOfferingConfig() { createDefaults(); }

    private void createDefaults() {
		List<String> roshiSkills = new ArrayList<>();
		roshiSkills.add("jump");
		roshiSkills.add("meditation");
		skillOfferings.put("roshi", roshiSkills);

		List<String> gokuSkills = new ArrayList<>();
		gokuSkills.add("fly");
		gokuSkills.add("kicontrol");
		gokuSkills.add("kisense");
		gokuSkills.add("fusion");
		skillOfferings.put("goku", gokuSkills);

		List<String> kingKaiSkills = new ArrayList<>();
		kingKaiSkills.add("kaioken");
		kingKaiSkills.add("potentialunlock");
		kingKaiSkills.add("kimanipulation");
		skillOfferings.put("kingkai", kingKaiSkills);

		List<String> defaultSkills = new ArrayList<>();
		defaultSkills.add("jump");
		skillOfferings.put("default", defaultSkills);
	}
}