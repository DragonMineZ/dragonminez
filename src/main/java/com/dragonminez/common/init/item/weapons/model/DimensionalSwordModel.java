package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.weapons.BraveSwordItem;
import com.dragonminez.common.init.item.weapons.DimensionalSwordItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DimensionalSwordModel extends GeoModel<DimensionalSwordItem> {
	@Override
	public ResourceLocation getModelResource(DimensionalSwordItem animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/dimensional_sword.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DimensionalSwordItem animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/dimensional_sword.png");
	}

	@Override
	public ResourceLocation getAnimationResource(DimensionalSwordItem animatable) {
		return null;
	}
}
