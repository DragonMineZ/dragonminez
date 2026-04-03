package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class TPGainEvents {

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onTPGain(DMZEvent.TPGainEvent event) {
		if (!(event.getPlayer() instanceof ServerPlayer player)) return;
		int baseTP = event.getTpGain();
		if (baseTP <= 0) return;
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null) return;
		int finalTP = data.calculateTPGain(baseTP);

		if (data.getStatus().isFused() && data.getStatus().isFusionLeader()) shareWithFusionPartner(player, data, finalTP);
		event.setTpGain(finalTP);
	}

	private static void shareWithFusionPartner(ServerPlayer leader, StatsData leaderData, int totalTP) {
		UUID partnerUUID = leaderData.getStatus().getFusionPartnerUUID();
		if (partnerUUID == null) return;

		ServerPlayer partner = leader.getServer().getPlayerList().getPlayer(partnerUUID);
		if (partner == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, partner).ifPresent(pData -> {
			pData.getResources().addTrainingPoints(totalTP / 2);
			NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(partner), partner);
		});
	}
}