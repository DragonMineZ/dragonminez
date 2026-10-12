package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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

public class KiWeaponRenderer {
	public static final float BLADE_WRIST_OFFSET = 3.0f / 16.0f;
	public static final float BLADE_REACH = 18.0f / 16.0f;
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

	private static void renderBladeThirdPerson(PlayerEffectQueue.WeaponRenderEntry entry, MultiBufferSource.BufferSource buffers) {
		String boneName = entry.player().getMainArm() == HumanoidArm.RIGHT ? "right_arm" : "left_arm";
		GeoBone arm = entry.playerModel().getBone(boneName).orElse(null);
		if (arm == null || arm.getCubes().isEmpty()) return;

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
		if (minY == Float.MAX_VALUE) return;

		float cx = (minX + maxX) * 0.5f;
		float cz = (minZ + maxZ) * 0.5f;
		Vector3f start = new Vector3f(cx, minY + BLADE_WRIST_OFFSET, cz);
		Vector3f end = new Vector3f(cx, minY - BLADE_REACH, cz);
		renderBlade(entry.poseMatrix(), buffers.getBuffer(ModRenderTypes.kiBlade()), start, end, BLADE_HALF_WIDTH, entry.color(), bladeSeed(entry.player()));
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
		Vector3f s = new Vector3f(start).add(front);
		Vector3f e = new Vector3f(end).add(front);

		bladeQuad(vc, pose, s, e, side, color, seed, 0.0f, 1.0f);

		Vector3f center = new Vector3f(start).add(new Vector3f(dir).mul(BURST_CENTER_OFFSET));
		Vector3f view = new Vector3f(camera).sub(center);
		if (view.lengthSquared() < 1.0E-10f) return;
		view.normalize();
		Vector3f right = dir.cross(view, new Vector3f());
		if (right.lengthSquared() < 1.0E-10f) right = new Vector3f(side);
		right.normalize(BURST_HALF_SIZE);
		Vector3f up = view.cross(right, new Vector3f()).normalize(BURST_HALF_SIZE);
		center.add(new Vector3f(view).mul(BLADE_FRONT_OFFSET * 1.2f));

		Vector3f a = new Vector3f(center).sub(up);
		Vector3f b = new Vector3f(center).add(up);
		bladeQuad(vc, pose, a, b, right, color, seed, 9.0f, 11.0f);
	}

	private static void bladeQuad(VertexConsumer vc, Matrix4f pose, Vector3f from, Vector3f to, Vector3f side, float[] color, float seed, float u0, float u1) {
		bladeVertex(vc, pose, from.x() - side.x(), from.y() - side.y(), from.z() - side.z(), color, seed, u0, -1.0f);
		bladeVertex(vc, pose, from.x() + side.x(), from.y() + side.y(), from.z() + side.z(), color, seed, u0, 1.0f);
		bladeVertex(vc, pose, to.x() + side.x(), to.y() + side.y(), to.z() + side.z(), color, seed, u1, 1.0f);
		bladeVertex(vc, pose, to.x() - side.x(), to.y() - side.y(), to.z() - side.z(), color, seed, u1, -1.0f);
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