package com.dragonminez.common.stats;

import com.dragonminez.common.config.ConfigManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;

public class Skills {
    private final Map<String, Skill> skillMap = new HashMap<>();

    public Skills() {
    }

    public void registerDefaultSkill(String skillName, int maxLevel) {
        skillMap.put(skillName, new Skill(skillName, maxLevel));
    }

    public Skill getSkill(String name) {
        return skillMap.get(name.toLowerCase());
    }

    public boolean hasSkill(String name) {
        return skillMap.containsKey(name.toLowerCase());
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
                if (skillCosts != null && skillCosts.getCosts() != null) {
                    costBasedMaxLevel = skillCosts.getCosts().size();
                }
            }
        } catch (Exception ignored) {}

        if (skillName.equalsIgnoreCase("potentialunlock")) return Math.min(costBasedMaxLevel, 30);
        else return Math.min(costBasedMaxLevel, 50);
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

    public void addSkillLevel(String name, int amount) {
        Skill skill = skillMap.get(name.toLowerCase());
        if (skill != null) {
            skill.addLevel(amount);
        }
    }

    public boolean isSkillActive(String name) {
        Skill skill = skillMap.get(name.toLowerCase());
        return skill != null && skill.isActive();
    }

    public void setSkillActive(String name, boolean active) {
        Skill skill = skillMap.get(name.toLowerCase());
        if (skill != null) {
            skill.setActive(active);
        }
    }

    public void toggleSkillActive(String name) {
        Skill skill = skillMap.get(name.toLowerCase());
        if (skill != null) {
            skill.setActive(!skill.isActive());
        }
    }

    public Map<String, Skill> getAllSkills() {
        return new HashMap<>(skillMap);
    }

    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        ListTag skillsList = new ListTag();

        for (Skill skill : skillMap.values()) {
            skillsList.add(skill.save());
        }

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
                    skill.setMaxLevel(calculateMaxLevel(skillName));
                }
                skillMap.put(skillName, skill);
            }
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(skillMap.size());
        for (Skill skill : skillMap.values()) {
            skill.toBytes(buf);
        }
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
