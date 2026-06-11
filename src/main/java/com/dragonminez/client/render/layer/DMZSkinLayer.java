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
import java.util.function.BiConsumer;

public class DMZSkinLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {
	private static final Map<ResourceLocation, ResourceLocation> VALIDATED_TEXTURES_CACHE = new ConcurrentHashMap<>();
	private static final ResourceLocation BLANK_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");

	private static final float[] DARK_GRAY = ColorUtils.hexToRgb("#383838");

	private static final String[] ARMOR_BONES = {
			"armorHead", "armorBody", "armorBody2", "armorLeggingsBody",
			"armorRightArm", "armorLeftArm", "armorLeftLeg", "armorLeftBoot",
			"armorRightLeg", "armorRightBoot"
	};

	private int currentKaiokenPhase = 0;
	private float currentTintProgress = 0.0f;
	private float[] currentAuraColor = new float[]{1.0f, 1.0f, 1.0f};

	private float currentSsj4Alpha = 0.0f;
	private String currentSsj4Key = null;
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
		this.currentKaiokenPhase = TransformationsHelper.getKaiokenPhase(stats);

		Ssj4Overlay ssj4 = resolveSsj4Overlay(stats);
		String ssj4Key = ssj4 != null ? ssj4.key() : null;
		float[] ssj4Color = ssj4 != null ? ssj4.color() : null;
		float ssj4Target = ssj4 != null ? ssj4.target() : 0.0f;
		this.currentSsj4Alpha = Ssj4FadeTracker.update(playerId, gameTime, ssj4Target, ssj4Key, ssj4Color);
		this.currentSsj4Key = ssj4Key != null ? ssj4Key : Ssj4FadeTracker.lastKey(playerId);
		this.currentSsj4Color = ssj4Color != null ? ssj4Color : Ssj4FadeTracker.lastColor(playerId);

		float alpha = player.isSpectator() ? 0.15f : 1.0f;
		TransformationMaskBufferSource maskBuffer = bufferSource instanceof TransformationMaskBufferSource mask ? mask : null;

