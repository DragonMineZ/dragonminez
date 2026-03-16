package com.dragonminez.server.events;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.block.custom.DragonBallBlock;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.RadarSyncS2C;
import com.dragonminez.server.world.data.DragonBallSavedData;
import com.dragonminez.server.world.dimension.NamekDimension;
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
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class DragonBallsHandler {
	private static final Queue<Runnable> generationQueue = new ConcurrentLinkedQueue<>();

	public static void scatterDragonBalls(ServerLevel level, boolean isNamek) {
		DragonBallSavedData data = DragonBallSavedData.get(level);
		Random random = new Random();
		int range = ConfigManager.getServerConfig().getWorldGen().getDBSpawnRange();
		BlockPos spawnPos = level.getSharedSpawnPos();

		Map<Integer, List<BlockPos>> active = data.getActiveBalls(isNamek);
		Map<Integer, List<BlockPos>> pending = data.getPendingBalls(isNamek);

		boolean isFirstSpawn = isNamek ? !data.isFirstSpawnNamek() : !data.isFirstSpawnEarth();

		int maxSets = ConfigManager.getServerConfig().getWorldGen().getDragonBallSets();
		int setsToSpawn = isFirstSpawn ? maxSets : 1;

		for (int star = 1; star <= 7; star++) {
			int currentCount = active.get(star).size() + pending.get(star).size();
			int actualToSpawn = Math.min(setsToSpawn, maxSets - currentCount);

			for (int i = 0; i < actualToSpawn; i++) {
				int x = spawnPos.getX() + random.nextInt(range * 2) - range;
				int z = spawnPos.getZ() + random.nextInt(range * 2) - range;

				BlockPos targetPos = new BlockPos(x, 0, z);

				pending.get(star).add(targetPos);
				LogUtil.debug(Env.SERVER, "Dragon Ball (pending) [" + star + "] assigned to " + targetPos + " on " + (isNamek ? "Namek" : "Earth") + " (Y is a dummy value)");
			}
		}

		if (isFirstSpawn) {
			if (isNamek) data.setFirstSpawnNamek(true);
			else data.setFirstSpawnEarth(true);
		}

		data.setDirty();
		syncRadar(level);
	}

	@SubscribeEvent
	public static void onChunkLoad(ChunkEvent.Load event) {
		if (!(event.getLevel() instanceof ServerLevel level)) return;

		boolean isNamek = level.dimension().location().getPath().contains("namek");
		if (!level.dimension().equals(Level.OVERWORLD) && !isNamek) return;

		DragonBallSavedData data = DragonBallSavedData.get(level);
		Map<Integer, List<BlockPos>> pending = data.getPendingBalls(isNamek);

		ChunkPos chunkPos = event.getChunk().getPos();

		pending.forEach((star, targets) -> {
			for (BlockPos target : new ArrayList<>(targets)) {
				if (chunkPos.x == (target.getX() >> 4) && chunkPos.z == (target.getZ() >> 4)) {
					generationQueue.add(() -> generateBallSafely(level, star, target, isNamek));
				}
			}
		});
	}

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;

		while (!generationQueue.isEmpty()) {
			Runnable task = generationQueue.poll();
			if (task != null) task.run();
		}
	}

	private static void generateBallSafely(ServerLevel level, int star, BlockPos targetXZ, boolean isNamek) {
		int x = targetXZ.getX();
		int z = targetXZ.getZ();

		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos realPos = new BlockPos(x, y, z);

		if (!level.isLoaded(realPos)) return;

		BlockPos.MutableBlockPos mutable = realPos.mutable();
		while (mutable.getY() > -60 && level.getBlockState(mutable.below()).isAir()) mutable.move(0, -1, 0);
		realPos = mutable.immutable();

		BlockState below = level.getBlockState(realPos.below());
		if (below.isAir() || below.is(Blocks.WATER) || below.is(MainBlocks.NAMEK_WATER_LIQUID.get())) {
			level.setBlock(realPos.below(), isNamek ? MainBlocks.NAMEK_GRASS_BLOCK.get().defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState(), 3);
		}

		BlockState ballState = getBallState(star, isNamek);
		if (ballState != null) {
			boolean success = level.setBlock(realPos, ballState, 3);

			if (success) {
				DragonBallSavedData data = DragonBallSavedData.get(level);

				data.getPendingBalls(isNamek).get(star).remove(targetXZ);
				data.getActiveBalls(isNamek).get(star).add(realPos);
				data.setDirty();

				LogUtil.info(Env.SERVER, "Dragon Ball [" + star + "] physically generated at " + realPos + " on " + (isNamek ? "Namek" : "Earth"));
				syncRadar(level);
			}
		}
	}

	@SubscribeEvent
	public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;

		Block block = event.getPlacedBlock().getBlock();
		int star = getStarFromBlock(block);
		if (star == -1) return;

		ServerLevel level = (ServerLevel) event.getLevel();
		boolean isNamek = isNamekBall(block);

		DragonBallSavedData data = DragonBallSavedData.get(level);

		data.getActiveBalls(isNamek).get(star).add(event.getPos());
		data.setDirty();
		syncRadar(level);
	}

	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		if (!(event.getPlayer() instanceof ServerPlayer player)) return;

		Block block = event.getState().getBlock();
		int star = getStarFromBlock(block);
		if (star == -1) return;

		ServerLevel level = (ServerLevel) event.getLevel();
		boolean isNamek = isNamekBall(block);

		DragonBallSavedData data = DragonBallSavedData.get(level);

		data.getActiveBalls(isNamek).get(star).remove(event.getPos());
		data.setDirty();
		syncRadar(level);
	}

	public static void syncRadar(ServerLevel level) {
		ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
		ServerLevel namek = level.getServer().getLevel(NamekDimension.NAMEK_KEY);

		List<BlockPos> earthList = new ArrayList<>();
		List<BlockPos> namekList = new ArrayList<>();

		if (overworld != null) {
			DragonBallSavedData data = DragonBallSavedData.get(overworld);
			earthList.addAll(data.getAllPositionsForRadar(false));
		}

		if (namek != null) {
			DragonBallSavedData data = DragonBallSavedData.get(namek);
			namekList.addAll(data.getAllPositionsForRadar(true));
		}

		NetworkHandler.sendToAllPlayers(new RadarSyncS2C(earthList, namekList));
	}

	private static BlockState getBallState(int star, boolean isNamek) {
		if (isNamek) {
			return switch (star) {
				case 1 -> MainBlocks.DRAGON_BALL_BLOCKS.get("namek_dball1").get().defaultBlockState();
				case 2 -> MainBlocks.DRAGON_BALL_BLOCKS.get("namek_dball2").get().defaultBlockState();
				case 3 -> MainBlocks.DRAGON_BALL_BLOCKS.get("namek_dball3").get().defaultBlockState();
				case 4 -> MainBlocks.DRAGON_BALL_BLOCKS.get("namek_dball4").get().defaultBlockState();
				case 5 -> MainBlocks.DRAGON_BALL_BLOCKS.get("namek_dball5").get().defaultBlockState();
				case 6 -> MainBlocks.DRAGON_BALL_BLOCKS.get("namek_dball6").get().defaultBlockState();
				case 7 -> MainBlocks.DRAGON_BALL_BLOCKS.get("namek_dball7").get().defaultBlockState();
				default -> null;
			};
		} else {
			return switch (star) {
				case 1 -> MainBlocks.DRAGON_BALL_BLOCKS.get("earth_dball1").get().defaultBlockState();
				case 2 -> MainBlocks.DRAGON_BALL_BLOCKS.get("earth_dball2").get().defaultBlockState();
				case 3 -> MainBlocks.DRAGON_BALL_BLOCKS.get("earth_dball3").get().defaultBlockState();
				case 4 -> MainBlocks.DRAGON_BALL_BLOCKS.get("earth_dball4").get().defaultBlockState();
				case 5 -> MainBlocks.DRAGON_BALL_BLOCKS.get("earth_dball5").get().defaultBlockState();
				case 6 -> MainBlocks.DRAGON_BALL_BLOCKS.get("earth_dball6").get().defaultBlockState();
				case 7 -> MainBlocks.DRAGON_BALL_BLOCKS.get("earth_dball7").get().defaultBlockState();
				default -> null;
			};
		}
	}

	private static int getStarFromBlock(Block block) {
		if (block instanceof DragonBallBlock) {
			return ((DragonBallBlock) block).getBallType().getStars();
		}
		return -1;
	}

	private static boolean isNamekBall(Block block) {
		return block.getDescriptionId().contains("namek");
	}
}