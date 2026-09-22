package com.dragonminez.common.worldboss;

import com.dragonminez.client.util.NumberFormattingUtil;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.rewards.ItemReward;
import com.dragonminez.common.quest.rewards.KiTechniqueReward;
import com.dragonminez.common.quest.rewards.SkillReward;
import com.dragonminez.common.quest.rewards.TPSReward;
import com.dragonminez.common.quest.rewards.TransformationReward;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public final class WorldBossRewardText {

	private WorldBossRewardText() {}

	public static MutableComponent name(QuestReward reward) {
		if (reward instanceof ItemReward item) {
			return Component.translatable("item." + ResourceLocation.parse(item.getItemId()).toLanguageKey());
		}
		if (reward instanceof TPSReward tps) {
			return Component.translatable("worldboss.dragonminez.results.tps", NumberFormattingUtil.formatLargeNumber(tps.getAmount()));
		}
		if (reward instanceof SkillReward skill) {
			return Component.translatable("skill.dragonminez." + skill.getSkill());
		}
		if (reward instanceof KiTechniqueReward technique) {
			String techniqueName = technique.getTemplate().getName();
			return techniqueName != null && techniqueName.contains(".") ? Component.translatable(techniqueName) : Component.literal(techniqueName == null ? "" : techniqueName);
		}
		if (reward instanceof TransformationReward) {
			return reward.getDescription().copy();
		}
		return reward.getDescription().copy();
	}

	public static MutableComponent describe(QuestReward reward, float amount) {
		if (reward instanceof ItemReward item) {
			return Component.translatable("worldboss.dragonminez.results.item", Math.max(1, Math.round(amount)),
					Component.translatable("item." + ResourceLocation.parse(item.getItemId()).toLanguageKey()));
		}
		if (reward instanceof TPSReward tps) {
			double shown = amount > 0.0f ? amount : tps.getAmount();
			return Component.translatable("worldboss.dragonminez.results.tps", NumberFormattingUtil.formatLargeNumber(shown));
		}
		return name(reward);
	}
}
