package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.data.DMZAnimatable;
import com.dragonminez.client.render.layer.base.BlockAndItemLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

import javax.annotation.Nullable;

public class DMZPlayerItemInHandLayer extends BlockAndItemLayer {
    public DMZPlayerItemInHandLayer(GeoReplacedEntityRenderer<AbstractClientPlayer, DMZAnimatable> renderer) {
        super(renderer);
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, DMZAnimatable animatable) {
        final AbstractClientPlayer player = this.player();
        if (player == null) {
            return null;
        }

        if (bone.getName().equals("right_hand_item")) {
            return player.getMainArm() == HumanoidArm.RIGHT ?
                    player.getMainHandItem() :
                    player.getOffhandItem();
        }

        if (bone.getName().equals("left_hand_item")) {
            return player.getMainArm() == HumanoidArm.LEFT ?
                    player.getMainHandItem() :
                    player.getOffhandItem();
        }


        return null;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, DMZAnimatable animatable) {
        final AbstractClientPlayer player = this.player();
        if (player == null) {
            return ItemDisplayContext.NONE;
        }
        if (bone.getName().equals("right_hand_item")) {
            return player.getMainArm() == HumanoidArm.RIGHT ?
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND :
                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        }

        if (bone.getName().equals("left_hand_item")) {
            return player.getMainArm() == HumanoidArm.LEFT ?
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND :
                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        }

        return ItemDisplayContext.NONE;
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                                      DMZAnimatable animatable, MultiBufferSource bufferSource,
                                      float partialTick, int packedLight, int packedOverlay) {
        if (bone.getName().equals("right_hand_item") || bone.getName().equals("left_hand_item")) {
            final AbstractClientPlayer player = this.player();
            if (player == null) {
                return;
            }

            poseStack.pushPose();

            boolean isMainHand = (bone.getName().equals("right_hand_item"));
            if (isMainHand) {
                if (stack.getItem() instanceof ShieldItem) {
                    if (player.isUsingItem() && player.getUseItem() == stack) {
                        poseStack.translate(-0.0, -0.01, -0.14);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-140));
                        poseStack.mulPose(Axis.XN.rotationDegrees(-65));
                        poseStack.mulPose(Axis.YP.rotationDegrees(-25));
//                        poseStack.mulPose(Axis.ZP.rotationDegrees(35));

                    } else {
                        poseStack.translate(-0.0, -0.15, -0.14);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    }
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
                    if (player.isUsingItem() && player.getUseItem() == stack) {
                        poseStack.translate(-0.70, 1.15, 0.35);
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

    private AbstractClientPlayer player() {
        if (this.renderer instanceof GeoReplacedEntityRenderer<?, ?> geoRenderer) {
            return (AbstractClientPlayer) geoRenderer.getCurrentEntity();
        }
        return null;
    }
}

