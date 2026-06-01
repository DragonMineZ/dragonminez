package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Random;

public class KiWaveRenderer extends EntityRenderer<KiWaveEntity> {
    private static final ResourceLocation TEXTURE_WAVE_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiwave.png");

    public KiWaveRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(KiWaveEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Matrix4f basePose = new Matrix4f(poseStack.last().pose());

        PlayerEffectQueue.addKiAttack((stack, proj) -> {
            stack.pushPose();
            stack.last().pose().set(basePose);

            float[] auraColor = entity.getRgbColorMain();
            float[] borderColor = entity.getRgbColorBorder();

            float exactAge = entity.tickCount + partialTick;
            float fadeAlpha = 1.0F;

            int maxLife = entity.getMaxLife();
            int fadeTicks = 20;

            if (entity.tickCount >= maxLife - fadeTicks) {
                fadeAlpha = (maxLife - exactAge) / (float) fadeTicks;
                fadeAlpha = Math.max(0.0F, fadeAlpha);
            }

            int renderType = entity.getKiRenderType();

            switch (renderType) {
                case 1:
                case 2:
                    KiRenderWaveBrightness(entity, exactAge, stack, proj, auraColor, borderColor, fadeAlpha, buffer);
                    break;
                case 3:
                    KiRenderWaveDouble(entity, exactAge, stack, proj, auraColor, borderColor, fadeAlpha, buffer);
                    break;
                default:
                    KiRenderWave(entity, exactAge, stack, proj, auraColor, borderColor, fadeAlpha);
                    break;
            }
            stack.popPose();
        });
    }

    private void KiRenderWave(KiWaveEntity entity, float ageInTicks, PoseStack poseStack, Matrix4f proj, float[] auraColor, float[] borderColor, float fadeAlpha) {
        float castTime = (float) entity.getCastWave();
        float targetCastSize = entity.getCastSize();
        float finalSize = entity.getSize();
        float[] outlineColor = entity.getRgbColorOutline();

        boolean isFiring = entity.isFiring();
        float currentWidth = (!isFiring) ? (castTime > 0.1f && ageInTicks <= castTime ? targetCastSize * (ageInTicks / castTime) : targetCastSize) : finalSize;

        float yaw = entity.getYRot();
        float pitch = entity.getXRot();

        ShaderInstance shader = DMZShaders.ki3dShader;
        if (shader == null) return;

        shader.safeGetUniform("colorCore").set(auraColor[0], auraColor[1], auraColor[2]);
        shader.safeGetUniform("colorBorder").set(borderColor[0], borderColor[1], borderColor[2]);
        shader.safeGetUniform("colorOutline").set(outlineColor[0], outlineColor[1], outlineColor[2]);
        shader.safeGetUniform("time").set(ageInTicks / 20.0f);
        shader.safeGetUniform("ProjMat").set(proj);

        VertexBuffer sphereMesh = KiMeshFactory.getSphereMesh();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        poseStack.pushPose();
        float ballScale = currentWidth * 1.5F;
        poseStack.scale(ballScale, ballScale, ballScale);

        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(1.0f * fadeAlpha);
        shader.apply();
        sphereMesh.bind();
        sphereMesh.drawWithShader(poseStack.last().pose(), proj, shader);

        poseStack.scale(1.25f, 1.25f, 1.25f);
        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(0.15f * fadeAlpha);
        shader.apply();
        sphereMesh.drawWithShader(poseStack.last().pose(), proj, shader);
        poseStack.popPose();
        VertexBuffer.unbind();

        if (isFiring) {
            float length = Math.max(entity.getBeamLength(), 0.1F);
            VertexBuffer cylinderMesh = KiMeshFactory.getCylinderMesh();

            poseStack.pushPose();
            float cylinderWidth = currentWidth * 1.2F;
            poseStack.scale(cylinderWidth, cylinderWidth, length);

            shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
            shader.safeGetUniform("alphaMult").set(1.0f * fadeAlpha);
            shader.apply();
            cylinderMesh.bind();
            cylinderMesh.drawWithShader(poseStack.last().pose(), proj, shader);

            poseStack.scale(1.2f, 1.2f, 1.0f);
            shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
            shader.safeGetUniform("alphaMult").set(0.15f * fadeAlpha);
            shader.apply();
            cylinderMesh.drawWithShader(poseStack.last().pose(), proj, shader);
            poseStack.popPose();
            VertexBuffer.unbind();
        }

        poseStack.popPose();
        shader.clear();
    }

