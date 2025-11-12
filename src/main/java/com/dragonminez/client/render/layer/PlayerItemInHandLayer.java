package com.dragonminez.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import javax.annotation.Nullable;

public class PlayerItemInHandLayer<T extends AbstractClientPlayer & GeoAnimatable> extends BlockAndItemGeoLayer<T> {

    public PlayerItemInHandLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, T animatable) {
        if (bone.getName().equals("right_hand_item")) {
            return animatable.getMainArm() == HumanoidArm.RIGHT ?
                   animatable.getMainHandItem() :
                   animatable.getOffhandItem();
        }

        if (bone.getName().equals("left_hand_item")) {
            return animatable.getMainArm() == HumanoidArm.LEFT ?
                   animatable.getMainHandItem() :
                   animatable.getOffhandItem();
        }

        return null;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, T animatable) {
        if (bone.getName().equals("right_hand_item")) {
            return animatable.getMainArm() == HumanoidArm.RIGHT ?
                   ItemDisplayContext.THIRD_PERSON_RIGHT_HAND :
                   ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        }

        if (bone.getName().equals("left_hand_item")) {
            return animatable.getMainArm() == HumanoidArm.LEFT ?
                   ItemDisplayContext.THIRD_PERSON_RIGHT_HAND :
                   ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        }

        return ItemDisplayContext.NONE;
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                                     T animatable, MultiBufferSource bufferSource,
                                     float partialTick, int packedLight, int packedOverlay) {

        if (bone.getName().equals("right_hand_item") || bone.getName().equals("left_hand_item")) {
            poseStack.pushPose();

            boolean isMainHand = (bone.getName().equals("right_hand_item"));
            if (isMainHand) {
                if (stack.getItem() instanceof ShieldItem) {
                    poseStack.translate(-0.0, -0.15, -0.14);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                } else if (stack.getItem() instanceof CrossbowItem) {
                    poseStack.translate(-0.075, -0.15, -0.14);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    poseStack.mulPose(Axis.YP.rotationDegrees(15));

                } else if (stack.getItem() instanceof BowItem) {
                    poseStack.translate(0.1, -0.26, -0.14);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-15));
                } else {
                    poseStack.translate(-0.0, -0.15, -0.14);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                }
            } else {
                if (stack.getItem() instanceof ShieldItem) {
                    if (animatable.isUsingItem() && animatable.getUseItem() == stack) {
                        poseStack.translate(-0.40, 1.85, 0.75);
                        poseStack.mulPose(Axis.XP.rotationDegrees(35));
                        poseStack.mulPose(Axis.YP.rotationDegrees(90));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-65));
                    } else {
                        poseStack.translate(-0.0, 1.34, -0.13);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                        poseStack.mulPose(Axis.YP.rotationDegrees(-180));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(0));
                    }
                } else if (stack.getItem() instanceof CrossbowItem) {
                    poseStack.translate(-0.21, -0.45, -0.15);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    poseStack.mulPose(Axis.YP.rotationDegrees(-75));

                } else if (stack.getItem() instanceof BowItem) {
                    poseStack.translate(-0.1, -0.20, -0.20);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-170));
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-15));
                } else {
                    poseStack.translate(-0.0, 0.10, -0.50);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-200));
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                }
            }

            super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
            poseStack.popPose();
        } else {
            super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
        }
    }
}

