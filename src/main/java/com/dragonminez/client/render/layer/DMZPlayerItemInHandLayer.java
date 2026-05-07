package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.util.WeaponGripProfile;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.logic.weapon.WeaponRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
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

	private static final String RIGHT_GRIP = "right_hand_item";
	private static final String LEFT_GRIP = "left_hand_item";

	public DMZPlayerItemInHandLayer(GeoRenderer<T> renderer) {
		super(renderer);
	}

	@Nullable
	@Override
	protected ItemStack getStackForBone(GeoBone bone, T animatable) {
		String name = bone.getName();
		boolean isTwoHanded = PlayerAttackHelper.isTwoHandedWielding(animatable);

		boolean rightIsOffhand = name.equals(RIGHT_GRIP) && animatable.getMainArm() != HumanoidArm.RIGHT;
		boolean leftIsOffhand  = name.equals(LEFT_GRIP)  && animatable.getMainArm() != HumanoidArm.LEFT;

		if (isTwoHanded && (rightIsOffhand || leftIsOffhand)) return ItemStack.EMPTY;

		if (name.equals(RIGHT_GRIP)) return animatable.getMainArm() == HumanoidArm.RIGHT ? animatable.getMainHandItem() : animatable.getOffhandItem();
		if (name.equals(LEFT_GRIP)) return animatable.getMainArm() == HumanoidArm.LEFT ? animatable.getMainHandItem() : animatable.getOffhandItem();

		return null;
	}

	@Override
	protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, T animatable) {
		String name = bone.getName();
		if (name.equals(RIGHT_GRIP)) return animatable.getMainArm() == HumanoidArm.RIGHT ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
		if (name.equals(LEFT_GRIP)) return animatable.getMainArm() == HumanoidArm.LEFT ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

		return ItemDisplayContext.NONE;
	}

	@Override
	protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, T animatable, MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
		if (animatable.isInvisible()) return;

		String name = bone.getName();
		boolean isRight = name.equals(RIGHT_GRIP);
		boolean isLeft  = name.equals(LEFT_GRIP);

		if (!isRight && !isLeft) {
			super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
			return;
		}

		poseStack.pushPose();

		boolean isUsing = animatable.isUsingItem() && animatable.getUseItem() == stack;
		String weaponType = resolveWeaponType(animatable);

		WeaponGripProfile profile = WeaponGripProfile.resolve(stack.getItem(), isUsing, weaponType);
		profile.apply(poseStack, isLeft);

		super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
		poseStack.popPose();
	}

	private String resolveWeaponType(T animatable) {
		var attrs = WeaponRegistry.getAttributes(animatable.getMainHandItem());
		return attrs != null ? attrs.category() : null;
	}
}