package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UnlockSagaC2S {
	private final String sagaId;

	public UnlockSagaC2S(String sagaId) {
		this.sagaId = sagaId;
	}

	public UnlockSagaC2S(FriendlyByteBuf buffer) {
		this.sagaId = buffer.readUtf();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(sagaId);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;

			Saga saga = QuestRegistry.getSaga(sagaId);
			if (saga == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
				PlayerQuestData pqd = stats.getPlayerQuestData();
				if (!pqd.isSagaUnlocked(sagaId)) {
					pqd.setSagaUnlocked(sagaId, true);
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
				}
			});
		});
		context.setPacketHandled(true);
	}
}