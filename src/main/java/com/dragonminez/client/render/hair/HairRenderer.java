package com.dragonminez.client.render.hair;

import com.dragonminez.Reference;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.CustomHair.HairFace;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.hair.HairStrand;
import com.dragonminez.common.stats.character.Character;
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
	public static int EDITING_STRAND_ID = -1;

	private static final float UNIT_SCALE = 1.0f / 16.0f;
	private static final float SIZE_DECAY = 0.85f;
	private static final float INVISIBLE_SCALE = 0.0001f;

	private static final float MICRO_SWAY = 4.0f;
	private static final float SEGMENT_PHASE = 0.6f;
	private static final float INERTIA_GAIN = 260.0f;
	private static final float INERTIA_MAX = 75.0f;
	private static final float KI_CHARGE_LIFT = 95.0f;
	private static final ResourceLocation HAIR_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/hair.png");
	private static final HairFace[] FACES = HairFace.values();

	public static void render(PoseStack poseStack, MultiBufferSource bufferSource, CustomHair hairFrom, CustomHair hairTo, float transitionFactor, Character character, StatsData stats, AbstractClientPlayer player, float[] rgbFrom, float[] rgbTo, boolean forceColorFrom, boolean forceColorTo, float partialTick, int packedLight, int packedOverlay, float baseAlpha, float physicsLodMultiplier, float chargeProgress) {
		if (hairFrom == null) hairFrom = new CustomHair();
		if (hairTo == null) hairTo = hairFrom;

		if (character != null && !HairManager.canUseHair(character)) return;

		float movementIntensity = 0.0f;
		boolean isCharging = false;
		float time = 0;

		float inertiaForward = 0.0f;
		float inertiaSide = 0.0f;
		float inertiaLift = 0.0f;
		boolean useFromOnly = transitionFactor <= 0.0001f;
		boolean useToOnly = transitionFactor >= 0.9999f;

		if (stats != null && player != null && PHYSICS_ENABLED && physicsLodMultiplier > 0.01f) {
			time = player.tickCount + partialTick;
			double velX = player.getX() - player.xo;
			double velY = player.getY() - player.yo;
			double velZ = player.getZ() - player.zo;
			double velocity = velX * velX + velY * velY + velZ * velZ;
			boolean isMoving = velocity > 0.0004 || player.isSprinting() || player.isSwimming();
			movementIntensity = (isMoving ? 1.0f : 0.2f) * physicsLodMultiplier;
			isCharging = stats.getStatus().isChargingKi() || stats.getStatus().isActionCharging();

			float headYaw = Mth.lerp(partialTick, player.yHeadRotO, player.yHeadRot);
			double hy = Math.toRadians(headYaw);
			float sinY = (float) Math.sin(hy);
			float cosY = (float) Math.cos(hy);
			float fwd = (float) (-sinY * velX + cosY * velZ);
			float side = (float) (cosY * velX + sinY * velZ);
			float vert = (float) velY;
			float gain = INERTIA_GAIN * physicsLodMultiplier;
			inertiaForward = Mth.clamp(fwd * gain, -INERTIA_MAX, INERTIA_MAX);
			inertiaSide = Mth.clamp(-side * gain, -INERTIA_MAX, INERTIA_MAX);
			inertiaLift = Mth.clamp(vert * gain, -INERTIA_MAX, INERTIA_MAX);
			if (chargeProgress > 0.0f) inertiaLift += KI_CHARGE_LIFT * physicsLodMultiplier * chargeProgress;
		}

		if (!useFromOnly && !useToOnly) {
			transitionFactor = transitionFactor * transitionFactor * (3.0f - 2.0f * transitionFactor);
		}

		float[] tempRgb = new float[3];
		RenderType opaqueType = RenderType.entityCutoutNoCull(HAIR_TEXTURE);
		RenderType translucentType = RenderType.entityTranslucent(HAIR_TEXTURE);

		for (HairFace face : FACES) {
			HairStrand[] strandsFrom = hairFrom.getStrands(face);
			HairStrand[] strandsTo = hairTo.getStrands(face);

			int maxStrands = Math.max(strandsFrom != null ? strandsFrom.length : 0, strandsTo != null ? strandsTo.length : 0);

			for (int i = 0; i < maxStrands; i++) {
				HairStrand s1 = (strandsFrom != null && i < strandsFrom.length) ? strandsFrom[i] : null;
				HairStrand s2 = (strandsTo != null && i < strandsTo.length) ? strandsTo[i] : null;

				boolean v1 = s1 != null && s1.isVisible();
				boolean v2 = s2 != null && s2.isVisible();

				if (!v1 && !v2) continue;
				if (useFromOnly && !v1) continue;
				if (useToOnly && !v2) continue;

				float currentAlpha = baseAlpha;

				float lerpRotX;
				float lerpRotY;
				float lerpRotZ;
				float lerpScaleX;
				float lerpScaleY;
				float lerpScaleZ;
				float lerpStretch;
				float lerpCurveX;
				float lerpCurveY;
				float lerpCurveZ;
				float lerpW;
				float lerpH;
				float lerpD;
				int length;
				int strandId;

				if (useFromOnly) {
					float[] fromRgb = (!forceColorFrom && s1.hasCustomColor()) ? s1.getRgbColor() : rgbFrom;
					tempRgb[0] = fromRgb[0];
					tempRgb[1] = fromRgb[1];
					tempRgb[2] = fromRgb[2];
					lerpRotX = s1.getRotationX();
					lerpRotY = s1.getRotationY();
					lerpRotZ = s1.getRotationZ();
					lerpScaleX = s1.getScaleX();
					lerpScaleY = s1.getScaleY();
					lerpScaleZ = s1.getScaleZ();
					lerpStretch = s1.getLengthScale();
					lerpCurveX = s1.getCurveX();
					lerpCurveY = s1.getCurveY();
					lerpCurveZ = s1.getCurveZ();
					lerpW = s1.getCubeWidth();
					lerpH = s1.getCubeHeight();
					lerpD = s1.getCubeDepth();
					length = s1.getLength();
					strandId = s1.getId();
				} else if (useToOnly) {
					float[] toRgb = (!forceColorTo && s2.hasCustomColor()) ? s2.getRgbColor() : rgbTo;
					tempRgb[0] = toRgb[0];
					tempRgb[1] = toRgb[1];
					tempRgb[2] = toRgb[2];
					lerpRotX = s2.getRotationX();
					lerpRotY = s2.getRotationY();
					lerpRotZ = s2.getRotationZ();
					lerpScaleX = s2.getScaleX();
					lerpScaleY = s2.getScaleY();
					lerpScaleZ = s2.getScaleZ();
					lerpStretch = s2.getLengthScale();
					lerpCurveX = s2.getCurveX();
					lerpCurveY = s2.getCurveY();
					lerpCurveZ = s2.getCurveZ();
					lerpW = s2.getCubeWidth();
					lerpH = s2.getCubeHeight();
					lerpD = s2.getCubeDepth();
					length = s2.getLength();
					strandId = s2.getId();
				} else {
					HairStrand colorFrom = v1 ? s1 : s2;
					HairStrand colorTo = v2 ? s2 : s1;
					fillInterpolatedRgb(colorFrom, colorTo, transitionFactor, rgbFrom, rgbTo, forceColorFrom, forceColorTo, tempRgb);

					float fromRotX = v1 ? s1.getRotationX() : (s2 != null ? s2.getRotationX() : 0.0f);
					float fromRotY = v1 ? s1.getRotationY() : (s2 != null ? s2.getRotationY() : 0.0f);
					float fromRotZ = v1 ? s1.getRotationZ() : (s2 != null ? s2.getRotationZ() : 0.0f);
					float toRotX = v2 ? s2.getRotationX() : (s1 != null ? s1.getRotationX() : 0.0f);
					float toRotY = v2 ? s2.getRotationY() : (s1 != null ? s1.getRotationY() : 0.0f);
					float toRotZ = v2 ? s2.getRotationZ() : (s1 != null ? s1.getRotationZ() : 0.0f);
					lerpRotX = Mth.lerp(transitionFactor, fromRotX, toRotX);
					lerpRotY = Mth.lerp(transitionFactor, fromRotY, toRotY);
					lerpRotZ = Mth.lerp(transitionFactor, fromRotZ, toRotZ);

					float fromScaleX = v1 ? s1.getScaleX() : INVISIBLE_SCALE;
					float fromScaleY = v1 ? s1.getScaleY() : INVISIBLE_SCALE;
					float fromScaleZ = v1 ? s1.getScaleZ() : INVISIBLE_SCALE;
					float toScaleX = v2 ? s2.getScaleX() : INVISIBLE_SCALE;
					float toScaleY = v2 ? s2.getScaleY() : INVISIBLE_SCALE;
					float toScaleZ = v2 ? s2.getScaleZ() : INVISIBLE_SCALE;
					lerpScaleX = Mth.lerp(transitionFactor, fromScaleX, toScaleX);
					lerpScaleY = Mth.lerp(transitionFactor, fromScaleY, toScaleY);
					lerpScaleZ = Mth.lerp(transitionFactor, fromScaleZ, toScaleZ);

					float fromStretch = v1 ? s1.getLengthScale() : (s2 != null ? s2.getLengthScale() : 1.0f);
					float toStretch = v2 ? s2.getLengthScale() : (s1 != null ? s1.getLengthScale() : 1.0f);
					lerpStretch = Mth.lerp(transitionFactor, fromStretch, toStretch);

					float fromCurveX = v1 ? s1.getCurveX() : (s2 != null ? s2.getCurveX() : 0.0f);
					float fromCurveY = v1 ? s1.getCurveY() : (s2 != null ? s2.getCurveY() : 0.0f);
					float fromCurveZ = v1 ? s1.getCurveZ() : (s2 != null ? s2.getCurveZ() : 0.0f);
					float toCurveX = v2 ? s2.getCurveX() : (s1 != null ? s1.getCurveX() : 0.0f);
					float toCurveY = v2 ? s2.getCurveY() : (s1 != null ? s1.getCurveY() : 0.0f);
					float toCurveZ = v2 ? s2.getCurveZ() : (s1 != null ? s1.getCurveZ() : 0.0f);
					lerpCurveX = Mth.lerp(transitionFactor, fromCurveX, toCurveX);
					lerpCurveY = Mth.lerp(transitionFactor, fromCurveY, toCurveY);
					lerpCurveZ = Mth.lerp(transitionFactor, fromCurveZ, toCurveZ);

					float fromW = v1 ? s1.getCubeWidth() : (s2 != null ? s2.getCubeWidth() : 2.0f);
					float fromH = v1 ? s1.getCubeHeight() : (s2 != null ? s2.getCubeHeight() : 2.0f);
					float fromD = v1 ? s1.getCubeDepth() : (s2 != null ? s2.getCubeDepth() : 2.0f);
					float toW = v2 ? s2.getCubeWidth() : (s1 != null ? s1.getCubeWidth() : 2.0f);
					float toH = v2 ? s2.getCubeHeight() : (s1 != null ? s1.getCubeHeight() : 2.0f);
					float toD = v2 ? s2.getCubeDepth() : (s1 != null ? s1.getCubeDepth() : 2.0f);
					lerpW = Mth.lerp(transitionFactor, fromW, toW);
					lerpH = Mth.lerp(transitionFactor, fromH, toH);
					lerpD = Mth.lerp(transitionFactor, fromD, toD);

					length = Math.max(v1 && s1 != null ? s1.getLength() : 0, v2 && s2 != null ? s2.getLength() : 0);
					strandId = s1 != null ? s1.getId() : (s2 != null ? s2.getId() : -1);
				}

				if (length <= 0) continue;

				if (EDITING_STRAND_ID != -1 && strandId != EDITING_STRAND_ID) {
					currentAlpha *= 0.35f;
				}
				Vector3f staticPos = CustomHair.getStrandBasePosition(face, i);

				VertexConsumer strandBuffer = bufferSource.getBuffer(currentAlpha < 1.0f ? translucentType : opaqueType);
				renderStrandInterpolated(poseStack, strandBuffer,
						staticPos, tempRgb[0], tempRgb[1], tempRgb[2], packedLight, packedOverlay,
						time, movementIntensity, chargeProgress,
						inertiaForward, inertiaSide, inertiaLift,
						lerpRotX, lerpRotY, lerpRotZ,
						lerpScaleX, lerpScaleY, lerpScaleZ, lerpStretch,
						lerpCurveX, lerpCurveY, lerpCurveZ,
						lerpW, lerpH, lerpD, length, strandId, face, currentAlpha, physicsLodMultiplier);
			}
		}
	}

	private static void fillInterpolatedRgb(HairStrand s1, HairStrand s2, float factor, float[] globalRgbFrom, float[] globalRgbTo, boolean forceColorFrom, boolean forceColorTo, float[] out) {
		float[] effectiveFrom = globalRgbFrom;
		if (!forceColorFrom && s1 != null && s1.hasCustomColor()) effectiveFrom = s1.getRgbColor();

		float[] effectiveTo = globalRgbTo;
		if (!forceColorTo && s2 != null && s2.hasCustomColor()) effectiveTo = s2.getRgbColor();

		if (factor <= 0.0f) {
			out[0] = effectiveFrom[0];
			out[1] = effectiveFrom[1];
			out[2] = effectiveFrom[2];
			return;
		}
		if (factor >= 1.0f) {
			out[0] = effectiveTo[0];
			out[1] = effectiveTo[1];
			out[2] = effectiveTo[2];
			return;
		}

		out[0] = Mth.lerp(factor, effectiveFrom[0], effectiveTo[0]);
		out[1] = Mth.lerp(factor, effectiveFrom[1], effectiveTo[1]);
		out[2] = Mth.lerp(factor, effectiveFrom[2], effectiveTo[2]);
	}

	private static void renderStrandInterpolated(PoseStack poseStack, VertexConsumer strandBuffer, Vector3f pos, float r, float g, float b, int packedLight, int packedOverlay,
	                                             float time, float moveIntensity, float chargeProgress,
	                                             float inertiaForward, float inertiaSide, float inertiaLift,
	                                             float rotX, float rotY, float rotZ,
	                                             float scaleX, float scaleY, float scaleZ, float stretchFactor,
	                                             float curveX, float curveY, float curveZ,
	                                             float width, float height, float depth, int length, int id, HairFace face, float alpha, float physicsLodMultiplier) {

		poseStack.pushPose();
		poseStack.translate(pos.x * UNIT_SCALE, pos.y * UNIT_SCALE, pos.z * UNIT_SCALE);

		float offset = (id * 13.0f);

		float baseSwaySpeed = moveIntensity > 0.5f ? 0.4f : 0.05f;
		float baseSwayAmount = (moveIntensity > 0.5f ? 5.0f : 0.6f) * physicsLodMultiplier;

		float targetSwaySpeed = 0.8f;
		float targetSwayAmount = 3.0f * physicsLodMultiplier;

		float currentSwaySpeed = Mth.lerp(chargeProgress, baseSwaySpeed, targetSwaySpeed);
		float currentSwayAmount = Mth.lerp(chargeProgress, baseSwayAmount, targetSwayAmount);
		float phase = (time + offset) * currentSwaySpeed;

		float animRotX = (time == 0) ? 0 : Mth.sin(phase) * currentSwayAmount;
		float animRotZ = (time == 0) ? 0 : Mth.cos(phase * 0.7f) * (currentSwayAmount * 0.5f);

		if (chargeProgress > 0.0f && time != 0) {
			float chargeLift = Mth.abs(Mth.sin(time * 0.5f)) * 5.0f * physicsLodMultiplier * chargeProgress;
			curveX += chargeLift;
		}

		float finalRotX = rotX + animRotX;
		float finalRotZ = rotZ + animRotZ;

		switch (face) {
			case FRONT -> finalRotX = animRotX > 0 ? Math.min(finalRotX, rotX) : finalRotX;
			case BACK -> finalRotX = animRotX < 0 ? Math.max(finalRotX, rotX) : finalRotX;
			case LEFT -> finalRotZ = animRotZ < 0 ? Math.max(finalRotZ, rotZ) : finalRotZ;
			case RIGHT -> finalRotZ = animRotZ > 0 ? Math.min(finalRotZ, rotZ) : finalRotZ;
		}

		applyRotation(poseStack, finalRotX, rotY, finalRotZ);
		poseStack.scale(scaleX, scaleY, scaleZ);

		float baseW = width * UNIT_SCALE;
		float baseH = height * UNIT_SCALE;
		float baseD = depth * UNIT_SCALE;
		float accumulatedHeight = 0;

		float sizeFactor = 1.0f;

		for (int i = 0; i < length; i++) {
			float cubeW = baseW * sizeFactor;
			float cubeH = baseH * sizeFactor * stretchFactor;
			float cubeD = baseD * sizeFactor;

			float overlap = 0.0f;
			if (i > 0) {
				poseStack.translate(0, accumulatedHeight, 0);

				float segCurveX = curveX;
				float segCurveZ = curveZ;

				if (time != 0) {
					float tip = (float) i / length;
					float seg = tip * physicsLodMultiplier;

					float altCurrentSwaySpeed = Mth.lerp(chargeProgress, baseSwaySpeed, targetSwaySpeed);
					float microPhase = (time * altCurrentSwaySpeed) + offset + i * SEGMENT_PHASE;

					float waveX = Mth.sin(microPhase);
					float waveZ = Mth.cos(microPhase * 0.7f);

					float altCurrentSwayAmount = Mth.lerp(chargeProgress, baseSwayAmount, targetSwayAmount);

					segCurveX += waveX * MICRO_SWAY * altCurrentSwayAmount * seg + (inertiaForward - inertiaLift) * seg;
					segCurveZ += waveZ * MICRO_SWAY * 0.5f * altCurrentSwayAmount * seg + inertiaSide * seg;
				}

				applyRotation(poseStack, segCurveX, curveY, segCurveZ);

				float radX = Math.abs(segCurveX) * ((float) Math.PI / 180.0f);
				float radZ = Math.abs(segCurveZ) * ((float) Math.PI / 180.0f);
				overlap = (cubeW / 2.0f) * Mth.sin(radZ) + (cubeD / 2.0f) * Mth.sin(radX) + 0.015f;
			}

			renderCube(poseStack, strandBuffer, cubeW, cubeH, cubeD, overlap, r, g, b, packedLight, packedOverlay, alpha);
			accumulatedHeight = cubeH;
			sizeFactor *= SIZE_DECAY;
		}

		poseStack.popPose();
	}

	private static void applyRotation(PoseStack poseStack, float rotX, float rotY, float rotZ) {
		if (rotX != 0) poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
		if (rotY != 0) poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
		if (rotZ != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
	}

	private static void renderCube(PoseStack poseStack, VertexConsumer buffer, float width, float height, float depth, float overlap, float r, float g, float b, int packedLight, int packedOverlay, float alpha) {
		Matrix4f pose = poseStack.last().pose();
		Matrix3f normal = poseStack.last().normal();

		float hw = width / 2.0f;
		float hd = depth / 2.0f;
		float h = height;
		float bottom = -overlap;

		addQuad(buffer, pose, normal, -hw, bottom, -hd, hw, bottom, -hd, hw, bottom, hd, -hw, bottom, hd, 0, -1, 0, r, g, b, 0.0f, 0.0f, 1.0f, 1.0f, packedLight, packedOverlay, alpha);
		addQuad(buffer, pose, normal, -hw, h, hd, hw, h, hd, hw, h, -hd, -hw, h, -hd, 0, 1, 0, r, g, b, 0.0f, 0.0f, 1.0f, 1.0f, packedLight, packedOverlay, alpha);
		addQuad(buffer, pose, normal, -hw, bottom, -hd, -hw, h, -hd, hw, h, -hd, hw, bottom, -hd, 0, 0, -1, r, g, b, 0.0f, 0.0f, 1.0f, 1.0f, packedLight, packedOverlay, alpha);
		addQuad(buffer, pose, normal, hw, bottom, hd, hw, h, hd, -hw, h, hd, -hw, bottom, hd, 0, 0, 1, r, g, b, 0.0f, 0.0f, 1.0f, 1.0f, packedLight, packedOverlay, alpha);
		addQuad(buffer, pose, normal, hw, bottom, -hd, hw, h, -hd, hw, h, hd, hw, bottom, hd, 1, 0, 0, r, g, b, 0.0f, 0.0f, 1.0f, 1.0f, packedLight, packedOverlay, alpha);
		addQuad(buffer, pose, normal, -hw, bottom, hd, -hw, h, hd, -hw, h, -hd, -hw, bottom, -hd, -1, 0, 0, r, g, b, 0.0f, 0.0f, 1.0f, 1.0f, packedLight, packedOverlay, alpha);
	}

	private static void addQuad(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float nx, float ny, float nz, float r, float g, float b, float u0, float v0, float u1, float v1, int packedLight, int packedOverlay, float alpha) {
		buffer.vertex(pose, x1, y1, z1).color(r, g, b, alpha).uv(u0, v0).overlayCoords(packedOverlay).uv2(packedLight).normal(normal, nx, ny, nz).endVertex();
		buffer.vertex(pose, x2, y2, z2).color(r, g, b, alpha).uv(u0, v1).overlayCoords(packedOverlay).uv2(packedLight).normal(normal, nx, ny, nz).endVertex();
		buffer.vertex(pose, x3, y3, z3).color(r, g, b, alpha).uv(u1, v1).overlayCoords(packedOverlay).uv2(packedLight).normal(normal, nx, ny, nz).endVertex();
		buffer.vertex(pose, x4, y4, z4).color(r, g, b, alpha).uv(u1, v0).overlayCoords(packedOverlay).uv2(packedLight).normal(normal, nx, ny, nz).endVertex();
	}
}