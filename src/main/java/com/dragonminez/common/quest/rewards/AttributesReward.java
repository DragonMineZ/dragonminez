package com.dragonminez.common.quest.rewards;

import com.dragonminez.client.util.NumberFormattingUtil;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

@Getter
public class AttributesReward extends QuestReward {
	private final int amount;

	public AttributesReward(int amount) {
		super(RewardType.ATTRIBUTES);
		this.amount = amount;
	}

	public int scaledAmount(double rewardMultiplier) {
		return (int) Math.max(0, Math.round(amount * rewardMultiplier));
	}

	@Override
	public void giveReward(ServerPlayer player) {
		giveReward(player, 1.0);
	}

	@Override
	public void giveReward(ServerPlayer player, double rewardMultiplier) {
		int scaled = scaledAmount(rewardMultiplier);
		if (scaled <= 0) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> data.grantAttributeReward(scaled));
	}

	@Override
	public Component getDescription() {
		return Component.translatable("gui.dragonminez.quests.rewards.attributes", amount);
	}

	@Override
	public Component getDescription(double rewardMultiplier) {
		return Component.translatable("gui.dragonminez.quests.rewards.attributes", scaledAmount(rewardMultiplier));
	}

	@Override
	public List<Component> getTooltipLines(double rewardMultiplier, StatsData data) {
		return breakdown(getDescription(rewardMultiplier), scaledAmount(rewardMultiplier), data);
	}

	public static List<Component> breakdown(Component title, int attributes, StatsData data) {
		List<Component> lines = new ArrayList<>();
		lines.add(title);
		if (data == null || attributes <= 0) return lines;
		int points = data.attributeRewardPointShare(attributes);
		int tpAttributes = attributes - points;
		lines.add(Component.translatable("gui.dragonminez.quests.rewards.attributes.breakdown", points, tpAttributes)
				.withStyle(ChatFormatting.GRAY));
		if (tpAttributes > 0) {
			double tp = data.estimateAttributeRewardTp(attributes);
			lines.add(Component.translatable("gui.dragonminez.quests.rewards.attributes.tp_now",
					NumberFormattingUtil.formatFullTps(Math.round(tp))).withStyle(ChatFormatting.DARK_GRAY));
		}
		return lines;
	}
}
