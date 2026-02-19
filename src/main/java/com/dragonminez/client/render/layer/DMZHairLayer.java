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

import java.util.*;

public class DMZHairLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {
	private final Map<Integer, Float> progressMap = new HashMap<>();
	private final Map<Integer, Long> tickMap = new HashMap<>();

    public DMZHairLayer(GeoRenderer<T> renderer) {
        super(renderer);

    }

	@Override
	public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		if(!bone.getName().contentEquals("head")) return;

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

		int entityId = animatable.getId();
		float lastHairProgress = progressMap.getOrDefault(entityId, 0.0f);
		long lastUpdateTick = tickMap.getOrDefault(entityId, 0L);

		if (character.hasActiveForm()) {
			hairFrom = getHairForForm(character, character.getActiveFormGroup(), character.getActiveForm());
			hairTo = hairFrom;
			colorFrom = getColorForForm(character, character.getActiveFormGroup(), character.getActiveForm());
			colorTo = colorFrom;
			factor = 1.0f;
			progressMap.put(entityId, 1.0f);
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
					tickMap.put(entityId, currentTick);
					progressMap.put(entityId, lastHairProgress);
				}

				float smoothProgress = Mth.lerp(partialTick * interpolationSpeed, lastHairProgress, targetProgress);
				smoothProgress = Math.max(0.0f, Math.min(1.0f, smoothProgress));

				CustomHair baseHair = character.getHairBase();
				CustomHair ssjHair = character.getHairSSJ();
				CustomHair ssj2Hair = character.getHairSSJ2();
				CustomHair ssj3Hair = character.getHairSSJ3();
				String baseColor = character.getHairColor();
				String actualFormColor = getColorForForm(character, targetGroup, character.getActiveForm());

				boolean targetIsSSJ3 = targetHair == ssj3Hair || (ssj3Hair != null && targetHair.equals(ssj3Hair));
				boolean targetIsSSJ2 = targetHair == ssj2Hair || (ssj2Hair != null && targetHair.equals(ssj2Hair));
				String targetForm = nextForm.getName().toLowerCase();

				if (ssjHair == null || ssjHair.isEmpty()) ssjHair = baseHair;
				if (ssj2Hair == null || ssj2Hair.isEmpty()) ssj2Hair = ssjHair;

				if (targetIsSSJ3) {
					if (smoothProgress < 0.33f) {
						hairFrom = baseHair;
						hairTo = ssjHair;
						colorFrom = baseColor;
						colorTo = targetColor;
						factor = smoothProgress * 3.0f;
					} else if (smoothProgress < 0.66f) {
						hairFrom = ssjHair;
						hairTo = ssj2Hair;
						colorFrom = actualFormColor;
						colorTo = targetColor;
						factor = (smoothProgress - 0.33f) * 3.0f;
					} else {
						hairFrom = ssj2Hair;
						hairTo = ssj3Hair;
						colorFrom = actualFormColor;
						colorTo = targetColor;
						factor = (smoothProgress - 0.66f) * 3.0f;
					}
				} else if (targetIsSSJ2) {
					if (smoothProgress < 0.5f) {
						hairFrom = baseHair;
						hairTo = ssjHair;
						colorFrom = baseColor;
						colorTo = targetColor;
						factor = smoothProgress * 2.0f;
					} else {
						hairFrom = ssjHair;
						hairTo = ssj2Hair;
						colorFrom = actualFormColor;
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
			progressMap.put(entityId, 0.0f);
		}

        int phase = stats.getStatus().getActiveKaiokenPhase();
        if (phase > 0) {
            colorFrom = applyKaiokenToHex(colorFrom, phase);
            colorTo = applyKaiokenToHex(colorTo, phase);
        }

		float alpha = 1.0f;
		if (animatable.isSpectator()) alpha = 0.15f;
		poseStack.pushPose();
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
					case "ssj2" -> { return character.getHairSSJ2(); }
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

    private String applyKaiokenToHex(String hexColor, int phase) {
        try {
            float[] rgb = ColorUtils.hexToRgb(hexColor);
            float intensity = Math.min(0.6f, phase * 0.1f);

            float r = rgb[0] * (1.0f - intensity) + (1.0f * intensity);
            float g = rgb[1] * (1.0f - intensity);
            float b = rgb[2] * (1.0f - intensity);

            return String.format("#%02x%02x%02x",
                    (int)(Mth.clamp(r, 0, 1) * 255),
                    (int)(Mth.clamp(g, 0, 1) * 255),
                    (int)(Mth.clamp(b, 0, 1) * 255));
        } catch (Exception e) {
            return hexColor;
        }
    }
}