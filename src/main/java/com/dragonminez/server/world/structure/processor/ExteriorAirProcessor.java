package com.dragonminez.server.world.structure.processor;

import com.dragonminez.server.world.structure.fitted.TemplateShape;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import javax.annotation.Nullable;

public class ExteriorAirProcessor extends StructureProcessor {
	public static final Codec<ExteriorAirProcessor> CODEC = Codec.unit(() -> new ExteriorAirProcessor(null));

	@Nullable
	private final TemplateShape shape;

	public ExteriorAirProcessor(@Nullable TemplateShape shape) {
		this.shape = shape;
	}

	@Nullable
	@Override
	public StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos, StructureBlockInfo original,
										   StructureBlockInfo relative, StructurePlaceSettings settings) {
		if (this.shape != null && relative.state().isAir() && this.shape.isExteriorAir(original.pos())) return null;
		return relative;
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return MainStructureProcessors.EXTERIOR_AIR.get();
	}
}
