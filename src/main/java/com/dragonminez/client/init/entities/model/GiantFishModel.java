package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.animal.GiantFishEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class GiantFishModel<T extends GiantFishEntity> extends GeoModel<T> {

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/animal/giantfish.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return animatable.getCurrentTexture();
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/animal/giantfish.animation.json");
    }
}
