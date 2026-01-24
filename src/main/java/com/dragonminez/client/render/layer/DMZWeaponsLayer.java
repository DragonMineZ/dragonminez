package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

public class DMZWeaponsLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {

    private static final ResourceLocation KI_WEAPONS_MODEL = new ResourceLocation(Reference.MOD_ID, "geo/entity/races/kiweapons.geo.json");
    private static final ResourceLocation KI_WEAPONS_TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/entity/races/kiweapons.png");

    public DMZWeaponsLayer(GeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel playerModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(null);
        if (stats == null || !stats.getSkills().isSkillActive("kimanipulation")) return;

        String weaponType = stats.getStatus().getKiWeaponType();
        if (weaponType == null || weaponType.equalsIgnoreCase("none")) return;

        BakedGeoModel weaponModel = getGeoModel().getBakedModel(KI_WEAPONS_MODEL);
        if (weaponModel == null) return;

        resetModelParts(weaponModel);

        HumanoidArm mainArm = animatable.getMainArm();
        boolean isMainHandRight = mainArm == HumanoidArm.RIGHT;

        String boneName = "";

        if (weaponType.equalsIgnoreCase("blade")) {
            boneName = isMainHandRight ? "blade_right" : "blade_left";
        } else if (weaponType.equalsIgnoreCase("scythe")) {
            boneName = isMainHandRight ? "scythe_right" : "scythe_left";
        } else if (weaponType.equalsIgnoreCase("clawlance")) {
            boneName = isMainHandRight ? "trident_right" : "trident_left";
        }

        if (!boneName.isEmpty()) {
            weaponModel.getBone(boneName).ifPresent(this::showBoneChain);

            syncModelToPlayer(weaponModel, playerModel);

            float[] color = getKiColor(stats);
            RenderType kiRenderType = ModRenderTypes.energy(KI_WEAPONS_TEXTURE);

            poseStack.pushPose();
            getRenderer().reRender(weaponModel, poseStack, bufferSource, animatable, kiRenderType,
                    bufferSource.getBuffer(kiRenderType), partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                    color[0], color[1], color[2], 0.8f);
            poseStack.popPose();
        }
    }

    private void showBoneChain(GeoBone bone) {
        setHiddenRecursive(bone, false);

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

    private void resetModelParts(BakedGeoModel model) {
        for (GeoBone bone : model.topLevelBones()) {
            setHiddenRecursive(bone, true);
        }
    }

    private void syncModelToPlayer(BakedGeoModel weaponModel, BakedGeoModel playerModel) {
        for (GeoBone weaponBone : weaponModel.topLevelBones()) {
            syncBoneRecursively(weaponBone, playerModel);
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
            destBone.setScaleX(sourceBone.getScaleX());
            destBone.setScaleY(sourceBone.getScaleY());
            destBone.setScaleZ(sourceBone.getScaleZ());
        });

        for (GeoBone child : destBone.getChildBones()) {
            syncBoneRecursively(child, sourceModel);
        }
    }

    private float[] getKiColor(StatsData stats) {
        var character = stats.getCharacter();
        String kiHex = character.getAuraColor();
        if (character.hasActiveForm() && character.getActiveFormData() != null) {
            String formColor = character.getActiveFormData().getAuraColor();
            if (formColor != null && !formColor.isEmpty()) kiHex = formColor;
        }
        return ColorUtils.hexToRgb(kiHex);
    }
}

