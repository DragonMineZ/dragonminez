package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.FormMasteries;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@Getter
public class TransformationReward extends QuestReward {
	private final String formGroup;
	private final String formName;
	private final double mastery;
	private final boolean stack;

	public TransformationReward(String formGroup, String formName, double mastery, boolean stack) {
		super(RewardType.TRANSFORMATION);
		this.formGroup = formGroup;
		this.formName = formName;
		this.mastery = mastery;
		this.stack = stack;
	}

	@Override
	public void giveReward(ServerPlayer player) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			Character character = data.getCharacter();
			FormMasteries masteries = stack ? character.getStackFormMasteries() : character.getFormMasteries();
			double current = masteries.getMastery(formGroup, formName);
			if (mastery > current) {
				masteries.setMastery(formGroup, formName, mastery, Double.MAX_VALUE);
			}
		});
	}

	@Override
	public Component getDescription() {
		return Component.translatable(
				"gui.dragonminez.quests.rewards.transformation",
				Component.literal(formName.replace('_', ' '))
		);
	}
}
