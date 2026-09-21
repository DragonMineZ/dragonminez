package com.dragonminez.server.world.dimension;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZCloudsRenderer;
import com.dragonminez.client.util.ClientStateHelper;
import com.dragonminez.server.world.gen.OtherworldGeneration;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.joml.Matrix4f;

public class CustomSpecialEffects extends DimensionSpecialEffects {
	public static final ResourceLocation NAMEK_EFFECTS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "namek_effects");
	public static final ResourceLocation OTHERWORLD_EFFECTS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "otherworld_effects");
	public static final ResourceLocation HTC_EFFECT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "htc_effects");
	public static final ResourceLocation SACREDKAI_EFFECTS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "sacredkai_effects");
	final DMZCloudsRenderer cloudRenderer;

	public CustomSpecialEffects(float cloudLevel, boolean hasGround, SkyType skyType, boolean forceBrightLightMap, boolean constantAmbientLight) {
		super(cloudLevel, hasGround, skyType, forceBrightLightMap, constantAmbientLight);
		this.cloudRenderer = new DMZCloudsRenderer();
	}

	@Override
	public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float daylight) {
		return biomeFogColor.multiply((double)(daylight * 0.94F + 0.06F), (double)(daylight * 0.94F + 0.06F), (double)(daylight * 0.91F + 0.09F));
	}

	@Override
	public boolean isFoggyAt(int x, int y) {
		return false;
	}

	@Override
	public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix) {
		return false;
	}

	@Override
	public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
		return false;
	}

	@Override
	public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ) {
		return false;
	}

	public static class NamekEffects extends CustomSpecialEffects {
		public NamekEffects() {
			super(192.0F, true, SkyType.NORMAL, false, false);
		}

		@Override
		public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float daylight) {
			if (ClientStateHelper.isPorungaActive) {
				return new Vec3(0.02, 0.02, 0.02);
			}
			return biomeFogColor.multiply((double)(daylight * 0.94F + 0.06F), (double)(daylight * 0.94F + 0.06F), (double)(daylight * 0.91F + 0.09F));
		}

		@Override
		public boolean isFoggyAt(int x, int y) {
			if (ClientStateHelper.isPorungaActive) {
				return true;
			}
			return false;
		}

		@Override
		public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
			if (!ClientStateHelper.isPorungaActive) {
				return false;
			}

			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.depthMask(false);

			RenderSystem.setShader(GameRenderer::getPositionColorShader);

			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder bufferbuilder = tesselator.getBuilder();

			float r = 0.05f;
			float g = 0.05f;
			float b = 0.05f;
			float a = 1.0f;

			for (int i = 0; i < 6; ++i) {
				poseStack.pushPose();
				if (i == 1) poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
				if (i == 2) poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
				if (i == 3) poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
				if (i == 4) poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
				if (i == 5) poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));

				Matrix4f matrix4f = poseStack.last().pose();

				bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
				bufferbuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).color(r, g, b, a).endVertex();
				bufferbuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).color(r, g, b, a).endVertex();
				bufferbuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).color(r, g, b, a).endVertex();
				bufferbuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).color(r, g, b, a).endVertex();
				tesselator.end();

				poseStack.popPose();
			}

			RenderSystem.depthMask(true);
			RenderSystem.disableBlend();

			return true;
		}

		@Override
		public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix) {
			Vec3 namekGreen = new Vec3(0.659D, 0.922D, 0.443D);
			this.cloudRenderer.render(poseStack, projectionMatrix, partialTick, camX, camY, camZ, namekGreen);
			return true;
		}

		@Override
		public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ) {
			return true;
		}
	}

	public static class HTCEffects extends CustomSpecialEffects {
		public HTCEffects() {
			super(192.0F, true, SkyType.NORMAL, false, false);
		}

		@Override
		public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float daylight) {
			return biomeFogColor.multiply((double)(daylight * 0.94F + 0.06F), (double)(daylight * 0.94F + 0.06F), (double)(daylight * 0.91F + 0.09F));
		}

		@Override
		public boolean isFoggyAt(int x, int y) {
			return Math.abs(x) > 64;
		}

		@Override
		public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix) {
			return true;
		}

		@Override
		public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
			return true;
		}

		@Override
		public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ) {
			return true;
		}
	}

	public static class OtherWorldEffects extends CustomSpecialEffects {
		private static final Vec3 HELL_SKY = new Vec3(0.851D, 0.290D, 0.106D);
		private static final Vec3 CLOUD_SKY = new Vec3(0.808D, 0.494D, 0.741D);
		private static final Vec3 TOURNAMENT_SKY = new Vec3(0.435D, 0.824D, 0.769D);
		private static final int HELL_FADE_START = OtherworldGeneration.HELL_CEILING - 8;
		private static final int HELL_FADE_END = OtherworldGeneration.LOWER_CLOUD_DECK + 20;
		private static final int TOURNAMENT_FADE_START = OtherworldGeneration.TOURNAMENT_LEVEL + 2;
		private static final int TOURNAMENT_FADE_END = OtherworldGeneration.TOURNAMENT_LEVEL + 46;

		public OtherWorldEffects() {
			super(192.0F, false, SkyType.NORMAL, true, false);
		}

		@Override
		public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float daylight) {
			Vec3 layerColor = layerColor(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y);
			return layerColor.multiply((double)(daylight * 0.94F + 0.06F), (double)(daylight * 0.94F + 0.06F), (double)(daylight * 0.91F + 0.09F));
		}

		private static Vec3 layerColor(double cameraY) {
			if (cameraY <= HELL_FADE_START) {
				return HELL_SKY;
			}
			if (cameraY < HELL_FADE_END) {
				return blend(HELL_SKY, CLOUD_SKY, (cameraY - HELL_FADE_START) / (HELL_FADE_END - HELL_FADE_START));
			}
			if (cameraY <= TOURNAMENT_FADE_START) {
				return CLOUD_SKY;
			}
			if (cameraY < TOURNAMENT_FADE_END) {
				return blend(CLOUD_SKY, TOURNAMENT_SKY, (cameraY - TOURNAMENT_FADE_START) / (TOURNAMENT_FADE_END - TOURNAMENT_FADE_START));
			}
			return TOURNAMENT_SKY;
		}

		private static Vec3 blend(Vec3 from, Vec3 to, double progress) {
			double t = Mth.clamp(progress, 0.0D, 1.0D);
			return new Vec3(Mth.lerp(t, from.x, to.x), Mth.lerp(t, from.y, to.y), Mth.lerp(t, from.z, to.z));
		}

		@Override
		public boolean isFoggyAt(int x, int y) {
			return false;
		}

		@Override
		public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix) {
			return true;
		}

		@Override
		public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
			return true;
		}

		@Override
		public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ) {
			return true;
		}
	}

	public static class SacredKaiEffects extends CustomSpecialEffects {
		public SacredKaiEffects() {
			super(192.0F, true, SkyType.NORMAL, false, false);
		}
	}

	public static void registerSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
		event.register(NAMEK_EFFECTS, new NamekEffects());
		event.register(OTHERWORLD_EFFECTS, new OtherWorldEffects());
		event.register(HTC_EFFECT, new HTCEffects());
		event.register(SACREDKAI_EFFECTS, new SacredKaiEffects());
	}

}

