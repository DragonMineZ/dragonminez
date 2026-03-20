package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.client.render.hair.HairRenderer;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.ActionMode;
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

		CustomHair hairFrom = character.getHairBase();
		float[] rgbFrom = character.getRgbHairColor().clone();

		if (character.hasActiveForm()) {
			hairFrom = getHairForForm(character, character.getActiveFormGroup(), character.getActiveForm());
			rgbFrom = getRgbForForm(character, character.getActiveFormGroup(), character.getActiveForm()).clone();
			if (character.isOozaruCached()) return;
		}

		if (character.hasActiveStackForm()) {
			hairFrom = getHairForStackForm(character, character.getActiveStackFormGroup(), character.getActiveStackForm(), hairFrom);
			rgbFrom = getRgbForStackForm(character, character.getActiveStackFormGroup(), character.getActiveStackForm(), rgbFrom).clone();
		}

		CustomHair hairTo = hairFrom;
		float[] rgbTo = rgbFrom.clone();
		float factor = 0.0f;

		int entityId = animatable.getId();
		float lastHairProgress = progressMap.getOrDefault(entityId, 0.0f);
		long lastUpdateTick = tickMap.getOrDefault(entityId, 0L);

		if (stats.getStatus().isActionCharging()) {
			String targetGroup;
			FormConfig.FormData nextForm = null;
			CustomHair targetHair = null;
			float[] targetRgb = null;

			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				targetGroup = character.getSelectedFormGroup();
				nextForm = TransformationsHelper.getNextAvailableForm(stats);
				if (nextForm != null) {
					targetHair = getHairForForm(character, targetGroup, nextForm.getName());
					targetRgb = getRgbForForm(character, targetGroup, nextForm.getName());
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				targetGroup = character.getSelectedStackFormGroup();
				nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				if (nextForm != null) {
					targetHair = getHairForStackForm(character, targetGroup, nextForm.getName(), hairFrom);
					targetRgb = getRgbForStackForm(character, targetGroup, nextForm.getName(), rgbFrom);
				}
			}

			if (nextForm != null && targetHair != null && targetRgb != null) {
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
				rgbTo = targetRgb.clone();
				factor = smoothProgress;
			}
		} else {
			progressMap.put(entityId, 0.0f);
		}

		int phase = TransformationsHelper.getKaiokenPhase(stats);
		if (phase > 0) {
			applyKaiokenToRgb(rgbFrom, phase);
			applyKaiokenToRgb(rgbTo, phase);
		} else {
			boolean isCharging = stats.getStatus().isChargingKi();
			if (isCharging || stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura()) {
				float[] rgbAura = character.getRgbAuraColor();
				if (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().getRgbAuraColor() != null) {
					rgbAura = character.getActiveFormData().getRgbAuraColor();
				}
				if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && character.getActiveStackFormData().getRgbAuraColor() != null) {
					rgbAura = character.getActiveStackFormData().getRgbAuraColor();
				}

				float intensity = 0.2f;
				applyAuraTintToRgb(rgbFrom, rgbAura, intensity);
				applyAuraTintToRgb(rgbTo, rgbAura, intensity);
			}
		}

		float alpha = animatable.isSpectator() ? 0.15f : 1.0f;

		poseStack.pushPose();
		HairRenderer.render(poseStack, bufferSource, hairFrom, hairTo, factor, character, stats, animatable, rgbFrom, rgbTo, partialTick, packedLight, packedOverlay, alpha);
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
				return resolveHairType(character, formData.getHairType());
			}
		}
		return character.getHairBase();
	}

	private CustomHair getHairForStackForm(Character character, String group, String formName, CustomHair fallback) {
		FormConfig config = ConfigManager.getStackFormGroup(group);
		if (config != null) {
			var formData = config.getForm(formName);
			if (formData != null && formData.hasHairCodeOverride()) {
				CustomHair override = HairManager.fromCode(formData.getForcedHairCode());
				if (override != null) return override;
			} else if (formData != null && formData.hasDefinedHairType()) {
				return resolveHairType(character, formData.getHairType());
			}
		}
		return fallback;
	}

	private CustomHair resolveHairType(Character character, String type) {
		return switch (type.toLowerCase()) {
			case "base" -> character.getHairBase();
			case "ssj" -> character.getHairSSJ();
			case "ssj2" -> character.getHairSSJ2();
			case "ssj3" -> character.getHairSSJ3();
			default -> character.getHairBase();
		};
	}

	private float[] getRgbForForm(Character character, String group, String formName) {
		FormConfig config = ConfigManager.getFormGroup(character.getRaceName(), group);
		if (config != null) {
			var formData = config.getForm(formName);
			if (formData != null && formData.getRgbHairColor() != null) {
				return formData.getRgbHairColor();
			}
		}
		return character.getRgbHairColor();
	}

	private float[] getRgbForStackForm(Character character, String group, String formName, float[] fallback) {
		FormConfig config = ConfigManager.getStackFormGroup(group);
		if (config != null) {
			var formData = config.getForm(formName);
			if (formData != null && formData.getRgbHairColor() != null) {
				return formData.getRgbHairColor();
			}
		}
		return fallback;
	}

	private void applyKaiokenToRgb(float[] rgb, int phase) {
		float intensity = Math.min(0.6f, phase * 0.1f);
		rgb[0] = Mth.clamp(rgb[0] * (1.0f - intensity) + (1.0f * intensity), 0, 1);
		rgb[1] = Mth.clamp(rgb[1] * (1.0f - intensity), 0, 1);
		rgb[2] = Mth.clamp(rgb[2] * (1.0f - intensity), 0, 1);
	}

	private void applyAuraTintToRgb(float[] rgb, float[] auraRgb, float intensity) {
		rgb[0] = rgb[0] * (1.0f - intensity) + (auraRgb[0] * intensity);
		rgb[1] = rgb[1] * (1.0f - intensity) + (auraRgb[1] * intensity);
		rgb[2] = rgb[2] * (1.0f - intensity) + (auraRgb[2] * intensity);
	}
}