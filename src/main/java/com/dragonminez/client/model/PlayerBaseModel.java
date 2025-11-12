package com.dragonminez.client.model;

import com.dragonminez.Reference;
import com.dragonminez.client.util.RenderUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class PlayerBaseModel<T extends AbstractClientPlayer & GeoAnimatable> extends GeoModel<T> {


    public PlayerBaseModel() {
    }

    @Override
    public ResourceLocation getModelResource(T t) {
        return new ResourceLocation(Reference.MOD_ID, "geo/entity/races/base.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T t) {
        return new ResourceLocation(Reference.MOD_ID, "textures/entity/races/krillin.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T t) {
        return new ResourceLocation(Reference.MOD_ID, "animations/entity/races/base.animation.json");
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        var head = this.getAnimationProcessor().getBone("head");
        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

        if(head != null){
            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }

        // Animaciones procedurales adicionales (arco, ballesta, etc.)
        // Solo se aplican cuando el jugador está usando estos items específicos
        try {
            float partialTick = animationState.getPartialTick();
            float ageInTicks = (float) animatable.getTick(animatable);

            var rightArm = this.getAnimationProcessor().getBone("right_arm");
            var leftArm = this.getAnimationProcessor().getBone("left_arm");

            if (rightArm != null) {
                RenderUtil.animateHand(animatable, rightArm, partialTick, ageInTicks);
            }
            if (leftArm != null) {
                RenderUtil.animateHand(animatable, leftArm, partialTick, ageInTicks);
            }
        } catch (Exception e) {
            // Ignorar errores de animación para evitar crashes
        }
    }


}