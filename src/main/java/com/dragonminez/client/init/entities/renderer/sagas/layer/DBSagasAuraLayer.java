package com.dragonminez.client.init.entities.renderer.sagas.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.SagaFreezer2ndEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class DBSagasAuraLayer<T extends DBSagasEntity> extends GeoRenderLayer<T> {

    private static final ResourceLocation AURA_MODEL = new ResourceLocation(Reference.MOD_ID, "geo/entity/races/kiaura.geo.json");
    private static final ResourceLocation AURA_TEX_0 = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/aura_ki_0.png");
    private static final ResourceLocation AURA_TEX_1 = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/aura_ki_1.png");
    private static final ResourceLocation AURA_TEX_2 = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/aura_ki_2.png");

    private static final ResourceLocation SPARK_MODEL = new ResourceLocation(Reference.MOD_ID, "geo/entity/races/kirayos.geo.json");
    private static final ResourceLocation SPARK_TEX_0 = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/rayo_0.png");
    private static final ResourceLocation SPARK_TEX_1 = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/rayo_1.png");
    private static final ResourceLocation SPARK_TEX_2 = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/rayo_2.png");


    public DBSagasAuraLayer(GeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel entityModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        boolean showAura = animatable.isTransforming() || animatable.isCharge();
        boolean showLightning = animatable.isLightning();

        if (!showAura && !showLightning) return;

        poseStack.pushPose();

        float scale = 1.3f;
        poseStack.scale(scale, scale, scale);

        long frame = (long) ((animatable.level().getGameTime() / 1.5f) % 3);

        if (showAura) {
            BakedGeoModel auraModel = getGeoModel().getBakedModel(AURA_MODEL);
            if (auraModel != null) {
                for (GeoBone rootBone : auraModel.topLevelBones()) {
                    setHiddenRecursive(rootBone, false);
                }

                ResourceLocation currentTexture;
                if (frame == 0) currentTexture = AURA_TEX_0;
                else if (frame == 1) currentTexture = AURA_TEX_1;
                else currentTexture = AURA_TEX_2;

                syncModelToEntity(auraModel, entityModel);

                float[] color = ColorUtils.rgbIntToFloat(animatable.getAuraColor());
                RenderType auraRenderType = ModRenderTypes.energy(currentTexture);

                getRenderer().reRender(auraModel, poseStack, bufferSource, animatable, auraRenderType,
                        bufferSource.getBuffer(auraRenderType), partialTick, 15728880,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        color[0], color[1], color[2], 0.2f);
            }
        }

        if (showLightning) {
            BakedGeoModel sparkModel = getGeoModel().getBakedModel(SPARK_MODEL);
            if (sparkModel != null) {
                for (GeoBone rootBone : sparkModel.topLevelBones()) {
                    setHiddenRecursive(rootBone, false);
                }

                ResourceLocation currentSparkTex;
                if (frame == 0) currentSparkTex = SPARK_TEX_0;
                else if (frame == 1) currentSparkTex = SPARK_TEX_1;
                else currentSparkTex = SPARK_TEX_2;

                syncModelToEntity(sparkModel, entityModel);

                float[] sparkColor = ColorUtils.rgbIntToFloat(animatable.getLightningColor());
                RenderType sparkRenderType = ModRenderTypes.energy(currentSparkTex);

                getRenderer().reRender(sparkModel, poseStack, bufferSource, animatable, sparkRenderType,
                        bufferSource.getBuffer(sparkRenderType), partialTick, 15728880,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        sparkColor[0], sparkColor[1], sparkColor[2], 0.5f);
            }
        }

        poseStack.popPose();
    }

    private void setHiddenRecursive(GeoBone bone, boolean hidden) {
        bone.setHidden(hidden);
        for (GeoBone child : bone.getChildBones()) {
            setHiddenRecursive(child, hidden);
        }
    }

    private void syncModelToEntity(BakedGeoModel auraModel, BakedGeoModel entityModel) {
        for (GeoBone auraBone : auraModel.topLevelBones()) {
            syncBoneRecursively(auraBone, entityModel);
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
}