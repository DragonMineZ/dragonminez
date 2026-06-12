package com.dragonminez.server.world.structure.placement;

import com.dragonminez.common.config.ConfigManager;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class StructureSpawnPlanner {
	private static final int ANGLES_PER_RING = 16;
	private static final TreeMap<Integer, BiomeAwareUniquePlacement> REGISTERED = new TreeMap<>();

	private static long cachedSeed = Long.MIN_VALUE;
	private static BiomeSource cachedBiomeSource = null;
	private static Map<Integer, ChunkPos> cachedPlan = Collections.emptyMap();

	private StructureSpawnPlanner() {}

	static synchronized void register(BiomeAwareUniquePlacement placement) {
		REGISTERED.put(placement.placementSalt(), placement);
		cachedSeed = Long.MIN_VALUE;
		cachedBiomeSource = null;
	}

	static synchronized ChunkPos getPositionFor(BiomeAwareUniquePlacement placement, long worldSeed, BiomeSource biomeSource, RandomState randomState) {
		if (biomeSource == null || randomState == null) return null;

		if (worldSeed != cachedSeed || biomeSource != cachedBiomeSource) {
			cachedPlan = computePlan(worldSeed, biomeSource, randomState);
			cachedSeed = worldSeed;
			cachedBiomeSource = biomeSource;
		}
		return cachedPlan.get(placement.placementSalt());
	}

	private static Map<Integer, ChunkPos> computePlan(long worldSeed, BiomeSource biomeSource, RandomState randomState) {
		GeneralServerConfigWorldGen cfg = new GeneralServerConfigWorldGen();

		double minChunks = cfg.minDistanceFromSpawn / 16.0;
		double maxChunks = Math.max(minChunks + 1.0, cfg.maxDistanceFromSpawn / 16.0);
		double spacingChunks = cfg.minDistanceBetween / 16.0;
		double spacingSqr = spacingChunks * spacingChunks;

		Map<Integer, ChunkPos> plan = new HashMap<>();
		List<ChunkPos> accepted = new ArrayList<>();

		int minRing = (int) Math.floor(minChunks);
		int maxRing = (int) Math.ceil(maxChunks);

		for (Map.Entry<Integer, BiomeAwareUniquePlacement> entry : REGISTERED.entrySet()) {
			BiomeAwareUniquePlacement placement = entry.getValue();
			ChunkPos found = searchNearest(placement, worldSeed, biomeSource, randomState,
					minRing, maxRing, accepted, spacingSqr);

			if (found != null) {
				plan.put(placement.placementSalt(), found);
				accepted.add(found);
			}
		}
		return plan;
	}

	private static ChunkPos searchNearest(BiomeAwareUniquePlacement placement, long worldSeed, BiomeSource biomeSource, RandomState randomState, int minRing, int maxRing, List<ChunkPos> accepted, double spacingSqr) {
		for (int ring = minRing; ring <= maxRing; ring++) {
			WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(worldSeed + placement.placementSalt() + ring));
			double baseAngle = random.nextDouble() * Math.PI * 2.0;

			int angleCount = ring == 0 ? 1 : ANGLES_PER_RING;

			for (int a = 0; a < angleCount; a++) {
				double angle = baseAngle + a * (Math.PI * 2.0 / angleCount);
				int chunkX = (int) Math.round(Math.cos(angle) * ring);
				int chunkZ = (int) Math.round(Math.sin(angle) * ring);

				if (tooClose(accepted, chunkX, chunkZ, spacingSqr)) continue;

				int quartX = QuartPos.fromBlock(chunkX * 16 + 8);
				int quartY = QuartPos.fromBlock(64);
				int quartZ = QuartPos.fromBlock(chunkZ * 16 + 8);
				Holder<Biome> biome = biomeSource.getNoiseBiome(quartX, quartY, quartZ, randomState.sampler());

				if (placement.getValidBiomes().contains(biome)) {
					return new ChunkPos(chunkX, chunkZ);
				}
			}
		}
		return null;
	}

	private static boolean tooClose(List<ChunkPos> accepted, int chunkX, int chunkZ, double spacingSqr) {
		if (spacingSqr <= 0) {
			return false;
		}
		for (ChunkPos other : accepted) {
			double dx = other.x - chunkX;
			double dz = other.z - chunkZ;
			if (dx * dx + dz * dz < spacingSqr) return true;
		}
		return false;
	}

	private static final class GeneralServerConfigWorldGen {
		final int minDistanceFromSpawn;
		final int maxDistanceFromSpawn;
		final int minDistanceBetween;

		GeneralServerConfigWorldGen() {
			var worldGen = ConfigManager.getServerConfig().getWorldGen();
			this.minDistanceFromSpawn = worldGen.getStructureMinDistanceFromSpawn();
			this.maxDistanceFromSpawn = worldGen.getStructureMaxDistanceFromSpawn();
			this.minDistanceBetween = worldGen.getStructureMinDistanceBetween();
		}
	}
}
