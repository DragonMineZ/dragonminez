package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.weapons.TamagamiHammerItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TamagamiHammerModel extends GeoModel<TamagamiHammerItem> {
	@Override
	public ResourceLocation getModelResource(TamagamiHammerItem animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/tamagami_hammer.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(TamagamiHammerItem animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/tamagami_hammer.png");
	}

	@Override
	public ResourceLocation getAnimationResource(TamagamiHammerItem animatable) {
		return null;
	}
}
