package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

public class KiProjectileRenderer extends EntityRenderer<AbstractKiProjectile> {
    private static final ResourceLocation TEXTURE_KI = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast.png");
    private static final ResourceLocation TEXTURE_KI_SPARKS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_sparkle1.png");
    private static final ResourceLocation TEXTURE_KI_SPARKS2 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_sparkle2.png");
    private static final ResourceLocation TEXTURE_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");
    private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0D) / 2.0D);

    public KiProjectileRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(AbstractKiProjectile entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Matrix4f basePose = new Matrix4f(poseStack.last().pose());

        PlayerEffectQueue.addKiAttack((stack, proj) -> {
            stack.pushPose();
            stack.last().pose().set(basePose);

            float ageInTicks = entity.tickCount + partialTick;
            float scale = entity.getSize();

            if (entity instanceof KiBlastEntity blastEntity) {
                boolean isFiring = blastEntity.isFiring();
                float castTime = (float) blastEntity.getCastTime();
                if (!isFiring) {
                    if (castTime > 0.1F && ageInTicks <= castTime) {
                        scale = scale * (ageInTicks / castTime);
                    }
                } else {
                    scale = entity.getSize();
                }
            }

            int renderType = entity.getKiRenderType();
            float[] coreColor = entity.getRgbColorMain();
            float[] borderColor = entity.getRgbColorBorder();
            float[] outlineColor = entity.getRgbColorOutline();

            stack.pushPose();

            float lerpYaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
            float lerpPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

            stack.translate(0.0D, entity.getBbHeight() / 2.0D, 0.0D);
            stack.mulPose(Axis.YP.rotationDegrees(-lerpYaw));
            stack.mulPose(Axis.XP.rotationDegrees(lerpPitch));

            stack.scale(scale, scale, scale);

            MultiBufferSource.BufferSource immediateBuffer = Minecraft.getInstance().renderBuffers().bufferSource();

            switch (renderType) {
                case 4:
                    float[] soulWhite = ColorUtils.rgbIntToFloat(0xFFFFFF);
                    stack.pushPose();
                    stack.scale(0.15F, 0.15F, 0.15F);
                    drawMesh(KiMeshFactory.getSphereMesh(), stack, proj, soulWhite, soulWhite, soulWhite, 1.0F, ageInTicks, true, null);
                    stack.popPose();
                    stack.pushPose();
                    stack.scale(0.4F, 0.4F, 0.4F);
                    renderSoulPunisherSpots(stack, immediateBuffer, ageInTicks);
                    renderCastigadorSpikes(stack, immediateBuffer, ageInTicks, soulWhite);
                    stack.popPose();
                    break;
                case 11:
                    float[] moonCore = coreColor;
                    float[] moonOutline = outlineColor;
                    stack.pushPose();
                    stack.scale(0.5F, 0.5F, 0.5F);
                    drawMesh(KiMeshFactory.getSphereMesh(), stack, proj, moonCore, moonCore, moonOutline, 1.0F, ageInTicks, true, null);
                    float moonPulse = 1.12F + (float) Math.sin(ageInTicks * 0.1F) * 0.08F;
                    stack.scale(moonPulse, moonPulse, moonPulse);
                    drawMesh(KiMeshFactory.getSphereMesh(), stack, proj, moonCore, moonOutline, moonOutline, 0.25F, ageInTicks, false, null);
                    stack.popPose();
                    break;
                case 6:
                    applyJitter(stack, ageInTicks, 0.01F);
                    stack.scale(0.5F, 0.5F, 0.5F);
                    drawMesh(KiMeshFactory.getSphereMesh(), stack, proj, coreColor, borderColor, outlineColor, 1.0F, ageInTicks, true, null);
                    break;
                case 7:
                    applyJitter(stack, ageInTicks, 0.03F);
                    stack.scale(2.0F, 2.0F, 2.0F);
                    drawMesh(KiMeshFactory.getSphereMesh(), stack, proj, ColorUtils.darkenColor(coreColor, 0.7F), borderColor, outlineColor, 1.0F, ageInTicks, false, null);
                    float deathPulse = 1.0F + (float)Math.sin(ageInTicks * 0.3F) * 0.15F;
                    stack.scale(deathPulse, deathPulse, deathPulse);
                    drawMesh(KiMeshFactory.getSphereMesh(), stack, proj, borderColor, outlineColor, outlineColor, 0.3F, ageInTicks, false, null);
                    stack.scale(1.1F, 1.1F, 1.1F);
                    int sparkCycle = (int) ageInTicks % 20;
                    if (sparkCycle < 10) {
                        ResourceLocation sparkTex = sparkCycle < 5 ? TEXTURE_KI_SPARKS : TEXTURE_KI_SPARKS2;
                        drawMesh(KiMeshFactory.getSphereMesh(), stack, proj, coreColor, borderColor, outlineColor, 0.8F, ageInTicks, false, sparkTex);
                    }
                    break;
                case 9:
                    float separation = 1.5F;

                    stack.pushPose();
                    stack.translate(-separation, 0.0D, 0.0D);
                    drawMesh(KiMeshFactory.getSphereMesh(), stack, proj, coreColor, borderColor, outlineColor, 1.0F, ageInTicks, true, null);
                    stack.popPose();

                    stack.pushPose();
                    stack.translate(separation, 0.0D, 0.0D);
                    drawMesh(KiMeshFactory.getSphereMesh(), stack, proj, coreColor, borderColor, outlineColor, 1.0F, ageInTicks, true, null);
                    stack.popPose();
                    break;
                default:
                    stack.scale(0.5F, 0.5F, 0.5F);
                    drawMesh(KiMeshFactory.getSphereMesh(), stack, proj, coreColor, borderColor, outlineColor, 1.0F, ageInTicks, true, null);
                    break;
            }

            PoseStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushPose();
            modelViewStack.setIdentity();
            RenderSystem.applyModelViewMatrix();
            immediateBuffer.endBatch();
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();

            ShaderInstance shader = DMZShaders.ki3dShader;
            if (shader != null) shader.clear();
            VertexBuffer.unbind();

            stack.popPose();
            stack.popPose();
        });
    }

    private void applyJitter(PoseStack stack, float ageInTicks, float intensity) {
        float jitterSpeed = ageInTicks * 20.0F;
        stack.translate(Math.sin(jitterSpeed) * intensity, Math.cos(jitterSpeed * 1.2) * intensity, Math.sin(jitterSpeed * 0.8) * intensity);
    }

    private void drawMesh(VertexBuffer mesh, PoseStack poseStack, Matrix4f proj, float[] c1, float[] c2, float[] c3, float alpha, float age, boolean addBloom, ResourceLocation texture) {
        ShaderInstance shader = DMZShaders.ki3dShader;
        if (shader == null) return;

        if (texture != null) {
            RenderSystem.setShaderTexture(0, texture);
            shader.safeGetUniform("texBlend").set(1.0f);
        } else {
            shader.safeGetUniform("texBlend").set(0.0f);
        }

        shader.safeGetUniform("colorCore").set(c1[0], c1[1], c1[2]);
        shader.safeGetUniform("colorBorder").set(c2[0], c2[1], c2[2]);
        shader.safeGetUniform("colorOutline").set(c3[0], c3[1], c3[2]);
        shader.safeGetUniform("time").set(age / 20.0f);
        shader.safeGetUniform("ProjMat").set(proj);

        mesh.bind();
        poseStack.pushPose();
        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(alpha);
        shader.apply();
        mesh.drawWithShader(poseStack.last().pose(), proj, shader);
        poseStack.popPose();
    }

    private void renderCastigadorSpikes(PoseStack poseStack, MultiBufferSource buffer, float ageInTicks, float[] colorRGB) {
        float rotationTime = ageInTicks * 3.55F;
        float rawSin = Mth.sin(ageInTicks * 0.1F);
        float normalizedFade = (rawSin + 1.0F) / 2.0F;
        float fade = 0.4F + (normalizedFade * 0.6F);
        float intensity = 0.4F;

        int r = (int)(colorRGB[0] * 255.0F);
        int g = (int)(colorRGB[1] * 255.0F);
        int b = (int)(colorRGB[2] * 255.0F);
        int alpha = (int)(155.0F * fade);

        RandomSource randomsource = RandomSource.create(432L);
        VertexConsumer vertexconsumer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_CORE));

        for(int i = 0; (float)i < (intensity + intensity * intensity) / 2.0F * 60.0F; ++i) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F + rotationTime * 90.0F));

            float width = randomsource.nextFloat() * 1.1F;
            float length = randomsource.nextFloat() * 0.3F;
            org.joml.Matrix4f matrix4f = poseStack.last().pose();

            vertex01(vertexconsumer, matrix4f, alpha, r, g, b);
            vertex2(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
            vertex3(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
            vertex01(vertexconsumer, matrix4f, alpha, r, g, b);
            vertex3(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
            vertex4(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
            vertex01(vertexconsumer, matrix4f, alpha, r, g, b);
            vertex4(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
            vertex2(vertexconsumer, matrix4f, width, length, r, g, b, alpha);

            poseStack.popPose();
        }
    }

    private void renderSoulPunisherSpots(PoseStack poseStack, MultiBufferSource buffer, float ageInTicks) {
        VertexConsumer vertexconsumer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_CORE));
        RandomSource posRandom = RandomSource.create(91L);
        int timeSeed = (int) (ageInTicks * 2.0F);
        int spotCount = 30;

        for (int i = 0; i < spotCount; i++) {
            float px = (posRandom.nextFloat() * 2.0F - 1.0F) * 0.42F;
            float py = (posRandom.nextFloat() * 2.0F - 1.0F) * 0.42F;
            float pz = (posRandom.nextFloat() * 2.0F - 1.0F) * 0.42F;
            float blobSize = 0.10F + posRandom.nextFloat() * 0.08F;

            RandomSource colorRandom = RandomSource.create((long) i * 9781L + (long) timeSeed * 131L);
            int r = colorRandom.nextInt(256);
            int g = colorRandom.nextInt(256);
            int b = colorRandom.nextInt(256);

            poseStack.pushPose();
            poseStack.translate(px, py, pz);
            poseStack.mulPose(Axis.XP.rotationDegrees(colorRandom.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(colorRandom.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(colorRandom.nextFloat() * 360.0F));

            org.joml.Matrix4f matrix4f = poseStack.last().pose();

            vertex01(vertexconsumer, matrix4f, 255, r, g, b);
            vertex2(vertexconsumer, matrix4f, blobSize, blobSize, r, g, b, 255);
            vertex3(vertexconsumer, matrix4f, blobSize, blobSize, r, g, b, 255);
            vertex01(vertexconsumer, matrix4f, 255, r, g, b);
            vertex3(vertexconsumer, matrix4f, blobSize, blobSize, r, g, b, 255);
            vertex4(vertexconsumer, matrix4f, blobSize, blobSize, r, g, b, 255);
            vertex01(vertexconsumer, matrix4f, 255, r, g, b);
            vertex4(vertexconsumer, matrix4f, blobSize, blobSize, r, g, b, 255);
            vertex2(vertexconsumer, matrix4f, blobSize, blobSize, r, g, b, 255);

            poseStack.popPose();
        }
    }

    private static void vertex01(VertexConsumer pConsumer, org.joml.Matrix4f pMatrix, int pAlpha, int r, int g, int b) {
        pConsumer.vertex(pMatrix, 0.0F, 0.0F, 0.0F).color(255, 255, 255, pAlpha).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0.0F, 1.0F, 0.0F).endVertex();
    }
    private static void vertex2(VertexConsumer pConsumer, org.joml.Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
        pConsumer.vertex(pMatrix, -HALF_SQRT_3 * pLength, pWidth, -0.5F * pLength).color(r, g, b, alpha).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0.0F, 1.0F, 0.0F).endVertex();
    }
    private static void vertex3(VertexConsumer pConsumer, org.joml.Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
        pConsumer.vertex(pMatrix, HALF_SQRT_3 * pLength, pWidth, -0.5F * pLength).color(r, g, b, alpha).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0.0F, 1.0F, 0.0F).endVertex();
    }
    private static void vertex4(VertexConsumer pConsumer, org.joml.Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
        pConsumer.vertex(pMatrix, 0.0F, pWidth, 1.0F * pLength).color(r, g, b, alpha).uv(0.5F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractKiProjectile pEntity) { return TEXTURE_KI; }
}