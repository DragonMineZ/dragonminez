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

	private static final List<Mark> MARKS = new ArrayList<>();

	private TelegraphEffect() {}

	public static void spawn(double x, double y, double z, float radius, int color, int lifetime, int followEntityId) {
		if (radius <= 0.0F || lifetime <= 0) return;
		if (MARKS.size() >= MAX_MARKS) MARKS.remove(0);
		MARKS.add(new Mark(x, y, z, radius, ColorUtils.rgbIntToFloat(color), lifetime, followEntityId));
	}

	public static void clear() {
		MARKS.clear();
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			MARKS.clear();
			return;
		}
		if (mc.isPaused() || MARKS.isEmpty()) return;
		MARKS.removeIf(mark -> ++mark.age >= mark.lifetime);
		for (Mark mark : MARKS) follow(mc, mark);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || MARKS.isEmpty()) return;

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

	private static PoseStack viewStack(Camera camera) {
		PoseStack stack = new PoseStack();
		stack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
		stack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
		return stack;
	}
}
