package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.weapons.TamagamiSwordItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TamagamiSwordModel extends GeoModel<TamagamiSwordItem> {
	@Override
	public ResourceLocation getModelResource(TamagamiSwordItem animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/tamagami_sword.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(TamagamiSwordItem animatable) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/tamagami_sword.png");
	}

	@Override
	public ResourceLocation getAnimationResource(TamagamiSwordItem animatable) {
		return null;
	}
}
