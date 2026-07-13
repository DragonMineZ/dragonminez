package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.item.WeightItem;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import top.theillusivec4.curios.api.CuriosApi;

public class DMZRacePartsLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {
	private static final ResourceLocation RACES_PARTS_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/raceparts.geo.json");
	private static final ResourceLocation RACES_PARTS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/raceparts.png");

	private static final ResourceLocation ACCESORIES_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/accesories.geo.json");
	private static final ResourceLocation SCOUTER_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/scouter.geo.json");

	private static final ResourceLocation YAJIROBE_SWORD_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/yajirobe_katana.geo.json");
	private static final ResourceLocation YAJIROBE_SWORD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/yajirobe_katana.png");
	private static final ResourceLocation Z_SWORD_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/z_sword.geo.json");
	private static final ResourceLocation Z_SWORD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/z_sword.png");
	private static final ResourceLocation BRAVE_SWORD_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/brave_sword.geo.json");
	private static final ResourceLocation BRAVE_SWORD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/brave_sword.png");
	private static final ResourceLocation POWER_POLE_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/power_pole.geo.json");
	private static final ResourceLocation POWER_POLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/power_pole.png");

	private static final ResourceLocation WEIGHTED_ITEMS_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/weighted_items.geo.json");
	private static final ResourceLocation WEIGHTED_ITEMS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/weighted_items.png");

	public DMZRacePartsLayer(GeoRenderer<T> entityRendererIn) {
		super(entityRendererIn);
	}

	@Override
	public void render(PoseStack poseStack, T animatable, BakedGeoModel playerModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(new StatsData(animatable));

		int playerId = animatable.getId();
		long gameTime = animatable.level().getGameTime();
		boolean shouldFadeIn = stats.getStatus().isChargingKi()
				|| stats.getStatus().isAuraActive()
				|| stats.getStatus().isPermanentAura();
		AuraTintTracker.update(playerId, gameTime, shouldFadeIn);
	}

	@Override
	public void renderForBone(PoseStack poseStack, T animatable, GeoBone playerBone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		String anchor = playerBone.getName();
		boolean isLimb = "right_arm".equals(anchor) || "left_arm".equals(anchor)
				|| "right_leg".equals(anchor) || "left_leg".equals(anchor);
		if (!"head".equals(anchor) && !"body".equals(anchor) && !isLimb) return;

		if (animatable.hasEffect(MainEffects.CANDY.get())) return;

		if (FirstPersonManager.shouldRenderFirstPerson(animatable)) {
			var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(new StatsData(animatable));
			if ("body".equals(anchor) && !animatable.isSpectator() && stats.getStatus().isHasCreatedCharacter()) {
				boolean isOozaru = stats.getCharacter().getActiveForm() != null && stats.getCharacter().getActiveForm().contains("ozaru");
				if (!isOozaru && stats.getStatus().isRenderKatana()) {
					BakedGeoModel yajirobeModel = getGeoModel().getBakedModel(YAJIROBE_SWORD_MODEL);
					if (yajirobeModel != null) {
						RenderType type = RenderType.entityCutoutNoCull(YAJIROBE_SWORD_TEXTURE);
						renderWeaponFromBodyAnchor(yajirobeModel, "katana", playerBone, poseStack, bufferSource, animatable, type, partialTick, packedLight, 1.0f);
					}
				}
			}
			bufferSource.getBuffer(renderType);
			return;
		}

		var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(new StatsData(animatable));
		float alpha = animatable.isSpectator() ? 0.15f : 1.0f;

		if (isLimb) {
			if (!animatable.isSpectator() && !stats.getCharacter().isOozaruCached()) {
				renderWeights(poseStack, animatable, bufferSource, anchor, partialTick, packedLight, alpha);
			}
			bufferSource.getBuffer(renderType);
			return;
		}

		float tintProgress = AuraTintTracker.get(animatable.getId());

		BakedGeoModel playerModel = getGeoModel().getBakedModel(getGeoModel().getModelResource(animatable));

		renderRacePartsForAnchor(poseStack, animatable, playerModel, bufferSource, stats, anchor, partialTick, packedLight, alpha, tintProgress);

		if (!animatable.isSpectator()) {
			if ("head".equals(anchor) && !stats.getCharacter().isOozaruCached()) {
				renderAccessories(poseStack, animatable, playerModel, bufferSource, partialTick, packedLight);
			} else {
				renderSword(poseStack, animatable, playerBone, bufferSource, partialTick, packedLight);
				if ("body".equals(anchor) && !stats.getCharacter().isOozaruCached()) {
					renderWeights(poseStack, animatable, bufferSource, anchor, partialTick, packedLight, alpha);
				}
			}
		}

		bufferSource.getBuffer(renderType);
	}

