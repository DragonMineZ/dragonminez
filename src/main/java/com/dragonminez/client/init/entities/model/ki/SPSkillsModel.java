package com.dragonminez.client.init.entities.model.ki;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.ki.SPBlueHurricaneEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class SPSkillsModel<T extends SPBlueHurricaneEntity & GeoEntity> extends GeoModel<T> {

    @Override
    public ResourceLocation getModelResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/skills/" + name + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/skills/" + name + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/skills/" + name + ".animation.json");
    }
}
