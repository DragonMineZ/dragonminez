package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.*;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class KiWaveRenderer extends EntityRenderer<KiWaveEntity> {

    private static final ResourceLocation TEXTURE_WAVE_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiwave.png");
    private static final ResourceLocation TEXTURE_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");

    private static final ResourceLocation TEXTURE_KI = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast.png");
    private static final ResourceLocation TEXTURE_BORDER = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiexp1_border.png");
    private static final ResourceLocation TEXTURE_BALL_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiball1.png");
    private static final ResourceLocation TEXTURE_BALL_BORDER = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiball1_border.png");

    private static final ResourceLocation TEXTURE_EXPLODE1 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiwave_explode1.png");
    private static final ResourceLocation TEXTURE_EXPLODE2 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiwave_explode2.png");


    private final KiWaveModel waveModel;
    private final KiWave2DModel wave2Model;
    private final KiBallPlaneModel ballModel;
    private final KiWaveExplodeModel explodeModel;

    public KiWaveRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
        this.waveModel = new KiWaveModel(pContext.bakeLayer(KiWaveModel.LAYER_LOCATION));
        this.wave2Model = new KiWave2DModel(pContext.bakeLayer(KiWave2DModel.LAYER_LOCATION));
        this.ballModel = new KiBallPlaneModel(pContext.bakeLayer(KiBallPlaneModel.LAYER_LOCATION));
        this.explodeModel = new KiWaveExplodeModel(pContext.bakeLayer(KiWaveExplodeModel.LAYER_LOCATION));

    }

    @Override
    public void render(KiWaveEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float ageInTicks = entity.tickCount + partialTick;

        float SCALE_MULTIPLIER = 16.0F;
        float length = Math.max(entity.getBeamLength(), 0.1F);
        float visualLength = length * SCALE_MULTIPLIER;

        float basePulse = 1.0F + (float) Math.sin(ageInTicks * 1.5F) * 0.15F;
        float jitter = (float) (Math.random() - 0.5) * 0.05F;
        float width = entity.getSize() * (basePulse + jitter);

        float yaw = entity.getFixedYaw();
        float pitch = entity.getFixedPitch();

        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());
        float[] borderColor = ColorUtils.rgbIntToFloat(entity.getColorBorde());
        float[] brightAuraColor = ColorUtils.lightenColor(auraColor, 0.5f);

        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        poseStack.translate(0.0D, -0.5D, 0.5D);
        float tubeLength = Math.max(length - 0.8F, 0.1F);
        renderKiWave2D(poseStack, buffer, entity, ageInTicks, width, tubeLength, auraColor, borderColor);


