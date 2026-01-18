package com.dragonminez.client.render.hair;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.CustomHair.HairFace;
import com.dragonminez.common.hair.HairStrand;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;


public class HairRenderer {
    private static final float UNIT_SCALE = 1.0f / 16.0f;
    private static final float SIZE_DECAY = 0.85f;

    private static final ResourceLocation WHITE_TEXTURE =
        new ResourceLocation("minecraft", "textures/misc/white.png");

    public static void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            CustomHair hair,
            String defaultColor,
            int packedLight,
            int packedOverlay
    ) {
        if (hair == null) {
            return;
        }

        for (HairFace face : HairFace.values()) {
            HairStrand[] strands = hair.getStrands(face);
            if (strands == null) continue;

            for (HairStrand strand : strands) {
                if (!strand.isVisible()) continue;

                String color = hair.getEffectiveColor(strand);
                if (color == null || color.isEmpty()) {
                    color = defaultColor;
                }

                renderStrand(poseStack, bufferSource, strand, color, packedLight, packedOverlay);
            }
        }
    }

    private static void renderStrand(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            HairStrand strand,
            String colorHex,
            int packedLight,
            int packedOverlay
    ) {
        if (strand.getLength() <= 0) return;

        float[] rgb = ColorUtils.hexToRgb(colorHex);

        poseStack.pushPose();

        poseStack.translate(
            strand.getOffsetX() * UNIT_SCALE,
            strand.getOffsetY() * UNIT_SCALE,
            strand.getOffsetZ() * UNIT_SCALE
        );

        applyRotation(poseStack, strand.getRotationX(), strand.getRotationY(), strand.getRotationZ());
        poseStack.scale(strand.getScaleX(), strand.getScaleY(), strand.getScaleZ());

        float baseW = strand.getCubeWidth() * UNIT_SCALE;
        float baseH = strand.getCubeHeight() * UNIT_SCALE;
        float baseD = strand.getCubeDepth() * UNIT_SCALE;

        float accumulatedHeight = 0;

        for (int i = 0; i < strand.getLength(); i++) {
            float sizeFactor = (float) Math.pow(SIZE_DECAY, i);
            float cubeW = baseW * sizeFactor;
            float cubeH = baseH * sizeFactor;
            float cubeD = baseD * sizeFactor;

            if (i > 0) {
                poseStack.translate(0, accumulatedHeight, 0);

                applyRotation(poseStack, strand.getCurveX(), strand.getCurveY(), strand.getCurveZ());

                accumulatedHeight = 0;
            }

            renderCube(poseStack, bufferSource, cubeW, cubeH, cubeD,
                      rgb[0], rgb[1], rgb[2], packedLight, packedOverlay);

            accumulatedHeight = cubeH;
        }

        poseStack.popPose();
    }

    private static void applyRotation(PoseStack poseStack, float rotX, float rotY, float rotZ) {
        if (rotX != 0) poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        if (rotY != 0) poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        if (rotZ != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
    }

    private static void renderCube(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float width, float height, float depth,
            float r, float g, float b,
            int packedLight,
            int packedOverlay
    ) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entitySolid(WHITE_TEXTURE));

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        float hw = width / 2.0f;
        float hd = depth / 2.0f;
        float h = height;

        addQuad(buffer, pose, normal,
            -hw, 0, -hd,  hw, 0, -hd,  hw, 0, hd,  -hw, 0, hd,
            0, -1, 0, r, g, b, packedLight, packedOverlay);

        addQuad(buffer, pose, normal,
            -hw, h, hd,  hw, h, hd,  hw, h, -hd,  -hw, h, -hd,
            0, 1, 0, r, g, b, packedLight, packedOverlay);

        addQuad(buffer, pose, normal,
            -hw, 0, -hd,  -hw, h, -hd,  hw, h, -hd,  hw, 0, -hd,
            0, 0, -1, r, g, b, packedLight, packedOverlay);

        addQuad(buffer, pose, normal,
            hw, 0, hd,  hw, h, hd,  -hw, h, hd,  -hw, 0, hd,
            0, 0, 1, r, g, b, packedLight, packedOverlay);

        addQuad(buffer, pose, normal,
            hw, 0, -hd,  hw, h, -hd,  hw, h, hd,  hw, 0, hd,
            1, 0, 0, r, g, b, packedLight, packedOverlay);

        addQuad(buffer, pose, normal,
            -hw, 0, hd,  -hw, h, hd,  -hw, h, -hd,  -hw, 0, -hd,
            -1, 0, 0, r, g, b, packedLight, packedOverlay);
    }

    private static void addQuad(
            VertexConsumer buffer, Matrix4f pose, Matrix3f normal,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float nx, float ny, float nz,
            float r, float g, float b,
            int packedLight, int packedOverlay
    ) {
        buffer.vertex(pose, x1, y1, z1).color(r, g, b, 1.0f).uv(0, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(normal, nx, ny, nz).endVertex();
        buffer.vertex(pose, x2, y2, z2).color(r, g, b, 1.0f).uv(0, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(normal, nx, ny, nz).endVertex();
        buffer.vertex(pose, x3, y3, z3).color(r, g, b, 1.0f).uv(1, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(normal, nx, ny, nz).endVertex();
        buffer.vertex(pose, x4, y4, z4).color(r, g, b, 1.0f).uv(1, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(normal, nx, ny, nz).endVertex();
    }
}
