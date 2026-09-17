package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.entity.PatternStationBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PatternStationBlockModel extends GeoModel<PatternStationBlockEntity> {
	@Override
	public ResourceLocation getModelResource(PatternStationBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/pattern_station.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(PatternStationBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/pattern_station.png");
	}

	@Override
	public ResourceLocation getAnimationResource(PatternStationBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/block/pattern_station.animation.json");
	}
}