	private void renderRacePartsForAnchor(PoseStack poseStack, T animatable, BakedGeoModel playerModel, MultiBufferSource bufferSource, StatsData stats, String anchor, float partialTick, int packedLight, float alpha, float tintProgress) {
		var character = stats.getCharacter();
		var isAlive = stats.getStatus().isAlive();
		String race = character.getRaceName().toLowerCase();
		String currentForm = character.getActiveForm();

		boolean isOozaru = currentForm != null && currentForm.toLowerCase().contains("oozaru");
		if (isOozaru) return;

		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(race);
		if (raceConfig == null) return;

		String formCustomModel = "";
		boolean hasStackForm = character.hasActiveStackForm() && character.getActiveStackFormData() != null;
		boolean hasForm = character.hasActiveForm() && character.getActiveFormData() != null;

		if (hasStackForm && character.getActiveStackFormData().hasCustomModel()) {
			formCustomModel = character.getActiveStackFormData().getCustomModel().toLowerCase();
		} else if (hasForm && character.getActiveFormData().hasCustomModel()) {
			formCustomModel = character.getActiveFormData().getCustomModel().toLowerCase();
		}

		String raceCustomModel = raceConfig.getCustomModel() != null ? raceConfig.getCustomModel().toLowerCase() : "";
		String key = formCustomModel.isEmpty() ? raceCustomModel : formCustomModel;
		if (key.isEmpty()) key = race;

		String logicKey = key;

		BakedGeoModel partsModel = getGeoModel().getBakedModel(RACES_PARTS_MODEL);
		if (partsModel == null) return;

		RenderType partsRenderType = RenderType.entityTranslucent(RACES_PARTS_TEXTURE);
		int phase = TransformationsHelper.getKaiokenPhase(stats);
		float[] topAuraColor = getTopAuraColor(stats);

		float[] accessoryColor = character.getRgbHairColor();

		if (character.hasActiveForm() && character.getActiveFormData() != null && !character.getActiveFormData().getHairColor().isEmpty()) {
			accessoryColor = character.getActiveFormData().getRgbHairColor();
		}
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && !character.getActiveStackFormData().getHairColor().isEmpty()) {
			accessoryColor = character.getActiveStackFormData().getRgbHairColor();
		}

		if (stats.getStatus().isActionCharging()) {
			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				var nextForm = TransformationsHelper.getNextAvailableForm(stats);
				if (nextForm != null && !nextForm.getHairColor().isEmpty()) {
					float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					accessoryColor = DMZSkinLayer.lerpColor(factor, accessoryColor, nextForm.getRgbHairColor());
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				var nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				if (nextForm != null && !nextForm.getHairColor().isEmpty()) {
					float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					accessoryColor = DMZSkinLayer.lerpColor(factor, accessoryColor, nextForm.getRgbHairColor());
				}
			}
		}

