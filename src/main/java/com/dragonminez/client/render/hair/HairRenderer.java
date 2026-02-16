package com.dragonminez.client.render.hair;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.CustomHair.HairFace;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.hair.HairStrand;
import com.dragonminez.common.stats.Character;
import com.dragonminez.common.stats.StatsData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;


public class HairRenderer {
	public static boolean PHYSICS_ENABLED = true;
    private static final float UNIT_SCALE = 1.0f / 16.0f;
    private static final float SIZE_DECAY = 0.85f;
    private static final ResourceLocation HAIR_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/hair.png");

	public static void render(PoseStack poseStack, MultiBufferSource bufferSource,
							  CustomHair hairFrom, CustomHair hairTo, float transitionFactor,
							  Character character, StatsData stats, AbstractClientPlayer player,
							  String colorFrom, String colorTo, float partialTick, int packedLight, int packedOverlay) {

		if (hairFrom == null) hairFrom = new CustomHair();
		if (hairTo == null) hairTo = hairFrom;

		if (character != null && !HairManager.canUseHair(character)) return;

		float movementIntensity = 0.0f;
		boolean isCharging = false;
		float time = 0;

		if (stats != null && player != null && PHYSICS_ENABLED) {
			time = player.tickCount + partialTick;
			double velocity = player.getDeltaMovement().lengthSqr();
			boolean isMoving = velocity > 0.002 || player.isSprinting() || player.isSwimming();
			movementIntensity = isMoving ? 1.0f : 0.2f;
			isCharging = stats.getStatus().isChargingKi() || stats.getStatus().isActionCharging();
		}

		for (HairFace face : HairFace.values()) {
			HairStrand[] strandsFrom = hairFrom.getStrands(face);
			HairStrand[] strandsTo = hairTo.getStrands(face);

			int maxStrands = Math.max(strandsFrom != null ? strandsFrom.length : 0, strandsTo != null ? strandsTo.length : 0);

			for (int i = 0; i < maxStrands; i++) {
				HairStrand s1 = (strandsFrom != null && i < strandsFrom.length) ? strandsFrom[i] : null;
				HairStrand s2 = (strandsTo != null && i < strandsTo.length) ? strandsTo[i] : null;

				boolean v1 = s1 != null && s1.isVisible();
				boolean v2 = s2 != null && s2.isVisible();

				if (!v1 && !v2) continue;
				if (!v1) s1 = createZeroScaleStrand(s2);
				if (!v2) s2 = createZeroScaleStrand(s1);

				String color = getColor(character, s1, s2, transitionFactor, colorFrom, colorTo);

				float lerpRotX = Mth.lerp(transitionFactor, s1.getRotationX(), s2.getRotationX());
				float lerpRotY = Mth.lerp(transitionFactor, s1.getRotationY(), s2.getRotationY());
				float lerpRotZ = Mth.lerp(transitionFactor, s1.getRotationZ(), s2.getRotationZ());

				float lerpScaleX = Mth.lerp(transitionFactor, s1.getScaleX(), s2.getScaleX());
				float lerpScaleY = Mth.lerp(transitionFactor, s1.getScaleY(), s2.getScaleY());
				float lerpScaleZ = Mth.lerp(transitionFactor, s1.getScaleZ(), s2.getScaleZ());

				float lerpStretch = Mth.lerp(transitionFactor, s1.getLengthScale(), s2.getLengthScale());

				float lerpCurveX = Mth.lerp(transitionFactor, s1.getCurveX(), s2.getCurveX());
				float lerpCurveY = Mth.lerp(transitionFactor, s1.getCurveY(), s2.getCurveY());
				float lerpCurveZ = Mth.lerp(transitionFactor, s1.getCurveZ(), s2.getCurveZ());

				float lerpW = Mth.lerp(transitionFactor, s1.getCubeWidth(), s2.getCubeWidth());
				float lerpH = Mth.lerp(transitionFactor, s1.getCubeHeight(), s2.getCubeHeight());
				float lerpD = Mth.lerp(transitionFactor, s1.getCubeDepth(), s2.getCubeDepth());

				int length = Math.max(s1.getLength(), s2.getLength());

				Vector3f staticPos = CustomHair.getStrandBasePosition(face, i);

				renderStrandInterpolated(poseStack, bufferSource,
						staticPos, color, packedLight, packedOverlay,
						time, movementIntensity, isCharging,
						lerpRotX, lerpRotY, lerpRotZ,
						lerpScaleX, lerpScaleY, lerpScaleZ, lerpStretch,
						lerpCurveX, lerpCurveY, lerpCurveZ,
						lerpW, lerpH, lerpD, length, s1.getId(), face);
			}
		}
	}

