package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DMZRacePartsLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {
	private static final ResourceLocation RACES_PARTS_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/raceparts.geo.json");
	private static final ResourceLocation RACES_PARTS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/raceparts.png");

	private static final ResourceLocation ACCESORIES_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/accesories.geo.json");
	private static final ResourceLocation SCOUTER_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/scouter.geo.json");

	private static final ResourceLocation YAJIROBE_SWORD_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/yajirobe_katana.geo.json");
	private static final ResourceLocation YAJIROBE_SWORD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/armas/yajirobe_katana.png");
	private static final ResourceLocation Z_SWORD_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/z_sword.geo.json");
	private static final ResourceLocation Z_SWORD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/armas/z_sword.png");
	private static final ResourceLocation BRAVE_SWORD_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/brave_sword.geo.json");
	private static final ResourceLocation BRAVE_SWORD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/armas/brave_sword.png");
	private static final ResourceLocation POWER_POLE_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/power_pole.geo.json");
	private static final ResourceLocation POWER_POLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/armas/power_pole.png");

	private static final Map<Integer, Float> AURA_TINT_PROGRESS = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_RENDER_TIME = new ConcurrentHashMap<>();
	private static final float FADE_SPEED = 0.005f;

	public DMZRacePartsLayer(GeoRenderer<T> entityRendererIn) {
		super(entityRendererIn);
	}

	@Override
	public void render(PoseStack poseStack, T animatable, BakedGeoModel playerModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

		var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(null);
		if (stats == null) return;

		float alpha = animatable.isSpectator() ? 0.15f : 1.0f;

		int playerId = animatable.getId();
		long gameTime = animatable.level().getGameTime();
		float tintProgress = AURA_TINT_PROGRESS.getOrDefault(playerId, 0.0f);

		if (gameTime - LAST_RENDER_TIME.getOrDefault(playerId, 0L) > 2) {
			tintProgress = 0.0f;
		}
		LAST_RENDER_TIME.put(playerId, gameTime);

		if (stats.getStatus().isChargingKi()) {
			if (tintProgress < 1.0f) {
				tintProgress += FADE_SPEED;
				if (tintProgress > 1.0f) tintProgress = 1.0f;
			}
		} else {
			if (tintProgress > 0.0f) {
				tintProgress -= FADE_SPEED;
				if (tintProgress < 0.0f) tintProgress = 0.0f;
			}
		}
		AURA_TINT_PROGRESS.put(playerId, tintProgress);

		renderRaceParts(poseStack, animatable, playerModel, bufferSource, stats, partialTick, packedLight, alpha, tintProgress);

		if (!animatable.isSpectator()) {
			renderAccessories(poseStack, animatable, playerModel, bufferSource, partialTick, packedLight);
			renderScouter(poseStack, animatable, playerModel, bufferSource, partialTick, packedLight);
			renderSword(poseStack, animatable, playerModel, bufferSource, partialTick, packedLight);
		}
	}

	private String resolveAccessoryBone(String accessoryName) {
		if (accessoryName == null || accessoryName.isEmpty()) return "";
		return switch (accessoryName.toLowerCase()) {
			case "saiyan_tail" -> "tailenrolled";
			case "majin_tail" -> "colamajin";
			case "namek_antennas", "antennas" -> "antenas";
			case "namek_ears", "namek_ears1", "majin_ears", "majin_ears1", "ears", "ears1" -> "orejas1";
			case "namek_ears2", "majin_ears2", "ears2" -> "orejas2";
			case "namek_ears3", "majin_ears3", "female_ears", "ears3" -> "orejas3";
			case "frost_horns", "horns", "horns1" -> "cuernos";
			case "frost_horns2", "horns2" -> "cuernos2";
			default -> accessoryName;
		};
	}

	private void renderRaceParts(PoseStack poseStack, T animatable, BakedGeoModel playerModel, MultiBufferSource bufferSource, StatsData stats, float partialTick, int packedLight, float alpha, float tintProgress) {
		var character = stats.getCharacter();
		String race = character.getRaceName().toLowerCase();

		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(race);
		if (raceConfig == null) return;

		BakedGeoModel partsModel = getGeoModel().getBakedModel(RACES_PARTS_MODEL);
		if (partsModel == null) return;

		RenderType partsRenderType = RenderType.entityTranslucent(RACES_PARTS_TEXTURE);
		int phase = TransformationsHelper.getKaiokenPhase(stats);
		float[] topAuraColor = getTopAuraColor(stats);

		boolean hasForm = character.hasActiveForm() && character.getActiveFormData() != null;
		boolean formHasAccessories = hasForm && character.getActiveFormData().getRacialAccessories() != null && character.getActiveFormData().getRacialAccessories().length > 0;
		boolean baseHasAccessories = raceConfig.getRacialAccessories() != null && raceConfig.getRacialAccessories().length > 0;

		if (formHasAccessories || baseHasAccessories) {
			String[] accessories = formHasAccessories ? character.getActiveFormData().getRacialAccessories() : raceConfig.getRacialAccessories();

			for (int i = 0; i < accessories.length; i++) {
				final int index = i;
				String configName = accessories[index];
				String boneName = resolveAccessoryBone(configName);
				partsModel.getBone(boneName).ifPresent(targetBone -> {
					syncTargetBoneAndParents(targetBone, playerModel);
					float[] rgb = formHasAccessories ? character.getActiveFormData().getRgbAccessoryColor(index) : raceConfig.getRgbAccessoryColor(index);
					float[] tintedColor = applyAuraTint(rgb[0], rgb[1], rgb[2], phase, topAuraColor, tintProgress);
					renderTargetedBone(targetBone, poseStack, bufferSource, animatable, partsRenderType, tintedColor[0], tintedColor[1], tintedColor[2], alpha, partialTick, packedLight);
				});
			}
		} else {
			String fallbackBone = resolveFallbackBone(stats);
			if (fallbackBone == null) return;

			partsModel.getBone(fallbackBone).ifPresent(targetBone -> {
				syncTargetBoneAndParents(targetBone, playerModel);
				float[] renderColor = setupPartsAndColor(partsModel, stats);

				if (renderColor != null) {
					float[] tintedColor = applyAuraTint(renderColor[0], renderColor[1], renderColor[2], phase, topAuraColor, tintProgress);
					renderTargetedBone(targetBone, poseStack, bufferSource, animatable, partsRenderType, tintedColor[0], tintedColor[1], tintedColor[2], alpha, partialTick, packedLight);
				}
			});
		}
	}

	private String resolveFallbackBone(StatsData stats) {
		var character = stats.getCharacter();
		String race = character.getRaceName().toLowerCase();
		String currentForm = character.getActiveForm() != null ? character.getActiveForm().toLowerCase() : "";
		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(race);

		boolean isSaiyanLogic = race.equals("saiyan");
		boolean hasSaiyanTail = raceConfig != null && raceConfig.getHasSaiyanTail();
		if ((isSaiyanLogic || hasSaiyanTail) && !stats.getStatus().isTailVisible() && character.isHasSaiyanTail()) return "tailenrolled";

		if (race.equals("namekian") || race.equals("namekian_orange")) return "antenas";
		if (race.equals("majin")) return character.getGender().toLowerCase().contains("female") ? "orejas3" : "colamajin";
		if (race.startsWith("frostdemon")) return currentForm.equals(FrostDemonForms.SECOND_FORM) ? "cuernos2" : "cuernos";

		return null;
	}

	private void renderTargetedBone(GeoBone targetBone, PoseStack poseStack, MultiBufferSource bufferSource, T animatable, RenderType renderType, float r, float g, float b, float alpha, float partialTick, int packedLight) {
		VertexConsumer buffer = bufferSource.getBuffer(renderType);
		getRenderer().renderRecursively(poseStack, animatable, targetBone, renderType, bufferSource, buffer, true, partialTick, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, alpha);
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

	private float[] applyAuraTint(float r, float g, float b, int kaiokenPhase, float[] auraColor, float tintProgress) {
		float intensity = kaiokenPhase > 0 ? Math.min(0.6f, kaiokenPhase * 0.1f) : 0.4f * tintProgress;

		if (intensity <= 0.001f) return new float[]{r, g, b};

		float newR = r * (1.0f - intensity) + (auraColor[0] * intensity);
		float newG = g * (1.0f - intensity) + (auraColor[1] * intensity);
		float newB = b * (1.0f - intensity) + (auraColor[2] * intensity);

		return new float[]{newR, newG, newB};
	}

	private float[] setupPartsAndColor(BakedGeoModel partsModel, StatsData stats) {
		var character = stats.getCharacter();
		String race = character.getRaceName().toLowerCase();
		String currentForm = character.getActiveForm() != null ? character.getActiveForm().toLowerCase() : "";
		String currentStackForm = character.getActiveStackForm() != null ? character.getActiveStackForm().toLowerCase() : "";
		boolean hasForm = (character.hasActiveForm() && !currentForm.equals("base") && !currentForm.isEmpty());
		boolean hasStackForm = (character.hasActiveStackForm() && !currentStackForm.equals("base") && !currentStackForm.isEmpty());

		float[] colorBody1 = character.getRgbBodyColor();
		float[] colorBody2 = character.getRgbBodyColor2();

		if (hasForm && character.getActiveFormData() != null) {
			String formBody = character.getActiveFormData().getBodyColor1();
			if (formBody != null && !formBody.isEmpty()) colorBody1 = character.getActiveFormData().getRgbBodyColor1();

			String formBody2 = character.getActiveFormData().getBodyColor2();
			if (formBody2 != null && !formBody2.isEmpty()) colorBody2 = character.getActiveFormData().getRgbBodyColor2();
		}

		if (hasStackForm && character.getActiveStackFormData() != null) {
			String formBody = character.getActiveStackFormData().getBodyColor1();
			if (formBody != null && !formBody.isEmpty()) colorBody1 = character.getActiveStackFormData().getRgbBodyColor1();

			String formBody2 = character.getActiveStackFormData().getBodyColor2();
			if (formBody2 != null && !formBody2.isEmpty()) colorBody2 = character.getActiveStackFormData().getRgbBodyColor2();
		}

		boolean isSaiyanLogic = race.equals("saiyan");
		boolean isOozaru = currentForm.contains("oozaru");
		boolean hasSaiyanTail = ConfigManager.getRaceCharacter(race) != null && ConfigManager.getRaceCharacter(race).getHasSaiyanTail();

		if ((isSaiyanLogic || hasSaiyanTail) && !stats.getStatus().isTailVisible() && !isOozaru && character.isHasSaiyanTail()) {
			boolean hasBodyColor2 = character.getBodyColor2() != null && !character.getBodyColor2().isEmpty();
			return (hasBodyColor2 || hasForm || hasStackForm || stats.getStatus().isActionCharging()) ? colorBody2 : ColorUtils.hexToRgb("#572117");
		}

		if (race.equals("namekian") || race.equals("namekian_orange")) return colorBody1;
		if (race.startsWith("majin")) return colorBody1;

		if (race.startsWith("frostdemon")) {
			boolean isSpecialForm = currentForm.equals(FrostDemonForms.FINAL_FORM) ||
					currentForm.equals(FrostDemonForms.FULLPOWER) ||
					currentForm.equals(FrostDemonForms.THIRD_FORM) ||
					currentForm.contains("fifth");

			if (isSpecialForm) return null;
			return ColorUtils.rgbIntToFloat(0x1A1A1A);
		}

		return null;
	}

	private void syncModelToPlayer(BakedGeoModel partsModel, BakedGeoModel playerModel) {
		for (GeoBone partBone : partsModel.topLevelBones()) {
			syncBoneRecursively(partBone, playerModel);
		}
	}

	private void syncBoneRecursively(GeoBone destBone, BakedGeoModel sourceModel) {
		sourceModel.getBone(destBone.getName()).ifPresent(sourceBone -> copyBoneData(sourceBone, destBone));
		for (GeoBone child : destBone.getChildBones()) {
			syncBoneRecursively(child, sourceModel);
		}
	}

	private void syncTargetBoneAndParents(GeoBone destBone, BakedGeoModel sourceModel) {
		GeoBone currentDest = destBone;
		while (currentDest != null) {
			final GeoBone finalDest = currentDest;
			sourceModel.getBone(finalDest.getName()).ifPresent(sourceBone -> copyBoneData(sourceBone, finalDest));
			currentDest = currentDest.getParent();
		}
	}

	private void copyBoneData(GeoBone source, GeoBone dest) {
		dest.setRotX(source.getRotX());
		dest.setRotY(source.getRotY());
		dest.setRotZ(source.getRotZ());
		dest.setPosX(source.getPosX());
		dest.setPosY(source.getPosY());
		dest.setPosZ(source.getPosZ());
		dest.setPivotX(source.getPivotX());
		dest.setPivotY(source.getPivotY());
		dest.setPivotZ(source.getPivotZ());
		dest.setScaleX(source.getScaleX());
		dest.setScaleY(source.getScaleY());
		dest.setScaleZ(source.getScaleZ());
	}

	private void renderAccessories(PoseStack poseStack, T animatable, BakedGeoModel playerModel, MultiBufferSource bufferSource, float partialTick, int packedLight) {
		boolean hasPothalaRight = animatable.getItemBySlot(EquipmentSlot.HEAD).getItem().getDescriptionId().contains("pothala_right");
		boolean hasPothalaLeft = animatable.getItemBySlot(EquipmentSlot.HEAD).getItem().getDescriptionId().contains("pothala_left");

		var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
		var stats = statsCap.orElse(new StatsData(animatable));

		boolean isFused = stats.getStatus().isFused() && stats.getStatus().getFusionType().equalsIgnoreCase("POTHALA");

		if (!isFused && !hasPothalaRight && !hasPothalaLeft) return;

		BakedGeoModel accModel = getGeoModel().getBakedModel(ACCESORIES_MODEL);
		if (accModel == null) return;

		String pothalaColor = stats.getStatus().getPothalaColor().contains("green") ? "green" : "yellow";
		RenderType accRenderType = RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + pothalaColor + "pothala.png"));

		if (hasPothalaRight || isFused) {
			accModel.getBone("pothala_right").ifPresent(bone -> {
				syncTargetBoneAndParents(bone, playerModel);
				renderTargetedBone(bone, poseStack, bufferSource, animatable, accRenderType, 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight);
			});
		}

		if (hasPothalaLeft || isFused) {
			accModel.getBone("pothala_left").ifPresent(bone -> {
				syncTargetBoneAndParents(bone, playerModel);
				renderTargetedBone(bone, poseStack, bufferSource, animatable, accRenderType, 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight);
			});
		}
	}

	private void renderScouter(PoseStack poseStack, T animatable, BakedGeoModel playerModel, MultiBufferSource bufferSource, float partialTick, int packedLight) {
		ItemStack headStack = animatable.getItemBySlot(EquipmentSlot.HEAD);
		Item headItem = headStack.getItem();

		String color = null;
		if (headItem == MainItems.GREEN_SCOUTER.get()) color = "green";
		else if (headItem == MainItems.RED_SCOUTER.get()) color = "red";
		else if (headItem == MainItems.BLUE_SCOUTER.get()) color = "blue";
		else if (headItem == MainItems.PURPLE_SCOUTER.get()) color = "purple";

		if (color == null) return;

		BakedGeoModel accModel = getGeoModel().getBakedModel(SCOUTER_MODEL);
		if (accModel == null) return;

		RenderType accRenderType = RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + color + "_scouter.png"));

		accModel.getBone("radar").ifPresent(bone -> {
			syncTargetBoneAndParents(bone, playerModel);
			renderTargetedBone(bone, poseStack, bufferSource, animatable, accRenderType, 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight);
		});
	}

	private void renderSword(PoseStack poseStack, T animatable, BakedGeoModel playerModel, MultiBufferSource bufferSource, float partialTick, int packedLight) {
		var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
		var stats = statsCap.orElse(new StatsData(animatable));

		if (!stats.getStatus().isHasCreatedCharacter()) return;
		if (stats.getCharacter().getActiveFormData() != null && stats.getCharacter().getActiveForm().contains("ozaru")) return;

		if (stats.getStatus().isRenderKatana()) {
			BakedGeoModel yajirobeModel = getGeoModel().getBakedModel(YAJIROBE_SWORD_MODEL);
			if (yajirobeModel != null) {
				RenderType type = RenderType.entityCutoutNoCull(YAJIROBE_SWORD_TEXTURE);
				syncModelToPlayer(yajirobeModel, playerModel);
				renderFullWeapon(yajirobeModel, poseStack, bufferSource, animatable, type, partialTick, packedLight, 1.0f);
			}
		}

		if (stats.getStatus().getBackWeapon() == null || stats.getStatus().getBackWeapon().isEmpty()) return;

		if (stats.getStatus().getBackWeapon().equals(MainItems.POWER_POLE.get().getDescriptionId())) {
			BakedGeoModel powerpole = getGeoModel().getBakedModel(POWER_POLE_MODEL);
			if (powerpole != null) {
				RenderType type = RenderType.entityCutoutNoCull(POWER_POLE_TEXTURE);
				syncModelToPlayer(powerpole, playerModel);
				renderFullWeapon(powerpole, poseStack, bufferSource, animatable, type, partialTick, packedLight, 1.0f);
			}
		} else if (stats.getStatus().getBackWeapon().equals(MainItems.Z_SWORD.get().getDescriptionId())) {
			BakedGeoModel zModel = getGeoModel().getBakedModel(Z_SWORD_MODEL);
			if (zModel != null) {
				RenderType type = RenderType.entityCutoutNoCull(Z_SWORD_TEXTURE);
				syncModelToPlayer(zModel, playerModel);
				renderFullWeapon(zModel, poseStack, bufferSource, animatable, type, partialTick, packedLight, 1.0f);
			}
		} else if (stats.getStatus().getBackWeapon().equals(MainItems.BRAVE_SWORD.get().getDescriptionId())) {
			BakedGeoModel braveModel = getGeoModel().getBakedModel(BRAVE_SWORD_MODEL);
			if (braveModel != null) {
				RenderType type = RenderType.entityCutoutNoCull(BRAVE_SWORD_TEXTURE);
				syncModelToPlayer(braveModel, playerModel);
				renderFullWeapon(braveModel, poseStack, bufferSource, animatable, type, partialTick, packedLight, 0.9f);
			}
		}
	}

	private void renderFullWeapon(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource, T animatable, RenderType type, float partialTick, int packedLight, float scale) {
		poseStack.pushPose();
		if (scale != 1.0f) poseStack.scale(scale, scale, scale);

		VertexConsumer vertexConsumer = bufferSource.getBuffer(type);
		for (GeoBone bone : model.topLevelBones()) {
			if (!bone.isHidden()) {
				getRenderer().renderRecursively(poseStack, animatable, bone, type, bufferSource, vertexConsumer, true, partialTick, packedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
			}
		}

		poseStack.popPose();
	}
}