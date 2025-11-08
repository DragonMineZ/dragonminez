package com.dragonminez.client.model;

import com.dragonminez.Reference;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class PlayerMaleModel<T extends AbstractClientPlayer & GeoAnimatable> extends GeoModel<T> {

    public PlayerMaleModel() {
    }

    @Override
    public ResourceLocation getModelResource(T entity) {
        return new ResourceLocation(Reference.MOD_ID, "geo/entity/races/male.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T entity) {
        return new ResourceLocation(Reference.MOD_ID, "textures/entity/races/base.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T t) {
        return null;
    }
}