	private static HairStrand createZeroScaleStrand(HairStrand source) {
		HairStrand empty = source.copy();
		empty.setScale(0, 0, 0);
		return empty;
	}

	private static String getColor(Character character, HairStrand s1, HairStrand s2, float factor, String colorFrom, String colorTo) {
		final boolean shouldInterpolate = colorFrom != null && colorTo != null && !colorFrom.equals(colorTo) && factor > 0.0f && factor < 1.0f;
		if (character != null && character.hasActiveForm()) {
			if (shouldInterpolate) {
				return interpolateColor(colorFrom, colorTo, factor);
			}
			return factor >= 1.0f ? colorTo : colorFrom;
		}

		HairStrand targetStrand = (factor > 0.5f) ? s2 : s1;

		if (s1 != null && s1.hasCustomColor() && s2 != null && s2.hasCustomColor()) {
			String c1 = s1.getColor();
			String c2 = s2.getColor();
			if (!c1.equals(c2) && factor > 0.0f && factor < 1.0f) {
				return interpolateColor(c1, c2, factor);
			}
			return (factor > 0.5f) ? c2 : c1;
		} else if (targetStrand != null && targetStrand.hasCustomColor()) {
			return targetStrand.getColor();
		}

		if (shouldInterpolate) {
			return interpolateColor(colorFrom, colorTo, factor);
		}
		return factor >= 1.0f ? colorTo : colorFrom;
	}

	private static String interpolateColor(String hexFrom, String hexTo, float factor) {
		float[] rgbFrom = ColorUtils.hexToRgb(hexFrom);
		float[] rgbTo = ColorUtils.hexToRgb(hexTo);

		float r = Mth.lerp(factor, rgbFrom[0], rgbTo[0]);
		float g = Mth.lerp(factor, rgbFrom[1], rgbTo[1]);
		float b = Mth.lerp(factor, rgbFrom[2], rgbTo[2]);

		int ri = (int) (r * 255);
		int gi = (int) (g * 255);
		int bi = (int) (b * 255);

		return String.format("#%02X%02X%02X", ri, gi, bi);
	}

	private static void renderStrandInterpolated(PoseStack poseStack, MultiBufferSource bufferSource, Vector3f pos, String colorHex, int packedLight, int packedOverlay,
												 float time, float moveIntensity, boolean isCharging,
												 float rotX, float rotY, float rotZ,
												 float scaleX, float scaleY, float scaleZ, float stretchFactor,
												 float curveX, float curveY, float curveZ,
												 float width, float height, float depth, int length, int id, HairFace face) {

		float[] rgb = ColorUtils.hexToRgb(colorHex);
		poseStack.pushPose();
		poseStack.translate(pos.x * UNIT_SCALE, pos.y * UNIT_SCALE, pos.z * UNIT_SCALE);

		float offset = (id * 13.0f);
		float swaySpeed = isCharging ? 0.8f : (moveIntensity > 0.5f ? 0.4f : 0.05f);
		float swayAmount = isCharging ? 3.0f : (moveIntensity > 0.5f ? 5.0f : 0.6f);

		float animRotX = (time == 0) ? 0 : Mth.sin((time + offset) * swaySpeed) * swayAmount;
		float animRotZ = (time == 0) ? 0 : Mth.cos((time + offset) * swaySpeed * 0.7f) * (swayAmount * 0.5f);

		if (isCharging && time != 0) {
			float chargeLift = Mth.abs(Mth.sin(time * 0.5f)) * 5.0f;
			curveX -= chargeLift;
		}

		float finalRotX = rotX + animRotX;
		float finalRotZ = rotZ + animRotZ;

		switch (face) {
			case FRONT:
				if (animRotX > 0) finalRotX = Math.min(finalRotX, rotX);
				break;
			case BACK:
				if (animRotX < 0) finalRotX = Math.max(finalRotX, rotX);
				break;
			case LEFT:
				if (animRotZ < 0) finalRotZ = Math.max(finalRotZ, rotZ);
				break;
			case RIGHT:
				if (animRotZ > 0) finalRotZ = Math.min(finalRotZ, rotZ);
				break;
			default: break;
		}

		applyRotation(poseStack, finalRotX, rotY, finalRotZ);
		poseStack.scale(scaleX, scaleY, scaleZ);

		float baseW = width * UNIT_SCALE;
		float baseH = height * UNIT_SCALE;
		float baseD = depth * UNIT_SCALE;
		float accumulatedHeight = 0;

		for (int i = 0; i < length; i++) {
			float sizeFactor = (float) Math.pow(SIZE_DECAY, i);
			float cubeW = baseW * sizeFactor;
			float cubeH = baseH * sizeFactor * stretchFactor;
			float cubeD = baseD * sizeFactor;

			if (i > 0) {
				poseStack.translate(0, accumulatedHeight, 0);
				applyRotation(poseStack, curveX, curveY, curveZ);
			}

			renderCube(poseStack, bufferSource, cubeW, cubeH, cubeD, rgb[0], rgb[1], rgb[2], packedLight, packedOverlay);
			accumulatedHeight = cubeH;
		}

		poseStack.popPose();
	}

