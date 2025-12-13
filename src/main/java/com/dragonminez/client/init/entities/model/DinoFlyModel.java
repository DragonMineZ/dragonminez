package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.animal.DinoFlyEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class DinoFlyModel extends GeoModel<DinoFlyEntity> {

    @Override
    public ResourceLocation getModelResource(DinoFlyEntity animatable) {
        return new ResourceLocation(Reference.MOD_ID, "geo/entity/animal/dino3.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DinoFlyEntity animatable) {
        return new ResourceLocation(Reference.MOD_ID, "textures/entity/animal/dino3.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DinoFlyEntity animatable) {
        return new ResourceLocation(Reference.MOD_ID, "animations/entity/animal/dino3.animation.json");
    }

    @Override
    public void setCustomAnimations(DinoFlyEntity animatable, long instanceId, AnimationState<DinoFlyEntity> animationState) {
        CoreGeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }
    }
}
