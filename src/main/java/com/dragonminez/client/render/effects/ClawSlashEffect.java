package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.util.ColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
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
import java.util.Random;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class ClawSlashEffect {

	private static final int MAX_SLASHES = 64;
	private static final int MAX_PER_SPAWN = 12;
	private static final double MAX_RENDER_DISTANCE_SQR = 96.0 * 96.0;
	private static final double CAMERA_PULL = 0.7;
	private static final float ARC_RADIUS = 1.2f;
	private static final float BURST_ROLL_JITTER = 40.0f;
	private static final int BURST_MAX_DELAY = 4;

	private static final class Slash {
		final double x, y, z;
		final float scale;
		final float flip;
		final float roll;
		final float offsetY;
		final float[] color;
		final int lifetime;
		final float seed;
		int age;

		private Slash(double x, double y, double z, float scale, float flip, float roll, float offsetY,
					  float[] color, int lifetime, float seed, int delay) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.scale = scale;
			this.flip = flip;
			this.roll = roll;
			this.offsetY = offsetY;
			this.color = color;
			this.lifetime = lifetime;
			this.seed = seed;
			this.age = -delay;
		}
	}

	private static final List<Slash> SLASHES = new ArrayList<>();
	private static final Random RANDOM = new Random();

	private ClawSlashEffect() {}

	public static void spawn(double x, double y, double z, float scale, int color, int lifetime, int count, boolean burst) {
		if (scale <= 0.0f || lifetime <= 0 || count <= 0) return;
		float[] rgb = ColorUtils.rgbIntToFloat(color);
		int total = Math.min(count, MAX_PER_SPAWN);
		float baseRoll = RANDOM.nextFloat() * 360.0f;

		for (int i = 0; i < total; i++) {
			while (SLASHES.size() >= MAX_SLASHES) SLASHES.remove(0);

			float flip = RANDOM.nextBoolean() ? 1.0f : -1.0f;
			float seed = RANDOM.nextFloat() * 100.0f;
			if (burst) {
				float size = scale * Mth.lerp(RANDOM.nextFloat(), 0.7f, 1.15f);
				float roll = baseRoll + 360.0f * i / total + (RANDOM.nextFloat() - 0.5f) * BURST_ROLL_JITTER;
				float offsetY = ARC_RADIUS * size * Mth.lerp(RANDOM.nextFloat(), 0.55f, 0.95f);
				SLASHES.add(new Slash(x, y, z, size, flip, roll, offsetY, rgb, lifetime, seed, RANDOM.nextInt(BURST_MAX_DELAY)));
			} else {
				float size = scale * Mth.lerp(RANDOM.nextFloat(), 0.85f, 1.15f);
				float roll = RANDOM.nextFloat() * 360.0f;
				SLASHES.add(new Slash(x, y, z, size, flip, roll, 0.0f, rgb, lifetime, seed, i * 2));
			}
		}
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
		if (mc.level == null || mc.player == null) return;
		if (SLASHES.isEmpty()) return;

		boolean iris = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		RenderLevelStageEvent.Stage targetStage = iris ? RenderLevelStageEvent.Stage.AFTER_LEVEL : RenderLevelStageEvent.Stage.AFTER_WEATHER;
		if (event.getStage() != targetStage) return;

		ShaderInstance shader = DMZShaders.clawSlashShader;
		if (shader == null) return;

		if (iris) mc.getMainRenderTarget().bindWrite(false);

		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 camPos = camera.getPosition();
		Matrix4f proj = event.getProjectionMatrix();
		float partialTick = mc.isPaused() ? 0.0f : event.getPartialTick();

		PoseStack poseStack = iris ? viewStack(camera) : event.getPoseStack();
		VertexBuffer mesh = AuraMeshFactory.getBillboardQuad();

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.setShader(() -> shader);

		shader.safeGetUniform("ProjMat").set(proj);

		mesh.bind();
		for (Slash slash : SLASHES) {
			if (slash.age < 0) continue;

			Vec3 toCamera = new Vec3(camPos.x - slash.x, camPos.y - slash.y, camPos.z - slash.z);
			double distSqr = toCamera.lengthSqr();
			if (distSqr > MAX_RENDER_DISTANCE_SQR) continue;

			double dist = Math.sqrt(distSqr);
			Vec3 pull = dist > 1.0E-4 ? toCamera.scale(Math.min(CAMERA_PULL, dist * 0.5) / dist) : Vec3.ZERO;
			float progress = Mth.clamp((slash.age + partialTick) / slash.lifetime, 0.0f, 1.0f);

			poseStack.pushPose();
			poseStack.translate(slash.x + pull.x - camPos.x, slash.y + pull.y - camPos.y, slash.z + pull.z - camPos.z);
			poseStack.mulPose(camera.rotation());
			poseStack.mulPose(Axis.ZP.rotationDegrees(slash.roll));
			poseStack.translate(0.0, slash.offsetY, 0.0);
			poseStack.scale(slash.scale * slash.flip, slash.scale, slash.scale);

			shader.safeGetUniform("progress").set(progress);
			shader.safeGetUniform("seed").set(slash.seed);
			shader.safeGetUniform("slashColor").set(slash.color[0], slash.color[1], slash.color[2]);
			shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());

			mesh.drawWithShader(poseStack.last().pose(), proj, shader);
			poseStack.popPose();
		}
		VertexBuffer.unbind();
		shader.clear();

		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	private static PoseStack viewStack(Camera camera) {
		PoseStack stack = new PoseStack();
		stack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
		stack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
		return stack;
	}
}
