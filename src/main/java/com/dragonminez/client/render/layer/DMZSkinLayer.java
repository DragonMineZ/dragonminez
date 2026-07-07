package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.TransformationMaskBufferSource;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.SkinGathererProvider;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.common.util.lists.BioAndroidForms;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.dragonminez.common.util.lists.SaiyanForms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DMZSkinLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {
	public static boolean PREVIEW_MODE = false;

	private static final Map<ResourceLocation, ResourceLocation> VALIDATED_TEXTURES_CACHE = new ConcurrentHashMap<>();
	private static final ResourceLocation BLANK_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");

	private static final float[] DARK_GRAY = ColorUtils.hexToRgb("#383838");

	private static final String[] ARMOR_BONES = {
			"armorHead", "armorBody", "armorBody2", "armorLeggingsBody",
			"armorRightArm", "armorLeftArm", "armorLeftLeg", "armorLeftBoot",
			"armorRightLeg", "armorRightBoot"
	};

	private float[] currentFormTintColor = null;
	private float currentFormTintIntensity = 0.0f;
	private float currentTintProgress = 0.0f;
	private float[] currentAuraColor = new float[]{1.0f, 1.0f, 1.0f};

	private static final String SSJ4_FUR_LAYER = "ssj4fur";

	private static final int WOUND_OPACITY_PASSES = 4;

	private float currentSsj4Alpha = 0.0f;
	private float[] currentSsj4Color = null;

	public DMZSkinLayer(GeoRenderer<T> entityRendererIn) {
		super(entityRendererIn);
	}

	@Override
	public void render(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

		var player = (AbstractClientPlayer) animatable;
		if (player == null) return;

		if (player.hasEffect(MainEffects.CANDY.get())) return;

		var statsCap = StatsProvider.get(StatsCapability.INSTANCE, player);
		var stats = statsCap.orElse(new StatsData(player));

		int playerId = player.getId();
		long gameTime = player.level().getGameTime();
		boolean shouldFadeIn = stats.getStatus().isChargingKi()
				|| stats.getStatus().isAuraActive()
				|| stats.getStatus().isPermanentAura();
		float tintProgress = AuraTintTracker.update(playerId, gameTime, shouldFadeIn);

		this.currentTintProgress = tintProgress;
		this.currentAuraColor = getTopAuraColor(stats);
		FormConfig.FormData tintForm = resolveTintForm(stats);
		this.currentFormTintIntensity = tintForm != null ? (float) tintForm.getTintIntensity() : 0.0f;
		this.currentFormTintColor = tintForm != null ? tintForm.getRgbTintColor() : null;

		Ssj4Overlay ssj4 = resolveSsj4Overlay(stats);
		float[] ssj4Color = ssj4 != null ? ssj4.color() : null;
		float ssj4Target = ssj4 != null ? ssj4.target() : 0.0f;

		float alpha = player.isSpectator() ? 0.15f : 1.0f;
		TransformationMaskBufferSource maskBuffer = bufferSource instanceof TransformationMaskBufferSource mask ? mask : null;

		List<BodyLayerFadeTracker.FadingLayer> fadingLayers = new ArrayList<>();
		SkinGathererProvider.BodyLayerSink geoConsumer = new SkinGathererProvider.BodyLayerSink() {
			@Override
			public void base(ResourceLocation texture, float[] color) {
				renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityCutoutNoCull(texture), color[0], color[1], color[2], 1.0f, partialTick, packedLight, packedOverlay, alpha, true);
			}

			@Override
			public void fading(String layerId, ResourceLocation texture, float[] color) {
				fadingLayers.add(new BodyLayerFadeTracker.FadingLayer(layerId, texture, color));
			}
		};

		SkinGathererProvider.INSTANCE.gatherBodyLayers(player, stats, partialTick, geoConsumer);
		if (ssj4 != null) {
			ResourceLocation furTex = getSafeTexture(SkinGathererProvider.getCachedTexture("textures/entity/races/humansaiyan/" + ssj4.key() + "_layer1.png"));
			fadingLayers.add(new BodyLayerFadeTracker.FadingLayer(SSJ4_FUR_LAYER, furTex, ssj4.color(), ssj4.target()));
		}
		SkinGathererProvider.INSTANCE.gatherAndroidLayers(player, stats, partialTick, geoConsumer);
		renderFadingBodyLayers(model, poseStack, animatable, bufferSource, playerId, gameTime, fadingLayers, partialTick, packedLight, packedOverlay, alpha);

		this.currentSsj4Alpha = PREVIEW_MODE ? ssj4Target : BodyLayerFadeTracker.getProgress(playerId, SSJ4_FUR_LAYER);
		this.currentSsj4Color = ssj4Color != null ? ssj4Color : BodyLayerFadeTracker.getColor(playerId, SSJ4_FUR_LAYER);

		if (maskBuffer != null) maskBuffer.setMaskCaptureEnabled(false);
		renderHair(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay, alpha);
		SkinGathererProvider.INSTANCE.gatherTattooLayers(player, stats, partialTick, geoConsumer);
		SkinGathererProvider.INSTANCE.gatherEffectLayers(player, stats, partialTick, geoConsumer);
		renderWounds(model, poseStack, animatable, bufferSource, player, partialTick, packedLight, packedOverlay, alpha);
		renderFace(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay, alpha);
		if (maskBuffer != null) maskBuffer.setMaskCaptureEnabled(true);

		bufferSource.getBuffer(renderType);
	}

	private float[] getTopAuraColor(StatsData stats) {
		var character = stats.getCharacter();
		float[] color = character.getRgbAuraColor();

		if (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().getAuraColor() != null && !character.getActiveFormData().getAuraColor().isEmpty()) {
			color = character.getActiveFormData().getRgbAuraColor();
		}

		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && character.getActiveStackFormData().getAuraColor() != null && !character.getActiveStackFormData().getAuraColor().isEmpty()) {
			color = character.getActiveStackFormData().getRgbAuraColor();
		}

		return color;
	}

	private void renderHair(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay, float alpha) {
		var character = stats.getCharacter();
		String raceName = character.getRaceName().toLowerCase();
		String currentForm = character.getActiveForm();
		int hairId = character.getHairId();

		if (!HairManager.canUseHair(character)) return;
		if (raceName.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU))) return;
		if (hairId == 5) return;
		if (hairId == 0 && character.getHairBase().getVisibleStrandCount() == 0) return;
		List<String> hairTypes = List.of("base", "ssj", "ssj2", "ssj3");

		float[] currentTint = character.getRgbHairColor();

		if (character.hasActiveForm() && character.getActiveFormData() != null) {
			if (character.getActiveFormData().hasDefinedHairType() && !hairTypes.contains(character.getActiveFormData().getHairType().toLowerCase())) return;
			if (!character.getActiveFormData().getHairColor().isEmpty()) {
				currentTint = character.getActiveFormData().getRgbHairColor();
			}
		}
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) {
			if (character.getActiveStackFormData().hasDefinedHairType() && !hairTypes.contains(character.getActiveStackFormData().getHairType().toLowerCase())) return;
			if (!character.getActiveStackFormData().getHairColor().isEmpty()) {
				currentTint = character.getActiveStackFormData().getRgbHairColor();
			}
		}

		float[] finalTint = currentTint;
		if (stats.getStatus().isActionCharging()) {
			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				var nextForm = TransformationsHelper.getNextAvailableForm(stats);
				if (nextForm != null && !nextForm.getHairColor().isEmpty()) {
					float chargeProgress = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					finalTint = lerpColor(chargeProgress, currentTint, nextForm.getRgbHairColor());
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				var nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				if (nextForm != null && !nextForm.getHairColor().isEmpty()) {
					float chargeProgress = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					finalTint = lerpColor(chargeProgress, currentTint, nextForm.getRgbHairColor());
				}
			}
		}

		float[] publishedHair = DMZHairLayer.getPublishedHairBaseColor(player.getId(), player.level().getGameTime());
		final float[] hairTint = publishedHair != null ? publishedHair : applyColorTint(finalTint, stats);

		model.getBone("head").ifPresent(headBone -> {
			float originalZ = headBone.getPosZ();
			float originalSX = headBone.getScaleX();
			float originalSY = headBone.getScaleY();
			float originalSZ = headBone.getScaleZ();

			float inflation = 0.006f;
			headBone.setPosZ(originalZ - inflation);
			headBone.setScaleX(originalSX + inflation);
			headBone.setScaleY(originalSY + inflation);
			headBone.setScaleZ(originalSZ + inflation);

			List<GeoBone> hiddenBones = hideAllTopLevelAndKeepHead(model, headBone);
			try {
				if (character.isRenderHairBase()) {
					renderColoredLayer(model, poseStack, animatable, bufferSource, "textures/entity/races/hair_base.png", hairTint, partialTick, packedLight, packedOverlay, alpha);
				}
			} finally {
				restoreHiddenBones(hiddenBones);
				headBone.setPosZ(originalZ);
				headBone.setScaleX(originalSX);
				headBone.setScaleY(originalSY);
				headBone.setScaleZ(originalSZ);
			}
		});
	}

	private void renderFace(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay, float alpha) {
		var character = stats.getCharacter();
		String raceName = character.getRaceName().toLowerCase();
		String currentForm = character.getActiveForm();
		int bodyType = character.getBodyType();

		String customModelValue = "";
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && character.getActiveStackFormData().hasCustomModel()) {
			customModelValue = character.getActiveStackFormData().getCustomModel().toLowerCase();
		} else if (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().hasCustomModel()) {
			customModelValue = character.getActiveFormData().getCustomModel().toLowerCase();
		}

		if (customModelValue.isEmpty()) {
			var raceConfig = ConfigManager.getRaceCharacter(raceName);
			if (raceConfig != null && raceConfig.getCustomModel() != null && !raceConfig.getCustomModel().isEmpty()) {
				customModelValue = raceConfig.getCustomModel().toLowerCase();
			}
		}

		final boolean isModelEmpty = customModelValue.isEmpty();
		final String finalFaceKey = isModelEmpty
				? (SkinGathererProvider.isBuiltInRace(raceName) ? raceName : "human")
				: customModelValue;

		boolean isOozaruForm = raceName.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU));
		if (isOozaruForm || finalFaceKey.equals("oozaru")) return;

		boolean isHumanoidModel = finalFaceKey.equals("human") || finalFaceKey.equals("saiyan") || finalFaceKey.contains("ssj4d") || finalFaceKey.contains("ssj4gt") || finalFaceKey.equals("buffed") || finalFaceKey.equals("4arms");
		if (isHumanoidModel && bodyType == 0) return;

		var raceConfig = ConfigManager.getRaceCharacter(raceName);
		if (raceConfig != null && Boolean.TRUE.equals(raceConfig.getUseVanillaSkin()) && bodyType == 0) return;

		model.getBone("head").ifPresent(headBone -> {
			float originalZ = headBone.getPosZ();
			float originalSX = headBone.getScaleX();
			float originalSY = headBone.getScaleY();
			float originalSZ = headBone.getScaleZ();

			float faceInflation = 0.002f;
			headBone.setPosZ(originalZ - faceInflation);
			headBone.setScaleX(originalSX + faceInflation);
			headBone.setScaleY(originalSY + faceInflation);
			headBone.setScaleZ(originalSZ + faceInflation);

			List<GeoBone> hiddenBones = hideAllTopLevelAndKeepHead(model, headBone);
			try {
				dispatchFaceRender(model, poseStack, animatable, bufferSource, stats, character, finalFaceKey, isModelEmpty, raceName, partialTick, packedLight, packedOverlay, alpha);
			} finally {
				restoreHiddenBones(hiddenBones);
				headBone.setPosZ(originalZ);
				headBone.setScaleX(originalSX);
				headBone.setScaleY(originalSY);
				headBone.setScaleZ(originalSZ);
			}
		});
	}

	private void dispatchFaceRender(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, StatsData stats, Character character, String faceKey, boolean isModelEmpty, String race, float pt, int pl, int po, float alpha) {

		float[] eye1 = character.getRgbEye1Color();
		float[] eye2 = character.getRgbEye2Color();
		float[] skin = character.getRgbBodyColor();
		float[] b2 = character.getRgbBodyColor2();
		float[] hair = character.getRgbHairColor();

		if (character.hasActiveForm() && character.getActiveFormData() != null) {
			var f = character.getActiveFormData();
			if (!f.getEye1Color().isEmpty()) eye1 = f.getRgbEye1Color();
			if (!f.getEye2Color().isEmpty()) eye2 = f.getRgbEye2Color();
			if (!f.getHairColor().isEmpty()) hair = f.getRgbHairColor();
			if (!f.getBodyColor1().isEmpty()) skin = f.getRgbBodyColor1();
			if (!f.getBodyColor2().isEmpty()) b2 = f.getRgbBodyColor2();
		}

		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) {
			var sf = character.getActiveStackFormData();
			if (!sf.getEye1Color().isEmpty()) eye1 = sf.getRgbEye1Color();
			if (!sf.getEye2Color().isEmpty()) eye2 = sf.getRgbEye2Color();
			if (!sf.getHairColor().isEmpty()) hair = sf.getRgbHairColor();
			if (!sf.getBodyColor1().isEmpty()) skin = sf.getRgbBodyColor1();
			if (!sf.getBodyColor2().isEmpty()) b2 = sf.getRgbBodyColor2();
		}

		if (stats.getStatus().isActionCharging()) {
			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				var nextForm = TransformationsHelper.getNextAvailableForm(stats);
				if (nextForm != null) {
					float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					if (!nextForm.getEye1Color().isEmpty())
						eye1 = lerpColor(factor, eye1, nextForm.getRgbEye1Color());
					if (!nextForm.getEye2Color().isEmpty())
						eye2 = lerpColor(factor, eye2, nextForm.getRgbEye2Color());
					if (!nextForm.getBodyColor1().isEmpty())
						skin = lerpColor(factor, skin, nextForm.getRgbBodyColor1());
					if (!nextForm.getBodyColor2().isEmpty())
						b2 = lerpColor(factor, b2, nextForm.getRgbBodyColor2());
					if (!nextForm.getHairColor().isEmpty())
						hair = lerpColor(factor, hair, nextForm.getRgbHairColor());
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				var nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				if (nextForm != null) {
					float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					if (!nextForm.getEye1Color().isEmpty())
						eye1 = lerpColor(factor, eye1, nextForm.getRgbEye1Color());
					if (!nextForm.getEye2Color().isEmpty())
						eye2 = lerpColor(factor, eye2, nextForm.getRgbEye2Color());
					if (!nextForm.getBodyColor1().isEmpty())
						skin = lerpColor(factor, skin, nextForm.getRgbBodyColor1());
					if (!nextForm.getBodyColor2().isEmpty())
						b2 = lerpColor(factor, b2, nextForm.getRgbBodyColor2());
					if (!nextForm.getHairColor().isEmpty())
						hair = lerpColor(factor, hair, nextForm.getRgbHairColor());
				}
			}
		}

		skin = applyColorTint(skin, stats);
		hair = applyColorTint(hair, stats);
		b2 = applyColorTint(b2, stats);

		String family = SkinGathererProvider.modelFamily(faceKey);

		if (family.equals("custom")) {
			var rConfig = ConfigManager.getRaceCharacter(race);
			if (rConfig != null && Boolean.TRUE.equals(rConfig.getIsLayered())) {
				renderCustomFace(model, poseStack, animatable, bufferSource, character, faceKey, race, eye1, eye2, skin, hair, pt, pl, po, alpha);
			}
			return;
		}

		switch (family) {
			case "human" ->
					renderHumanFace(model, poseStack, animatable, bufferSource, character, eye1, eye2, skin, hair, pt, pl, po, alpha);
			case "namekian" ->
					renderNamekianFace(model, poseStack, animatable, bufferSource, character, eye1, eye2, skin, pt, pl, po, alpha);
			case "frostdemon" ->
					renderFrostFace(model, poseStack, animatable, bufferSource, character, faceKey, isModelEmpty, family, eye1, eye2, skin, b2, pt, pl, po, alpha);
			case "bioandroid" ->
					renderBioFace(model, poseStack, animatable, bufferSource, character, faceKey, isModelEmpty, family, eye1, eye2, pt, pl, po, alpha);
			case "majin" ->
					renderMajinFace(model, poseStack, animatable, bufferSource, character, faceKey, eye1, eye2, skin, b2, pt, pl, po, alpha);
		}
	}

	private void renderCustomFace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, String faceKey, String race, float[] eye1, float[] eye2, float[] skin, float[] hair, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/" + race + "/faces/";
		String prefix = faceKey + "_";
		float[] white = {1.0f, 1.0f, 1.0f};

		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_" + character.getEyesType() + "_0.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_0_0.png")).getPath(), white, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_" + character.getEyesType() + "_1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_0_1.png")).getPath(), eye1, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_" + character.getEyesType() + "_2.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_0_2.png")).getPath(), eye2, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_" + character.getEyesType() + "_3.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_0_3.png")).getPath(), hair, pt, pl, po, alpha);

        renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "nose_" + character.getNoseType() + ".png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "nose_0.png")).getPath(), skin, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "mouth_" + character.getMouthType() + ".png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "mouth_0.png")).getPath(), skin, pt, pl, po, alpha);
	}

	private void renderHumanFace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, float[] eye1, float[] eye2, float[] skin, float[] hair, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/humansaiyan/faces/";
		String eyeBase = "humansaiyan_eye_" + character.getEyesType();
        var legendaryGroup = character.getActiveFormGroup().equals("legendaryforms");
		float[] white = {1.0f, 1.0f, 1.0f};

		boolean isMajin = animatable.hasEffect(MainEffects.MAJIN.get());

		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_0.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "humansaiyan_eye_0_0.png")).getPath(), white, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "humansaiyan_eye_0_1.png")).getPath(), eye1, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_2.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "humansaiyan_eye_0_2.png")).getPath(), eye2, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_3.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "humansaiyan_eye_0_3.png")).getPath(), hair, pt, pl, po, alpha);

		boolean isSsj3 = (character.hasActiveStackForm() && character.getActiveStackFormData() != null && character.getActiveStackFormData().getHairType().equalsIgnoreCase("ssj3")) ||
				(character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().getHairType().equalsIgnoreCase("ssj3"));
		if (isSsj3) renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "ssj3eyebrows_eye_" + character.getEyesType() + ".png", skin, pt, pl, po, alpha);

		String ssj4Eyes = folder + "ssj4_eyes_" + character.getEyesType() + ".png";
		if (isMajin) {
			renderColoredLayer(model, poseStack, animatable, bufferSource, ssj4Eyes, ColorUtils.hexToRgb("#292929"), pt, pl, po, alpha);
		} else if (this.currentSsj4Alpha > 0.001f && this.currentSsj4Color != null) {
			renderFadingColoredLayer(model, poseStack, animatable, bufferSource, ssj4Eyes, this.currentSsj4Color, pt, pl, po, alpha * this.currentSsj4Alpha);
		}

        if(legendaryGroup && (character.getActiveForm().equals("shiyoken") || character.getActiveForm().equals("shin_shiyoken") || character.getActiveForm().equals("chou_shiyoken"))){

            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "shiyoken_eye0.png", ColorUtils.hexToRgb("#FFFFFF"), pt, pl, po, alpha, true);
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "shiyoken_eye1.png", eye1, pt, pl, po, alpha, true);

        }

        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "humansaiyan_nose_" + character.getNoseType() + ".png", skin, pt, pl, po, alpha, false);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "humansaiyan_mouth_" + character.getMouthType() + ".png", skin, pt, pl, po, alpha, false);
	}

	private void renderNamekianFace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, float[] eye1, float[] eye2, float[] skin, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/namekian/faces/";
		String eyeBase = "namekian_eye_" + character.getEyesType();
        var bodytype = character.getBodyType();
        var hairColor = character.getRgbHairColor();
		float[] white = {1.0f, 1.0f, 1.0f};

		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_0.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "namekian_eye_0_0.png")).getPath(), white, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "namekian_eye_0_1.png")).getPath(), eye1, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_2.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "namekian_eye_0_2.png")).getPath(), eye2, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_3.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "namekian_eye_0_3.png")).getPath(), skin, pt, pl, po, alpha);

        if(bodytype == 1 || bodytype == 2) skin = hairColor;
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "namekian_nose_" + character.getNoseType() + ".png", skin, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "namekian_mouth_" + character.getMouthType() + ".png", skin, pt, pl, po, alpha);
	}

	private void renderFrostFace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, String faceKey, boolean isModelEmpty, String race, float[] eye1, float[] eye2, float[] skin, float[] b2, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/frostdemon/faces/";
		int bodyType = character.getBodyType();
		String currentForm = character.getActiveForm() != null ? character.getActiveForm().toLowerCase() : "";
		boolean isFifth = faceKey.equals("frostdemon_fifth") || currentForm.contains(FrostDemonForms.FIFTH_FORM);
        boolean isMetalCore = faceKey.equals("frostdemon_metalcore");

        float[] eyeBgColor = isFifth ? ColorUtils.hexToRgb("#D11A11") :
                (isMetalCore ? ColorUtils.hexToRgb("#242424") : ColorUtils.hexToRgb("#F2F2F2"));
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_0.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_0.png")).getPath(), eyeBgColor, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_1.png")).getPath(), eye1, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_2.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_2.png")).getPath(), eye2, pt, pl, po, alpha);

		if (isFifth) {
			renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "frostdemon_fifth_mouth.png", skin, pt, pl, po, alpha, true);
			return;
		}

		boolean isPrimitiveForm = !character.hasActiveForm() || currentForm.equals("second") || currentForm.equals("third");
		float[] finalDetailColor = (isPrimitiveForm && (faceKey.equals("frostdemon") || faceKey.equals("frostdemon_third") || faceKey.equals("frostdemon_second"))) ? b2 : (bodyType == 1 ? b2 : skin);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "frostdemon_nose_" + character.getNoseType() + ".png", finalDetailColor, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "frostdemon_mouth_" + character.getMouthType() + ".png", finalDetailColor, pt, pl, po, alpha);
	}

	private void renderBioFace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, String faceKey, boolean isModelEmpty, String race, float[] eye1, float[] eye2, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/bioandroid/faces/";
		String phase;
		String currentForm = character.getActiveForm() != null ? character.getActiveForm() : "";

		if (faceKey.equals("bioandroid_semi")) phase = "semiperfect";
		else if (faceKey.equals("bioandroid_perfect")) phase = "perfect";
		else if (faceKey.equals("bioandroid_base")) phase = "base";
		else if (faceKey.equals("bioandroid") && !isModelEmpty) phase = "base";
		else if (race.equals("bioandroid"))
			phase = (character.hasActiveForm() && currentForm.equals(BioAndroidForms.SEMI_PERFECT)) ? "semiperfect" : (character.hasActiveForm() ? "perfect" : "base");
		else phase = "base";

		String textureBase = folder + phase + "_eye_layer";

		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, textureBase + "0.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "base_eye_layer0.png")).getPath(), eye2, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, textureBase + "1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "base_eye_layer1.png")).getPath(), eye1, pt, pl, po, alpha);
	}

	private void renderMajinFace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, String faceKey, float[] eye1, float[] eye2, float[] skin, float[] b2, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/majin/faces/";

        if ("janemba_fat".equals(faceKey)) {
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "janemba_eye_0.png", DARK_GRAY, pt, pl, po, alpha);
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "janemba_eye_1.png", eye1, pt, pl, po, alpha);
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "janemba_mouth.png", skin, pt, pl, po, alpha);
            return;
        }

		if ("janemba_super".equals(faceKey)) {
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "majin_eye_2_0.png", eye1, pt, pl, po, alpha);
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "majin_eye_2_1.png", eye2, pt, pl, po, alpha);
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "majin_mouth_" + character.getMouthType() + ".png", skin, pt, pl, po, alpha);
            return;
        }

		int eyeType = character.getEyesType();
        var bodytype = character.getBodyType();
		float[] bgColor = eyeType == 0 ? skin : DARK_GRAY;
		float[] layer1Color = eyeType == 0 ? skin : eye1;
		String eyePath = folder + "majin_eye_" + eyeType + "_";

		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, eyePath + "0.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "majin_eye_0_0.png")).getPath(), bgColor, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, eyePath + "1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "majin_eye_0_1.png")).getPath(), layer1Color, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, eyePath + "2.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "majin_eye_0_2.png")).getPath(), skin, pt, pl, po, alpha);

        if(bodytype == 1) skin = b2;
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "majin_nose_" + character.getNoseType() + ".png", skin, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "majin_mouth_" + character.getMouthType() + ".png", skin, pt, pl, po, alpha);
	}

	private void renderLayerWholeModel(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource, T animatable, RenderType renderType, float r, float g, float b, float scaleInflation, float partialTick, int packedLight, int packedOverlay, float alpha, boolean applyTransformationTint) {
		if (applyTransformationTint) {
			float[] rgb = {r, g, b};
			tintInPlace(rgb);
			r = rgb[0];
			g = rgb[1];
			b = rgb[2];
		}

		poseStack.pushPose();
		if (scaleInflation > 1.0f) poseStack.scale(scaleInflation, scaleInflation, scaleInflation);

		List<GeoBone> hiddenArmors = new ArrayList<>();
		for (String armorBone : ARMOR_BONES) {
			model.getBone(armorBone).ifPresent(bone -> {
				if (!bone.isHidden()) {
					bone.setHidden(true);
					hiddenArmors.add(bone);
				}
			});
		}

		getRenderer().reRender(model, poseStack, bufferSource, animatable, renderType, bufferSource.getBuffer(renderType), partialTick, packedLight, packedOverlay, r, g, b, alpha);

		for (GeoBone bone : hiddenArmors) bone.setHidden(false);

		poseStack.popPose();
	}

	private void renderColoredLayer(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, String path, float[] rgb, float partialTick, int packedLight, int packedOverlay, float alpha) {
		renderColoredLayer(model, poseStack, animatable, bufferSource, path, rgb, partialTick, packedLight, packedOverlay, alpha, false);
	}

	private void renderColoredLayer(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, String path, float[] rgb, float partialTick, int packedLight, int packedOverlay, float alpha, boolean applyTransformationTint) {
		ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path);
		renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityCutoutNoCull(getSafeTexture(loc)), rgb[0], rgb[1], rgb[2], 1.0f, partialTick, packedLight, packedOverlay, alpha, applyTransformationTint);
	}

	public record Ssj4Overlay(String key, float[] color, float target) {}

	public static Ssj4Overlay resolveSsj4Overlay(StatsData stats) {
		var character = stats.getCharacter();

		String activeModel = "";
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && Boolean.TRUE.equals(character.getActiveStackFormData().hasCustomModel())) {
			activeModel = character.getActiveStackFormData().getCustomModel().toLowerCase();
		} else if (character.hasActiveForm() && character.getActiveFormData() != null && Boolean.TRUE.equals(character.getActiveFormData().hasCustomModel())) {
			activeModel = character.getActiveFormData().getCustomModel().toLowerCase();
		}
		boolean activeSsj4 = isSsj4Model(activeModel);

		boolean chargingSsj4 = false;
		String targetModel = "";
		float chargeFraction = 0.0f;
		FormConfig.FormData nextForm = null;
		if (stats.getStatus().isActionCharging()) {
			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) nextForm = TransformationsHelper.getNextAvailableForm(stats);
			else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) nextForm = TransformationsHelper.getNextAvailableStackForm(stats);

			if (nextForm != null && Boolean.TRUE.equals(nextForm.hasCustomModel())) {
				targetModel = nextForm.getCustomModel().toLowerCase();
				chargingSsj4 = isSsj4Model(targetModel);
				chargeFraction = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
			}
		}

		if (!activeSsj4 && !chargingSsj4) return null;

		String key = activeSsj4 ? activeModel.contains("ssj4gt") ? "ssj4gt" : "ssj4d" : targetModel.contains("ssj4gt") ? "ssj4gt" : "ssj4d";
		float target = activeSsj4 ? 1.0f : chargeFraction;
		float[] color = resolveSsj4OverlayColor(character, chargingSsj4 ? nextForm : null, chargeFraction);
		return new Ssj4Overlay(key, color, target);
	}

	private static boolean isSsj4Model(String model) {
		return model.contains("ssj4d") || model.contains("ssj4gt");
	}

	private static float[] resolveSsj4OverlayColor(Character character, FormConfig.FormData chargeTarget, float chargeFraction) {
		float[] b2 = character.getRgbBodyColor2();
		if (character.hasActiveForm() && character.getActiveFormData() != null && !character.getActiveFormData().getBodyColor2().isEmpty()) {
			b2 = character.getActiveFormData().getRgbBodyColor2();
		}
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && !character.getActiveStackFormData().getBodyColor2().isEmpty()) {
			b2 = character.getActiveStackFormData().getRgbBodyColor2();
		}
		if (chargeTarget != null && chargeTarget.getRgbBodyColor2() != null) {
			b2 = lerpColor(chargeFraction, b2, chargeTarget.getRgbBodyColor2());
		}
		return b2;
	}

	private void renderFadingBodyLayers(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, int entityId, long gameTime, List<BodyLayerFadeTracker.FadingLayer> activeLayers, float partialTick, int packedLight, int packedOverlay, float baseAlpha) {
		List<BodyLayerFadeTracker.RenderEntry> toRender;
		if (PREVIEW_MODE) {
			toRender = new ArrayList<>();
			for (BodyLayerFadeTracker.FadingLayer l : activeLayers) toRender.add(new BodyLayerFadeTracker.RenderEntry(l.texture(), l.color(), l.target()));
		} else {
			toRender = BodyLayerFadeTracker.update(entityId, gameTime, activeLayers);
		}

		for (BodyLayerFadeTracker.RenderEntry entry : toRender) {
			float a = baseAlpha * entry.alpha();
			if (a <= 0.001f) continue;
			float[] color = entry.color();
			RenderType renderType = a < 1.0f ? RenderType.entityTranslucent(entry.texture()) : RenderType.entityCutoutNoCull(entry.texture());
			renderLayerWholeModel(model, poseStack, bufferSource, animatable, renderType, color[0], color[1], color[2], 1.0f, partialTick, packedLight, packedOverlay, a, true);
		}
	}

	private void renderWounds(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, AbstractClientPlayer player, float partialTick, int packedLight, int packedOverlay, float alpha) {
		float maxHealth = player.getMaxHealth();
		if (maxHealth <= 0.0f) return;
		float healthRatio = Mth.clamp(player.getHealth() / maxHealth, 0.0f, 1.0f);

		float woundsAlpha = Mth.clamp((0.75f - healthRatio) / (0.75f - 0.50f), 0.0f, 1.0f);
		float grievousAlpha = Mth.clamp((0.40f - healthRatio) / (0.40f - 0.20f), 0.0f, 1.0f);

		if (woundsAlpha > 0.001f) {
			renderWoundLayer(model, poseStack, animatable, bufferSource, "textures/entity/races/wounds.png", partialTick, packedLight, packedOverlay, alpha * woundsAlpha);
		}
		if (grievousAlpha > 0.001f) {
			renderWoundLayer(model, poseStack, animatable, bufferSource, "textures/entity/races/grievous_wounds.png", partialTick, packedLight, packedOverlay, alpha * grievousAlpha);
		}
	}

	private void renderWoundLayer(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, String path, float partialTick, int packedLight, int packedOverlay, float alpha) {
		ResourceLocation loc = getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path));
		RenderType renderType = RenderType.entityTranslucent(loc);
		for (int pass = 0; pass < WOUND_OPACITY_PASSES; pass++) {
			renderLayerWholeModel(model, poseStack, bufferSource, animatable, renderType, 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight, packedOverlay, alpha, false);
		}
	}

	private void renderFadingColoredLayer(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, String path, float[] rgb, float partialTick, int packedLight, int packedOverlay, float alpha) {
		ResourceLocation loc = getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path));
		RenderType renderType = alpha < 1.0f ? RenderType.entityTranslucent(loc) : RenderType.entityCutoutNoCull(loc);
		renderLayerWholeModel(model, poseStack, bufferSource, animatable, renderType, rgb[0], rgb[1], rgb[2], 1.0f, partialTick, packedLight, packedOverlay, alpha, false);
	}

	public static FormConfig.FormData resolveTintForm(StatsData stats) {
		var character = stats.getCharacter();
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && character.getActiveStackFormData().hasTint()) {
			return character.getActiveStackFormData();
		}
		if (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().hasTint()) {
			return character.getActiveFormData();
		}
		return null;
	}

	private float[] applyColorTint(float[] rgb, StatsData stats) {
		if (rgb == null || rgb.length < 3) return rgb;
		if (this.currentFormTintIntensity <= 0.0f && this.currentTintProgress <= 0.0f) return rgb;

		float[] tinted = rgb.clone();
		tintInPlace(tinted);
		return tinted;
	}

	private void tintInPlace(float[] rgb) {
		if (this.currentFormTintIntensity > 0.0f && this.currentFormTintColor != null) {
			applyFormTintToRgb(rgb, this.currentFormTintColor, this.currentFormTintIntensity);
		} else if (this.currentTintProgress > 0.0f) {
			applyAuraTintToRgb(rgb, this.currentAuraColor, 0.2f * this.currentTintProgress);
		}
	}

	private void applyFormTintToRgb(float[] rgb, float[] tint, float intensity) {
		intensity = Mth.clamp(intensity, 0.0f, 1.0f) * AuraTintTracker.darkTintScale(rgb);
		rgb[0] = Mth.clamp(rgb[0] * (1.0f - intensity) + tint[0] * intensity, 0.0f, 1.0f);
		rgb[1] = Mth.clamp(rgb[1] * (1.0f - intensity) + tint[1] * intensity, 0.0f, 1.0f);
		rgb[2] = Mth.clamp(rgb[2] * (1.0f - intensity) + tint[2] * intensity, 0.0f, 1.0f);
	}

	private void applyAuraTintToRgb(float[] rgb, float[] auraRgb, float intensity) {
		intensity *= AuraTintTracker.darkTintScale(rgb);
		rgb[0] = rgb[0] * (1.0f - intensity) + (auraRgb[0] * intensity);
		rgb[1] = rgb[1] * (1.0f - intensity) + (auraRgb[1] * intensity);
		rgb[2] = rgb[2] * (1.0f - intensity) + (auraRgb[2] * intensity);
	}

	private void unhideParents(GeoBone bone) {
		GeoBone parent = bone.getParent();
		while (parent != null) {
			parent.setHidden(false);
			parent = parent.getParent();
		}
	}

	private List<GeoBone> hideAllTopLevelAndKeepHead(BakedGeoModel model, GeoBone headBone) {
		List<GeoBone> hiddenBones = new ArrayList<>();
		for (GeoBone bone : model.topLevelBones()) {
			if (!bone.isHidden()) {
				hiddenBones.add(bone);
				bone.setHidden(true);
			}
		}
		headBone.setHidden(false);
		unhideParents(headBone);
		return hiddenBones;
	}

	private void restoreHiddenBones(List<GeoBone> hiddenBones) {
		for (GeoBone hiddenBone : hiddenBones) {
			hiddenBone.setHidden(false);
		}
	}

	public static float[] lerpColor(float factor, float[] current, float[] target) {
		return new float[]{
				Mth.lerp(factor, current[0], target[0]),
				Mth.lerp(factor, current[1], target[1]),
				Mth.lerp(factor, current[2], target[2])
		};
	}

	public static ResourceLocation getSafeTexture(ResourceLocation originalLoc) {
		return VALIDATED_TEXTURES_CACHE.computeIfAbsent(originalLoc, loc -> {
			System.out.println("Validating texture: " + loc);
			if (Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()) return loc;
			return BLANK_TEXTURE;
		});
	}

	public static ResourceLocation getSafeTexture(ResourceLocation originalLoc, ResourceLocation fallbackLoc) {
		return VALIDATED_TEXTURES_CACHE.computeIfAbsent(originalLoc, loc -> {
			if (Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()) return loc;
			return fallbackLoc;
		});
	}

	public static void clearValidatedTexturesCache() {
		VALIDATED_TEXTURES_CACHE.clear();
	}
}