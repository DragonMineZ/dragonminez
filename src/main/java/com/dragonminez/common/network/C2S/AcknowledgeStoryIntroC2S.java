package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AcknowledgeStoryIntroC2S {

	public AcknowledgeStoryIntroC2S() {
	}

	public static AcknowledgeStoryIntroC2S decode(FriendlyByteBuf buffer) {
		return new AcknowledgeStoryIntroC2S();
	}

	public static void encode(AcknowledgeStoryIntroC2S msg, FriendlyByteBuf buffer) {
		// no payload
	}

	public static void handle(AcknowledgeStoryIntroC2S msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getPlayerQuestData().isIntroPromptShown()) {
					data.getPlayerQuestData().setIntroPromptShown(true);
					NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
				}
			});
		});
		context.setPacketHandled(true);
	}
}

