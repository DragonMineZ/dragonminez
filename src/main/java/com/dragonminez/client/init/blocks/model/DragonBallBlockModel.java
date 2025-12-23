package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.entity.DragonBallBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DragonBallBlockModel extends GeoModel<DragonBallBlockEntity> {
	@Override
	public ResourceLocation getModelResource(DragonBallBlockEntity dballBlockEntity) {
		String modelName = dballBlockEntity.isNamekian() ? "dballnamek" : "dball";
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/" + modelName + ".geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DragonBallBlockEntity dballBlockEntity) {
		String prefix = dballBlockEntity.isNamekian() ? "dballnamekblock" : "dballblock";
		int starNumber = dballBlockEntity.getBallType().getStars();
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/" + prefix + starNumber + ".png");
	}

	@Override
	public ResourceLocation getAnimationResource(DragonBallBlockEntity dballBlockEntity) {
		String animName = dballBlockEntity.isNamekian() ? "dballnamek" : "dball";
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/block/dball.animation.json");
	}
}
