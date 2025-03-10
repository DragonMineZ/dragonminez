package com.yuseix.dragonminez.init.entity.client.model.masters;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.init.entity.custom.masters.EnmaEntity;
import com.yuseix.dragonminez.init.entity.custom.masters.GuruEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;


public class GuruModel extends GeoModel<GuruEntity> {
	@Override
	public ResourceLocation getModelResource(GuruEntity karinEntity) {
		return new ResourceLocation(DragonMineZ.MOD_ID, "geo/masters/guru.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(GuruEntity karinEntity) {
		return new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/masters/guru.png");
	}

	@Override
	public ResourceLocation getAnimationResource(GuruEntity wa) {
		return new ResourceLocation(DragonMineZ.MOD_ID, "animations/guru.animation.json");
	}


}
