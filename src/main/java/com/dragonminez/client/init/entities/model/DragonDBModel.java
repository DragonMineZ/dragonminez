
package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonAssetDefinition;
import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.init.entities.dragon.DragonWishEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class DragonDBModel<T extends DragonWishEntity> extends GeoModel<T> {

	@Override
	public ResourceLocation getModelResource(T animatable) {
		DragonAssetDefinition assets = resolveAssets(animatable);
		if (assets != null && assets.getModelPath().isPresent()) return ResourceLocation.parse(assets.getModelPath().get());
		String name = ForgeRegistries.ENTITY_TYPES.getKey(animatable.getType()).getPath();
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/dragon/" + name + ".geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(T animatable) {
		DragonAssetDefinition assets = resolveAssets(animatable);
		if (assets != null && assets.getTexturePath().isPresent()) return ResourceLocation.parse(assets.getTexturePath().get());
		String name = ForgeRegistries.ENTITY_TYPES.getKey(animatable.getType()).getPath();
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/dragon/" + name + ".png");
	}

	@Override
	public ResourceLocation getAnimationResource(T animatable) {
		DragonAssetDefinition assets = resolveAssets(animatable);
		if (assets != null && assets.getAnimationPath().isPresent()) return ResourceLocation.parse(assets.getAnimationPath().get());
		String name = ForgeRegistries.ENTITY_TYPES.getKey(animatable.getType()).getPath();
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/dragon/" + name + ".animation.json");
	}

	private DragonAssetDefinition resolveAssets(T animatable) {
		DragonDefinition definition = animatable.getDragonDefinition();
		return definition == null ? null : definition.resolveAssetDefinition();
	}

	@Override
	public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
		CoreGeoBone head = getAnimationProcessor().getBone("head");

		if (head != null) {
			EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

			head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
			head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
		}
	}
}
