package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@Getter
public class KiTechniqueReward extends QuestReward {
	private final KiAttackData template;

	public KiTechniqueReward(KiAttackData template) {
		super(RewardType.KI_TECHNIQUE);
		this.template = template;
	}

	@Override
	public void giveReward(ServerPlayer player) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			KiAttackData copy = new KiAttackData();
			copy.load(template.save());
			data.getTechniques().unlockTechnique(copy);
		});
	}

	@Override
	public Component getDescription() {
		return Component.translatable(
				"gui.dragonminez.quests.rewards.ki_technique",
				Component.literal(template.getName())
		);
	}
}
