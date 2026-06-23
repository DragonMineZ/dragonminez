package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.entity.GravityDeviceBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GravityDeviceBlockModel extends GeoModel<GravityDeviceBlockEntity> {
	@Override
	public ResourceLocation getModelResource(GravityDeviceBlockEntity blockEntity) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/gravitydevice.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(GravityDeviceBlockEntity blockEntity) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/gravitydevice.png");
	}

	@Override
	public ResourceLocation getAnimationResource(GravityDeviceBlockEntity blockEntity) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/block/gravitydevice.animation.json");
	}
}
