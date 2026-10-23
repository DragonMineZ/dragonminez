package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.shader.BloomPipeline;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.combat.logic.weapon.KiWeaponHelper;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	private record EntityBladeEntry(Entity entity, BakedGeoModel model, Matrix4f pose, float[] color, float partialTick) {}

	private static final List<EntityBladeEntry> ENTITY_BLADES = new ArrayList<>();

	public static void queueEntityBlade(Entity entity, BakedGeoModel model, PoseStack poseStack, float[] color, float partialTick) {
		ENTITY_BLADES.add(new EntityBladeEntry(entity, model, new Matrix4f(poseStack.last().pose()), color, partialTick));
	}

	private static void processEntityBlades(MultiBufferSource.BufferSource buffers) {
		if (ENTITY_BLADES.isEmpty()) return;
		if (DMZShaders.kiBladeShader != null) {
			for (EntityBladeEntry entry : ENTITY_BLADES) {
				float[] anchor = boneAnchor(entry.model(), "right_arm");
				if (anchor == null) continue;
				Vector3f start = new Vector3f(anchor[0], anchor[1] + BLADE_WRIST_OFFSET + BLADE_ARM_SHIFT, anchor[2]);
				Vector3f end = new Vector3f(anchor[0], anchor[1] - bladeReach(entry.entity(), entry.partialTick()) + BLADE_ARM_SHIFT, anchor[2]);
				float seed = bladeSeed(entry.entity());
				float[] color = entry.color().clone();
				renderBlade(entry.pose(), buffers.getBuffer(ModRenderTypes.kiBlade()), start, end, BLADE_HALF_WIDTH, color, seed);
				Matrix4f pose = new Matrix4f(entry.pose());
				BLOOM_WEAPONS.add(vc -> renderBlade(pose, vc, start, end, BLADE_HALF_WIDTH, color, seed));
			}
		}
		ENTITY_BLADES.clear();
	}

	public static class SagaBladeLayer<T extends DBSagasEntity> extends GeoRenderLayer<T> {

		public SagaBladeLayer(GeoRenderer<T> renderer) {
			super(renderer);
		}

		@Override
		public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource,
								  VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
			if (!"right_arm".equals(bone.getName()) || animatable.isInvisible() || !forcesBlade(animatable)) return;
			BakedGeoModel model = getGeoModel().getBakedModel(getGeoModel().getModelResource(animatable));
			if (model == null) return;
			int aura = animatable.getAuraColor();
			float[] rgb = {((aura >> 16) & 0xFF) / 255.0f, ((aura >> 8) & 0xFF) / 255.0f, (aura & 0xFF) / 255.0f};
			queueEntityBlade(animatable, model, poseStack, KiWeaponHelper.resolveColorForType("blade", rgb), partialTick);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void processWeapons(MultiBufferSource.BufferSource buffers, PoseStack poseStack) {
		processEntityBlades(buffers);
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

						rawRenderer.renderRecursively(poseStack, (AbstractClientPlayer) player, targetBone, renderType, buffers, vertexConsumer, true,
							entry.partialTick(), 15728880, OverlayTexture.NO_OVERLAY,
							com.dragonminez.client.render.util.RenderBufferUtil.packColor(entry.color()[0], entry.color()[1], entry.color()[2], 0.65f));

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
		Vector3f end = new Vector3f(anchor[0], anchor[1] - bladeReach(entry.player(), entry.partialTick()) + BLADE_ARM_SHIFT, anchor[2]);
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
		return boneAnchor(entry.playerModel(), boneName);
	}

	private static float[] boneAnchor(BakedGeoModel model, String boneName) {
		GeoBone arm = model.getBone(boneName).orElse(null);
		if (arm == null) return null;
		List<GeoCube> cubes = new ArrayList<>(arm.getCubes());
		if (cubes.isEmpty()) arm.getChildBones().forEach(child -> cubes.addAll(child.getCubes()));
		if (cubes.isEmpty()) return null;

		float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (GeoCube cube : cubes) {
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
		drawBloom(BLOOM_WEAPONS);
	}

	private static final List<Consumer<VertexConsumer>> FIRST_PERSON_BLOOM = new ArrayList<>();
	private static final float FP_ARM_PIVOT_X = 5.0f / 16.0f;
	private static final float FP_ARM_PIVOT_Y = 22.0f / 16.0f;
	private static final float FP_GEO_ARM_X = 6.0f / 16.0f;
	private static final float FP_GEO_HAND_Y = 12.0f / 16.0f;

	public static void renderFirstPerson(Matrix4f armPose, MultiBufferSource buffer, String type, boolean rightArm, Entity entity, float[] color, float partialTick) {
		float seed = bladeSeed(entity);
		float[] c = color.clone();
		Consumer<VertexConsumer> draw;
		if (usesShaderBlade(type)) {
			Matrix4f pose = new Matrix4f(armPose);
			float cx = (rightArm ? -1.0f : 1.0f) / 16.0f;
			float handEnd = 10.0f / 16.0f;
			Vector3f start = new Vector3f(cx, handEnd - BLADE_WRIST_OFFSET - BLADE_ARM_SHIFT, 0.0f);
			Vector3f end = new Vector3f(cx, handEnd + bladeReach(entity, partialTick) - BLADE_ARM_SHIFT, 0.0f);
			draw = vc -> renderBlade(pose, vc, start, end, BLADE_HALF_WIDTH, c, seed);
		} else if (usesShaderScythe(type)) {
			Matrix4f pose = geoArmPose(armPose, rightArm);
			float gripY = FP_GEO_HAND_Y + SCYTHE_GRIP_HEIGHT;
			Vector3f head = new Vector3f(FP_GEO_ARM_X, gripY, -SCYTHE_HEAD_FORWARD);
			Vector3f butt = new Vector3f(FP_GEO_ARM_X, gripY, -SCYTHE_HEAD_FORWARD + SCYTHE_SHAFT_LENGTH);
			Vector3f bladeSide = new Vector3f(0.0f, -1.0f, 0.0f);
			draw = vc -> renderScythe(pose, vc, head, butt, bladeSide, c, seed);
		} else if (usesShaderClawLance(type)) {
			Matrix4f pose = geoArmPose(armPose, rightArm).rotateY(CLAW_YAW).rotateX(CLAW_PITCH).translate(0.0f, -0.1f, -1.0f);
			draw = vc -> renderClawLance(pose, vc, c, seed);
		} else {
			return;
		}
		draw.accept(buffer.getBuffer(ModRenderTypes.kiBlade()));
		FIRST_PERSON_BLOOM.add(draw);
	}

	private static Matrix4f geoArmPose(Matrix4f armPose, boolean rightArm) {
		return new Matrix4f(armPose).translate(rightArm ? FP_ARM_PIVOT_X : -FP_ARM_PIVOT_X, FP_ARM_PIVOT_Y, 0.0f).scale(rightArm ? -1.0f : 1.0f, -1.0f, 1.0f);
	}

	public static void renderFirstPersonBloom() {
		if (FIRST_PERSON_BLOOM.isEmpty()) return;
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		if (DMZShaders.kiBladeShader == null || !BloomPipeline.beginRedraw(main)) {
			FIRST_PERSON_BLOOM.clear();
			return;
		}
		try {
			RenderSystem.depthMask(false);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();
			RenderSystem.enableDepthTest();
			drawBloom(FIRST_PERSON_BLOOM);
		} finally {
			RenderSystem.enableCull();
			RenderSystem.disableBlend();
			RenderSystem.depthMask(true);
			BloomPipeline.endRedraw();
		}
		BloomPipeline.resolve(main);
	}

	private static void drawBloom(List<Consumer<VertexConsumer>> draws) {
		ShaderInstance shader = DMZShaders.kiBladeShader;
		if (shader == null || draws.isEmpty()) {
			draws.clear();
			return;
		}

		com.mojang.blaze3d.vertex.ByteBufferBuilder byteBufferBuilder = new com.mojang.blaze3d.vertex.ByteBufferBuilder(256 * 1024);
		MultiBufferSource.BufferSource source = MultiBufferSource.immediate(byteBufferBuilder);
		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		modelView.identity();
		RenderSystem.applyModelViewMatrix();
		shader.safeGetUniform("bloomMode").set(1.0f);
		try {
			VertexConsumer vc = source.getBuffer(ModRenderTypes.kiBlade());
			for (Consumer<VertexConsumer> draw : draws) draw.accept(vc);
			source.endBatch();
		} finally {
			shader.safeGetUniform("bloomMode").set(0.0f);
			modelView.popMatrix();
			RenderSystem.applyModelViewMatrix();
			byteBufferBuilder.close();
			draws.clear();
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

	private static final Map<Integer, long[]> BLADE_SUMMONS = new HashMap<>();
	private static final Map<Integer, float[]> BLADE_EXTENSIONS = new HashMap<>();
	private static final float BLADE_SUMMON_TICKS = 6.0f;
	private static final float BLADE_GROW_TICKS = 2.0f;
	private static final float BLADE_SHRINK_TICKS = 3.0f;

	public static void summonBlade(int entityId, int ticks) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		if (ticks <= 0) {
			BLADE_SUMMONS.remove(entityId);
			BLADE_EXTENSIONS.remove(entityId);
			return;
		}
		BLADE_SUMMONS.put(entityId, new long[]{mc.level.getGameTime(), ticks});
	}

	public static boolean forcesBlade(Entity entity) {
		long[] summon = BLADE_SUMMONS.get(entity.getId());
		if (summon == null) return false;
		long age = entity.level().getGameTime() - summon[0];
		if (age < 0L || age > summon[1]) {
			BLADE_SUMMONS.remove(entity.getId());
			return false;
		}
		return true;
	}

	public static void extendBlade(int entityId, float length, int ticks) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		BLADE_EXTENSIONS.put(entityId, new float[]{length, mc.level.getGameTime(), ticks});
	}

	public static float bladeReach(Entity player, float partialTick) {
		float reach = BLADE_REACH;
		float[] extension = BLADE_EXTENSIONS.get(player.getId());
		if (extension != null) {
			float age = player.level().getGameTime() - extension[1] + partialTick;
			if (age > extension[2] || age < 0.0f) {
				BLADE_EXTENSIONS.remove(player.getId());
			} else {
				float grow = Mth.clamp(age / BLADE_GROW_TICKS, 0.0f, 1.0f);
				float shrink = Mth.clamp((extension[2] - age) / BLADE_SHRINK_TICKS, 0.0f, 1.0f);
				reach = Mth.lerp(Math.min(grow, shrink), BLADE_REACH, Math.max(BLADE_REACH, extension[0]));
			}
		}
		long[] summon = BLADE_SUMMONS.get(player.getId());
		if (summon != null) {
			float age = player.level().getGameTime() - summon[0] + partialTick;
			reach *= Mth.clamp(age / BLADE_SUMMON_TICKS, 0.05f, 1.0f);
		}
		return reach;
	}

	public static float bladeSeed(Entity player) {
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
		vc.addVertex(pose, x, y, z).setColor(color[0], color[1], color[2], seed).setUv(u, v);
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
