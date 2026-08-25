package com.dragonminez.server.world.structure.placement;

import com.dragonminez.common.config.ConfigManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public class WideRandomSpreadPlacement extends RandomSpreadStructurePlacement {
	public static final Codec<WideRandomSpreadPlacement> CODEC = RecordCodecBuilder.create(instance ->
			placementCodec(instance).and(
					RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR)
							.forGetter(RandomSpreadStructurePlacement::spreadType)
			).apply(instance, WideRandomSpreadPlacement::new));

	private static final int FALLBACK_SPACING = 1875;
	private static final int FALLBACK_SEPARATION = 750;

	public WideRandomSpreadPlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod,
									 float frequency, int salt, Optional<ExclusionZone> exclusionZone,
									 RandomSpreadType spreadType) {
		super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone,
				FALLBACK_SPACING, FALLBACK_SEPARATION, spreadType);
	}

	public WideRandomSpreadPlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod,
									 float frequency, int salt, Optional<ExclusionZone> exclusionZone) {
		this(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone, RandomSpreadType.LINEAR);
	}

	private static int chunksFromBlocks(int blocks) {
		return Math.max(1, blocks >> 4);
	}

	@Override
	public int spacing() {
		return chunksFromBlocks(ConfigManager.getServerConfig().getWorldGen().getStructureSpacingBlocks());
	}

	@Override
	public int separation() {
		int spacing = spacing();
		int separation = chunksFromBlocks(ConfigManager.getServerConfig().getWorldGen().getStructureSeparationBlocks());
		return Math.min(separation, spacing - 1);
	}

	public int placementSalt() {
		return this.salt();
	}

	@Override
	public @NonNull ChunkPos getPotentialStructureChunk(long seed, int chunkX, int chunkZ) {
		int spacing = spacing();
		int range = spacing - separation();

		int regionX = Math.floorDiv(chunkX, spacing);
		int regionZ = Math.floorDiv(chunkZ, spacing);

		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureWithSalt(seed, regionX, regionZ, this.salt());

		int offsetX = this.spreadType().evaluate(random, range);
		int offsetZ = this.spreadType().evaluate(random, range);

		return new ChunkPos(regionX * spacing + offsetX, regionZ * spacing + offsetZ);
	}

	@Override
	protected boolean isPlacementChunk(@NonNull ChunkGeneratorStructureState structureState, int x, int z) {
		if (!ConfigManager.getServerConfig().getWorldGen().getGenerateCustomStructures()) return false;
		ChunkPos pos = getPotentialStructureChunk(structureState.getLevelSeed(), x, z);
		return pos.x == x && pos.z == z;
	}

	@Override
	public @NonNull StructurePlacementType<?> type() {
		return MainStructurePlacements.WIDE_RANDOM_SPREAD.get();
	}
}