		if (anchor.equals("head")) {
			boolean extraHeadBonesEnabled = character.areExtraHeadBonesEnabled();
			String activeBone = character.getRenderableHeadBone();

			if (extraHeadBonesEnabled && activeBone != null && !activeBone.isEmpty() && !activeBone.equals("hair")) {
				for (String boneName : activeBone.split("\\+")) {
					if (boneName.isEmpty() || boneName.equals("hair")) continue;
					GeoBone targetBone = partsModel.getBone(boneName).orElse(null);
					boolean fromPlayerModel = false;
					if (targetBone == null) {
						targetBone = playerModel.getBone(boneName).orElse(null);
						fromPlayerModel = true;
					}
					if (targetBone != null) {
						if (!fromPlayerModel) {
							syncTargetBoneAndParents(targetBone, playerModel);
						}

						float[] colorToTint = accessoryColor;

						if (character.getRaceName().equals("majin") || character.getRaceName().equals("namekian")) {
							colorToTint = resolveBodyColor1(stats);
						}

						float[] tintedColor = applyAuraTint(colorToTint[0], colorToTint[1], colorToTint[2], phase, topAuraColor, tintProgress);

						if (boneName.contains("horn") && character.getRaceName().equals("frostdemon")) {
							tintedColor = ColorUtils.hexToRgb("#1A1A1A");
						}

						renderTargetedBone(targetBone, poseStack, bufferSource, animatable, partsRenderType, tintedColor[0], tintedColor[1], tintedColor[2], alpha, partialTick, packedLight);
					}
				}
			}

			if (extraHeadBonesEnabled && (race.equals("namekian") || logicKey.equals("namekian_orange") || logicKey.equals("namekian_buffed"))) {

				GeoBone antennaBone = partsModel.getBone("antennas1").orElse(null);
				boolean antennaFromPlayerModel = false;

				if (antennaBone == null) {
					antennaBone = playerModel.getBone("antennas1").orElse(null);
					antennaFromPlayerModel = true;
				}

				if (antennaBone != null) {
					if (!antennaFromPlayerModel) {
						syncTargetBoneAndParents(antennaBone, playerModel);
					}
					float[] antennaColor = resolveBodyColor1(stats);
					float[] tintedColor = applyAuraTint(antennaColor[0], antennaColor[1], antennaColor[2], phase, topAuraColor, tintProgress);
					renderTargetedBone(antennaBone, poseStack, bufferSource, animatable, partsRenderType, tintedColor[0], tintedColor[1], tintedColor[2], alpha, partialTick, packedLight);
				}
			}

			if (extraHeadBonesEnabled && race.equals("majin")) {
				float[] majinBodyColor = resolveBodyColor1(stats);

				GeoBone earsBone = partsModel.getBone("ears3").orElse(null);
				boolean earsFromPlayerModel = false;

				if (earsBone == null) {
					earsBone = playerModel.getBone("ears3").orElse(null);
					earsFromPlayerModel = true;
				}

				if (earsBone != null) {
					if (!earsFromPlayerModel) {
						syncTargetBoneAndParents(earsBone, playerModel);
					}

					float[] tintedColor = applyAuraTint(majinBodyColor[0], majinBodyColor[1], majinBodyColor[2], phase, topAuraColor, tintProgress);
					renderTargetedBone(earsBone, poseStack, bufferSource, animatable, partsRenderType, tintedColor[0], tintedColor[1], tintedColor[2], alpha, partialTick, packedLight);
				}
			}

			if (!stats.getStatus().isAlive() || stats.getStatus().isForceHalo()) {
				GeoBone haloBone = partsModel.getBone("halo").orElse(null);

				if (haloBone != null) {
					syncTargetBoneAndParents(haloBone, playerModel);

					float[] haloColor = ColorUtils.hexToRgb("#FFF461");

					renderTargetedBone(haloBone, poseStack, bufferSource, animatable,
							ModRenderTypes.energy(RACES_PARTS_TEXTURE),
							haloColor[0], haloColor[1], haloColor[2], 0.75f, partialTick, packedLight);
				}
			}
		}