	private static void applyRotation(PoseStack poseStack, float rotX, float rotY, float rotZ) {
		if (rotX != 0) poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
		if (rotY != 0) poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
		if (rotZ != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
	}

    private static void renderCube(PoseStack poseStack, MultiBufferSource bufferSource, float width, float height, float depth, float r, float g, float b, int packedLight, int packedOverlay) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(HAIR_TEXTURE));
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        float hw = width / 2.0f;
        float hd = depth / 2.0f;
        float h = height;

        float u0 = 0.0f;
        float u1 = 1.0f;
        float v0 = 0.0f;
        float v1 = 1.0f;

		// Bottom, Top, North, South, East, West
		addQuad(buffer, pose, normal, -hw, 0, -hd, hw, 0, -hd, hw, 0, hd, -hw, 0, hd, 0, -1, 0, r, g, b, u0, v0, u1, v1, packedLight, packedOverlay);
		addQuad(buffer, pose, normal, -hw, h, hd, hw, h, hd, hw, h, -hd, -hw, h, -hd, 0, 1, 0, r, g, b, u0, v0, u1, v1, packedLight, packedOverlay);
		addQuad(buffer, pose, normal, -hw, 0, -hd, -hw, h, -hd, hw, h, -hd, hw, 0, -hd, 0, 0, -1, r, g, b, u0, v0, u1, v1, packedLight, packedOverlay);
		addQuad(buffer, pose, normal, hw, 0, hd, hw, h, hd, -hw, h, hd, -hw, 0, hd, 0, 0, 1, r, g, b, u0, v0, u1, v1, packedLight, packedOverlay);
		addQuad(buffer, pose, normal, hw, 0, -hd, hw, h, -hd, hw, h, hd, hw, 0, hd, 1, 0, 0, r, g, b, u0, v0, u1, v1, packedLight, packedOverlay);
		addQuad(buffer, pose, normal, -hw, 0, hd, -hw, h, hd, -hw, h, -hd, -hw, 0, -hd, -1, 0, 0, r, g, b, u0, v0, u1, v1, packedLight, packedOverlay);
	}

    private static void addQuad(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float nx, float ny, float nz, float r, float g, float b, float u0, float v0, float u1, float v1, int packedLight, int packedOverlay) {
        buffer.vertex(pose, x1, y1, z1).color(r, g, b, 1.0f).uv(u0, v0).overlayCoords(packedOverlay).uv2(packedLight).normal(normal, nx, ny, nz).endVertex();
        buffer.vertex(pose, x2, y2, z2).color(r, g, b, 1.0f).uv(u0, v1).overlayCoords(packedOverlay).uv2(packedLight).normal(normal, nx, ny, nz).endVertex();
        buffer.vertex(pose, x3, y3, z3).color(r, g, b, 1.0f).uv(u1, v1).overlayCoords(packedOverlay).uv2(packedLight).normal(normal, nx, ny, nz).endVertex();
        buffer.vertex(pose, x4, y4, z4).color(r, g, b, 1.0f).uv(u1, v0).overlayCoords(packedOverlay).uv2(packedLight).normal(normal, nx, ny, nz).endVertex();
    }
}