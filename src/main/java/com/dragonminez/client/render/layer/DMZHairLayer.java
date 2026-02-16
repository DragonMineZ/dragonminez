package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.client.render.hair.HairRenderer;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.Character;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DMZHairLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {

	private static float lastHairProgress = 0.0f;
	private static long lastUpdateTick = 0;

    public DMZHairLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		if (animatable.isInvisible() && !animatable.isSpectator()) return;
		if (FirstPersonManager.shouldRenderFirstPerson(animatable)) return;

        var headItem = animatable.getItemBySlot(EquipmentSlot.HEAD);
        if (!headItem.isEmpty() && !headItem.getItem().getDescriptionId().contains("pothala") && !headItem.getItem().getDescriptionId().contains("scouter") && !headItem.getItem().getDescriptionId().contains("invencible")) return;

        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
        var stats = statsCap.orElse(new StatsData(animatable));
        Character character = stats.getCharacter();
        if (!HairManager.canUseHair(character)) return;

        CustomHair effectiveHair = HairManager.getEffectiveHair(character);
        if (effectiveHair == null || effectiveHair.isEmpty()) return;

		CustomHair hairFrom = character.getHairBase();
		CustomHair hairTo = character.getHairBase();
		String colorFrom = character.getHairColor();
		String colorTo = character.getHairColor();
		float factor = 0.0f;

		if (character.hasActiveForm()) {
			hairFrom = getHairForForm(character, character.getActiveFormGroup(), character.getActiveForm());
			hairTo = hairFrom;
			colorFrom = getColorForForm(character, character.getActiveFormGroup(), character.getActiveForm());
			colorTo = colorFrom;
			factor = 1.0f;
			lastHairProgress = 1.0f;
			if (character.getActiveForm().contains("oozaru")) return;
		} else if (stats.getStatus().isActionCharging() && stats.getStatus().getSelectedAction() == ActionMode.FORM) {
			String targetGroup = character.getSelectedFormGroup();
			var nextForm = TransformationsHelper.getNextAvailableForm(stats);
			if (nextForm != null) {
				CustomHair targetHair = getHairForForm(character, targetGroup, nextForm.getName());
				String targetColor = getColorForForm(character, targetGroup, nextForm.getName());
				float targetProgress = stats.getResources().getActionCharge() / 100.0f;

				long currentTick = animatable.tickCount;
				float interpolationSpeed = 0.15f;

				if (currentTick != lastUpdateTick) {
					lastHairProgress = lastHairProgress + (targetProgress - lastHairProgress) * interpolationSpeed;
					lastUpdateTick = currentTick;
				}

				float smoothProgress = Mth.lerp(partialTick * interpolationSpeed, lastHairProgress, targetProgress);
				smoothProgress = Math.max(0.0f, Math.min(1.0f, smoothProgress));

				CustomHair baseHair = character.getHairBase();
				CustomHair ssjHair = character.getHairSSJ();
				CustomHair ssj3Hair = character.getHairSSJ3();
				String baseColor = character.getHairColor();

				boolean targetIsSSJ3 = targetHair == ssj3Hair || (ssj3Hair != null && targetHair.equals(ssj3Hair));

				if (targetIsSSJ3 && ssjHair != null && !ssjHair.isEmpty()) {
					String ssjColor = getColorForForm(character, targetGroup, "ssj");
					if (smoothProgress < 0.5f) {
						hairFrom = baseHair;
						hairTo = ssjHair;
						colorFrom = baseColor;
						colorTo = ssjColor;
						factor = smoothProgress * 2.0f;
					} else {
						hairFrom = ssjHair;
						hairTo = ssj3Hair;
						colorFrom = ssjColor;
						colorTo = targetColor;
						factor = (smoothProgress - 0.5f) * 2.0f;
					}
				} else {
					hairFrom = baseHair;
					hairTo = targetHair;
					colorFrom = baseColor;
					colorTo = targetColor;
					factor = smoothProgress;
				}
			}
		} else {
			lastHairProgress = 0.0f;
		}

		Optional<GeoBone> headBoneOpt = model.getBone("head");
		if (headBoneOpt.isEmpty()) return;
		GeoBone headBone = headBoneOpt.get();
		List<GeoBone> boneChain = new ArrayList<>();
		CoreGeoBone currentBone = headBone;
		while (currentBone != null) {
			boneChain.add((GeoBone) currentBone);
			currentBone = currentBone.getParent();
		}
		Collections.reverse(boneChain);

		poseStack.pushPose();

		float bodyYaw = Mth.lerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
		if (animatable.deathTime > 0) {
			float deathProgress = ((float)animatable.deathTime + partialTick - 1.0F) / 20.0F * 1.6F;
			deathProgress = Mth.sqrt(deathProgress);
			if (deathProgress > 1.0F) deathProgress = 1.0F;
			poseStack.mulPose(Axis.ZP.rotationDegrees(deathProgress * 90.0F));
		}
		float modelScale = 0.0625f;

		double parentPivotX = 0;
		double parentPivotY = 0;
		double parentPivotZ = 0;

		for (CoreGeoBone bone : boneChain) {
			if (bone instanceof GeoBone geoBone) {
				float dx = (float) ((geoBone.getPivotX() - parentPivotX) * modelScale);
				float dy = (float) ((geoBone.getPivotY() - parentPivotY) * modelScale);
				float dz = (float) ((geoBone.getPivotZ() - parentPivotZ) * modelScale);
				poseStack.translate(dx, dy, dz);
				poseStack.translate(geoBone.getPosX() * modelScale, geoBone.getPosY() * modelScale, geoBone.getPosZ() * modelScale);
				poseStack.mulPose(Axis.ZP.rotation(geoBone.getRotZ()));
				poseStack.mulPose(Axis.YP.rotation(geoBone.getRotY()));
				poseStack.mulPose(Axis.XP.rotation(geoBone.getRotX()));
				RenderUtils.scaleMatrixForBone(poseStack, geoBone);
				parentPivotX = geoBone.getPivotX();
				parentPivotY = geoBone.getPivotY();
				parentPivotZ = geoBone.getPivotZ();
			}
		}

		float alpha = 1.0f;
		if (animatable.isSpectator()) alpha = 0.15f;

		poseStack.translate(0, 0, 0);
		HairRenderer.render(poseStack, bufferSource, hairFrom, hairTo, factor, character, stats, animatable, colorFrom, colorTo, partialTick, packedLight, packedOverlay, alpha);
		poseStack.popPose();
	}

	private CustomHair getHairForForm(Character character, String group, String formName) {
		FormConfig config = ConfigManager.getFormGroup(character.getRaceName(), group);
		if (config != null) {
			var formData = config.getForm(formName);
			if (formData != null && formData.hasHairCodeOverride()) {
				CustomHair override = HairManager.fromCode(formData.getForcedHairCode());
				if (override != null) return override;
			} else if (formData != null && formData.hasDefinedHairType()) {
				switch (formData.getHairType().toLowerCase()) {
					case "base" -> { return character.getHairBase(); }
					case "ssj" -> { return character.getHairSSJ(); }
					case "ssj3" -> { return character.getHairSSJ3(); }
					default -> {}
				}
			}
		}

		return character.getHairBase();
	}

	private String getColorForForm(Character character, String group, String formName) {
		FormConfig config = ConfigManager.getFormGroup(character.getRaceName(), group);
		if (config != null) {
			var formData = config.getForm(formName);
			if (formData != null && formData.hasHairColorOverride()) {
				return formData.getHairColor();
			}
		}
		return character.getHairColor();
	}
}