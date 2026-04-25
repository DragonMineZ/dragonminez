package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Resources;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@Getter
public class AlignmentReward extends QuestReward {
	private final int amount;

	public AlignmentReward(int amount) {
		super(RewardType.ALIGNMENT);
		this.amount = amount;
	}

	public void apply(StatsData data) {
		if (data == null) {
			return;
		}
		apply(data.getResources());
	}

	public void apply(Resources resources) {
		if (resources == null) {
			return;
		}
		resources.addAlignment(amount);
	}

	@Override
	public void giveReward(ServerPlayer player) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(this::apply);
	}

	@Override
	public Component getDescription() {
		return Component.translatable("gui.dragonminez.quests.rewards.alignment", amount);
	}
}
