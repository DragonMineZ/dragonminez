package com.yuseix.dragonminez.init.entity.client.renderer.masters;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.init.entity.client.model.masters.DendeModel;
import com.yuseix.dragonminez.init.entity.custom.masters.DendeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/*
 * This file uses GeckoLib, licensed under the MIT License.
 * Copyright © 2024 GeckoThePecko.
 */

public class DendeRenderer extends GeoEntityRenderer<DendeEntity> {
	public DendeRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new DendeModel());
	}

	@Override
	public ResourceLocation getTextureLocation(DendeEntity animatable) {
		return new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/masters/dende_master.png");
	}
}
