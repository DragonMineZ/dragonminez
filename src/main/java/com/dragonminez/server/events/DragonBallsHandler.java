package com.dragonminez.server.events;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallSetDefinition;
import com.dragonminez.common.init.block.entity.DragonBallBlockEntity;
import com.dragonminez.common.init.entities.dragon.DragonWishEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.DragonSkyS2C;
import com.dragonminez.common.network.S2C.RadarSyncS2C;
import com.dragonminez.server.world.data.DragonBallSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class DragonBallsHandler {
	public static final long DRAGON_WAIT_TICKS = 20L * 60L * 10L;

	private static final Queue<Runnable> generationQueue = new ConcurrentLinkedQueue<>();
	private static final int MAINTENANCE_INTERVAL = 20;
	private static final int RADAR_SYNC_INTERVAL = 100;
	private static final Random RANDOM = new Random();
	private static final Map<ServerLevel, Boolean> DRAGON_SKY = new WeakHashMap<>();
	private static boolean radarDirty = false;

	public static void scatterDragonBalls(ServerLevel level, String setId) {
		DragonBallSetDefinition definition = DragonBallDefinitions.getBallSet(setId);
		if (definition == null || !definition.isNaturalSpawn() || !definition.supportsDimension(level.dimension())) return;

		DragonBallSavedData data = DragonBallSavedData.get(level);
		validateLoadedActiveBalls(level, data, definition);

		boolean isFirstSpawn = !data.isFirstSpawnComplete(setId);
		if (!isFirstSpawn) topUpCopies(level, setId);

		int maxSets = definition.getCopies();
		int setsToSpawn = isFirstSpawn ? maxSets : 1;
		for (int star : definition.getStars()) {
			int actualToSpawn = Math.min(setsToSpawn, maxSets - data.getTrackedCount(setId, star));
			for (int i = 0; i < actualToSpawn; i++) addPendingBall(level, data, definition, star);
		}

		if (isFirstSpawn) {
			data.setFirstSpawnComplete(setId, true);
			data.setSpawnedCopies(setId, maxSets);
		}
		data.setDirty();
		syncRadar(level);
	}

	public static void topUpCopies(ServerLevel level, String setId) {
		DragonBallSetDefinition definition = DragonBallDefinitions.getBallSet(setId);
		if (definition == null || !definition.isNaturalSpawn() || !definition.supportsDimension(level.dimension())) return;
		DragonBallSavedData data = DragonBallSavedData.get(level);
		if (!data.isFirstSpawnComplete(setId)) return;

		int copies = definition.getCopies();
		int spawned = data.getSpawnedCopies(setId);
		if (spawned < 0) {
			int tracked = 0;
			for (int star : definition.getStars()) tracked = Math.max(tracked, data.getTrackedCount(setId, star));
			spawned = Math.max(1, Math.min(copies, tracked));
		}
		if (copies > spawned) {
			int delta = copies - spawned;
			for (int star : definition.getStars()) {
				int toSpawn = Math.min(delta, copies - data.getTrackedCount(setId, star));
				for (int i = 0; i < toSpawn; i++) addPendingBall(level, data, definition, star);
			}
			LogUtil.info(Env.SERVER, "Dragon Ball set " + setId + " raised from " + spawned + " to " + copies + " copies");
		}
		if (data.getSpawnedCopies(setId) != copies) data.setSpawnedCopies(setId, copies);
	}

	public static int regenerateSet(ServerLevel level, String setId) {
		DragonBallSetDefinition definition = DragonBallDefinitions.getBallSet(setId);
		if (definition == null || !definition.isNaturalSpawn() || !definition.supportsDimension(level.dimension())) return 0;
		DragonBallSavedData data = DragonBallSavedData.get(level);
		validateLoadedActiveBalls(level, data, definition);

		int copies = definition.getCopies();
		int added = 0;
		for (int star : definition.getStars()) {
			while (data.getTrackedCount(setId, star) < copies) {
				addPendingBall(level, data, definition, star);
				added++;
			}
		}
		if (!data.isFirstSpawnComplete(setId)) data.setFirstSpawnComplete(setId, true);
		data.setSpawnedCopies(setId, copies);
		LogUtil.info(Env.SERVER, "Dragon Ball set " + setId + " regenerated by command, " + added + " ball(s) added");
		return added;
	}

	public static ServerLevel getHomeLevel(MinecraftServer server, DragonBallSetDefinition definition) {
		for (ResourceLocation dimension : definition.getValidDimensions()) {
			ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
			if (level != null) return level;
		}
		return null;
	}

	public static void registerSummon(ServerLevel level, DragonWishEntity dragon, String setId) {
		DragonBallSavedData.get(level).addSummon(dragon.getUUID(), setId == null ? "" : setId, dragon.getSummonExpiresAt());
		refreshDragonSky(level);
	}

	public static void refreshDragonSky(ServerLevel level) {
		boolean active = isDragonSkyActive(level);
		Boolean previous = DRAGON_SKY.put(level, active);
		if (previous == null ? !active : previous == active) return;
		DragonSkyS2C packet = new DragonSkyS2C(level.dimension().location(), active);
		for (ServerPlayer player : level.players()) NetworkHandler.sendToPlayer(packet, player);
	}

	private static boolean isDragonSkyActive(ServerLevel level) {
		for (UUID dragonId : DragonBallSavedData.get(level).getSummons().keySet()) {
			if (!(level.getEntity(dragonId) instanceof DragonWishEntity dragon) || !dragon.isFading()) return true;
		}
		return false;
	}

	private static void syncDragonSky(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		NetworkHandler.sendToPlayer(new DragonSkyS2C(level.dimension().location(), isDragonSkyActive(level)), player);
	}

	private static void addPendingBall(ServerLevel level, DragonBallSavedData data, DragonBallSetDefinition definition, int star) {
		int range = definition.getSpawnRange();
		BlockPos spawnPos = level.getSharedSpawnPos();
		int x = spawnPos.getX() + RANDOM.nextInt(range * 2) - range;
		int z = spawnPos.getZ() + RANDOM.nextInt(range * 2) - range;
		BlockPos targetPos = new BlockPos(x, 0, z);
		data.getPendingBalls(definition.getId(), star).add(targetPos);
		data.setDirty();
		LogUtil.debug(Env.SERVER, "Dragon Ball (pending) [" + star + "] assigned to " + targetPos + " for set " + definition.getId() + " (Y is a dummy value)");
		if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) != null) {
			generationQueue.add(() -> generateBallSafely(level, definition, star, targetPos));
		}
		syncRadar(level);
	}

	private static void respawnBall(MinecraftServer server, DragonBallSetDefinition definition, int star, ServerLevel preferred) {
		if (!ConfigManager.getServerConfig().getWorldGen().getGenerateDragonBalls() || !definition.isNaturalSpawn()) return;
		ServerLevel level = preferred != null && definition.supportsDimension(preferred.dimension()) ? preferred : getHomeLevel(server, definition);
		if (level == null) return;
		DragonBallSavedData data = DragonBallSavedData.get(level);
		if (data.getTrackedCount(definition.getId(), star) >= definition.getCopies()) return;
		addPendingBall(level, data, definition, star);
	}

	private static void dropStaleBall(ServerLevel level, DragonBallSavedData data, DragonBallSetDefinition definition, int star, BlockPos pos) {
		if (!data.getActiveBalls(definition.getId(), star).remove(pos)) return;
		data.setDirty();
		LogUtil.warn(Env.SERVER, "Dragon Ball [" + star + "] of set " + definition.getId() + " was tracked at " + pos + " but is no longer there, respawning it");
		respawnBall(level.getServer(), definition, star, level);
		syncRadar(level);
	}

	private static void validateLoadedActiveBalls(ServerLevel level, DragonBallSavedData data, DragonBallSetDefinition definition) {
		ServerChunkCache chunks = level.getChunkSource();
		for (int star : definition.getStars()) {
			Block expected = definition.getBlockForStar(star);
			if (expected == null) continue;
			for (BlockPos pos : new ArrayList<>(data.getActiveBalls(definition.getId(), star))) {
				LevelChunk chunk = chunks.getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
				if (chunk != null && !chunk.getBlockState(pos).is(expected)) dropStaleBall(level, data, definition, star, pos);
			}
		}
	}

	@SubscribeEvent
	public static void onChunkLoad(ChunkEvent.Load event) {
		if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
		DragonBallSavedData data = DragonBallSavedData.get(level);
		ChunkPos chunkPos = chunk.getPos();

		for (BlockEntity blockEntity : new ArrayList<>(chunk.getBlockEntities().values())) {
			if (blockEntity instanceof DragonBallBlockEntity) {
				onDragonBallPlaced(level, chunk.getBlockState(blockEntity.getBlockPos()).getBlock(), blockEntity.getBlockPos().immutable());
			}
		}

		for (String setId : new ArrayList<>(data.getKnownSetIds())) {
			DragonBallSetDefinition definition = DragonBallDefinitions.getBallSet(setId);
			if (definition == null) continue;
			for (int star : definition.getStars()) {
				Block expected = definition.getBlockForStar(star);
				if (expected != null) {
					for (BlockPos pos : new ArrayList<>(data.getActiveBalls(setId, star))) {
						if ((pos.getX() >> 4) == chunkPos.x && (pos.getZ() >> 4) == chunkPos.z && !chunk.getBlockState(pos).is(expected)) {
							dropStaleBall(level, data, definition, star, pos);
						}
					}
				}
				if (!definition.supportsDimension(level.dimension())) continue;
				for (BlockPos target : new ArrayList<>(data.getPendingBalls(setId, star))) {
					if ((target.getX() >> 4) == chunkPos.x && (target.getZ() >> 4) == chunkPos.z) {
						generationQueue.add(() -> generateBallSafely(level, definition, star, target));
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
		if (level.getGameTime() % MAINTENANCE_INTERVAL == 0) maintainLevel(level);
		while (!generationQueue.isEmpty()) {
			Runnable task = generationQueue.poll();
			if (task != null) task.run();
		}
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.getServer() == null) return;
		if (event.getServer().getTickCount() % RADAR_SYNC_INTERVAL == 0) radarDirty = true;
		if (!radarDirty) return;
		radarDirty = false;
		RadarSyncS2C packet = buildRadarPacket(event.getServer());
		if (packet != null) NetworkHandler.sendToAllPlayers(packet);
	}

	private static void maintainLevel(ServerLevel level) {
		DragonBallSavedData data = DragonBallSavedData.get(level);
		ServerChunkCache chunks = level.getChunkSource();
		for (String setId : new ArrayList<>(data.getKnownSetIds())) {
			DragonBallSetDefinition definition = DragonBallDefinitions.getBallSet(setId);
			if (definition == null) continue;
			validateLoadedActiveBalls(level, data, definition);
			if (!definition.supportsDimension(level.dimension())) continue;
			for (int star : definition.getStars()) {
				for (BlockPos target : new ArrayList<>(data.getPendingBalls(setId, star))) {
					if (chunks.getChunkNow(target.getX() >> 4, target.getZ() >> 4) != null) {
						generationQueue.add(() -> generateBallSafely(level, definition, star, target));
					}
				}
			}
		}
		expireAbandonedSummons(level, data);
		refreshDragonSky(level);
	}

	private static void expireAbandonedSummons(ServerLevel level, DragonBallSavedData data) {
		long now = level.getGameTime();
		for (Map.Entry<UUID, DragonBallSavedData.Summon> entry : new ArrayList<>(data.getSummons().entrySet())) {
			if (now < entry.getValue().expiresAt()) continue;
			Entity entity = level.getEntity(entry.getKey());
			if (entity instanceof DragonWishEntity dragon && !dragon.isRemoved()) {
				dragon.beginDespawn();
				continue;
			}
			data.removeSummon(entry.getKey());
			if (ConfigManager.getServerConfig().getWorldGen().getGenerateDragonBalls() && !entry.getValue().setId().isBlank()) {
				LogUtil.info(Env.SERVER, "Dragon " + entry.getKey() + " was left waiting while unloaded, rescattering set " + entry.getValue().setId());
				scatterDragonBalls(level, entry.getValue().setId());
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			syncRadarForPlayer(player);
			syncDragonSky(player);
			scheduleDelayedSync(player, 40);
		}
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			syncRadarForPlayer(player);
			syncDragonSky(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			syncRadarForPlayer(player);
			syncDragonSky(player);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onBallItemExpire(ItemExpireEvent event) {
		ItemEntity item = event.getEntity();
		if (item.level() instanceof ServerLevel level) respawnLostItem(level, item.getItem());
	}

	@SubscribeEvent
	public static void onBallItemLeaveLevel(EntityLeaveLevelEvent event) {
		if (!(event.getEntity() instanceof ItemEntity item) || !(event.getLevel() instanceof ServerLevel level)) return;
		Entity.RemovalReason reason = item.getRemovalReason();
		boolean lost = reason == Entity.RemovalReason.KILLED
				|| (reason == Entity.RemovalReason.DISCARDED && item.getY() < level.getMinBuildHeight() - 64);
		if (lost) respawnLostItem(level, item.getItem());
	}

	private static void respawnLostItem(ServerLevel level, ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) return;
		Block block = blockItem.getBlock();
		DragonBallSetDefinition definition = DragonBallDefinitions.getBallSetForBlock(block);
		if (definition == null) return;
		Integer star = definition.getStarForBlock(block);
		if (star == null) return;
		LogUtil.info(Env.SERVER, "Dragon Ball [" + star + "] of set " + definition.getId() + " was lost as an item, respawning it");
		for (int i = 0; i < stack.getCount(); i++) respawnBall(level.getServer(), definition, star, level);
	}

	private static void scheduleDelayedSync(ServerPlayer player, int delayTicks) {
		player.server.tell(new net.minecraft.server.TickTask(player.server.getTickCount() + delayTicks, () -> {
			if (player.hasDisconnected()) return;
			syncRadarForPlayer(player);
		}));
	}

	private static void generateBallSafely(ServerLevel level, DragonBallSetDefinition definition, int star, BlockPos targetXZ) {
		DragonBallSavedData data = DragonBallSavedData.get(level);
		List<BlockPos> pendingForStar = data.getPendingBalls(definition.getId(), star);
		if (!pendingForStar.contains(targetXZ)) return;
		if (level.getChunkSource().getChunkNow(targetXZ.getX() >> 4, targetXZ.getZ() >> 4) == null) return;

		Block block = definition.getBlockForStar(star);
		if (block == null) return;

		int x = targetXZ.getX();
		int z = targetXZ.getZ();
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos realPos = new BlockPos(x, y, z);

		BlockPos.MutableBlockPos mutable = realPos.mutable();
		while (mutable.getY() > level.getMinBuildHeight() && level.getBlockState(mutable.below()).canBeReplaced()) {
			mutable.move(0, -1, 0);
		}
		realPos = mutable.immutable();

		while (!level.getBlockState(realPos).canBeReplaced() && realPos.getY() < level.getMaxBuildHeight() - 1) {
			realPos = realPos.above();
		}

		if (level.isOutsideBuildHeight(realPos)) return;

		BlockState below = level.getBlockState(realPos.below());
		if (below.isAir() || below.is(Blocks.WATER)) {
			level.setBlock(realPos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
		}

		boolean success = level.setBlock(realPos, block.defaultBlockState(), 2);
		if (!success || !level.getBlockState(realPos).is(block)) return;

		pendingForStar.remove(targetXZ);
		onDragonBallPlaced(level, block, realPos);
		data.setDirty();
		LogUtil.info(Env.SERVER, "Dragon Ball [" + star + "] physically generated at " + realPos + " for set " + definition.getId());
	}

	public static void onDragonBallPlaced(ServerLevel level, Block block, BlockPos pos) {
		DragonBallSetDefinition definition = DragonBallDefinitions.getBallSetForBlock(block);
		if (definition == null) return;
		Integer star = definition.getStarForBlock(block);
		if (star == null) return;
		DragonBallSavedData data = DragonBallSavedData.get(level);
		List<BlockPos> positions = data.getActiveBalls(definition.getId(), star);
		if (positions.contains(pos)) return;
		positions.add(pos);
		data.setDirty();
		syncRadar(level);
	}

	public static void onDragonBallRemoved(ServerLevel level, Block block, BlockPos pos) {
		DragonBallSetDefinition definition = DragonBallDefinitions.getBallSetForBlock(block);
		if (definition == null) return;
		Integer star = definition.getStarForBlock(block);
		if (star == null) return;
		DragonBallSavedData data = DragonBallSavedData.get(level);
		if (!data.getActiveBalls(definition.getId(), star).remove(pos)) return;
		data.setDirty();
		syncRadar(level);
	}

	public static void syncRadar(ServerLevel level) {
		radarDirty = true;
	}

	public static void syncRadarForPlayer(ServerPlayer player) {
		if (player == null) return;
		RadarSyncS2C packet = buildRadarPacket(player.serverLevel().getServer());
		if (packet != null) NetworkHandler.sendToPlayer(packet, player);
	}

	private static RadarSyncS2C buildRadarPacket(MinecraftServer server) {
		if (server == null) return null;
		Map<String, List<BlockPos>> positionsBySet = new HashMap<>();
		for (DragonBallSetDefinition definition : DragonBallDefinitions.getBallSets()) {
			List<BlockPos> positions = positionsBySet.computeIfAbsent(definition.getId(), ignored -> new ArrayList<>());
			for (ResourceLocation dimension : definition.getValidDimensions()) {
				ServerLevel setLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
				if (setLevel == null) continue;
				positions.addAll(DragonBallSavedData.get(setLevel).getAllKnownPositionsForRadar(definition.getId()));
			}
		}
		List<BlockPos> earthPositions = new ArrayList<>(positionsBySet.getOrDefault("earth", List.of()));
		List<BlockPos> namekPositions = new ArrayList<>(positionsBySet.getOrDefault("namek", List.of()));
		return new RadarSyncS2C(earthPositions, namekPositions, positionsBySet);
	}
}
