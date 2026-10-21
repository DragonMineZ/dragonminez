package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.weapons.TamagamiTridentItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TamagamiTridentModel extends GeoModel<TamagamiTridentItem> {
	@Override
	public ResourceLocation getModelResource(TamagamiTridentItem animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/tamagami_trident.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(TamagamiTridentItem animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/tamagami_trident.png");
	}

	@Override
	public ResourceLocation getAnimationResource(TamagamiTridentItem animatable) {
		return null;
	}
}
