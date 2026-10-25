package com.dragonminez.server.world.structure.fitted;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@FunctionalInterface
public interface HeightSampler {
	long sample(int x, int z);

	static int floor(long packed) {
		return (int) (packed >> 32);
	}

	static int surface(long packed) {
		return (int) packed;
	}

	static long measure(ChunkGenerator generator, LevelHeightAccessor heights, RandomState random, int x, int z) {
		int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, random) - 1;
		int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, random) - 1;
		return ((long) floor << 32) | (surface & 0xFFFFFFFFL);
	}

	static HeightSampler direct(ChunkGenerator generator, LevelHeightAccessor heights, RandomState random) {
		Map<Long, Long> cache = new HashMap<>();
		return (x, z) -> cache.computeIfAbsent(ChunkPos.asLong(x, z), key -> measure(generator, heights, random, x, z));
	}

	static HeightSampler estimating(ChunkGenerator generator, LevelHeightAccessor heights, RandomState random,
									ConcurrentHashMap<Long, Long> cache) {
		int water = generator.getSeaLevel() - 1;
		return (x, z) -> cache.computeIfAbsent(ChunkPos.asLong(x, z), key -> {
			int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, random) - 1;
			return ((long) floor << 32) | (Math.max(floor, water) & 0xFFFFFFFFL);
		});
	}
}
