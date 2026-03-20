package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.KiBallPlaneModel;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
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

    private static final ResourceLocation TEXTURE_NOVA = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_nova.png");
    private static final ResourceLocation TEXTURE_NOVA2 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_nova_2.png");
    private static final ResourceLocation TEXTURE_NOVA_FIRE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_novafire.png");
    private static final ResourceLocation TEXTURE_NOVA_FIRE2 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_novafire_2.png");
    private static final ResourceLocation TEXTURE_NOVA_FIRE3 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_novafire_3.png");

    private static final ResourceLocation TEXTURE_KI_SPARKS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_sparkle1.png");
    private static final ResourceLocation TEXTURE_KI_SPARKS2 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_sparkle2.png");

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
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -0.2, 0.0f);



        switch (renderType) {
            case 0: // Small
                brightAuraColor = ColorUtils.lightenColor(coreColor, 0.5f);
                renderKiSmallBall(poseStack, entity, buffer, scale, ageInTicks, coreColor, brightAuraColor, borderColor);
                break;
            case 1: // Medium
                brightAuraColor = ColorUtils.lightenColor(coreColor, 0.5f);
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
            case 5: // Genkidama
                brightAuraColor = ColorUtils.lightenColor(coreColor, 1.0f);
                renderKiGenki(poseStack, entity, buffer, scale, ageInTicks, coreColor, brightAuraColor, borderColor);
                break;
            case 6: // SuperNova
                //invertedAuraColor = ColorUtils.rgbIntToFloat(0xFFC261);
                invertedAuraColor = ColorUtils.darkenColor(coreColor, 0.4f);
                renderKiNova(poseStack, entity, buffer, scale, ageInTicks, coreColor, invertedAuraColor, borderColor);
                break;
            case 7: // DeathBall
                //invertedAuraColor = ColorUtils.rgbIntToFloat(0xFFC261);
                invertedAuraColor = ColorUtils.darkenColor(coreColor, 0.4f);
                renderKiDeathball(poseStack, entity, buffer, scale, ageInTicks, coreColor, invertedAuraColor, borderColor);
                break;
            default:
                renderKiBlast(poseStack, entity, buffer, scale, ageInTicks, coreColor, brightAuraColor, borderColor);
                break;
        }

    }

    private void renderKiLargeBlast(PoseStack poseStack, AbstractKiProjectile entity, MultiBufferSource buffer, float scale, float ageInTicks, float[] coreColor, float[] brightAuraColor, float[] borderColor) {
        poseStack.pushPose();

        float jitterSpeed = ageInTicks * 20.0F;
        float intensity = 0.03F;

        float shakeX = (float) (Math.sin(jitterSpeed) * intensity);
        float shakeY = (float) (Math.cos(jitterSpeed * 1.2) * intensity);
        float shakeZ = (float) (Math.sin(jitterSpeed * 0.8) * intensity);

        poseStack.scale(scale + shakeX, scale + shakeY, scale + shakeZ);

        //poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -0.2, 0.0f);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.pushPose();
        poseStack.translate(0, 0.15, 0.000f);
        poseStack.scale(1.0f, 1.0f, 1.0f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.2F);
        poseStack.popPose();

// CAPA 2: Color borde 1.0
        poseStack.pushPose();
        poseStack.translate(0, 0.2, 0.001f);
        poseStack.scale(0.85f, 0.85f, 0.85f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F);
        poseStack.popPose();

 // CAPA 3: Color core transparente 0.8
        poseStack.pushPose();
        poseStack.translate(0, 0.23, 0.002f);
        poseStack.scale(0.75f, 0.75f, 0.75f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.003f);
        poseStack.scale(0.6f, 0.6f, 0.6f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.8F);
        poseStack.popPose();

// CAPA 5:
        poseStack.pushPose();
        poseStack.translate(0, 0.35, 0.004f); // 4 milímetros hacia la cámara
        poseStack.scale(0.5f, 0.5f, 0.5f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F);
        poseStack.popPose();

        // CAPA 6
        poseStack.pushPose();
        poseStack.translate(0, 0.37, 0.005f);
        poseStack.scale(0.45f, 0.45f, 0.45f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.3F);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderKiBlast(PoseStack poseStack, AbstractKiProjectile entity, MultiBufferSource buffer, float scale, float ageInTicks, float[] coreColor, float[] brightAuraColor, float[] borderColor) {
        poseStack.pushPose();

        float jitterSpeed = ageInTicks * 20.0F;
        float intensity = 0.03F;

        float shakeX = (float) (Math.sin(jitterSpeed) * intensity);
        float shakeY = (float) (Math.cos(jitterSpeed * 1.2) * intensity);
        float shakeZ = (float) (Math.sin(jitterSpeed * 0.8) * intensity);

        poseStack.scale(scale + shakeX, scale + shakeY, scale + shakeZ);

        //poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -0.2, 0.0f);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.pushPose();
        poseStack.translate(0, 0.15, 0.000f);
        poseStack.scale(1.0f, 1.0f, 1.0f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.2F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.2, 0.001f);
        poseStack.scale(0.85f, 0.85f, 0.85f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.8F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.23, 0.002f);
        poseStack.scale(0.75f, 0.75f, 0.75f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.003f);
        poseStack.scale(0.6f, 0.6f, 0.6f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.8F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.35, 0.004f); // 4 milímetros hacia la cámara
        poseStack.scale(0.5f, 0.5f, 0.5f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.37, 0.005f);
        poseStack.scale(0.45f, 0.45f, 0.45f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.3F);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderInvertedKiBlast(PoseStack poseStack, AbstractKiProjectile entity, MultiBufferSource buffer, float scale, float ageInTicks, float[] coreColor, float[] brightAuraColor, float[] borderColor) {
        poseStack.pushPose();

        float jitterSpeed = ageInTicks * 20.0F;
        float intensity = 0.03F;

        float shakeX = (float) (Math.sin(jitterSpeed) * intensity);
        float shakeY = (float) (Math.cos(jitterSpeed * 1.2) * intensity);
        float shakeZ = (float) (Math.sin(jitterSpeed * 0.8) * intensity);

        poseStack.scale(scale + shakeX, scale + shakeY, scale + shakeZ);

        //poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -0.2, 0.0f);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.pushPose();
        poseStack.translate(0, 0.15, 0.000f);
        poseStack.scale(1.0f, 1.0f, 1.0f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.2F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.2, 0.001f);
        poseStack.scale(0.85f, 0.85f, 0.85f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.8F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.23, 0.002f);
        poseStack.scale(0.75f, 0.75f, 0.75f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.003f);
        poseStack.scale(0.6f, 0.6f, 0.6f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.8F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.35, 0.004f); // 4 milímetros hacia la cámara
        poseStack.scale(0.5f, 0.5f, 0.5f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.37, 0.005f);
        poseStack.scale(0.45f, 0.45f, 0.45f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.3F);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderKiSmallBall(PoseStack poseStack, AbstractKiProjectile entity, MultiBufferSource buffer, float scale, float ageInTicks, float[] coreColor, float[] brightAuraColor, float[] borderColor) {
        poseStack.pushPose();

        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -0.2, 0.0f);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.pushPose();
        poseStack.translate(0, 0.2, 0.001f);
        poseStack.scale(0.85f, 0.85f, 0.85f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.23, 0.002f);
        poseStack.scale(0.75f, 0.75f, 0.75f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.003f);
        poseStack.scale(0.6f, 0.6f, 0.6f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.8F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.35, 0.004f); // 4 milímetros hacia la cámara
        poseStack.scale(0.5f, 0.5f, 0.5f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.37, 0.005f);
        poseStack.scale(0.45f, 0.45f, 0.45f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.3F);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderKiCastigador(PoseStack poseStack, AbstractKiProjectile entity, MultiBufferSource buffer, float scale, float ageInTicks, float[] coreColor, float[] brightAuraColor, float[] borderColor) {
        poseStack.pushPose();

        float jitterSpeed = ageInTicks * 50.0F;
        float intensity = 0.03F;

        float shakeX = (float) (Math.sin(jitterSpeed) * intensity);
        float shakeY = (float) (Math.cos(jitterSpeed * 1.2) * intensity);
        float shakeZ = (float) (Math.sin(jitterSpeed * 0.8) * intensity);

        poseStack.scale(scale + shakeX, scale + shakeY, scale + shakeZ);
        //poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -0.2, 0.0f);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.pushPose();
        poseStack.translate(0, 0.15, 0.000f);
        poseStack.scale(1.0f, 1.0f, 1.0f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.2F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.2, 0.001f);
        poseStack.scale(0.85f, 0.85f, 0.85f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.8F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.23, 0.002f);
        poseStack.scale(0.75f, 0.75f, 0.75f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.003f);
        poseStack.scale(0.6f, 0.6f, 0.6f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.8F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.35, 0.004f); // 4 milímetros hacia la cámara
        poseStack.scale(0.5f, 0.5f, 0.5f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.37, 0.005f);
        poseStack.scale(0.45f, 0.45f, 0.45f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.3F);
        poseStack.popPose();

        poseStack.translate(0, 0.45, 0); // 4 milímetros hacia la cámara
        renderCastigadorGrande(poseStack, buffer, ageInTicks, borderColor);
        renderCastigadorSpikes(poseStack, buffer, ageInTicks, ColorUtils.rgbIntToFloat(0xFF96FF));
        renderCastigadorGrande(poseStack, buffer, ageInTicks, ColorUtils.rgbIntToFloat(0xFF579E));
        renderCastigadorSpikes(poseStack, buffer, ageInTicks, ColorUtils.rgbIntToFloat(0x70FF7D));
        renderCastigadorGrande(poseStack, buffer, ageInTicks, ColorUtils.rgbIntToFloat(0x70F5FF));

        poseStack.popPose();
    }

    private void renderKiGenki(PoseStack poseStack, AbstractKiProjectile entity, MultiBufferSource buffer, float scale, float ageInTicks, float[] coreColor, float[] brightAuraColor, float[] borderColor) {
        poseStack.pushPose();

        float jitterSpeed = ageInTicks * 20.0F;
        float intensity = 0.03F;

        float shakeX = (float) (Math.sin(jitterSpeed) * intensity);
        float shakeY = (float) (Math.cos(jitterSpeed * 1.2) * intensity);
        float shakeZ = (float) (Math.sin(jitterSpeed * 0.8) * intensity);

        poseStack.scale(scale + shakeX, scale + shakeY, scale + shakeZ);

        //poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -0.2, 0.0f);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        // Pulso exterior
        poseStack.pushPose();
        poseStack.translate(0, 0.05, -0.001f);
        float speed = 0.05f;
        float pulse = (ageInTicks * speed) % 1.0f;

        float dynamicScale = 1.0f + (pulse * 0.4f);
        poseStack.scale(dynamicScale, dynamicScale, dynamicScale);

        float dynamicAlpha = 0.4f * (1.0f - pulse);

        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], dynamicAlpha);
        poseStack.popPose();
        //

        poseStack.pushPose();
        poseStack.translate(0, 0.15, 0.000f);
        poseStack.scale(1.0f, 1.0f, 1.0f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.2F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.2, 0.001f);
        poseStack.scale(0.85f, 0.85f, 0.85f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.23, 0.002f);
        poseStack.scale(0.75f, 0.75f, 0.75f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.003f);
        poseStack.scale(0.6f, 0.6f, 0.6f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.8F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.004f);
        poseStack.scale(0.6f, 0.6f, 0.6f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.25, 0.005f);
        poseStack.scale(0.7f, 0.7f, 0.7f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.5F);
        poseStack.popPose();

        //Anim pulso 2
        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.006f);
        float speedCore = 0.04f;
        float pulseCore = (ageInTicks * speedCore) % 1.0f;

        float dynamicScaleCore = 0.6f + (pulseCore * 0.2f);
        poseStack.scale(dynamicScaleCore, dynamicScaleCore, dynamicScaleCore);

        float dynamicAlphaCore = 1.0f * (1.0f - pulseCore);

        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], dynamicAlphaCore);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderKiDeathball(PoseStack poseStack, AbstractKiProjectile entity, MultiBufferSource buffer, float scale, float ageInTicks, float[] coreColor, float[] brightAuraColor, float[] borderColor) {
        poseStack.pushPose();

        float jitterSpeed = ageInTicks * 20.0F;
        float intensity = 0.03F;

        float shakeX = (float) (Math.sin(jitterSpeed) * intensity);
        float shakeY = (float) (Math.cos(jitterSpeed * 1.2) * intensity);
        float shakeZ = (float) (Math.sin(jitterSpeed * 0.8) * intensity);

        poseStack.scale(scale + shakeX, scale + shakeY, scale + shakeZ);

        //poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -0.2, 0.0f);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.pushPose();
        poseStack.translate(0, 0.15, 0.000f);
        poseStack.scale(1.0f, 1.0f, 1.0f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.2F);
        poseStack.popPose();

//        poseStack.pushPose();
//        poseStack.translate(0, 0.2, 0.001f);
//        poseStack.scale(0.85f, 0.85f, 0.85f);
//        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
//        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F);
//        poseStack.popPose();

        poseStack.pushPose();
        this.model.setupAnim(entity, 0.0F, 0.0F, 0.0f, 0.0F, 0.0F);
        poseStack.translate(0, 0.2, 0.001f);
        poseStack.scale(0.85f, 0.85f, 0.85f);
        int swapSpeed = 3;
        ResourceLocation fastTexture = ((int)(ageInTicks / swapSpeed) % 2 == 0) ? TEXTURE_NOVA : TEXTURE_NOVA2;

        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(fastTexture));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F);
        poseStack.popPose();

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        poseStack.pushPose();
        poseStack.translate(0, 0.23, 0.002f);
        poseStack.scale(0.75f, 0.75f, 0.75f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.003f);
        poseStack.scale(0.6f, 0.6f, 0.6f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.8F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.004f);
        poseStack.scale(0.6f, 0.6f, 0.6f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.25, 0.005f);
        poseStack.scale(0.7f, 0.7f, 0.7f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.5F);
        poseStack.popPose();

        //Anim pulso 2
        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.006f);
        float speedCore = 0.04f;
        float pulseCore = (ageInTicks * speedCore) % 1.0f;

        float dynamicScaleCore = 0.6f + (pulseCore * 0.2f);
        poseStack.scale(dynamicScaleCore, dynamicScaleCore, dynamicScaleCore);

        float dynamicAlphaCore = 1.0f * (1.0f - pulseCore);

        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], dynamicAlphaCore);
        poseStack.popPose();

        //RAYOS
        poseStack.pushPose();
        this.model.setupAnim(entity, 0.0F, 0.0F, 0.0f, 0.0F, 0.0F);
        poseStack.translate(0, 0.3, 0.006f);
        poseStack.scale(0.7f, 0.7f, 0.7f);

        int cycleLength = 20;
        int currentStep = (int) ageInTicks % cycleLength;

        ResourceLocation currentTexture = null;

        if (currentStep < 5) {
            // Primer segundo (0-19): NOVA 1
            currentTexture = TEXTURE_KI_SPARKS;
        } else if (currentStep < 10) {
            // Segundo segundo (20-39): NOVA 2
            currentTexture = TEXTURE_KI_SPARKS2;
        }

        if (currentTexture != null) {
            borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(currentTexture));
            this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 0.8F);
        }
        poseStack.popPose();

        poseStack.popPose();

    }

    private void renderKiNova(PoseStack poseStack, AbstractKiProjectile entity, MultiBufferSource buffer, float scale, float ageInTicks, float[] coreColor, float[] brightAuraColor, float[] borderColor) {
        poseStack.pushPose();

        float jitterSpeed = ageInTicks * 20.0F;
        float intensity = 0.03F;

        float shakeX = (float) (Math.sin(jitterSpeed) * intensity);
        float shakeY = (float) (Math.cos(jitterSpeed * 1.2) * intensity);
        float shakeZ = (float) (Math.sin(jitterSpeed * 0.8) * intensity);

        poseStack.scale(scale + shakeX, scale + shakeY, scale + shakeZ);

        //poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -0.2, 0.0f);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.pushPose();
        poseStack.translate(0, 0.15, 0.000f);
        poseStack.scale(1.0f, 1.0f, 1.0f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.2F);
        poseStack.popPose();

//        poseStack.pushPose();
//        poseStack.translate(0, 0.2, 0.001f);
//        poseStack.scale(0.85f, 0.85f, 0.85f);
//        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
//        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F);
//        poseStack.popPose();

        poseStack.pushPose();
        this.model.setupAnim(entity, 0.0F, 0.0F, 0.0f, 0.0F, 0.0F);
        poseStack.translate(0, 0.2, 0.001f);
        poseStack.scale(0.85f, 0.85f, 0.85f);
        int swapSpeed = 3;
        ResourceLocation fastTexture = ((int)(ageInTicks / swapSpeed) % 2 == 0) ? TEXTURE_NOVA : TEXTURE_NOVA2;

        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(fastTexture));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F);
        poseStack.popPose();

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        poseStack.pushPose();
        poseStack.translate(0, 0.23, 0.002f);
        poseStack.scale(0.75f, 0.75f, 0.75f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.003f);
        poseStack.scale(0.6f, 0.6f, 0.6f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.8F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.004f);
        poseStack.scale(0.6f, 0.6f, 0.6f);

        swapSpeed = 3;
        int frame = (int)(ageInTicks / swapSpeed) % 3;

        ResourceLocation currentFireTexture = switch (frame) {
            case 0 -> TEXTURE_NOVA_FIRE;
            case 1 -> TEXTURE_NOVA_FIRE2;
            default -> TEXTURE_NOVA_FIRE3;
        };
        this.model.setupAnim(entity, 0.0F, 0.0F, 0.0f, 0.0F, 0.0F);

        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(currentFireTexture));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 0.5F);
        poseStack.popPose();

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        poseStack.pushPose();
        poseStack.translate(0, 0.25, 0.005f);
        poseStack.scale(0.7f, 0.7f, 0.7f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.5F);
        poseStack.popPose();

        //Anim pulso 2
        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.006f);
        float speedCore = 0.04f;
        float pulseCore = (ageInTicks * speedCore) % 1.0f;

        float dynamicScaleCore = 0.6f + (pulseCore * 0.2f);
        poseStack.scale(dynamicScaleCore, dynamicScaleCore, dynamicScaleCore);

        float dynamicAlphaCore = 1.0f * (1.0f - pulseCore);

        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], dynamicAlphaCore);
        poseStack.popPose();

        //RAYOS
        poseStack.pushPose();
        this.model.setupAnim(entity, 0.0F, 0.0F, 0.0f, 0.0F, 0.0F);
        poseStack.translate(0, 0.3, 0.006f);
        poseStack.scale(0.7f, 0.7f, 0.7f);

        int cycleLength = 20;
        int currentStep = (int) ageInTicks % cycleLength;

        ResourceLocation currentTexture = null;

        if (currentStep < 5) {
            // Primer segundo (0-19): NOVA 1
            currentTexture = TEXTURE_KI_SPARKS;
        } else if (currentStep < 10) {
            // Segundo segundo (20-39): NOVA 2
            currentTexture = TEXTURE_KI_SPARKS2;
        }

        if (currentTexture != null) {
            borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(currentTexture));
            this.model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 0.8F);
        }
        poseStack.popPose();

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

            float width = randomsource.nextFloat() * 0.7F;
            float length = randomsource.nextFloat() * 1.5F;

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

            float width = randomsource.nextFloat() * 1.6F;
            float length = randomsource.nextFloat() * 0.7F;

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
