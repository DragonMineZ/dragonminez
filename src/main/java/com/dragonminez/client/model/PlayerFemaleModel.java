package com.dragonminez.client.model;

import com.dragonminez.Reference;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

public class PlayerFemaleModel<T extends AbstractClientPlayer & GeoAnimatable> extends PlayerBaseModel<T> {

    public PlayerFemaleModel(String raceName, String customModel) {
        super(raceName, customModel);
    }

    public PlayerFemaleModel() {
        super();
    }

    @Override
    public ResourceLocation getTextureResource(T t) {
        boolean isCustomModel = !getModelResource(t).getPath().contains("base.geo.json");

        if (isCustomModel) {
            return super.getTextureResource(t);
        }
        
        return new ResourceLocation(Reference.MOD_ID, "textures/entity/races/pan.png");
    }
}
