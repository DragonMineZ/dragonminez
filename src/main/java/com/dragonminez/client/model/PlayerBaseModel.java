package com.dragonminez.client.model;

import com.dragonminez.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.util.RenderUtils;

public class PlayerBaseModel<T extends AbstractClientPlayer & GeoAnimatable> extends GeoModel<T> {

    private final PlayerModel<T> vanillaModel;

    public PlayerBaseModel() {
        var modelSet = Minecraft.getInstance().getEntityModels();
        ModelPart root = modelSet.bakeLayer(ModelLayers.PLAYER);
        this.vanillaModel = new PlayerModel<>(root, false);
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
        return new ResourceLocation(Reference.MOD_ID, "animations/entity/races/base.animation.json");
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        var head = this.getAnimationProcessor().getBone("head");
        var body = this.getAnimationProcessor().getBone("body");
        var right_arm = this.getAnimationProcessor().getBone("right_arm");
        var left_arm = this.getAnimationProcessor().getBone("left_arm");
        var right_leg = this.getAnimationProcessor().getBone("right_leg");
        var left_leg = this.getAnimationProcessor().getBone("left_leg");

        float partialTick = animationState.getPartialTick();
        float ageInTicks = animatable.tickCount + partialTick;
        float limbSwing = animationState.getLimbSwing();
        float limbSwingAmount = animationState.getLimbSwingAmount();

        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

        float headYaw = entityData.netHeadYaw();
        float headPitch = entityData.headPitch();


        this.vanillaModel.riding = animatable.isPassenger();
        this.vanillaModel.crouching = animatable.isCrouching();


        this.vanillaModel.setupAnim(animatable, limbSwing, limbSwingAmount, ageInTicks,  headYaw, headPitch);

        if(head != null){
            RenderUtils.matchModelPartRot(this.vanillaModel.head, head);
//            head.updatePosition(vanillaModel.head.xRot, -vanillaModel.head.yRot, vanillaModel.head.zRot);
            head.setRotX(vanillaModel.head.xRot);
            head.setRotY(vanillaModel.head.yRot);
            head.setRotZ(vanillaModel.head.zRot);
        }

        RenderUtils.matchModelPartRot(this.vanillaModel.body, body);
//        body.updatePosition(vanillaModel.body.xRot, -vanillaModel.body.yRot, vanillaModel.body.zRot);
//        body.setRotX(-vanillaModel.body.xRot);
//        body.setRotY(vanillaModel.body.yRot);
//        body.setRotZ(vanillaModel.body.zRot);

        RenderUtils.matchModelPartRot(this.vanillaModel.rightArm, right_arm);
//        right_arm.updatePosition(vanillaModel.rightArm.xRot, vanillaModel.rightArm.yRot, vanillaModel.rightArm.zRot);
//        right_arm.setRotX(-vanillaModel.rightArm.xRot);
//        right_arm.setRotY(vanillaModel.rightArm.yRot);
//        right_arm.setRotZ(vanillaModel.rightArm.zRot);

        RenderUtils.matchModelPartRot(this.vanillaModel.leftArm, left_arm);
//        left_arm.updatePosition(vanillaModel.leftArm.xRot, vanillaModel.leftArm.yRot, vanillaModel.leftArm.zRot);
//        left_arm.setRotX(-vanillaModel.leftArm.xRot);
//        left_arm.setRotY(vanillaModel.leftArm.yRot);
//        left_arm.setRotZ(vanillaModel.leftArm.zRot);

        RenderUtils.matchModelPartRot(this.vanillaModel.rightLeg, right_leg);
//        right_leg.updatePosition(vanillaModel.rightLeg.x, -vanillaModel.rightLeg.y, vanillaModel.rightLeg.z);
//        right_leg.setRotX(vanillaModel.rightLeg.xRot);
//        right_leg.setRotY(-vanillaModel.rightLeg.yRot);
//        right_leg.setRotZ(-vanillaModel.rightLeg.zRot);

//        left_leg.updatePosition(vanillaModel.leftLeg.x, -vanillaModel.leftLeg.y, vanillaModel.leftLeg.z);
//        left_leg.setRotX(-vanillaModel.leftLeg.xRot);
//        left_leg.setRotY(vanillaModel.leftLeg.yRot);
//        left_leg.setRotZ(vanillaModel.leftLeg.zRot);
        RenderUtils.matchModelPartRot(this.vanillaModel.leftLeg, left_leg);

    }


}