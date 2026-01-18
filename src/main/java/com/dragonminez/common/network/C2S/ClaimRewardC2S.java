package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.quest.SagaManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class ClaimRewardC2S {
	private final String sagaId;
	private final int questId;

	public ClaimRewardC2S(String sagaId, int questId) {
		this.sagaId = sagaId;
		this.questId = questId;
	}

	public ClaimRewardC2S(FriendlyByteBuf buffer) {
		this.sagaId = buffer.readUtf();
		this.questId = buffer.readInt();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(sagaId);
		buffer.writeInt(questId);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
				Saga saga = SagaManager.getSaga(sagaId);
				if (saga == null) return;
				Quest quest = saga.getQuestById(questId);
				if (quest == null) return;

				if (stats.getQuestData().isQuestCompleted(sagaId, questId)) {

					List<QuestReward> rewards = quest.getRewards();
					boolean anyClaimed = false;

					for (int i = 0; i < rewards.size(); i++) {
						if (!stats.getQuestData().isRewardClaimed(sagaId, questId, i)) {
							QuestReward reward = rewards.get(i);

							reward.giveReward(player);

							stats.getQuestData().claimReward(sagaId, questId, i);
							anyClaimed = true;
						}
					}

					if (anyClaimed) {
						NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
					}
				}
			});
		});
		context.setPacketHandled(true);
	}
}