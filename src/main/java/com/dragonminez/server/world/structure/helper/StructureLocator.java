package com.dragonminez.server.world.structure.helper;

import com.dragonminez.server.world.structure.placement.BiomeAwareUniquePlacement;
import com.dragonminez.server.world.structure.placement.FixedStructurePlacement;
import com.dragonminez.server.world.structure.placement.UniqueNearSpawnPlacement;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import javax.annotation.Nullable;

public class StructureLocator {

	@Nullable
	public static BlockPos locateStructure(ServerLevel level, ResourceKey<Structure> structureKey, BlockPos searchFrom) {
		var structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
		var structureSetRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);

		StructurePlacement placement = null;

		for (var entry : structureSetRegistry.entrySet()) {
			StructureSet set = entry.getValue();
			for (var structureEntry : set.structures()) {
				if (structureEntry.structure().is(structureKey)) {
					placement = set.placement();
					break;
				}
			}
			if (placement != null) break;
		}

		if (placement == null) {
			return null;
		}

		BlockPos result = getPositionFromPlacement(level, structureKey, structureRegistry, placement);

		if (result == null) {
			HolderSet<Structure> holderSet = HolderSet.direct(structureRegistry.getHolderOrThrow(structureKey));
			Pair<BlockPos, Holder<Structure>> searchResult = level.getChunkSource().getGenerator()
					.findNearestMapStructure(level, holderSet, searchFrom, 100, false);
			if (searchResult != null) {
				result = searchResult.getFirst();
			}
		}

		return result;
	}

	@Nullable
	private static BlockPos getPositionFromPlacement(ServerLevel level, ResourceKey<Structure> structureKey,
													 net.minecraft.core.Registry<Structure> structureRegistry,
													 StructurePlacement placement) {
		if (placement instanceof BiomeAwareUniquePlacement uniquePlacement) {
			ChunkPos chunkPos = uniquePlacement.getStructureChunk(
					level.getSeed(),
					level.getChunkSource().getGenerator().getBiomeSource(),
					level.getChunkSource().randomState()
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
}
