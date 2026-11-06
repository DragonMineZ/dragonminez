package com.dragonminez.common.init.item.render;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.PatternStationItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PatternStationItemModel extends GeoModel<PatternStationItem> {
	@Override
	public ResourceLocation getModelResource(PatternStationItem animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/pattern_station.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(PatternStationItem animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/pattern_station.png");
	}

	@Override
	public ResourceLocation getAnimationResource(PatternStationItem animatable) {
		return null;
	}
}
