package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

@Getter
public class SkillReward extends QuestReward {
	private final String skill;
	private final int level;

	public SkillReward(String skill, int level) {
		super(RewardType.SKILL);
		this.skill = skill;
		this.level = level;
	}

	@Override
	public void giveReward(ServerPlayer player) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (data.getBaseSkills().getSkillLevel(skill) < level) data.grantSkillLevel(skill, level);
		});
	}

	@Override
	public Component getDescription() {
		return Component.translatable(
				"gui.dragonminez.quests.rewards.skill",
				displayName(skill),
				level
		);
	}

	public MutableComponent displayName() {
		return displayName(skill);
	}

	public static MutableComponent displayName(String skill) {
		String techniqueKey = PredefinedTechniques.nameKey(skill);
		return Component.translatable(techniqueKey != null ? techniqueKey : "skill.dragonminez." + skill);
	}
}
