package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.quest.*;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class ClaimSideQuestRewardC2S {
	private final String sideQuestId;

	public ClaimSideQuestRewardC2S(String sideQuestId) {
		this.sideQuestId = sideQuestId;
	}

	public ClaimSideQuestRewardC2S(FriendlyByteBuf buffer) {
		this.sideQuestId = buffer.readUtf();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(sideQuestId);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
				Quest sideQuest = QuestRegistry.getQuest(sideQuestId);
				if (sideQuest == null) return;

				PlayerQuestData pqd = stats.getPlayerQuestData();
				if (!pqd.isQuestCompleted(sideQuestId)) return;

				List<QuestReward> rewards = sideQuest.getRewards();
				boolean anyClaimed = false;

				for (int i = 0; i < rewards.size(); i++) {
					if (!pqd.isRewardClaimed(sideQuestId, i)) {
						rewards.get(i).giveReward(player);
						pqd.claimReward(sideQuestId, i);
						anyClaimed = true;
					}
				}

				if (anyClaimed) NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
	}
}
