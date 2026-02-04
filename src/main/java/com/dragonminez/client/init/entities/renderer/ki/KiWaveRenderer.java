package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.KiBallPlaneModel;
import com.dragonminez.client.init.entities.model.ki.KiLaserModel;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
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

    // Puedes usar una textura distinta si quieres, o la misma del laser
    private static final ResourceLocation TEXTURE_WAVE_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");

    private static final ResourceLocation TEXTURE_BALL_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiball1.png");
    private static final ResourceLocation TEXTURE_BALL_BORDER = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiball1_border.png");

    private final KiLaserModel laserModel; // Reutilizamos el modelo del tubo
    private final KiBallPlaneModel ballModel; // Reutilizamos el modelo de la punta

    public KiWaveRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
        // Usamos las mismas capas que el láser porque el modelo geométrico es un cilindro simple
        this.laserModel = new KiLaserModel(pContext.bakeLayer(KiLaserModel.LAYER_LOCATION));
        this.ballModel = new KiBallPlaneModel(pContext.bakeLayer(KiBallPlaneModel.LAYER_LOCATION));
    }

    @Override
    public void render(KiWaveEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float SCALE_MULTIPLIER = 16.0F;
        float length = entity.getBeamLength();

        // Obtenemos el tamaño de la entidad (definido como 2.5F en la clase Entity)
        float width = entity.getSize();

        if (length < 0.1F) length = 0.1F;

        float visualLength = length * SCALE_MULTIPLIER;

        float yaw = entity.getFixedYaw();
        float pitch = entity.getFixedPitch();
        float ageInTicks = entity.tickCount + partialTick;

        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());
        float[] borderColor = ColorUtils.rgbIntToFloat(entity.getColorBorde());

        // Rotación global
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        // Ajuste de altura inicial (Desde donde sale)
        poseStack.translate(0.0D, -0.5D, 0.5D);

        // --- CUERPO DE LA OLA (WAVE BODY) ---
        poseStack.pushPose();

        // Ajustamos la posición vertical relativa al modelo
        poseStack.translate(0.0D, -45.7D, 0.0D);

        // ESCALADO: Aquí está la clave.
        // width = Ancho y Alto
        // visualLength = Largo (profundidad)
        poseStack.scale(12.0f*width, 12.0f*width, visualLength);

        this.laserModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        // Render Core
        VertexConsumer laserCoreBuffer = buffer.getBuffer(ModRenderTypes.energy(TEXTURE_WAVE_CORE));
        this.laserModel.renderToBuffer(poseStack, laserCoreBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 1.0F);

        // Render Borde (Un poco más grande)
        poseStack.pushPose();
        poseStack.scale(1.1F, 1.1F, 1.0F); // Borde 10% más ancho que el núcleo
        VertexConsumer laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow(TEXTURE_WAVE_CORE));
        this.laserModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F);
        poseStack.popPose();

        poseStack.popPose();

        // --- PUNTA DE LA OLA (WAVE HEAD/BALL) ---
        poseStack.pushPose();

        // Nos movemos al final del recorrido actual
        poseStack.translate(0.0D, 0.0D, length);

        // Billboard Effect (Mirar a cámara)
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        // Escala de la bola final. Debe ser proporcional al ancho de la ola.
        float endBallScale = width * 0.8F; // La bola es casi tan grande como el ancho del rayo

        poseStack.scale(endBallScale, endBallScale, endBallScale);

        renderBall(entity, poseStack, buffer, ageInTicks, auraColor, borderColor, 1.0F);

        poseStack.popPose();

        poseStack.popPose();
    }


    private void renderBall(KiWaveEntity entity, PoseStack poseStack, MultiBufferSource buffer, float ageInTicks, float[] auraColor, float[] borderColor, float alpha) {
        this.ballModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer ballCoreBuffer = buffer.getBuffer(ModRenderTypes.energy(TEXTURE_BALL_CORE));
        this.ballModel.renderToBuffer(poseStack, ballCoreBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 1.0F);

        poseStack.pushPose();
        poseStack.translate(0, 0, -0.01F);
        VertexConsumer ballBorderBuffer = buffer.getBuffer(ModRenderTypes.glow(TEXTURE_BALL_BORDER));
        this.ballModel.renderToBuffer(poseStack, ballBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(KiWaveEntity pEntity) {
        return TEXTURE_WAVE_CORE;
    }
}