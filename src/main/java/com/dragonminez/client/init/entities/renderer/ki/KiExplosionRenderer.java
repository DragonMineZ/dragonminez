package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.KiBallModel;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.KiExplosionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class KiExplosionRenderer extends EntityRenderer<KiExplosionEntity> {

    private static final ResourceLocation TEXTURE_BORDER = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiexp1_border.png");
    private static final ResourceLocation TEXTURE_EXPLOSION = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");

    private final KiBallModel modelexp;

    public KiExplosionRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);

        this.modelexp = new KiBallModel(pContext.bakeLayer(KiBallModel.LAYER_LOCATION));

    }

    @Override
    public void render(KiExplosionEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float ageInTicks = entity.tickCount + partialTick;

        float fadeAlpha = 1.0F;
        int maxLife = entity.getMaxLife();
        int fadeTicks = 10;

        if (entity.tickCount >= maxLife - fadeTicks) {
            fadeAlpha = (maxLife - ageInTicks) / (float) fadeTicks;
            fadeAlpha = Math.max(0.0F, fadeAlpha);
        }

        renderExplosionCore2(entity, partialTick, poseStack, buffer, fadeAlpha);
    }

    private void renderExplosionCore(KiExplosionEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, float fadealpha) {
        float ageInTicks = entity.tickCount + partialTick;
        float castTime = (float) entity.getCastExplosion();
        float halfCastTime = castTime / 2.0F;
        float expansionTime = 60.0F;

        float maxRadius = entity.getMaxRadius();
        float baseScale = entity.getSize();

        float currentScale;

        boolean isFiring = entity.isFiring();
        int fireTick = entity.getFireTick();

        if (!isFiring) {
            if (ageInTicks <= halfCastTime) {
                currentScale = baseScale * (ageInTicks / halfCastTime);
            } else {
                currentScale = baseScale;
            }
        } else {
            float activeTicks = ageInTicks - fireTick;
            if (activeTicks <= expansionTime) {
                float progress = activeTicks / expansionTime;
                currentScale = baseScale + ((maxRadius - baseScale) * progress);
            } else {
                currentScale = maxRadius;
            }
        }

        float[] auraColor = entity.getRgbColorMain();
        float[] brightnessColor = ColorUtils.lightenColor(auraColor,  0.5f);

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 0.0D);
        poseStack.scale(currentScale, currentScale, currentScale);
        poseStack.translate(0.0D, -1.3D, 0.0D);

        this.modelexp.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        VertexConsumer auraBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_EXPLOSION));

        this.modelexp.renderToBuffer(poseStack, auraBuffer, 15728880, OverlayTexture.NO_OVERLAY,
                brightnessColor[0], brightnessColor[1], brightnessColor[2], 0.5F * fadealpha);

        poseStack.scale(1.1f, 1.1f, 1.1f);
        poseStack.translate(0.0D, -0.1D, 0.0D);
        this.modelexp.renderToBuffer(poseStack, auraBuffer, 15728880, OverlayTexture.NO_OVERLAY,
                auraColor[0], auraColor[1], auraColor[2], 0.3F * fadealpha);

        poseStack.scale(1.3f, 1.3f, 1.3f);
        poseStack.translate(0.0D, -0.1D, 0.0D);
        this.modelexp.renderToBuffer(poseStack, auraBuffer, 15728880, OverlayTexture.NO_OVERLAY,
                auraColor[0], auraColor[1], auraColor[2], 0.1F * fadealpha);

        poseStack.popPose();
    }

    private void renderExplosionCore2(KiExplosionEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, float fadealpha) {
        float ageInTicks = entity.tickCount + partialTick;
        float castTime = (float) entity.getCastExplosion();
        float halfCastTime = castTime / 2.0F;
        float expansionTime = 60.0F;

        float maxRadius = entity.getMaxRadius();
        float baseScale = entity.getSize();

        float currentScale;

        boolean isFiring = entity.isFiring();
        int fireTick = entity.getFireTick();

        if (!isFiring) {
            if (ageInTicks <= halfCastTime) {
                currentScale = baseScale * (ageInTicks / halfCastTime);
            } else {
                currentScale = baseScale;
            }
        } else {
            float activeTicks = ageInTicks - fireTick;
            if (activeTicks <= expansionTime) {
                float progress = activeTicks / expansionTime;
                currentScale = baseScale + ((maxRadius - baseScale) * progress);
            } else {
                currentScale = maxRadius;
            }
        }

        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());
        float[] brightnessColor = ColorUtils.lightenColor(auraColor,  0.7f);

        poseStack.pushPose();

        poseStack.translate(0.0D, 0.0D, 0.0D);

        poseStack.scale(currentScale, currentScale, currentScale);

        poseStack.mulPose(Axis.YP.rotationDegrees(ageInTicks * 85.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(ageInTicks * 45.0F));

        VertexConsumer auraBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_EXPLOSION));

        poseStack.pushPose();
        poseStack.scale(0.85f, 0.85f, 0.85f);
        renderSphere(poseStack, auraBuffer, 15728880, brightnessColor, 0.6F * fadealpha, ageInTicks);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(1.0f, 1.0f, 1.0f);
        renderSphere(poseStack, auraBuffer, 15728880, auraColor, 0.3F * fadealpha, ageInTicks);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(1.15f, 1.15f, 1.15f);
        poseStack.mulPose(Axis.XP.rotationDegrees(ageInTicks * -2.5F));
        renderSphere(poseStack, auraBuffer, 15728880, auraColor, 0.15F * fadealpha, ageInTicks);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderSphere(PoseStack poseStack, VertexConsumer consumer, int packedLight, float[] color, float alpha, float ageInTicks) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        int rings = 4;
        int sectors = 6;

        float r = color[0];
        float g = color[1];
        float b = color[2];

        float textureScroll = ageInTicks * 0.05F;

        for (int i = 0; i < rings; i++) {
            float phi1 = (float) (Math.PI * i / rings);
            float phi2 = (float) (Math.PI * (i + 1) / rings);

            float y1 = (float) Math.cos(phi1);
            float y2 = (float) Math.cos(phi2);

            float r1 = (float) Math.sin(phi1);
            float r2 = (float) Math.sin(phi2);

            for (int j = 0; j < sectors; j++) {
                float theta1 = (float) (2.0 * Math.PI * j / sectors);
                float theta2 = (float) (2.0 * Math.PI * (j + 1) / sectors);

                float x11 = r1 * (float) Math.cos(theta1);
                float z11 = r1 * (float) Math.sin(theta1);
                float x12 = r1 * (float) Math.cos(theta2);
                float z12 = r1 * (float) Math.sin(theta2);

                float x21 = r2 * (float) Math.cos(theta1);
                float z21 = r2 * (float) Math.sin(theta1);
                float x22 = r2 * (float) Math.cos(theta2);
                float z22 = r2 * (float) Math.sin(theta2);

                float u1 = (float) j / sectors;
                float u2 = (float) (j + 1) / sectors;
                float v1 = ((float) i / rings) - textureScroll;
                float v2 = ((float) (i + 1) / rings) - textureScroll;

                consumer.vertex(pose, x11, y1, z11).color(r, g, b, alpha).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, x11, y1, z11).endVertex();
                consumer.vertex(pose, x21, y2, z21).color(r, g, b, alpha).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, x21, y2, z21).endVertex();
                consumer.vertex(pose, x22, y2, z22).color(r, g, b, alpha).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, x22, y2, z22).endVertex();
                consumer.vertex(pose, x11, y1, z11).color(r, g, b, alpha).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, x11, y1, z11).endVertex();

                consumer.vertex(pose, x11, y1, z11).color(r, g, b, alpha).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, x11, y1, z11).endVertex();
                consumer.vertex(pose, x22, y2, z22).color(r, g, b, alpha).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, x22, y2, z22).endVertex();
                consumer.vertex(pose, x12, y1, z12).color(r, g, b, alpha).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, x12, y1, z12).endVertex();
                consumer.vertex(pose, x11, y1, z11).color(r, g, b, alpha).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, x11, y1, z11).endVertex();
            }
        }
    }
    @Override
    public ResourceLocation getTextureLocation(KiExplosionEntity pEntity) {
        return TEXTURE_BORDER;
    }
}
