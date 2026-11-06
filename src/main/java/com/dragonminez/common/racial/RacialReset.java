package com.dragonminez.common.racial;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.racial.impl.BioAndroidEvolution;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

public final class RacialReset {
	private static final String ABSORPTION_SLOT_COOLDOWN = "AbsorptionSlot_";
	private static final String CELL_JR_SLOT_COOLDOWN = "CellJrSlot_";

	private RacialReset() {
	}

	public static void reset(ServerPlayer player, StatsData data) {
		RacialData racial = data.getRacialData();
		BioAndroidEvolution.despawnAllCellJrs(new RacialContext(player, data));

		var ownedBonusNames = racial.getOwnedBonusNames();
		if (!ownedBonusNames.isEmpty()) {
			for (String bonusName : new ArrayList<>(ownedBonusNames)) data.getBonusStats().removeAllBonuses(bonusName);
			ownedBonusNames.clear();
		} else {
			clearLegacyBonuses(data);
		}

		racial.getAssimilations().clear();
		racial.getAbsorptions().clear();
		racial.getCellJrs().clear();
		racial.setAbsorptionSlotCounter(0);
		racial.setZenkaiPermanentUses(0);
		racial.setZenkaiReleaseBonus(0.0);
		racial.setTempZenkaiBuffName("");

		Cooldowns cooldowns = data.getCooldowns();
		cooldowns.removeCooldown(Cooldowns.ZENKAI);
		cooldowns.removeCooldown(Cooldowns.ZENKAI_TEMP_BUFF);
		for (String key : cooldowns.getAllCooldowns().keySet()) {
			if (key.startsWith(ABSORPTION_SLOT_COOLDOWN) || key.startsWith(CELL_JR_SLOT_COOLDOWN)) cooldowns.removeCooldown(key);
		}
		data.getResources().setRacialSkillCount(0);
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
