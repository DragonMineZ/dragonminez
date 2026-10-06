package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.EffectBloomRenderer;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.util.ColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class TelegraphEffect {

	private static final int MAX_MARKS = 24;
	private static final double MAX_RENDER_DISTANCE_SQR = 128.0 * 128.0;
	private static final int SEGMENTS = 48;
	private static final float GROUND_OFFSET = 0.06F;
	private static final float RING_THICKNESS = 0.45F;
	private static final float FILL_ALPHA = 0.30F;
	private static final float RING_ALPHA = 0.75F;

	private static final class Mark {
		double x, y, z;
		final int followEntityId;
		final float radius;
		final float[] color;
		final int lifetime;
		int age;

		private Mark(double x, double y, double z, float radius, float[] color, int lifetime, int followEntityId) {
			this.followEntityId = followEntityId;
			this.x = x;
			this.y = y;
			this.z = z;
			this.radius = radius;
			this.color = color;
			this.lifetime = lifetime;
		}
	}

	private static final class Lane {
		final int ownerId;
		final Vec3 start;
		final Vec3 end;
		final float halfWidth;
		final float[] color;
		final int lifetime;
		final boolean locked;
		int age;

		private Lane(int ownerId, Vec3 start, Vec3 end, float halfWidth, float[] color, int lifetime, boolean locked) {
			this.ownerId = ownerId;
			this.start = start;
			this.end = end;
			this.halfWidth = halfWidth;
			this.color = color;
			this.lifetime = lifetime;
			this.locked = locked;
		}
	}

	private static final int MAX_LANES = 12;
	private static final int LANE_SEGMENTS = 24;
	private static final float LANE_EDGE = 0.18F;
	private static final float CHEVRON_SPACING = 2.2F;
	private static final float CHEVRON_SPEED = 0.45F;
	private static final float CHEVRON_THICKNESS = 0.28F;

	private static final List<Mark> MARKS = new ArrayList<>();
	private static final List<Lane> LANES = new ArrayList<>();

	private TelegraphEffect() {}

	public static void spawnLane(int ownerId, double startX, double startY, double startZ, double endX, double endY, double endZ,
								 float halfWidth, int color, int lifetime, boolean locked) {
		if (halfWidth <= 0.0F || lifetime <= 0) return;
		LANES.removeIf(lane -> lane.ownerId == ownerId);
		if (LANES.size() >= MAX_LANES) LANES.remove(0);
		LANES.add(new Lane(ownerId, new Vec3(startX, startY, startZ), new Vec3(endX, endY, endZ), halfWidth,
				ColorUtils.rgbIntToFloat(color), lifetime, locked));
	}

	public static void spawn(double x, double y, double z, float radius, int color, int lifetime, int followEntityId) {
		if (radius <= 0.0F || lifetime <= 0) return;
		if (MARKS.size() >= MAX_MARKS) MARKS.remove(0);
		MARKS.add(new Mark(x, y, z, radius, ColorUtils.rgbIntToFloat(color), lifetime, followEntityId));
	}

	public static void clear() {
		MARKS.clear();
		LANES.clear();
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			clear();
			return;
		}
		if (mc.isPaused()) return;
		LANES.removeIf(lane -> ++lane.age >= lane.lifetime);
		if (MARKS.isEmpty()) return;
		MARKS.removeIf(mark -> ++mark.age >= mark.lifetime);
		for (Mark mark : MARKS) follow(mc, mark);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || (MARKS.isEmpty() && LANES.isEmpty())) return;

		boolean iris = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		float partialTick = mc.isPaused() ? 0.0F : event.getPartialTick();

		if (!iris) {
			if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
			PlayerEffectQueue.addKiAttack((stack, projection) -> drawAll(stack, partialTick));
			return;
		}

		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
		mc.getMainRenderTarget().bindWrite(false);
		drawAll(viewStack(mc.gameRenderer.getMainCamera()), partialTick);
	}

	private static void follow(Minecraft mc, Mark mark) {
		if (mark.followEntityId < 0 || mc.level == null) return;
		Entity entity = mc.level.getEntity(mark.followEntityId);
		if (entity == null) return;

		mark.x = entity.getX();
		mark.z = entity.getZ();

		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		int ix = Mth.floor(mark.x);
		int iz = Mth.floor(mark.z);
		for (int y = Mth.floor(entity.getY()) + 1; y >= mc.level.getMinBuildHeight(); y--) {
			cursor.set(ix, y, iz);
			if (!mc.level.getBlockState(cursor).isAir()) {
				mark.y = y + 1.0D;
				return;
			}
		}
	}

	private static void drawAll(PoseStack poseStack, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
		boolean bloom = EffectBloomRenderer.bloomPass;

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

		for (Mark mark : MARKS) {
			double dx = camPos.x - mark.x;
			double dy = camPos.y - mark.y;
			double dz = camPos.z - mark.z;
			if (dx * dx + dy * dy + dz * dz > MAX_RENDER_DISTANCE_SQR) continue;

			float progress = Mth.clamp((mark.age + partialTick) / mark.lifetime, 0.0F, 1.0F);

			poseStack.pushPose();
			poseStack.translate(mark.x - camPos.x, mark.y - camPos.y + GROUND_OFFSET, mark.z - camPos.z);
			drawMark(poseStack.last().pose(), mark, progress, bloom);
			poseStack.popPose();
		}

		for (Lane lane : LANES) {
			if (lane.start.distanceToSqr(camPos) > MAX_RENDER_DISTANCE_SQR && lane.end.distanceToSqr(camPos) > MAX_RENDER_DISTANCE_SQR) continue;
			drawLane(poseStack.last().pose(), lane, camPos, partialTick, bloom);
		}

		modelView.popPose();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static void drawMark(Matrix4f pose, Mark mark, float progress, boolean bloom) {
		float r = mark.color[0];
		float g = mark.color[1];
		float b = mark.color[2];

		float pulse = 0.8F + 0.2F * Mth.sin(progress * 28.0F);
		float urgency = 0.6F + 0.4F * progress;
		float fillRadius = mark.followEntityId >= 0 ? 0.0F : mark.radius * progress;

		float fillAlpha = (bloom ? 0.55F : FILL_ALPHA) * pulse * urgency;
		float ringAlpha = (bloom ? 1.0F : RING_ALPHA) * pulse;
		float thickness = bloom ? RING_THICKNESS * 2.4F : RING_THICKNESS;

		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.getBuilder();

		buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
		buffer.vertex(pose, 0.0F, 0.0F, 0.0F).color(r, g, b, fillAlpha).endVertex();
		for (int i = 0; i <= SEGMENTS; i++) {
			double angle = (Math.PI * 2.0D / SEGMENTS) * i;
			float px = (float) (Math.cos(angle) * fillRadius);
			float pz = (float) (Math.sin(angle) * fillRadius);
			buffer.vertex(pose, px, 0.0F, pz).color(r, g, b, fillAlpha * 0.45F).endVertex();
		}
		tesselator.end();

		ring(pose, Math.max(0.0F, mark.radius - thickness), mark.radius, r, g, b, ringAlpha, ringAlpha);
		ring(pose, mark.radius, mark.radius + thickness * 1.6F, r, g, b, ringAlpha * 0.55F, 0.0F);
		if (fillRadius > 0.05F) {
			ring(pose, Math.max(0.0F, fillRadius - thickness * 0.5F), fillRadius, 1.0F, 0.85F, 0.8F, ringAlpha * 0.8F, ringAlpha * 0.8F);
		}
	}

	private static void ring(Matrix4f pose, float inner, float outer, float r, float g, float b, float innerAlpha, float outerAlpha) {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.getBuilder();

		buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i <= SEGMENTS; i++) {
			double angle = (Math.PI * 2.0D / SEGMENTS) * i;
			float cos = (float) Math.cos(angle);
			float sin = (float) Math.sin(angle);
			buffer.vertex(pose, cos * inner, 0.0F, sin * inner).color(r, g, b, innerAlpha).endVertex();
			buffer.vertex(pose, cos * outer, 0.0F, sin * outer).color(r, g, b, outerAlpha).endVertex();
		}
		tesselator.end();
	}

	private static void drawLane(Matrix4f pose, Lane lane, Vec3 camPos, float partialTick, boolean bloom) {
		Vec3 start = lane.start.subtract(camPos);
		Vec3 axis = lane.end.subtract(lane.start);
		double length = axis.length();
		if (length < 0.1D) return;
		Vec3 dir = axis.scale(1.0D / length);

		float r = lane.color[0];
		float g = lane.color[1];
		float b = lane.color[2];
		float age = lane.age + partialTick;
		float progress = Mth.clamp(age / lane.lifetime, 0.0F, 1.0F);
		float pulse = 0.8F + 0.2F * Mth.sin(age * 0.9F);
		float width = lane.halfWidth * (bloom ? 1.15F : 1.0F);
		float fillAlpha = (lane.locked ? 0.32F : 0.16F) * pulse * (bloom ? 1.6F : 1.0F);
		float edgeAlpha = (lane.locked ? 0.95F : 0.5F) * pulse;
		float edge = bloom ? LANE_EDGE * 2.2F : LANE_EDGE;

		Vec3[] points = new Vec3[LANE_SEGMENTS + 1];
		Vec3[] sides = new Vec3[LANE_SEGMENTS + 1];
		for (int i = 0; i <= LANE_SEGMENTS; i++) {
			Vec3 point = start.add(dir.scale(length * i / LANE_SEGMENTS));
			points[i] = point;
			sides[i] = laneSide(dir, point);
		}

		strip(pose, points, sides, -width, -width * 0.15F, r, g, b, fillAlpha * 0.35F, fillAlpha);
		strip(pose, points, sides, -width * 0.15F, width * 0.15F, r, g, b, fillAlpha, fillAlpha);
		strip(pose, points, sides, width * 0.15F, width, r, g, b, fillAlpha, fillAlpha * 0.35F);
		strip(pose, points, sides, -width - edge, -width, r, g, b, 0.0F, edgeAlpha);
		strip(pose, points, sides, width, width + edge, r, g, b, edgeAlpha, 0.0F);

		if (lane.locked) {
			int filled = Math.max(1, Math.round(LANE_SEGMENTS * progress));
			Vec3[] corePoints = new Vec3[filled + 1];
			Vec3[] coreSides = new Vec3[filled + 1];
			System.arraycopy(points, 0, corePoints, 0, filled + 1);
			System.arraycopy(sides, 0, coreSides, 0, filled + 1);
			float core = width * 0.35F;
			strip(pose, corePoints, coreSides, -core, core, 1.0F, 0.85F, 0.8F, edgeAlpha * 0.55F, edgeAlpha * 0.55F);
		}

		float chevronAlpha = (lane.locked ? 0.9F : 0.45F) * pulse;
		float arm = width * 0.75F;
		float offset = (age * CHEVRON_SPEED * (lane.locked ? 2.0F : 1.0F)) % CHEVRON_SPACING;
		for (float d = offset + arm; d < length - 0.2F; d += CHEVRON_SPACING) {
			Vec3 tip = start.add(dir.scale(d));
			Vec3 side = laneSide(dir, tip);
			chevronArm(pose, tip, dir, side.scale(arm), arm, r, g, b, chevronAlpha);
			chevronArm(pose, tip, dir, side.scale(-arm), arm, r, g, b, chevronAlpha);
		}
	}

	private static Vec3 laneSide(Vec3 dir, Vec3 point) {
		Vec3 side = dir.cross(point.scale(-1.0D));
		if (side.lengthSqr() < 1.0E-6D) side = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));
		if (side.lengthSqr() < 1.0E-6D) side = new Vec3(1.0D, 0.0D, 0.0D);
		return side.normalize();
	}

	private static void strip(Matrix4f pose, Vec3[] points, Vec3[] sides, float from, float to, float r, float g, float b,
							  float fromAlpha, float toAlpha) {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.getBuilder();
		buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < points.length; i++) {
			Vec3 a = points[i].add(sides[i].scale(from));
			Vec3 c = points[i].add(sides[i].scale(to));
			buffer.vertex(pose, (float) a.x, (float) a.y, (float) a.z).color(r, g, b, fromAlpha).endVertex();
			buffer.vertex(pose, (float) c.x, (float) c.y, (float) c.z).color(r, g, b, toAlpha).endVertex();
		}
		tesselator.end();
	}

	private static void chevronArm(Matrix4f pose, Vec3 tip, Vec3 dir, Vec3 sideOffset, float arm, float r, float g, float b, float alpha) {
		Vec3 outer = tip.subtract(dir.scale(arm)).add(sideOffset);
		Vec3 back = dir.scale(CHEVRON_THICKNESS);
		Vec3[] corners = {outer, tip, tip.subtract(back), outer.subtract(back)};

		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (Vec3 corner : corners) {
			buffer.vertex(pose, (float) corner.x, (float) corner.y, (float) corner.z).color(r, g, b, alpha).endVertex();
		}
		tesselator.end();
	}

	private static PoseStack viewStack(Camera camera) {
		PoseStack stack = new PoseStack();
		stack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
		stack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
		return stack;
	}
}
