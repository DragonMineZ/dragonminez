package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.*;
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
    private static final ResourceLocation TEXTURE_KI = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast.png");
    private static final ResourceLocation TEXTURE_EXP_COLOR = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser_expl.png");

    private static final ResourceLocation TEXTURE_EXPLODE1 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiwave_explode1.png");
    private static final ResourceLocation TEXTURE_EXPLODE2 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiwave_explode2.png");

    private static final ResourceLocation TEXTURE_LASER_EXPLODE1 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser_explode1.png");
    private static final ResourceLocation TEXTURE_LASER_EXPLODE2 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser_explode2.png");

    private final KiLaserModel laserModel;
    private final KiBallPlaneModel ballModel;
    private final KiLaserExplosionModel expModel;
    private final KiLaserExplosion2Model exp2Model;
    private final KiWaveExplodeModel explodeModel;

    public KiLaserRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
        this.laserModel = new KiLaserModel(pContext.bakeLayer(KiLaserModel.LAYER_LOCATION));
        this.ballModel = new KiBallPlaneModel(pContext.bakeLayer(KiBallPlaneModel.LAYER_LOCATION));
        this.expModel = new KiLaserExplosionModel(pContext.bakeLayer(KiLaserExplosionModel.LAYER_LOCATION));
        this.exp2Model = new KiLaserExplosion2Model(pContext.bakeLayer(KiLaserExplosion2Model.LAYER_LOCATION));
        this.explodeModel = new KiWaveExplodeModel(pContext.bakeLayer(KiWaveExplodeModel.LAYER_LOCATION));

    }

    @Override
    public void render(KiLaserEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        int renderType = entity.getKiRenderType();

        float alphaMultiplier = 1.0F;
        float ageInTicks = entity.tickCount + partialTick;
        float width = entity.getSize();

        boolean isFiring = entity.isFiring();
        float castTime = (float) entity.getCastTime();

        if (!isFiring) {
            if (castTime > 0.1F && ageInTicks <= castTime) {
                width *= (ageInTicks / castTime);
            }
        } else {
            width = entity.getSize();
        }

        int maxLife = entity.getMaxLife();
        int fadeTicks = 10;

        if (entity.tickCount >= maxLife - fadeTicks) {
            alphaMultiplier = (maxLife - ageInTicks) / (float) fadeTicks;
            alphaMultiplier = Math.max(0.0F, alphaMultiplier);
        }

        switch (renderType) {
            case 1:
                renderKiMakkankosanpo(entity, partialTick, poseStack, buffer, alphaMultiplier);
                break;
            case 2:
                renderKiLaserAro(entity, partialTick, poseStack, buffer, alphaMultiplier);
                break;
            default:
                renderKiLaser(entity, partialTick, poseStack, buffer, alphaMultiplier);
                break;
        }
    }

    private void renderKiLaser(KiLaserEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, float alphaMultiplier) {
        float SCALE_MULTIPLIER = 16.0F;
        float length = Math.max(entity.getBeamLength(), 0.1F);
        float width = entity.getSize();
        float visualLength = length * SCALE_MULTIPLIER;

        float yaw = entity.getFixedYaw();
        float pitch = entity.getFixedPitch();
        float ageInTicks = entity.tickCount + partialTick;

        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());
        float[] borderColor = ColorUtils.rgbIntToFloat(entity.getColorBorde());

        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        poseStack.translate(0.0D, -0.2D, 0.5D);

        // EXPLOSION
        poseStack.pushPose();

        poseStack.scale(width, width, width);
        poseStack.translate(0.0D, -1.3D, 0.0D);

        this.expModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        this.exp2Model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer expBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_EXP_COLOR));

        float explodePulse = (float) Math.sin(ageInTicks * 4.1F) * 0.1F;
        float explodeJitter = (float) (Math.random() - 0.5) * 0.02F;

        poseStack.pushPose();
        float scale1 = 0.5F * (1.0F + explodePulse + explodeJitter);
        poseStack.scale(scale1, scale1, scale1);
        poseStack.translate(0.0D, 1.2D, -0.35D);
        boolean useFirstTexture = (entity.tickCount / 3) % 2 == 0;
        ResourceLocation currentExplodeTexture = useFirstTexture ? TEXTURE_EXPLODE1 : TEXTURE_EXPLODE2;
        expBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(currentExplodeTexture));
        this.explodeModel.renderToBuffer(poseStack, expBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 0.6F * alphaMultiplier);
        poseStack.popPose();


        poseStack.pushPose();
        poseStack.translate(-0.03D, 1.08D, 0.15D);
        poseStack.scale(0.3F, 0.3F, 0.3F);
        this.expModel.renderToBuffer(poseStack, expBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F * alphaMultiplier);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-0.018D, 0.891D, 0.07D);
        poseStack.scale(0.45F, 0.45F, 0.45F);
        this.exp2Model.renderToBuffer(poseStack, expBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F * alphaMultiplier);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.502D, -0.015D);
        poseStack.scale(0.75F, 0.75F, 0.75F);
        this.expModel.renderToBuffer(poseStack, expBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.8F * alphaMultiplier);
        poseStack.popPose();

        poseStack.popPose();

        // LASER
        poseStack.pushPose();

        poseStack.scale(width, width, visualLength);
        poseStack.translate(0.0D, -1.3D, 0.0D);

        this.laserModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        VertexConsumer laserCoreBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_LASER_CORE));
        this.laserModel.renderToBuffer(poseStack, laserCoreBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 1.0F * alphaMultiplier);

        poseStack.pushPose();
        poseStack.scale(1.3F, 1.3F, 1.0F);
        poseStack.translate(0.0D, -0.34D, 0.0D);
        VertexConsumer laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_LASER_CORE));
        this.laserModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.5F * alphaMultiplier);
        poseStack.popPose();

        poseStack.popPose();

        // BOLA FINAL
        poseStack.pushPose();

        poseStack.translate(0.0D, 0.0D, length);

        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        float endBallScale = width * 0.2F;
        poseStack.scale(endBallScale, endBallScale, endBallScale);

        poseStack.translate(0.1D, 0.25D, 0.0D);

        renderBall(entity, poseStack, buffer, ageInTicks, auraColor, borderColor, 1.0F * alphaMultiplier);

        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderKiLaserAro(KiLaserEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, float alphaMultiplier) {
        float SCALE_MULTIPLIER = 16.0F;
        float length = Math.max(entity.getBeamLength(), 0.1F);
        float width = entity.getSize();
        float visualLength = length * SCALE_MULTIPLIER;

        float yaw = entity.getFixedYaw();
        float pitch = entity.getFixedPitch();
        float ageInTicks = entity.tickCount + partialTick;

        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());
        float[] borderColor = ColorUtils.rgbIntToFloat(entity.getColorBorde());

        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        poseStack.translate(0.0D, -0.2D, 0.5D);

        // EXPLOSION
        poseStack.pushPose();

        poseStack.scale(width, width, width);
        poseStack.translate(0.0D, -1.3D, 0.0D);

        this.expModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        this.exp2Model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer expBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_EXP_COLOR));

        poseStack.pushPose();
        poseStack.translate(-0.03D, 1.08D, 0.15D);
        poseStack.scale(0.3F, 0.3F, 0.3F);
        this.expModel.renderToBuffer(poseStack, expBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F * alphaMultiplier);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-0.018D, 0.891D, 0.07D);
        poseStack.scale(0.45F, 0.45F, 0.45F);
        this.exp2Model.renderToBuffer(poseStack, expBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F * alphaMultiplier);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.502D, -0.015D);
        poseStack.scale(0.75F, 0.75F, 0.75F);
        this.expModel.renderToBuffer(poseStack, expBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F * alphaMultiplier);
        poseStack.popPose();

        poseStack.popPose();

        // LASER CENTRAL
        poseStack.pushPose();

        poseStack.scale(width, width, visualLength);
        poseStack.translate(0.0D, -1.3D, 0.0D);

        this.laserModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        VertexConsumer laserCoreBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_LASER_CORE));
        this.laserModel.renderToBuffer(poseStack, laserCoreBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 1.0F * alphaMultiplier);

        poseStack.pushPose();
        poseStack.scale(1.3F, 1.3F, 1.0F);
        poseStack.translate(0.0D, -0.34D, 0.0D);
        VertexConsumer laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_LASER_CORE));
        this.laserModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.5F * alphaMultiplier);
        poseStack.popPose();

        poseStack.popPose();

        // AROS
        poseStack.pushPose();

        poseStack.scale(width, width, 1.0F);
        poseStack.translate(0.0D, 0.1D, 0.0D);

        float twistSpeed = ageInTicks * 30.0F;
        poseStack.mulPose(Axis.ZP.rotationDegrees(-twistSpeed));

        float radius = 0.01F;
        float aroGrosor = 1.0F;
        float twistsPerBlock = 0.6F;

        int density = 15;
        int totalSegments = (int) (length * density);
        if (totalSegments < 1) totalSegments = 1;

        float segVisualLength = (length / totalSegments) * SCALE_MULTIPLIER * 2.5F;
        VertexConsumer aroBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_LASER_CORE));

        for (int i = 0; i < totalSegments; i++) {
            float currentZ = i * (length / totalSegments);
            float angle1 = currentZ * (360.0F * twistsPerBlock);

            poseStack.pushPose();
            poseStack.translate(0.0D, 0.0D, currentZ);
            poseStack.mulPose(Axis.ZP.rotationDegrees(angle1));
            poseStack.translate(0.0D, radius, 0.0D);
            poseStack.scale(aroGrosor, aroGrosor, segVisualLength);
            poseStack.translate(0.0D, -1.3D, 0.0D);

            this.laserModel.renderToBuffer(poseStack, aroBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F * alphaMultiplier);
            poseStack.popPose();
        }

        poseStack.popPose();

        // ESFERA
        poseStack.pushPose();

        poseStack.translate(0.0D, 0.0D, length);

        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        float endBallScale = width * 0.2F;
        poseStack.scale(endBallScale, endBallScale, endBallScale);

        poseStack.translate(0.1D, 0.25D, 0.0D);

        renderBall(entity, poseStack, buffer, ageInTicks, auraColor, borderColor, 1.0F * alphaMultiplier);

        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderKiMakkankosanpo(KiLaserEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, float alphaMultiplier) {
        float SCALE_MULTIPLIER = 16.0F;
        float length = Math.max(entity.getBeamLength(), 0.1F);
        float width = entity.getSize();
        float visualLength = length * SCALE_MULTIPLIER;

        float yaw = entity.getFixedYaw();
        float pitch = entity.getFixedPitch();
        float ageInTicks = entity.tickCount + partialTick;

        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());
        float[] borderColor = ColorUtils.rgbIntToFloat(entity.getColorBorde());
        float[] brigterauraColor = ColorUtils.lightenColor(auraColor, 0.8f);
        float[] purpleColor = ColorUtils.rgbIntToFloat(0xEF00FF);
        float[] brightnesspurpleColor = ColorUtils.lightenColor(purpleColor, 0.8f);

        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        poseStack.translate(0.0D, -0.2D, 0.5D);

        // EXPLOSION
        poseStack.pushPose();

        poseStack.scale(width, width, width);
        poseStack.translate(0.0D, -1.3D, 0.0D);

        this.expModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        this.exp2Model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer expBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_EXP_COLOR));

        poseStack.pushPose();
        poseStack.translate(-0.03D, 1.08D, 0.15D);
        poseStack.scale(0.3F, 0.3F, 0.3F);
        this.expModel.renderToBuffer(poseStack, expBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F * alphaMultiplier);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-0.018D, 0.891D, 0.07D);
        poseStack.scale(0.45F, 0.45F, 0.45F);
        this.exp2Model.renderToBuffer(poseStack, expBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F * alphaMultiplier);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.502D, -0.015D);
        poseStack.scale(0.75F, 0.75F, 0.75F);
        this.expModel.renderToBuffer(poseStack, expBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F * alphaMultiplier);
        poseStack.popPose();

        poseStack.popPose();

        // LASER CENTRAL
        poseStack.pushPose();

        poseStack.scale(width, width, visualLength);
        poseStack.translate(0.0D, -1.3D, 0.0D);

        this.laserModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        VertexConsumer laserCoreBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_LASER_CORE));
        this.laserModel.renderToBuffer(poseStack, laserCoreBuffer, 15728880, OverlayTexture.NO_OVERLAY, brigterauraColor[0], brigterauraColor[1], brigterauraColor[2], 1.0F * alphaMultiplier);

        poseStack.pushPose();
        poseStack.scale(1.3F, 1.3F, 1.0F);
        poseStack.translate(0.0D, -0.34D, 0.0D);
        VertexConsumer laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_LASER_CORE));
        this.laserModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 0.5F * alphaMultiplier);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(1.5F, 1.5F, 1.0F);
        poseStack.translate(0.0D, -0.5D, 0.0D);
        laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_LASER_CORE));
        this.laserModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightnesspurpleColor[0], brightnesspurpleColor[1], brightnesspurpleColor[2], 0.8F * alphaMultiplier);
        poseStack.popPose();

        poseStack.popPose();

        // AROS
        poseStack.pushPose();
        poseStack.scale(width, width, 1.0F);
        poseStack.translate(0.0D, 0.1D, 0.0D);

        float twistSpeed = ageInTicks * 30.0F;
        poseStack.mulPose(Axis.ZP.rotationDegrees(-twistSpeed));

        float radius = 0.01F;
        float aroGrosor = 1.0F;
        float twistsPerBlock = 0.9F;

        int density = 15;
        int totalSegments = (int) (length * density);
        if (totalSegments < 1) totalSegments = 1;

        float segVisualLength = (length / totalSegments) * SCALE_MULTIPLIER * 2.5F;
        VertexConsumer aroBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_LASER_CORE));

        for (int i = 0; i < totalSegments; i++) {
            float currentZ = i * (length / totalSegments);
            float angle1 = currentZ * (360.0F * twistsPerBlock);

            //AMARILLO
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.0D, currentZ);
            poseStack.mulPose(Axis.ZP.rotationDegrees(angle1));
            poseStack.translate(0.0D, radius, 0.0D);
            poseStack.scale(aroGrosor*1.3F, aroGrosor*1.3F, segVisualLength);
            poseStack.translate(0.0D, -1.3D, -0.02D);
            this.laserModel.renderToBuffer(poseStack, aroBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 1.0F * alphaMultiplier);
            poseStack.popPose();

            // MORADO CLARO
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.0D, currentZ);
            poseStack.mulPose(Axis.ZP.rotationDegrees(angle1));
            poseStack.translate(0.0D, radius, 0.0D);
            poseStack.scale(aroGrosor, aroGrosor, segVisualLength);
            poseStack.translate(0.0D, -1.3D, 0.0D);
            this.laserModel.renderToBuffer(poseStack, aroBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightnesspurpleColor[0], brightnesspurpleColor[1], brightnesspurpleColor[2], 1.0F * alphaMultiplier);
            poseStack.popPose();

            //aro un poco más grande
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.0D, currentZ);
            poseStack.mulPose(Axis.ZP.rotationDegrees(angle1));
            poseStack.translate(0.0D, radius, 0.0D);
            poseStack.scale(aroGrosor*1.5f, aroGrosor*1.5f, segVisualLength);
            poseStack.translate(0.0D, -1.3D, 0.0D);
            this.laserModel.renderToBuffer(poseStack, aroBuffer, 15728880, OverlayTexture.NO_OVERLAY, purpleColor[0], purpleColor[1], purpleColor[2], 0.5F * alphaMultiplier);
            poseStack.popPose();
        }

        poseStack.popPose();

        // ESFERA
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, length);
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        float endBallScale = width * 0.35F;
        poseStack.scale(endBallScale, endBallScale, endBallScale);
        poseStack.translate(0.1D, 0.0D, 0.0D);

        renderBall(entity, poseStack, buffer, ageInTicks, auraColor, borderColor, 1.0F * alphaMultiplier);
        poseStack.popPose();

        // EFECTO DE KI EXPLOSIVO
        float explodePulse = (float) Math.sin(ageInTicks * 4.1F) * 0.1F;
        float explodeJitter = (float) (Math.random() - 0.5) * 0.02F;

        poseStack.pushPose();
        float scale1 = 0.8F * (1.0F + explodePulse + explodeJitter);
        poseStack.scale(scale1, scale1, scale1);
        poseStack.translate(0.0D, -1.5D, -0.35D);
        boolean useFirstTexture = (entity.tickCount / 3) % 2 == 0;
        ResourceLocation currentExplodeTexture = useFirstTexture ? TEXTURE_EXPLODE1 : TEXTURE_EXPLODE2;
        laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(currentExplodeTexture));
        this.explodeModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 0.6F * alphaMultiplier);
        poseStack.popPose();

        // KI CIRCULAR EXPLOSIVO
        poseStack.pushPose();
        scale1 = 0.6F * (1.0F + explodePulse + explodeJitter);
        poseStack.scale(scale1, scale1, scale1);
        poseStack.translate(0.0D, -0.3d, -0.15D);
        useFirstTexture = (entity.tickCount / 3) % 2 == 0;
        currentExplodeTexture = useFirstTexture ? TEXTURE_LASER_EXPLODE1 : TEXTURE_LASER_EXPLODE2;
        laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(currentExplodeTexture));
        this.ballModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 0.9F * alphaMultiplier);
        poseStack.popPose();

        // KI CIRCULAR EXPLOSIVO CLARO
        poseStack.pushPose();
        scale1 = 0.4F * (1.0F + explodePulse + explodeJitter);
        poseStack.scale(scale1, scale1, scale1);
        poseStack.translate(0.0D, -0.3d, -0.25D);
        useFirstTexture = (entity.tickCount / 3) % 2 == 0;
        currentExplodeTexture = useFirstTexture ? TEXTURE_LASER_EXPLODE1 : TEXTURE_LASER_EXPLODE2;
        laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(currentExplodeTexture));
        this.ballModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, brigterauraColor[0], brigterauraColor[1], brigterauraColor[2], 0.7F * alphaMultiplier);
        poseStack.popPose();

        poseStack.popPose();
    }


    private void renderBall(KiLaserEntity entity, PoseStack poseStack, MultiBufferSource buffer, float ageInTicks, float[] auraColor, float[] borderColor, float alpha) {
        this.ballModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        // ¡OJO ACÁ! Ya tenías el parámetro 'alpha', ahora lo usamos de verdad
        VertexConsumer ballCoreBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.ballModel.renderToBuffer(poseStack, ballCoreBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], alpha);

        poseStack.pushPose();
        poseStack.translate(0, 0, -0.01F);
        VertexConsumer ballBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.ballModel.renderToBuffer(poseStack, ballBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], alpha);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(KiLaserEntity pEntity) {
        return TEXTURE_KI;
    }
}
