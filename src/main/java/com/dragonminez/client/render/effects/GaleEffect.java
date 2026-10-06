package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.EffectBloomRenderer;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.mojang.blaze3d.platform.GlStateManager;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
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
public final class GaleEffect {

	private static final int MAX_STREAKS = 360;
	private static final float STREAKS_PER_TICK = 8.0F;
	private static final double SPAWN_SPREAD = 22.0D;
	private static final double SPAWN_UPWIND = 10.0D;
	private static final double SPAWN_BELOW = 6.0D;
	private static final double SPAWN_ABOVE = 16.0D;
	private static final double DOWNDRAFT = 0.12D;
	private static final float[] COLOR = {0.92F, 0.96F, 1.0F};

	private static final class Gale {
		final int sourceId;
		Vec3 center;
		final Vec3 dir;
		final float radius;
		final float intensity;
		int remaining;

		private Gale(int sourceId, Vec3 center, Vec3 dir, float radius, float intensity, int remaining) {
			this.sourceId = sourceId;
			this.center = center;
			this.dir = dir;
			this.radius = radius;
			this.intensity = intensity;
			this.remaining = remaining;
		}
	}

	private static final class Streak {
		double x, y, z;
		double prevX, prevY, prevZ;
		final Vec3 velocity;
		final float length;
		final float width;
		final float alpha;
		final int maxLife;
		int life;

		private Streak(double x, double y, double z, Vec3 velocity, float length, float width, float alpha, int maxLife) {
			this.x = this.prevX = x;
			this.y = this.prevY = y;
			this.z = this.prevZ = z;
			this.velocity = velocity;
			this.length = length;
			this.width = width;
			this.alpha = alpha;
			this.maxLife = maxLife;
		}
	}

	private static final List<Gale> GALES = new ArrayList<>();
	private static final List<Streak> STREAKS = new ArrayList<>();
	private static final RandomSource RANDOM = RandomSource.create();
	private static float spawnDebt;

	private GaleEffect() {}

	public static void start(int sourceId, double x, double y, double z, float dirX, float dirZ, float radius, float intensity, int duration) {
		if (duration <= 0) return;
		Vec3 dir = new Vec3(dirX, 0.0D, dirZ);
		if (dir.lengthSqr() < 1.0E-6D) return;
		GALES.removeIf(gale -> gale.sourceId == sourceId);
		GALES.add(new Gale(sourceId, new Vec3(x, y, z), dir.normalize(), radius, intensity, duration));
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			GALES.clear();
			STREAKS.clear();
			return;
		}
		if (mc.isPaused()) return;

		for (Streak streak : STREAKS) {
			streak.prevX = streak.x;
			streak.prevY = streak.y;
			streak.prevZ = streak.z;
			streak.x += streak.velocity.x;
			streak.y += streak.velocity.y;
			streak.z += streak.velocity.z;
		}
		STREAKS.removeIf(streak -> ++streak.life >= streak.maxLife);

		if (GALES.isEmpty()) return;
		Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
		for (Gale gale : GALES) {
			Entity source = mc.level.getEntity(gale.sourceId);
			if (source != null) gale.center = source.position();
			if (camera.distanceToSqr(gale.center) > (gale.radius + 16.0D) * (gale.radius + 16.0D)) continue;
			spawnDebt += STREAKS_PER_TICK * gale.intensity;
			while (spawnDebt >= 1.0F && STREAKS.size() < MAX_STREAKS) {
				spawnDebt -= 1.0F;
				spawnStreak(gale, camera);
			}
		}
		GALES.removeIf(gale -> --gale.remaining <= 0);
	}

	private static void spawnStreak(Gale gale, Vec3 camera) {
		double x = camera.x + (RANDOM.nextDouble() * 2.0D - 1.0D) * SPAWN_SPREAD - gale.dir.x * SPAWN_UPWIND;
		double y = camera.y - SPAWN_BELOW + RANDOM.nextDouble() * (SPAWN_BELOW + SPAWN_ABOVE);
		double z = camera.z + (RANDOM.nextDouble() * 2.0D - 1.0D) * SPAWN_SPREAD - gale.dir.z * SPAWN_UPWIND;
		double speed = 1.3D + RANDOM.nextDouble() * 0.8D;
		Vec3 velocity = gale.dir.scale(speed).add(0.0D, -DOWNDRAFT * gale.intensity, 0.0D);
		float length = 3.0F + RANDOM.nextFloat() * 4.5F;
		float width = 0.04F + RANDOM.nextFloat() * 0.07F;
		float alpha = (0.35F + RANDOM.nextFloat() * 0.4F) * Math.min(1.0F, 0.4F + gale.intensity * 0.6F);
		STREAKS.add(new Streak(x, y, z, velocity, length, width, alpha, 14 + RANDOM.nextInt(10)));
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || STREAKS.isEmpty()) return;

		boolean iris = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		float partialTick = mc.isPaused() ? 0.0F : event.getPartialTick();

		if (!iris) {
			if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
			PlayerEffectQueue.addKiAttack((stack, projection) -> draw(stack, partialTick));
			return;
		}

		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
		mc.getMainRenderTarget().bindWrite(false);
		draw(viewStack(mc.gameRenderer.getMainCamera()), partialTick);
	}

	private static void draw(PoseStack poseStack, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
		boolean bloom = EffectBloomRenderer.bloomPass;
		Matrix4f pose = poseStack.last().pose();

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();

		PoseStack modelView = RenderSystem.getModelViewStack();
		modelView.pushPose();
		modelView.setIdentity();
		RenderSystem.applyModelViewMatrix();

		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (Streak streak : STREAKS) {
			float age = streak.life + partialTick;
			float fade = Math.min(1.0F, age / 3.0F) * Math.min(1.0F, (streak.maxLife - age) / 5.0F);
			if (fade <= 0.0F) continue;
			float alpha = streak.alpha * fade * (bloom ? 0.8F : 1.0F);

			Vec3 head = new Vec3(Mth.lerp(partialTick, streak.prevX, streak.x) - camPos.x,
					Mth.lerp(partialTick, streak.prevY, streak.y) - camPos.y,
					Mth.lerp(partialTick, streak.prevZ, streak.z) - camPos.z);
			Vec3 dir = streak.velocity.normalize();
			Vec3 tail = head.subtract(dir.scale(streak.length));
			Vec3 side = dir.cross(head.scale(-1.0D));
			if (side.lengthSqr() < 1.0E-6D) continue;
			side = side.normalize().scale(streak.width * (bloom ? 2.5F : 1.0F));

			vertex(buffer, pose, tail.subtract(side), 0.0F);
			vertex(buffer, pose, tail.add(side), 0.0F);
			vertex(buffer, pose, head.add(side), alpha);
			vertex(buffer, pose, head.subtract(side), alpha);
		}
		tesselator.end();

		modelView.popPose();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(true);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static void vertex(BufferBuilder buffer, Matrix4f pose, Vec3 point, float alpha) {
		buffer.vertex(pose, (float) point.x, (float) point.y, (float) point.z).color(COLOR[0], COLOR[1], COLOR[2], alpha).endVertex();
	}

	private static PoseStack viewStack(Camera camera) {
		PoseStack stack = new PoseStack();
		stack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
		stack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
		return stack;
	}
}
