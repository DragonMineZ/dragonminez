package com.dragonminez.client.init.entities.renderer.sagas.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Random;

public class DBSagasAuraLayer<T extends DBSagasEntity> extends GeoRenderLayer<T> {

    public DBSagasAuraLayer(GeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel entityModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        boolean showAura = animatable.isTransforming() || animatable.isCharge();
        boolean showLightning = animatable.isLightning();

        if (!showAura && !showLightning) return;

        if (showAura) {
            PoseStack absoluteStack = new PoseStack();
            Minecraft mc = Minecraft.getInstance();
            Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

            double lerpX = Mth.lerp(partialTick, animatable.xo, animatable.getX());
            double lerpY = Mth.lerp(partialTick, animatable.yo, animatable.getY());
            double lerpZ = Mth.lerp(partialTick, animatable.zo, animatable.getZ());

            absoluteStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z);

            executeAuraShaderDraw(animatable, absoluteStack, partialTick);
        }

        if (showLightning) {
            PoseStack absoluteStack = new PoseStack();
            Minecraft mc = Minecraft.getInstance();
            Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

            double lerpX = Mth.lerp(partialTick, animatable.xo, animatable.getX());
            double lerpY = Mth.lerp(partialTick, animatable.yo, animatable.getY());
            double lerpZ = Mth.lerp(partialTick, animatable.zo, animatable.getZ());

            absoluteStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z);

