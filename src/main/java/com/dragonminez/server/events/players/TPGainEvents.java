package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.util.GravityLogic;
import com.dragonminez.server.world.dimension.HTCDimension;
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

		double additiveMultiplier = 1.0;

		// Gravity Bonus
		if (player.level().dimension().equals(HTCDimension.HTC_KEY)) additiveMultiplier += (ConfigManager.getServerConfig().getGameplay().getHTCTpMultiplier() - 1.0);
		else {
			double bonusGravity = GravityLogic.getBonusGravity(player);
			if (bonusGravity > 0) additiveMultiplier += (bonusGravity * 0.05);
		}

		// Frost Demon Racial Skill Bonus :p
		String race = data.getCharacter().getRace();

		if (ConfigManager.getServerConfig().getRacialSkills().getEnableRacialSkills()
				&& ConfigManager.getServerConfig().getRacialSkills().getFrostDemonRacialSkill()
				&& "frostdemon".equals(race)) {
			additiveMultiplier += (ConfigManager.getServerConfig().getRacialSkills().getFrostDemonTPBoost() - 1.0);
		}

		// Per Class Bonus
		var raceStats = ConfigManager.getRaceStats(race);
		if (raceStats != null) {
			var classStats = raceStats.getClassStats(data.getCharacter().getCharacterClass());
			if (classStats != null) additiveMultiplier += (classStats.getTpGainMultiplier() - 1.0);
		}

		double globalMultiplier = ConfigManager.getServerConfig().getGameplay().getTpsGainMultiplier();
		int finalTP = (int) (baseTP * additiveMultiplier * globalMultiplier);

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
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(partner), partner);
		});
	}
}