    private void KiRenderWaveBrightness(KiWaveEntity entity, float ageInTicks, PoseStack poseStack, Matrix4f proj, float[] auraColor, float[] borderColor, float fadeAlpha, MultiBufferSource buffer) {
        float castTime = (float) entity.getCastWave();
        float targetCastSize = entity.getCastSize();
        float finalSize = entity.getSize();

        boolean isFiring = entity.isFiring();
        float currentWidth = (!isFiring) ? (castTime > 0.1f && ageInTicks <= castTime ? targetCastSize * (ageInTicks / castTime) : targetCastSize) : finalSize;

        float basePulse = 1.0F + (float) Math.sin(ageInTicks * 1.5F) * 0.15F;
        float width = currentWidth * basePulse;

        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);

        poseStack.pushPose();

        if (!isFiring) {
            poseStack.pushPose();
            float startBallScale = width * 1.5F;
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            renderKiSphereWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha);
            poseStack.popPose();
            poseStack.popPose();
            return;
        }

        float length = Math.max(entity.getBeamLength(), 0.1F);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        float tubeLength = Math.max(length, 0.1F);
        renderKiCylinderWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, width, tubeLength, fadeAlpha);

        if (entity.getKiRenderType() == 2) {
            renderGalickLightning(poseStack, entity, buffer, borderColor, fadeAlpha, ageInTicks, width, isFiring);
        }
        poseStack.popPose();

        poseStack.pushPose();
        Vec3 startPosSphere = dir.scale(0.1D);
        poseStack.translate(startPosSphere.x, startPosSphere.y, startPosSphere.z);
        float startBallScaleDisp = width * 1.5F;
        poseStack.scale(startBallScaleDisp, startBallScaleDisp, startBallScaleDisp);
        renderKiSphereWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha);
        poseStack.popPose();

        poseStack.pushPose();
        Vec3 endPosSphere = dir.scale(length);
        poseStack.translate(endPosSphere.x, endPosSphere.y, endPosSphere.z);
        float endBallScaleDisp = width * 2.5F;
        poseStack.scale(endBallScaleDisp, endBallScaleDisp, endBallScaleDisp);
        renderKiSphereWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void KiRenderWaveDouble(KiWaveEntity entity, float ageInTicks, PoseStack poseStack, Matrix4f proj, float[] auraColor, float[] borderColor, float fadeAlpha, MultiBufferSource buffer) {
        float castTime = (float) entity.getCastWave();
        float targetCastSize = entity.getCastSize();
        float finalSize = entity.getSize();

        boolean isFiring = entity.isFiring();
        float chargeProgress = (castTime > 0.1F) ? Math.min(1.0F, ageInTicks / castTime) : 1.0F;
        float currentWidth = (!isFiring) ? (targetCastSize * chargeProgress) : finalSize;

        float basePulse = 1.0F + (float) Math.sin(ageInTicks * 1.5F) * 0.15F;
        float width = currentWidth * basePulse;

        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);

        poseStack.pushPose();

        if (!isFiring) {
            float startBallScale = width;
            float initialSpread = 1.8F;
            float hitboxWidth = entity.getOwner() != null ? entity.getOwner().getBbWidth() : 0.6F;
            float lateralOffset = (hitboxWidth * initialSpread) * (1.0F - chargeProgress);

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.translate(lateralOffset, 0.0D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            renderKiSphereWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.translate(-lateralOffset, 0.0D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            renderKiSphereWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha);
            poseStack.popPose();

            poseStack.popPose();
            return;
        }

        float length = Math.max(entity.getBeamLength(), 0.1F);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        renderKiCylinderWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, width, length, fadeAlpha);
        renderGalickLightning(poseStack, entity, buffer, borderColor, fadeAlpha, ageInTicks, width, isFiring);
        poseStack.popPose();

        poseStack.pushPose();
        Vec3 startPos = dir.scale(0.1D);
        poseStack.translate(startPos.x, startPos.y, startPos.z);
        float startBallScale = width * 1.8F;
        poseStack.scale(startBallScale, startBallScale, startBallScale);
        renderKiSphereWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha);
        poseStack.popPose();

        poseStack.pushPose();
        Vec3 endPos = dir.scale(length);
        poseStack.translate(endPos.x, endPos.y, endPos.z);
        poseStack.scale(width * 2.5F, width * 2.5F, width * 2.5F);
        renderKiSphereWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderGalickLightning(PoseStack poseStack, KiWaveEntity entity, MultiBufferSource buffer, float[] color, float alpha, float ageInTicks, float dynamicWidth, boolean isFiring) {
        ShaderInstance shader = DMZShaders.lightningShader;
        VertexBuffer mesh = AuraRenderer.getLightningMesh();

        if (shader == null || mesh == null) return;

        shader.safeGetUniform("time").set(ageInTicks / 20.0f);
        shader.safeGetUniform("speedModifier").set(isFiring ? 2.5f : 1.5f);
        shader.safeGetUniform("color1").set(1.0f, 1.0f, 1.0f);
        shader.safeGetUniform("color2").set(color[0], color[1], color[2]);
        shader.safeGetUniform("alp1").set(alpha);
        shader.safeGetUniform("alp2").set(0.1f * alpha);
        shader.safeGetUniform("projectionMatrix").set(RenderSystem.getProjectionMatrix());

        RenderType lightningType = ModRenderTypes.getCustomLightning(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png"));
        lightningType.setupRenderState();

        shader.apply();
        mesh.bind();

        Random seededRand = new Random((long) (entity.getId() + ((int)ageInTicks * 5)));
        float baseScale = dynamicWidth * (isFiring ? 2.2f : 1.8f);

        for (int i = 0; i < (isFiring ? 6 : 4); i++) {
            poseStack.pushPose();
            poseStack.translate(0.0D, isFiring ? -0.2D : -0.3D, 0.0D);

            poseStack.mulPose(Axis.XP.rotationDegrees(seededRand.nextFloat() * 360));
            poseStack.mulPose(Axis.YP.rotationDegrees(seededRand.nextFloat() * 360));
            poseStack.mulPose(Axis.ZP.rotationDegrees(seededRand.nextFloat() * 360));

            float individualScale = baseScale * (0.8f + seededRand.nextFloat() * 0.4f);
            poseStack.scale(individualScale, individualScale, individualScale);

            shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
            shader.safeGetUniform("normalMatrix").set(new Matrix4f(new Matrix3f(poseStack.last().normal())));
            shader.apply();

            mesh.drawWithShader(poseStack.last().pose(), com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix(), shader);
            poseStack.popPose();
        }

        VertexBuffer.unbind();
        shader.clear();
        lightningType.clearRenderState();
    }

    private void renderKiSphereWithShader(KiWaveEntity entity, PoseStack poseStack, Matrix4f proj, float[] coreColor, float[] borderColor, float ageInTicks, float alphaMultiplier) {
        float[] outlineColor = entity.getRgbColorOutline();
        ShaderInstance shader = DMZShaders.ki3dShader;
        if (shader == null) return;

        shader.safeGetUniform("colorCore").set(coreColor[0], coreColor[1], coreColor[2]);
        shader.safeGetUniform("colorBorder").set(borderColor[0], borderColor[1], borderColor[2]);
        shader.safeGetUniform("colorOutline").set(outlineColor[0], outlineColor[1], outlineColor[2]);
        shader.safeGetUniform("time").set(ageInTicks / 20.0f);
        shader.safeGetUniform("ProjMat").set(proj);

        VertexBuffer mesh = KiMeshFactory.getSphereMesh();
        mesh.bind();

        poseStack.pushPose();
        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(1.0f * alphaMultiplier);
        shader.apply();
        mesh.drawWithShader(poseStack.last().pose(), proj, shader);

        poseStack.scale(1.25f, 1.25f, 1.25f);
        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(0.15f * alphaMultiplier);
        shader.apply();
        mesh.drawWithShader(poseStack.last().pose(), proj, shader);

        poseStack.popPose();
        VertexBuffer.unbind();
        shader.clear();
    }

    private void renderKiCylinderWithShader(KiWaveEntity entity, PoseStack poseStack, Matrix4f proj, float[] coreColor, float[] borderColor, float ageInTicks, float radius, float length, float alphaMultiplier) {
        float[] outlineColor = entity.getRgbColorOutline();
        ShaderInstance shader = DMZShaders.ki3dShader;
        if (shader == null) return;

        shader.safeGetUniform("colorCore").set(coreColor[0], coreColor[1], coreColor[2]);
        shader.safeGetUniform("colorBorder").set(borderColor[0], borderColor[1], borderColor[2]);
        shader.safeGetUniform("colorOutline").set(outlineColor[0], outlineColor[1], outlineColor[2]);
        shader.safeGetUniform("time").set(ageInTicks / 20.0f);
        shader.safeGetUniform("ProjMat").set(proj);

        VertexBuffer mesh = KiMeshFactory.getCylinderMesh();
        mesh.bind();

        poseStack.pushPose();
        poseStack.scale(radius, radius, length);

        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(1.0f * alphaMultiplier);
        shader.apply();
        mesh.drawWithShader(poseStack.last().pose(), proj, shader);

        poseStack.scale(1.3f, 1.3f, 1.0f);
        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(0.15f * alphaMultiplier);
        shader.apply();
        mesh.drawWithShader(poseStack.last().pose(), proj, shader);

        poseStack.popPose();
        VertexBuffer.unbind();
        shader.clear();
    }

    @Override
    public ResourceLocation getTextureLocation(KiWaveEntity pEntity) {
        return TEXTURE_WAVE_CORE;
    }
}