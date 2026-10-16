package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class KiWeaponRenderer {
	public static final float BLADE_WRIST_OFFSET = 3.0f / 16.0f;
	public static final float BLADE_REACH = 24.0f / 16.0f;
	public static final float BLADE_ARM_SHIFT = 2.0f / 16.0f;
	private static final float SCYTHE_SHAFT_LENGTH = 55.0f / 16.0f;
	private static final float SCYTHE_HEAD_FORWARD = 28.0f / 16.0f;
	private static final float SCYTHE_GRIP_HEIGHT = 2.25f / 16.0f;
	private static final float SCYTHE_PAD = 4.0f / 16.0f;
	private static final float SCYTHE_BACK_REACH = 5.0f / 16.0f;
	private static final float SCYTHE_BLADE_REACH = 25.0f / 16.0f;
	private static final float SCYTHE_UV_OFFSET = 20.0f;
	private static final float CLAW_AXIS_X = 6.15f / 16.0f;
	private static final float CLAW_AXIS_Y = 14.0f / 16.0f;
	private static final float CLAW_HEAD_Z = -17.0f / 16.0f;
	private static final float CLAW_BUTT_Z = 13.0f / 16.0f;
	private static final float CLAW_ELBOW_Z = -9.5f / 16.0f;
	private static final float CLAW_TINE_TIP_Z = -28.75f / 16.0f;
	private static final float CLAW_PRONG_RADIUS = 5.0f / 16.0f;
	private static final float CLAW_ROD_PAD = 0.3f;
	private static final float CLAW_ROD_HALF_REACH = 0.36f;
	private static final float CLAW_ROD_FRONT = 1.5f / 16.0f;
	private static final float CLAW_ROD_UV = 60.0f;
	private static final float CLAW_ROD_STRIDE = 4.0f;
	private static final float[][] CLAW_PRONG_DIRS = {{1.0f, 0.0f}, {0.0f, 1.0f}, {-1.0f, 0.0f}, {0.0f, -1.0f}};
	private static final float CLAW_YAW = (float) Math.toRadians(35.0);
	private static final float CLAW_PITCH = (float) Math.toRadians(35.0);
	public static final float BLADE_HALF_WIDTH = 7.5f / 16.0f;
	private static final float BLADE_FRONT_OFFSET = 2.6f / 16.0f;
	private static final float BURST_CENTER_OFFSET = 3.0f / 16.0f;
	private static final float BURST_HALF_SIZE = 10.0f / 16.0f;

	private static ResourceLocation weaponModel(String type) {
		ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/kiweapon_" + type.toLowerCase() + ".geo.json");
		if (Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()) return loc;
		return null;
	}

	private static ResourceLocation weaponTexture(String type) {
		ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/weapons/kiweapon_" + type.toLowerCase() + ".png");
		if (Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()) return loc;
		return null;
	}

	private static String weaponBone(String type) {
		return "kiweapon_" + type.toLowerCase();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void processWeapons(MultiBufferSource.BufferSource buffers, PoseStack poseStack) {
		var weapons = PlayerEffectQueue.getAndClearWeapons();
		if (weapons == null || weapons.isEmpty()) return;

		for (var entry : weapons) {
			if (entry == null || entry.player() == null) continue;

			String type = entry.weaponType();
			if (type == null || type.isEmpty()) continue;

			Player player = entry.player();

			if (usesShaderBlade(type)) {
				renderBladeThirdPerson(entry, buffers);
				continue;
			}

			if (usesShaderScythe(type)) {
				renderScytheThirdPerson(entry, buffers);
				continue;
			}

			if (usesShaderClawLance(type)) {
				renderClawLanceThirdPerson(entry, buffers);
				continue;
			}

			DMZPlayerRenderer<?> renderer = DMZRendererCache.getTPRenderer(player);

			if (renderer != null) {
				ResourceLocation modelLoc = weaponModel(type);
				if (modelLoc == null) continue;
				BakedGeoModel weaponModel = renderer.getGeoModel().getBakedModel(modelLoc);
				if (weaponModel == null) continue;

				String boneName = weaponBone(type);
				ResourceLocation texture = weaponTexture(type);

				weaponModel.getBone(boneName).ifPresent(targetBone -> {

					syncTargetBoneAndParents(targetBone, entry.playerModel());

					poseStack.pushPose();
					poseStack.last().pose().set(entry.poseMatrix());

					if (type.equalsIgnoreCase("clawlance")) {
						poseStack.mulPose(Axis.YP.rotationDegrees(35.0F));
                        poseStack.mulPose(Axis.XP.rotationDegrees(35.f));
                        poseStack.translate(0.0F / 16f, -0.1F, -1.0F);
                    }

					RenderType renderType = ModRenderTypes.energy2(texture);
					VertexConsumer vertexConsumer = buffers.getBuffer(renderType);

					DMZPlayerRenderer rawRenderer = renderer;

					rawRenderer.renderRecursively(poseStack, player, targetBone, renderType, buffers, vertexConsumer, true,
							entry.partialTick(), 15728880, OverlayTexture.NO_OVERLAY,
							entry.color()[0], entry.color()[1], entry.color()[2], 0.65f);

					poseStack.popPose();
				});
			}
		}
	}

	public static boolean usesShaderBlade(String type) {
		return "blade".equalsIgnoreCase(type) && DMZShaders.kiBladeShader != null;
	}

	public static boolean usesShaderScythe(String type) {
		return "scythe".equalsIgnoreCase(type) && DMZShaders.kiBladeShader != null;
	}

	private static void renderBladeThirdPerson(PlayerEffectQueue.WeaponRenderEntry entry, MultiBufferSource.BufferSource buffers) {
		float[] anchor = armAnchor(entry);
		if (anchor == null) return;
		Vector3f start = new Vector3f(anchor[0], anchor[1] + BLADE_WRIST_OFFSET + BLADE_ARM_SHIFT, anchor[2]);
		Vector3f end = new Vector3f(anchor[0], anchor[1] - BLADE_REACH + BLADE_ARM_SHIFT, anchor[2]);
		float seed = bladeSeed(entry.player());
		float[] color = entry.color().clone();
		renderBlade(entry.poseMatrix(), buffers.getBuffer(ModRenderTypes.kiBlade()), start, end, BLADE_HALF_WIDTH, color, seed);
		Matrix4f pose = new Matrix4f(entry.poseMatrix());
		BLOOM_WEAPONS.add(vc -> renderBlade(pose, vc, start, end, BLADE_HALF_WIDTH, color, seed));
	}

	private static void renderScytheThirdPerson(PlayerEffectQueue.WeaponRenderEntry entry, MultiBufferSource.BufferSource buffers) {
		float[] anchor = armAnchor(entry);
		if (anchor == null) return;
		float gripY = anchor[1] + SCYTHE_GRIP_HEIGHT;
		Vector3f head = new Vector3f(anchor[0], gripY, anchor[2] - SCYTHE_HEAD_FORWARD);
		Vector3f butt = new Vector3f(anchor[0], gripY, anchor[2] - SCYTHE_HEAD_FORWARD + SCYTHE_SHAFT_LENGTH);
		Vector3f bladeSide = new Vector3f(0.0f, -1.0f, 0.0f);
		float seed = bladeSeed(entry.player());
		float[] color = entry.color().clone();
		renderScythe(entry.poseMatrix(), buffers.getBuffer(ModRenderTypes.kiBlade()), head, butt, bladeSide, color, seed);
		Matrix4f pose = new Matrix4f(entry.poseMatrix());
		BLOOM_WEAPONS.add(vc -> renderScythe(pose, vc, head, butt, bladeSide, color, seed));
	}

	public static boolean usesShaderClawLance(String type) {
		return "clawlance".equalsIgnoreCase(type) && DMZShaders.kiBladeShader != null;
	}

	private static void renderClawLanceThirdPerson(PlayerEffectQueue.WeaponRenderEntry entry, MultiBufferSource.BufferSource buffers) {
		Matrix4f pose = new Matrix4f(entry.poseMatrix()).rotateY(CLAW_YAW).rotateX(CLAW_PITCH).translate(0.0f, -0.1f, -1.0f);
		float seed = bladeSeed(entry.player());
		float[] color = entry.color().clone();
		renderClawLance(pose, buffers.getBuffer(ModRenderTypes.kiBlade()), color, seed);
		BLOOM_WEAPONS.add(vc -> renderClawLance(pose, vc, color, seed));
	}

	private static float[] armAnchor(PlayerEffectQueue.WeaponRenderEntry entry) {
		String boneName = entry.player().getMainArm() == HumanoidArm.RIGHT ? "right_arm" : "left_arm";
		GeoBone arm = entry.playerModel().getBone(boneName).orElse(null);
		if (arm == null || arm.getCubes().isEmpty()) return null;

		float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (GeoCube cube : arm.getCubes()) {
			for (GeoQuad quad : cube.quads()) {
				if (quad == null) continue;
				for (GeoVertex vertex : quad.vertices()) {
					Vector3f p = vertex.position();
					minX = Math.min(minX, p.x());
					maxX = Math.max(maxX, p.x());
					minY = Math.min(minY, p.y());
					minZ = Math.min(minZ, p.z());
					maxZ = Math.max(maxZ, p.z());
				}
			}
		}
		if (minY == Float.MAX_VALUE) return null;
		return new float[]{(minX + maxX) * 0.5f, minY, (minZ + maxZ) * 0.5f};
	}

	private static final List<Consumer<VertexConsumer>> BLOOM_WEAPONS = new ArrayList<>();

	public static boolean hasBloomBlades() {
		return !BLOOM_WEAPONS.isEmpty();
	}

	public static void clearBloomBlades() {
		BLOOM_WEAPONS.clear();
	}

	public static void renderBloomBlades() {
		ShaderInstance shader = DMZShaders.kiBladeShader;
		if (shader == null || BLOOM_WEAPONS.isEmpty()) {
			BLOOM_WEAPONS.clear();
			return;
		}

		MultiBufferSource.BufferSource source = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		PoseStack modelView = RenderSystem.getModelViewStack();
		modelView.pushPose();
		modelView.setIdentity();
		RenderSystem.applyModelViewMatrix();
		shader.safeGetUniform("bloomMode").set(1.0f);
		try {
			VertexConsumer vc = source.getBuffer(ModRenderTypes.kiBlade());
			for (Consumer<VertexConsumer> draw : BLOOM_WEAPONS) draw.accept(vc);
			source.endBatch();
		} finally {
			shader.safeGetUniform("bloomMode").set(0.0f);
			modelView.popPose();
			RenderSystem.applyModelViewMatrix();
			BLOOM_WEAPONS.clear();
		}
	}

	public static void renderScythe(Matrix4f pose, VertexConsumer vc, Vector3f head, Vector3f butt, Vector3f bladeSide, float[] color, float seed) {
		renderAxisWeapon(pose, vc, head, butt, bladeSide, -SCYTHE_PAD, SCYTHE_SHAFT_LENGTH + SCYTHE_PAD, -SCYTHE_BACK_REACH, SCYTHE_BLADE_REACH, SCYTHE_UV_OFFSET, BLADE_FRONT_OFFSET, null, color, seed);
	}

	public static void renderClawLance(Matrix4f pose, VertexConsumer vc, float[] color, float seed) {
		Vector3f head = new Vector3f(CLAW_AXIS_X, CLAW_AXIS_Y, CLAW_HEAD_Z);
		Vector3f center = new Vector3f(CLAW_AXIS_X, CLAW_AXIS_Y, (CLAW_TINE_TIP_Z + CLAW_BUTT_Z) * 0.5f);
		Vector3f front = new Matrix4f(pose).invert().transformPosition(new Vector3f()).sub(center);
		if (front.lengthSquared() < 1.0E-10f) return;
		front.normalize(CLAW_ROD_FRONT);
		clawRod(pose, vc, head, new Vector3f(CLAW_AXIS_X, CLAW_AXIS_Y, CLAW_BUTT_Z), 0, front, color, seed);
		Vector3f elbowStart = new Vector3f(CLAW_AXIS_X, CLAW_AXIS_Y, CLAW_ELBOW_Z);
		for (int i = 0; i < CLAW_PRONG_DIRS.length; i++) {
			clawRod(pose, vc, elbowStart, prongBase(i), 1 + i, front, color, seed);
		}
		for (int i = 0; i < CLAW_PRONG_DIRS.length; i++) {
			Vector3f base = prongBase(i);
			clawRod(pose, vc, base, new Vector3f(base.x(), base.y(), CLAW_TINE_TIP_Z), 1 + CLAW_PRONG_DIRS.length + i, front, color, seed);
		}
	}

	private static Vector3f prongBase(int i) {
		return new Vector3f(CLAW_AXIS_X + CLAW_PRONG_DIRS[i][0] * CLAW_PRONG_RADIUS, CLAW_AXIS_Y + CLAW_PRONG_DIRS[i][1] * CLAW_PRONG_RADIUS, CLAW_HEAD_Z);
	}

	private static void clawRod(Matrix4f pose, VertexConsumer vc, Vector3f from, Vector3f to, int piece, Vector3f front, float[] color, float seed) {
		float length = new Vector3f(to).sub(from).length();
		renderAxisWeapon(pose, vc, from, to, null, -CLAW_ROD_PAD, length + CLAW_ROD_PAD, -CLAW_ROD_HALF_REACH, CLAW_ROD_HALF_REACH, CLAW_ROD_UV + piece * CLAW_ROD_STRIDE, CLAW_ROD_FRONT, front, color, seed);
	}

	private static void renderAxisWeapon(Matrix4f pose, VertexConsumer vc, Vector3f head, Vector3f butt, Vector3f sideRef,
										 float s0, float s1, float t0, float t1, float uOffset, float frontOffset, Vector3f fixedFront, float[] color, float seed) {
		Vector3f camera = new Matrix4f(pose).invert().transformPosition(new Vector3f());
		Vector3f axis = new Vector3f(butt).sub(head);
		if (axis.lengthSquared() < 1.0E-10f) return;
		Vector3f dir = new Vector3f(axis).normalize();
		Vector3f mid = new Vector3f(head).add(butt).mul(0.5f);
		Vector3f toCamera = new Vector3f(camera).sub(mid);
		Vector3f side = new Vector3f(dir).cross(toCamera);
		if (side.lengthSquared() < 1.0E-10f) return;
		side.normalize();
		if (sideRef != null && side.dot(sideRef) < 0.0f) side.negate();

		Vector3f front = new Vector3f(toCamera).sub(new Vector3f(dir).mul(toCamera.dot(dir)));
		if (front.lengthSquared() > 1.0E-10f) front.normalize(frontOffset);
		if (fixedFront != null) front.set(fixedFront);
		Vector3f origin = new Vector3f(head).add(front);

		axisVertex(vc, pose, origin, dir, side, s0, t0, uOffset, color, seed);
		axisVertex(vc, pose, origin, dir, side, s0, t1, uOffset, color, seed);
		axisVertex(vc, pose, origin, dir, side, s1, t1, uOffset, color, seed);
		axisVertex(vc, pose, origin, dir, side, s1, t0, uOffset, color, seed);
	}

	private static void axisVertex(VertexConsumer vc, Matrix4f pose, Vector3f origin, Vector3f dir, Vector3f side, float s, float t, float uOffset, float[] color, float seed) {
		float x = origin.x() + dir.x() * s + side.x() * t;
		float y = origin.y() + dir.y() * s + side.y() * t;
		float z = origin.z() + dir.z() * s + side.z() * t;
		bladeVertex(vc, pose, x, y, z, color, seed, uOffset + s, t);
	}

	public static float bladeSeed(Player player) {
		return (player.getId() * 0.618034f) % 1.0f;
	}

	public static void renderBlade(Matrix4f pose, VertexConsumer vc, Vector3f start, Vector3f end, float halfWidth, float[] color, float seed) {
		Vector3f camera = new Matrix4f(pose).invert().transformPosition(new Vector3f());
		Vector3f mid = new Vector3f(start).add(end).mul(0.5f);
		Vector3f axis = new Vector3f(end).sub(start);
		if (axis.lengthSquared() < 1.0E-10f) return;
		Vector3f dir = new Vector3f(axis).normalize();
		Vector3f toCamera = new Vector3f(camera).sub(mid);
		Vector3f side = axis.cross(toCamera, new Vector3f());
		if (side.lengthSquared() < 1.0E-10f) return;
		side.normalize(halfWidth);

		Vector3f front = new Vector3f(toCamera).sub(new Vector3f(dir).mul(toCamera.dot(dir)));
		if (front.lengthSquared() > 1.0E-10f) front.normalize(BLADE_FRONT_OFFSET);

		float length = axis.length();
		float uStart = Math.min(0.0f, (BURST_CENTER_OFFSET - BURST_HALF_SIZE) / length);
		float vMax = Math.max(1.0f, BURST_HALF_SIZE / halfWidth);
		Vector3f s = new Vector3f(start).add(front).add(new Vector3f(dir).mul(uStart * length));
		Vector3f e = new Vector3f(end).add(front);
		side.mul(vMax);

		bladeQuad(vc, pose, s, e, side, color, seed, uStart, 1.0f, vMax);
	}

	private static void bladeQuad(VertexConsumer vc, Matrix4f pose, Vector3f from, Vector3f to, Vector3f side, float[] color, float seed, float u0, float u1, float v) {
		bladeVertex(vc, pose, from.x() - side.x(), from.y() - side.y(), from.z() - side.z(), color, seed, u0, -v);
		bladeVertex(vc, pose, from.x() + side.x(), from.y() + side.y(), from.z() + side.z(), color, seed, u0, v);
		bladeVertex(vc, pose, to.x() + side.x(), to.y() + side.y(), to.z() + side.z(), color, seed, u1, v);
		bladeVertex(vc, pose, to.x() - side.x(), to.y() - side.y(), to.z() - side.z(), color, seed, u1, -v);
	}

	private static void bladeVertex(VertexConsumer vc, Matrix4f pose, float x, float y, float z, float[] color, float seed, float u, float v) {
		vc.vertex(pose, x, y, z).color(color[0], color[1], color[2], seed).uv(u, v).endVertex();
	}

	private static void syncTargetBoneAndParents(GeoBone destBone, BakedGeoModel sourceModel) {
		GeoBone currentDest = destBone;
		while (currentDest != null) {
			final GeoBone finalDest = currentDest;
			sourceModel.getBone(finalDest.getName()).ifPresent(sourceBone -> {
				finalDest.setRotX(sourceBone.getRotX());
				finalDest.setRotY(sourceBone.getRotY());
				finalDest.setRotZ(sourceBone.getRotZ());
				finalDest.setPosX(sourceBone.getPosX());
				finalDest.setPosY(sourceBone.getPosY());
				finalDest.setPosZ(sourceBone.getPosZ());
				finalDest.setScaleX(sourceBone.getScaleX());
				finalDest.setScaleY(sourceBone.getScaleY());
				finalDest.setScaleZ(sourceBone.getScaleZ());
			});
			currentDest = currentDest.getParent();
		}
	}
}