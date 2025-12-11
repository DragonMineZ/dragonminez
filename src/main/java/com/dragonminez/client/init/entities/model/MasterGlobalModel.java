package com.dragonminez.client.init.entities.model;


import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.MastersEntity;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.model.GeoModel;

public class MasterGlobalModel<T extends MastersEntity> extends GeoModel<T> {

    @Override
    public ResourceLocation getModelResource(T animatable) {
        String name = ForgeRegistries.ENTITY_TYPES.getKey(animatable.getType()).getPath();
        return new ResourceLocation(Reference.MOD_ID, "geo/entity/master/" + name + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        String name = ForgeRegistries.ENTITY_TYPES.getKey(animatable.getType()).getPath();
        return new ResourceLocation(Reference.MOD_ID, "textures/entity/master/" + name + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        String name = ForgeRegistries.ENTITY_TYPES.getKey(animatable.getType()).getPath();
        return new ResourceLocation(Reference.MOD_ID, "animations/entity/master/masterentity.animation.json");
    }
}
