package com.dragonminez.client.init.entities.model.rr;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.redribbon.RedRibbonEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.core.registries.BuiltInRegistries;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class RedRibbonRobotModel<T extends RedRibbonEntity> extends GeoModel<T> {

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/enemies/robot1.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/enemies/" + name + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/enemies/robot1.animation.json");
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        GeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }
    }

}
