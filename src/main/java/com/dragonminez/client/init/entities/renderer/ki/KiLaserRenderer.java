package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.KiSpiralMesh;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class KiLaserRenderer extends EntityRenderer<KiLaserEntity> {
    private static final ResourceLocation TEXTURE_KI = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast.png");

    public KiLaserRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(KiLaserEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Matrix4f basePose = new Matrix4f(poseStack.last().pose());

        PlayerEffectQueue.addKiAttack((stack, proj) -> {
            stack.pushPose();
            stack.last().pose().set(basePose);

            int renderType = entity.getKiRenderType();
            float alphaMultiplier = 1.0F;
            float ageInTicks = entity.tickCount + partialTick;

            int maxLife = entity.getMaxLife();
            int fadeTicks = 10;

            if (entity.tickCount >= maxLife - fadeTicks) {
                alphaMultiplier = (maxLife - ageInTicks) / (float) fadeTicks;
                alphaMultiplier = Math.max(0.0F, alphaMultiplier);
            }

            switch (renderType) {
                case 1:
                    renderKiMakkankosanpo(entity, partialTick, stack, proj, alphaMultiplier);
                    break;
                case 2:
                    renderKiLaserAro(entity, partialTick, stack, proj, alphaMultiplier);
                    break;
                default:
                    renderKiLaser(entity, partialTick, stack, proj, alphaMultiplier);
                    break;
            }
            stack.popPose();
        });
    }

    public void renderKiLaser(KiLaserEntity entity, float partialTick, PoseStack poseStack, Matrix4f proj, float alphaMultiplier) {
        float[] coreColor = entity.getRgbColorMain();
        float[] borderColor = entity.getRgbColorBorder();
        float[] outlineColor = entity.getRgbColorOutline();

        float radius = entity.getSize() * 0.08f;
        boolean isFiring = entity.isFiring();
        float length = isFiring ? entity.getBeamLength() : radius;
        float ageInTicks = entity.tickCount + partialTick;

        float yaw = entity.getYRot();
        float pitch = entity.getXRot();

        ShaderInstance shader = DMZShaders.ki3dShader;
        if (shader == null) return;

        shader.safeGetUniform("colorCore").set(coreColor[0], coreColor[1], coreColor[2]);
        shader.safeGetUniform("colorBorder").set(borderColor[0], borderColor[1], borderColor[2]);
        shader.safeGetUniform("colorOutline").set(outlineColor[0], outlineColor[1], outlineColor[2]);
        shader.safeGetUniform("time").set(ageInTicks / 20.0f);
        shader.safeGetUniform("ProjMat").set(proj);

        VertexBuffer mesh = isFiring ? KiMeshFactory.getCylinderMesh() : KiMeshFactory.getSphereMesh();
        mesh.bind();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        poseStack.pushPose();
        if (isFiring) {
            poseStack.scale(radius, radius, length);
        } else {
            float chargeScale = entity.getSize() * 0.16f;
            poseStack.scale(chargeScale, chargeScale, chargeScale);
        }

        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(1.0f * alphaMultiplier);
        shader.apply();
        mesh.drawWithShader(poseStack.last().pose(), proj, shader);
        poseStack.popPose();

        poseStack.popPose();
        VertexBuffer.unbind();
        shader.clear();
    }

    private void renderKiLaserAro(KiLaserEntity entity, float partialTick, PoseStack poseStack, Matrix4f proj, float alphaMultiplier) {
        boolean isFiring = entity.isFiring();
        float width = entity.getSize() * 0.2f;
        float length = isFiring ? entity.getBeamLength() : entity.getSize() * 0.08f;

        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        float ageInTicks = entity.tickCount + partialTick;

        float[] coreColor = entity.getRgbColorMain();
        float[] borderColor = entity.getRgbColorBorder();
        float[] outlineColor = entity.getRgbColorOutline();

        ShaderInstance shader = DMZShaders.ki3dShader;
        if (shader == null) return;

        shader.safeGetUniform("time").set(ageInTicks / 20.0f);
        shader.safeGetUniform("ProjMat").set(proj);
        shader.safeGetUniform("colorCore").set(coreColor[0], coreColor[1], coreColor[2]);
        shader.safeGetUniform("colorBorder").set(borderColor[0], borderColor[1], borderColor[2]);
        shader.safeGetUniform("colorOutline").set(outlineColor[0], outlineColor[1], outlineColor[2]);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        if (!isFiring) {
            VertexBuffer sphereMesh = KiMeshFactory.getSphereMesh();
            sphereMesh.bind();

            poseStack.pushPose();
            float chargeScale = entity.getSize() * 0.16f;
            poseStack.scale(chargeScale, chargeScale, chargeScale);

            shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
            shader.safeGetUniform("alphaMult").set(1.0f * alphaMultiplier);
            shader.apply();
            sphereMesh.drawWithShader(poseStack.last().pose(), proj, shader);
            poseStack.popPose();
            VertexBuffer.unbind();
            shader.clear();
            poseStack.popPose();
            return;
        }

        Matrix4f beamBase = new Matrix4f(poseStack.last().pose());

        VertexBuffer cylinderMesh = KiMeshFactory.getCylinderMesh();
        cylinderMesh.bind();

        poseStack.pushPose();
        poseStack.scale(width, width, length);
        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(1.0f * alphaMultiplier);
        shader.apply();
        cylinderMesh.drawWithShader(poseStack.last().pose(), proj, shader);
        poseStack.popPose();
        VertexBuffer.unbind();

        float twistRad = (float) Math.toRadians(-(ageInTicks * 30.0F));
        Matrix4f spiralMV = new Matrix4f(beamBase).rotateZ(twistRad);

        VertexBuffer spiralMesh = KiSpiralMesh.get(2, entity.getSize(), length,
                width * 1.5f, width * 0.4f, 0.0f, 0.6f, false);
        spiralMesh.bind();
        shader.safeGetUniform("localPosMode").set(1.0f);
        shader.safeGetUniform("alphaMult").set(1.0f * alphaMultiplier);
        spiralMesh.drawWithShader(spiralMV, proj, shader);
        shader.safeGetUniform("localPosMode").set(0.0f);
        VertexBuffer.unbind();

        VertexBuffer sphereMesh = KiMeshFactory.getSphereMesh();
        sphereMesh.bind();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, length);
        float endBallScale = width * 2.5f;
        poseStack.scale(endBallScale, endBallScale, endBallScale);

        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(1.0f * alphaMultiplier);
        shader.apply();
        sphereMesh.drawWithShader(poseStack.last().pose(), proj, shader);
        poseStack.popPose();

        VertexBuffer.unbind();
        shader.clear();
        poseStack.popPose();
    }

    private void renderKiMakkankosanpo(KiLaserEntity entity, float partialTick, PoseStack poseStack, Matrix4f proj, float alphaMultiplier) {
        boolean isFiring = entity.isFiring();
        float width = entity.getSize() * 0.15f;
        float length = isFiring ? entity.getBeamLength() : entity.getSize() * 0.08f;

        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        float ageInTicks = entity.tickCount + partialTick;

        float[] coreColor = entity.getRgbColorMain();
        float[] borderColor = entity.getRgbColorBorder();
        float[] outlineColor = entity.getRgbColorOutline();

        ShaderInstance shader = DMZShaders.ki3dShader;
        if (shader == null) return;

        shader.safeGetUniform("time").set(ageInTicks / 20.0f);
        shader.safeGetUniform("ProjMat").set(proj);
        shader.safeGetUniform("colorCore").set(coreColor[0], coreColor[1], coreColor[2]);
        shader.safeGetUniform("colorBorder").set(borderColor[0], borderColor[1], borderColor[2]);
        shader.safeGetUniform("colorOutline").set(outlineColor[0], outlineColor[1], outlineColor[2]);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        if (!isFiring) {
            VertexBuffer sphereMesh = KiMeshFactory.getSphereMesh();
            sphereMesh.bind();

            poseStack.pushPose();
            float chargeScale = entity.getSize() * 0.16f;
            poseStack.scale(chargeScale, chargeScale, chargeScale);

            shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
            shader.safeGetUniform("alphaMult").set(1.0f * alphaMultiplier);
            shader.apply();
            sphereMesh.drawWithShader(poseStack.last().pose(), proj, shader);
            poseStack.popPose();
            VertexBuffer.unbind();
            shader.clear();
            poseStack.popPose();
            return;
        }

        Matrix4f beamBase = new Matrix4f(poseStack.last().pose());

        VertexBuffer cylinderMesh = KiMeshFactory.getCylinderMesh();
        cylinderMesh.bind();

        poseStack.pushPose();
        poseStack.scale(width, width, length);
        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(1.0f * alphaMultiplier);
        shader.apply();
        cylinderMesh.drawWithShader(poseStack.last().pose(), proj, shader);
        poseStack.popPose();
        VertexBuffer.unbind();

        float twistRad = (float) Math.toRadians(-(ageInTicks * 30.0F));
        Matrix4f spiralMV = new Matrix4f(beamBase).rotateZ(twistRad);

        float aroGrosor = width * 0.5f;
        VertexBuffer spiralMesh = KiSpiralMesh.get(1, entity.getSize(), length,
                width * 2.0f, aroGrosor * 1.3f, aroGrosor, 0.9f, true);
        spiralMesh.bind();
        shader.safeGetUniform("localPosMode").set(1.0f);
        shader.safeGetUniform("alphaMult").set(1.0f * alphaMultiplier);
        spiralMesh.drawWithShader(spiralMV, proj, shader);
        shader.safeGetUniform("localPosMode").set(0.0f);
        VertexBuffer.unbind();

        VertexBuffer sphereMesh = KiMeshFactory.getSphereMesh();
        sphereMesh.bind();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, length);
        float endBallScale = width * 1.5f;
        poseStack.scale(endBallScale, endBallScale, endBallScale);
        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(1.0f * alphaMultiplier);
        shader.apply();
        sphereMesh.drawWithShader(poseStack.last().pose(), proj, shader);
        poseStack.popPose();

        VertexBuffer.unbind();
        shader.clear();
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(KiLaserEntity pEntity) {
        return TEXTURE_KI;
    }
}