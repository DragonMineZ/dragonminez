package com.dragonminez.server.world.structure.placement;

import com.dragonminez.common.config.ConfigManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;

public class FixedStructurePlacement extends StructurePlacement {

	public static final Codec<FixedStructurePlacement> CODEC = RecordCodecBuilder.create(instance ->
			placementCodec(instance).and(instance.group(
					Codec.INT.fieldOf("fixed_x").forGetter(p -> p.fixedX),
					Codec.INT.fieldOf("fixed_z").forGetter(p -> p.fixedZ),
					Rotation.CODEC.optionalFieldOf("rotation", Rotation.NONE).forGetter(p -> p.rotation)
			)).apply(instance, FixedStructurePlacement::new));

	private final int fixedX;
	private final int fixedZ;
	private final Rotation rotation;

	public FixedStructurePlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod,
								   float frequency, int salt, Optional<ExclusionZone> exclusionZone,
								   int fixedX, int fixedZ, Rotation rotation) {
		super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
		this.fixedX = fixedX;
		this.fixedZ = fixedZ;
		this.rotation = rotation;
	}

	public FixedStructurePlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod,
								   float frequency, int salt, Optional<ExclusionZone> exclusionZone,
								   int fixedX, int fixedZ) {
		this(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone, fixedX, fixedZ, Rotation.NONE);
	}

	@Override
	protected boolean isPlacementChunk(ChunkGeneratorStructureState structureState, int x, int z) {
		if (!ConfigManager.getServerConfig().getWorldGen().isGenerateCustomStructures()) {
			return false;
		} else if (x == this.fixedX && z == this.fixedZ) {
			return true;
		}
		return false;
	}

	public int getFixedX() {
		return this.fixedX;
	}

	public int getFixedZ() {
		return this.fixedZ;
	}

	public Rotation getRotation() {
		return this.rotation;
	}

	@Override
	public StructurePlacementType<?> type() {
		return MainStructurePlacements.FIXED_PLACEMENT.get();
	}
}