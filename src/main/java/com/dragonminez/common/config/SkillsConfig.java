package com.dragonminez.common.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class SkillsConfig {
	public static final int CURRENT_VERSION = 4;

	@Setter
	private int configVersion;

	private final List<String> kiSkills = new ArrayList<>();
	private final List<String> formSkills = new ArrayList<>();
	private final List<String> stackSkills = new ArrayList<>();
	private final List<String> androidBlacklistedForms = new ArrayList<>();
	private final Map<String, SkillCosts> skills = new HashMap<>();
	private final Map<String, List<String>> skillOfferings = new HashMap<>();

	public SkillsConfig() {
		createDefaults();
	}

	private void createDefaults() {
		formSkills.add("superforms");
		formSkills.add("legendaryforms");
		formSkills.add("godforms");
		formSkills.add("androidforms");

		stackSkills.add("kaioken");
 		stackSkills.add("ultimate");
//		stackSkills.add("ultrainstinct");
//		stackSkills.add("ultraego");

		androidBlacklistedForms.add("superforms");
		androidBlacklistedForms.add("legendaryforms");

		kiSkills.add("spiritbomb");
		kiSkills.add("supernova");
		kiSkills.add("supernova_cooler");
		kiSkills.add("big_bang");
		kiSkills.add("burning_attack");
		kiSkills.add("final_flash");
		kiSkills.add("kamehameha");
		kiSkills.add("galick_gun");
		kiSkills.add("masenko");
		kiSkills.add("kienzan");
		kiSkills.add("kienzan_doble");
		kiSkills.add("death_beam");
		kiSkills.add("emperor_death_beam");
		kiSkills.add("ki_barrage");
		kiSkills.add("final_explosion");

		skills.put("kamehameha", new SkillCosts(List.of(2000)));
		skills.put("galick_gun", new SkillCosts(List.of(2000)));
		skills.put("masenko", new SkillCosts(List.of(1500)));
		skills.put("kienzan", new SkillCosts(List.of(3000)));
		skills.put("kienzan_doble", new SkillCosts(List.of(4000)));
		skills.put("death_beam", new SkillCosts(List.of(2500)));
		skills.put("emperor_death_beam", new SkillCosts(List.of(5000)));
		skills.put("big_bang", new SkillCosts(List.of(4000)));
		skills.put("burning_attack", new SkillCosts(List.of(3500)));
		skills.put("ki_barrage", new SkillCosts(List.of(1000)));
		skills.put("spiritbomb", new SkillCosts(List.of(10000)));
		skills.put("supernova", new SkillCosts(List.of(12000)));
		skills.put("supernova_cooler", new SkillCosts(List.of(15000)));
		skills.put("final_explosion", new SkillCosts(List.of(20000)));
		skills.put("final_flash", new SkillCosts(List.of(5000)));

		List<Integer> jumpCosts = new ArrayList<>();
		jumpCosts.add(300);
		jumpCosts.add(600);
		jumpCosts.add(900);
		jumpCosts.add(1200);
		jumpCosts.add(1500);
		jumpCosts.add(1800);
		jumpCosts.add(2100);
		jumpCosts.add(2400);
		jumpCosts.add(2700);
		jumpCosts.add(3000);
		skills.put("jump", new SkillCosts(jumpCosts));

		List<Integer> flyCosts = new ArrayList<>();
		flyCosts.add(1500);
		flyCosts.add(600);
		flyCosts.add(900);
		flyCosts.add(1200);
		flyCosts.add(1500);
		flyCosts.add(1800);
		flyCosts.add(2100);
		flyCosts.add(2400);
		flyCosts.add(2700);
		flyCosts.add(3000);
		skills.put("fly", new SkillCosts(flyCosts));

		List<Integer> potentialUnlockCosts = new ArrayList<>();
		potentialUnlockCosts.add(600);
		potentialUnlockCosts.add(1600);
		potentialUnlockCosts.add(2400);
		potentialUnlockCosts.add(3200);
		potentialUnlockCosts.add(4000);
		potentialUnlockCosts.add(4800);
		potentialUnlockCosts.add(5600);
		potentialUnlockCosts.add(6400);
		potentialUnlockCosts.add(7200);
		potentialUnlockCosts.add(8000);
		potentialUnlockCosts.add(-1);
		potentialUnlockCosts.add(8800);
		potentialUnlockCosts.add(9600);
		skills.put("potentialunlock", new SkillCosts(potentialUnlockCosts));

		List<Integer> meditationCosts = new ArrayList<>();
		meditationCosts.add(300);
		meditationCosts.add(600);
		meditationCosts.add(900);
		meditationCosts.add(1200);
		meditationCosts.add(1500);
		meditationCosts.add(1800);
		meditationCosts.add(2100);
		meditationCosts.add(2400);
		meditationCosts.add(2700);
		meditationCosts.add(3000);
		skills.put("meditation", new SkillCosts(meditationCosts));

		List<Integer> kiControlCosts = new ArrayList<>();
		kiControlCosts.add(300);
		kiControlCosts.add(600);
		kiControlCosts.add(900);
		kiControlCosts.add(1200);
		kiControlCosts.add(1500);
		kiControlCosts.add(1800);
		kiControlCosts.add(2100);
		kiControlCosts.add(2400);
		kiControlCosts.add(2700);
		kiControlCosts.add(3000);
		skills.put("kicontrol", new SkillCosts(kiControlCosts));

		List<Integer> kiSenseCosts = new ArrayList<>();
		kiSenseCosts.add(300);
		kiSenseCosts.add(600);
		kiSenseCosts.add(900);
		kiSenseCosts.add(1200);
		kiSenseCosts.add(1500);
		kiSenseCosts.add(1800);
		kiSenseCosts.add(2100);
		kiSenseCosts.add(2400);
		kiSenseCosts.add(2700);
		kiSenseCosts.add(3000);
		skills.put("kisense", new SkillCosts(kiSenseCosts));

		List<Integer> kiManipulationCosts = new ArrayList<>();
		kiManipulationCosts.add(3600);
		kiManipulationCosts.add(600);
		kiManipulationCosts.add(900);
		kiManipulationCosts.add(1200);
		kiManipulationCosts.add(1500);
		kiManipulationCosts.add(1800);
		kiManipulationCosts.add(2100);
		kiManipulationCosts.add(2400);
		kiManipulationCosts.add(2700);
		kiManipulationCosts.add(3300);
		skills.put("kimanipulation", new SkillCosts(kiManipulationCosts));

		List<Integer> instantTranmission = new ArrayList<>();
		instantTranmission.add(3600);
		instantTranmission.add(600);
		instantTranmission.add(900);
		instantTranmission.add(1200);
		instantTranmission.add(1500);
		instantTranmission.add(1800);
		instantTranmission.add(2100);
		instantTranmission.add(2400);
		instantTranmission.add(2700);
		instantTranmission.add(3300);
		skills.put("instant_transmission", new SkillCosts(instantTranmission));


		List<Integer> defensePenetration = new ArrayList<>();
		defensePenetration.add(3600);
		defensePenetration.add(600);
		defensePenetration.add(900);
		defensePenetration.add(1200);
		defensePenetration.add(1500);
		defensePenetration.add(1800);
		defensePenetration.add(2100);
		defensePenetration.add(2400);
		defensePenetration.add(2700);
		defensePenetration.add(3300);
		skills.put("defense_penetration", new SkillCosts(defensePenetration));

		List<Integer> kaiokenCosts = new ArrayList<>();
		kaiokenCosts.add(1000);
		kaiokenCosts.add(1500);
		kaiokenCosts.add(2500);
		kaiokenCosts.add(4000);
		kaiokenCosts.add(7500);
//		kaiokenCosts.add(25000);
		skills.put("kaioken", new SkillCosts(kaiokenCosts));
		skills.put("ultimate", new SkillCosts(List.of(-1)));

//		List<Integer> ultraInstinctCosts = new ArrayList<>();
//		ultraInstinctCosts.add(-1);
//		ultraInstinctCosts.add(5000);
//		skills.put("ultrainstinct", new SkillCosts(ultraInstinctCosts));

//		List<Integer> ultraEgoCosts = new ArrayList<>();
//		ultraEgoCosts.add(-1);
//		ultraEgoCosts.add(5000);
//		skills.put("ultraego", new SkillCosts(ultraEgoCosts));

		List<Integer> fusionCosts = new ArrayList<>();
		fusionCosts.add(25000);
		fusionCosts.add(5000);
		fusionCosts.add(10000);
		fusionCosts.add(15000);
		fusionCosts.add(20000);
		skills.put("fusion", new SkillCosts(fusionCosts));

		// MASTER SKILLS OFFERINGS
		List<String> roshiSkills = new ArrayList<>();
		roshiSkills.add("jump");
		roshiSkills.add("meditation");
		roshiSkills.add("kicontrol");
		roshiSkills.add("kamehameha");
		skillOfferings.put("roshi", roshiSkills);

		List<String> gokuSkills = new ArrayList<>();
		gokuSkills.add("fly");
		gokuSkills.add("kicontrol");
		gokuSkills.add("kisense");
		gokuSkills.add("defense_penetration");
		gokuSkills.add("fusion");
		gokuSkills.add("potentialunlock");
		gokuSkills.add("kamehameha");
		gokuSkills.add("spiritbomb");
		skillOfferings.put("goku", gokuSkills);

		List<String> kingKaiSkills = new ArrayList<>();
		kingKaiSkills.add("kaioken");
		kingKaiSkills.add("potentialunlock");
		kingKaiSkills.add("kimanipulation");
		kingKaiSkills.add("fusion");
		kingKaiSkills.add("spiritbomb");
		skillOfferings.put("kingkai", kingKaiSkills);

		List<String> vegetaSkills = new ArrayList<>();
		vegetaSkills.add("defense_penetration");
		vegetaSkills.add("galick_gun");
		vegetaSkills.add("big_bang");
		vegetaSkills.add("final_flash");
		vegetaSkills.add("final_explosion");
		skillOfferings.put("vegeta", vegetaSkills);

		List<String> piccoloSkills = new ArrayList<>();
		piccoloSkills.add("masenko");
		skillOfferings.put("piccolo", piccoloSkills);

		List<String> krillinSkills = new ArrayList<>();
		krillinSkills.add("kamehameha");
		krillinSkills.add("kienzan");
		krillinSkills.add("kibarrage");
		skillOfferings.put("krillin", krillinSkills);

		List<String> friezaSkills = new ArrayList<>();
		friezaSkills.add("death_beam");
		friezaSkills.add("emperor_death_beam");
		friezaSkills.add("kienzan_doble");
		friezaSkills.add("supernova");
		skillOfferings.put("frieza", friezaSkills);

		List<String> coolerSkills = new ArrayList<>();
		coolerSkills.add("supernova_cooler");
		coolerSkills.add("death_beam");
		skillOfferings.put("cooler", coolerSkills);

		List<String> trunksSkills = new ArrayList<>();
		trunksSkills.add("burning_attack");
		trunksSkills.add("galick_gun");
		trunksSkills.add("kibarrage");
		skillOfferings.put("trunks", trunksSkills);

		List<String> defaultSkills = new ArrayList<>();
		defaultSkills.add("jump");
		skillOfferings.put("default", defaultSkills);
	}

	public SkillCosts getSkillCosts(String skillName) {
		return skills.getOrDefault(skillName.toLowerCase(), new SkillCosts(new ArrayList<>(), new ArrayList<>()));
	}

	public boolean isSkillAllowedForRace(String skillName, String raceName) {
		if (skillName == null || skillName.isEmpty()) return false;

		SkillCosts skillCosts = getSkillCosts(skillName);
		if (skillCosts == null || skillCosts.getAllowedRaces() == null || skillCosts.getAllowedRaces().isEmpty()) return true;
		if (raceName == null || raceName.isEmpty()) return false;

		String normalizedRace = raceName.toLowerCase();
		for (String allowedRace : skillCosts.getAllowedRaces()) {
			if (allowedRace == null || allowedRace.isEmpty()) continue;
			String normalizedAllowed = allowedRace.toLowerCase();
			if (normalizedAllowed.equals("all") || normalizedAllowed.equals(normalizedRace)) return true;
		}

		return false;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SkillCosts {
		private List<Integer> costs = new ArrayList<>();
		private List<String> allowedRaces = new ArrayList<>();

		public SkillCosts(List<Integer> costs) {
			this.costs = costs;
			this.allowedRaces = new ArrayList<>();
		}
	}
}