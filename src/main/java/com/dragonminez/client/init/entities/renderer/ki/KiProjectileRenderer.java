package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.KiBallPlaneModel;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class KiProjectileRenderer extends EntityRenderer<AbstractKiProjectile> {

    private static final ResourceLocation TEXTURE_BORDER = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiexp1_border.png");
    private static final ResourceLocation TEXTURE_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");

    private static final ResourceLocation TEXTURE_KI = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast.png");

    private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0D) / 2.0D);

    private final KiBallPlaneModel model;

    public KiProjectileRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);

        this.model = new KiBallPlaneModel(pContext.bakeLayer(KiBallPlaneModel.LAYER_LOCATION));

    }

    @Override
    public void render(AbstractKiProjectile entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float ageInTicks = entity.tickCount + partialTick;

        float[] coreColor = ColorUtils.rgbIntToFloat(entity.getColor());
        float[] brightAuraColor = ColorUtils.lightenColor(coreColor, 0.8f);
        float[] invertedAuraColor = ColorUtils.darkenColor(coreColor, 0.8f);
        float[] borderColor = ColorUtils.rgbIntToFloat(entity.getColorBorde());
        float scale = entity.getSize();
        int renderType = entity.getKiRenderType();
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -0.2, 0.0f);

        switch (renderType) {
            case 0: // Small
                renderKiSmallBall(poseStack, entity, buffer, scale, ageInTicks, coreColor, brightAuraColor, borderColor);
                break;
            case 1: // Medium
                renderKiBlast(poseStack, entity, buffer, scale, ageInTicks, coreColor, brightAuraColor, borderColor);
                break;
            case 2: // Large
                renderKiLargeBlast(poseStack, entity, buffer, scale, ageInTicks, coreColor, brightAuraColor, borderColor);
                break;
            case 3: // Inverted
                renderInvertedKiBlast(poseStack, entity, buffer, scale, ageInTicks, coreColor, invertedAuraColor, borderColor);
                break;
            case 4: // Castigador de almas
                renderKiCastigador(poseStack, entity, buffer, scale, ageInTicks, coreColor, brightAuraColor, borderColor);
                break;
            default:
                renderKiBlast(poseStack, entity, buffer, scale, ageInTicks, coreColor, brightAuraColor, borderColor);
                break;
        }

    }

    private void renderKiLargeBlast(PoseStack poseStack, AbstractKiProjectile entity, MultiBufferSource buffer, float scale, float ageInTicks, float[] coreColor, float[] brightAuraColor, float[] borderColor) {
        poseStack.pushPose(); // Iniciamos la matriz de esta capa

        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -0.2, 0.0f);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.pushPose();
        poseStack.translate(0, -0.4, 0.000f);
        poseStack.scale(1.4f, 1.4f, 1.4f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F);
        poseStack.popPose();

// CAPA 2: Color borde 1.0
        poseStack.pushPose();
        poseStack.translate(0, -0.35 , 0.001f);
        poseStack.scale(1.3f, 1.3f, 1.3f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.8F);
        poseStack.popPose();

 // CAPA 3: Color core transparente 0.8
        poseStack.pushPose();
        poseStack.translate(0, -0.3, 0.002f);
        poseStack.scale(1.2f, 1.2f, 1.2f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -0.25, 0.003f);
        poseStack.scale(1.1f, 1.1f, 1.1f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.8F);
        poseStack.popPose();

