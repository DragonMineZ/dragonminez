package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.compat.CosmeticArmorCompat;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.client.render.hair.HairRenderer;
import com.dragonminez.client.render.shader.TransformationMaskBufferSource;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DMZHairLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {
	private final Map<Integer, Float> progressMap = new HashMap<>();
	private final Map<Integer, CustomHair> fadeTargetHairMap = new HashMap<>();
	private final Map<Integer, float[]> fadeTargetRgbMap = new HashMap<>();
	private final Map<Integer, Boolean> fadeTargetForceMap = new HashMap<>();
	private final Map<Integer, Long> lastSeenMsMap = new HashMap<>();
	private final Map<Integer, Float> kiChargeProgressMap = new HashMap<>();

	private static final Map<Integer, float[]> PUBLISHED_BASE_COLOR = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> PUBLISHED_BASE_TIME = new ConcurrentHashMap<>();
	private static final double PHYSICS_LOD_NEAR_DISTANCE_SQR = 24.0 * 24.0;
	private static final double PHYSICS_LOD_FAR_DISTANCE_SQR = 48.0 * 48.0;
	private static final float FADE_OUT_RATE = 0.05f;
	private static final long TRACKING_TTL_MS = 30_000L;
	private static final long CLEANUP_INTERVAL_MS = 5_000L;
	private long lastCleanupMs = 0L;

	public DMZHairLayer(GeoRenderer<T> renderer) {
		super(renderer);
	}

	@Override
	public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		if (!bone.getName().contentEquals("head")) return;

		TransformationMaskBufferSource maskBuffer = null;
		if (bufferSource instanceof TransformationMaskBufferSource) {
			maskBuffer = (TransformationMaskBufferSource) bufferSource;
			maskBuffer.setMaskCaptureEnabled(true);
		}

		poseStack.pushPose();
		RenderUtils.translateToPivotPoint(poseStack, bone);
		renderHair(poseStack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
		bufferSource.getBuffer(renderType);
		poseStack.popPose();

		if (maskBuffer != null) {
			maskBuffer.setMaskCaptureEnabled(false);
		}
	}

	public void renderHair(PoseStack poseStack, T animatable, MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
		if (animatable.isInvisible() && !animatable.isSpectator()) return;
		if (FirstPersonManager.shouldRenderFirstPerson(animatable)) return;

		ItemStack headItem = resolveHeadArmorStack(animatable);
		if (!headItem.isEmpty()) {
			ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(headItem.getItem());
			if (itemId != null) {
				List<String> allowedHelmets = ConfigManager.getServerConfig().getGameplay().getHelmetsThatKeepHair();
				if (!allowedHelmets.contains(itemId.toString())) return;
			}
		}

		var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
		var stats = statsCap.orElse(new StatsData(animatable));
		Character character = stats.getCharacter();

		if (animatable.hasEffect(MainEffects.CANDY.get())) return;

		if (!HairManager.canUseHair(character)) return;

		CustomHair effectiveHair = HairManager.getEffectiveHair(character);
		if (effectiveHair == null || effectiveHair.isEmpty()) return;

		CustomHair hairFrom = character.getHairBase();
		float[] rgbFrom = character.getRgbHairColor();

		if (character.hasActiveForm()) {
			hairFrom = getHairForForm(character, character.getActiveFormGroup(), character.getActiveForm());
			rgbFrom = getRgbForForm(character, character.getActiveFormGroup(), character.getActiveForm());
			if (character.isOozaruCached()) return;
		}

		if (character.hasActiveStackForm()) {
			hairFrom = getHairForStackForm(character, character.getActiveStackFormGroup(), character.getActiveStackForm(), hairFrom);
			rgbFrom = getRgbForStackForm(character.getActiveStackFormGroup(), character.getActiveStackForm(), rgbFrom);
		}

		boolean overrideFrom = false;
		if (character.hasActiveForm() && character.getActiveFormData() != null && Boolean.TRUE.equals(character.getActiveFormData().hasHairColorOverride())) overrideFrom = true;
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && Boolean.TRUE.equals(character.getActiveStackFormData().hasHairColorOverride())) overrideFrom = true;

		CustomHair hairTo = hairFrom;
		float[] rgbTo = rgbFrom;
		float factor = 0.0f;
		boolean forceTo = overrideFrom;

		int entityId = animatable.getId();
		long nowMs = System.currentTimeMillis();
		long gameTime = animatable.level().getGameTime();
		lastSeenMsMap.put(entityId, nowMs);
		float curHairProgress = progressMap.getOrDefault(entityId, 0.0f);

		if (stats.getStatus().isActionCharging()) {
			String targetGroup;
			FormConfig.FormData nextForm = null;
			CustomHair targetHair = null;
			float[] targetRgb = null;
			int chargeMastery = 0;

			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				targetGroup = character.getSelectedFormGroup();
				nextForm = TransformationsHelper.getNextAvailableForm(stats);
				if (nextForm != null) {
					targetHair = getHairForForm(character, targetGroup, nextForm.getName());
					targetRgb = getRgbForForm(character, targetGroup, nextForm.getName());
					String masteryGroup = character.hasActiveForm() ? character.getActiveFormGroup() : targetGroup;
					chargeMastery = (int) character.getFormMasteries().getMastery(masteryGroup, nextForm.getName());
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				targetGroup = character.getSelectedStackFormGroup();
				nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				if (nextForm != null) {
					targetHair = getHairForStackForm(character, targetGroup, nextForm.getName(), hairFrom);
					targetRgb = getRgbForStackForm(targetGroup, nextForm.getName(), rgbFrom);
					String masteryGroup = character.hasActiveStackForm() ? character.getActiveStackFormGroup() : targetGroup;
					chargeMastery = (int) character.getStackFormMasteries().getMastery(masteryGroup, nextForm.getName());
				}
			}

			if (nextForm != null && targetHair != null && targetRgb != null) {
				int increment = 5 + Math.max(20, chargeMastery);
				float ratePerTick = increment / 2000.0f;
				float dt = Minecraft.getInstance().getDeltaFrameTime();
				curHairProgress = Math.min(1.0f, curHairProgress + ratePerTick * dt);
				progressMap.put(entityId, curHairProgress);

				hairTo = targetHair;
				rgbTo = nextForm.hasHairColorOverride() ? targetRgb : rgbFrom;
				forceTo = nextForm.hasHairColorOverride() || overrideFrom;
				fadeTargetHairMap.put(entityId, hairTo);
				fadeTargetRgbMap.put(entityId, rgbTo);
				fadeTargetForceMap.put(entityId, forceTo);
				factor = curHairProgress;
			}
		} else if (curHairProgress > 0.0f) {
				float dt = Minecraft.getInstance().getDeltaFrameTime();
				curHairProgress = Math.max(0.0f, curHairProgress - FADE_OUT_RATE * dt);
				CustomHair fadeTarget = fadeTargetHairMap.get(entityId);

				if (curHairProgress <= 0.0f || fadeTarget == null) {
					clearHairTracking(entityId);
				} else {
					progressMap.put(entityId, curHairProgress);
					float[] fadeRgb = fadeTargetRgbMap.get(entityId);
					hairTo = fadeTarget;
					rgbTo = fadeRgb != null ? fadeRgb : rgbFrom;
					forceTo = fadeTargetForceMap.getOrDefault(entityId, overrideFrom);
					factor = curHairProgress;
				}
			} else clearHairTracking(entityId);

		if (nowMs - lastCleanupMs >= CLEANUP_INTERVAL_MS) {
			cleanupStaleTracking(nowMs);
			lastCleanupMs = nowMs;
		}

		FormConfig.FormData tintForm = DMZSkinLayer.resolveTintForm(stats);
		float[] formTintColor = tintForm != null ? tintForm.getRgbTintColor() : null;
		float formTintIntensity = tintForm != null ? (float) tintForm.getTintIntensity() : 0.0f;
		boolean hasFormTint = formTintIntensity > 0.0f && formTintColor != null;
		boolean shouldFadeIn = stats.getStatus().isChargingKi() || stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura();
		float auraTintProgress = AuraTintTracker.update(entityId, gameTime, shouldFadeIn);

		if (hasFormTint || auraTintProgress > 0.0f) {
			float[] baseFrom = rgbFrom;
			float[] baseTo = rgbTo;
			rgbFrom = baseFrom.clone();
			rgbTo = baseTo == baseFrom ? rgbFrom : baseTo.clone();
		}

		if (hasFormTint) {
			applyFormTintToRgb(rgbFrom, formTintColor, formTintIntensity);
			applyFormTintToRgb(rgbTo, formTintColor, formTintIntensity);
		} else if (auraTintProgress > 0.0f) {
			float[] rgbAura = character.getRgbAuraColor();
			FormConfig.FormData activeFormData = character.hasActiveForm() ? character.getActiveFormData() : null;
			FormConfig.FormData activeStackFormData = character.hasActiveStackForm() ? character.getActiveStackFormData() : null;
			if (activeFormData != null && activeFormData.getRgbAuraColor() != null) rgbAura = activeFormData.getRgbAuraColor();
			if (activeStackFormData != null && activeStackFormData.getRgbAuraColor() != null) rgbAura = activeStackFormData.getRgbAuraColor();

			float intensity = 0.2f * auraTintProgress;
			applyAuraTintToRgb(rgbFrom, rgbAura, intensity);
			applyAuraTintToRgb(rgbTo, rgbAura, intensity);
		}

		float alpha = animatable.isSpectator() ? 0.15f : 1.0f;
		float physicsLodMultiplier = getPhysicsLodMultiplier(animatable);

		boolean isCharging = stats.getStatus().isChargingKi() || stats.getStatus().isPermanentAura() || (stats.getStatus().isActionCharging() && (!stats.getStatus().getSelectedAction().equals(ActionMode.STACK) && !stats.getStatus().getSelectedAction().equals(ActionMode.FORM)));
		float kiChargeProgress = kiChargeProgressMap.getOrDefault(entityId, 0.0f);
		float dt = Minecraft.getInstance().getDeltaFrameTime();

		if (isCharging) kiChargeProgress = Math.min(1.0f, kiChargeProgress + dt * 0.25f);
		else kiChargeProgress = Math.max(0.0f, kiChargeProgress - dt * 0.15f);

		if (kiChargeProgress > 0.0f) kiChargeProgressMap.put(entityId, kiChargeProgress);
		else kiChargeProgressMap.remove(entityId);

		publishHairBaseColor(entityId, gameTime, rgbFrom, rgbTo, factor);

		poseStack.pushPose();
		HairRenderer.render(poseStack, bufferSource, hairFrom, hairTo, factor, character, stats, animatable, rgbFrom, rgbTo, overrideFrom, forceTo, partialTick, packedLight, packedOverlay, alpha, physicsLodMultiplier, kiChargeProgress);
		poseStack.popPose();
	}

	private ItemStack resolveHeadArmorStack(T animatable) {
		ItemStack stack = animatable.getItemBySlot(EquipmentSlot.HEAD);
		if (CosmeticArmorCompat.isLoaded()) {
			ItemStack cosmeticStack = CosmeticArmorCompat.getCosmeticStack(animatable, EquipmentSlot.HEAD);
			if (cosmeticStack != null) return cosmeticStack;
		}
		return stack;
	}

	private float getPhysicsLodMultiplier(AbstractClientPlayer animatable) {
		var cameraEntity = Minecraft.getInstance().getCameraEntity();
		if (cameraEntity == null) return 1.0f;

		double distanceSqr = animatable.distanceToSqr(cameraEntity);
		if (distanceSqr <= PHYSICS_LOD_NEAR_DISTANCE_SQR) return 1.0f;
		if (distanceSqr >= PHYSICS_LOD_FAR_DISTANCE_SQR) return 0.0f;

		double t = (distanceSqr - PHYSICS_LOD_NEAR_DISTANCE_SQR) / (PHYSICS_LOD_FAR_DISTANCE_SQR - PHYSICS_LOD_NEAR_DISTANCE_SQR);
		return (float) (1.0 - t);
	}

	private void clearHairTracking(int entityId) {
		progressMap.remove(entityId);
		fadeTargetHairMap.remove(entityId);
		fadeTargetRgbMap.remove(entityId);
		fadeTargetForceMap.remove(entityId);
	}

	public static float[] getPublishedHairBaseColor(int entityId, long gameTime) {
		Long t = PUBLISHED_BASE_TIME.get(entityId);
		if (t == null || gameTime - t > 2L) return null;
		return PUBLISHED_BASE_COLOR.get(entityId);
	}

	private void publishHairBaseColor(int entityId, long gameTime, float[] rgbFrom, float[] rgbTo, float factor) {
		float smooth = factor;
		if (smooth > 0.0001f && smooth < 0.9999f) smooth = smooth * smooth * (3.0f - 2.0f * smooth);
		float[] color;
		if (smooth <= 0.0001f) color = rgbFrom.clone();
		else if (smooth >= 0.9999f) color = rgbTo.clone();
		else color = new float[]{Mth.lerp(smooth, rgbFrom[0], rgbTo[0]), Mth.lerp(smooth, rgbFrom[1], rgbTo[1]), Mth.lerp(smooth, rgbFrom[2], rgbTo[2])};
		PUBLISHED_BASE_COLOR.put(entityId, color);
		PUBLISHED_BASE_TIME.put(entityId, gameTime);
	}

	private void cleanupStaleTracking(long nowMs) {
		if (lastSeenMsMap.size() < 64) return;
		lastSeenMsMap.entrySet().removeIf(entry -> {
			boolean stale = nowMs - entry.getValue() > TRACKING_TTL_MS;
			if (stale) {
				clearHairTracking(entry.getKey());
			}
			return stale;
		});
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
			default -> character.emptyHair();
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

	private float[] getRgbForStackForm(String group, String formName, float[] fallback) {
		FormConfig config = ConfigManager.getStackFormGroup(group);
		if (config != null) {
			var formData = config.getForm(formName);
			if (formData != null && formData.getRgbHairColor() != null) {
				return formData.getRgbHairColor();
			}
		}
		return fallback;
	}

	private void applyFormTintToRgb(float[] rgb, float[] tint, float intensity) {
		intensity = Mth.clamp(intensity, 0.0f, 1.0f) * AuraTintTracker.darkTintScale(rgb);
		rgb[0] = Mth.clamp(rgb[0] * (1.0f - intensity) + tint[0] * intensity, 0, 1);
		rgb[1] = Mth.clamp(rgb[1] * (1.0f - intensity) + tint[1] * intensity, 0, 1);
		rgb[2] = Mth.clamp(rgb[2] * (1.0f - intensity) + tint[2] * intensity, 0, 1);
	}

	private void applyAuraTintToRgb(float[] rgb, float[] auraRgb, float intensity) {
		intensity *= AuraTintTracker.darkTintScale(rgb);
		rgb[0] = rgb[0] * (1.0f - intensity) + (auraRgb[0] * intensity);
		rgb[1] = rgb[1] * (1.0f - intensity) + (auraRgb[1] * intensity);
		rgb[2] = rgb[2] * (1.0f - intensity) + (auraRgb[2] * intensity);
	}
}