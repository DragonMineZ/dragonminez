package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.data.DMZAnimatable;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class DMZRacePartsLayer extends GeoRenderLayer<DMZAnimatable> {

    private static final ResourceLocation RACES_PARTS_MODEL = new ResourceLocation(Reference.MOD_ID,
            "geo/entity/raceparts.geo.json");
    private static final ResourceLocation RACES_PARTS_MAJIN_FAT = new ResourceLocation(Reference.MOD_ID,
            "geo/entity/raceparts_majinfat.geo.json");
    private static final ResourceLocation RACES_PARTS_TEXTURE = new ResourceLocation(Reference.MOD_ID,
            "textures/entity/races/raceparts.png");

    public DMZRacePartsLayer(GeoRenderer<DMZAnimatable> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, DMZAnimatable animatable, BakedGeoModel playerModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        AbstractClientPlayer player = player();
        if (player == null) return;

        var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (stats == null) return;

        var character = stats.getCharacter();
        String race = character.getRaceName().toLowerCase();
        String gender = character.getGender().toLowerCase();
        int hairType = character.getHairId();

        boolean isSaiyan = race.equalsIgnoreCase("saiyan");
        boolean isTailEnrolled = stats.getStatus().isTailVisible();
        boolean isNamek = race.equalsIgnoreCase("namekian");
        boolean isMajin = race.equalsIgnoreCase("majin");
        boolean isFrostDemon = race.equalsIgnoreCase("frostdemon");
        boolean isMale = gender.equalsIgnoreCase("male");

        if (!((isSaiyan && isTailEnrolled) || isNamek || isMajin || isFrostDemon)) return;

        ResourceLocation modelLoc = (isMajin && isMale) ? RACES_PARTS_MAJIN_FAT
                : RACES_PARTS_MODEL;

        BakedGeoModel partsModel = getGeoModel().getBakedModel(modelLoc);
        if (partsModel == null) return;

        for (GeoBone bone : partsModel.topLevelBones()) {
            setHiddenRecursive(bone, true);
        }

        float[] renderColor = {1.0f, 1.0f, 1.0f};

        if (isSaiyan && isTailEnrolled) {
            setupSaiyanParts(partsModel);
            renderColor = ColorUtils.rgbIntToFloat(0x6B1E0E);
        }
        else if (isFrostDemon) {
            setupFrostDemon(partsModel);
            partsModel.getBone("body").ifPresent(b -> setHiddenRecursive(b, true));
            renderColor = ColorUtils.rgbIntToFloat(0x1A1A1A);
        }
        else {
            if (isNamek) setupNamekianParts(partsModel, hairType);
            else if (isMajin) setupMajinParts(partsModel, gender, hairType);

            partsModel.getBone("body").ifPresent(b -> setHiddenRecursive(b, true));
            renderColor = ColorUtils.hexToRgb(character.getBodyColor());
        }

        for (GeoBone partBone : partsModel.topLevelBones()) {
            syncBoneRecursively(partBone, playerModel);
        }

        RenderType partsRenderType = RenderType.entityCutoutNoCull(RACES_PARTS_TEXTURE);

        poseStack.pushPose();
        float baseScale = 1.0666667f;
        poseStack.scale(baseScale, baseScale, baseScale);

        getRenderer().reRender(partsModel, poseStack, bufferSource, animatable, partsRenderType,
                bufferSource.getBuffer(partsRenderType), partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                renderColor[0], renderColor[1], renderColor[2], 1.0f);

        poseStack.popPose();
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

    private void setupFrostDemon(BakedGeoModel partsModel) {
        partsModel.getBone("cuernos").ifPresent(this::showBoneChain);
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

    public AbstractClientPlayer player() {
        if (getRenderer() instanceof GeoReplacedEntityRenderer<?, ?> geoRenderer) {
            return (AbstractClientPlayer) geoRenderer.getCurrentEntity();
        }
        return null;
    }

    private void setHiddenRecursive(GeoBone bone, boolean hidden) {
        bone.setHidden(hidden);
        for (GeoBone child : bone.getChildBones()) {
            setHiddenRecursive(child, hidden);
        }
    }
}