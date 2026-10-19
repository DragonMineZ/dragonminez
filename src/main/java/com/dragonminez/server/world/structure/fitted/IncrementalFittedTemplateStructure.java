package com.dragonminez.server.world.structure.fitted;

import com.dragonminez.server.world.structure.helper.MainStructureTypes;
import com.dragonminez.server.world.structure.placement.IncrementalStructure;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class IncrementalFittedTemplateStructure extends FittedTemplateStructure implements IncrementalStructure {
	public static final Codec<IncrementalFittedTemplateStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			settingsCodec(instance),
			ResourceLocation.CODEC.fieldOf("template").forGetter(FittedTemplateStructure::template),
			TerrainFit.CODEC.optionalFieldOf("terrain_fit", TerrainFit.NONE).forGetter(FittedTemplateStructure::fit),
			Rotation.CODEC.optionalFieldOf("rotation").forGetter(FittedTemplateStructure::fixedRotation),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("min_water", 0.0F).forGetter(FittedTemplateStructure::minWater),
			Codec.intRange(2, 64).optionalFieldOf("sample_step", 16).forGetter(structure -> structure.sampleStep)
	).apply(instance, IncrementalFittedTemplateStructure::new));

	private static final int COMPLETION_REACH = 200;
	private static final int ESTIMATE_COARSENESS = 2;

	private final float minWater;
	private final int sampleStep;

	public IncrementalFittedTemplateStructure(StructureSettings settings, ResourceLocation template, TerrainFit fit,
											  Optional<Rotation> fixedRotation, float minWater, int sampleStep) {
		super(settings, template, fit, fixedRotation, 0.0F);
		this.minWater = minWater;
		this.sampleStep = sampleStep;
	}

	@Override
	public float minWater() {
		return this.minWater;
	}

	@Override
	protected int referenceReach() {
		return COMPLETION_REACH;
	}

	@Override
	protected int estimateCoarseness() {
		return ESTIMATE_COARSENESS;
	}

	@Override
	protected int sampleStep(TemplateShape shape) {
		return this.sampleStep;
	}

	@Override
	public StructureType<?> type() {
		return MainStructureTypes.INCREMENTAL_FITTED_TEMPLATE.get();
	}
}
