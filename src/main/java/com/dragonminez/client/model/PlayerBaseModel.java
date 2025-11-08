package com.dragonminez.client.model;

import com.dragonminez.DragonMineZ;
import com.dragonminez.Reference;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class PlayerBaseModel<T extends AbstractClientPlayer & GeoAnimatable> extends GeoModel<T> {

    public PlayerBaseModel() {
    }

    @Override
    public ResourceLocation getModelResource(T t) {
        return new ResourceLocation(Reference.MOD_ID, "geo/entity/races/base.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T t) {
        return new ResourceLocation(Reference.MOD_ID, "textures/entity/races/base.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T t) {
        return null;
    }
}
