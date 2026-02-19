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

import java.util.UUID;
import java.util.concurrent.*;

public class StorageManager {
	private static IDataStorage activeStorage;
	private static ScheduledExecutorService autoSaveScheduler;
	private static ExecutorService dbExecutor;

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
			int threads = ConfigManager.getServerConfig().getStorage().getThreadPoolSize();
			dbExecutor = Executors.newFixedThreadPool(threads);
			LogUtil.info(Env.SERVER, "Storage initialized with " + threads + " async threads.");
			startAutoSave();
		}
	}

	public static void reload() {
		LogUtil.info(Env.SERVER, "Reloading Storage Subsystem...");

		if (ServerLifecycleHooks.getCurrentServer() != null) {
			LogUtil.info(Env.SERVER, "Saving online players before storage switch...");
			for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
				savePlayer(player);
			}
		}

		shutdown();
		init();
		LogUtil.info(Env.SERVER, "Storage Subsystem reloaded. Active: " + (activeStorage == null ? "NBT (Vanilla)" : activeStorage.getName()));
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

		final UUID uuid = player.getUUID();

		CompletableFuture.supplyAsync(() -> activeStorage.loadData(uuid), dbExecutor)
				.thenAccept(loadedData -> {
					ServerLifecycleHooks.getCurrentServer().execute(() -> {
						if (loadedData != null && player.connection != null) {
							applyLoadedData(player, loadedData);
						}
					});
				})
				.exceptionally(ex -> {
					LogUtil.error(Env.SERVER, "Error loading data async for " + player.getName().getString(), ex);
					return null;
				});
	}

	private static void applyLoadedData(ServerPlayer player, CompoundTag loadedData) {
		MinecraftForge.EVENT_BUS.post(new DMZEvent.PlayerDataLoadEvent(player, loadedData));

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			stats.load(loadedData);

			if (!stats.getQuestData().isSagaUnlocked("saiyan_saga")) {
				stats.getQuestData().unlockSaga("saiyan_saga");
			}

			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			LogUtil.info(Env.SERVER, "Async data loaded for: " + player.getName().getString());
		});
	}

	public static void savePlayer(ServerPlayer player) {
		if (activeStorage == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			CompoundTag dataToSave = stats.save();

			MinecraftForge.EVENT_BUS.post(new DMZEvent.PlayerDataSaveEvent(player, dataToSave));

			String name = player.getScoreboardName();
			UUID uuid = player.getUUID();

			dbExecutor.submit(() -> {
				try {
					activeStorage.saveData(uuid, name, dataToSave);
				} catch (Exception e) {
					LogUtil.error(Env.SERVER, "Failed to save data async for " + name, e);
				}
			});
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