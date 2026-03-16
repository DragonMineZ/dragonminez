package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.client.render.hair.HairRenderer;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.Character;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.HashMap;
import java.util.Map;

public class DMZHairLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {
	private final Map<Integer, Float> progressMap = new HashMap<>();
	private final Map<Integer, Long> tickMap = new HashMap<>();

	public DMZHairLayer(GeoRenderer<T> renderer) {
		super(renderer);

	}

	@Override
	public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		if (!bone.getName().contentEquals("head")) return;

		poseStack.pushPose();
		RenderUtils.translateToPivotPoint(poseStack, bone);
		renderHair(poseStack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
		bufferSource.getBuffer(renderType);
		poseStack.popPose();
	}

	public void renderHair(PoseStack poseStack, T animatable, MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
		if (animatable.isInvisible() && !animatable.isSpectator()) return;
		if (FirstPersonManager.shouldRenderFirstPerson(animatable)) return;

		var headItem = animatable.getItemBySlot(EquipmentSlot.HEAD);
		if (!headItem.isEmpty() && !headItem.getItem().getDescriptionId().contains("pothala") && !headItem.getItem().getDescriptionId().contains("scouter") && !headItem.getItem().getDescriptionId().contains("invencible"))
			return;

		var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
		var stats = statsCap.orElse(new StatsData(animatable));
		Character character = stats.getCharacter();
		if (!HairManager.canUseHair(character)) return;

		CustomHair effectiveHair = HairManager.getEffectiveHair(character);
		if (effectiveHair == null || effectiveHair.isEmpty()) return;

		CustomHair baseHair = character.getHairBase();
		String baseColor = character.getHairColor();

		CustomHair formHair = null;
		String formColor = null;
		if (character.hasActiveForm()) {
			formHair = resolveHair(character, character.getRaceName(), character.getActiveFormGroup(), character.getActiveForm(), false);
			formColor = resolveColor(character.getRaceName(), character.getActiveFormGroup(), character.getActiveForm(), false);
			if (character.getActiveForm().toLowerCase().contains("ozaru")) return;
		}

		CustomHair stackHair = null;
		String stackColor = null;
		if (character.hasActiveStackForm()) {
			stackHair = resolveHair(character, character.getRaceName(), character.getActiveStackFormGroup(), character.getActiveStackForm(), true);
			stackColor = resolveColor(character.getRaceName(), character.getActiveStackFormGroup(), character.getActiveStackForm(), true);
		}

		CustomHair hairFrom = stackHair != null ? stackHair : (formHair != null ? formHair : baseHair);
		String colorFrom = stackColor != null ? stackColor : (formColor != null ? formColor : baseColor);
		boolean forceColorFrom = (stackColor != null || formColor != null);

		CustomHair hairTo = hairFrom;
		String colorTo = colorFrom;
		boolean forceColorTo = forceColorFrom;
		float factor = 0.0f;

		int entityId = animatable.getId();
		float lastHairProgress = progressMap.getOrDefault(entityId, 0.0f);
		long lastUpdateTick = tickMap.getOrDefault(entityId, 0L);

		if (stats.getStatus().isActionCharging()) {
			FormConfig.FormData nextForm = null;
			CustomHair targetHair = null;
			String targetColor = null;
			boolean targetForceColor = false;

			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				nextForm = TransformationsHelper.getNextAvailableForm(stats);
				if (nextForm != null) {
					CustomHair tFormHair = resolveHair(character, character.getRaceName(), character.getSelectedFormGroup(), nextForm.getName(), false);
					String tFormColor = resolveColor(character.getRaceName(), character.getSelectedFormGroup(), nextForm.getName(), false);

					targetHair = stackHair != null ? stackHair : (tFormHair != null ? tFormHair : baseHair);
					targetColor = stackColor != null ? stackColor : (tFormColor != null ? tFormColor : baseColor);
					targetForceColor = (stackColor != null || tFormColor != null);
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				if (nextForm != null) {
					CustomHair tStackHair = resolveHair(character, character.getRaceName(), character.getSelectedStackFormGroup(), nextForm.getName(), true);
					String tStackColor = resolveColor(character.getRaceName(), character.getSelectedStackFormGroup(), nextForm.getName(), true);

					targetHair = tStackHair != null ? tStackHair : (formHair != null ? formHair : baseHair);
					targetColor = tStackColor != null ? tStackColor : (formColor != null ? formColor : baseColor);
					targetForceColor = (tStackColor != null || formColor != null);
				}
			}

			if (nextForm != null && targetHair != null) {
				float targetProgress = stats.getResources().getActionCharge() / 100.0f;
				long currentTick = animatable.tickCount;
				float interpolationSpeed = 0.1f;

				if (currentTick != lastUpdateTick) {
					lastHairProgress = lastHairProgress + (targetProgress - lastHairProgress) * interpolationSpeed;
					tickMap.put(entityId, currentTick);
					progressMap.put(entityId, lastHairProgress);
				}

				float smoothProgress = Mth.lerp(partialTick * interpolationSpeed, lastHairProgress, targetProgress);
				smoothProgress = Math.max(0.0f, Math.min(1.0f, smoothProgress));

				hairTo = targetHair;
				colorTo = targetColor;
				forceColorTo = targetForceColor;
				factor = smoothProgress;
			}
		} else {
			progressMap.put(entityId, 0.0f);
		}

		int phase = TransformationsHelper.getKaiokenPhase(stats);
		if (phase > 0) {
			colorFrom = applyKaiokenToHex(colorFrom, phase);
			colorTo = applyKaiokenToHex(colorTo, phase);
			forceColorFrom = true;
			forceColorTo = true;
		}

		float alpha = 1.0f;
		if (animatable.isSpectator()) alpha = 0.15f;

		poseStack.pushPose();
		HairRenderer.render(poseStack, bufferSource, hairFrom, hairTo, factor, character, stats, animatable, colorFrom, colorTo, forceColorFrom, forceColorTo, partialTick, packedLight, packedOverlay, alpha);
		poseStack.popPose();
	}

