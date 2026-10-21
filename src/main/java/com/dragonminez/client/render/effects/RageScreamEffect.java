package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class RageScreamEffect {

	private static final int RAY_COPIES = 10;
	private static final int CYCLE_TICKS = 15;
	private static final float DEFAULT_HITBOX = 1.8f;
	private static final float BASE_MIN_SPREAD = 0.4f;
	private static final float BASE_MAX_SPREAD = 3.6f;
	private static final float BASE_RAY_SCALE = 1.4f;
	private static final float LENGTH_VARIANCE_MIN = 0.7f;
	private static final float LENGTH_VARIANCE_MAX = 1.3f;

	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/skills/rage_scream.png");

	private static final class Entry {
		int ticksRemaining;
		final int totalTicks;

		private Entry(int ticksRemaining, int totalTicks) {
			this.ticksRemaining = ticksRemaining;
			this.totalTicks = totalTicks;
		}

		float getProgress() {
			if (totalTicks <= 0) return 1.0f;
			return 1.0f - (float) ticksRemaining / (float) totalTicks;
		}

		float getCycleProgress() {
			int elapsed = Math.max(0, totalTicks - ticksRemaining);
			return (elapsed % CYCLE_TICKS) / (float) CYCLE_TICKS;
		}
	}

	private static final Map<Integer, Entry> ACTIVE = new ConcurrentHashMap<>();

	private RageScreamEffect() {}

	public static void start(int entityId, int durationTicks) {
		if (durationTicks <= 0) return;
		ACTIVE.put(entityId, new Entry(durationTicks, durationTicks));
	}

	public static java.util.Map<Integer, Float> getActiveProgress() {
		java.util.Map<Integer, Float> out = new java.util.HashMap<>();
		for (var entry : ACTIVE.entrySet()) out.put(entry.getKey(), entry.getValue().getProgress());
		return out;
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;
		if (Minecraft.getInstance().isPaused()) return;
		if (ACTIVE.isEmpty()) return;

		Iterator<Map.Entry<Integer, Entry>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = it.next().getValue();
			entry.ticksRemaining--;
			if (entry.ticksRemaining <= 0) it.remove();
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;
		if (ACTIVE.isEmpty()) return;

		boolean iris = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		RenderLevelStageEvent.Stage targetStage = iris ? RenderLevelStageEvent.Stage.AFTER_LEVEL : RenderLevelStageEvent.Stage.AFTER_WEATHER;
		if (event.getStage() != targetStage) return;

		ShaderInstance shader = DMZShaders.auraShader;
		if (shader == null) return;

		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 camPos = camera.getPosition();
		Matrix4f proj = event.getProjectionMatrix();
		float partialTick = event.getPartialTick();

		PoseStack poseStack = iris ? viewStack(mc) : event.getPoseStack();
		VertexBuffer mesh = AuraMeshFactory.getBillboardQuad();
		RenderType renderType = auraType(TEXTURE);

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		for (var mapEntry : ACTIVE.entrySet()) {
			int entityId = mapEntry.getKey();
			Entity entity = mc.level.getEntity(entityId);
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;

			float cycleProgress = mapEntry.getValue().getCycleProgress();
			float eased = 1.0f - (1.0f - cycleProgress) * (1.0f - cycleProgress) * (1.0f - cycleProgress);
			float alpha = Mth.clamp(1.0f - cycleProgress, 0.0f, 1.0f);
			if (alpha <= 0.001f) continue;

			float hitbox = Math.max(living.getBbWidth(), living.getBbHeight());
			float sizeFactor = hitbox / DEFAULT_HITBOX;

			float minSpread = BASE_MIN_SPREAD * sizeFactor;
			float maxSpread = BASE_MAX_SPREAD * sizeFactor;
			float baseSpread = Mth.lerp(eased, minSpread, maxSpread);
			float baseRayScale = BASE_RAY_SCALE * sizeFactor;

			double lerpX = Mth.lerp(partialTick, living.xo, living.getX());
			double lerpY = Mth.lerp(partialTick, living.yo, living.getY());
			double lerpZ = Mth.lerp(partialTick, living.zo, living.getZ());
			float bbHeight = living.getBbHeight();

			for (int i = 0; i < RAY_COPIES; i++) {
				float baseAngle = 360.0f * i / RAY_COPIES;
				float angleJitter = (pseudoRandomUnit(entityId, i, 1) - 0.5f) * (360.0f / RAY_COPIES) * 0.6f;
				float lengthVariance = Mth.lerp(pseudoRandomUnit(entityId, i, 2), LENGTH_VARIANCE_MIN, LENGTH_VARIANCE_MAX);

				drawBurstLayer(poseStack, camera, camPos, lerpX, lerpY, lerpZ, bbHeight, proj,
						mesh, renderType, shader, baseSpread * lengthVariance, baseRayScale * lengthVariance,
						alpha, baseAngle + angleJitter);
			}
		}

		VertexBuffer.unbind();
		shader.clear();
	}

	private static void drawBurstLayer(PoseStack poseStack, Camera camera, Vec3 camPos, double lerpX, double lerpY, double lerpZ,
										float bbHeight, Matrix4f proj, VertexBuffer mesh, RenderType renderType,
										ShaderInstance shader, float spread, float rayScale, float alpha, float rotationDegrees) {
		poseStack.pushPose();
		poseStack.translate(lerpX - camPos.x, lerpY - camPos.y + bbHeight * 0.5, lerpZ - camPos.z);
		poseStack.mulPose(camera.rotation());
		poseStack.mulPose(Axis.ZP.rotationDegrees(rotationDegrees));
		poseStack.translate(0.0, spread, 0.0);
		poseStack.scale(rayScale, rayScale, rayScale);

		shader.safeGetUniform("speed").set(0.0f);
		shader.safeGetUniform("ProjMat").set(proj);
		shader.safeGetUniform("color1").set(1.0f, 1.0f, 1.0f, 1.0f);
		shader.safeGetUniform("color2").set(1.0f, 1.0f, 1.0f, 0.85f);
		shader.safeGetUniform("color3").set(1.0f, 1.0f, 1.0f, 0.6f);
		shader.safeGetUniform("color4").set(1.0f, 1.0f, 1.0f, 0.35f);
		shader.safeGetUniform("alp1").set(alpha);
		shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());

		customSetup(renderType, TEXTURE, shader);
		mesh.bind();
		mesh.drawWithShader(poseStack.last().pose(), proj, shader);
		customClear(renderType);

		poseStack.popPose();
	}

	private static float pseudoRandomUnit(int entityId, int copyIndex, int salt) {
		int seed = entityId * 73856093 ^ copyIndex * 19349663 ^ salt * 83492791;
		return (Math.abs(seed) % 10000) / 10000.0f;
	}

	private static PoseStack viewStack(Minecraft mc) {
		PoseStack stack = new PoseStack();
		Camera cam = mc.gameRenderer.getMainCamera();
		stack.mulPose(Axis.XP.rotationDegrees(cam.getXRot()));
		stack.mulPose(Axis.YP.rotationDegrees(cam.getYRot() + 180.0F));
		return stack;
	}

	private static RenderType auraType(ResourceLocation texture) {
		return IrisCompat.isShaderPackInUse() ? ModRenderTypes.getCustomAuraCompat(texture) : ModRenderTypes.getCustomAura(texture);
	}

	private static void customSetup(RenderType type, ResourceLocation texture, ShaderInstance shader) {
		if (IrisCompat.isShaderPackInUse()) {
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(GL11.GL_LEQUAL);
			RenderSystem.depthMask(false);
			RenderSystem.disableCull();
			RenderSystem.setShaderTexture(0, texture);
			RenderSystem.setShader(() -> shader);
		} else {
			type.setupRenderState();
		}
	}

	private static void customClear(RenderType type) {
		if (IrisCompat.isShaderPackInUse()) {
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(true);
			RenderSystem.enableCull();
			RenderSystem.disableBlend();
		} else {
			type.clearRenderState();
		}
	}
}
