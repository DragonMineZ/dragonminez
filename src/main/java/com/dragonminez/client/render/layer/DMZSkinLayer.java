package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.SkinGathererProvider;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.Character;
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
	private int currentKaiokenPhase = 0;

	public DMZSkinLayer(GeoRenderer<T> entityRendererIn) {
		super(entityRendererIn);
	}

	@Override
	public void render(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

		var player = (AbstractClientPlayer) animatable;
		if (player == null) return;

		var statsCap = StatsProvider.get(StatsCapability.INSTANCE, player);
		var stats = statsCap.orElse(new StatsData(player));

		this.currentKaiokenPhase = TransformationsHelper.getKaiokenPhase(stats);

		float alpha = player.isSpectator() ? 0.15f : 1.0f;

		BiConsumer<ResourceLocation, float[]> geoConsumer = (texture, color) -> renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityTranslucent(texture), color[0], color[1], color[2], 1.0f, partialTick, packedLight, packedOverlay, alpha);

		SkinGathererProvider.INSTANCE.gatherBodyLayers(player, stats, partialTick, geoConsumer);
		renderHair(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay, alpha);
		SkinGathererProvider.INSTANCE.gatherAndroidLayers(player, stats, partialTick, geoConsumer);

		SkinGathererProvider.INSTANCE.gatherTattooLayers(player, stats, partialTick, geoConsumer);
		renderFace(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay, alpha);
	}

	private void renderHair(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay, float alpha) {
		var character = stats.getCharacter();
		String raceName = character.getRaceName().toLowerCase();
		String currentForm = character.getActiveForm();
		int hairId = character.getHairId();

		if (!HairManager.canUseHair(character)) return;
		if (raceName.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU)))
			return;
		if (hairId == 5) return;
		if (hairId == 0 && character.getHairBase().getVisibleStrandCount() == 0) return;

		float[] currentTint = ColorUtils.hexToRgb(character.getHairColor());

		if (character.hasActiveForm() && character.getActiveFormData() != null) {
			if (!character.getActiveFormData().getHairColor().isEmpty()) {
				currentTint = ColorUtils.hexToRgb(character.getActiveFormData().getHairColor());
			}
		}
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) {
			if (!character.getActiveStackFormData().getHairColor().isEmpty()) {
				currentTint = ColorUtils.hexToRgb(character.getActiveStackFormData().getHairColor());
			}
		}

		float[] finalTint = currentTint;
		if (stats.getStatus().isActionCharging()) {
			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				var nextForm = TransformationsHelper.getNextAvailableForm(stats);
				if (nextForm != null && !nextForm.getHairColor().isEmpty()) {
					float chargeProgress = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					finalTint = lerpColor(chargeProgress, currentTint, ColorUtils.hexToRgb(nextForm.getHairColor()));
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				var nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				if (nextForm != null && !nextForm.getHairColor().isEmpty()) {
					float chargeProgress = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					finalTint = lerpColor(chargeProgress, currentTint, ColorUtils.hexToRgb(nextForm.getHairColor()));
				}
			}
		}

		final float[] hairTint = finalTint;

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

			List<String> hiddenBones = new ArrayList<>();
			for (GeoBone bone : model.topLevelBones()) {
				if (!bone.isHidden()) {
					hiddenBones.add(bone.getName());
					bone.setHidden(true);
				}
			}

			headBone.setHidden(false);
			unhideParents(headBone);

			renderColoredLayer(model, poseStack, animatable, bufferSource, "textures/entity/races/hair_base.png", hairTint, partialTick, packedLight, packedOverlay, alpha);

			for (GeoBone bone : model.topLevelBones()) {
				if (hiddenBones.contains(bone.getName())) bone.setHidden(false);
			}

			headBone.setPosZ(originalZ);
			headBone.setScaleX(originalSX);
			headBone.setScaleY(originalSY);
			headBone.setScaleZ(originalSZ);
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
		boolean isHumanoidModel = finalFaceKey.equals("human") || finalFaceKey.equals("saiyan") || finalFaceKey.equals("saiyan_ssj4") || finalFaceKey.equals("buffed");
		if (isHumanoidModel && bodyType == 0) return;

		model.getBone("head").ifPresent(headBone -> {
			float originalZ = headBone.getPosZ();
			headBone.setPosZ(originalZ - 0.001f);
			dispatchFaceRender(model, poseStack, animatable, bufferSource, character, finalFaceKey, isModelEmpty, raceName, partialTick, packedLight, packedOverlay, alpha);
			headBone.setPosZ(originalZ);
		});
	}

	private void dispatchFaceRender(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, String faceKey, boolean isModelEmpty, String race, float pt, int pl, int po, float alpha) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(null);

		float[] eye1 = ColorUtils.hexToRgb(character.getEye1Color());
		float[] eye2 = ColorUtils.hexToRgb(character.getEye2Color());
		float[] skin = ColorUtils.hexToRgb(character.getBodyColor());
		float[] b2 = ColorUtils.hexToRgb(character.getBodyColor2());
		float[] hair = ColorUtils.hexToRgb(character.getHairColor());

		if (character.hasActiveForm() && character.getActiveFormData() != null) {
			var f = character.getActiveFormData();
			if (!f.getEye1Color().isEmpty()) eye1 = ColorUtils.hexToRgb(f.getEye1Color());
			if (!f.getEye2Color().isEmpty()) eye2 = ColorUtils.hexToRgb(f.getEye2Color());
			if (!f.getHairColor().isEmpty()) hair = ColorUtils.hexToRgb(f.getHairColor());
			if (!f.getBodyColor1().isEmpty()) skin = ColorUtils.hexToRgb(f.getBodyColor1());
			if (!f.getBodyColor2().isEmpty()) b2 = ColorUtils.hexToRgb(f.getBodyColor2());
		}

		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) {
			var sf = character.getActiveStackFormData();
			if (!sf.getEye1Color().isEmpty()) eye1 = ColorUtils.hexToRgb(sf.getEye1Color());
			if (!sf.getEye2Color().isEmpty()) eye2 = ColorUtils.hexToRgb(sf.getEye2Color());
			if (!sf.getHairColor().isEmpty()) hair = ColorUtils.hexToRgb(sf.getHairColor());
			if (!sf.getBodyColor1().isEmpty()) skin = ColorUtils.hexToRgb(sf.getBodyColor1());
			if (!sf.getBodyColor2().isEmpty()) b2 = ColorUtils.hexToRgb(sf.getBodyColor2());
		}

		if (stats != null && stats.getStatus().isActionCharging()) {
			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				var nextForm = TransformationsHelper.getNextAvailableForm(stats);
				if (nextForm != null) {
					float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					if (!nextForm.getEye1Color().isEmpty())
						eye1 = lerpColor(factor, eye1, ColorUtils.hexToRgb(nextForm.getEye1Color()));
					if (!nextForm.getEye2Color().isEmpty())
						eye2 = lerpColor(factor, eye2, ColorUtils.hexToRgb(nextForm.getEye2Color()));
					if (!nextForm.getBodyColor1().isEmpty())
						skin = lerpColor(factor, skin, ColorUtils.hexToRgb(nextForm.getBodyColor1()));
					if (!nextForm.getHairColor().isEmpty())
						hair = lerpColor(factor, hair, ColorUtils.hexToRgb(nextForm.getHairColor()));
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				var nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				if (nextForm != null) {
					float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					if (!nextForm.getEye1Color().isEmpty())
						eye1 = lerpColor(factor, eye1, ColorUtils.hexToRgb(nextForm.getEye1Color()));
					if (!nextForm.getEye2Color().isEmpty())
						eye2 = lerpColor(factor, eye2, ColorUtils.hexToRgb(nextForm.getEye2Color()));
					if (!nextForm.getBodyColor1().isEmpty())
						skin = lerpColor(factor, skin, ColorUtils.hexToRgb(nextForm.getBodyColor1()));
					if (!nextForm.getHairColor().isEmpty())
						hair = lerpColor(factor, hair, ColorUtils.hexToRgb(nextForm.getHairColor()));
				}
			}
		}

		if (faceKey.equals("human") || faceKey.equals("saiyan") || faceKey.equals("saiyan_ssj4") || faceKey.equals("buffed")) {
			renderHumanFace(model, poseStack, animatable, bufferSource, character, eye1, eye2, skin, hair, pt, pl, po, alpha);
			return;
		}
		if (faceKey.equals("namekian") || faceKey.equals("namekian_orange")) {
			renderNamekianFace(model, poseStack, animatable, bufferSource, character, eye1, eye2, skin, pt, pl, po, alpha);
			return;
		}
		if (faceKey.startsWith("frostdemon")) {
			renderFrostFace(model, poseStack, animatable, bufferSource, character, faceKey, isModelEmpty, race, eye1, eye2, skin, b2, pt, pl, po, alpha);
			return;
		}
		if (faceKey.startsWith("bioandroid")) {
			renderBioFace(model, poseStack, animatable, bufferSource, character, faceKey, isModelEmpty, race, eye1, eye2, pt, pl, po, alpha);
			return;
		}
		if (faceKey.equals("majin") || faceKey.equals("majin_super") || faceKey.equals("majin_ultra") || faceKey.equals("majin_evil") || faceKey.equals("majin_kid")) {
			renderMajinFace(model, poseStack, animatable, bufferSource, character, eye1, skin, pt, pl, po, alpha);
			return;
		}

		renderCustomFace(model, poseStack, animatable, bufferSource, character, race, eye1, eye2, skin, hair, pt, pl, po, alpha);

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
					renderMajinFace(model, poseStack, animatable, bufferSource, character, eye1, skin, pt, pl, po, alpha);
		}
	}

	private void renderHumanFace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, float[] eye1, float[] eye2, float[] skin, float[] hair, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/humansaiyan/faces/";
		String eyeBase = "humansaiyan_eye_" + character.getEyesType();
		float[] white = {1.0f, 1.0f, 1.0f};

		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_0.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "humansaiyan_eye_0_0.png")).getPath(), white, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "humansaiyan_eye_0_1.png")).getPath(), eye1, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_2.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "humansaiyan_eye_0_2.png")).getPath(), eye2, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_3.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "humansaiyan_eye_0_3.png")).getPath(), hair, pt, pl, po, alpha);

		boolean isSsj3 = false;
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && character.getActiveStackFormData().getHairType().equalsIgnoreCase("ssj3")) {
			isSsj3 = true;
		} else if (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().getHairType().equalsIgnoreCase("ssj3")) {
			isSsj3 = true;
		}

		if (isSsj3) {
			renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "ssj3eyebrows_eye_" + character.getEyesType() + ".png", skin, pt, pl, po, alpha);
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
		float[] white = {1.0f, 1.0f, 1.0f};
		float[] red = {1.0f, 0.0f, 0.0f};
		int bodyType = character.getBodyType();
		String currentForm = character.getActiveForm() != null ? character.getActiveForm().toLowerCase() : "";
		boolean isFifth = faceKey.equals("frostdemon_fifth") || currentForm.contains(FrostDemonForms.FIFTH_FORM);

		float[] eyeBgColor = isFifth ? red : white;
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_0.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_0.png")).getPath(), eyeBgColor, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_1.png")).getPath(), eye1, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_2.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_2.png")).getPath(), eye2, pt, pl, po, alpha);

		if (isFifth) {
			renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "frostdemon_fifth_mouth.png", skin, pt, pl, po, alpha);
			return;
		}

		boolean isPrimitiveForm = !character.hasActiveForm() || currentForm.equals("second") || currentForm.equals("third");
		float[] finalDetailColor = (isPrimitiveForm && (faceKey.equals("frostdemon") || faceKey.equals("frostdemon_third"))) ? b2 : (bodyType == 1 ? b2 : skin);
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

		float[] color0 = phase.equals("base") ? ColorUtils.hexToRgb("#FF6B6B") : ColorUtils.hexToRgb("#FFFFFF");
		String textureBase = folder + phase + "_eye_layer";

		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, textureBase + "0.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "base_eye_layer0.png")).getPath(), eye2, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, textureBase + "1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "base_eye_layer1.png")).getPath(), eye1, pt, pl, po, alpha);
	}

	private void renderCustomFace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, String faceKey, float[] eye1, float[] eye2, float[] skin, float[] hair, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/" + faceKey + "/faces/";
		String prefix = faceKey + "_";
		float[] white = {1.0f, 1.0f, 1.0f};

		renderColoredLayer(model, poseStack, animatable, bufferSource, folder + prefix + "eye_" + character.getEyesType() + "_0.png", white, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, folder + prefix + "eye_" + character.getEyesType() + "_1.png", eye1, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, folder + prefix + "eye_" + character.getEyesType() + "_2.png", eye2, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, folder + prefix + "eye_" + character.getEyesType() + "_3.png", hair, pt, pl, po, alpha);

		renderColoredLayer(model, poseStack, animatable, bufferSource, folder + prefix + "nose_" + character.getNoseType() + ".png", skin, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, folder + prefix + "mouth_" + character.getMouthType() + ".png", skin, pt, pl, po, alpha);
	}

	private void renderMajinFace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, float[] eye1, float[] skin, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/majin/faces/";
		int eyeType = character.getEyesType();
		float[] darkGray = ColorUtils.hexToRgb("#383838");

		float[] bgColor = eyeType == 0 ? skin : darkGray;
		float[] layer1Color = eyeType == 0 ? skin : eye1;
		String eyePath = folder + "majin_eye_" + eyeType + "_";

		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, eyePath + "0.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "majin_eye_0_0.png")).getPath(), bgColor, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, eyePath + "1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "majin_eye_0_1.png")).getPath(), layer1Color, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, eyePath + "2.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, folder + "majin_eye_0_2.png")).getPath(), skin, pt, pl, po, alpha);

		renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "majin_nose_" + character.getNoseType() + ".png", skin, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "majin_mouth_" + character.getMouthType() + ".png", skin, pt, pl, po, alpha);
	}

	private void renderLayerWholeModel(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource, T animatable, RenderType renderType, float r, float g, float b, float scaleInflation, float partialTick, int packedLight, int packedOverlay, float alpha) {

		if (this.currentKaiokenPhase > 0) {
			float intensity = Math.min(0.6f, this.currentKaiokenPhase * 0.1f);

			r = r * (1.0f - intensity) + (intensity);
			g = g * (1.0f - intensity);
			b = b * (1.0f - intensity);
		}

		poseStack.pushPose();
		if (scaleInflation > 1.0f) poseStack.scale(scaleInflation, scaleInflation, scaleInflation);

		getRenderer().reRender(model, poseStack, bufferSource, animatable, renderType, bufferSource.getBuffer(renderType), partialTick, packedLight, packedOverlay, r, g, b, alpha);

		poseStack.popPose();
	}

	private void renderColoredLayer(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, String path, float[] rgb, float partialTick, int packedLight, int packedOverlay, float alpha) {
		ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path);
		renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityTranslucent(getSafeTexture(loc)), rgb[0], rgb[1], rgb[2], 1.0f, partialTick, packedLight, packedOverlay, alpha);
	}

	private void unhideParents(GeoBone bone) {
		GeoBone parent = bone.getParent();
		while (parent != null) {
			parent.setHidden(false);
			parent = parent.getParent();
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