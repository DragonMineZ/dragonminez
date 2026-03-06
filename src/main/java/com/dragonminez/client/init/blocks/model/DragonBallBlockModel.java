package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.entity.DragonBallBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DragonBallBlockModel extends GeoModel<DragonBallBlockEntity> {
	@Override
	public ResourceLocation getModelResource(DragonBallBlockEntity dballBlockEntity) {
		String modelName = dballBlockEntity.getBallName();
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/" + modelName + ".geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DragonBallBlockEntity dballBlockEntity) {
		String prefix = dballBlockEntity.getBallName();
		int stars = dballBlockEntity.getBallType().getStars();
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/" + prefix + stars + ".png");
	}

	@Override
	public ResourceLocation getAnimationResource(DragonBallBlockEntity dballBlockEntity) {
		String animName = dballBlockEntity.getBallName();
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/block/" + animName + ".animation.json");
	}
}
