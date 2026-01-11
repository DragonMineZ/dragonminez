package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.KiBallPlaneModel;
import com.dragonminez.client.init.entities.model.ki.KiLaserExplosion2Model;
import com.dragonminez.client.init.entities.model.ki.KiLaserExplosionModel;
import com.dragonminez.client.init.entities.model.ki.KiLaserModel;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class KiLaserRenderer extends EntityRenderer<KiLaserEntity> {
    private static final ResourceLocation TEXTURE_LASER_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");

    private static final ResourceLocation TEXTURE_BALL_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiball1.png");
    private static final ResourceLocation TEXTURE_BALL_BORDER = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiball1_border.png");

    private static final ResourceLocation TEXTURE_EXP_COLOR = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser_expl.png");

    private final KiLaserModel laserModel;
    private final KiBallPlaneModel ballModel;
    private final KiLaserExplosionModel expModel;
    private final KiLaserExplosion2Model exp2Model;

    public KiLaserRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
        this.laserModel = new KiLaserModel(pContext.bakeLayer(KiLaserModel.LAYER_LOCATION));
        this.ballModel = new KiBallPlaneModel(pContext.bakeLayer(KiBallPlaneModel.LAYER_LOCATION));
        this.expModel = new KiLaserExplosionModel(pContext.bakeLayer(KiLaserExplosionModel.LAYER_LOCATION));
        this.exp2Model = new KiLaserExplosion2Model(pContext.bakeLayer(KiLaserExplosion2Model.LAYER_LOCATION));

    }

    @Override
    public void render(KiLaserEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float SCALE_MULTIPLIER = 16.0F;
        float length = entity.getBeamLength();
        float width = 1.5f;

        if (length < 0.1F) length = 0.1F;

        float visualLength = length * SCALE_MULTIPLIER;

        float yaw = entity.getFixedYaw();
        float pitch = entity.getFixedPitch();
        float ageInTicks = entity.tickCount + partialTick;

        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());
        float[] borderColor = ColorUtils.rgbIntToFloat(entity.getColorBorde());

        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        poseStack.translate(0.0D, -0.5D, 0.5D);


        // EXPLOSION LASER
        poseStack.pushPose();

        poseStack.translate(0.0D, -1.7D, 0.0D);

        this.expModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        this.exp2Model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer expBuffer = buffer.getBuffer(ModRenderTypes.glow(TEXTURE_EXP_COLOR));


        poseStack.pushPose();
        float scaleSmall = width * 0.3F;
        poseStack.scale(scaleSmall, scaleSmall, scaleSmall);
        poseStack.translate(-0.1D, 3.6D, 0.5D);
        this.expModel.renderToBuffer(poseStack, expBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F);
        poseStack.popPose();


        poseStack.pushPose();
        float scaleMedium = width * 0.45F;
        poseStack.translate(0.0D, 0.0D, 0.01D);
        poseStack.scale(scaleMedium, scaleMedium, scaleMedium);
        poseStack.translate(-0.04D, 1.98D, 0.15D);
        this.exp2Model.renderToBuffer(poseStack, expBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F);
        poseStack.popPose();


        poseStack.pushPose();
        float scaleBig = width * 0.75F;
        poseStack.translate(0.0D, 0.0D, 0.02D);
        poseStack.scale(scaleBig, scaleBig, scaleBig);
        poseStack.translate(0.0D, 0.67D, -0.02D);
        this.expModel.renderToBuffer(poseStack, expBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.8F);
        poseStack.popPose();

        poseStack.popPose();

        //RASHO LASER
        poseStack.pushPose();

        poseStack.translate(0.0D, -1.7D, 0.0D);
        poseStack.scale(width, width, visualLength);

        this.laserModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        VertexConsumer laserCoreBuffer = buffer.getBuffer(ModRenderTypes.energy(TEXTURE_LASER_CORE));
        this.laserModel.renderToBuffer(poseStack, laserCoreBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 1.0F);

        poseStack.pushPose();
        poseStack.translate(0.0D, -0.3D, 0.0D);
        poseStack.scale(1.2F, 1.2F, 1.0F);
        VertexConsumer laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow(TEXTURE_LASER_CORE));
        this.laserModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F);
        poseStack.popPose();

        poseStack.popPose();

        //BOLA FINAL
        poseStack.pushPose();

        poseStack.translate(0.0D, 0.0D, length);

        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        float endBallScale = width * 0.2F;
        poseStack.translate(0.06D, 0.33D, 0.3D);
        poseStack.scale(endBallScale, endBallScale, endBallScale);

        renderBall(entity, poseStack, buffer, ageInTicks, auraColor, borderColor, 1.0F);

        poseStack.popPose();


        poseStack.popPose();
    }


    private void renderBall(KiLaserEntity entity, PoseStack poseStack, MultiBufferSource buffer, float ageInTicks, float[] auraColor, float[] borderColor, float alpha) {
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
    public ResourceLocation getTextureLocation(KiLaserEntity pEntity) {
        return TEXTURE_BALL_CORE;
    }
}