		BiConsumer<ResourceLocation, float[]> geoConsumer = (texture, color) -> renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityCutoutNoCull(texture), color[0], color[1], color[2], 1.0f, partialTick, packedLight, packedOverlay, alpha, true);

		SkinGathererProvider.INSTANCE.gatherBodyLayers(player, stats, partialTick, geoConsumer);
		renderSsj4Fur(model, poseStack, animatable, bufferSource, partialTick, packedLight, packedOverlay, alpha);
		SkinGathererProvider.INSTANCE.gatherAndroidLayers(player, stats, partialTick, geoConsumer);

		if (maskBuffer != null) maskBuffer.setMaskCaptureEnabled(false);
		renderHair(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay, alpha);
		SkinGathererProvider.INSTANCE.gatherTattooLayers(player, stats, partialTick, geoConsumer);
		renderFace(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay, alpha);
		if (maskBuffer != null) maskBuffer.setMaskCaptureEnabled(true);
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

		final float[] hairTint = applyColorTint(finalTint, stats);

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
        final String finalFaceKey = isModelEmpty ? raceName : customModelValue;

        boolean isOozaruForm = raceName.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU));
        if (isOozaruForm || finalFaceKey.equals("oozaru")) return;
        boolean isHumanoidModel = finalFaceKey.equals("human") || finalFaceKey.equals("saiyan") || finalFaceKey.equals("ssj4d") || finalFaceKey.equals("ssj4gt") || finalFaceKey.equals("buffed") || finalFaceKey.equals("4arms");
        if (isHumanoidModel && bodyType == 0) return;

        model.getBone("head").ifPresent(headBone -> {
            List<GeoBone> hiddenBones = hideAllTopLevelAndKeepHead(model, headBone);
            try {
                dispatchFaceRender(model, poseStack, animatable, bufferSource, stats, character, finalFaceKey, isModelEmpty, raceName, partialTick, packedLight, packedOverlay, alpha);
            } finally {
                restoreHiddenBones(hiddenBones);
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

		if (!faceKey.equals("human") && !faceKey.equals("saiyan") && !faceKey.equals("ssj4d") && !faceKey.equals("ssj4gt") && !faceKey.equals("buffed")
				&& !faceKey.equals("namekian") && !faceKey.equals("namekian_orange") && !faceKey.equals("namekian_buffed")
				&& !faceKey.startsWith("frostdemon") && !faceKey.startsWith("bioandroid")
				&& !faceKey.startsWith("majin") && !faceKey.startsWith("janemba_super") && !faceKey.startsWith("janemba_fat")
                && !faceKey.startsWith("4arms")) {

			var rConfig = ConfigManager.getRaceCharacter(race);
			if (rConfig != null && Boolean.TRUE.equals(rConfig.getIsLayered())) {
				renderCustomFace(model, poseStack, animatable, bufferSource, character, faceKey, race, eye1, eye2, skin, hair, pt, pl, po, alpha);
			}
			return;
		}

		switch (race) {
			case "human", "saiyan" ->
					renderHumanFace(model, poseStack, animatable, bufferSource, character, eye1, eye2, skin, hair, pt, pl, po, alpha);
			case "namekian" ->
					renderNamekianFace(model, poseStack, animatable, bufferSource, character, eye1, eye2, skin, pt, pl, po, alpha);
			case "frostdemon" ->
					renderFrostFace(model, poseStack, animatable, bufferSource, character, faceKey, isModelEmpty, race, eye1, eye2, skin, b2, pt, pl, po, alpha);
			case "bioandroid" ->
					renderBioFace(model, poseStack, animatable, bufferSource, character, faceKey, isModelEmpty, race, eye1, eye2, pt, pl, po, alpha);
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

        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "humansaiyan_nose_" + character.getNoseType() + ".png", skin, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "humansaiyan_mouth_" + character.getMouthType() + ".png", skin, pt, pl, po, alpha);
	}

	private void renderNamekianFace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, float[] eye1, float[] eye2, float[] skin, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/namekian/faces/";
		String eyeBase = "namekian_eye_" + character.getEyesType();
		float[] white = {1.0f, 1.0f, 1.0f};

		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_0.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "namekian_eye_0_0.png")).getPath(), white, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "namekian_eye_0_1.png")).getPath(), eye1, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_2.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "namekian_eye_0_2.png")).getPath(), eye2, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_3.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "namekian_eye_0_3.png")).getPath(), skin, pt, pl, po, alpha);

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
			float intensity;
			if (this.currentKaiokenPhase > 0) intensity = Math.min(0.6f, this.currentKaiokenPhase * 0.1f);
			else intensity = 0.2f * this.currentTintProgress;

			if (intensity > 0.001f) {
				r = r * (1.0f - intensity) + (this.currentAuraColor[0] * intensity);
				g = g * (1.0f - intensity) + (this.currentAuraColor[1] * intensity);
				b = b * (1.0f - intensity) + (this.currentAuraColor[2] * intensity);
			}
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

		String key = activeSsj4 ? activeModel : targetModel;
		float target = activeSsj4 ? 1.0f : chargeFraction;
		float[] color = resolveSsj4OverlayColor(character, chargingSsj4 ? nextForm : null, chargeFraction);
		return new Ssj4Overlay(key, color, target);
	}

	private static boolean isSsj4Model(String model) {
		return model.equals("ssj4d") || model.equals("ssj4gt");
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

	private void renderSsj4Fur(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay, float baseAlpha) {
		if (this.currentSsj4Alpha <= 0.001f || this.currentSsj4Key == null || this.currentSsj4Color == null) return;

		float furAlpha = baseAlpha * this.currentSsj4Alpha;
		ResourceLocation tex = getSafeTexture(SkinGathererProvider.getCachedTexture("textures/entity/races/humansaiyan/" + this.currentSsj4Key + "_layer1.png"));
		RenderType renderType = furAlpha < 1.0f ? RenderType.entityTranslucent(tex) : RenderType.entityCutoutNoCull(tex);
		float[] color = this.currentSsj4Color;
		renderLayerWholeModel(model, poseStack, bufferSource, animatable, renderType, color[0], color[1], color[2], 1.0f, partialTick, packedLight, packedOverlay, furAlpha, true);
	}

	private void renderFadingColoredLayer(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, String path, float[] rgb, float partialTick, int packedLight, int packedOverlay, float alpha) {
		ResourceLocation loc = getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path));
		RenderType renderType = alpha < 1.0f ? RenderType.entityTranslucent(loc) : RenderType.entityCutoutNoCull(loc);
		renderLayerWholeModel(model, poseStack, bufferSource, animatable, renderType, rgb[0], rgb[1], rgb[2], 1.0f, partialTick, packedLight, packedOverlay, alpha, false);
	}

	private float[] applyColorTint(float[] rgb, StatsData stats) {
		if (rgb == null || rgb.length < 3) return rgb;

		int phase = TransformationsHelper.getKaiokenPhase(stats);
		if (phase <= 0 && this.currentTintProgress <= 0.0f) return rgb;

		float[] tinted = rgb.clone();
		if (phase > 0) {
			applyKaiokenToRgb(tinted, phase);
		} else {
			float intensity = 0.2f * this.currentTintProgress;
			float[] auraRgb = getTopAuraColor(stats);
			applyAuraTintToRgb(tinted, auraRgb, intensity);
		}

		return tinted;
	}

	private void applyKaiokenToRgb(float[] rgb, int phase) {
		float intensity = Math.min(0.6f, phase * 0.1f);
		rgb[0] = Mth.clamp(rgb[0] * (1.0f - intensity) + intensity, 0.0f, 1.0f);
		rgb[1] = Mth.clamp(rgb[1] * (1.0f - intensity), 0.0f, 1.0f);
		rgb[2] = Mth.clamp(rgb[2] * (1.0f - intensity), 0.0f, 1.0f);
	}

	private void applyAuraTintToRgb(float[] rgb, float[] auraRgb, float intensity) {
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
}