// CAPA 5: Color core blanco 0.8 (La más cercana a la cámara)
        poseStack.pushPose();
        poseStack.translate(0, -0.2, 0.004f); // 4 milímetros hacia la cámara
        poseStack.scale(1.0f, 1.0f, 1.0f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F);
        poseStack.popPose();

        // CAPA 6
        poseStack.pushPose();
        poseStack.translate(0, -0.12, 0.005f);
        poseStack.scale(0.9f, 0.9f, 0.9f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.2F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -0.12, 0.006f);
        poseStack.scale(0.9f, 0.9f, 0.9f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.2F);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderKiBlast(PoseStack poseStack, AbstractKiProjectile entity, MultiBufferSource buffer, float scale, float ageInTicks, float[] coreColor, float[] brightAuraColor, float[] borderColor) {
        poseStack.pushPose(); // Iniciamos la matriz de esta capa

        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -0.2, 0.0f);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.pushPose();
        poseStack.translate(0, -0.4, -0.002f);
        poseStack.scale(1.6f, 1.6f, 1.6f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.2F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -0.25, -0.001f);
        poseStack.scale(1.3f, 1.3f, 1.3f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.4F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -0.2, 0.000f);
        poseStack.scale(1.2f, 1.2f, 1.2f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F);
        poseStack.popPose();

        // CAPA 2: Color borde 1.0
        poseStack.pushPose();
        poseStack.translate(0, -0.15, 0.001f);
        poseStack.scale(1.1f, 1.1f, 1.1f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.8F);
        poseStack.popPose();

        // CAPA 3: Color core transparente 0.8
        poseStack.pushPose();
        poseStack.translate(0, -0.1, 0.002f);
        poseStack.scale(1.0f, 1.0f, 1.0f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -0.05, 0.003f);
        poseStack.scale(0.9f, 0.9f, 0.9f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.8F);
        poseStack.popPose();

        // CAPA 5: Color core blanco 0.8 (La más cercana a la cámara)
        poseStack.pushPose();
        poseStack.translate(0, 0, 0.004f); // 4 milímetros hacia la cámara
        poseStack.scale(0.8f, 0.8f, 0.8f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F);
        poseStack.popPose();

        // CAPA 6
        poseStack.pushPose();
        poseStack.translate(0, 0.05, 0.005f);
        poseStack.scale(0.7f, 0.7f, 0.7f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.2F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0, 0.006f);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.2F);
        poseStack.popPose();

        poseStack.popPose(); // Cerramos la matriz principal
    }

    private void renderInvertedKiBlast(PoseStack poseStack, AbstractKiProjectile entity, MultiBufferSource buffer, float scale, float ageInTicks, float[] coreColor, float[] brightAuraColor, float[] borderColor) {
        poseStack.pushPose(); // Iniciamos la matriz de esta capa

        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -0.2, 0.0f);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.pushPose();
        poseStack.translate(0, -0.4, -0.002f);
        poseStack.scale(1.6f, 1.6f, 1.6f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.2F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -0.25, -0.001f);
        poseStack.scale(1.3f, 1.3f, 1.3f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.4F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -0.2, 0.000f);
        poseStack.scale(1.2f, 1.2f, 1.2f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F);
        poseStack.popPose();

        // CAPA 2: Color borde 1.0
        poseStack.pushPose();
        poseStack.translate(0, -0.15, 0.001f);
        poseStack.scale(1.1f, 1.1f, 1.1f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.8F);
        poseStack.popPose();

        // CAPA 3: Color core transparente 0.8
        poseStack.pushPose();
        poseStack.translate(0, -0.1, 0.002f);
        poseStack.scale(1.0f, 1.0f, 1.0f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -0.05, 0.003f);
        poseStack.scale(0.9f, 0.9f, 0.9f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.8F);
        poseStack.popPose();

        // CAPA 5: Color core blanco 0.8 (La más cercana a la cámara)
        poseStack.pushPose();
        poseStack.translate(0, 0, 0.004f); // 4 milímetros hacia la cámara
        poseStack.scale(0.8f, 0.8f, 0.8f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F);
        poseStack.popPose();

        // CAPA 6
        poseStack.pushPose();
        poseStack.translate(0, 0.05, 0.005f);
        poseStack.scale(0.7f, 0.7f, 0.7f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.2F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.05, 0.006f);
        poseStack.scale(0.7f, 0.7f, 0.7f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.2F);
        poseStack.popPose();

        poseStack.popPose(); // Cerramos la matriz principal
    }

    private void renderKiSmallBall(PoseStack poseStack, AbstractKiProjectile entity, MultiBufferSource buffer, float scale, float ageInTicks, float[] coreColor, float[] brightAuraColor, float[] borderColor) {
        poseStack.pushPose();

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.translate(0, -0.2, 0.0f);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));

        poseStack.pushPose();
        poseStack.translate(0, -0.1, 0.002f);
        poseStack.scale(1.0f, 1.0f, 1.0f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -0.05, 0.003f);
        poseStack.scale(0.9f, 0.9f, 0.9f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.8F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0, 0.004f); // 4 milímetros hacia la cámara
        poseStack.scale(0.8f, 0.8f, 0.8f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.05, 0.005f);
        poseStack.scale(0.7f, 0.7f, 0.7f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.2F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.05, 0.006f);
        poseStack.scale(0.7f, 0.7f, 0.7f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.2F);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderKiCastigador(PoseStack poseStack, AbstractKiProjectile entity, MultiBufferSource buffer, float scale, float ageInTicks, float[] coreColor, float[] brightAuraColor, float[] borderColor) {
        poseStack.pushPose();

        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -0.2, 0.0f);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.pushPose();
        poseStack.translate(0, -0.4, -0.002f);
        poseStack.scale(1.6f, 1.6f, 1.6f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.2F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -0.25, -0.001f);
        poseStack.scale(1.3f, 1.3f, 1.3f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.4F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -0.2, 0.000f);
        poseStack.scale(1.2f, 1.2f, 1.2f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -0.15, 0.001f);
        poseStack.scale(1.1f, 1.1f, 1.1f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.8F);
        poseStack.popPose();

        // CAPA 3
        poseStack.pushPose();
        poseStack.translate(0, -0.1, 0.002f);
        poseStack.scale(1.0f, 1.0f, 1.0f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -0.05, 0.003f);
        poseStack.scale(0.9f, 0.9f, 0.9f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.8F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0, 0.004f); // 4 milímetros hacia la cámara
        poseStack.scale(1.0f, 1.0f, 1.0f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F);
        poseStack.popPose();

        // CAPA 6
        poseStack.pushPose();
        poseStack.translate(0, 0.05, 0.005f);
        poseStack.scale(0.7f, 0.7f, 0.7f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.2F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0, 0.006f);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.2F);
        poseStack.popPose();

        renderCastigadorGrande(poseStack, buffer, ageInTicks, borderColor);
        renderCastigadorSpikes(poseStack, buffer, ageInTicks, ColorUtils.rgbIntToFloat(0xFF96FF));
        renderCastigadorGrande(poseStack, buffer, ageInTicks, ColorUtils.rgbIntToFloat(0xFF579E));
        renderCastigadorSpikes(poseStack, buffer, ageInTicks, ColorUtils.rgbIntToFloat(0x70FF7D));
        renderCastigadorGrande(poseStack, buffer, ageInTicks, ColorUtils.rgbIntToFloat(0x70F5FF));

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

            float width = randomsource.nextFloat() * 3F;
            float length = randomsource.nextFloat() * 0.5F;

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

            float width = randomsource.nextFloat() * 6.0F;
            float length = randomsource.nextFloat() * 0.5F;

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
    public ResourceLocation getTextureLocation(AbstractKiProjectile pEntity) {
        return TEXTURE_BORDER;
    }
}
