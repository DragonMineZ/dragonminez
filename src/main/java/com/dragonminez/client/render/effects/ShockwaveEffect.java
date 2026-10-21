package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.util.ColorUtils;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
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
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class ShockwaveEffect {

	private static final int MAX_WAVES = 32;
	private static final double MAX_RENDER_DISTANCE_SQR = 96.0 * 96.0;
	private static final double CAMERA_PULL = 0.6;

	private static final class Wave {
		final double x, y, z;
		final float scale;
		final float[] color;
		final int lifetime;
		final float seed;
		int age;

		private Wave(double x, double y, double z, float scale, float[] color, int lifetime, float seed) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.scale = scale;
			this.color = color;
			this.lifetime = lifetime;
			this.seed = seed;
		}
	}

	private static final List<Wave> WAVES = new ArrayList<>();
	private static TextureTarget sceneCopy = null;

	private ShockwaveEffect() {}

	public static void spawn(double x, double y, double z, float scale, int color, int lifetime) {
		if (scale <= 0.0f || lifetime <= 0) return;
		if (WAVES.size() >= MAX_WAVES) WAVES.remove(0);
		float seed = (float) (Math.random() * 100.0);
		WAVES.add(new Wave(x, y, z, scale, ColorUtils.rgbIntToFloat(color), lifetime, seed));
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			WAVES.clear();
			return;
		}
		if (mc.isPaused() || WAVES.isEmpty()) return;
		WAVES.removeIf(wave -> ++wave.age >= wave.lifetime);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;
		if (WAVES.isEmpty()) return;

		boolean iris = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		RenderLevelStageEvent.Stage targetStage = iris ? RenderLevelStageEvent.Stage.AFTER_LEVEL : RenderLevelStageEvent.Stage.AFTER_WEATHER;
		if (event.getStage() != targetStage) return;

		ShaderInstance shader = DMZShaders.shockwaveShader;
		if (shader == null) return;

		RenderTarget main = mc.getMainRenderTarget();
		if (iris) main.bindWrite(false);

		boolean distort = captureScene(main);

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

		shader.setSampler("Scene", sceneCopy);
		shader.safeGetUniform("ProjMat").set(proj);
		shader.safeGetUniform("distort").set(distort ? 1.0f : 0.0f);
		shader.safeGetUniform("screenSize").set((float) main.width, (float) main.height);

		mesh.bind();
		for (Wave wave : WAVES) {
			Vec3 toCamera = new Vec3(camPos.x - wave.x, camPos.y - wave.y, camPos.z - wave.z);
			double distSqr = toCamera.lengthSqr();
			if (distSqr > MAX_RENDER_DISTANCE_SQR) continue;

			double dist = Math.sqrt(distSqr);
			Vec3 pull = dist > 1.0E-4 ? toCamera.scale(Math.min(CAMERA_PULL, dist * 0.5) / dist) : Vec3.ZERO;
			float progress = Mth.clamp((wave.age + partialTick) / wave.lifetime, 0.0f, 1.0f);

			poseStack.pushPose();
			poseStack.translate(wave.x + pull.x - camPos.x, wave.y + pull.y - camPos.y, wave.z + pull.z - camPos.z);
			poseStack.mulPose(camera.rotation());
			poseStack.scale(wave.scale, wave.scale, wave.scale);

			shader.safeGetUniform("progress").set(progress);
			shader.safeGetUniform("seed").set(wave.seed);
			shader.safeGetUniform("waveColor").set(wave.color[0], wave.color[1], wave.color[2]);
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

	private static boolean captureScene(RenderTarget main) {
		int bound = GlStateManager.getBoundFramebuffer();

		if (sceneCopy == null || sceneCopy.width != main.width || sceneCopy.height != main.height) {
			if (sceneCopy == null) sceneCopy = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
			else sceneCopy.resize(main.width, main.height, Minecraft.ON_OSX);
			sceneCopy.setFilterMode(GL11.GL_LINEAR);
			GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, bound);
			GlStateManager._viewport(0, 0, main.viewWidth, main.viewHeight);
		}

		if (bound != main.frameBufferId) return false;

		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sceneCopy.frameBufferId);
		GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
				GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, bound);
		return true;
	}

	private static PoseStack viewStack(Camera camera) {
		PoseStack stack = new PoseStack();
		stack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
		stack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
		return stack;
	}
}
