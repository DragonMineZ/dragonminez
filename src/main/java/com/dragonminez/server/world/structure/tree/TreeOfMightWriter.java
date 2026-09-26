package com.dragonminez.server.world.structure.tree;

import com.dragonminez.common.init.MainBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public final class TreeOfMightWriter {
	private static final int LIGHT_SPACING = 6;
	private static final int LIGHT_HEIGHT = 6;
	private static final int LIGHT_LEVEL = 15;
	private static final int GROUND_SCAN_UP = 36;
	private static final int GROUND_SCAN_DOWN = 40;
	private static final int ARENA_FILL_DEPTH = 16;

	private TreeOfMightWriter() {}

	public static void place(WorldGenLevel level, TreeOfMightShape tree, ChunkPos chunkPos, BoundingBox box) {
		int x0 = chunkPos.getMinBlockX();
		int z0 = chunkPos.getMinBlockZ();
		int y0 = tree.minY();
		int sy = tree.maxY() - y0 + 1;
		Region region = new Region(Math.max(box.minX(), x0), Math.min(box.maxX(), x0 + 15),
				Math.max(box.minY(), y0), Math.min(box.maxY(), tree.maxY()),
				Math.max(box.minZ(), z0), Math.min(box.maxZ(), z0 + 15));
		if (region.isEmpty()) return;

		byte[] buffer = new byte[16 * sy * 16];
		tree.rasterize(x0, y0, z0, 16, sy, 16, buffer);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

		shapeArena(level, tree, buffer, x0, z0, y0, region, cursor);
		tree.applyRoots(x0, y0, z0, 16, sy, 16, buffer, (x, y, z) -> blocksRoot(level.getBlockState(cursor.set(x, y, z))));
		extendBase(level, tree, buffer, x0, z0, y0, sy, cursor);
		byte[] faces = new byte[buffer.length];
		tree.applyLichen(x0, y0, z0, 16, sy, 16, buffer, faces);
		placeLights(level, tree, buffer, x0, z0, y0, region, cursor);
		write(level, buffer, faces, x0, z0, y0, region, cursor);
	}

	public static boolean isTreeBlock(BlockState state) {
		return state.is(MainBlocks.MIGHT_TREE_WOOD.get()) || state.is(MainBlocks.MIGHT_TREE_ROOT.get())
				|| state.is(MainBlocks.MIGHT_TREE_LEAVES.get()) || state.is(MainBlocks.MIGHT_TREE_FRUIT_BLOCK.get());
	}

	private record Region(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		boolean isEmpty() {
			return minX > maxX || minY > maxY || minZ > maxZ;
		}

		boolean contains(int x, int y, int z) {
			return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
		}
	}

	private static boolean blocksRoot(BlockState state) {
		if (state.isAir() || isTreeBlock(state)) return false;
		return !state.canBeReplaced();
	}

	private static boolean isRemovable(WorldGenLevel level, BlockPos pos, BlockState state) {
		if (state.is(Blocks.BEDROCK) || state.hasBlockEntity()) return false;
		return state.getDestroySpeed(level, pos) >= 0.0F;
	}

	private static int index(int dx, int dy, int dz) {
		return TreeOfMightShape.index(16, 16, dx, dy, dz);
	}

	private static void shapeArena(WorldGenLevel level, TreeOfMightShape tree, byte[] buffer, int x0, int z0, int y0,
								   Region region, BlockPos.MutableBlockPos cursor) {
		int floor = tree.arenaFloor();
		double reach = TreeOfMightShape.ARENA_RADIUS + TreeOfMightShape.ARENA_BLEND;
		for (int z = region.minZ(); z <= region.maxZ(); z++) {
			for (int x = region.minX(); x <= region.maxX(); x++) {
				double distance = tree.arenaDistance(x, z);
				if (distance > reach) continue;
				int dx = x - x0;
				int dz = z - z0;
				if (distance <= TreeOfMightShape.ARENA_RADIUS) {
					for (int y = floor + 1; y <= floor + TreeOfMightShape.ARENA_HEADROOM; y++) {
						if (!region.contains(x, y, z) || buffer[index(dx, y - y0, dz)] != TreeOfMightShape.NONE) continue;
						BlockState state = level.getBlockState(cursor.set(x, y, z));
						if (state.isAir() || isTreeBlock(state) || !isRemovable(level, cursor, state)) continue;
						level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
					}
					fillColumn(level, buffer, x, z, dx, dz, y0, floor, true, region, cursor);
				} else {
					int target = floor - (int) Math.ceil((distance - TreeOfMightShape.ARENA_RADIUS) * 0.6D);
					fillColumn(level, buffer, x, z, dx, dz, y0, target, false, region, cursor);
				}
			}
		}
	}

	private static void fillColumn(WorldGenLevel level, byte[] buffer, int x, int z, int dx, int dz, int y0, int top,
								   boolean resurface, Region region, BlockPos.MutableBlockPos cursor) {
		for (int y = top, depth = 0; y >= y0 && depth <= ARENA_FILL_DEPTH; y--, depth++) {
			BlockState state = level.getBlockState(cursor.set(x, y, z));
			boolean solid = blocksRoot(state);
			if (solid && (y < top || !resurface)) return;
			if (!region.contains(x, y, z) || buffer[index(dx, y - y0, dz)] != TreeOfMightShape.NONE) continue;
			if (isTreeBlock(state) || !isRemovable(level, cursor, state)) continue;
			level.setBlock(cursor, y == top ? surfaceFor(x, z) : Blocks.DIRT.defaultBlockState(), 2);
		}
	}

	private static BlockState surfaceFor(int x, int z) {
		int patch = Math.floorMod(hash(x >> 2, z >> 2) + Math.floorMod(hash(x, z), 3), 10);
		if (patch < 3) return Blocks.ROOTED_DIRT.defaultBlockState();
		if (patch < 6) return Blocks.COARSE_DIRT.defaultBlockState();
		if (patch < 8) return Blocks.MUD.defaultBlockState();
		return Blocks.PODZOL.defaultBlockState();
	}

	private static int hash(int x, int z) {
		int h = x * 73428767 ^ z * 912931;
		h ^= h >>> 13;
		h *= 0x5BD1E995;
		return h ^ (h >>> 15);
	}

	private static void extendBase(WorldGenLevel level, TreeOfMightShape tree, byte[] buffer, int x0, int z0, int y0, int sy,
								   BlockPos.MutableBlockPos cursor) {
		for (int dz = 0; dz < 16; dz++) {
			for (int dx = 0; dx < 16; dx++) {
				int bottom = tree.baseBottom(buffer, 16, sy, 16, dx, dz, y0);
				if (bottom == Integer.MIN_VALUE) continue;
				for (int y = bottom - 1, depth = 0; y >= y0 && depth < TreeOfMightShape.MAX_BASE_EXTENSION; y--, depth++) {
					int index = index(dx, y - y0, dz);
					if (buffer[index] == TreeOfMightShape.WOOD) break;
					if (blocksRoot(level.getBlockState(cursor.set(x0 + dx, y, z0 + dz)))) break;
					buffer[index] = TreeOfMightShape.WOOD;
				}
			}
		}
	}

	private static void placeLights(WorldGenLevel level, TreeOfMightShape tree, byte[] buffer, int x0, int z0, int y0,
									Region region, BlockPos.MutableBlockPos cursor) {
		for (int z = region.minZ(); z <= region.maxZ(); z++) {
			if (Math.floorMod(z, LIGHT_SPACING) != 0) continue;
			for (int x = region.minX(); x <= region.maxX(); x++) {
				if (Math.floorMod(x, LIGHT_SPACING) != 0 || !tree.isUnderCanopy(x, z)) continue;
				int ground = findGround(level, tree, x, z, cursor);
				if (ground == Integer.MIN_VALUE) continue;
				int y = ground + LIGHT_HEIGHT;
				if (!region.contains(x, y, z) || buffer[index(x - x0, y - y0, z - z0)] != TreeOfMightShape.NONE) continue;
				BlockState current = level.getBlockState(cursor.set(x, y, z));
				boolean water = isWaterSource(current);
				if (!current.isAir() && !water) continue;
				level.setBlock(cursor, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LIGHT_LEVEL)
						.setValue(LightBlock.WATERLOGGED, water), 2);
			}
		}
	}

	private static boolean isWaterSource(BlockState state) {
		FluidState fluid = state.getFluidState();
		return fluid.is(Fluids.WATER) && fluid.isSource();
	}

	private static int findGround(WorldGenLevel level, TreeOfMightShape tree, int x, int z, BlockPos.MutableBlockPos cursor) {
		int top = tree.baseY() + GROUND_SCAN_UP;
		if (blocksRoot(level.getBlockState(cursor.set(x, top, z)))) return Integer.MIN_VALUE;
		for (int y = top - 1; y >= tree.baseY() - GROUND_SCAN_DOWN; y--) {
			if (blocksRoot(level.getBlockState(cursor.set(x, y, z)))) return y;
		}
		return Integer.MIN_VALUE;
	}

	private static BlockState lichenState(int mask, boolean water) {
		BlockState state = Blocks.GLOW_LICHEN.defaultBlockState();
		for (int face = 0; face < 6; face++) {
			if ((mask & (1 << face)) != 0) state = state.setValue(MultifaceBlock.getFaceProperty(Direction.from3DDataValue(face)), true);
		}
		return state.setValue(BlockStateProperties.WATERLOGGED, water);
	}

	private static void write(WorldGenLevel level, byte[] buffer, byte[] faces, int x0, int z0, int y0, Region region,
							  BlockPos.MutableBlockPos cursor) {
		BlockState wood = MainBlocks.MIGHT_TREE_WOOD.get().defaultBlockState();
		BlockState root = MainBlocks.MIGHT_TREE_ROOT.get().defaultBlockState();
		BlockState leaves = MainBlocks.MIGHT_TREE_LEAVES.get().defaultBlockState();
		BlockState fruit = MainBlocks.MIGHT_TREE_FRUIT_BLOCK.get().defaultBlockState();
		for (int y = region.minY(); y <= region.maxY(); y++) {
			for (int z = region.minZ(); z <= region.maxZ(); z++) {
				for (int x = region.minX(); x <= region.maxX(); x++) {
					int index = index(x - x0, y - y0, z - z0);
					byte code = buffer[index];
					if (code == TreeOfMightShape.NONE) continue;
					BlockState current = level.getBlockState(cursor.set(x, y, z));
					BlockState next;
					if (code == TreeOfMightShape.WOOD) {
						if (!isTreeBlock(current) && !isRemovable(level, cursor, current)) continue;
						next = wood;
					} else if (code == TreeOfMightShape.ROOT || code == TreeOfMightShape.FRUIT) {
						if (blocksRoot(current)) continue;
						next = code == TreeOfMightShape.ROOT ? root : fruit;
					} else if (code == TreeOfMightShape.LICHEN) {
						boolean water = isWaterSource(current);
						if (!current.isAir() && !water) continue;
						next = lichenState(faces[index], water);
					} else {
						if (!current.isAir() && !current.canBeReplaced() && !current.is(BlockTags.LEAVES) && !isTreeBlock(current)) continue;
						next = leaves;
					}
					if (current == next) continue;
					level.setBlock(cursor, next, 2);
				}
			}
		}
	}
}
