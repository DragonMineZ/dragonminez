package com.dragonminez.common.wish.wishes;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.wish.Wish;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

public class PassiveResetWish extends Wish {

	public PassiveResetWish(String name, String description) {
		super(name, description, "passivereset");
	}

	@Override
	public void grant(ServerPlayer player) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			var ownedBonusNames = data.getRacialData().getOwnedBonusNames();
			if (!ownedBonusNames.isEmpty()) {
				for (String bonusName : new ArrayList<>(ownedBonusNames)) {
					data.getBonusStats().removeAllBonuses(bonusName);
				}
				ownedBonusNames.clear();
			} else {
				clearLegacyBonuses(data);
			}

			data.getCooldowns().removeCooldown(Cooldowns.ZENKAI);
			data.getResources().setRacialSkillCount(0);
		});
	}

	private static void clearLegacyBonuses(StatsData data) {
		String[] statBoosts = switch (data.getCharacter().getRace()) {
			case "namekian" -> ConfigManager.getServerConfig().getRacialSkills().getNamekianAssimilationBoosts();
			case "majin" -> ConfigManager.getServerConfig().getRacialSkills().getMajinAbsorptionBoosts();
			case "saiyan" -> ConfigManager.getServerConfig().getRacialSkills().getSaiyanZenkaiBoosts();
			default -> new String[0];
		};

		for (String stat : statBoosts) {
			for (int i = data.getResources().getRacialSkillCount(); i >= 0; i--) {
				data.getBonusStats().clearBonusSplit(stat, "Absorption_");
				data.getBonusStats().clearBonusSplit(stat, "Assimilation_");
				data.getBonusStats().clearBonusSplit(stat, "Zenkai_");
			}
		}
	}

}