//        poseStack.pushPose();
//        this.waveModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
//        float shortenedLength = Math.max(length - 1.2F, 0.1F);
//        float shortenedVisualLength = shortenedLength * SCALE_MULTIPLIER;
//        poseStack.scale(1.5F, 1.5F, shortenedVisualLength);
//        poseStack.translate(0.0D, -1.0D, 0.0002D);
//        VertexConsumer laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_WAVE_CORE));
//        this.waveModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.2F);
//        poseStack.popPose();

        //renderKiWave3D(poseStack, buffer, entity, ageInTicks, width, visualLength, auraColor, borderColor);

        float explodePulse = (float) Math.sin(ageInTicks * 4.1F) * 0.1F;

        // Temblor casi imperceptible para no verse "glitcheado"
        float explodeJitter = (float) (Math.random() - 0.5) * 0.02F;

        // PRIMERA BLANCO
        poseStack.pushPose();
        float scale1 = 1.5F * (1.0F + explodePulse + explodeJitter);
        poseStack.scale(scale1, scale1, scale1);
        poseStack.translate(0.0D, -1.7D, -0.2D);
        boolean useFirstTexture = (entity.tickCount / 3) % 2 == 0;
        ResourceLocation currentExplodeTexture = useFirstTexture ? TEXTURE_EXPLODE1 : TEXTURE_EXPLODE2;
        VertexConsumer laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(currentExplodeTexture));
        this.explodeModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 0.6F);
        poseStack.popPose();

        // SEGUNDA BORDE
        poseStack.pushPose();
        float scale2 = 2.0F * (1.0F - explodePulse + explodeJitter);
        poseStack.scale(scale2, scale2, scale2);
        poseStack.translate(1.0D, -1.3D, -0.1D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(35.0F));
        useFirstTexture = (entity.tickCount / 3) % 2 == 0;
        currentExplodeTexture = useFirstTexture ? TEXTURE_EXPLODE1 : TEXTURE_EXPLODE2;
        laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(currentExplodeTexture));
        this.explodeModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F);
        poseStack.popPose();

        //ESFERA INICIO
        poseStack.pushPose();

        float startBallScale = width * 1.5F;
        poseStack.scale(startBallScale, startBallScale, startBallScale);

        poseStack.translate(0.0D, -0.3D, -0.1D);

        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        renderKiBlast(poseStack, entity, buffer, 1.0F, ageInTicks, auraColor, brightAuraColor, borderColor);

        poseStack.popPose();


        //ESFERA FINAL
        poseStack.translate(0.0D, 0.0D, length);

        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        width = entity.getSize();
        float endBallScale = width * 2F;
        poseStack.scale(endBallScale, endBallScale, endBallScale);
        poseStack.translate(0.0D, -0.3D, 0.0D);
        renderKiBlast(poseStack, entity, buffer, 1.0F, ageInTicks, auraColor, brightAuraColor, borderColor);

        poseStack.popPose();
    }

    private void renderKiWave2D(PoseStack poseStack, MultiBufferSource buffer, KiWaveEntity entity, float ageInTicks, float width, float visualLength, float[] auraColor, float[] borderColor) {
        poseStack.pushPose();

        poseStack.scale(width, width, visualLength);

        this.wave2Model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        poseStack.pushPose();
        poseStack.scale(1.15F, 1.15F, 1.0F); // Un poco más gordo para envolver
        poseStack.translate(0.0D, -1.5D, -0.001D);
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_WAVE_CORE));
        this.wave2Model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(1.2F, 1.0F, 1.0F);
        poseStack.translate(0.0D, -1.5D, -0.001D);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_WAVE_CORE));
        this.wave2Model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(1.2F, 1.0F, 1.0F); // Un poco más gordo para envolver
        poseStack.translate(-0.05D, -1.5D, -0.001D);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_WAVE_CORE));
        this.wave2Model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 1.0F);
        poseStack.popPose();



        poseStack.popPose();
    }

    private void renderKiWave3D(PoseStack poseStack, MultiBufferSource buffer, KiWaveEntity entity, float ageInTicks, float width, float visualLength, float[] auraColor, float[] borderColor) {
        poseStack.pushPose();
        poseStack.scale(width, width, visualLength);
        poseStack.translate(0.0D, -0.05D, 0.0D);

        this.waveModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        poseStack.pushPose();
        poseStack.scale(1.05F, 1.05F, 1.0F);
        poseStack.translate(0.0D, -0.5D, -0.001D);
        VertexConsumer laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_WAVE_CORE));
        this.waveModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F);
        poseStack.popPose();

        float coreScale = 0.9F;
        float[][] coreOffsets = {
                {0.05F, -0.5F}, {-0.067F, -0.39F}, {0.08F, -0.35F}, {-0.05F, -0.32F}
        };
        VertexConsumer coreBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_WAVE_CORE));

        for (float[] offset : coreOffsets) {
            poseStack.pushPose();
            poseStack.scale(coreScale, coreScale, 1.0F);
            poseStack.translate(offset[0], offset[1], 0.0D);
            this.waveModel.renderToBuffer(poseStack, coreBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 1.0F);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderBall(KiWaveEntity entity, PoseStack poseStack, MultiBufferSource buffer, float ageInTicks, float[] auraColor, float[] borderColor, float alpha) {
        this.ballModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer ballCoreBuffer = buffer.getBuffer(ModRenderTypes.energy(TEXTURE_BALL_CORE));
        this.ballModel.renderToBuffer(poseStack, ballCoreBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 1.0F);

        poseStack.pushPose();
        poseStack.translate(0, 0, -0.01F);
        VertexConsumer ballBorderBuffer = buffer.getBuffer(ModRenderTypes.kiblast(TEXTURE_BALL_BORDER));
        this.ballModel.renderToBuffer(poseStack, ballBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F);
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

        this.ballModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.translate(0.0F, -0.07F, 0.0F);

        poseStack.pushPose();
        poseStack.translate(0, 0.15, 0.000f);
        poseStack.scale(1.0f, 1.0f, 1.0f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.ballModel.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.2F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.2, 0.001f);
        poseStack.scale(0.85f, 0.85f, 0.85f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.ballModel.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.8F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.23, 0.002f);
        poseStack.scale(0.75f, 0.75f, 0.75f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.ballModel.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.6F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.003f);
        poseStack.scale(0.6f, 0.6f, 0.6f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.ballModel.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.8F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.35, 0.004f); // 4 milímetros hacia la cámara
        poseStack.scale(0.5f, 0.5f, 0.5f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.ballModel.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.37, 0.005f);
        poseStack.scale(0.45f, 0.45f, 0.45f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.ballModel.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.3F);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(KiWaveEntity pEntity) {
        return TEXTURE_WAVE_CORE;
    }
}