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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

public class StorageManager {
	private static IDataStorage activeStorage;
	private static ScheduledExecutorService autoSaveScheduler;
	private static ExecutorService dbExecutor;
	private static final ConcurrentHashMap<UUID, CompletableFuture<Void>> saveChains = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<UUID, Object> pendingLoads = new ConcurrentHashMap<>();
	private static final Set<UUID> failedLoads = ConcurrentHashMap.newKeySet();
	private static final long LOAD_WARN_SECONDS = 30L;
	private static final int LOAD_MAX_ATTEMPTS = 3;
	private static final long LOAD_RETRY_DELAY_SECONDS = 5L;

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

		var server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			LogUtil.info(Env.SERVER, "Saving online players before storage switch...");
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				savePlayer(player);
			}
		}

		Set<UUID> reloadLoads = new HashSet<>(pendingLoads.keySet());
		reloadLoads.addAll(failedLoads);
		shutdown();
		pendingLoads.clear();
		failedLoads.clear();
		init();

		if (server != null) {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (reloadLoads.contains(player.getUUID())) loadPlayer(player);
			}
		}
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
		final String name = player.getName().getString();
		final IDataStorage storage = activeStorage;
		final ExecutorService executor = dbExecutor;
		final Object loadToken = new Object();
		pendingLoads.put(uuid, loadToken);
		failedLoads.remove(uuid);

		CompletableFuture<Void> previousSave = saveChains.getOrDefault(uuid, CompletableFuture.completedFuture(null));
		CompletableFuture<CompoundTag> load = previousSave.exceptionally(ex -> null)
				.thenCompose(ignored -> loadWithRetry(storage, uuid, name, executor, 1));

		CompletableFuture.delayedExecutor(LOAD_WARN_SECONDS, TimeUnit.SECONDS).execute(() -> {
			if (!load.isDone() && pendingLoads.get(uuid) == loadToken) {
				LogUtil.warn(Env.SERVER, "Storage load for {} is taking more than {}s; their storage saves stay paused until it finishes", name, LOAD_WARN_SECONDS);
			}
		});

		load.whenComplete((loadedData, ex) -> {
			var server = ServerLifecycleHooks.getCurrentServer();
			if (server == null) {
				pendingLoads.remove(uuid, loadToken);
				return;
			}
			server.execute(() -> {
				try {
					if (pendingLoads.get(uuid) != loadToken) return;
					if (ex != null) {
						failedLoads.add(uuid);
						LogUtil.error(Env.SERVER, "Could not load storage data for " + name + "; their storage record will not be overwritten this session", ex);
					} else if (loadedData != null && isCurrentOnlinePlayer(player)) {
						applyLoadedData(player, loadedData);
					}
				} finally {
					pendingLoads.remove(uuid, loadToken);
				}
			});
		});
	}

	private static CompletableFuture<CompoundTag> loadWithRetry(IDataStorage storage, UUID uuid, String name, ExecutorService executor, int attempt) {
		Executor runner = attempt == 1 ? executor : CompletableFuture.delayedExecutor(LOAD_RETRY_DELAY_SECONDS, TimeUnit.SECONDS, executor);
		return CompletableFuture.supplyAsync(() -> storage.loadData(uuid), runner)
				.handle((data, ex) -> {
					if (ex == null) return CompletableFuture.completedFuture(data);
					if (attempt >= LOAD_MAX_ATTEMPTS) return CompletableFuture.<CompoundTag>failedFuture(ex);
					LogUtil.warn(Env.SERVER, "Storage load attempt {} for {} failed, retrying: {}", attempt, name, ex.toString());
					return loadWithRetry(storage, uuid, name, executor, attempt + 1);
				})
				.thenCompose(future -> future);
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

		if (pendingLoads.containsKey(player.getUUID()) || failedLoads.contains(player.getUUID())) return;

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