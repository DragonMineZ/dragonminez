package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.dragonminez.common.util.lists.SaiyanForms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Objects;

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

    public DMZRacePartsLayer(GeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel playerModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(null);
        if (stats == null) return;

        renderRaceParts(poseStack, animatable, playerModel, bufferSource, stats, partialTick, packedLight);
        renderAccessories(poseStack, animatable, playerModel, bufferSource, partialTick, packedLight);
		renderScouter(poseStack, animatable, playerModel, bufferSource, partialTick, packedLight);
        renderSword(poseStack, animatable, playerModel, bufferSource, partialTick, packedLight);
    }

    private void renderRaceParts(PoseStack poseStack, T animatable, BakedGeoModel playerModel, MultiBufferSource bufferSource, StatsData stats, float partialTick, int packedLight) {
        BakedGeoModel partsModel = getGeoModel().getBakedModel(RACES_PARTS_MODEL);
        if (partsModel == null) return;

        resetModelParts(partsModel);
        float[] renderColor = setupPartsAndColor(partsModel, stats);

        if (renderColor != null) {
            syncModelToPlayer(partsModel, playerModel);
            RenderType partsRenderType = RenderType.entityCutoutNoCull(RACES_PARTS_TEXTURE);

            int phase = stats.getStatus().getActiveKaiokenPhase();
            float[] tintedColor = applyKaiokenTint(renderColor[0], renderColor[1], renderColor[2], phase);

            poseStack.pushPose();
            getRenderer().reRender(partsModel, poseStack, bufferSource, animatable, partsRenderType,
                    bufferSource.getBuffer(partsRenderType), partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                    tintedColor[0], tintedColor[1], tintedColor[2], 1.0f);
            poseStack.popPose();
        }
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

        resetModelParts(accModel);

        if (hasPothalaRight) accModel.getBone("pothala_right").ifPresent(this::showBoneChain);
        if (hasPothalaLeft) accModel.getBone("pothala_left").ifPresent(this::showBoneChain);

		if (isFused) {
			accModel.getBone("pothala_right").ifPresent(this::showBoneChain);
			accModel.getBone("pothala_left").ifPresent(this::showBoneChain);
		}

        syncModelToPlayer(accModel, playerModel);

		String pothalaColor = animatable.getItemBySlot(EquipmentSlot.HEAD).getItem().getDescriptionId().contains("green") ? "green" : "yellow";
        RenderType accRenderType = RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + pothalaColor + "pothala.png"));

        poseStack.pushPose();
        getRenderer().reRender(accModel, poseStack, bufferSource, animatable, accRenderType,
                bufferSource.getBuffer(accRenderType), partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();
    }

    private void renderScouter(PoseStack poseStack, T animatable, BakedGeoModel playerModel, MultiBufferSource bufferSource, float partialTick, int packedLight) {

        ItemStack headStack = animatable.getItemBySlot(EquipmentSlot.HEAD);
        Item headItem = headStack.getItem();

        String color = null;

        if (headItem == MainItems.GREEN_SCOUTER.get()) {
            color = "green";
        } else if (headItem == MainItems.RED_SCOUTER.get()) {
            color = "red";
        } else if (headItem == MainItems.BLUE_SCOUTER.get()) {
            color = "blue";
        } else if (headItem == MainItems.PURPLE_SCOUTER.get()) {
            color = "purple";
        }

        if (color == null) return;

        BakedGeoModel accModel = getGeoModel().getBakedModel(SCOUTER_MODEL);
        if (accModel == null) return;

        resetModelParts(accModel);

        accModel.getBone("radar").ifPresent(this::showBoneChain);

        syncModelToPlayer(accModel, playerModel);

        RenderType accRenderType = RenderType.entityTranslucent(
                ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + color + "_scouter.png")
        );

        poseStack.pushPose();
        getRenderer().reRender(accModel, poseStack, bufferSource, animatable, accRenderType,
                bufferSource.getBuffer(accRenderType), partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();
    }

    private void renderSword(PoseStack poseStack, T animatable, BakedGeoModel playerModel, MultiBufferSource bufferSource, float partialTick, int packedLight) {
		var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
		var stats = statsCap.orElse(new StatsData(animatable));

		if (!stats.getStatus().hasCreatedCharacter()) return;

        if (stats.getStatus().isRenderKatana()) {
            BakedGeoModel yajirobeModel = getGeoModel().getBakedModel(YAJIROBE_SWORD_MODEL);
            if (yajirobeModel != null) {
                RenderType type = RenderType.entityCutoutNoCull(YAJIROBE_SWORD_TEXTURE);

                syncModelToPlayer(yajirobeModel, playerModel);

                poseStack.pushPose();
                getRenderer().reRender(yajirobeModel, poseStack, bufferSource, animatable, type,
                        bufferSource.getBuffer(type), partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                        1.0f, 1.0f, 1.0f, 1.0f);
                poseStack.popPose();
            }
        }

		if (stats.getStatus().getBackWeapon() == null || stats.getStatus().getBackWeapon().isEmpty()) return;

        if (stats.getStatus().getBackWeapon().equals(MainItems.POWER_POLE.get().getDescriptionId())) {
            BakedGeoModel powerpole = getGeoModel().getBakedModel(POWER_POLE_MODEL);
            if (powerpole != null) {
                RenderType type = RenderType.entityCutoutNoCull(POWER_POLE_TEXTURE);

                syncModelToPlayer(powerpole, playerModel);

                poseStack.pushPose();
                getRenderer().reRender(powerpole, poseStack, bufferSource, animatable, type,
                        bufferSource.getBuffer(type), partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                        1.0f, 1.0f, 1.0f, 1.0f);
                poseStack.popPose();
            }
        } else if (stats.getStatus().getBackWeapon().equals(MainItems.Z_SWORD.get().getDescriptionId())) {
            BakedGeoModel zModel = getGeoModel().getBakedModel(Z_SWORD_MODEL);
            if (zModel != null) {
                RenderType type = RenderType.entityCutoutNoCull(Z_SWORD_TEXTURE);

                syncModelToPlayer(zModel, playerModel);

                poseStack.pushPose();
                getRenderer().reRender(zModel, poseStack, bufferSource, animatable, type,
                        bufferSource.getBuffer(type), partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                        1.0f, 1.0f, 1.0f, 1.0f);
                poseStack.popPose();
            }
		} else if (stats.getStatus().getBackWeapon().equals(MainItems.BRAVE_SWORD.get().getDescriptionId())) {
            BakedGeoModel braveModel = getGeoModel().getBakedModel(BRAVE_SWORD_MODEL);
            if (braveModel != null) {
                RenderType type = RenderType.entityCutoutNoCull(BRAVE_SWORD_TEXTURE);

                for (GeoBone bone : braveModel.topLevelBones()) {
                    setHiddenRecursive(bone, false);
                }

                syncModelToPlayer(braveModel, playerModel);

                poseStack.pushPose();

                float scale = 0.9f;
                poseStack.scale(scale, scale, scale);

                getRenderer().reRender(braveModel, poseStack, bufferSource, animatable, type,
                        bufferSource.getBuffer(type), partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                        1.0f, 1.0f, 1.0f, 1.0f);
                poseStack.popPose();
            }
        }
    }

    private float[] setupPartsAndColor(BakedGeoModel partsModel, StatsData stats) {
        var character = stats.getCharacter();
        String race = character.getRaceName().toLowerCase();
        String currentForm = character.getActiveForm();
        boolean hasForm = (currentForm != null && !currentForm.isEmpty() && !currentForm.equals("base"));

        if (race.equals("saiyan") && !stats.getStatus().isTailVisible() &&
                !Objects.equals(currentForm, SaiyanForms.OOZARU) &&
                !Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU)) {

            setupSaiyanParts(partsModel);
            float[] color = ColorUtils.hexToRgb("#572117");

            if (hasForm && character.getActiveFormData() != null) {
                String formHair = character.getActiveFormData().getHairColor();
                if (formHair != null && !formHair.isEmpty()) {
                    color = ColorUtils.hexToRgb(formHair);
                }
            }
            return color;
        }

        if (race.equals("frostdemon")) {
            setupFrostDemonParts(partsModel, currentForm);
            return ColorUtils.rgbIntToFloat(0x1A1A1A);
        }

        if (race.equals("namekian") || race.equals("majin")) {
            if (race.equals("namekian")) setupNamekianParts(partsModel, character.getHairId());
            else setupMajinParts(partsModel, character.getGender().toLowerCase(), character.getHairId());

            float[] color = ColorUtils.hexToRgb(character.getBodyColor());
            if (hasForm && character.getActiveFormData() != null) {
                String formBody = character.getActiveFormData().getBodyColor1();
                if (formBody != null && !formBody.isEmpty()) color = ColorUtils.hexToRgb(formBody);
            }
            return color;
        }

        return null;
    }

    private void resetModelParts(BakedGeoModel model) {
        for (GeoBone bone : model.topLevelBones()) {
            setHiddenRecursive(bone, true);
        }
    }

    private void syncModelToPlayer(BakedGeoModel partsModel, BakedGeoModel playerModel) {
        for (GeoBone partBone : partsModel.topLevelBones()) {
            syncBoneRecursively(partBone, playerModel);
        }
    }

    private void syncBoneRecursively(GeoBone destBone, BakedGeoModel sourceModel) {
        sourceModel.getBone(destBone.getName()).ifPresent(sourceBone -> {
            destBone.setRotX(sourceBone.getRotX());
            destBone.setRotY(sourceBone.getRotY());
            destBone.setRotZ(sourceBone.getRotZ());
            destBone.setPosX(sourceBone.getPosX());
            destBone.setPosY(sourceBone.getPosY());
            destBone.setPosZ(sourceBone.getPosZ());
            destBone.setPivotX(sourceBone.getPivotX());
            destBone.setPivotY(sourceBone.getPivotY());
            destBone.setPivotZ(sourceBone.getPivotZ());
            destBone.setScaleX(sourceBone.getScaleX());
            destBone.setScaleY(sourceBone.getScaleY());
            destBone.setScaleZ(sourceBone.getScaleZ());
        });

        for (GeoBone child : destBone.getChildBones()) {
            syncBoneRecursively(child, sourceModel);
        }
    }

    private void setupSaiyanParts(BakedGeoModel partsModel) {
        partsModel.getBone("tailenrolled").ifPresent(this::showBoneChain);
    }

    private void setupNamekianParts(BakedGeoModel partsModel, int hairType) {
        partsModel.getBone("antenas").ifPresent(this::showBoneChain);
        if (hairType <= 2) {
            partsModel.getBone("orejas" + (hairType + 1)).ifPresent(this::showBoneChain);
        }
    }

    private void setupFrostDemonParts(BakedGeoModel partsModel, String currentForm) {
        if (currentForm == null || currentForm.isEmpty() || currentForm.equalsIgnoreCase("base")) {
            partsModel.getBone("cuernos").ifPresent(this::showBoneChain);
        } else if (Objects.equals(currentForm, FrostDemonForms.SECOND_FORM)) {
            partsModel.getBone("cuernos2").ifPresent(this::showBoneChain);
        }
    }

    private void setupMajinParts(BakedGeoModel partsModel, String gender, int hairType) {
        String earName = (gender.contains("female") || gender.contains("mujer")) ? "orejas3" :
                (hairType == 0 ? "orejas3" : (hairType == 1 ? "orejas1" : "orejas2"));

        partsModel.getBone("colamajin").ifPresent(this::showBoneChain);
        partsModel.getBone(earName).ifPresent(this::showBoneChain);
    }

    private void showBoneChain(GeoBone bone) {
        bone.setHidden(false);
        for (GeoBone child : bone.getChildBones()) {
            setHiddenRecursive(child, false);
        }
        GeoBone parent = bone.getParent();
        while (parent != null) {
            parent.setHidden(false);
            parent = parent.getParent();
        }
    }

    private void setHiddenRecursive(GeoBone bone, boolean hidden) {
        bone.setHidden(hidden);
        for (GeoBone child : bone.getChildBones()) {
            setHiddenRecursive(child, hidden);
        }
    }

    private float[] applyKaiokenTint(float r, float g, float b, int phase) {
        if (phase <= 0) return new float[]{r, g, b};

        float intensity = Math.min(0.6f, phase * 0.1f);

        float newR = r * (1.0f - intensity) + (1.0f * intensity);
        float newG = g * (1.0f - intensity);
        float newB = b * (1.0f - intensity);

        return new float[]{newR, newG, newB};
    }
}