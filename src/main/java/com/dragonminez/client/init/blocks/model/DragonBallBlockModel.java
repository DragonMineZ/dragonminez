package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallSetAssetDefinition;
import com.dragonminez.common.init.block.entity.DragonBallBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DragonBallBlockModel extends GeoModel<DragonBallBlockEntity> {
	@Override
	public ResourceLocation getModelResource(DragonBallBlockEntity blockEntity) {
		DragonBallSetAssetDefinition assets = DragonBallDefinitions.getBallSet(blockEntity.getBallSetId()) != null
			? DragonBallDefinitions.getBallSet(blockEntity.getBallSetId()).resolveAssetDefinition()
			: null;
		if (assets != null && assets.getGeoModelPath().isPresent()) {
			return ResourceLocation.parse(assets.getGeoModelPath().get());
		}
		String modelName = blockEntity.isNamekian() ? "dballnamek" : "dball";
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/block/" + modelName + ".geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DragonBallBlockEntity blockEntity) {
		DragonBallSetAssetDefinition assets = DragonBallDefinitions.getBallSet(blockEntity.getBallSetId()) != null
			? DragonBallDefinitions.getBallSet(blockEntity.getBallSetId()).resolveAssetDefinition()
			: null;
		if (assets != null && assets.getGeoTexturePathForStar(blockEntity.getBallType().getStars()).isPresent()) {
			return ResourceLocation.parse(assets.getGeoTexturePathForStar(blockEntity.getBallType().getStars()).get());
		}
		String prefix = blockEntity.isNamekian() ? "dballnamekblock" : "dballblock";
		int starNumber = blockEntity.getBallType().getStars();
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/" + prefix + starNumber + ".png");
	}

	@Override
	public ResourceLocation getAnimationResource(DragonBallBlockEntity blockEntity) {
		DragonBallSetAssetDefinition assets = DragonBallDefinitions.getBallSet(blockEntity.getBallSetId()) != null
			? DragonBallDefinitions.getBallSet(blockEntity.getBallSetId()).resolveAssetDefinition()
			: null;
		if (assets != null && assets.getAnimationPath().isPresent()) {
			return ResourceLocation.parse(assets.getAnimationPath().get());
		}
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/block/dball.animation.json");
	}
}
