package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.KiBallPlaneModel;
import com.dragonminez.client.init.entities.model.ki.KiLaserModel;
import com.dragonminez.client.init.entities.model.ki.KiWaveModel;
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

    private static final ResourceLocation TEXTURE_WAVE_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiwave.png");
    private static final ResourceLocation TEXTURE_BALL_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiball1.png");
    private static final ResourceLocation TEXTURE_BALL_BORDER = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiball1_border.png");

    private final KiWaveModel waveModel; // <--- CAMBIO AQUÍ
    private final KiBallPlaneModel ballModel;

    public KiWaveRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
        this.waveModel = new KiWaveModel(pContext.bakeLayer(KiWaveModel.LAYER_LOCATION));
        this.ballModel = new KiBallPlaneModel(pContext.bakeLayer(KiBallPlaneModel.LAYER_LOCATION));
    }

    @Override
    public void render(KiWaveEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float SCALE_MULTIPLIER = 16.0F;
        float length = entity.getBeamLength();
        if (length < 0.1F) length = 0.1F;

        float visualLength = length * SCALE_MULTIPLIER;

        float baseWidth = entity.getSize();
        float ageInTicks = entity.tickCount + partialTick;

        float pulse = 1.0F + (float) Math.sin(ageInTicks * 0.2F) * 0.1F;

        float width = baseWidth * pulse;

        float yaw = entity.getFixedYaw();
        float pitch = entity.getFixedPitch();

        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());
        float[] borderColor = ColorUtils.rgbIntToFloat(entity.getColorBorde());

        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        poseStack.translate(0.0D, -0.5D, 0.5D);

        poseStack.pushPose();

        poseStack.scale(width, width, visualLength);

        poseStack.translate(0.0D, -0.05D, 0.0D);

        this.waveModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        poseStack.pushPose();

        poseStack.scale(1.05F, 1.05F, 1.0F);

        poseStack.translate(0.0D, -0.5D, -0.001D);

        VertexConsumer laserBorderBuffer = buffer.getBuffer(ModRenderTypes.energy(TEXTURE_WAVE_CORE));
        this.waveModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F);

        poseStack.popPose();
        poseStack.popPose();


        poseStack.pushPose();

        poseStack.translate(0.0D, 0.0D, length);

        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        float endBallScale = width * 1.5F;

        poseStack.scale(endBallScale, endBallScale, endBallScale);

        poseStack.translate(0.0D, -0.2D, 0.1D);

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
        VertexConsumer ballBorderBuffer = buffer.getBuffer(ModRenderTypes.kiblast(TEXTURE_BALL_BORDER));
        this.ballModel.renderToBuffer(poseStack, ballBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(KiWaveEntity pEntity) {
        return TEXTURE_WAVE_CORE;
    }
}