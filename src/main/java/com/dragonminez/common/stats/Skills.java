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
        registerDefaultSkills();
    }

    private void registerDefaultSkills() {
        skillMap.put("superform", new Skill("superform", 0));
        skillMap.put("godform", new Skill("godform", 0));
        skillMap.put("legendaryforms", new Skill("legendaryforms", 0));
    }

    public void updateTransformationMaxLevels(int superformMax, int godformMax, int legendaryformsMax, int androidformsMax) {
        Skill superform = skillMap.get("superform");
        if (superform != null) {
            superform.setMaxLevel(superformMax);
        }

        Skill godform = skillMap.get("godform");
        if (godform != null) {
            godform.setMaxLevel(godformMax);
        }

        Skill legendaryforms = skillMap.get("legendaryforms");
        if (legendaryforms != null) {
            legendaryforms.setMaxLevel(legendaryformsMax);
        }

        Skill androidforms = skillMap.get("androidforms");
        if (androidforms != null) {
            androidforms.setMaxLevel(androidformsMax);
        }
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
        int costBasedMaxLevel = ConfigManager.getSkillsConfig().getSkillCosts(skillName).getCosts().size();
        if (skillName.equalsIgnoreCase("potentialunlock") || skillName.equalsIgnoreCase("kaioken")) {
            return Math.min(costBasedMaxLevel, 30);
        } else {
            return Math.min(costBasedMaxLevel, 50);
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
		registerDefaultSkills();
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
                if (!skillName.equals("superform") && !skillName.equals("godform") && !skillName.equals("legendaryforms") && !skillName.equals("androidforms")) {
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
