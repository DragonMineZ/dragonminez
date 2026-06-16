package com.dragonminez.server.world.structure.placement;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class StructureAsyncResolver {
	private static final int RING_STEP = 64;
	private static final int FORCE_LOAD_RADIUS = 1;

	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "DMZ-StructureResolver");
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		return thread;
	});

	private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();

	private StructureAsyncResolver() {}

	static void enqueue(long worldSeed, BiomeSource biomeSource, RandomState randomState, ChunkGenerator generator,
						LevelHeightAccessor heightAccessor, int maxRing, double spacingSqr, List<ChunkPos> accepted,
						List<BiomeAwareUniquePlacement> pending, Map<Integer, HolderSet<Biome>> structureBiomes,
						ChunkGeneratorStructureState state, List<Holder<StructureSet>> avoid) {
		String key = worldSeed + ":" + System.identityHashCode(biomeSource);
		if (!IN_FLIGHT.add(key)) return;

		EXECUTOR.submit(() -> {
			try {
				resolve(worldSeed, biomeSource, randomState, generator, heightAccessor, maxRing, spacingSqr,
						accepted, pending, structureBiomes, state, avoid);
			} catch (Throwable t) {
				System.err.println("[DMZ] StructureAsyncResolver failed: " + t.getMessage());
			} finally {
				IN_FLIGHT.remove(key);
			}
		});
	}

	private static void resolve(long worldSeed, BiomeSource biomeSource, RandomState randomState,
								ChunkGenerator generator, LevelHeightAccessor heightAccessor, int maxRing,
								double spacingSqr, List<ChunkPos> accepted, List<BiomeAwareUniquePlacement> pending,
								Map<Integer, HolderSet<Biome>> structureBiomes, ChunkGeneratorStructureState state,
								List<Holder<StructureSet>> avoid) {
		int absoluteCap = maxRing + 8000;

		for (BiomeAwareUniquePlacement placement : pending) {
			HolderSet<Biome> structBiomes = structureBiomes.get(placement.placementSalt());
			if (structBiomes == null) continue;

			ChunkPos found = null;
			int from = maxRing + 1;
			while (found == null && from <= absoluteCap) {
				int to = Math.min(from + RING_STEP - 1, absoluteCap);
				found = StructureSpawnPlanner.searchNearest(placement, structBiomes, biomeSource, randomState,
						generator, heightAccessor, from, to, accepted, spacingSqr, state, avoid);
				from = to + 1;
			}

			if (found == null) {
				System.err.println("[DMZ] StructureAsyncResolver: no valid placement found for salt "
						+ placement.placementSalt() + " within " + absoluteCap + " chunks of spawn.");
				continue;
			}

			accepted.add(found);
			StructureSpawnPlanner.injectResolved(worldSeed, biomeSource, placement.placementSalt(), found);
			forceGenerate(generator, found);
		}
	}

	private static void forceGenerate(ChunkGenerator generator, ChunkPos pos) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return;

		server.execute(() -> {
			ServerLevel target = null;
			for (ServerLevel level : server.getAllLevels()) {
				if (level.getChunkSource().getGenerator() == generator) {
					target = level;
					break;
				}
			}
			if (target == null) return;

			for (int dx = -FORCE_LOAD_RADIUS; dx <= FORCE_LOAD_RADIUS; dx++) {
				for (int dz = -FORCE_LOAD_RADIUS; dz <= FORCE_LOAD_RADIUS; dz++) {
					target.getChunk(pos.x + dx, pos.z + dz, ChunkStatus.FULL, true);
				}
			}
		});
	}
}
