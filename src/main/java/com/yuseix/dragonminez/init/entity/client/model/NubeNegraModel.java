package com.yuseix.dragonminez.init.entity.client.model;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.init.entity.custom.NubeNegraEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/*
 * This file uses GeckoLib, licensed under the MIT License.
 * Copyright © 2024 GeckoThePecko.
 */

public class NubeNegraModel extends GeoModel<NubeNegraEntity> {
	@Override
	public ResourceLocation getModelResource(NubeNegraEntity nubeNegraEntity) {
		return new ResourceLocation(DragonMineZ.MOD_ID, "geo/cloud.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(NubeNegraEntity nubeNegraEntity) {
		return new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/black_cloud.png");
	}

	@Override
	public ResourceLocation getAnimationResource(NubeNegraEntity nubeNegraEntity) {
		return null;
	}
}
