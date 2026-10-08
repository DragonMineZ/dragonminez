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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StructureLocator {

	private static final int MAX_HINT_BIOMES = 8;
	private static final Map<String, BlockPos> PREDICTED_CENTERS = new ConcurrentHashMap<>();

	@Nullable
	public static BlockPos locateStructure(ServerLevel level, ResourceKey<Structure> structureKey, BlockPos searchFrom) {
		var structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
		var structureSetRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);

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

		for (StructurePlacement placement : placements) {
			BlockPos pos = getPositionFromPlacement(level, structureKey, structureRegistry, placement);
			if (pos == null) continue;
			double dist = searchFrom.distSqr(pos);
			if (best == null || dist < bestDist) {
				best = pos;
				bestDist = dist;
			}
		}

		if (best != null) {
			return best;
		}

		HolderSet<Structure> holderSet = HolderSet.direct(structureRegistry.getHolderOrThrow(structureKey));
		Pair<BlockPos, Holder<Structure>> searchResult = level.getChunkSource().getGenerator()
				.findNearestMapStructure(level, holderSet, searchFrom, 100, false);
		if (searchResult != null) {
			best = searchResult.getFirst();
		}

		return best;
	}

	@Nullable
	private static BlockPos getPositionFromPlacement(ServerLevel level, ResourceKey<Structure> structureKey,
													 Registry<Structure> structureRegistry,
													 StructurePlacement placement) {
		ChunkPos chunkPos = null;
		int fallbackY = 90;
		if (placement instanceof BiomeAwareUniquePlacement uniquePlacement) {
			chunkPos = uniquePlacement.getStructureChunk(
					level.getSeed(),
					level.getChunkSource().getGenerator().getBiomeSource(),
					level.getChunkSource().randomState(),
					level.getChunkSource().getGeneratorState()
			);
		} else if (placement instanceof FixedStructurePlacement fixedPlacement) {
			chunkPos = new ChunkPos(fixedPlacement.getFixedX(), fixedPlacement.getFixedZ());
			fallbackY = 30;
		} else if (placement instanceof UniqueNearSpawnPlacement spawnPlacement) {
			chunkPos = spawnPlacement.getStructureChunk(level.getSeed());
		}
		if (chunkPos == null) return null;

		BlockPos center = structureCenter(level, structureRegistry.get(structureKey), chunkPos);
		return center != null ? center : new BlockPos(chunkPos.getMiddleBlockX(), fallbackY, chunkPos.getMiddleBlockZ());
	}

	@Nullable
	public static BlockPos structureCenter(ServerLevel level, @Nullable Structure structure, ChunkPos chunkPos) {
		if (structure == null) return null;
		LevelChunk loaded = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
		if (loaded != null) {
			StructureStart start = loaded.getStartForStructure(structure);
			if (start != null && start.isValid()) {
				BoundingBox box = StructureFrame.core(start).map(StructureFrame::box).orElseGet(start::getBoundingBox);
				return new BlockPos(box.getCenter().getX(), box.minY(), box.getCenter().getZ());
			}
		}

		String key = level.dimension().location() + "|" + level.getSeed() + "|" + chunkPos.toLong() + "|" + System.identityHashCode(structure);
		BlockPos cached = PREDICTED_CENTERS.get(key);
		if (cached != null) return cached;
		try {
			ChunkGenerator generator = level.getChunkSource().getGenerator();
			Structure.GenerationContext context = new Structure.GenerationContext(level.registryAccess(), generator,
					generator.getBiomeSource(), level.getChunkSource().randomState(), level.getStructureManager(),
					level.getSeed(), chunkPos, level, biome -> true);
			BlockPos predicted = structure.findValidGenerationPoint(context).map(Structure.GenerationStub::position).orElse(null);
			if (predicted != null) PREDICTED_CENTERS.put(key, predicted);
			return predicted;
		} catch (RuntimeException e) {
			return null;
		}
	}

	public static int getDistanceTo(BlockPos from, BlockPos to) {
		return (int) Math.sqrt(from.distSqr(to));
	}

	public record SpawnHint(Component dimensions, Component biomes) {
	}

	@Nullable
	public static SpawnHint spawnHint(MinecraftServer server, ResourceKey<Structure> structureKey) {
		var structureRegistry = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
		var structureSetRegistry = server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
		Holder<Structure> holder = structureRegistry.getHolder(structureKey).orElse(null);
		if (holder == null) return null;

		Set<Holder<Biome>> candidates = new LinkedHashSet<>();
		for (StructureSet set : structureSetRegistry) {
			if (set.structures().stream().noneMatch(entry -> entry.structure().is(structureKey))) continue;
			if (set.placement() instanceof BiomeAwareUniquePlacement unique) {
				unique.getValidBiomes().forEach(candidates::add);
			} else {
				holder.value().biomes().forEach(candidates::add);
			}
		}
		if (candidates.isEmpty()) holder.value().biomes().forEach(candidates::add);

		List<Component> dimensions = new ArrayList<>();
		Set<Holder<Biome>> biomes = new LinkedHashSet<>();
		for (ServerLevel level : server.getAllLevels()) {
			if (level.getChunkSource().getGeneratorState().getPlacementsForStructure(holder).isEmpty()) continue;
			Set<Holder<Biome>> possible = level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
			List<Holder<Biome>> matching = candidates.stream().filter(possible::contains).toList();
			if (matching.isEmpty()) continue;
			ResourceLocation dim = level.dimension().location();
			dimensions.add(Component.translatableWithFallback("dimension." + dim.getNamespace() + "." + dim.getPath(), dim.toString()));
			biomes.addAll(matching);
		}
		if (dimensions.isEmpty() || biomes.isEmpty()) return null;

		List<Component> biomeNames = new ArrayList<>();
		for (Holder<Biome> biome : biomes) {
			biome.unwrapKey().ifPresent(key -> biomeNames.add(Component.translatableWithFallback(
					"biome." + key.location().getNamespace() + "." + key.location().getPath(), key.location().toString())));
			if (biomeNames.size() >= MAX_HINT_BIOMES) break;
		}
		MutableComponent biomeList = ComponentUtils.formatList(biomeNames, Component.literal(", ")).copy();
		if (biomes.size() > biomeNames.size()) biomeList.append(Component.literal(", ..."));
		return new SpawnHint(ComponentUtils.formatList(dimensions, Component.literal(", ")), biomeList);
	}

	public static boolean usesCustomPlacement(ServerLevel level, ResourceKey<Structure> structureKey) {
		var structureSetRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
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