            executeLightningShaderDraw(animatable, absoluteStack, partialTick);
        }
    }

    private void executeAuraShaderDraw(T animatable, PoseStack absoluteStack, float partialTick) {
        ShaderInstance shader = DMZShaders.auraShader;
        if (shader == null) return;

        Minecraft mc = Minecraft.getInstance();
        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();

        String auraType = animatable.getAuraType() != null && !animatable.getAuraType().isEmpty() ? animatable.getAuraType().toLowerCase() : "kakarot";
        ResourceLocation mainTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + auraType + "_aura.png");
        ResourceLocation crossTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + auraType + "_cross.png");

        float animSpeed = (animatable.tickCount + partialTick) * 0.5f;
        shader.safeGetUniform("speed").set(animSpeed);
        shader.safeGetUniform("ProjMat").set(projectionMatrix);

        float[] color = ColorUtils.rgbIntToFloat(animatable.getAuraColor());

        shader.safeGetUniform("color1").set(color[0] * 1.6f, color[1] * 1.6f, color[2] * 1.6f, 1.0f);
        shader.safeGetUniform("color2").set(color[0] * 1.3f, color[1] * 1.3f, color[2] * 1.3f, 1.0f);
        shader.safeGetUniform("color3").set(color[0] * 1.0f, color[1] * 1.0f, color[2] * 1.0f, 0.85f);
        shader.safeGetUniform("color4").set(color[0] * 0.75f, color[1] * 0.75f, color[2] * 0.75f, 0.65f);

        float cameraPitch = mc.gameRenderer.getMainCamera().getXRot();
        float absPitch = Math.abs(cameraPitch);
        float crossFactor = 0.0f;
        float pitchSquash = 1.0f;
        boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();

        if (absPitch > 45.0f && !isFirstPerson) {
            crossFactor = (float) Math.pow((absPitch - 45.0f) / 45.0f, 2.0);
            pitchSquash = 1.0f - (crossFactor * 0.5f);
        }

        float scaleMultiplier = 2.2f;
        float finalScaleX = scaleMultiplier;
        float finalScaleY = scaleMultiplier;
        float finalScaleZ = scaleMultiplier;

        if (crossFactor < 1.0f) {
            absoluteStack.pushPose();

            absoluteStack.translate(0.0, animatable.getBbHeight() * 2f, 0.0);
            absoluteStack.mulPose(mc.gameRenderer.getMainCamera().rotation());
            absoluteStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));

            absoluteStack.scale(finalScaleX, finalScaleY * pitchSquash, finalScaleZ);

            shader.safeGetUniform("alp1").set(1.0f - crossFactor);
            shader.safeGetUniform("modelMatrix").set(absoluteStack.last().pose());

            RenderType mainRender = ModRenderTypes.getCustomAura(mainTex);
            mainRender.setupRenderState();
            shader.apply();

            VertexBuffer mesh = AuraMeshFactory.getBillboardQuad();
            mesh.bind();
            mesh.drawWithShader(absoluteStack.last().pose(), projectionMatrix, shader);
            mainRender.clearRenderState();

            absoluteStack.popPose();
        }

        if (crossFactor > 0.0f) {
            absoluteStack.pushPose();

            absoluteStack.translate(0.0, 0.05, 0.0);
            absoluteStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-mc.gameRenderer.getMainCamera().getYRot()));

            if (cameraPitch < 0.0f) {
                absoluteStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
            }

            absoluteStack.scale(finalScaleX, 1.0f, finalScaleZ);

            shader.safeGetUniform("alp1").set(crossFactor);
            shader.safeGetUniform("modelMatrix").set(absoluteStack.last().pose());

            RenderType crossRender = ModRenderTypes.getCustomAura(crossTex);
            crossRender.setupRenderState();
            shader.apply();

            VertexBuffer mesh = AuraMeshFactory.getGroundQuad();
            mesh.bind();
            mesh.drawWithShader(absoluteStack.last().pose(), projectionMatrix, shader);
            crossRender.clearRenderState();

            absoluteStack.popPose();
        }

        VertexBuffer.unbind();
        shader.clear();
    }

    private void executeLightningShaderDraw(T animatable, PoseStack poseStack, float partialTick) {
        ShaderInstance shader = DMZShaders.lightningShader;
        if (shader == null) return;

        float speedMod = 1.0f;
        int maxBranches = 5;
        float maxScale = 0.5f;

        float[] colorRgb = ColorUtils.rgbIntToFloat(animatable.getLightningColor());
        float time = (animatable.tickCount + partialTick) / 20.0f;
        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();

        shader.safeGetUniform("projectionMatrix").set(projectionMatrix);
        shader.safeGetUniform("time").set(time);
        shader.safeGetUniform("speedModifier").set(speedMod);

        shader.safeGetUniform("color1").set(
                Mth.lerp(0.8f, colorRgb[0], 1.0f),
                Mth.lerp(0.8f, colorRgb[1], 1.0f),
                Mth.lerp(0.8f, colorRgb[2], 1.0f)
        );
        shader.safeGetUniform("color2").set(colorRgb[0], colorRgb[1], colorRgb[2]);
        shader.safeGetUniform("alp1").set(1.0f);
        shader.safeGetUniform("alp2").set(0.1f);
        shader.safeGetUniform("power").set(3.0f);
        shader.safeGetUniform("divis").set(1.0f);

        RenderType renderType = ModRenderTypes.getCustomLightning(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png"));
        renderType.setupRenderState();

        shader.apply();
        VertexBuffer mesh = AuraRenderer.getLightningMesh();
        mesh.bind();

        long timeHash = animatable.level().getGameTime() / 2L;
        Random seededRand = new Random(animatable.getId() + timeHash);
        float bbHeight = animatable.getBbHeight();

        for (int i = 0; i < maxBranches; i++) {
            poseStack.pushPose();

            float spread = 1.8f;
            float randomY = seededRand.nextFloat() * bbHeight;

            poseStack.translate((seededRand.nextFloat() - 0.5f) * spread, randomY, (seededRand.nextFloat() - 0.5f) * spread);
            poseStack.mulPose(Axis.YP.rotationDegrees(seededRand.nextFloat() * 360));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90f + (seededRand.nextFloat() - 0.5f) * 40f));

            float scale = 0.15f + seededRand.nextFloat() * maxScale;
            poseStack.scale(scale, scale, scale);

            shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
            shader.safeGetUniform("normalMatrix").set(new Matrix4f(new Matrix3f(poseStack.last().normal())));
            shader.apply();

            mesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
            poseStack.popPose();
        }

        VertexBuffer.unbind();
        shader.clear();
        renderType.clearRenderState();
    }
}