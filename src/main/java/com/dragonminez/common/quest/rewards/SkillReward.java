package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SkillReward extends QuestReward {
    private final String skill;
    private final int level;

    public SkillReward(String skill, int level) {
        super(RewardType.SKILL);
        this.skill = skill;
        this.level = level;
    }

    public String getSkill() {
        return skill;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public void giveReward(ServerPlayer player) {
        if (!isClaimed()) {
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                data.getSkills().setSkillLevel(skill, level);
                setClaimed(true);
            });
        }
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
                "gui.dragonminez.quests.rewards.skill",
                Component.translatable("skill.dragonminez." + skill),
                level
        );
    }
}
