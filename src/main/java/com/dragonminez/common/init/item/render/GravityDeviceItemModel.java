package com.dragonminez.common.init.item.render;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.GravityDeviceItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GravityDeviceItemModel extends GeoModel<GravityDeviceItem> {
	@Override
	public ResourceLocation getModelResource(GravityDeviceItem animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/gravitydevice.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(GravityDeviceItem animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/gravitydevice.png");
	}

	@Override
	public ResourceLocation getAnimationResource(GravityDeviceItem animatable) {
		return null;
	}
}
