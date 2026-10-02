package com.dragonminez.client.render.layer;

import com.dragonminez.client.init.entities.renderer.sagas.SaiyanInvaderRenderer;
import com.dragonminez.Reference;
import com.dragonminez.client.render.HeadPortraitRenderer;
import com.dragonminez.client.render.hair.SaiyanTailMeshBuilder;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.client.render.util.DonorBoneRenderer;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.SaiyanTailRules;
import com.dragonminez.client.util.SkinGathererProvider;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.item.WeightItem;
import com.dragonminez.common.stats.FusedData;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.util.FusionTraits;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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

import java.util.ArrayList;
import java.util.List;

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

	private static  float BRAVE_BACK_X = 0.7F;
	private static  float BRAVE_BACK_Y = 2.0F;
	private static  float BRAVE_BACK_Z = 0.15F;
	private static  float BRAVE_BACK_ROT_X = 0.0F;
	private static  float BRAVE_BACK_ROT_Y = 0.0F;
	private static  float BRAVE_BACK_ROT_Z = 135.0f;
	private static  float BRAVE_BACK_SCALE = 0.9F;
	private static final ResourceLocation POWER_POLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/power_pole.png");

	private static final ResourceLocation WEIGHTED_ITEMS_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/weighted_items.geo.json");
	private static final ResourceLocation WEIGHTED_ITEMS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/weighted_items.png");

	private static final ResourceLocation SAIYAN_TAIL_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/human.geo.json");
	private static final ResourceLocation FROST_DEMON_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon.geo.json");
	private static final ResourceLocation BIO_ANDROID_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid.geo.json");
	private static final float[] HORN_COLOR = ColorUtils.hexToRgb("#1A1A1A");
	private static final float PARTS_AURA_TINT = 0.4f;
	private static final float BODY_AURA_TINT = 0.2f;

	private final SaiyanTailMeshBuilder tailMesh = new SaiyanTailMeshBuilder();

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
				|| stats.getStatus().isPermanentAura() || stats.getStatus().isForcedAura();
		AuraTintTracker.update(playerId, gameTime, shouldFadeIn);
	}

	@Override
	public void renderForBone(PoseStack poseStack, T animatable, GeoBone playerBone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		String anchor = playerBone.getName();
		boolean isLimb = "right_arm".equals(anchor) || "left_arm".equals(anchor)
				|| "right_leg".equals(anchor) || "left_leg".equals(anchor);
		if (!"head".equals(anchor) && !"body".equals(anchor) && !isLimb) return;

		if (animatable.hasEffect(MainEffects.CANDY.get())) return;

		if (!HeadPortraitRenderer.isActive() && FirstPersonManager.shouldRenderFirstPerson(animatable)) {
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

		renderRacePartsForAnchor(poseStack, animatable, playerModel, bufferSource, stats, anchor, partialTick, packedLight, packedOverlay, alpha, tintProgress);

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

	private void renderRacePartsForAnchor(PoseStack poseStack, T animatable, BakedGeoModel playerModel, MultiBufferSource bufferSource, StatsData stats, String anchor, float partialTick, int packedLight, int packedOverlay, float alpha, float tintProgress) {
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
		FormConfig.FormData tintForm = DMZSkinLayer.resolveTintForm(stats);
		float[] formTintColor = tintForm != null ? tintForm.getRgbTintColor() : null;
		float formTintIntensity = tintForm != null ? (float) tintForm.getTintIntensity() : 0.0f;
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
				var nextForm = TransformationsHelper.presentNextForm(stats);
				if (nextForm != null && !nextForm.getHairColor().isEmpty()) {
					float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					accessoryColor = DMZSkinLayer.lerpColor(factor, accessoryColor, nextForm.getRgbHairColor());
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				var nextForm = TransformationsHelper.presentNextStackForm(stats);
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

						float[] tintedColor = applyAuraTint(colorToTint[0], colorToTint[1], colorToTint[2], formTintColor, formTintIntensity, topAuraColor, tintProgress);

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
					float[] tintedColor = applyAuraTint(antennaColor[0], antennaColor[1], antennaColor[2], formTintColor, formTintIntensity, topAuraColor, tintProgress);
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

					float[] tintedColor = applyAuraTint(majinBodyColor[0], majinBodyColor[1], majinBodyColor[2], formTintColor, formTintIntensity, topAuraColor, tintProgress);
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

			if ((isSaiyanLogic || hasSaiyanTail) && !stats.getStatus().isTailVisible() && SaiyanTailRules.hasTail(character)) {
				RenderType tailRenderType = RenderType.entityTranslucentCull(RACES_PARTS_TEXTURE);
				partsModel.getBone("tailenrolled").ifPresent(targetBone -> {
					syncTargetBoneAndParents(targetBone, playerModel);
					float[] tailColor = resolveTailColor(stats);
					float[] tintedColor = applyAuraTint(tailColor[0], tailColor[1], tailColor[2], formTintColor, formTintIntensity, topAuraColor, tintProgress);
					renderTargetedBone(targetBone, poseStack, bufferSource, animatable, tailRenderType, tintedColor[0], tintedColor[1], tintedColor[2], alpha, partialTick, packedLight);
				});
			}
		}

		FusedData fused = stats.getFusedData();
		if (fused != null) {
			renderFusionParts(poseStack, animatable, playerModel, partsModel, bufferSource, stats, fused, anchor, partialTick, packedLight, packedOverlay, alpha,
					formTintColor, formTintIntensity, topAuraColor, tintProgress, accessoryColor);
		}
	}

	private void renderFusionParts(PoseStack poseStack, T animatable, BakedGeoModel playerModel, BakedGeoModel partsModel, MultiBufferSource bufferSource,
								   StatsData stats, FusedData fused, String anchor, float partialTick, int packedLight, int packedOverlay, float alpha,
								   float[] formTint, float formTintIntensity, float[] auraColor, float tintProgress, float[] accessoryColor) {
		boolean body = anchor.equals("body");
		float[][] bodyColors = null;
		for (FusionTraits.Part part : fused.traitPlan(stats.getCharacter()).borrowed()) {
			if (part.onBody() != body) continue;
			switch (part.source()) {
				case RACE_PARTS -> {
					GeoBone bone = partsModel.getBone(part.bone()).orElse(null);
					if (bone == null) continue;
					syncTargetBoneAndParents(bone, playerModel);
					float[] color = part.category() == FusionTraits.Category.HORNS ? HORN_COLOR
							: tint(part.category() == FusionTraits.Category.OTHER ? accessoryColor : resolveBodyColor1(stats), formTint, formTintIntensity, auraColor, tintProgress, PARTS_AURA_TINT);
					renderTargetedBone(bone, poseStack, bufferSource, animatable, RenderType.entityTranslucent(RACES_PARTS_TEXTURE), color[0], color[1], color[2], alpha, partialTick, packedLight);
				}
				case SAIYAN_TAIL -> renderBorrowedSaiyanTail(poseStack, animatable, playerModel, partsModel, bufferSource, stats, part.leader(), partialTick, packedLight, packedOverlay, alpha, formTint, formTintIntensity, auraColor, tintProgress);
				case FROST_DEMON, BIO_ANDROID -> {
					if (bodyColors == null) bodyColors = SkinGathererProvider.resolveBodyColors(stats);
					renderBorrowedModelPart(poseStack, animatable, playerModel, bufferSource, stats, fused, part, bodyColors, partialTick, packedLight, packedOverlay, alpha, formTint, formTintIntensity, auraColor, tintProgress);
				}
			}
		}
	}

	private void renderBorrowedSaiyanTail(PoseStack poseStack, T animatable, BakedGeoModel playerModel, BakedGeoModel partsModel, MultiBufferSource bufferSource, StatsData stats, boolean leaderTail,
										  float partialTick, int packedLight, int packedOverlay, float alpha, float[] formTint, float formTintIntensity, float[] auraColor, float tintProgress) {
		float[] tailColor = resolveTailColor(stats);
		if (stats.getStatus().isTailVisible()) {
			float[] color = tint(tailColor, formTint, formTintIntensity, auraColor, tintProgress, BODY_AURA_TINT);
			tailMesh.reset();
			DonorBoneRenderer.visit(animatable, playerModel, SAIYAN_TAIL_MODEL, "tail1", true, poseStack, partialTick, (bone, stack) -> {
				if (SaiyanTailMeshBuilder.isTailBone(bone.getName())) tailMesh.add(bone, stack.last().pose(), stack.last().normal());
			});
			RenderType type = alpha < 1.0f ? RenderType.entityTranslucent(SaiyanTailMeshBuilder.TEXTURE) : RenderType.entityCutoutNoCull(SaiyanTailMeshBuilder.TEXTURE);
			tailMesh.emit(bufferSource.getBuffer(type), color, packedLight, packedOverlay, alpha, DMZHairLayer.wantsPixelDetail(animatable));
			return;
		}
		if (leaderTail) return;
		GeoBone enrolled = partsModel.getBone("tailenrolled").orElse(null);
		if (enrolled == null) return;
		syncTargetBoneAndParents(enrolled, playerModel);
		float[] color = tint(tailColor, formTint, formTintIntensity, auraColor, tintProgress, PARTS_AURA_TINT);
		renderTargetedBone(enrolled, poseStack, bufferSource, animatable, RenderType.entityTranslucentCull(RACES_PARTS_TEXTURE), color[0], color[1], color[2], alpha, partialTick, packedLight);
	}

	private void renderBorrowedModelPart(PoseStack poseStack, T animatable, BakedGeoModel playerModel, MultiBufferSource bufferSource, StatsData stats, FusedData fused, FusionTraits.Part part,
										 float[][] bodyColors, float partialTick, int packedLight, int packedOverlay, float alpha, float[] formTint, float formTintIntensity, float[] auraColor, float tintProgress) {
		boolean frost = part.source() == FusionTraits.Source.FROST_DEMON;
		List<DonorBoneRenderer.Layer> layers = new ArrayList<>();
		SkinGathererProvider.BodyLayerSink sink = new SkinGathererProvider.BodyLayerSink() {
			@Override
			public void base(ResourceLocation texture, float[] color) {
				add(alpha < 1.0f ? RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture), color, 1.0f);
			}

			@Override
			public void overlay(ResourceLocation texture, float[] color) {
				add(alpha < 1.0f ? ModRenderTypes.skinOverlayTranslucent(texture) : ModRenderTypes.skinOverlayCutout(texture), color, 1.0f);
			}

			@Override
			public void translucent(ResourceLocation texture, float[] color) {
				add(RenderType.entityTranslucent(texture), color, 1.0f);
			}

			@Override
			public void fading(String layerId, ResourceLocation texture, float[] color, float targetAlpha) {
				add(alpha < 1.0f || targetAlpha < 1.0f ? ModRenderTypes.skinOverlayTranslucent(texture) : ModRenderTypes.skinOverlayCutout(texture), color, targetAlpha);
			}

			private void add(RenderType type, float[] color, float opacity) {
				if (opacity <= 0.001f) return;
				float[] tinted = tint(color, formTint, formTintIntensity, auraColor, tintProgress, BODY_AURA_TINT);
				layers.add(new DonorBoneRenderer.Layer(type, tinted[0], tinted[1], tinted[2], alpha * opacity));
			}
		};
		int bodyType = part.leader() ? stats.getCharacter().getBodyType() : fused.getPartnerBodyType();
		String gender = part.leader() ? stats.getCharacter().getGender() : fused.getPartnerGender();
		SkinGathererProvider.INSTANCE.gatherDonorLayers(frost ? "frostdemon" : "bioandroid", bodyType, gender, bodyColors, sink);
		DonorBoneRenderer.render(getRenderer(), animatable, playerModel, frost ? FROST_DEMON_MODEL : BIO_ANDROID_MODEL, part.bone(),
				part.category() == FusionTraits.Category.TAIL, layers, poseStack, bufferSource, partialTick, packedLight, packedOverlay);
	}

	private float[] resolveTailColor(StatsData stats) {
		var character = stats.getCharacter();
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
				var nextForm = TransformationsHelper.presentNextForm(stats);
				if (nextForm != null && !nextForm.getBodyColor2().isEmpty()) {
					float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					tailColor = DMZSkinLayer.lerpColor(factor, tailColor, nextForm.getRgbBodyColor2());
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				var nextForm = TransformationsHelper.presentNextStackForm(stats);
				if (nextForm != null && !nextForm.getBodyColor2().isEmpty()) {
					float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					tailColor = DMZSkinLayer.lerpColor(factor, tailColor, nextForm.getRgbBodyColor2());
				}
			}
		}
		return tailColor;
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
				var nextForm = TransformationsHelper.presentNextForm(stats);
				if (nextForm != null && !nextForm.getBodyColor1().isEmpty()) {
					color = DMZSkinLayer.lerpColor(factor, color, nextForm.getRgbBodyColor1());
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				var nextForm = TransformationsHelper.presentNextStackForm(stats);
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

	private float[] applyAuraTint(float r, float g, float b, float[] formTintColor, float formTintIntensity, float[] auraColor, float tintProgress) {
		return applyAuraTint(r, g, b, formTintColor, formTintIntensity, auraColor, tintProgress, PARTS_AURA_TINT);
	}

	private float[] tint(float[] rgb, float[] formTintColor, float formTintIntensity, float[] auraColor, float tintProgress, float auraStrength) {
		return applyAuraTint(rgb[0], rgb[1], rgb[2], formTintColor, formTintIntensity, auraColor, tintProgress, auraStrength);
	}

	private float[] applyAuraTint(float r, float g, float b, float[] formTintColor, float formTintIntensity, float[] auraColor, float tintProgress, float auraStrength) {
		boolean hasFormTint = formTintIntensity > 0.0f && formTintColor != null;
		float intensity = hasFormTint ? Mth.clamp(formTintIntensity, 0.0f, 1.0f) : auraStrength * tintProgress;
		intensity *= AuraTintTracker.darkTintScale(r, g, b);

		if (intensity <= 0.001f) return new float[]{r, g, b};

		float[] target = hasFormTint ? formTintColor : auraColor;
		float newR = r * (1.0f - intensity) + (target[0] * intensity);
		float newG = g * (1.0f - intensity) + (target[1] * intensity);
		float newB = b * (1.0f - intensity) + (target[2] * intensity);

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
		if (animatable instanceof SaiyanInvaderRenderer.Puppet puppet) {
			renderScouter(poseStack, animatable, playerModel, bufferSource, partialTick, packedLight, puppet.getScouter());
			return;
		}
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
		String headTechId = stack.getItem().getDescriptionId();
		boolean hasPothalaPair = headTechId.contains("pothala_pair");
		boolean hasPothalaRight = hasPothalaPair || headTechId.contains("pothala_right");
		boolean hasPothalaLeft = hasPothalaPair || headTechId.contains("pothala_left");

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

		RenderType accRenderType = ModRenderTypes.scouterLens(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + color + "_scouter.png"));

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

		String backWeapon = stats.getStatus().getBackWeapon();

		if (backWeapon == null || backWeapon.isEmpty()) return;

		// backWeapon only names the weapon the player owns. Whether it is in hand right now
		// is read straight off the entity, which costs no server round trip, so the sheath
		// never blinks out while a stale value catches up. Held items are synced for every
		// player, so this is correct for other players too.
		if (backWeapon.equals(MainItems.POWER_POLE.get().getDescriptionId())) {
			renderPowerPoleOnBack(poseStack, animatable, playerBodyBone, bufferSource, partialTick, packedLight, isHolding(animatable, MainItems.POWER_POLE.get()));
		} else if (backWeapon.equals(MainItems.Z_SWORD.get().getDescriptionId())) {
			// The Z Sword model carries no sheath bone, so it just leaves the back when drawn.
			if (!isHolding(animatable, MainItems.Z_SWORD.get())) {
				BakedGeoModel zModel = getGeoModel().getBakedModel(Z_SWORD_MODEL);
				if (zModel != null) {
					RenderType type = RenderType.entityCutoutNoCull(Z_SWORD_TEXTURE);
					renderWeaponFromBodyAnchor(zModel, "espada", playerBodyBone, poseStack, bufferSource, animatable, type, partialTick, packedLight, 1.0f);
				}
			}
		} else if (backWeapon.equals(MainItems.BRAVE_SWORD.get().getDescriptionId())) {
			renderBraveSwordOnBack(poseStack, animatable, playerBodyBone, bufferSource, partialTick, packedLight, isHolding(animatable, MainItems.BRAVE_SWORD.get()));
		}
	}

	private static boolean isHolding(AbstractClientPlayer player, Item item) {
		return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
	}

	/**
	 * Renders the Brave Sword on the player's back. With {@code scabbardOnly} the sword
	 * bone is hidden so only the scabbard draws, which is what stays behind while the
	 * sword itself is in hand.
	 */
	private void renderBraveSwordOnBack(PoseStack poseStack, T animatable, GeoBone playerBodyBone, MultiBufferSource bufferSource, float partialTick, int packedLight, boolean scabbardOnly) {
		BakedGeoModel braveModel = getGeoModel().getBakedModel(BRAVE_SWORD_MODEL);
		if (braveModel == null) return;

		GeoBone blade = scabbardOnly ? braveModel.getBone("espada").orElse(null) : null;
		boolean bladeWasHidden = blade != null && blade.isHidden();
		if (blade != null) blade.setHidden(true);

		RenderType type = RenderType.entityCutoutNoCull(BRAVE_SWORD_TEXTURE);
		poseStack.pushPose();
		poseStack.translate(BRAVE_BACK_X, BRAVE_BACK_Y, BRAVE_BACK_Z);
		if (BRAVE_BACK_ROT_X != 0.0F) poseStack.mulPose(Axis.XP.rotationDegrees(BRAVE_BACK_ROT_X));
		if (BRAVE_BACK_ROT_Y != 0.0F) poseStack.mulPose(Axis.YP.rotationDegrees(BRAVE_BACK_ROT_Y));
		if (BRAVE_BACK_ROT_Z != 0.0F) poseStack.mulPose(Axis.ZP.rotationDegrees(BRAVE_BACK_ROT_Z));
		renderWeaponFromBodyAnchor(braveModel, "espadatrunks", playerBodyBone, poseStack, bufferSource, animatable, type, partialTick, packedLight, BRAVE_BACK_SCALE);
		poseStack.popPose();

		if (blade != null) blade.setHidden(bladeWasHidden);
	}

	/**
	 * Renders the Power Pole on the player's back. With {@code holsterOnly} the pole bone
	 * is hidden so only the holster draws.
	 */
	private void renderPowerPoleOnBack(PoseStack poseStack, T animatable, GeoBone playerBodyBone, MultiBufferSource bufferSource, float partialTick, int packedLight, boolean holsterOnly) {
		BakedGeoModel powerpole = getGeoModel().getBakedModel(POWER_POLE_MODEL);
		if (powerpole == null) return;

		GeoBone pole = holsterOnly ? powerpole.getBone("palo").orElse(null) : null;
		boolean poleWasHidden = pole != null && pole.isHidden();
		if (pole != null) pole.setHidden(true);

		RenderType type = RenderType.entityCutoutNoCull(POWER_POLE_TEXTURE);
		renderWeaponFromBodyAnchor(powerpole, "baculo", playerBodyBone, poseStack, bufferSource, animatable, type, partialTick, packedLight, 1.0f);

		if (pole != null) pole.setHidden(poleWasHidden);
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