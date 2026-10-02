package com.dragonminez.server.world.worldboss;

import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.worldboss.AllWorldBossesEntity;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import com.dragonminez.server.world.data.StructurePlanSavedData;
import com.dragonminez.server.world.dimension.NamekDimension;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import com.dragonminez.server.world.structure.BossStructures.GeteStarShape;
import com.dragonminez.server.world.structure.BossStructures.GeteStarStructure;
import com.dragonminez.server.world.structure.helper.DMZStructureSets;
import com.dragonminez.server.world.structure.placement.StructureSpawnPlanner;
import com.dragonminez.server.world.structure.BossStructures.TreeOfMightShape;
import com.dragonminez.server.world.structure.BossStructures.TreeOfMightShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

public interface WorldBossLair {
	String key();

	String displayName();

	ResourceKey<Level> dimension();

	boolean isReady(ServerLevel level);

	BlockPos pickColumn(ServerLevel level);

	boolean isAreaLoaded(ServerLevel level, BlockPos column);

	BlockPos resolveGround(ServerLevel level, BlockPos column);

	void buildArena(ServerLevel level, BlockPos ground);

	WorldBossEntity createBoss(ServerLevel level);

	default long respawnTicks() {
		return 72000L;
	}

	default BlockPos teleportTarget(ServerLevel level, BlockPos lair) {
		return lair;
	}

	final class Janemba implements WorldBossLair {
		private static final int LAIR_DISTANCE = 1000;
		private static final long LAIR_SALT = 0x4A414E454D4241L;
		private static final int HELL_SCAN_TOP = 92;
		private static final int HELL_SCAN_BOTTOM = -50;

		private static final int FLOOR_RADIUS = 16;
		private static final int RIM_RADIUS = 22;
		private static final int HEADROOM = 22;
		private static final int FOUNDATION_DEPTH = 4;
		private static final int RIM_HEIGHT = 7;
		private static final int RING_SPIKES = 18;
		private static final int INNER_SPIKES = 9;

		@Override
		public String key() {
			return WorldBossEntity.JANEMBA;
		}

		@Override
		public String displayName() {
			return "Janemba";
		}

		@Override
		public ResourceKey<Level> dimension() {
			return OtherworldDimension.OTHERWORLD_KEY;
		}

		@Override
		public boolean isReady(ServerLevel level) {
			return true;
		}

		@Override
		public BlockPos pickColumn(ServerLevel level) {
			RandomSource random = RandomSource.create(level.getSeed() ^ LAIR_SALT);
			double angle = random.nextDouble() * Math.PI * 2.0D;
			int x = Mth.floor(Math.cos(angle) * LAIR_DISTANCE);
			int z = Mth.floor(Math.sin(angle) * LAIR_DISTANCE);
			return new BlockPos(x, HELL_SCAN_TOP, z);
		}

		@Override
		public boolean isAreaLoaded(ServerLevel level, BlockPos column) {
			for (int dx = -RIM_RADIUS; dx <= RIM_RADIUS; dx += 16) {
				for (int dz = -RIM_RADIUS; dz <= RIM_RADIUS; dz += 16) {
					if (!level.isLoaded(column.offset(dx, 0, dz))) return false;
				}
			}
			return level.isLoaded(column);
		}

		@Override
		public BlockPos resolveGround(ServerLevel level, BlockPos column) {
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			for (int y = HELL_SCAN_TOP; y >= HELL_SCAN_BOTTOM; y--) {
				cursor.set(column.getX(), y, column.getZ());
				if (!level.getBlockState(cursor).isAir()) return new BlockPos(column.getX(), y, column.getZ());
			}
			return null;
		}

