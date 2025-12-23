package com.dragonminez.server.world.structure.placement;

import com.dragonminez.common.config.ConfigManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.lang.reflect.Field;
import java.util.Optional;

public class BiomeAwareUniquePlacement extends StructurePlacement {

	public static final Codec<BiomeAwareUniquePlacement> CODEC = RecordCodecBuilder.create(instance ->
			placementCodec(instance).and(
					RegistryCodecs.homogeneousList(Registries.BIOME)
							.fieldOf("valid_biomes")
							.forGetter(BiomeAwareUniquePlacement::validBiomes)
			).apply(instance, BiomeAwareUniquePlacement::new));

	private final HolderSet<Biome> validBiomes;

	private static Field biomeSourceField = null;

	public BiomeAwareUniquePlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod,
									 float frequency, int salt, Optional<ExclusionZone> exclusionZone,
									 HolderSet<Biome> validBiomes) {
		super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
		this.validBiomes = validBiomes;
	}

	public HolderSet<Biome> validBiomes() {
		return this.validBiomes;
	}

	private BiomeSource getBiomeSourceReflection(ChunkGeneratorStructureState state) {
		try {
			if (biomeSourceField == null) {
				for (Field f : ChunkGeneratorStructureState.class.getDeclaredFields()) {
					if (BiomeSource.class.isAssignableFrom(f.getType())) {
						f.setAccessible(true);
						biomeSourceField = f;
						break;
					}
				}
			}
			if (biomeSourceField != null) {
				return (BiomeSource) biomeSourceField.get(state);
			}
		} catch (Exception e) {
			System.err.println("[DMZ Debug] Error trying to get BiomeSource: " + e.getMessage());
		}
		return null;
	}

	@Override
	protected boolean isPlacementChunk(ChunkGeneratorStructureState structureState, int x, int z) {
		if (!ConfigManager.getServerConfig().getWorldGen().isGenerateCustomStructures()) {
			return false;
		}

		ChunkPos pos = getStructureChunk(structureState.getLevelSeed(), getBiomeSourceReflection(structureState), structureState.randomState());

		return pos != null && pos.x == x && pos.z == z;
	}

	public ChunkPos getStructureChunk(long worldSeed, BiomeSource biomeSource, RandomState randomState) {
		if (biomeSource == null || randomState == null) return null;

		for (int attempt = 0; attempt < 100; attempt++) {
			WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(worldSeed + this.salt() + attempt));

			int targetChunkX = random.nextInt(400) - 200;
			int targetChunkZ = random.nextInt(400) - 200;

			int quartX = QuartPos.fromBlock(targetChunkX * 16 + 8);

			int quartY = QuartPos.fromBlock(64);

			int quartZ = QuartPos.fromBlock(targetChunkZ * 16 + 8);

			Holder<Biome> biome = biomeSource.getNoiseBiome(quartX, quartY, quartZ, randomState.sampler());

			if (this.validBiomes.contains(biome)) {
				return new ChunkPos(targetChunkX, targetChunkZ);
			}
		}
		return null;
	}

	@Override
	public StructurePlacementType<?> type() {
		return MainStructurePlacements.BIOME_AWARE_PLACEMENT.get();
	}
}