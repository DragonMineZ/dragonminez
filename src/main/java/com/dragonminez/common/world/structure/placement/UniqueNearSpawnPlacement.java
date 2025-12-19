package com.dragonminez.common.world.structure.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class UniqueNearSpawnPlacement extends StructurePlacement {

    public static final Codec<UniqueNearSpawnPlacement> CODEC = RecordCodecBuilder.create(instance ->
            placementCodec(instance).apply(instance, UniqueNearSpawnPlacement::new));

    public UniqueNearSpawnPlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod,
                                   float frequency, int salt, Optional<ExclusionZone> exclusionZone) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState structureState, int x, int z) {
		long worldSeed = structureState.getLevelSeed();

		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(worldSeed + this.salt()));

		int targetChunkX = random.nextInt(200) - 100;
		int targetChunkZ = random.nextInt(200) - 100;

		return x == targetChunkX && z == targetChunkZ;
    }

    @Override
    public @NotNull StructurePlacementType<?> type() {
        return MainStructurePlacements.UNIQUE_NEAR_SPAWN.get();
    }
}

