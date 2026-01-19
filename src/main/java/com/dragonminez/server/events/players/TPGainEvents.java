package com.dragonminez.server.events.players;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.world.dimension.HTCDimension;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber
public class TPGainEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTPGain(DMZEvent.TPGainEvent event) {
        if (event.getPlayer() == null || event.getTpGain() <= 0) return;
		int baseTP = event.getTpGain();
		final int[] modifiedTP = {event.getTpGain()};

        if (event.getPlayer().level().dimension().equals(HTCDimension.HTC_KEY)) {
            double htcMultiplier = ConfigManager.getServerConfig().getGameplay().getHTCTpMultiplier();
            modifiedTP[0] = (int) (baseTP + baseTP * htcMultiplier);
        }

		if (event.getPlayer() instanceof ServerPlayer player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (data.getStatus().isFused() && data.getStatus().isFusionLeader()) {
					UUID partnerUUID = data.getStatus().getFusionPartnerUUID();
					if (partnerUUID != null) {
						ServerPlayer partner = player.getServer().getPlayerList().getPlayer(partnerUUID);
						if (partner != null) {
							int shareAmount = modifiedTP[0] / 2;
							StatsProvider.get(StatsCapability.INSTANCE, partner).ifPresent(pData -> {
								pData.getResources().addTrainingPoints(shareAmount);
								NetworkHandler.sendToPlayer(new StatsSyncS2C(partner), partner);
							});
						}
					}
				}
			});
		}

		// FrostDemon passive
		if (ConfigManager.getServerConfig().getRacialSkills().isEnableRacialSkills() && ConfigManager.getServerConfig().getRacialSkills().isFrostDemonRacialSkill()) {
			StatsProvider.get(StatsCapability.INSTANCE, event.getPlayer()).ifPresent(data -> {
				if (data.getCharacter().getRace().equals("frostdemon")) {
					double frostDemonMultiplier = ConfigManager.getServerConfig().getRacialSkills().getFrostDemonTPBoost();
					modifiedTP[0] = (int) (modifiedTP[0] + baseTP * frostDemonMultiplier);
				}
			});
		}

        event.setTpGain(modifiedTP[0]);
    }
}

