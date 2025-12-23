package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.entity.KikonoStationBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class KikonoStationBlockModel extends GeoModel<KikonoStationBlockEntity> {
	@Override
	public ResourceLocation getModelResource(KikonoStationBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/kikono_station.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(KikonoStationBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/kikono_station.png");
	}

	@Override
	public ResourceLocation getAnimationResource(KikonoStationBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/block/kikono_station.animation.json");
	}
}