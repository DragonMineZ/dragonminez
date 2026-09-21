package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class DimensionalFistEffect {

	private static final int MAX_FISTS = 12;
	private static final double MAX_RENDER_DISTANCE_SQR = 128.0 * 128.0;

	private static final int EMERGE_TICKS = 4;
	private static final int HOLD_TICKS = 3;
	private static final int RETRACT_TICKS = 5;
	private static final int CLOSE_TICKS = 4;

	private static final float ARM_LENGTH = 10.0F / 16.0F;
	private static final float ARM_CENTER_OFFSET = 1.25F;
	private static final float DEFAULT_PIVOT_X = 5.0F;
	private static final float DEFAULT_PIVOT_Y = 22.0F;
	private static final float MIN_ARM_SCALE = 0.04F;
	private static final float ARM_FORWARD_ROT = (float) (Math.PI / 2.0D);

	private static final int RING_SEGMENTS = 40;
	private static final float[] RIM_COLOR = {0.93F, 0.87F, 1.0F};
	private static final float[] CORE_COLOR = {1.0F, 0.48F, 0.74F};
	private static final float[] GLOW_COLOR = {0.77F, 0.32F, 1.0F};

	private static final class Fist {
		final int entityId;
		final double x, y, z;
		final float yaw;
		final float pitch;
		final boolean leftArm;
		final int openTicks;
		final int lifetime;
		int age;

		private Fist(int entityId, double x, double y, double z, float yaw, float pitch, boolean leftArm, int openTicks) {
			this.entityId = entityId;
			this.x = x;
			this.y = y;
			this.z = z;
			this.yaw = yaw;
			this.pitch = pitch;
			this.leftArm = leftArm;
			this.openTicks = Math.max(1, openTicks);
			this.lifetime = this.openTicks + HOLD_TICKS + RETRACT_TICKS + CLOSE_TICKS;
		}

		float armExtension(float time) {
			float emergeStart = openTicks - EMERGE_TICKS;
			if (time < emergeStart) return 0.0F;
			if (time < openTicks) return easeOut((time - emergeStart) / EMERGE_TICKS);
			if (time < openTicks + HOLD_TICKS) return 1.0F;
			float retract = (time - openTicks - HOLD_TICKS) / RETRACT_TICKS;
			return Mth.clamp(1.0F - retract, 0.0F, 1.0F);
		}

		float portalOpening(float time) {
			float grow = Math.max(1.0F, openTicks - EMERGE_TICKS);
			if (time < grow) return easeOut(time / grow);
			float closeStart = lifetime - CLOSE_TICKS;
			if (time < closeStart) return 1.0F;
			return Mth.clamp(1.0F - (time - closeStart) / CLOSE_TICKS, 0.0F, 1.0F);
		}
	}

	private static final class Placement {
		float scaleX = 1.0F;
		float scaleY = 1.0F;
		float scaleZ = 1.0F;
		float pivotX = DEFAULT_PIVOT_X;
		float pivotY = DEFAULT_PIVOT_Y;
	}

	private static final List<Fist> FISTS = new ArrayList<>();
	private static final Map<GeoBone, boolean[]> SNAPSHOT = new IdentityHashMap<>();

	private static MultiBufferSource.BufferSource buffers;
	private static boolean renderingArm;
	private static String isolatedArm = "right_arm";

	private DimensionalFistEffect() {}

	public static boolean isRenderingArm() {
		return renderingArm;
	}

	public static void spawn(int entityId, double x, double y, double z, float yaw, float pitch, boolean leftArm, int openTicks) {
		if (FISTS.size() >= MAX_FISTS) FISTS.remove(0);
		FISTS.add(new Fist(entityId, x, y, z, yaw, pitch, leftArm, openTicks));
	}

	public static void isolateArm(BakedGeoModel model) {
		for (GeoBone bone : model.topLevelBones()) snapshotAndHide(bone);

		model.getBone(isolatedArm).ifPresent(arm -> {
			arm.setHidden(false);
			arm.setChildrenHidden(false);

			for (GeoBone child : arm.getChildBones()) {
				if (!child.getName().endsWith("_layer")) continue;
				boolean[] original = SNAPSHOT.get(child);
				if (original == null) continue;
				child.setHidden(original[0]);
				child.setChildrenHidden(original[1]);
			}

			GeoBone parent = arm.getParent();
			while (parent != null) {
				parent.setChildrenHidden(false);
				parent = parent.getParent();
			}
		});
	}

	public static float[] beginBonePose(GeoBone bone) {
		if (!renderingArm) return null;

		float[] saved = {
				bone.getRotX(), bone.getRotY(), bone.getRotZ(),
				bone.getPosX(), bone.getPosY(), bone.getPosZ(),
				bone.getScaleX(), bone.getScaleY(), bone.getScaleZ()
		};

		boolean arm = bone.getName().equals(isolatedArm);
		bone.setRotX(arm ? ARM_FORWARD_ROT : 0.0F);
		bone.setRotY(0.0F);
		bone.setRotZ(0.0F);
		bone.setPosX(0.0F);
		bone.setPosY(0.0F);
		bone.setPosZ(0.0F);
		bone.setScaleX(1.0F);
		bone.setScaleY(1.0F);
		bone.setScaleZ(1.0F);
		return saved;
	}

	public static void endBonePose(GeoBone bone, float[] saved) {
		if (saved == null) return;
		bone.setRotX(saved[0]);
		bone.setRotY(saved[1]);
		bone.setRotZ(saved[2]);
		bone.setPosX(saved[3]);
		bone.setPosY(saved[4]);
		bone.setPosZ(saved[5]);
		bone.setScaleX(saved[6]);
		bone.setScaleY(saved[7]);
		bone.setScaleZ(saved[8]);
	}

	private static void snapshotAndHide(GeoBone bone) {
		SNAPSHOT.putIfAbsent(bone, new boolean[]{bone.isHidden(), bone.isHidingChildren()});
		bone.setHidden(true);
		bone.setChildrenHidden(true);
		for (GeoBone child : bone.getChildBones()) snapshotAndHide(child);
	}

	private static void restoreSnapshot() {
		for (Map.Entry<GeoBone, boolean[]> entry : SNAPSHOT.entrySet()) {
			GeoBone bone = entry.getKey();
			bone.setHidden(entry.getValue()[0]);
			bone.setChildrenHidden(entry.getValue()[1]);
		}
		SNAPSHOT.clear();
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			FISTS.clear();
			return;
		}
		if (mc.isPaused() || FISTS.isEmpty()) return;
		FISTS.removeIf(fist -> ++fist.age >= fist.lifetime);
	}

	@SubscribeEvent
	public static void onRenderArms(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
		if (FISTS.isEmpty()) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		float partialTick = mc.isPaused() ? 0.0F : event.getPartialTick();
		Vec3 camPos = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();

		for (Fist fist : FISTS) {
			float extension = fist.armExtension(fist.age + partialTick);
			if (extension < MIN_ARM_SCALE) continue;
			if (camPos.distanceToSqr(fist.x, fist.y, fist.z) > MAX_RENDER_DISTANCE_SQR) continue;

			Entity entity = mc.level.getEntity(fist.entityId);
			if (!(entity instanceof LivingEntity owner) || !owner.isAlive()) continue;

			renderArm(mc, owner, fist, extension, partialTick, poseStack, camPos);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void renderArm(Minecraft mc, LivingEntity owner, Fist fist, float extension,
								  float partialTick, PoseStack poseStack, Vec3 camPos) {
		EntityRenderer renderer = owner instanceof AbstractClientPlayer acp
				? DMZRendererCache.getTPRenderer(acp)
				: mc.getEntityRenderDispatcher().getRenderer(owner);
		if (!(renderer instanceof GeoEntityRenderer geoRenderer)) return;

		String armName = fist.leftArm ? "left_arm" : "right_arm";
		Placement placement = resolvePlacement(owner, geoRenderer, armName);

		float armLength = ARM_LENGTH * placement.scaleZ;
		float shoulderHeight = placement.pivotY / 16.0F * placement.scaleY;
		float shoulderSide = (Math.abs(placement.pivotX) + ARM_CENTER_OFFSET) / 16.0F * placement.scaleX;
		if (fist.leftArm) shoulderSide = -shoulderSide;

		Vec3 dir = forward(fist.yaw, fist.pitch);
		Vec3 portal = new Vec3(fist.x, fist.y, fist.z).subtract(dir.scale(armLength));

		float bodyYaw = Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot);
		double bodyRad = Math.toRadians(bodyYaw);
		double rightX = -Math.cos(bodyRad);
		double rightZ = -Math.sin(bodyRad);

		int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(fist.x, fist.y, fist.z));

		poseStack.pushPose();
		poseStack.translate(portal.x - camPos.x, portal.y - camPos.y, portal.z - camPos.z);
		poseStack.mulPose(Axis.YP.rotationDegrees(-fist.yaw));
		poseStack.mulPose(Axis.XP.rotationDegrees(fist.pitch));
		poseStack.scale(1.0F, 1.0F, extension);
		poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw));
		poseStack.translate(-rightX * shoulderSide, -shoulderHeight, -rightZ * shoulderSide);

		boolean saga = !(owner instanceof AbstractClientPlayer);
		isolatedArm = armName;
		renderingArm = true;
		try {
			if (saga) {
				GeoModel model = geoRenderer.getGeoModel();
				BakedGeoModel baked = model.getBakedModel(model.getModelResource((software.bernie.geckolib.core.animatable.GeoAnimatable) owner));
				if (baked != null) isolateArm(baked);
			}
			if (buffers == null) buffers = MultiBufferSource.immediate(new BufferBuilder(4096));
			renderer.render(owner, bodyYaw, partialTick, poseStack, buffers, light);
			buffers.endBatch();
		} finally {
			renderingArm = false;
			restoreSnapshot();
			poseStack.popPose();
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Placement resolvePlacement(LivingEntity owner, GeoEntityRenderer geoRenderer, String armName) {
		Placement placement = new Placement();

		if (owner instanceof DBSagasEntity saga) {
			float scale = saga.getScale();
			placement.scaleX = scale;
			placement.scaleY = scale;
			placement.scaleZ = scale;
		} else if (owner instanceof AbstractClientPlayer player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
				Float[] resolved = stats.getCharacter().getResolvedModelScaling();
				if (resolved == null || resolved.length < 3) return;
				placement.scaleX = resolved[0];
				placement.scaleY = resolved[1];
				placement.scaleZ = resolved[2];
			});
		}

		geoRenderer.getGeoModel().getBone(armName).ifPresent(bone -> {
			GeoBone arm = (GeoBone) bone;
			placement.pivotX = arm.getPivotX();
			placement.pivotY = arm.getPivotY();
		});
		return placement;
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderPortals(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || FISTS.isEmpty()) return;

		boolean iris = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		RenderLevelStageEvent.Stage target = iris
				? RenderLevelStageEvent.Stage.AFTER_LEVEL
				: RenderLevelStageEvent.Stage.AFTER_WEATHER;
		if (event.getStage() != target) return;

		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 camPos = camera.getPosition();
		float partialTick = mc.isPaused() ? 0.0F : event.getPartialTick();

		if (iris) mc.getMainRenderTarget().bindWrite(false);
		PoseStack poseStack = iris ? viewStack(camera) : event.getPoseStack();

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();

		PoseStack modelView = RenderSystem.getModelViewStack();
		modelView.pushPose();
		modelView.setIdentity();
		RenderSystem.applyModelViewMatrix();

		for (Fist fist : FISTS) {
			if (camPos.distanceToSqr(fist.x, fist.y, fist.z) > MAX_RENDER_DISTANCE_SQR) continue;

			Entity entity = mc.level.getEntity(fist.entityId);
			float scale = lengthScaleOf(entity);

			float time = fist.age + partialTick;
			float opening = fist.portalOpening(time);
			if (opening <= 0.01F) continue;

			Vec3 dir = forward(fist.yaw, fist.pitch);
			Vec3 portal = new Vec3(fist.x, fist.y, fist.z).subtract(dir.scale(ARM_LENGTH * scale));
			float radius = (0.30F * scale + 0.45F) * opening;

			poseStack.pushPose();
			poseStack.translate(portal.x - camPos.x, portal.y - camPos.y, portal.z - camPos.z);
			poseStack.mulPose(Axis.YP.rotationDegrees(-fist.yaw));
			poseStack.mulPose(Axis.XP.rotationDegrees(fist.pitch));
			drawPortal(poseStack.last().pose(), radius, time, opening);
			poseStack.popPose();
		}

		modelView.popPose();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static void drawPortal(Matrix4f pose, float radius, float time, float opening) {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.getBuilder();

		float pulse = 0.85F + 0.15F * Mth.sin(time * 0.9F);
		float coreAlpha = 0.55F * opening;

		buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
		buffer.vertex(pose, 0.0F, 0.0F, 0.0F).color(CORE_COLOR[0], CORE_COLOR[1], CORE_COLOR[2], coreAlpha).endVertex();
		for (int i = 0; i <= RING_SEGMENTS; i++) {
			double angle = (Math.PI * 2.0D / RING_SEGMENTS) * i;
			float px = (float) Math.cos(angle) * radius * 0.86F;
			float py = (float) Math.sin(angle) * radius * 0.86F;
			buffer.vertex(pose, px, py, 0.0F).color(GLOW_COLOR[0], GLOW_COLOR[1], GLOW_COLOR[2], coreAlpha * 0.75F).endVertex();
		}
		tesselator.end();

		drawRing(pose, radius * 0.86F, radius, RIM_COLOR, 0.95F * opening * pulse, 0.0F);
		drawRing(pose, radius * 1.08F, radius * 1.16F, GLOW_COLOR, 0.55F * opening * pulse, time * 0.12F);
		drawRing(pose, radius * 0.48F, radius * 0.54F, RIM_COLOR, 0.45F * opening, -time * 0.2F);
	}

	private static void drawRing(Matrix4f pose, float inner, float outer, float[] color, float alpha, float spin) {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.getBuilder();

		buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i <= RING_SEGMENTS; i++) {
			double angle = (Math.PI * 2.0D / RING_SEGMENTS) * i + spin;
			float cos = (float) Math.cos(angle);
			float sin = (float) Math.sin(angle);
			buffer.vertex(pose, cos * inner, sin * inner, 0.0F).color(color[0], color[1], color[2], alpha).endVertex();
			buffer.vertex(pose, cos * outer, sin * outer, 0.0F).color(color[0], color[1], color[2], alpha * 0.85F).endVertex();
		}
		tesselator.end();
	}

	private static float lengthScaleOf(Entity entity) {
		if (entity instanceof DBSagasEntity saga) return saga.getScale();
		if (entity instanceof AbstractClientPlayer player) {
			return StatsProvider.get(StatsCapability.INSTANCE, player).map(stats -> {
				Float[] resolved = stats.getCharacter().getResolvedModelScaling();
				return resolved != null && resolved.length >= 3 ? resolved[2] : 1.0F;
			}).orElse(1.0F);
		}
		return 1.0F;
	}

	private static Vec3 forward(float yaw, float pitch) {
		double yawRad = Math.toRadians(yaw);
		double pitchRad = Math.toRadians(pitch);
		double flat = Math.cos(pitchRad);
		return new Vec3(-Math.sin(yawRad) * flat, -Math.sin(pitchRad), Math.cos(yawRad) * flat);
	}

	private static float easeOut(float t) {
		float clamped = Mth.clamp(t, 0.0F, 1.0F);
		float inv = 1.0F - clamped;
		return 1.0F - inv * inv * inv;
	}

	private static PoseStack viewStack(Camera camera) {
		PoseStack stack = new PoseStack();
		stack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
		stack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
		return stack;
	}
}
