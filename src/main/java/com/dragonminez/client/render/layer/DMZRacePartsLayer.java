package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainItems;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

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
		var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(new StatsData(animatable));

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
	}

	@Override
	public void renderForBone(PoseStack poseStack, T animatable, GeoBone playerBone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		String anchor = playerBone.getName();
		if (!"head".equals(anchor) && !"body".equals(anchor)) return;

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
			return;
		}

		var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(new StatsData(animatable));
		float alpha = animatable.isSpectator() ? 0.15f : 1.0f;
		float tintProgress = AURA_TINT_PROGRESS.getOrDefault(animatable.getId(), 0.0f);

		BakedGeoModel playerModel = getGeoModel().getBakedModel(getGeoModel().getModelResource(animatable));

		renderRacePartsForAnchor(poseStack, animatable, playerModel, bufferSource, stats, anchor, partialTick, packedLight, alpha, tintProgress);

		if (!animatable.isSpectator()) {
			if ("head".equals(anchor) && !stats.getCharacter().isOozaruCached()) {
				renderAccessories(poseStack, animatable, playerModel, bufferSource, partialTick, packedLight);
				renderScouter(poseStack, animatable, playerModel, bufferSource, partialTick, packedLight);
			} else renderSword(poseStack, animatable, playerBone, bufferSource, partialTick, packedLight);
		}

		bufferSource.getBuffer(renderType);
	}

	private void renderRacePartsForAnchor(PoseStack poseStack, T animatable, BakedGeoModel playerModel, MultiBufferSource bufferSource, StatsData stats, String anchor, float partialTick, int packedLight, float alpha, float tintProgress) {
		var character = stats.getCharacter();
		String race = character.getRaceName().toLowerCase();
		String currentForm = character.getActiveForm();

		boolean isOozaru = currentForm != null && currentForm.toLowerCase().contains("oozaru");
		if (isOozaru) return;

		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(race);
		if (raceConfig == null) return;

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

				GeoBone targetBone = partsModel.getBone(activeBone).orElse(null);
				boolean fromPlayerModel = false;

				if (targetBone == null) {
					targetBone = playerModel.getBone(activeBone).orElse(null);
					fromPlayerModel = true;
				}

				if (targetBone != null) {
					if (!fromPlayerModel) {
						syncTargetBoneAndParents(targetBone, playerModel);
					}
					float[] tintedColor = applyAuraTint(accessoryColor[0], accessoryColor[1], accessoryColor[2], phase, topAuraColor, tintProgress);
					if (activeBone.contains("horn") && character.getRaceName().equals("frostdemon")) tintedColor = ColorUtils.hexToRgb("#1A1A1A");
					renderTargetedBone(targetBone, poseStack, bufferSource, animatable, partsRenderType, tintedColor[0], tintedColor[1], tintedColor[2], alpha, partialTick, packedLight);
				}
			}

			if (extraHeadBonesEnabled && (race.equals("namekian") || race.equals("namekian_orange"))) {

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
					float[] tintedColor = applyAuraTint(accessoryColor[0], accessoryColor[1], accessoryColor[2], phase, topAuraColor, tintProgress);
					renderTargetedBone(antennaBone, poseStack, bufferSource, animatable, partsRenderType, tintedColor[0], tintedColor[1], tintedColor[2], alpha, partialTick, packedLight);
				}
			}

            if (extraHeadBonesEnabled && race.equals("majin")) {
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
                    float[] tintedColor = applyAuraTint(accessoryColor[0], accessoryColor[1], accessoryColor[2], phase, topAuraColor, tintProgress);
                    renderTargetedBone(earsBone, poseStack, bufferSource, animatable, partsRenderType, tintedColor[0], tintedColor[1], tintedColor[2], alpha, partialTick, packedLight);
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

		RenderType accRenderType = RenderType.entityTranslucentCull(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + color + "_scouter.png"));

		accModel.getBone("radar").ifPresent(bone -> {
			syncTargetBoneAndParents(bone, playerModel);
			renderTargetedBone(bone, poseStack, bufferSource, animatable, accRenderType, 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight);
		});
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
}