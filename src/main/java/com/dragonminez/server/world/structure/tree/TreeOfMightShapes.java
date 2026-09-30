package com.dragonminez.server.world.structure.tree;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TreeOfMightShapes {
	private static final int CACHE_LIMIT = 8;
	private static final Map<Key, TreeOfMightShape> CACHE = new ConcurrentHashMap<>();

	private record Key(long seed, int centerX, int centerZ, int version) {}

	private TreeOfMightShapes() {}

	public static TreeOfMightShape cached(long seed, int centerX, int centerZ) {
		return CACHE.get(new Key(seed, centerX, centerZ, TreeOfMightShape.VERSION));
	}

	public static void remember(TreeOfMightShape shape) {
		if (CACHE.size() >= CACHE_LIMIT) CACHE.clear();
		CACHE.put(new Key(shape.seed(), shape.centerX(), shape.centerZ(), TreeOfMightShape.VERSION), shape);
	}

	public static long seedFor(long levelSeed, ChunkPos origin) {
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureSeed(levelSeed, origin.x, origin.z);
		return random.nextLong();
	}

	public static TreeOfMightShape planned(ServerLevel level, ChunkGenerator generator, ChunkPos origin) {
		long seed = seedFor(level.getSeed(), origin);
		int centerX = origin.getMiddleBlockX();
		int centerZ = origin.getMiddleBlockZ();
		TreeOfMightShape shape = cached(seed, centerX, centerZ);
		if (shape != null) return shape;
		RandomState randomState = level.getChunkSource().randomState();
		shape = TreeOfMightShape.create(centerX, centerZ, seed, level.getMinBuildHeight(), level.getMaxBuildHeight(),
				generator.getSeaLevel(), (x, z) -> generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState));
		remember(shape);
		return shape;
	}
}
