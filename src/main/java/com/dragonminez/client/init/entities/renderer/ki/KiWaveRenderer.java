package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.events.AuraRenderHandler;
import com.dragonminez.client.init.entities.model.ki.*;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Random;

public class KiWaveRenderer extends EntityRenderer<KiWaveEntity> {

    private static final ResourceLocation TEXTURE_WAVE_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiwave.png");

    private static final ResourceLocation TEXTURE_KI = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast.png");
    private static final ResourceLocation TEXTURE_BORDER = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiexp1_border.png");

    private static final ResourceLocation TEXTURE_EXPLODE1 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiwave_explode1.png");
    private static final ResourceLocation TEXTURE_EXPLODE2 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiwave_explode2.png");

    private static final ResourceLocation TEXTURE_LASER_EXPLODE1 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser_explode1.png");
    private static final ResourceLocation TEXTURE_LASER_EXPLODE2 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser_explode2.png");
    private static final ResourceLocation SPARK_TEX_0 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/rayo_0.png");

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
        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());
        float[] borderColor = ColorUtils.rgbIntToFloat(entity.getColorBorde());

        float exactAge = entity.tickCount + partialTick;
        float fadeAlpha = 1.0F;

        int maxLife = entity.getMaxLife();
        int fadeTicks = 20;

        if (entity.tickCount >= maxLife - fadeTicks) {
            fadeAlpha = (maxLife - exactAge) / (float) fadeTicks;
            fadeAlpha = Math.max(0.0F, fadeAlpha);
        }

        int renderType = entity.getKiRenderType();

        switch (renderType) {
            case 1: // Kamehameha
            case 2: // Galick Gun
                KiRenderWaveBrightness(entity, partialTick, poseStack, buffer, auraColor, borderColor, fadeAlpha);
                break;
            case 3: // FINAL FLASH AAAAAAAAAAAA
                KiRenderWaveDouble(entity, partialTick, poseStack, buffer, auraColor, borderColor, fadeAlpha);
                break;
            default: // Rayo Genérico
                KiRenderWave(entity, partialTick, poseStack, buffer, auraColor, borderColor, fadeAlpha);
                break;
        }
    }

    private void KiRenderWave(KiWaveEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, float[] auraColor, float[] borderColor, float fadeAlpha) {
        float ageInTicks = entity.tickCount + partialTick;
        float castTime = (float) entity.getCastWave();
        float halfCastTime = castTime / 2.0F;
        float targetCastSize = entity.getCastSize();
        float finalSize = entity.getSize();

        float currentWidth;
        if (ageInTicks <= halfCastTime) {
            float progress = ageInTicks / halfCastTime;
            currentWidth = targetCastSize * progress;
        } else if (ageInTicks <= castTime) {
            currentWidth = targetCastSize;
        } else {
            currentWidth = finalSize;
        }

        boolean isFiring = ageInTicks > castTime;

        float basePulse = 1.0F + (float) Math.sin(ageInTicks * 1.5F) * 0.15F;
        float jitter = (float) (Math.random() - 0.5) * 0.05F;
        float width = currentWidth * (basePulse + jitter);

        float yaw = entity.getFixedYaw();
        float pitch = entity.getFixedPitch();
        float[] brightAuraColor = ColorUtils.lightenColor(auraColor, 0.45f);

        poseStack.pushPose();

        if (!isFiring) {
            float startBallScale = width * 1.5F;
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            poseStack.translate(0.0D, -0.35D, 0.0D);
            renderKiBlast(poseStack, entity, buffer, 1.0F, ageInTicks, auraColor, brightAuraColor, borderColor, fadeAlpha);
            poseStack.popPose();
            return;
        }

        float SCALE_MULTIPLIER = 16.0F;
        float length = Math.max(entity.getBeamLength(), 0.1F);

        poseStack.translate(0.0D, 0.5D, 0.0D);
        Vec3 dir = net.minecraft.world.phys.Vec3.directionFromRotation(pitch, yaw);

        poseStack.pushPose();
        poseStack.translate(0.0D, -0.5D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.translate(0.0D, 0.0D, 0.5D);

        float tubeLength = Math.max(length - 0.8F, 0.1F);
        renderKiWave2DFullColor(poseStack, buffer, entity, ageInTicks, width, tubeLength, auraColor, borderColor, fadeAlpha);

        float explodePulse = (float) Math.sin(ageInTicks * 4.1F) * 0.1F;
        float explodeJitter = (float) (Math.random() - 0.5) * 0.02F;

        //EXPLOSION
        poseStack.pushPose();
        float scale1 = width * 1.0F * (1.0F + explodePulse + explodeJitter);
        poseStack.scale(scale1, scale1, scale1);
        poseStack.translate(0.0D, -1.7D, -0.2D);
        boolean useFirstTexture = (entity.tickCount / 3) % 2 == 0;
        ResourceLocation currentExplodeTexture = useFirstTexture ? TEXTURE_EXPLODE1 : TEXTURE_EXPLODE2;
        VertexConsumer laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(currentExplodeTexture));
        this.explodeModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 0.6F * fadeAlpha);
        poseStack.popPose();

        poseStack.pushPose();
        float scale2 = width * 1.5F * (1.0F - explodePulse + explodeJitter);
        poseStack.scale(scale2, scale2, scale2);
        poseStack.translate(1.0D, -1.3D, -0.1D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(35.0F));
        useFirstTexture = (entity.tickCount / 3) % 2 == 0;
        currentExplodeTexture = useFirstTexture ? TEXTURE_EXPLODE1 : TEXTURE_EXPLODE2;
        laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(currentExplodeTexture));
        this.explodeModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F * fadeAlpha);
        poseStack.popPose();

        poseStack.popPose();

        //ESFERA INICIO
        poseStack.pushPose();
        Vec3 startPos = dir.scale(0.1D);
        poseStack.translate(startPos.x, startPos.y - 0.5D, startPos.z);
        float startBallScale = width * 1.5F;
        poseStack.scale(startBallScale, startBallScale, startBallScale);
        poseStack.translate(0.0D, -0.35D, 0.0D);
        renderKiBlast(poseStack, entity, buffer, 1.0F, ageInTicks, auraColor, brightAuraColor, borderColor, fadeAlpha);
        poseStack.popPose();

        // ESFERA DEL FINAL (IMPACTO)
        poseStack.pushPose();
        Vec3 endPos = dir.scale(0.5D + length);
        poseStack.translate(endPos.x, endPos.y - 0.5D, endPos.z);
        float endBallScale = width * 2F;
        poseStack.scale(endBallScale, endBallScale, endBallScale);
        poseStack.translate(0.0D, -0.35D, 0.0D);
        renderKiBlast(poseStack, entity, buffer, 1.0F, ageInTicks, auraColor, brightAuraColor, borderColor, fadeAlpha);
        poseStack.popPose();

        poseStack.popPose(); // FIN DEL PUSH GLOBAL
    }

    private void KiRenderWaveBrightness(KiWaveEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, float[] auraColor, float[] borderColor, float fadeAlpha) {
        float ageInTicks = entity.tickCount + partialTick;
        float castTime = (float) entity.getCastWave();
        float halfCastTime = castTime / 2.0F;
        float targetCastSize = entity.getCastSize();
        float finalSize = entity.getSize();

        float currentWidth;
        if (ageInTicks <= halfCastTime) {
            float progress = ageInTicks / halfCastTime;
            currentWidth = targetCastSize * progress;
        } else if (ageInTicks <= castTime) {
            currentWidth = targetCastSize;
        } else {
            currentWidth = finalSize;
        }

        boolean isFiring = ageInTicks > castTime;

        float basePulse = 1.0F + (float) Math.sin(ageInTicks * 1.5F) * 0.15F;
        float jitter = (float) (Math.random() - 0.5) * 0.05F;
        float width = currentWidth * (basePulse + jitter);

        float yaw = entity.getFixedYaw();
        float pitch = entity.getFixedPitch();
        float[] brightAuraColor = ColorUtils.lightenColor(auraColor, 0.85f);

        poseStack.pushPose(); // PUSH GLOBAL

        // SOLO LA BOLA EN EL CASTEO
        if (!isFiring) {
            float startBallScale = width * 1.5F;
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            poseStack.translate(0.0D, -0.35D, 0.0D);
            renderKiBlast(poseStack, entity, buffer, 1.0F, ageInTicks, auraColor, brightAuraColor, borderColor, fadeAlpha);
            poseStack.popPose();

            if (entity.getKiRenderType() == 2) {
                renderGalickLightning(poseStack, entity, buffer, borderColor, fadeAlpha, ageInTicks, width, true);
            }

            return;
        }



        float length = Math.max(entity.getBeamLength(), 0.1F);

        poseStack.translate(0.0D, 0.5D, 0.0D);
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);

        poseStack.pushPose();
        poseStack.translate(0.0D, -0.5D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.translate(0.0D, 0.0D, 0.5D);

        float tubeLength = Math.max(length - 0.8F, 0.1F);
        renderKiWave2D(poseStack, buffer, entity, ageInTicks, width, tubeLength, auraColor, borderColor, fadeAlpha);

        float explodePulse = (float) Math.sin(ageInTicks * 4.1F) * 0.1F;
        float explodeJitter = (float) (Math.random() - 0.5) * 0.02F;


        poseStack.pushPose();
        float scale1 = width * 1.5F * (1.0F + explodePulse + explodeJitter);
        poseStack.scale(scale1, scale1, scale1);
        poseStack.translate(0.0D, -0.5d, -0.05D);
        boolean useFirstTexture = (entity.tickCount / 3) % 2 == 0;
        ResourceLocation currentExplodeTexture = useFirstTexture ? TEXTURE_LASER_EXPLODE1 : TEXTURE_LASER_EXPLODE2;
        VertexConsumer laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(currentExplodeTexture));
        this.ballModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 0.9F * fadeAlpha);
        poseStack.popPose();

        //KI CIRCULAR CLARO
        poseStack.pushPose();
        scale1 = width * 0.8F * (1.0F + explodePulse + explodeJitter);
        poseStack.scale(scale1, scale1, scale1);
        poseStack.translate(0.0D, -0.5d, -0.15D);
        useFirstTexture = (entity.tickCount / 3) % 2 == 0;
        currentExplodeTexture = useFirstTexture ? TEXTURE_LASER_EXPLODE1 : TEXTURE_LASER_EXPLODE2;
        laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(currentExplodeTexture));
        this.ballModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 0.7F * fadeAlpha);
        poseStack.popPose();

        // AURA DE KI EXPLOTANDO
        poseStack.pushPose();
        scale1 = width * 1.3F * (1.0F + explodePulse + explodeJitter);
        poseStack.scale(scale1, scale1, scale1);
        poseStack.translate(0.0D, -1.7D, -0.35D);
        useFirstTexture = (entity.tickCount / 3) % 2 == 0;
        currentExplodeTexture = useFirstTexture ? TEXTURE_EXPLODE1 : TEXTURE_EXPLODE2;
        laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(currentExplodeTexture));
        this.explodeModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, auraColor[0], auraColor[1], auraColor[2], 0.6F * fadeAlpha);
        poseStack.popPose();

        poseStack.pushPose();
        float scale2 = width * 1.8F * (1.0F - explodePulse + explodeJitter);
        poseStack.scale(scale2, scale2, scale2);
        poseStack.translate(1.0D, -1.3D, -0.3D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(35.0F));
        useFirstTexture = (entity.tickCount / 3) % 2 == 0;
        currentExplodeTexture = useFirstTexture ? TEXTURE_EXPLODE1 : TEXTURE_EXPLODE2;
        laserBorderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(currentExplodeTexture));
        this.explodeModel.renderToBuffer(poseStack, laserBorderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.6F* fadeAlpha);
        poseStack.popPose();

        poseStack.popPose(); // FIN DEL PUSH DE ROTACIÓN

        //ESFERA INICIO
        poseStack.pushPose();
        Vec3 startPos = dir.scale(0.1D);
        poseStack.translate(startPos.x, startPos.y - 0.5D, startPos.z);
        float startBallScale = width * 1.5F;
        poseStack.scale(startBallScale, startBallScale, startBallScale);
        poseStack.translate(0.0D, -0.35D, 0.0D);
        renderKiBlast(poseStack, entity, buffer, 1.0F, ageInTicks, auraColor, brightAuraColor, borderColor, fadeAlpha);
        poseStack.popPose();

        if (entity.getKiRenderType() == 2) {
            poseStack.pushPose();
            double offsetForward = 0.6D;
            Vec3 lightningPos = startPos.add(dir.scale(offsetForward));

            poseStack.translate(lightningPos.x, lightningPos.y, lightningPos.z);

            renderGalickLightning(poseStack, entity, buffer, borderColor, fadeAlpha, ageInTicks, width, true);
            poseStack.popPose();
        }

        // ESFERA FINAL
        poseStack.pushPose();
        Vec3 endPos = dir.scale(0.5D + length);
        poseStack.translate(endPos.x, endPos.y - 0.5D, endPos.z);
        float endBallScale = width * 2F;
        poseStack.scale(endBallScale, endBallScale, endBallScale);
        poseStack.translate(0.0D, -0.35D, 0.0D);
        renderKiBlast(poseStack, entity, buffer, 1.0F, ageInTicks, auraColor, brightAuraColor, borderColor, fadeAlpha);
        poseStack.popPose();

        poseStack.popPose(); // push global
    }

    private void KiRenderWaveDouble(KiWaveEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, float[] auraColor, float[] borderColor, float fadeAlpha) {
        float ageInTicks = entity.tickCount + partialTick;
        float castTime = (float) entity.getCastWave();
        float targetCastSize = entity.getCastSize();
        float finalSize = entity.getSize();

        // Progreso de carga (de 0.0 a 1.0)
        float chargeProgress = Math.min(1.0F, ageInTicks / castTime);

        float currentWidth = (ageInTicks <= castTime) ? (targetCastSize * chargeProgress) : finalSize;
        boolean isFiring = ageInTicks > castTime;

        float basePulse = 1.0F + (float) Math.sin(ageInTicks * 1.5F) * 0.15F;
        float width = currentWidth * basePulse;

        float yaw = entity.getFixedYaw();
        float pitch = entity.getFixedPitch();
        float[] brightAuraColor = ColorUtils.lightenColor(auraColor, 0.85f);
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);

        poseStack.pushPose();

        if (!isFiring) {
            float startBallScale = width * 1.2F;

            float initialSpread = 3.0F;
            float hitboxWidth = entity.getOwner() != null ? entity.getOwner().getBbWidth() : 0.6F;

            float lateralOffset = (hitboxWidth * initialSpread) * (1.0F - chargeProgress);

            // Esfera Derecha
            poseStack.pushPose();
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            poseStack.translate(lateralOffset, -0.35D, 0.0D);
            renderKiBlast(poseStack, entity, buffer, 1.0F, ageInTicks, auraColor, brightAuraColor, borderColor, fadeAlpha);
            renderGalickLightning(poseStack, entity, buffer, borderColor, fadeAlpha, ageInTicks, width, false);
            poseStack.popPose();

            // Esfera Izquierda
            poseStack.pushPose();
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            poseStack.translate(-lateralOffset, -0.35D, 0.0D);
            renderKiBlast(poseStack, entity, buffer, 1.0F, ageInTicks, auraColor, brightAuraColor, borderColor, fadeAlpha);
            renderGalickLightning(poseStack, entity, buffer, borderColor, fadeAlpha, ageInTicks, width, false);
            poseStack.popPose();

            poseStack.popPose();
            return;
        }

        float length = Math.max(entity.getBeamLength(), 0.1F);
        poseStack.translate(0.0D, 0.5D, 0.0D);

        poseStack.pushPose();
        poseStack.translate(0.0D, -0.5D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.translate(0.0D, 0.0D, 0.5D);
        renderKiWave2D(poseStack, buffer, entity, ageInTicks, width, Math.max(length - 0.8F, 0.1F), auraColor, borderColor, fadeAlpha);
        poseStack.popPose();

        poseStack.pushPose();
        Vec3 startPos = dir.scale(0.1D);
        poseStack.translate(startPos.x, startPos.y - 0.5D, startPos.z);
        float startBallScale = width * 1.8F;
        poseStack.scale(startBallScale, startBallScale, startBallScale);
        poseStack.translate(0.0D, -0.35D, 0.0D);
        renderKiBlast(poseStack, entity, buffer, 1.0F, ageInTicks, auraColor, brightAuraColor, borderColor, fadeAlpha);
        renderGalickLightning(poseStack, entity, buffer, borderColor, fadeAlpha, ageInTicks, width, true);
        poseStack.popPose();

        poseStack.pushPose();
        Vec3 endPos = dir.scale(0.5D + length);
        poseStack.translate(endPos.x, endPos.y - 0.5D, endPos.z);
        poseStack.scale(width * 2.5F, width * 2.5F, width * 2.5F);
        poseStack.translate(0.0D, -0.35D, 0.0D);
        renderKiBlast(poseStack, entity, buffer, 1.0F, ageInTicks, auraColor, brightAuraColor, borderColor, fadeAlpha);
        poseStack.popPose();

        poseStack.popPose();
    }


    private void renderKiWave2DFullColor(PoseStack poseStack, MultiBufferSource buffer, KiWaveEntity entity, float ageInTicks, float width, float visualLength, float[] auraColor, float[] borderColor, float fadeAlpha) {
        poseStack.pushPose();
        float[] brightAuraColor = ColorUtils.lightenColor(auraColor, 0.45f);

        poseStack.scale(width, width, visualLength);

        this.wave2Model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        poseStack.pushPose();
        poseStack.scale(1.15F, 1.15F, 1.0F);
        poseStack.translate(0.0D, -1.5D, -0.001D);
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_WAVE_CORE));
        this.wave2Model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F * fadeAlpha);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(1.3F, 1.0F, 1.0F);
        poseStack.translate(0.0D, -1.5D, -0.001D);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_WAVE_CORE));
        this.wave2Model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F * fadeAlpha);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderKiWave2D(PoseStack poseStack, MultiBufferSource buffer, KiWaveEntity entity, float ageInTicks, float width, float visualLength, float[] auraColor, float[] borderColor, float fadeAlpha) {
        poseStack.pushPose();
        float[] brightAuraColor = ColorUtils.lightenColor(auraColor, 0.85f);

        poseStack.scale(width, width, visualLength);

        this.wave2Model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        poseStack.pushPose();
        poseStack.scale(1.15F, 1.15F, 1.0F);
        poseStack.translate(0.0D, -1.5D, -0.001D);
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_WAVE_CORE));
        this.wave2Model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 1.0F * fadeAlpha);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(1.3F, 1.0F, 1.0F);
        poseStack.translate(0.0D, -1.5D, -0.001D);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_WAVE_CORE));
        this.wave2Model.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F * fadeAlpha);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderKiBlast(PoseStack poseStack, AbstractKiProjectile entity, MultiBufferSource buffer, float scale, float ageInTicks, float[] coreColor, float[] brightAuraColor, float[] borderColor, float fadeAlpha) {
        poseStack.pushPose();

        float jitterSpeed = ageInTicks * 20.0F;
        float intensity = 0.03F;

        float shakeX = (float) (Math.sin(jitterSpeed) * intensity);
        float shakeY = (float) (Math.cos(jitterSpeed * 1.2) * intensity);
        float shakeZ = (float) (Math.sin(jitterSpeed * 0.8) * intensity);

        poseStack.scale(scale + shakeX, scale + shakeY, scale + shakeZ);

        this.ballModel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        VertexConsumer borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.translate(0.0F, -0.27F, 0.0F);

        poseStack.pushPose();
        poseStack.translate(0, 0.15, 0.000f);
        poseStack.scale(1.0f, 1.0f, 1.0f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.ballModel.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.2F * fadeAlpha);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.2, 0.001f);
        poseStack.scale(0.85f, 0.85f, 0.85f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.ballModel.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, borderColor[0], borderColor[1], borderColor[2], 0.8F * fadeAlpha);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.23, 0.002f);
        poseStack.scale(0.75f, 0.75f, 0.75f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.ballModel.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.6F * fadeAlpha);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.3, 0.003f);
        poseStack.scale(0.6f, 0.6f, 0.6f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.ballModel.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.8F * fadeAlpha);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.35, 0.004f);
        poseStack.scale(0.5f, 0.5f, 0.5f);
        solidBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_KI));
        this.ballModel.renderToBuffer(poseStack, solidBuffer, 15728880, OverlayTexture.NO_OVERLAY, brightAuraColor[0], brightAuraColor[1], brightAuraColor[2], 1.0F * fadeAlpha);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.37, 0.005f);
        poseStack.scale(0.45f, 0.45f, 0.45f);
        borderBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.ballModel.renderToBuffer(poseStack, borderBuffer, 15728880, OverlayTexture.NO_OVERLAY, coreColor[0], coreColor[1], coreColor[2], 0.3F * fadeAlpha);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderGalickLightning(PoseStack poseStack, KiWaveEntity entity, MultiBufferSource buffer, float[] color, float alpha, float ageInTicks, float dynamicWidth, boolean isFiring) {
        ShaderInstance shader = DMZShaders.lightningShader;
        VertexBuffer mesh = AuraRenderHandler.getLightningMesh();

        if (shader == null || mesh == null) return;

        shader.safeGetUniform("time").set(ageInTicks / 20.0f);
        shader.safeGetUniform("speedModifier").set(isFiring ? 2.5f : 1.5f);
        shader.safeGetUniform("color1").set(1.0f, 1.0f, 1.0f);
        shader.safeGetUniform("color2").set(color[0], color[1], color[2]);
        shader.safeGetUniform("alp1").set(alpha);
        shader.safeGetUniform("alp2").set(0.1f * alpha);
        shader.safeGetUniform("projectionMatrix").set(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix());

        RenderType lightningType = ModRenderTypes.getCustomLightning(SPARK_TEX_0);
        lightningType.setupRenderState();

        shader.apply();
        mesh.bind();

        Random seededRand = new Random((long) (entity.getId() + ((int)ageInTicks * 5)));

        float baseScale = dynamicWidth * (isFiring ? 2.2f : 1.8f);

        for (int i = 0; i < (isFiring ? 6 : 4); i++) {
            poseStack.pushPose();
            poseStack.translate(0.0D, isFiring ? -0.2D : -0.3D, 0.0D);

            poseStack.mulPose(Axis.XP.rotationDegrees(seededRand.nextFloat() * 360));
            poseStack.mulPose(Axis.YP.rotationDegrees(seededRand.nextFloat() * 360));
            poseStack.mulPose(Axis.ZP.rotationDegrees(seededRand.nextFloat() * 360));

            float individualScale = baseScale * (0.8f + seededRand.nextFloat() * 0.4f);
            poseStack.scale(individualScale, individualScale, individualScale);

            shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
            shader.safeGetUniform("normalMatrix").set(new Matrix4f(new Matrix3f(poseStack.last().normal())));
            shader.apply();

            mesh.drawWithShader(poseStack.last().pose(), com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix(), shader);
            poseStack.popPose();
        }

        VertexBuffer.unbind();
        shader.clear();
        lightningType.clearRenderState();
    }

    @Override
    public ResourceLocation getTextureLocation(KiWaveEntity pEntity) {
        return TEXTURE_WAVE_CORE;
    }
}