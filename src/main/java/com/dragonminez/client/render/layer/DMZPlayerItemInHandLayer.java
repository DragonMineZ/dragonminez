package com.dragonminez.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import javax.annotation.Nullable;

public class DMZPlayerItemInHandLayer<T extends AbstractClientPlayer & GeoAnimatable> extends BlockAndItemGeoLayer<T> {
	private static final String RIGHT_HAND = "right_hand_item";
	private static final String LEFT_HAND = "left_hand_item";

	private static final Quaternionf X_NEG_90 = Axis.XP.rotationDegrees(-90);
	private static final Quaternionf X_NEG_180 = Axis.XP.rotationDegrees(-180);
	private static final Quaternionf X_NEG_25 = Axis.XP.rotationDegrees(-25);
	private static final Quaternionf X_45 = Axis.XP.rotationDegrees(45);

	private static final Quaternionf Y_180 = Axis.YP.rotationDegrees(180);
	private static final Quaternionf Y_125 = Axis.YP.rotationDegrees(125);
	private static final Quaternionf Y_12 = Axis.YP.rotationDegrees(12);

	private static final Quaternionf Z_180 = Axis.ZP.rotationDegrees(180);
	private static final Quaternionf Z_60 = Axis.ZP.rotationDegrees(60);
	private static final Quaternionf Z_NEG_12 = Axis.ZP.rotationDegrees(-12);
	private static final Quaternionf Z_NEG_95 = Axis.ZP.rotationDegrees(-95);

	public DMZPlayerItemInHandLayer(GeoRenderer<T> renderer) {
		super(renderer);
	}

	@Nullable
	@Override
	protected ItemStack getStackForBone(GeoBone bone, T animatable) {
		String boneName = bone.getName();
		if (boneName.equals(RIGHT_HAND)) {
			return animatable.getMainArm() == HumanoidArm.RIGHT ? animatable.getMainHandItem() : animatable.getOffhandItem();
		}

		if (boneName.equals(LEFT_HAND)) {
			return animatable.getMainArm() == HumanoidArm.LEFT ? animatable.getMainHandItem() : animatable.getOffhandItem();
		}

		return null;
	}

	@Override
	protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, T animatable) {
		String boneName = bone.getName();
		if (boneName.equals(RIGHT_HAND)) {
			return animatable.getMainArm() == HumanoidArm.RIGHT ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
		}

		if (boneName.equals(LEFT_HAND)) {
			return animatable.getMainArm() == HumanoidArm.LEFT ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
		}

		return ItemDisplayContext.NONE;
	}

	@Override
	protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, T animatable, MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
		if (animatable.isInvisible()) return;

		String boneName = bone.getName();
		boolean isRightHand = boneName.equals(RIGHT_HAND);
		boolean isLeftHand = boneName.equals(LEFT_HAND);

		if (isRightHand || isLeftHand) {
			poseStack.pushPose();

			Item item = stack.getItem();
			boolean isTool = item instanceof TieredItem || item instanceof TridentItem || item instanceof SwordItem;

			if (isLeftHand) {
				if (item instanceof BlockItem) {
					poseStack.mulPose(X_NEG_90);
					poseStack.translate(0, 0.1, -0.1);
				} else if (item instanceof ShieldItem && animatable.getUseItem() != stack) {
					poseStack.mulPose(X_NEG_90);
					poseStack.mulPose(Y_180);
					poseStack.translate(-0.03, 0.135, -1.39);
				} else if (item instanceof ShieldItem && animatable.isUsingItem() && animatable.getUseItem() == stack) {
					poseStack.mulPose(X_45);
					poseStack.mulPose(Y_125);
					poseStack.mulPose(Z_NEG_95);
					poseStack.translate(-0.80, 0.75, -0.45);
				} else if (item instanceof BowItem) {
					poseStack.mulPose(X_NEG_180);
					poseStack.mulPose(Y_12);
					poseStack.mulPose(Z_NEG_12);
					poseStack.translate(0.1, 0.05, -0.16);
				} else if (item instanceof CrossbowItem) {
					poseStack.mulPose(Z_60);
					poseStack.mulPose(X_NEG_90);
					poseStack.translate(-0.42, 0.135, 0.1);
				} else if (isTool) {
					poseStack.mulPose(X_NEG_25);
					poseStack.mulPose(Z_180);
					poseStack.translate(-0.06, -0.38, -0.4);
				} else {
					poseStack.mulPose(X_NEG_90);
					poseStack.translate(0.055, 0.13, -0.1);
				}
			} else {
				poseStack.mulPose(X_NEG_90);
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