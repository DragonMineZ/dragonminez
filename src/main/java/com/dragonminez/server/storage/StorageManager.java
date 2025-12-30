package com.dragonminez.server.storage;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StorageManager {
	private static IDataStorage activeStorage;
	private static ScheduledExecutorService autoSaveScheduler;

	public static void init() {
		GeneralServerConfig.StorageConfig.StorageType type = ConfigManager.getServerConfig().getStorage().getStorageType();

		switch (type) {
			case DATABASE -> activeStorage = new DatabaseManager();
			case JSON -> activeStorage = new JsonStorage();
			case NBT -> {
				LogUtil.info(Env.SERVER, "Using default NBT storage (Vanilla).");
				activeStorage = null;
			}
		}

		if (activeStorage != null) {
			activeStorage.init();
			startAutoSave();
		}
	}

	public static void shutdown() {
		if (autoSaveScheduler != null && !autoSaveScheduler.isShutdown()) {
			autoSaveScheduler.shutdown();
		}
		if (activeStorage != null) {
			activeStorage.shutdown();
		}
	}

	public static void loadPlayer(ServerPlayer player) {
		if (activeStorage == null) return;

		CompoundTag loadedData = activeStorage.loadData(player.getUUID());

		if (loadedData != null) {
			MinecraftForge.EVENT_BUS.post(new DMZEvent.PlayerDataLoadEvent(player, loadedData));

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
				stats.load(loadedData);

				if (!stats.getQuestData().isSagaUnlocked("saiyan_saga")) {
					stats.getQuestData().unlockSaga("saiyan_saga");
				}

				NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
			});

			LogUtil.info(Env.SERVER, "Loaded data for " + player.getName().getString() + " from " + activeStorage.getName());
		}
	}

	public static void savePlayer(ServerPlayer player) {
		if (activeStorage == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			CompoundTag dataToSave = stats.save();

			MinecraftForge.EVENT_BUS.post(new DMZEvent.PlayerDataSaveEvent(player, dataToSave));

			activeStorage.saveData(player.getUUID(), player.getScoreboardName(), dataToSave);
		});
	}

	private static void startAutoSave() {
		autoSaveScheduler = Executors.newSingleThreadScheduledExecutor();
		autoSaveScheduler.scheduleAtFixedRate(StorageManager::performAutoSave, 5, 5, TimeUnit.MINUTES);
	}

	private static void performAutoSave() {
		if (ServerLifecycleHooks.getCurrentServer() == null || activeStorage == null) return;

		LogUtil.info(Env.SERVER, "Auto-Saving data...");
		for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
			savePlayer(player);
		}
	}
}