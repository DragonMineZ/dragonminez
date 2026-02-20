package com.dragonminez.common.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillsConfig {
	public static final int CURRENT_VERSION = 1;
	private int configVersion = CURRENT_VERSION;
	public int getConfigVersion() { return configVersion; }
	public void setConfigVersion(int configVersion) { this.configVersion = configVersion; }

	private final List<String> formSkills = new ArrayList<>();
	private final List<String> stackSkills = new ArrayList<>();
	private final Map<String, SkillCosts> skills = new HashMap<>();

    public SkillsConfig() { createDefaults(); }

    private void createDefaults() {
		formSkills.add("superform");
		formSkills.add("legendaryforms");
		formSkills.add("godform");
		formSkills.add("androidforms");

		stackSkills.add("kaioken");
		stackSkills.add("ultrainstinct");
		stackSkills.add("ultraego");

        List<Integer> jumpCosts = new ArrayList<>();
		jumpCosts.add(-1);
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
		flyCosts.add(-1);
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
		potentialUnlockCosts.add(-1);
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
		meditationCosts.add(-1);
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
		kiControlCosts.add(-1);
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
		kiSenseCosts.add(-1);
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
		kiManipulationCosts.add(-1);
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
		kaiokenCosts.add(-1);
		kaiokenCosts.add(5000);
		kaiokenCosts.add(7000);
		kaiokenCosts.add(9000);
		kaiokenCosts.add(11000);
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
		fusionCosts.add(-1);
		fusionCosts.add(10000);
		fusionCosts.add(30000);
		fusionCosts.add(50000);
		fusionCosts.add(75000);
		skills.put("fusion", new SkillCosts(fusionCosts));
    }

	public List<String> getFormSkills() {
		return formSkills;
	}

	public List<String> getStackSkills() {
		return stackSkills;
	}

	public Map<String, SkillCosts> getSkills() {
        return skills;
    }

    public SkillCosts getSkillCosts(String skillName) {
        return skills.getOrDefault(skillName.toLowerCase(), new SkillCosts(new ArrayList<>()));
    }

    public int getCostForLevel(String skillName, int level) {
        SkillCosts skillCosts = getSkillCosts(skillName);
        if (level < 1 || level > skillCosts.costs.size()) {
            return -1;
        }
        return Math.max(0, skillCosts.costs.get(level - 1));
    }

    public double getMultiplierForLevel(String skillName, int level) {
        SkillCosts skillCosts = getSkillCosts(skillName);
        if (skillCosts.multipliers == null || skillCosts.multipliers.isEmpty()) {
            return 0.0;
        }
        if (level < 1) {
            return 0.0;
        }
        return Math.max(0.0, skillCosts.multipliers.get(level - 1));
    }

	public double getDrainRateForLevel(String skillName, int level) {
		SkillCosts skillCosts = getSkillCosts(skillName);
		if (skillCosts.drainRate == null || skillCosts.drainRate.isEmpty()) {
			return 0.0;
		}
		if (level < 1) {
			return 0.0;
		}
		return Math.max(0.0, skillCosts.drainRate.get(level - 1));
	}

    public boolean canPurchaseLevel(String skillName, int level) {
        int cost = getCostForLevel(skillName, level);
        return cost >= 0;
    }

    public static class SkillCosts {
        private List<Integer> costs;
        private List<Double> multipliers;
		private List<Double> drainRate;

        public SkillCosts(List<Integer> costs) {
            this.costs = costs;
            this.multipliers = null;
			this.drainRate = null;
        }

        public SkillCosts(List<Integer> costs, List<Double> multipliers) {
            this.costs = costs;
            this.multipliers = multipliers;
        }

		public SkillCosts(List<Integer> costs, List<Double> multipliers, List<Double> drainRate) {
			this.costs = costs;
			this.multipliers = multipliers;
			this.drainRate = drainRate;
		}

        public List<Integer> getCosts() {
            return costs;
        }

        public List<Double> getMultipliers() {
            return multipliers;
        }

		public List<Double> getDrainRate() {
			return drainRate;
		}

        public int getMaxLevel() {
            return costs.size();
        }
    }
}