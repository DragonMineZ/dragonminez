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
public final class SwordSlashEffect {

	private static final int MAX_SLASHES = 40;
	private static final double MAX_RENDER_DISTANCE_SQR = 160.0 * 160.0;

	private static final int SEGMENTS = 28;
	private static final float HALF_ARC = (float) Math.toRadians(78.0D);
	private static final float THICKNESS = 0.26F;
	private static final float GLOW = 0.20F;
	private static final float RIBBON = 0.16F;
	private static final int TRAIL_GHOSTS = 3;
	private static final float TRAIL_SPACING = 0.9F;
	private static final int FADE_IN_TICKS = 2;
	private static final int FADE_OUT_TICKS = 5;

	private static final class Slash {
		final double x, y, z;
		final Vec3 dir;
		final float speed;
		final float roll;
		final float radius;
		final float[] color;
		final int lifetime;
		final float yaw;
		final float pitch;
		int age;

		private Slash(double x, double y, double z, Vec3 dir, float speed, float roll, float radius, float[] color, int lifetime) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.dir = dir;
			this.speed = speed;
			this.roll = roll;
			this.radius = radius;
			this.color = color;
			this.lifetime = lifetime;
			this.yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
			this.pitch = (float) -Math.toDegrees(Math.asin(Mth.clamp(dir.y, -1.0D, 1.0D)));
		}
	}

	private static final List<Slash> SLASHES = new ArrayList<>();

	private SwordSlashEffect() {}

	public static void spawn(double x, double y, double z, double dx, double dy, double dz,
							 float speed, float roll, float radius, int color, int lifetime) {
		Vec3 dir = new Vec3(dx, dy, dz);
		if (dir.lengthSqr() < 1.0E-6 || radius <= 0.0F || lifetime <= 0) return;
		if (SLASHES.size() >= MAX_SLASHES) SLASHES.remove(0);
		SLASHES.add(new Slash(x, y, z, dir.normalize(), speed, roll, radius, ColorUtils.rgbIntToFloat(color), lifetime));
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			SLASHES.clear();
			return;
		}
		if (mc.isPaused() || SLASHES.isEmpty()) return;
		SLASHES.removeIf(slash -> ++slash.age >= slash.lifetime);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || SLASHES.isEmpty()) return;

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

	private static void drawAll(PoseStack poseStack, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
		boolean bloom = EffectBloomRenderer.bloomPass;

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
				com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();

		PoseStack modelView = RenderSystem.getModelViewStack();
		modelView.pushPose();
		modelView.setIdentity();
		RenderSystem.applyModelViewMatrix();

		for (Slash slash : SLASHES) {
			float time = slash.age + partialTick;
			float fade = Math.min(1.0F, time / FADE_IN_TICKS)
					* Mth.clamp((slash.lifetime - time) / FADE_OUT_TICKS, 0.0F, 1.0F);
			if (fade <= 0.01F) continue;

			for (int ghost = TRAIL_GHOSTS; ghost >= 0; ghost--) {
				float back = ghost * TRAIL_SPACING * slash.speed;
				float travelled = Math.max(0.0F, time * slash.speed - back);
				double px = slash.x + slash.dir.x * travelled;
				double py = slash.y + slash.dir.y * travelled;
				double pz = slash.z + slash.dir.z * travelled;
				if (camPos.distanceToSqr(px, py, pz) > MAX_RENDER_DISTANCE_SQR) continue;

				float ghostAlpha = ghost == 0 ? 1.0F : 0.32F / ghost;
				float ghostScale = ghost == 0 ? 1.0F : 1.0F - ghost * 0.06F;

				poseStack.pushPose();
				poseStack.translate(px - camPos.x, py - camPos.y, pz - camPos.z);
				poseStack.mulPose(Axis.YP.rotationDegrees(-slash.yaw));
				poseStack.mulPose(Axis.XP.rotationDegrees(slash.pitch));
				poseStack.mulPose(Axis.ZP.rotationDegrees(slash.roll));
				drawCrescent(poseStack.last().pose(), slash, slash.radius * ghostScale, fade * ghostAlpha, bloom);
				poseStack.popPose();
			}
		}

		modelView.popPose();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static void drawCrescent(Matrix4f pose, Slash slash, float radius, float alpha, boolean bloom) {
		float r = slash.color[0];
		float g = slash.color[1];
		float b = slash.color[2];

		float boost = bloom ? 1.5F : 1.0F;
		float glow = radius * GLOW * (bloom ? 2.2F : 1.0F);
		float thickness = radius * THICKNESS;

		strip(pose, radius, radius + glow, 0.0F, 0.0F, r, g, b, 0.85F * alpha * boost, r, g, b, 0.0F);
		strip(pose, radius - thickness * 0.5F, radius, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, alpha * boost, 1.0F, 0.92F, 0.92F, alpha * boost);
		strip(pose, radius - thickness, radius - thickness * 0.5F, 0.0F, 0.0F, r, g, b, 0.55F * alpha * boost, 1.0F, 1.0F, 1.0F, alpha * boost);
		strip(pose, radius - thickness * 1.9F, radius - thickness, 0.0F, 0.0F, r * 0.6F, g * 0.6F, b * 0.6F, 0.0F, r, g, b, 0.55F * alpha * boost);

		float ribbon = radius * RIBBON * (bloom ? 1.6F : 1.0F);
		strip(pose, radius - thickness * 0.4F, radius - thickness * 0.4F, 0.0F, ribbon, 1.0F, 0.9F, 0.9F, 0.75F * alpha * boost, r, g, b, 0.0F);
		strip(pose, radius - thickness * 0.4F, radius - thickness * 0.4F, 0.0F, -ribbon, 1.0F, 0.9F, 0.9F, 0.75F * alpha * boost, r, g, b, 0.0F);
	}

	private static void strip(Matrix4f pose, float innerRadius, float outerRadius, float innerLift, float outerLift,
							  float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2) {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.getBuilder();
		float arcRadius = Math.max(innerRadius, outerRadius);

		buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i <= SEGMENTS; i++) {
			float t = (float) i / SEGMENTS;
			float angle = Mth.lerp(t, -HALF_ARC, HALF_ARC);
			float taper = (float) Math.pow(Math.cos(angle / HALF_ARC * (Math.PI / 2.0D)), 0.85D);

			float sin = Mth.sin(angle);
			float cos = Mth.cos(angle);

			float inner = Mth.lerp(taper, arcRadius, innerRadius);
			float outer = Mth.lerp(taper, arcRadius, outerRadius);

			buffer.vertex(pose, sin * inner, innerLift * taper, cos * inner - arcRadius)
					.color(r1, g1, b1, a1 * taper).endVertex();
			buffer.vertex(pose, sin * outer, outerLift * taper, cos * outer - arcRadius)
					.color(r2, g2, b2, a2 * taper).endVertex();
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
