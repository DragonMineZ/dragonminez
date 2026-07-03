package com.dragonminez.server.world.structure.helper;

import com.dragonminez.server.world.structure.placement.BiomeAwareUniquePlacement;
import com.dragonminez.server.world.structure.placement.FixedStructurePlacement;
import com.dragonminez.server.world.structure.placement.UniqueNearSpawnPlacement;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class StructureLocator {

	@Nullable
	public static BlockPos locateStructure(ServerLevel level, ResourceKey<Structure> structureKey, BlockPos searchFrom) {
		var structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
		var structureSetRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);

		// A structure can belong to several sets (e.g. a near-spawn BiomeAware set plus a far
		// RandomSpread set), so collect every placement and return whichever copy is closest.
		List<StructurePlacement> placements = new ArrayList<>();
		for (var entry : structureSetRegistry.entrySet()) {
			StructureSet set = entry.getValue();
			for (var structureEntry : set.structures()) {
				if (structureEntry.structure().is(structureKey)) {
					placements.add(set.placement());
					break;
				}
			}
		}
		if (placements.isEmpty()) {
			return null;
		}

		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;

		// Vanilla search resolves RandomSpread copies (including WideRandomSpread).
		HolderSet<Structure> holderSet = HolderSet.direct(structureRegistry.getHolderOrThrow(structureKey));
		Pair<BlockPos, Holder<Structure>> searchResult = level.getChunkSource().getGenerator()
				.findNearestMapStructure(level, holderSet, searchFrom, 100, false);
		if (searchResult != null) {
			best = searchResult.getFirst();
			bestDist = searchFrom.distSqr(best);
		}

		// Custom placements (near-spawn biome search, fixed, unique-near-spawn) are invisible to the
		// vanilla search, so probe them directly and keep the nearest overall.
		for (StructurePlacement placement : placements) {
			BlockPos pos = getPositionFromPlacement(level, structureKey, structureRegistry, placement);
			if (pos == null) continue;
			double dist = searchFrom.distSqr(pos);
			if (best == null || dist < bestDist) {
				best = pos;
				bestDist = dist;
			}
		}

		return best;
	}

	@Nullable
	private static BlockPos getPositionFromPlacement(ServerLevel level, ResourceKey<Structure> structureKey,
													 Registry<Structure> structureRegistry,
													 StructurePlacement placement) {
		if (placement instanceof BiomeAwareUniquePlacement uniquePlacement) {
			ChunkPos chunkPos = uniquePlacement.getStructureChunk(
					level.getSeed(),
					level.getChunkSource().getGenerator().getBiomeSource(),
					level.getChunkSource().randomState(),
					level.getChunkSource().getGeneratorState()
			);
			if (chunkPos != null) {
				return new BlockPos(chunkPos.getMiddleBlockX(), 90, chunkPos.getMiddleBlockZ());
			}
		} else if (placement instanceof FixedStructurePlacement fixedPlacement) {
			int x = (fixedPlacement.getFixedX() << 4) + 8;
			int z = (fixedPlacement.getFixedZ() << 4) + 8;
			return new BlockPos(x, 30, z);
		} else if (placement instanceof UniqueNearSpawnPlacement spawnPlacement) {
			ChunkPos chunkPos = spawnPlacement.getStructureChunk(level.getSeed());
			return new BlockPos(chunkPos.getMiddleBlockX(), 90, chunkPos.getMiddleBlockZ());
		}

		return null;
	}

	public static int getDistanceTo(BlockPos from, BlockPos to) {
		return (int) Math.sqrt(from.distSqr(to));
	}

	public static boolean usesCustomPlacement(ServerLevel level, ResourceKey<Structure> structureKey) {
		var structureSetRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
		// True if ANY set for this structure uses a custom (non-vanilla-searchable) placement, so the
		// mod's locate path runs and considers the near-spawn copy alongside the far RandomSpread ones.
		for (var entry : structureSetRegistry.entrySet()) {
			StructureSet set = entry.getValue();
			for (var structureEntry : set.structures()) {
				if (structureEntry.structure().is(structureKey)) {
					StructurePlacement placement = set.placement();
					if (placement instanceof BiomeAwareUniquePlacement
							|| placement instanceof FixedStructurePlacement
							|| placement instanceof UniqueNearSpawnPlacement) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
