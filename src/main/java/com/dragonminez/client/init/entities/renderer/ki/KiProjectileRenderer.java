package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.KiBallPlaneModel;
import com.dragonminez.client.init.entities.model.ki.KiBlockModel;
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
import org.joml.Matrix4f;

public class KiProjectileRenderer extends EntityRenderer<AbstractKiProjectile> {
    private static final ResourceLocation TEXTURE_KI = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast.png");
    private static final ResourceLocation TEXTURE_KI_BLOCK = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_block1.png");
    private static final ResourceLocation TEXTURE_KI_BLOCK2 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_block2.png");
    private static final ResourceLocation TEXTURE_NOVA = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_nova.png");
    private static final ResourceLocation TEXTURE_NOVA2 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_nova_2.png");
    private static final ResourceLocation TEXTURE_NOVA_FIRE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_novafire.png");
    private static final ResourceLocation TEXTURE_NOVA_FIRE2 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_novafire_2.png");
    private static final ResourceLocation TEXTURE_NOVA_FIRE3 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_novafire_3.png");
    private static final ResourceLocation TEXTURE_KI_SPARKS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_sparkle1.png");
    private static final ResourceLocation TEXTURE_KI_SPARKS2 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_sparkle2.png");
    private static final ResourceLocation TEXTURE_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");
    private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0D) / 2.0D);

    private final KiBlockModel model2;

    public KiProjectileRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
        this.model2 = new KiBlockModel(pContext.bakeLayer(KiBlockModel.LAYER_LOCATION));
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

            // BILLBOARDING ESTRICTO: Resuelve los problemas de offset y rotación de cámara
            stack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            stack.mulPose(Axis.YP.rotationDegrees(180.0F));

            stack.translate(0, -0.2D, 0.0D);
            stack.scale(scale, scale, scale);

            MultiBufferSource.BufferSource immediateBuffer = Minecraft.getInstance().renderBuffers().bufferSource();

            switch (renderType) {
                case 1:
                    renderLegacyBlockModel(stack, entity, immediateBuffer, ageInTicks, coreColor, borderColor, packedLight);
                    break;
                case 4:
                    renderLegacyBlockModel(stack, entity, immediateBuffer, ageInTicks, coreColor, borderColor, packedLight);
                    stack.translate(0, 0.35, 0);
                    renderCastigadorGrande(stack, immediateBuffer, ageInTicks, borderColor);
                    renderCastigadorSpikes(stack, immediateBuffer, ageInTicks, ColorUtils.rgbIntToFloat(0xFF96FF));
                    renderCastigadorGrande(stack, immediateBuffer, ageInTicks, ColorUtils.rgbIntToFloat(0xFF579E));
                    renderCastigadorSpikes(stack, immediateBuffer, ageInTicks, ColorUtils.rgbIntToFloat(0x70FF7D));
                    renderCastigadorGrande(stack, immediateBuffer, ageInTicks, ColorUtils.rgbIntToFloat(0x70F5FF));
                    break;
                case 6:
                    applyJitter(stack, ageInTicks, 0.04F);
                    stack.scale(2.0F, 2.0F, 2.0F);
                    drawMesh(KiMeshFactory.getSphereMesh(), stack, proj, coreColor, ColorUtils.darkenColor(coreColor, 0.4F), outlineColor, 1.0F, ageInTicks, false, null);
                    stack.scale(1.05F, 1.05F, 1.05F);
                    int swapSpeed = 3;
                    ResourceLocation fastTexture = ((int)(ageInTicks / swapSpeed) % 2 == 0) ? TEXTURE_NOVA : TEXTURE_NOVA2;
                    drawMesh(KiMeshFactory.getSphereMesh(), stack, proj, coreColor, borderColor, outlineColor, 0.8F, ageInTicks, false, fastTexture);
                    stack.scale(1.1F, 1.1F, 1.1F);
                    int frame = (int)(ageInTicks / swapSpeed) % 3;
                    ResourceLocation fireTex = frame == 0 ? TEXTURE_NOVA_FIRE : (frame == 1 ? TEXTURE_NOVA_FIRE2 : TEXTURE_NOVA_FIRE3);
                    drawMesh(KiMeshFactory.getSphereMesh(), stack, proj, coreColor, borderColor, outlineColor, 0.6F, ageInTicks, false, fireTex);
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
                default:
                    drawMesh(KiMeshFactory.getSphereMesh(), stack, proj, coreColor, borderColor, outlineColor, 1.0F, ageInTicks, true, null);
                    break;
            }

            immediateBuffer.endBatch();

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

        if (addBloom && texture == null) {
            poseStack.pushPose();
            poseStack.scale(1.25F, 1.25F, 1.25F);
            shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
            shader.safeGetUniform("alphaMult").set(alpha * 0.15F);
            shader.apply();
            mesh.drawWithShader(poseStack.last().pose(), proj, shader);
            poseStack.popPose();
        }
    }

    private void renderLegacyBlockModel(PoseStack poseStack, AbstractKiProjectile entity, MultiBufferSource buffer, float ageInTicks, float[] coreColor, float[] borderColor, int packedLight) {
        float[] brightAuraColor = ColorUtils.lightenColor(coreColor, 0.5f);
        this.model2.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        poseStack.pushPose();
        poseStack.translate(0, -0.25, 0.0f);
        poseStack.scale(0.6f,0.6f ,0.6f );
        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI_BLOCK));
        this.model2.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -0.25, 0.0f);
        poseStack.scale(0.6f,0.6f ,0.6f );
        VertexConsumer auraBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI_BLOCK2));
        this.model2.renderToBuffer(poseStack, auraBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -0.4, 0.0f);
        poseStack.scale(0.7f, 0.7f, 0.7f);
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_CORE));
        this.model2.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.35F);
        poseStack.popPose();
    }

    private void renderCastigadorSpikes(PoseStack poseStack, MultiBufferSource buffer, float ageInTicks, float[] colorRGB) {
        float rotationTime = ageInTicks * 3.55F;
        float rawSin = net.minecraft.util.Mth.sin(ageInTicks * 0.1F);
        float normalizedFade = (rawSin + 1.0F) / 2.0F;
        float fade = 0.4F + (normalizedFade * 0.6F);
        float intensity = 0.6F;

        int r = (int)(colorRGB[0] * 255.0F);
        int g = (int)(colorRGB[1] * 255.0F);
        int b = (int)(colorRGB[2] * 255.0F);
        int alpha = (int)(155.0F * fade);

        net.minecraft.util.RandomSource randomsource = net.minecraft.util.RandomSource.create(432L);
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

    private void renderCastigadorGrande(PoseStack poseStack, MultiBufferSource buffer, float ageInTicks, float[] colorRGB) {
        float rotationTime = ageInTicks * 0.35F;
        float rawSin = net.minecraft.util.Mth.sin(ageInTicks * 0.1F);
        float normalizedFade = (rawSin + 1.0F) / 2.0F;
        float fade = 0.4F + (normalizedFade * 0.6F);
        float intensity = 0.6F;

        int r = (int)(colorRGB[0] * 255.0F);
        int g = (int)(colorRGB[1] * 255.0F);
        int b = (int)(colorRGB[2] * 255.0F);
        int alpha = (int)(55.0F);

        net.minecraft.util.RandomSource randomsource = net.minecraft.util.RandomSource.create(432L);
        VertexConsumer vertexconsumer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_CORE));

        for(int i = 0; (float)i < (intensity + intensity * intensity) / 2.0F * 60.0F; ++i) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F + rotationTime * 90.0F));

            float width = randomsource.nextFloat() * 1.2F;
            float length = randomsource.nextFloat() * 0.2F;
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