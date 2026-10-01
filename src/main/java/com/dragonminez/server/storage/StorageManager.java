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
import com.dragonminez.common.util.TransformationsHelper;
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
	private static final ConcurrentHashMap<UUID, CompletableFuture<Void>> saveChains = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<UUID, Object> pendingLoads = new ConcurrentHashMap<>();
	private static final long LOAD_TIMEOUT_SECONDS = 30L;

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
			if (dbExecutor != null) {
				shutdownDbExecutor();
			}
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
		if (dbExecutor != null) {
			shutdownDbExecutor();
			dbExecutor = null;
		}
	}

	private static void shutdownDbExecutor() {
		dbExecutor.shutdown();
		try {
			if (!dbExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
				dbExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			dbExecutor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	public static void loadPlayer(ServerPlayer player) {
		if (activeStorage == null) return;

		final UUID uuid = player.getUUID();
		final IDataStorage storage = activeStorage;
		final Object loadToken = new Object();
		pendingLoads.put(uuid, loadToken);

		CompletableFuture<Void> previousSave = saveChains.getOrDefault(uuid, CompletableFuture.completedFuture(null));
		previousSave.exceptionally(ex -> null)
				.thenApplyAsync(ignored -> storage.loadData(uuid), dbExecutor)
				.orTimeout(LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.whenComplete((loadedData, ex) -> {
					if (ex != null) LogUtil.error(Env.SERVER, "Error loading data async for " + player.getName().getString(), ex);
					var server = ServerLifecycleHooks.getCurrentServer();
					if (server == null) {
						pendingLoads.remove(uuid, loadToken);
						return;
					}
					server.execute(() -> {
						try {
							if (ex == null && loadedData != null && pendingLoads.get(uuid) == loadToken && isCurrentOnlinePlayer(player)) {
								applyLoadedData(player, loadedData);
							}
						} finally {
							pendingLoads.remove(uuid, loadToken);
						}
					});
				});
	}

	public static boolean isLoadPending(ServerPlayer player) {
		return pendingLoads.containsKey(player.getUUID());
	}

	private static boolean isCurrentOnlinePlayer(ServerPlayer player) {
		if (player.hasDisconnected()) return false;
		var server = ServerLifecycleHooks.getCurrentServer();
		return server != null && server.getPlayerList().getPlayer(player.getUUID()) == player;
	}

	private static void applyLoadedData(ServerPlayer player, CompoundTag loadedData) {
		MinecraftForge.EVENT_BUS.post(new DMZEvent.PlayerDataLoadEvent(player, loadedData));

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			try {
				stats.load(loadedData);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}

			if (stats.getPlayerQuestData().isSagaLocked("saiyan_saga")) {
				stats.getPlayerQuestData().setSagaUnlocked("saiyan_saga", true);
			}

			TransformationsHelper.ensureSelectedFormDefault(stats);
			TransformationsHelper.ensureSelectedStackFormDefault(stats);

			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			StatsCapability.syncSkinPixels(player, stats);
			LogUtil.info(Env.SERVER, "Async data loaded for: " + player.getName().getString());
		});
	}

	public static void savePlayer(ServerPlayer player) {
		if (activeStorage == null) return;

		if (pendingLoads.containsKey(player.getUUID())) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			if (!stats.isDataLoaded() && !stats.getStatus().isHasCreatedCharacter()) return;
			CompoundTag dataToSave = stats.save();

			MinecraftForge.EVENT_BUS.post(new DMZEvent.PlayerDataSaveEvent(player, dataToSave));

			String name = player.getScoreboardName();
			UUID uuid = player.getUUID();

			saveChains.compute(uuid, (id, previous) -> {
				CompletableFuture<Void> previousStage = previous != null ? previous : CompletableFuture.completedFuture(null);
				CompletableFuture<Void> chained = previousStage.thenRunAsync(() -> {
					try {
						activeStorage.saveData(uuid, name, dataToSave);
					} catch (Exception e) {
						LogUtil.error(Env.SERVER, "Failed to save data async for " + name, e);
					}
				}, dbExecutor);
				chained.whenComplete((v, ex) -> saveChains.remove(uuid, chained));
				return chained;
			});
		});
	}

	private static void startAutoSave() {
		autoSaveScheduler = Executors.newSingleThreadScheduledExecutor();
		autoSaveScheduler.scheduleAtFixedRate(StorageManager::performAutoSave, 5, 5, TimeUnit.MINUTES);
	}

	private static void performAutoSave() {
		var server = ServerLifecycleHooks.getCurrentServer();
		if (server == null || activeStorage == null) return;

		server.execute(() -> {
			LogUtil.info(Env.SERVER, "Auto-Saving data...");
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				savePlayer(player);
			}
		});
	}
}