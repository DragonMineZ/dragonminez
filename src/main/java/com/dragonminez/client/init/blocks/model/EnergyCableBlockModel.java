package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.entity.EnergyCableBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EnergyCableBlockModel extends GeoModel<EnergyCableBlockEntity> {
	@Override
	public ResourceLocation getModelResource(EnergyCableBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/energy_cable.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(EnergyCableBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/energy_cable.png");
	}

	@Override
	public ResourceLocation getAnimationResource(EnergyCableBlockEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/block/energy_cable.animation.json");
	}
}