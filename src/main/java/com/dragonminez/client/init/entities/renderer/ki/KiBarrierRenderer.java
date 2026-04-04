package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.KiBarrierEntity;
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

public class KiBarrierRenderer extends EntityRenderer<KiBarrierEntity> {
    private static final ResourceLocation TEXTURE_BARRIER = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");

    public KiBarrierRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(KiBarrierEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (!entity.isFiring()) return;

        float ageInTicks = entity.tickCount + partialTick;
        float currentScale = entity.getCurrentSize();
        if (currentScale < 0.1F) return;

        float[] colorMain = entity.getRgbColorMain();
        float[] colorBorder = entity.getRgbColorBorder();
        float[] brightnessColor = ColorUtils.lightenColor(colorMain, 0.4f);

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() / 2.0F, 0.0D);
        poseStack.scale(currentScale, currentScale, currentScale);

        // Rotación de escudo activa
        poseStack.mulPose(Axis.YP.rotationDegrees(ageInTicks * 80.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(ageInTicks * 50.0F));

        VertexConsumer auraBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BARRIER));

        // Capa Núcleo
        poseStack.pushPose();
        poseStack.scale(0.85f, 0.85f, 0.85f);
        renderSphere(poseStack, auraBuffer, 15728880, brightnessColor, 0.4F, ageInTicks);
        poseStack.popPose();

        // Capa Principal
        renderSphere(poseStack, auraBuffer, 15728880, colorMain, 0.25F, ageInTicks);

        // Capa Borde
        poseStack.pushPose();
        poseStack.scale(1.1f, 1.1f, 1.1f);
        renderSphere(poseStack, auraBuffer, 15728880, colorBorder, 0.15F, ageInTicks);
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
    public ResourceLocation getTextureLocation(KiBarrierEntity pEntity) {
        return TEXTURE_BARRIER;
    }
}