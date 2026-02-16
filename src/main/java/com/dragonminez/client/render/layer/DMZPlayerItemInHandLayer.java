package com.dragonminez.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import javax.annotation.Nullable;

public class DMZPlayerItemInHandLayer<T extends AbstractClientPlayer & GeoAnimatable> extends BlockAndItemGeoLayer<T> {

    public DMZPlayerItemInHandLayer(GeoRenderer<T> renderer) {
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
		if (animatable.isInvisible()) return;

		if (bone.getName().equals("right_hand_item") || bone.getName().equals("left_hand_item")) {
			poseStack.pushPose();
			boolean isLeftHand = bone.getName().equals("left_hand_item");

			Item item = stack.getItem();
			boolean isTool = item instanceof TieredItem || item instanceof TridentItem || item instanceof SwordItem;

			if (isLeftHand) {
				if (item instanceof BlockItem) {
					poseStack.mulPose(Axis.XP.rotationDegrees(-90));
					poseStack.translate(0, 0.1, -0.1);
				} else if (item instanceof ShieldItem && !(animatable.getUseItem() == stack)) {
					poseStack.mulPose(Axis.XP.rotationDegrees(-90));
					poseStack.mulPose(Axis.YP.rotationDegrees(180));
					poseStack.translate(-0.03, 0.135, -1.39);
				} else if (item instanceof ShieldItem && animatable.isUsingItem() && animatable.getUseItem() == stack) {
					poseStack.mulPose(Axis.XP.rotationDegrees(45));
					poseStack.mulPose(Axis.YP.rotationDegrees(125));
					poseStack.mulPose(Axis.ZP.rotationDegrees(-95));
					poseStack.translate(-0.80, 0.75, -0.45);
				} else if (item instanceof BowItem) {
					poseStack.mulPose(Axis.XP.rotationDegrees(-180));
					poseStack.mulPose(Axis.YP.rotationDegrees(12));
					poseStack.mulPose(Axis.ZP.rotationDegrees(-12));
					poseStack.translate(0.1, 0.05, -0.16);
				} else if (item instanceof CrossbowItem) {
					poseStack.mulPose(Axis.ZP.rotationDegrees(60));
					poseStack.mulPose(Axis.XP.rotationDegrees(-90));
					poseStack.translate(-0.42, 0.135, 0.1);
				} else if (isTool) {
					poseStack.mulPose(Axis.XP.rotationDegrees(-25));
					poseStack.mulPose(Axis.ZP.rotationDegrees(180));
					poseStack.translate(-0.06, -0.38, -0.4);
				} else {
					poseStack.mulPose(Axis.XP.rotationDegrees(-90));
					poseStack.translate(0.055, 0.13, -0.1);
				}
			} else {
				poseStack.mulPose(Axis.XP.rotationDegrees(-90));
				if (item instanceof ShieldItem && animatable.isUsingItem() && animatable.getUseItem() == stack) {
					poseStack.translate(-0.15f, 0.135f, -0.05f);
				} else if (item instanceof BowItem) {
					poseStack.translate(0.02f, 0.135f, -0.1f);
				} else {
					poseStack.translate(-0.05f, 0.135f, -0.1f);
				}
			}

			super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
			poseStack.popPose();
		} else {
			super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
		}
	}
}

