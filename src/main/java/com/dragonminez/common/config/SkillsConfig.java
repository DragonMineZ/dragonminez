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
	public static final int CURRENT_VERSION = 3;

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
		formSkills.add("superform");
		formSkills.add("legendaryforms");
		formSkills.add("godform");
		formSkills.add("androidforms");

		stackSkills.add("kaioken");
//		stackSkills.add("ultrainstinct");
//		stackSkills.add("ultraego");

		androidBlacklistedForms.add("superform");
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
		jumpCosts.add(1000);
		jumpCosts.add(2000);
		jumpCosts.add(3000);
		jumpCosts.add(4000);
		jumpCosts.add(5000);
		jumpCosts.add(6000);
		jumpCosts.add(7000);
		jumpCosts.add(8000);
		jumpCosts.add(9000);
		jumpCosts.add(10000);
		skills.put("jump", new SkillCosts(jumpCosts));

		List<Integer> flyCosts = new ArrayList<>();
		flyCosts.add(5000);
		flyCosts.add(2000);
		flyCosts.add(3000);
		flyCosts.add(4000);
		flyCosts.add(5000);
		flyCosts.add(6000);
		flyCosts.add(7000);
		flyCosts.add(8000);
		flyCosts.add(9000);
		flyCosts.add(10000);
		skills.put("fly", new SkillCosts(flyCosts));

		List<Integer> potentialUnlockCosts = new ArrayList<>();
		potentialUnlockCosts.add(1500);
		potentialUnlockCosts.add(4000);
		potentialUnlockCosts.add(6000);
		potentialUnlockCosts.add(8000);
		potentialUnlockCosts.add(10000);
		potentialUnlockCosts.add(12000);
		potentialUnlockCosts.add(14000);
		potentialUnlockCosts.add(16000);
		potentialUnlockCosts.add(18000);
		potentialUnlockCosts.add(20000);
		potentialUnlockCosts.add(-1);
		potentialUnlockCosts.add(22000);
		potentialUnlockCosts.add(24000);
		skills.put("potentialunlock", new SkillCosts(potentialUnlockCosts));

		List<Integer> meditationCosts = new ArrayList<>();
		meditationCosts.add(1000);
		meditationCosts.add(2000);
		meditationCosts.add(3000);
		meditationCosts.add(4000);
		meditationCosts.add(5000);
		meditationCosts.add(6000);
		meditationCosts.add(7000);
		meditationCosts.add(8000);
		meditationCosts.add(9000);
		meditationCosts.add(10000);
		skills.put("meditation", new SkillCosts(meditationCosts));

		List<Integer> kiControlCosts = new ArrayList<>();
		kiControlCosts.add(1000);
		kiControlCosts.add(2000);
		kiControlCosts.add(3000);
		kiControlCosts.add(4000);
		kiControlCosts.add(5000);
		kiControlCosts.add(6000);
		kiControlCosts.add(7000);
		kiControlCosts.add(8000);
		kiControlCosts.add(9000);
		kiControlCosts.add(10000);
		skills.put("kicontrol", new SkillCosts(kiControlCosts));

		List<Integer> kiSenseCosts = new ArrayList<>();
		kiSenseCosts.add(1000);
		kiSenseCosts.add(2000);
		kiSenseCosts.add(3000);
		kiSenseCosts.add(4000);
		kiSenseCosts.add(5000);
		kiSenseCosts.add(6000);
		kiSenseCosts.add(7000);
		kiSenseCosts.add(8000);
		kiSenseCosts.add(9000);
		kiSenseCosts.add(10000);
		skills.put("kisense", new SkillCosts(kiSenseCosts));

		List<Integer> kiManipulationCosts = new ArrayList<>();
		kiManipulationCosts.add(25000);
		kiManipulationCosts.add(2000);
		kiManipulationCosts.add(3000);
		kiManipulationCosts.add(4000);
		kiManipulationCosts.add(5000);
		kiManipulationCosts.add(6000);
		kiManipulationCosts.add(7000);
		kiManipulationCosts.add(8000);
		kiManipulationCosts.add(9000);
		kiManipulationCosts.add(10000);
		skills.put("kimanipulation", new SkillCosts(kiManipulationCosts));

		List<Integer> kaiokenCosts = new ArrayList<>();
		kaiokenCosts.add(10000);
		kaiokenCosts.add(7500);
		kaiokenCosts.add(10000);
		kaiokenCosts.add(15000);
		kaiokenCosts.add(20000);
//		kaiokenCosts.add(25000);
		skills.put("kaioken", new SkillCosts(kaiokenCosts));

//		List<Integer> ultraInstinctCosts = new ArrayList<>();
//		ultraInstinctCosts.add(-1);
//		ultraInstinctCosts.add(5000);
//		skills.put("ultrainstinct", new SkillCosts(ultraInstinctCosts));

//		List<Integer> ultraEgoCosts = new ArrayList<>();
//		ultraEgoCosts.add(-1);
//		ultraEgoCosts.add(5000);
//		skills.put("ultraego", new SkillCosts(ultraEgoCosts));

		List<Integer> fusionCosts = new ArrayList<>();
		fusionCosts.add(50000);
		fusionCosts.add(10000);
		fusionCosts.add(30000);
		fusionCosts.add(50000);
		fusionCosts.add(75000);
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
		return skills.getOrDefault(skillName.toLowerCase(), new SkillCosts(new ArrayList<>()));
	}

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SkillCosts {
		private List<Integer> costs;
	}
}