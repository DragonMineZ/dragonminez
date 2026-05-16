package com.dragonminez.client.render.layer;

import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.render.util.WeaponGripProfile;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.logic.player.PlayerAttackProperties;
import com.dragonminez.common.combat.player.AttackHand;
import com.dragonminez.common.combat.logic.weapon.WeaponRegistry;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
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
		boolean isCombatAnim = animatable instanceof IPlayerAnimatable playerAnim && playerAnim.dragonminez$isPlayingCombatAnimation();
		if (isCombatAnim && animatable instanceof PlayerAttackProperties props) {
			AttackHand hand = PlayerAttackHelper.getCurrentAttack(animatable, props.getComboCount());
			if (hand == null) isCombatAnim = false;
		}
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
		boolean isCombatAnim = animatable instanceof IPlayerAnimatable playerAnim && playerAnim.dragonminez$isPlayingCombatAnimation();
		if (isCombatAnim && animatable instanceof PlayerAttackProperties props) {
			AttackHand hand = PlayerAttackHelper.getCurrentAttack(animatable, props.getComboCount());
			if (hand == null) isCombatAnim = false;
		}
		if (isCombatAnim) {
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

		boolean boneIsOffhand = (isRight && animatable.getMainArm() == HumanoidArm.LEFT) || (isLeft && animatable.getMainArm() == HumanoidArm.RIGHT);

		boolean isCombatAnim = false;
		boolean isOffhandAttack = false;

		if (animatable instanceof IPlayerAnimatable playerAnim) {
			isCombatAnim = playerAnim.dragonminez$isPlayingCombatAnimation();
			isOffhandAttack = playerAnim.dragonminez$isAttackingWithOffhand();
		}
		if (isCombatAnim && animatable instanceof PlayerAttackProperties props) {
			AttackHand hand = PlayerAttackHelper.getCurrentAttack(animatable, props.getComboCount());
			if (hand == null) isCombatAnim = false;
		}

		boolean cancelGripForThisBone = isCombatAnim && (boneIsOffhand == isOffhandAttack);

		if (!cancelGripForThisBone) {
			boolean isUsing = animatable.isUsingItem() && animatable.getUseItem() == stack;
			String weaponType = resolveWeaponType(stack);
			WeaponGripProfile profile = WeaponGripProfile.resolve(stack.getItem(), isUsing, weaponType);
			profile.apply(poseStack, isLeft);
		}

		float itemScale = resolveItemScale(animatable);
		if (itemScale != 1.0F) poseStack.scale(itemScale, itemScale, itemScale);

		super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
		poseStack.popPose();
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
