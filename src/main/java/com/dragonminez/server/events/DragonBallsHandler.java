package com.dragonminez.server.events;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallSetDefinition;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.RadarSyncS2C;
import com.dragonminez.server.world.data.DragonBallSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class DragonBallsHandler {
	private static final Queue<Runnable> generationQueue = new ConcurrentLinkedQueue<>();
	private static final int PENDING_RESCAN_INTERVAL = 100;
	private static int pendingRescanTimer = 0;

	private static final int RADAR_SYNC_INTERVAL = 100;
	private static int radarSyncTimer = 0;

	private static final double NEARBY_GEN_RANGE_SQR = 128.0 * 128.0;

	public static void scatterDragonBalls(ServerLevel level, String setId) {
		DragonBallSetDefinition definition = DragonBallDefinitions.getBallSet(setId);
		if (definition == null || !definition.supportsDimension(level.dimension())) return;

		DragonBallSavedData data = DragonBallSavedData.get(level);
		Random random = new Random();
		int range = definition.getSpawnRange();
		BlockPos spawnPos = level.getSharedSpawnPos();

		Map<Integer, List<BlockPos>> active = data.getActiveBalls(setId);
		Map<Integer, List<BlockPos>> pending = data.getPendingBalls(setId);

		boolean isFirstSpawn = !data.isFirstSpawnComplete(setId);
		int maxSets = definition.getCopies();
		int setsToSpawn = isFirstSpawn ? maxSets : 1;

		for (int star : definition.getStars()) {
			int currentCount = active.get(star).size() + pending.get(star).size();
			int actualToSpawn = Math.min(setsToSpawn, maxSets - currentCount);
			for (int i = 0; i < actualToSpawn; i++) {
				int x = spawnPos.getX() + random.nextInt(range * 2) - range;
				int z = spawnPos.getZ() + random.nextInt(range * 2) - range;
				BlockPos targetPos = new BlockPos(x, 0, z);
				pending.get(star).add(targetPos);
				LogUtil.debug(Env.SERVER, "Dragon Ball (pending) [" + star + "] assigned to " + targetPos + " for set " + setId + " (Y is a dummy value)");
				if (level.isLoaded(targetPos)) generationQueue.add(() -> generateBallSafely(level, definition, star, targetPos));
			}
		}

		if (isFirstSpawn) {
			data.setFirstSpawnComplete(setId, true);
		}
		data.setDirty();
		syncRadar(level);
	}

	public static void unregisterConsumedDragonBalls(ServerLevel level, Collection<BlockPos> consumedPositions, String setId) {
		if (consumedPositions == null || consumedPositions.isEmpty()) return;
		DragonBallSavedData data = DragonBallSavedData.get(level);
		Map<Integer, List<BlockPos>> active = data.getActiveBalls(setId);
		Set<BlockPos> consumedSet = new HashSet<>(consumedPositions);
		boolean changed = false;
		for (int star : DragonBallDefinitions.getBallSet(setId).getStars()) {
			List<BlockPos> positions = active.get(star);
			if (positions != null && positions.removeIf(consumedSet::contains)) changed = true;
		}
		if (changed) {
			data.setDirty();
			syncRadar(level);
		}
	}

	@SubscribeEvent
	public static void onChunkLoad(ChunkEvent.Load event) {
		if (!(event.getLevel() instanceof ServerLevel level)) return;
		DragonBallSavedData data = DragonBallSavedData.get(level);
		ChunkPos chunkPos = event.getChunk().getPos();

		for (DragonBallSetDefinition definition : DragonBallDefinitions.getBallSetsForDimension(level.dimension())) {
			Map<Integer, List<BlockPos>> pending = data.getPendingBalls(definition.getId());
			pending.forEach((star, targets) -> {
				for (BlockPos target : new ArrayList<>(targets)) {
					if (chunkPos.x == (target.getX() >> 4) && chunkPos.z == (target.getZ() >> 4)) {
						generationQueue.add(() -> generateBallSafely(level, definition, star, target));
					}
				}
			});
		}
	}

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
		if (event.level instanceof ServerLevel level && ++pendingRescanTimer >= PENDING_RESCAN_INTERVAL) {
			pendingRescanTimer = 0;
			rescanPendingBalls(level);
		}
		if (event.level instanceof ServerLevel level && level.dimension().equals(Level.OVERWORLD)
				&& ++radarSyncTimer >= RADAR_SYNC_INTERVAL) {
			radarSyncTimer = 0;
			syncRadar(level);
		}
		if (event.level instanceof ServerLevel level) {
			generateNearbyPendingBalls(level);
		}
		while (!generationQueue.isEmpty()) {
			Runnable task = generationQueue.poll();
			if (task != null) task.run();
		}
	}

	private static void generateNearbyPendingBalls(ServerLevel level) {
		List<ServerPlayer> players = level.players();
		if (players.isEmpty()) return;
		DragonBallSavedData data = DragonBallSavedData.get(level);
		for (DragonBallSetDefinition definition : DragonBallDefinitions.getBallSetsForDimension(level.dimension())) {
			Map<Integer, List<BlockPos>> pending = data.getPendingBalls(definition.getId());
			pending.forEach((star, targets) -> {
				for (BlockPos target : new ArrayList<>(targets)) {
					if (!level.isLoaded(target)) continue;
					for (ServerPlayer player : players) {
						double dx = player.getX() - (target.getX() + 0.5);
						double dz = player.getZ() - (target.getZ() + 0.5);
						if (dx * dx + dz * dz <= NEARBY_GEN_RANGE_SQR) {
							generationQueue.add(() -> generateBallSafely(level, definition, star, target));
							break;
						}
					}
				}
			});
		}
	}

	private static void rescanPendingBalls(ServerLevel level) {
		DragonBallSavedData data = DragonBallSavedData.get(level);
		for (DragonBallSetDefinition definition : DragonBallDefinitions.getBallSetsForDimension(level.dimension())) {
			Map<Integer, List<BlockPos>> pending = data.getPendingBalls(definition.getId());
			pending.forEach((star, targets) -> {
				for (BlockPos target : new ArrayList<>(targets)) {
					if (level.isLoaded(target)) {
						generationQueue.add(() -> generateBallSafely(level, definition, star, target));
					}
				}
			});
		}
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) syncRadarForPlayer(player);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) syncRadarForPlayer(player);
	}

	private static void generateBallSafely(ServerLevel level, DragonBallSetDefinition definition, int star, BlockPos targetXZ) {
		int x = targetXZ.getX();
		int z = targetXZ.getZ();
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos realPos = new BlockPos(x, y, z);

		if (!level.isLoaded(realPos)) return;

		BlockPos.MutableBlockPos mutable = realPos.mutable();
		while (mutable.getY() > level.getMinBuildHeight() && level.getBlockState(mutable.below()).canBeReplaced()) {
			mutable.move(0, -1, 0);
		}
		realPos = mutable.immutable();

		while (!level.getBlockState(realPos).canBeReplaced() && realPos.getY() < level.getMaxBuildHeight() - 1) {
			realPos = realPos.above();
		}

		BlockState below = level.getBlockState(realPos.below());
		if (below.isAir() || below.is(Blocks.WATER)) {
			level.setBlock(realPos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
		}

		Block block = definition.getBlockForStar(star);
		if (block == null) return;

		boolean success = level.setBlock(realPos, block.defaultBlockState(), 2);

		if (!success || level.getBlockState(realPos).getBlock() != block) return;

		DragonBallSavedData data = DragonBallSavedData.get(level);
		data.getPendingBalls(definition.getId()).get(star).remove(targetXZ);

		if (!data.getActiveBalls(definition.getId()).get(star).contains(realPos)) data.getActiveBalls(definition.getId()).get(star).add(realPos);

		data.setDirty();
		LogUtil.info(Env.SERVER, "Dragon Ball [" + star + "] physically generated at " + realPos + " for set " + definition.getId());
		syncRadar(level);
	}

	@SubscribeEvent
	public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
		Block block = event.getPlacedBlock().getBlock();
		DragonBallSetDefinition definition = DragonBallDefinitions.getBallSetForBlock(block);
		if (definition == null || !(event.getLevel() instanceof ServerLevel level)) return;
		Integer star = definition.getStarForBlock(block);
		if (star == null) return;
		DragonBallSavedData data = DragonBallSavedData.get(level);
		if (!data.getActiveBalls(definition.getId()).get(star).contains(event.getPos())) data.getActiveBalls(definition.getId()).get(star).add(event.getPos());
		data.setDirty();
		syncRadar(level);
	}

	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		Block block = event.getState().getBlock();
		DragonBallSetDefinition definition = DragonBallDefinitions.getBallSetForBlock(block);
		if (definition == null || !(event.getLevel() instanceof ServerLevel level)) return;
		Integer star = definition.getStarForBlock(block);
		if (star == null) return;
		DragonBallSavedData data = DragonBallSavedData.get(level);
		data.getActiveBalls(definition.getId()).get(star).remove(event.getPos());
		data.setDirty();
		syncRadar(level);
	}

	public static void syncRadar(ServerLevel level) {
		if (level == null) return;
		Map<String, List<BlockPos>> positionsBySet = new HashMap<>();
		for (DragonBallSetDefinition definition : DragonBallDefinitions.getBallSets()) {
			ServerLevel setLevel = level.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, definition.getValidDimensions().iterator().next()));
			if (setLevel != null) {
				DragonBallSavedData data = DragonBallSavedData.get(setLevel);
				positionsBySet.put(definition.getId(), new ArrayList<>(data.getAllKnownPositionsForRadar(definition.getId())));
			}
		}
		List<BlockPos> earthPositions = new ArrayList<>(positionsBySet.getOrDefault("earth", List.of()));
		List<BlockPos> namekPositions = new ArrayList<>(positionsBySet.getOrDefault("namek", List.of()));
		NetworkHandler.sendToAllPlayers(new RadarSyncS2C(earthPositions, namekPositions, positionsBySet));
	}

	public static void syncRadarForPlayer(ServerPlayer player) {
		if (player == null) return;
		Map<String, List<BlockPos>> positionsBySet = new HashMap<>();
		for (DragonBallSetDefinition definition : DragonBallDefinitions.getBallSets()) {
			ServerLevel setLevel = player.serverLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, definition.getValidDimensions().iterator().next()));
			if (setLevel != null) {
				DragonBallSavedData data = DragonBallSavedData.get(setLevel);
				positionsBySet.put(definition.getId(), new ArrayList<>(data.getAllKnownPositionsForRadar(definition.getId())));
			}
		}
		List<BlockPos> earthPositions = new ArrayList<>(positionsBySet.getOrDefault("earth", List.of()));
		List<BlockPos> namekPositions = new ArrayList<>(positionsBySet.getOrDefault("namek", List.of()));
		NetworkHandler.sendToPlayer(new RadarSyncS2C(earthPositions, namekPositions, positionsBySet), player);
	}
}
