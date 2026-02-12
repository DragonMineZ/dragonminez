package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.BlackNimbusEntity;
import com.dragonminez.common.init.entities.FlyingNimbusEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BlackNimbusModel<T extends BlackNimbusEntity> extends GeoModel<T> {

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/kinton.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/black_kinton.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/kinton.animation.json");
    }

}
