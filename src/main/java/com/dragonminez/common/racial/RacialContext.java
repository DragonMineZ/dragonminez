package com.dragonminez.common.racial;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;

public record RacialContext(ServerPlayer player, StatsData data) {

	public GeneralServerConfig.RacialSkillsConfig config() {
		return ConfigManager.getServerConfig().getRacialSkills();
	}
}
