package com.dragonminez.client.render.layer;

import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.render.util.WeaponGripProfile;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.logic.player.PlayerAttackProperties;
import com.dragonminez.common.combat.logic.weapon.WeaponRegistry;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.util.RenderUtils;

import javax.annotation.Nullable;

public class DMZPlayerItemInHandLayer<T extends AbstractClientPlayer & GeoAnimatable> extends BlockAndItemGeoLayer<T> {

	private static final String RIGHT_GRIP = "right_hand_item";
	private static final String LEFT_GRIP = "left_hand_item";

	public DMZPlayerItemInHandLayer(GeoRenderer<T> renderer) {
		super(renderer);
	}

	@Override
	public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource,
							  VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		ItemStack stack = getStackForBone(bone, animatable);
		if (stack == null || stack.isEmpty()) return;

		poseStack.pushPose();

		float combatWeight = combatPlacementWeight(bone, animatable);
		RenderUtils.translateToPivotPoint(poseStack, bone);
		rotateBoneScaled(poseStack, bone, 1.0F - combatWeight);

		renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);

		bufferSource.getBuffer(renderType);
		poseStack.popPose();
	}

	@Nullable
	@Override
	protected ItemStack getStackForBone(GeoBone bone, T animatable) {
		String name = bone.getName();
		boolean isCombatAnim = isCombatAnim(animatable);
		boolean isTwoHanded = PlayerAttackHelper.isTwoHandedWielding(animatable);

		boolean rightIsOffhand = name.equals(RIGHT_GRIP) && animatable.getMainArm() != HumanoidArm.RIGHT;
		boolean leftIsOffhand  = name.equals(LEFT_GRIP)  && animatable.getMainArm() != HumanoidArm.LEFT;

		if (isTwoHanded && (rightIsOffhand || leftIsOffhand)) return ItemStack.EMPTY;
		if (isCombatAnim) {
			if (name.equals(RIGHT_GRIP)) return animatable.getMainHandItem();
			if (name.equals(LEFT_GRIP)) return animatable.getOffhandItem();
		}

		if (name.equals(RIGHT_GRIP)) return animatable.getMainArm() == HumanoidArm.RIGHT ? animatable.getMainHandItem() : animatable.getOffhandItem();
		if (name.equals(LEFT_GRIP)) return animatable.getMainArm() == HumanoidArm.LEFT ? animatable.getMainHandItem() : animatable.getOffhandItem();

		return null;
	}

	@Override
	protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, T animatable) {
		String name = bone.getName();
		if (useCombatPlacement(bone, animatable)) {
			if (name.equals(RIGHT_GRIP)) return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
			if (name.equals(LEFT_GRIP)) return ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
		}
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

		boolean isCombatAnim = isCombatAnim(animatable);
		float gripWeight = 1.0F - combatPlacementWeight(bone, animatable);

		if (gripWeight > 0.0F) {
			boolean isUsing = animatable.isUsingItem() && animatable.getUseItem() == stack;
			String weaponType = resolveWeaponType(stack);
			WeaponGripProfile profile = WeaponGripProfile.resolve(stack.getItem(), isUsing, weaponType);

			boolean applyAsLeft = isLeft;
			if (!isCombatAnim && animatable.getMainArm() == HumanoidArm.LEFT) applyAsLeft = !isLeft;

			applyGripEased(poseStack, profile, applyAsLeft, gripWeight);

			if (!isCombatAnim && animatable.getMainArm() == HumanoidArm.LEFT) poseStack.translate(0.1 * gripWeight, 0, 0);
		}

		float itemScale = resolveItemScale(animatable);
		if (itemScale != 1.0F) poseStack.scale(itemScale, itemScale, itemScale);

		super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
		poseStack.popPose();
	}

	private boolean isCombatAnim(T animatable) {
		if (!(animatable instanceof IPlayerAnimatable playerAnim) || !playerAnim.dragonminez$isPlayingCombatAnimation()) return false;
		if (animatable instanceof PlayerAttackProperties props) {
			return PlayerAttackHelper.getCurrentAttack(animatable, props.getComboCount()) != null;
		}
		return true;
	}

	private boolean useCombatPlacement(GeoBone bone, T animatable) {
		return combatPlacementWeight(bone, animatable) > 0.5F;
	}

	private float combatPlacementWeight(GeoBone bone, T animatable) {
		if (!(animatable instanceof IPlayerAnimatable playerAnim)) return 0.0F;

		float weight = playerAnim.dragonminez$getCombatPlacementWeight();
		if (weight <= 0.0F) return 0.0F;

		String name = bone.getName();
		boolean isRight = name.equals(RIGHT_GRIP);
		boolean isLeft = name.equals(LEFT_GRIP);
		if (!isRight && !isLeft) return 0.0F;

		boolean boneIsOffhand = (isRight && animatable.getMainArm() == HumanoidArm.LEFT) || (isLeft && animatable.getMainArm() == HumanoidArm.RIGHT);
		boolean isOffhandAttack = playerAnim.dragonminez$isAttackingWithOffhand();
		return boneIsOffhand == isOffhandAttack ? weight : 0.0F;
	}

	private void rotateBoneScaled(PoseStack poseStack, GeoBone bone, float scale) {
		if (scale <= 0.0F) return;
		if (bone.getRotZ() != 0) poseStack.mulPose(Axis.ZP.rotation(bone.getRotZ() * scale));
		if (bone.getRotY() != 0) poseStack.mulPose(Axis.YP.rotation(bone.getRotY() * scale));
		if (bone.getRotX() != 0) poseStack.mulPose(Axis.XP.rotation(bone.getRotX() * scale));
	}

	private void applyGripEased(PoseStack poseStack, WeaponGripProfile profile, boolean isLeft, float weight) {
		if (weight >= 1.0F) {
			profile.apply(poseStack, isLeft);
			return;
		}

		PoseStack temp = new PoseStack();
		profile.apply(temp, isLeft);
		Matrix4f matrix = temp.last().pose();

		Vector3f translation = matrix.getTranslation(new Vector3f());
		Quaternionf rotation = matrix.getNormalizedRotation(new Quaternionf());
		Quaternionf eased = new Quaternionf().slerp(rotation, weight);

		poseStack.translate(translation.x * weight, translation.y * weight, translation.z * weight);
		poseStack.mulPose(eased);
	}

	private String resolveWeaponType(ItemStack stack) {
		var attrs = WeaponRegistry.getAttributes(stack);
		return attrs != null ? attrs.category() : null;
	}

	private float resolveItemScale(T animatable) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(null);
		if (stats == null) return 1.0F;

		var character = stats.getCharacter();
		if (character == null) return 1.0F;

		Float[] baseScale = character.getModelScaling();
		if (baseScale == null || baseScale.length < 3) return 1.0F;

		float sx = baseScale[0];
		float sy = baseScale[1];
		float sz = baseScale[2];

		if (character.hasActiveForm()) {
			var form = character.getActiveFormData();
			if (form != null && form.getModelScaling() != null && form.getModelScaling().length >= 3) {
				sx *= form.getModelScaling()[0];
				sy *= form.getModelScaling()[1];
				sz *= form.getModelScaling()[2];
			}
		}

		if (character.hasActiveStackForm()) {
			var form = character.getActiveStackFormData();
			if (form != null && form.getModelScaling() != null && form.getModelScaling().length >= 3) {
				sx *= form.getModelScaling()[0];
				sy *= form.getModelScaling()[1];
				sz *= form.getModelScaling()[2];
			}
		}

		float uniform = (sx + sy + sz) / 3.0F;
		return Math.max(0.25F, Math.min(uniform, 8.0F));
	}
}