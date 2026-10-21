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
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class ImpactBurstEffect {

	private static final int MAX_BURSTS = 32;
	private static final double MAX_RENDER_DISTANCE_SQR = 96.0 * 96.0;
	private static final double CAMERA_PULL = 0.8;

	private static final class Burst {
		final double x, y, z;
		final Vector3f direction;
		final float scale;
		final float[] color1;
		final float[] color2;
		final boolean twoTone;
		final int lifetime;
		final float seed;
		int age;

		private Burst(double x, double y, double z, Vector3f direction, float scale, float[] color1, float[] color2,
					  boolean twoTone, int lifetime, float seed) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.direction = direction;
			this.scale = scale;
			this.color1 = color1;
			this.color2 = color2;
			this.twoTone = twoTone;
			this.lifetime = lifetime;
			this.seed = seed;
		}
	}

	private static final List<Burst> BURSTS = new ArrayList<>();

	private ImpactBurstEffect() {}

	public static void spawn(double x, double y, double z, float dirX, float dirY, float dirZ, float scale,
							 int color1, int color2, boolean twoTone, int lifetime) {
		if (scale <= 0.0f || lifetime <= 0) return;
		if (BURSTS.size() >= MAX_BURSTS) BURSTS.remove(0);

		Vector3f direction = new Vector3f(dirX, dirY, dirZ);
		if (direction.lengthSquared() > 1.0E-6f) direction.normalize();
		else direction.set(0.0f, 0.0f, 0.0f);

		float seed = (float) (Math.random() * 100.0);
		BURSTS.add(new Burst(x, y, z, direction, scale, ColorUtils.rgbIntToFloat(color1), ColorUtils.rgbIntToFloat(color2),
				twoTone, lifetime, seed));
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			BURSTS.clear();
			return;
		}
		if (mc.isPaused() || BURSTS.isEmpty()) return;
		BURSTS.removeIf(burst -> ++burst.age >= burst.lifetime);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;
		if (BURSTS.isEmpty()) return;

		boolean iris = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		RenderLevelStageEvent.Stage targetStage = iris ? RenderLevelStageEvent.Stage.AFTER_LEVEL : RenderLevelStageEvent.Stage.AFTER_WEATHER;
		if (event.getStage() != targetStage) return;

		ShaderInstance shader = DMZShaders.impactBurstShader;
		if (shader == null) return;

		if (iris) mc.getMainRenderTarget().bindWrite(false);

		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 camPos = camera.getPosition();
		Matrix4f proj = event.getProjectionMatrix();
		float partialTick = mc.isPaused() ? 0.0f : event.getPartialTick();
		Quaternionf inverseCamera = new Quaternionf(camera.rotation()).conjugate();

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
		for (Burst burst : BURSTS) {
			Vec3 toCamera = new Vec3(camPos.x - burst.x, camPos.y - burst.y, camPos.z - burst.z);
			double distSqr = toCamera.lengthSqr();
			if (distSqr > MAX_RENDER_DISTANCE_SQR) continue;

			double dist = Math.sqrt(distSqr);
			Vec3 pull = dist > 1.0E-4 ? toCamera.scale(Math.min(CAMERA_PULL, dist * 0.5) / dist) : Vec3.ZERO;
			float progress = Mth.clamp((burst.age + partialTick) / burst.lifetime, 0.0f, 1.0f);

			Vector3f localDir = inverseCamera.transform(new Vector3f(burst.direction));
			float dirStrength = Mth.clamp((float) Math.sqrt(localDir.x * localDir.x + localDir.y * localDir.y), 0.0f, 1.0f);
			float dirAngle = dirStrength > 1.0E-3f ? (float) Math.atan2(localDir.y, localDir.x) : 0.0f;

			poseStack.pushPose();
			poseStack.translate(burst.x + pull.x - camPos.x, burst.y + pull.y - camPos.y, burst.z + pull.z - camPos.z);
			poseStack.mulPose(camera.rotation());
			poseStack.scale(burst.scale, burst.scale, burst.scale);

			shader.safeGetUniform("progress").set(progress);
			shader.safeGetUniform("seed").set(burst.seed);
			shader.safeGetUniform("dirAngle").set(dirAngle);
			shader.safeGetUniform("dirStrength").set(dirStrength);
			shader.safeGetUniform("twoTone").set(burst.twoTone ? 1.0f : 0.0f);
			shader.safeGetUniform("color1").set(burst.color1[0], burst.color1[1], burst.color1[2]);
			shader.safeGetUniform("color2").set(burst.color2[0], burst.color2[1], burst.color2[2]);
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