		@Override
		public void buildArena(ServerLevel level, BlockPos center) {
			RandomSource random = RandomSource.create(center.asLong());

			BlockState floor = MainBlocks.HELL_STONE.get().defaultBlockState();
			BlockState ground = MainBlocks.HELL_GROUND.get().defaultBlockState();
			BlockState deep = MainBlocks.HELL_DEEPSTONE.get().defaultBlockState();

			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			int baseY = center.getY();

			for (int dx = -RIM_RADIUS; dx <= RIM_RADIUS; dx++) {
				for (int dz = -RIM_RADIUS; dz <= RIM_RADIUS; dz++) {
					double dist = Math.sqrt(dx * dx + dz * dz);
					if (dist > RIM_RADIUS) continue;

					int x = center.getX() + dx;
					int z = center.getZ() + dz;

					if (dist <= FLOOR_RADIUS) {
						BlockState surface = random.nextInt(4) == 0 ? ground : floor;
						cursor.set(x, baseY, z);
						level.setBlock(cursor, surface, 2);

						for (int depth = 1; depth <= FOUNDATION_DEPTH; depth++) {
							cursor.set(x, baseY - depth, z);
							if (level.getBlockState(cursor).isAir()) level.setBlock(cursor, deep, 2);
						}

						for (int up = 1; up <= HEADROOM; up++) {
							cursor.set(x, baseY + up, z);
							if (!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
						}
					} else {
						double ramp = (dist - FLOOR_RADIUS) / (double) (RIM_RADIUS - FLOOR_RADIUS);
						int wallTop = baseY + Mth.ceil(ramp * RIM_HEIGHT);

						for (int y = baseY - FOUNDATION_DEPTH; y <= wallTop; y++) {
							cursor.set(x, y, z);
							BlockState existing = level.getBlockState(cursor);
							if (existing.isAir() || y >= baseY) {
								level.setBlock(cursor, y >= wallTop - 1 ? floor : deep, 2);
							}
						}

						for (int up = wallTop + 1; up <= baseY + HEADROOM; up++) {
							cursor.set(x, up, z);
							if (!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
			}

			for (int i = 0; i < RING_SPIKES; i++) {
				double angle = (Math.PI * 2.0D / RING_SPIKES) * i + random.nextDouble() * 0.15D;
				int radius = RIM_RADIUS - 1 - random.nextInt(2);
				int x = center.getX() + Mth.floor(Math.cos(angle) * radius);
				int z = center.getZ() + Mth.floor(Math.sin(angle) * radius);
				int top = surfaceAt(level, x, z, baseY + RIM_HEIGHT + 2, baseY - FOUNDATION_DEPTH);
				if (top != Integer.MIN_VALUE) spike(level, x, top + 1, z, 5 + random.nextInt(6), random);
			}

			for (int i = 0; i < INNER_SPIKES; i++) {
				double angle = random.nextDouble() * Math.PI * 2.0D;
				int radius = 4 + random.nextInt(FLOOR_RADIUS - 5);
				int x = center.getX() + Mth.floor(Math.cos(angle) * radius);
				int z = center.getZ() + Mth.floor(Math.sin(angle) * radius);
				if (Math.abs(x - center.getX()) < 4 && Math.abs(z - center.getZ()) < 4) continue;
				spike(level, x, baseY + 1, z, 3 + random.nextInt(4), random);
			}
		}

		@Override
		public WorldBossEntity createBoss(ServerLevel level) {
			return MainEntities.WORLDBOSS_JANEMBA_FAT.get().create(level);
		}

		private static int surfaceAt(ServerLevel level, int x, int z, int from, int to) {
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			for (int y = from; y >= to; y--) {
				cursor.set(x, y, z);
				if (!level.getBlockState(cursor).isAir()) return y;
			}
			return Integer.MIN_VALUE;
		}

		private static void spike(ServerLevel level, int x, int baseY, int z, int height, RandomSource random) {
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			BlockState bone = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);

			for (int i = 0; i < height; i++) {
				int width = i < height / 3 ? 1 : 0;
				for (int dx = -width; dx <= width; dx++) {
					for (int dz = -width; dz <= width; dz++) {
						if (width == 1 && Math.abs(dx) + Math.abs(dz) == 2) continue;
						cursor.set(x + dx, baseY + i, z + dz);
						if (level.getBlockState(cursor).isAir()) level.setBlock(cursor, bone, 2);
					}
				}
				if (i > height / 2 && random.nextInt(4) == 0) break;
			}
		}
	}

	final class Turles implements WorldBossLair {
		private static final int AREA_CHECK_RADIUS = TreeOfMightShape.ARENA_RADIUS + 8;
		private static final int GROUND_SCAN = 24;

		@Override
		public String key() {
			return WorldBossEntity.TURLES;
		}

		@Override
		public String displayName() {
			return "Turles";
		}

		@Override
		public ResourceKey<Level> dimension() {
			return Level.OVERWORLD;
		}

		@Override
		public boolean isReady(ServerLevel level) {
			return StructurePlanSavedData.get(level).isBuilt(DMZStructureSets.TREE_OF_MIGHT_SALT);
		}

		@Override
		public BlockPos pickColumn(ServerLevel level) {
			ChunkPos origin = StructureSpawnPlanner.publishedPositions(level).get(DMZStructureSets.TREE_OF_MIGHT_SALT);
			if (origin == null) return null;
			TreeOfMightShape tree = TreeOfMightShapes.planned(level, level.getChunkSource().getGenerator(), origin);
			return new BlockPos(tree.arenaX(), tree.arenaFloor(), tree.arenaZ());
		}

		@Override
		public boolean isAreaLoaded(ServerLevel level, BlockPos column) {
			int minX = (column.getX() - AREA_CHECK_RADIUS) >> 4;
			int maxX = (column.getX() + AREA_CHECK_RADIUS) >> 4;
			int minZ = (column.getZ() - AREA_CHECK_RADIUS) >> 4;
			int maxZ = (column.getZ() + AREA_CHECK_RADIUS) >> 4;
			for (int cx = minX; cx <= maxX; cx++) {
				for (int cz = minZ; cz <= maxZ; cz++) {
					if (!level.hasChunk(cx, cz)) return false;
				}
			}
			return true;
		}

		@Override
		public BlockPos resolveGround(ServerLevel level, BlockPos column) {
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			for (int y = column.getY() + GROUND_SCAN; y >= column.getY() - GROUND_SCAN; y--) {
				cursor.set(column.getX(), y, column.getZ());
				BlockState state = level.getBlockState(cursor);
				if (!state.isAir() && !state.canBeReplaced()) return cursor.immutable();
			}
			return column;
		}

		@Override
		public void buildArena(ServerLevel level, BlockPos ground) {
		}

		@Override
		public WorldBossEntity createBoss(ServerLevel level) {
			return MainEntities.WORLDBOSS_TURLES.get().create(level);
		}
	}

	final class GeteStar implements WorldBossLair {
		private static final int AREA_CHECK_RADIUS = GeteStarShape.RING_OUTER;

		@Override
		public String key() {
			return WorldBossEntity.METAL_COOLER_CORE;
		}

		@Override
		public String displayName() {
			return "Metal Cooler Core";
		}

		@Override
		public ResourceKey<Level> dimension() {
			return NamekDimension.NAMEK_KEY;
		}

		@Override
		public boolean isReady(ServerLevel level) {
			return StructureSpawnPlanner.publishedPositions(level).containsKey(DMZStructureSets.GETE_STAR_SALT);
		}

		@Override
		public BlockPos teleportTarget(ServerLevel level, BlockPos lair) {
			GeteStarShape shape = shape(level);
			return shape == null ? lair : shape.entrancePosition();
		}

		private static GeteStarShape shape(ServerLevel level) {
			ChunkPos origin = StructureSpawnPlanner.publishedPositions(level).get(DMZStructureSets.GETE_STAR_SALT);
			if (origin == null) return null;
			return GeteStarShape.planned(level, level.getChunkSource().getGenerator(), origin);
		}

		@Override
		public BlockPos pickColumn(ServerLevel level) {
			GeteStarShape shape = shape(level);
			return shape == null ? null : shape.corePosition();
		}

		@Override
		public boolean isAreaLoaded(ServerLevel level, BlockPos column) {
			int minX = (column.getX() - AREA_CHECK_RADIUS) >> 4;
			int maxX = (column.getX() + AREA_CHECK_RADIUS) >> 4;
			int minZ = (column.getZ() - AREA_CHECK_RADIUS) >> 4;
			int maxZ = (column.getZ() + AREA_CHECK_RADIUS) >> 4;
			for (int cx = minX; cx <= maxX; cx++) {
				for (int cz = minZ; cz <= maxZ; cz++) {
					if (!level.hasChunk(cx, cz)) return false;
				}
			}
			return true;
		}

		@Override
		public BlockPos resolveGround(ServerLevel level, BlockPos column) {
			return GeteStarStructure.isStarBlock(level.getBlockState(column)) ? column : null;
		}

		@Override
		public void buildArena(ServerLevel level, BlockPos ground) {
		}

		@Override
		public WorldBossEntity createBoss(ServerLevel level) {
			AllWorldBossesEntity.MetalCoolerCore core = MainEntities.WORLDBOSS_METAL_COOLER_CORE.get().create(level);
			GeteStarShape shape = shape(level);
			if (core != null && shape != null) {
				core.setChamber(shape.chamberCenter(), shape.capsulePositions());
			}
			return core;
		}
	}
}
