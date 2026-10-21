package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.animal.GiantTurtleEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GiantTurtleModel<T extends GiantTurtleEntity> extends GeoModel<T> {

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/animal/giantturtle.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return animatable.getCurrentTexture();
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/animal/giantturtle.animation.json");
    }
}