		if (anchor.equals("body")) {
			boolean isSaiyanLogic = race.equals("saiyan");
			boolean hasSaiyanTail = raceConfig.getHasSaiyanTail() != null && raceConfig.getHasSaiyanTail();

			if ((isSaiyanLogic || hasSaiyanTail) && !stats.getStatus().isTailVisible() && character.isHasSaiyanTail()) {
				partsModel.getBone("tailenrolled").ifPresent(targetBone -> {
					syncTargetBoneAndParents(targetBone, playerModel);
					float[] tailColor = ColorUtils.hexToRgb("#572117");

					if (character.getBodyColor2() != null && !character.getBodyColor2().isEmpty()) {
						tailColor = character.getRgbBodyColor2();
					}
					if (character.hasActiveForm() && character.getActiveFormData() != null && !character.getActiveFormData().getBodyColor2().isEmpty()) {
						tailColor = character.getActiveFormData().getRgbBodyColor2();
					}
					if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && !character.getActiveStackFormData().getBodyColor2().isEmpty()) {
						tailColor = character.getActiveStackFormData().getRgbBodyColor2();
					}

					if (stats.getStatus().isActionCharging()) {
						if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
							var nextForm = TransformationsHelper.getNextAvailableForm(stats);
							if (nextForm != null && !nextForm.getBodyColor2().isEmpty()) {
								float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
								tailColor = DMZSkinLayer.lerpColor(factor, tailColor, nextForm.getRgbBodyColor2());
							}
						} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
							var nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
							if (nextForm != null && !nextForm.getBodyColor2().isEmpty()) {
								float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
								tailColor = DMZSkinLayer.lerpColor(factor, tailColor, nextForm.getRgbBodyColor2());
							}
						}
					}

					float[] tintedColor = applyAuraTint(tailColor[0], tailColor[1], tailColor[2], phase, topAuraColor, tintProgress);
					renderTargetedBone(targetBone, poseStack, bufferSource, animatable, partsRenderType, tintedColor[0], tintedColor[1], tintedColor[2], alpha, partialTick, packedLight);
				});
			}
		}
	}

	private void renderTargetedBone(GeoBone targetBone, PoseStack poseStack, MultiBufferSource bufferSource, T animatable, RenderType renderType, float r, float g, float b, float alpha, float partialTick, int packedLight) {
		VertexConsumer buffer = bufferSource.getBuffer(renderType);
		getRenderer().renderRecursively(poseStack, animatable, targetBone, renderType, bufferSource, buffer, true, partialTick, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, alpha);
	}

	private float[] resolveBodyColor1(StatsData stats) {
		var character = stats.getCharacter();
		float[] color = character.getRgbBodyColor();

		if (character.hasActiveForm() && character.getActiveFormData() != null && !character.getActiveFormData().getBodyColor1().isEmpty()) {
			color = character.getActiveFormData().getRgbBodyColor1();
		}
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && !character.getActiveStackFormData().getBodyColor1().isEmpty()) {
			color = character.getActiveStackFormData().getRgbBodyColor1();
		}

		if (stats.getStatus().isActionCharging()) {
			float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				var nextForm = TransformationsHelper.getNextAvailableForm(stats);
				if (nextForm != null && !nextForm.getBodyColor1().isEmpty()) {
					color = DMZSkinLayer.lerpColor(factor, color, nextForm.getRgbBodyColor1());
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				var nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				if (nextForm != null && !nextForm.getBodyColor1().isEmpty()) {
					color = DMZSkinLayer.lerpColor(factor, color, nextForm.getRgbBodyColor1());
				}
			}
		}

		return color;
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
		intensity *= AuraTintTracker.darkTintScale(r, g, b);

		if (intensity <= 0.001f) return new float[]{r, g, b};

		float newR = r * (1.0f - intensity) + (auraColor[0] * intensity);
		float newG = g * (1.0f - intensity) + (auraColor[1] * intensity);
		float newB = b * (1.0f - intensity) + (auraColor[2] * intensity);

		return new float[]{newR, newG, newB};
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
		for (int i = 0; i < getCuriosSlotSize(animatable, "head_tech"); i++) {
			ItemStack headTechStack = getRenderableCurio(animatable, "head_tech", i);
			renderPothala(poseStack, animatable,  playerModel, bufferSource, partialTick, packedLight, headTechStack);
			renderScouter(poseStack, animatable,  playerModel, bufferSource, partialTick, packedLight, headTechStack);

			ItemStack cosmeticHeadTechStack = getRenderableCosmetic(animatable, "head_tech", i);
			renderPothala(poseStack, animatable, playerModel, bufferSource, partialTick, packedLight, cosmeticHeadTechStack);
			renderScouter(poseStack, animatable, playerModel, bufferSource, partialTick, packedLight, cosmeticHeadTechStack);
		}
	}

	private void renderPothala(PoseStack poseStack, T animatable, BakedGeoModel playerModel, MultiBufferSource bufferSource, float partialTick, int packedLight, ItemStack stack) {
		boolean hasPothalaRight = stack.getItem().getDescriptionId().contains("pothala_right");
		boolean hasPothalaLeft = stack.getItem().getDescriptionId().contains("pothala_left");

		var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
		var stats = statsCap.orElse(new StatsData(animatable));

		boolean isFused = stats.getStatus().isFused() && stats.getStatus().getFusionType().equalsIgnoreCase("POTHALA");

		if (!isFused && !hasPothalaRight && !hasPothalaLeft) return;

		BakedGeoModel accModel = getGeoModel().getBakedModel(ACCESORIES_MODEL);
		if (accModel == null) return;

		String pothalaColor = stack.getDescriptionId().contains("green") ? "green" : "yellow";
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

	private void renderScouter(PoseStack poseStack, T animatable, BakedGeoModel playerModel, MultiBufferSource bufferSource, float partialTick, int packedLight, ItemStack stack) {
		if (stack.isEmpty()) return;

		Item item = stack.getItem();
		String color = null;

		if (item == MainItems.GREEN_SCOUTER.get()) color = "green";
		else if (item == MainItems.RED_SCOUTER.get()) color = "red";
		else if (item == MainItems.BLUE_SCOUTER.get()) color = "blue";
		else if (item == MainItems.PURPLE_SCOUTER.get()) color = "purple";

		if (color == null) return;

		BakedGeoModel accModel = getGeoModel().getBakedModel(SCOUTER_MODEL);
		if (accModel == null) return;

		RenderType accRenderType = RenderType.entityTranslucentCull(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + color + "_scouter.png"));

		accModel.getBone("radar").ifPresent(bone -> {
			syncTargetBoneAndParents(bone, playerModel);
			renderTargetedBone(bone, poseStack, bufferSource, animatable, accRenderType, 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight);
		});
	}

	private void renderWeights(PoseStack poseStack, T animatable, MultiBufferSource bufferSource, String anchor, float partialTick, int packedLight, float alpha) {
		for (int i = 0; i < getCuriosSlotSize(animatable, "weights"); i++) {
			ItemStack weightStack = getRenderableCurio(animatable, "weights", i);
			renderWeightedItems(poseStack, animatable, bufferSource, anchor, partialTick, packedLight, alpha, weightStack);

			ItemStack cosmeticWeightStack = getRenderableCosmetic(animatable, "weights", i);
			renderWeightedItems(poseStack, animatable, bufferSource, anchor, partialTick, packedLight, alpha, cosmeticWeightStack);
		}
	}

	private void renderWeightedItems(PoseStack poseStack, T animatable, MultiBufferSource bufferSource, String anchor, float partialTick, int packedLight, float alpha, ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof WeightItem weightItem)) return;

		BakedGeoModel weightModel = getGeoModel().getBakedModel(WEIGHTED_ITEMS_MODEL);
		if (weightModel == null) return;

		RenderType type = RenderType.entityCutoutNoCull(WEIGHTED_ITEMS_TEXTURE);

		switch (weightItem.getWeightType()) {
			case TURTLE_SHELL -> {
				if ("body".equals(anchor)) {
					renderWeightBone(weightModel, "turtleweight", poseStack, animatable, bufferSource, type, partialTick, packedLight, alpha);
				}
			}
			case WORKOUT_WEIGHTS -> {
				switch (anchor) {
					case "right_arm" -> renderWeightBone(weightModel, "right_arm_glove", poseStack, animatable, bufferSource, type, partialTick, packedLight, alpha);
					case "left_arm" -> renderWeightBone(weightModel, "left_arm_glove", poseStack, animatable, bufferSource, type, partialTick, packedLight, alpha);
					case "right_leg" -> renderWeightBone(weightModel, "right_leg_glove", poseStack, animatable, bufferSource, type, partialTick, packedLight, alpha);
					case "left_leg" -> renderWeightBone(weightModel, "left_leg_glove", poseStack, animatable, bufferSource, type, partialTick, packedLight, alpha);
				}
			}
			case PICCOLO_CAPE -> {
				switch (anchor) {
					case "body" -> {
						GeoBone capeBone = weightModel.getBone("cape").orElse(null);
						boolean wasHidden = capeBone != null && capeBone.isHidden();
						if (capeBone != null) capeBone.setHidden(true);

						renderWeightBone(weightModel, "piccoloweight_middle", poseStack, animatable, bufferSource, type, partialTick, packedLight, alpha);

						if (capeBone != null) capeBone.setHidden(wasHidden);
					}
					case "right_arm" -> renderWeightBone(weightModel, "piccoloweight_right", poseStack, animatable, bufferSource, type, partialTick, packedLight, alpha);
					case "left_arm" -> renderWeightBone(weightModel, "piccoloweight_left", poseStack, animatable, bufferSource, type, partialTick, packedLight, alpha);
				}
			}
		}
	}

	private void renderWeightBone(BakedGeoModel weightModel, String boneName, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, RenderType type, float partialTick, int packedLight, float alpha) {
		weightModel.getBone(boneName).ifPresent(bone -> renderTargetedBone(bone, poseStack, bufferSource, animatable, type, 1.0f, 1.0f, 1.0f, alpha, partialTick, packedLight));
	}

	private void renderSword(PoseStack poseStack, T animatable, GeoBone playerBodyBone, MultiBufferSource bufferSource, float partialTick, int packedLight) {
		var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
		var stats = statsCap.orElse(new StatsData(animatable));

		if (!stats.getStatus().isHasCreatedCharacter()) return;
		if (stats.getCharacter().getActiveFormData() != null && stats.getCharacter().getActiveForm().contains("ozaru")) return;

		if (stats.getStatus().isRenderKatana()) {
			BakedGeoModel yajirobeModel = getGeoModel().getBakedModel(YAJIROBE_SWORD_MODEL);
			if (yajirobeModel != null) {
				RenderType type = RenderType.entityCutoutNoCull(YAJIROBE_SWORD_TEXTURE);
				renderWeaponFromBodyAnchor(yajirobeModel, "katana", playerBodyBone, poseStack, bufferSource, animatable, type, partialTick, packedLight, 1.0f);
			}
		}

		if (stats.getStatus().getBackWeapon() == null || stats.getStatus().getBackWeapon().isEmpty()) return;

		if (stats.getStatus().getBackWeapon().equals(MainItems.POWER_POLE.get().getDescriptionId())) {
			BakedGeoModel powerpole = getGeoModel().getBakedModel(POWER_POLE_MODEL);
			if (powerpole != null) {
				RenderType type = RenderType.entityCutoutNoCull(POWER_POLE_TEXTURE);
				renderWeaponFromBodyAnchor(powerpole, "baculo", playerBodyBone, poseStack, bufferSource, animatable, type, partialTick, packedLight, 1.0f);
			}
		} else if (stats.getStatus().getBackWeapon().equals(MainItems.Z_SWORD.get().getDescriptionId())) {
			BakedGeoModel zModel = getGeoModel().getBakedModel(Z_SWORD_MODEL);
			if (zModel != null) {
				RenderType type = RenderType.entityCutoutNoCull(Z_SWORD_TEXTURE);
				renderWeaponFromBodyAnchor(zModel, "espada", playerBodyBone, poseStack, bufferSource, animatable, type, partialTick, packedLight, 1.0f);
			}
		} else if (stats.getStatus().getBackWeapon().equals(MainItems.BRAVE_SWORD.get().getDescriptionId())) {
			BakedGeoModel braveModel = getGeoModel().getBakedModel(BRAVE_SWORD_MODEL);
			if (braveModel != null) {
				RenderType type = RenderType.entityCutoutNoCull(BRAVE_SWORD_TEXTURE);
				renderWeaponFromBodyAnchor(braveModel, "espadatrunks", playerBodyBone, poseStack, bufferSource, animatable, type, partialTick, packedLight, 0.9f);
			}
		}
	}

	private void renderWeaponFromBodyAnchor(BakedGeoModel weaponModel, String anchorBoneName, GeoBone playerBodyBone, PoseStack poseStack, MultiBufferSource bufferSource, T animatable, RenderType type, float partialTick, int packedLight, float scale) {
		GeoBone weaponAnchor = weaponModel.getBone(anchorBoneName).orElse(null);
		if (weaponAnchor == null) return;

		syncBoneAndParentsByHierarchy(weaponAnchor.getParent(), playerBodyBone);

		poseStack.pushPose();
		if (scale != 1.0f) poseStack.scale(scale, scale, scale);

		VertexConsumer vertexConsumer = bufferSource.getBuffer(type);
		getRenderer().renderRecursively(poseStack, animatable, weaponAnchor, type, bufferSource, vertexConsumer, true, partialTick, packedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);

		poseStack.popPose();
	}

	private void syncBoneAndParentsByHierarchy(GeoBone destBone, GeoBone sourceBone) {
		GeoBone currentDest = destBone;
		GeoBone currentSource = sourceBone;
		while (currentDest != null && currentSource != null) {
			copyBoneData(currentSource, currentDest);
			currentDest = currentDest.getParent();
			currentSource = currentSource.getParent();
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

	private ItemStack getRenderableCurio(T animatable, String slotId, int index) {
		var inventory = CuriosApi.getCuriosInventory(animatable).orElse(null);
		if (inventory == null) return ItemStack.EMPTY;

		var stacksHandler = inventory.getCurios().get(slotId);
		if (stacksHandler == null) return ItemStack.EMPTY;

		if (!stacksHandler.getRenders().get(index)) return ItemStack.EMPTY;

		return stacksHandler.getStacks().getStackInSlot(index);
	}

	private ItemStack getRenderableCosmetic(T animatable, String slotId, int index) {
		var inventory = CuriosApi.getCuriosInventory(animatable).orElse(null);
		if (inventory == null) return ItemStack.EMPTY;

		var stacksHandler = inventory.getCurios().get(slotId);
		if (stacksHandler == null) return ItemStack.EMPTY;

		if (!stacksHandler.getRenders().get(index)) return ItemStack.EMPTY;

		return stacksHandler.getCosmeticStacks().getStackInSlot(index);
	}

	private int getCuriosSlotSize(T animatable, String slotId) {
		var inventory = CuriosApi.getCuriosInventory(animatable).orElse(null);
		if (inventory == null) return 0;

		var stacksHandler = inventory.getCurios().get(slotId);
		if (stacksHandler == null) return 0;

		return stacksHandler.getSlots();
	}
}