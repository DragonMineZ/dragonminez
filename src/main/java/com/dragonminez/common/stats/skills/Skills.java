package com.dragonminez.common.stats.skills;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.SkillsConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Skills {
	private static final double NAME_SIMILARITY_THRESHOLD = 0.8;

	private final Map<String, Skill> skillMap = new HashMap<>();

	public Skills() {
	}

	public void registerDefaultSkill(String skillName, int maxLevel) {
		String lowerName = skillName.toLowerCase();
		if (skillMap.containsKey(lowerName)) skillMap.get(lowerName).setMaxLevel(maxLevel);
		else skillMap.put(lowerName, new Skill(skillName, maxLevel));
	}

	public Skill getSkill(String name) {
		return skillMap.get(name.toLowerCase());
	}

	public boolean hasSkill(String name) {
		return skillMap.containsKey(name.toLowerCase());
	}

	public boolean isUnlockedAtLevel(String name, int requiredLevel) {
		Skill skill = skillMap.get(name.toLowerCase());
		return skill != null && skill.isUnlockedAt(requiredLevel);
	}

	public int getSkillLevel(String name) {
		Skill skill = skillMap.get(name.toLowerCase());
		return skill != null ? skill.getLevel() : 0;
	}

	public int getMaxSkillLevel(String name) {
		Skill skill = skillMap.get(name.toLowerCase());
		return skill != null ? skill.getMaxLevel() : 0;
	}

	private int calculateMaxLevel(String skillName) {
		int costBasedMaxLevel = 0;
		try {
			var config = ConfigManager.getSkillsConfig();
			if (config != null) {
				var skillCosts = config.getSkillCosts(skillName);
				if (skillCosts != null && skillCosts.getCosts() != null) costBasedMaxLevel = skillCosts.getCosts().size();
			}
		} catch (Exception ignored) {}

		if (skillName.equalsIgnoreCase("potentialunlock")) return Math.min(costBasedMaxLevel, 30);
		else return Math.min(costBasedMaxLevel, 50);
	}

	public void refreshNonFormSkillMaxLevels() {
		var formSkills = ConfigManager.getSkillsConfig().getFormSkills();
		for (Skill skill : skillMap.values()) {
			String skillName = skill.getName().toLowerCase();
			if (formSkills.contains(skillName)) continue;
			int newMax = calculateMaxLevel(skillName);
			if (newMax > 0) skill.setMaxLevel(newMax);
		}
	}

	public void setSkillLevel(String name, int level) {
		String lowerName = name.toLowerCase();
		if (!skillMap.containsKey(lowerName)) {
			int finalMaxLevel = calculateMaxLevel(lowerName);
			skillMap.put(lowerName, new Skill(name, 0, false, finalMaxLevel));
		}
		skillMap.get(lowerName).setLevel(level);
	}

	public void removeSkill(String name) {
		skillMap.remove(name.toLowerCase());
	}

	public void removeAllSkills() {
		skillMap.clear();
	}

	public Map<String, String> repairSkillNames() {
		Map<String, String> renamed = new LinkedHashMap<>();
		SkillsConfig config = ConfigManager.getSkillsConfig();
		if (config == null) return renamed;

		Set<String> validNames = new HashSet<>();
		validNames.addAll(config.getSkills().keySet());
		validNames.addAll(config.getFormSkills());
		validNames.addAll(config.getStackSkills());
		validNames.addAll(config.getKiSkills());
		validNames.addAll(config.getStrikeSkills());
		if (validNames.isEmpty()) return renamed;

		List<String> invalidKeys = new ArrayList<>();
		for (String key : skillMap.keySet()) if (!validNames.contains(key)) invalidKeys.add(key);

		for (String badKey : invalidKeys) {
			String canonical = resolveCanonicalAlias(badKey, validNames);
			if (canonical == null) canonical = findClosestSkill(badKey, validNames);
			if (canonical == null || canonical.equals(badKey)) continue;

			Skill legacy = skillMap.remove(badKey);
			if (legacy == null) continue;

			int maxLevel = config.getFormSkills().contains(canonical) ? legacy.getMaxLevel() : calculateMaxLevel(canonical);

			Skill target = skillMap.get(canonical);
			if (target != null) {
				target.setMaxLevel(Math.max(target.getMaxLevel(), maxLevel));
				target.setLevel(Math.max(target.getLevel(), legacy.getLevel()));
				target.setActive(target.isActive() || legacy.isActive());
			} else {
				Skill migrated = new Skill(canonical, maxLevel);
				migrated.setLevel(legacy.getLevel());
				migrated.setActive(legacy.isActive());
				skillMap.put(canonical, migrated);
			}
			renamed.put(badKey, canonical);
		}
		return renamed;
	}

	private static String resolveCanonicalAlias(String input, Set<String> candidates) {
		if (input == null || input.isEmpty()) return null;
		if (candidates.contains(input + "s")) return input + "s";
		if (input.endsWith("s") && candidates.contains(input.substring(0, input.length() - 1))) return input.substring(0, input.length() - 1);
		return null;
	}

	private static String findClosestSkill(String input, Set<String> candidates) {
		String best = null;
		double bestSimilarity = 0.0;
		for (String candidate : candidates) {
			int maxLen = Math.max(input.length(), candidate.length());
			if (maxLen == 0) continue;
			double similarity = 1.0 - (double) levenshtein(input, candidate) / maxLen;
			if (similarity > bestSimilarity) {
				bestSimilarity = similarity;
				best = candidate;
			}
		}
		return bestSimilarity >= NAME_SIMILARITY_THRESHOLD ? best : null;
	}

	private static int levenshtein(String a, String b) {
		int[] prev = new int[b.length() + 1];
		int[] curr = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) prev[j] = j;
		for (int i = 1; i <= a.length(); i++) {
			curr[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
			}
			int[] tmp = prev;
			prev = curr;
			curr = tmp;
		}
		return prev[b.length()];
	}

	public void addSkillLevel(String name, int amount) {
		Skill skill = skillMap.get(name.toLowerCase());
		if (skill != null) skill.addLevel(amount);
	}

	public boolean isSkillActive(String name) {
		Skill skill = skillMap.get(name.toLowerCase());
		return skill != null && skill.isActive();
	}

	public void setSkillActive(String name, boolean active) {
		Skill skill = skillMap.get(name.toLowerCase());
		if (skill != null) skill.setActive(active);
	}

	public void toggleSkillActive(String name) {
		Skill skill = skillMap.get(name.toLowerCase());
		if (skill != null) skill.setActive(!skill.isActive());
	}

	public Map<String, Skill> getAllSkills() {
		return new HashMap<>(skillMap);
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		ListTag skillsList = new ListTag();
		for (Skill skill : skillMap.values()) skillsList.add(skill.save());
		nbt.put("SkillsList", skillsList);
		return nbt;
	}

	public void load(CompoundTag nbt) {
		if (nbt.contains("SkillsList", Tag.TAG_LIST)) {
			ListTag skillsList = nbt.getList("SkillsList", Tag.TAG_COMPOUND);
			skillMap.clear();
			for (int i = 0; i < skillsList.size(); i++) {
				CompoundTag skillTag = skillsList.getCompound(i);
				Skill skill = Skill.load(skillTag);

				String skillName = skill.getName().toLowerCase();
				if (!ConfigManager.getSkillsConfig().getFormSkills().contains(skillName)) {
					int newMax = calculateMaxLevel(skillName);
					if (newMax > 0) skill.setMaxLevel(newMax);
				}
				skillMap.put(skillName, skill);
			}
		}
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeInt(skillMap.size());
		for (Skill skill : skillMap.values()) skill.toBytes(buf);
	}

	public void fromBytes(FriendlyByteBuf buf) {
		int size = buf.readInt();
		skillMap.clear();
		for (int i = 0; i < size; i++) {
			Skill skill = Skill.fromBytes(buf);
			skillMap.put(skill.getName().toLowerCase(), skill);
		}
	}

	public void copyFrom(Skills other) {
		this.skillMap.clear();
		for (Map.Entry<String, Skill> entry : other.skillMap.entrySet()) {
			Skill newSkill = new Skill(
					entry.getValue().getName(),
					entry.getValue().getLevel(),
					entry.getValue().isActive(),
					entry.getValue().getMaxLevel()
			);
			this.skillMap.put(entry.getKey(), newSkill);
		}
	}
}
