package com.dragonminez.server.world.structure.fitted;

import com.dragonminez.server.world.structure.helper.MainStructureTypes;
import com.dragonminez.server.world.structure.processor.ExteriorAirProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class FittedTemplatePiece extends TemplateStructurePiece {
	private final Rotation rotation;

	public FittedTemplatePiece(StructureTemplateManager templates, ResourceLocation location, BlockPos origin, Rotation rotation) {
		super(MainStructureTypes.FITTED_TEMPLATE_PIECE.get(), 0, templates, location, location.toString(),
				settings(templates, location, rotation), origin);
		this.rotation = rotation;
	}

	public FittedTemplatePiece(StructureTemplateManager templates, CompoundTag tag) {
		super(MainStructureTypes.FITTED_TEMPLATE_PIECE.get(), tag, templates,
				location -> settings(templates, location, Rotation.valueOf(tag.getString("Rot"))));
		this.rotation = Rotation.valueOf(tag.getString("Rot"));
	}

	private static StructurePlaceSettings settings(StructureTemplateManager templates, ResourceLocation location, Rotation rotation) {
		return new StructurePlaceSettings()
				.setRotation(rotation)
				.setKnownShape(true)
				.setIgnoreEntities(false)
				.setFinalizeEntities(true)
				.addProcessor(new ExteriorAirProcessor(TemplateShape.of(templates.getOrCreate(location))))
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
				.addProcessor(JigsawReplacementProcessor.INSTANCE);
	}

	public ResourceLocation templateId() {
		return this.makeTemplateLocation();
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putString("Rot", this.rotation.name());
	}

	@Override
	protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
		if (box.isInside(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
	}
}
