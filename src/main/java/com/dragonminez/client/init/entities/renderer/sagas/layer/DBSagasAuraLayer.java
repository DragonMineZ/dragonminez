package com.dragonminez.client.init.entities.renderer.sagas.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.aura.AuraMeshFactory;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class DBSagasAuraLayer<T extends DBSagasEntity> extends GeoRenderLayer<T> {

    private static final ResourceLocation DUMMY_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");

    private static final ResourceLocation SPARK_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/kirayos.geo.json");
    private static final ResourceLocation SPARK_TEX_0 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/rayo_0.png");
    private static final ResourceLocation SPARK_TEX_1 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/rayo_1.png");
    private static final ResourceLocation SPARK_TEX_2 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/rayo_2.png");

    public DBSagasAuraLayer(GeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel entityModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        boolean showAura = animatable.isTransforming() || animatable.isCharge();
        boolean showLightning = animatable.isLightning();

        if (!showAura && !showLightning) return;

        long frame = (long) ((animatable.level().getGameTime() / 1.5f) % 3);

        if (showAura) {
            poseStack.pushPose();

            poseStack.translate(0.0, 1.375, 0.0);
            float baseScale = 1.3f;
            poseStack.scale(baseScale * 1.5f, baseScale * 2.2f, baseScale * 1.5f);

            executeAuraShaderDraw(animatable, poseStack, partialTick);

            poseStack.popPose();
        }

        if (showLightning) {
            poseStack.pushPose();
            float scale = 1.3f;
            poseStack.scale(scale, scale, scale);

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
            poseStack.popPose();
        }
    }

    private void executeAuraShaderDraw(T animatable, PoseStack poseStack, float partialTick) {
        ShaderInstance shader;
        VertexBuffer mesh;

        String auraType = animatable.getAuraType() != null ? animatable.getAuraType() : "smooth";

        switch (auraType.toLowerCase()) {
            case "sharp":
                shader = DMZShaders.auraSharpShader;
                mesh = AuraMeshFactory.getSharpAuraMesh();
                break;
            case "sparking":
                shader = DMZShaders.auraSparkingShader;
                mesh = AuraMeshFactory.getSparkingAuraMesh();
                break;
            case "smooth":
            default:
                shader = DMZShaders.auraSmoothShader;
                mesh = AuraMeshFactory.getSmoothAuraMesh();
                break;
        }

        if (shader == null) return;

        float time = (animatable.tickCount + partialTick) / 20.0f;
        float[] color = ColorUtils.rgbIntToFloat(animatable.getAuraColor());

        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();

        shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
        shader.safeGetUniform("ProjMat").set(projectionMatrix);
        shader.safeGetUniform("normalMatrix").set(new Matrix4f(new Matrix3f(poseStack.last().normal())));
        shader.safeGetUniform("time").set(time);
        shader.safeGetUniform("auravar").set(1.0f);

        float coreIntensity = 0.55f;
        shader.safeGetUniform("color1").set(
                Mth.lerp(coreIntensity, color[0], 1.0f),
                Mth.lerp(coreIntensity, color[1], 1.0f),
                Mth.lerp(coreIntensity, color[2], 1.0f)
        );

        float borderIntensity = 1.05f;
        shader.safeGetUniform("color2").set(
                color[0] * borderIntensity,
                color[1] * borderIntensity,
                color[2] * borderIntensity
        );

        float finalAlpha = 1.0f;

        shader.safeGetUniform("alp1").set(0.45f * finalAlpha);
        shader.safeGetUniform("alp2").set(0.45f * finalAlpha);
        shader.safeGetUniform("power").set(6.0f);
        shader.safeGetUniform("divis").set(0.02f);

        RenderType auraRenderType = ModRenderTypes.getCustomAura(DUMMY_TEXTURE);
        auraRenderType.setupRenderState();

        shader.apply();
        mesh.bind();
        mesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
        VertexBuffer.unbind();
        shader.clear();

        auraRenderType.clearRenderState();
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