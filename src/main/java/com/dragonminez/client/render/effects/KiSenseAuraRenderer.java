package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.shader.SearchGrayscaleManager;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import com.dragonminez.client.systems.kisense.KiSenseState;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class KiSenseAuraRenderer {

	private static final double LOD_DISTANCE = 24.0;

	private KiSenseAuraRenderer() {}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		boolean iris = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		RenderLevelStageEvent.Stage targetStage = iris ? RenderLevelStageEvent.Stage.AFTER_LEVEL : RenderLevelStageEvent.Stage.AFTER_WEATHER;
		if (event.getStage() != targetStage) return;

		if (!KiSenseState.isSearch()) {
			SearchGrayscaleManager.reset();
			return;
		}

		if (iris) {
			mc.getMainRenderTarget().bindWrite(false);
			SearchGrayscaleManager.process(event.getPartialTick(), false);
			renderAuras(mc, event, true);
		} else {
			SearchGrayscaleManager.process(event.getPartialTick());
			renderAuras(mc, event, false);
		}
	}

	private static void renderAuras(Minecraft mc, RenderLevelStageEvent event, boolean iris) {
		StatsData myData = StatsProvider.get(StatsCapability.INSTANCE, mc.player).orElse(null);
		if (myData == null) return;

		ShaderInstance shader = DMZShaders.auraShader;
		if (shader == null) return;

		int myBP = KiSenseScan.getMyBP();
		if (myBP <= 0) myBP = Math.max(1, myData.getBattlePower());

		ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/kakarot_cross.png");

		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 camPos = camera.getPosition();
		Matrix4f proj = event.getProjectionMatrix();
		float partialTick = event.getPartialTick();
		long gameTime = mc.level.getGameTime();

		PoseStack poseStack = iris ? viewStack(mc) : event.getPoseStack();
		VertexBuffer mesh = AuraMeshFactory.getBillboardQuad();
		RenderType renderType = auraType(tex);

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		for (int id : KiSenseScan.getSearchEntities()) {
			Entity entity = mc.level.getEntity(id);
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;

			double dist = mc.player.distanceTo(living);
			boolean lod = dist > LOD_DISTANCE;

			int bp = KiSenseScan.getCachedBP(id);
			double ratio = bp / (double) Math.max(1, myBP);
			float[][] colors = auraColors(living, ratio);
			float[] border = colors[0];
			float[] inner = colors[1];

			double lerpX = Mth.lerp(partialTick, living.xo, living.getX());
			double lerpY = Mth.lerp(partialTick, living.yo, living.getY());
			double lerpZ = Mth.lerp(partialTick, living.zo, living.getZ());
			float bbHeight = living.getBbHeight();

			float hitbox = Math.max(living.getBbWidth(), bbHeight);
			float baseRadius = hitbox * 0.9f + 0.6f;
			float powerMul = (float) Mth.clamp(1.0 + 0.5 * Math.log10(Math.max(1.0, ratio)), 1.0, 3.0);

			float speed = lod ? 0.0f : (gameTime + partialTick) * 0.5f;
			float scale = baseRadius * powerMul;

			drawAuraLayer(poseStack, camera, camPos, lerpX, lerpY, lerpZ, bbHeight, proj, mesh, renderType, tex, shader, border, scale, 0.9f, speed);
			drawAuraLayer(poseStack, camera, camPos, lerpX, lerpY, lerpZ, bbHeight, proj, mesh, renderType, tex, shader, inner, scale * 0.66f, 0.5f, speed * 1.2f);
			drawAuraLayer(poseStack, camera, camPos, lerpX, lerpY, lerpZ, bbHeight, proj, mesh, renderType, tex, shader, inner, scale * 0.78f, 0.4f, speed * 1.45f);
		}

		VertexBuffer.unbind();
		shader.clear();
	}

	private static void drawAuraLayer(PoseStack poseStack, Camera camera, Vec3 camPos, double lerpX, double lerpY, double lerpZ,
									  float bbHeight, Matrix4f proj, VertexBuffer mesh, RenderType renderType, ResourceLocation tex,
									  ShaderInstance shader, float[] color, float scale, float alpha, float speed) {
		poseStack.pushPose();
		poseStack.translate(lerpX - camPos.x, lerpY - camPos.y + bbHeight * 0.5, lerpZ - camPos.z);
		poseStack.mulPose(camera.rotation());
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
		poseStack.scale(scale, scale, scale);

		shader.safeGetUniform("speed").set(speed);
		shader.safeGetUniform("ProjMat").set(proj);
		shader.safeGetUniform("color1").set(color[0] * 1.6f, color[1] * 1.6f, color[2] * 1.6f, 1.0f);
		shader.safeGetUniform("color2").set(color[0] * 1.3f, color[1] * 1.3f, color[2] * 1.3f, 1.0f);
		shader.safeGetUniform("color3").set(color[0], color[1], color[2], 0.85f);
		shader.safeGetUniform("color4").set(color[0] * 0.75f, color[1] * 0.75f, color[2] * 0.75f, 0.65f);
		shader.safeGetUniform("alp1").set(alpha);
		shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());

		customSetup(renderType, tex, shader);
		mesh.bind();
		mesh.drawWithShader(poseStack.last().pose(), proj, shader);
		customClear(renderType);

		poseStack.popPose();
	}

	private static float[][] auraColors(LivingEntity entity, double ratio) {
		if (isPassive(entity)) return new float[][]{{0.3f, 0.5f, 1.0f}, {0.55f, 0.85f, 1.0f}};
		if (ratio > 1.15) return new float[][]{{1.0f, 0.25f, 0.2f}, {1.0f, 0.6f, 0.2f}};
		if (ratio < 0.85) return new float[][]{{0.3f, 0.5f, 1.0f}, {0.55f, 0.85f, 1.0f}};
		return new float[][]{{1.0f, 0.9f, 0.4f}, {1.0f, 1.0f, 0.7f}};
	}

	private static boolean isPassive(LivingEntity entity) {
		return !(entity instanceof Player) && !(entity instanceof Enemy) && !(entity instanceof NeutralMob);
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
