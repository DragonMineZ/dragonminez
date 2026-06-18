package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.server.util.MutantManager;
import com.dragonminez.server.world.data.MutantSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MutantEvents {

	private static final int CHECK_INTERVAL_TICKS = 100;

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;

		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return;
		if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;

		GeneralServerConfig.MutantConfig cfg = ConfigManager.getServerConfig() != null
				? ConfigManager.getServerConfig().getMutant() : null;
		if (cfg == null || !cfg.getEnabled()) return;

		var overworld = server.getLevel(Level.OVERWORLD);
		if (overworld == null) return;

		MutantSavedData saved = MutantSavedData.get(server);
		long now = overworld.getGameTime();
		long intervalTicks = (long) cfg.getRollIntervalMinutes() * 60L * 20L;

		if (saved.getNextRollTick() < 0) {
			saved.setNextRollTick(now + intervalTicks);
			return;
		}

		if (now >= saved.getNextRollTick()) {
			MutantManager.runLottery(server);
			saved.setNextRollTick(now + intervalTicks);
		}
	}
}