	private CustomHair resolveHair(Character character, String race, String group, String formName, boolean isStack) {
		FormConfig config = isStack ? ConfigManager.getStackFormGroup(group) : ConfigManager.getFormGroup(race, group);
		if (config != null) {
			var formData = config.getForm(formName);
			if (formData != null) {
				if (formData.hasHairCodeOverride()) {
					String code = formData.getForcedHairCode();
					if (code != null && !code.trim().isEmpty()) {
						CustomHair override = HairManager.fromCode(code);
						if (override != null) return override;
					}
				}
				if (formData.hasDefinedHairType()) {
					String type = formData.getHairType();
					if (type != null && !type.trim().isEmpty()) {
						switch (type.toLowerCase()) {
							case "base":
								return character.getHairBase();
							case "ssj":
								return character.getHairSSJ();
							case "ssj2":
								return character.getHairSSJ2();
							case "ssj3":
								return character.getHairSSJ3();
						}
					}
				}
			}
		}
		return null;
	}

	private String resolveColor(String race, String group, String formName, boolean isStack) {
		FormConfig config = isStack ? ConfigManager.getStackFormGroup(group) : ConfigManager.getFormGroup(race, group);
		if (config != null) {
			var formData = config.getForm(formName);
			if (formData != null && formData.hasHairColorOverride()) {
				String color = formData.getHairColor();
				if (color != null && !color.trim().isEmpty()) return color;
			}
		}
		return null;
	}

	private String applyKaiokenToHex(String hexColor, int phase) {
		try {
			float[] rgb = ColorUtils.hexToRgb(hexColor);
			float intensity = Math.min(0.6f, phase * 0.1f);

			float r = rgb[0] * (1.0f - intensity) + (1.0f * intensity);
			float g = rgb[1] * (1.0f - intensity);
			float b = rgb[2] * (1.0f - intensity);

			return String.format("#%02x%02x%02x",
					(int) (Mth.clamp(r, 0, 1) * 255),
					(int) (Mth.clamp(g, 0, 1) * 255),
					(int) (Mth.clamp(b, 0, 1) * 255));
		} catch (Exception e) {
			return hexColor;
		}
	}
}