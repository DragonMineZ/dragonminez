package com.dragonminez.common.world.structure.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
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
					if (f.getType() == BiomeSource.class) {
						biomeSourceField = f;
						biomeSourceField.setAccessible(true);
						break;
					}
				}
			}
			if (biomeSourceField != null) {
				return (BiomeSource) biomeSourceField.get(state);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		throw new RuntimeException("No se pudo acceder al BiomeSource vía reflexión en BiomeAwareUniquePlacement");
	}

	@Override
	protected boolean isPlacementChunk(ChunkGeneratorStructureState structureState, int x, int z) {
		long worldSeed = structureState.getLevelSeed();

		for (int attempt = 0; attempt < 100; attempt++) {
			WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(worldSeed + this.salt() + attempt));

			int targetChunkX = random.nextInt(400) - 200;
			int targetChunkZ = random.nextInt(400) - 200;

			if (x == targetChunkX && z == targetChunkZ) {
				var randomState = structureState.randomState();

				var biomeSource = getBiomeSourceReflection(structureState);

				int quartX = QuartPos.fromBlock(x * 16 + 8);
				int quartY = QuartPos.fromBlock(64);
				int quartZ = QuartPos.fromBlock(z * 16 + 8);

				Holder<Biome> biome = biomeSource.getNoiseBiome(
						quartX, quartY, quartZ,
						randomState.sampler()
				);

				if (this.validBiomes.contains(biome)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public StructurePlacementType<?> type() {
		return MainStructurePlacements.BIOME_AWARE_PLACEMENT.get();
	}
}