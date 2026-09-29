package com.dragonminez.server.world.dimension;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZCloudsRenderer;
import com.dragonminez.client.util.ClientStateHelper;
import com.dragonminez.server.world.biome.DemonRealmBiomes;
import com.dragonminez.server.world.gen.DemonRealmGeneration;
import com.dragonminez.server.world.gen.OtherworldGeneration;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

public class CustomSpecialEffects extends DimensionSpecialEffects {
	public static final ResourceLocation NAMEK_EFFECTS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "namek_effects");
	public static final ResourceLocation OTHERWORLD_EFFECTS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "otherworld_effects");
	public static final ResourceLocation HTC_EFFECT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "htc_effects");
	public static final ResourceLocation SACREDKAI_EFFECTS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "sacredkai_effects");
	public static final ResourceLocation DEMON_REALM_EFFECTS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "demon_realm_effects");
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

	public static class DemonRealmEffects extends CustomSpecialEffects {
		private static final ResourceLocation NAMEK_PLANET = skyTexture("namek_planet");
		private static final ResourceLocation PALE_MOON = skyTexture("pale_moon");
		private static final ResourceLocation GLIND_PLANET = skyTexture("glind_planet");
		private static final ResourceLocation MEGA_PLANET = skyTexture("mega_planet");
		private static final ResourceLocation RED_SUN = skyTexture("red_sun");

		private static final Vec3[] HORIZON = {Vec3.fromRGB24(DemonRealmBiomes.FIRST_SKY), Vec3.fromRGB24(DemonRealmBiomes.SECOND_SKY), Vec3.fromRGB24(DemonRealmBiomes.THIRD_SKY)};
		private static final Vec3[] ZENITH = {Vec3.fromRGB24(0x6E0D0D), Vec3.fromRGB24(0x93C93A), Vec3.fromRGB24(0xC24FC6)};
		private static final Vec3[] CLOUD_COLORS = {new Vec3(0.5D, 0.14D, 0.12D), new Vec3(0.97D, 1.0D, 0.88D), new Vec3(1.0D, 0.86D, 0.97D)};
		private static final float[] CLOUD_HEIGHTS = {DemonRealmGeneration.FIRST_CLOUDS, DemonRealmGeneration.SECOND_CLOUDS, DemonRealmGeneration.THIRD_CLOUDS};
		private static final float LIGHT_HAZE_START = 0.65F;

		private static final int LOWER_FADE_START = DemonRealmGeneration.CRUST_A_BOTTOM - DemonRealmGeneration.STALACTITE_DEPTH;
		private static final int LOWER_FADE_END = DemonRealmGeneration.CRUST_A_TOP;
		private static final int UPPER_FADE_START = DemonRealmGeneration.CRUST_B_BOTTOM - DemonRealmGeneration.STALACTITE_DEPTH;
		private static final int UPPER_FADE_END = DemonRealmGeneration.CRUST_B_TOP;

		private static final float SKY_RADIUS = 100.0F;
		private static final int DOME_RINGS = 12;
		private static final int DOME_RINGS_BELOW = 3;
		private static final int DOME_SEGMENTS = 32;

		public DemonRealmEffects() {
			super(192.0F, true, SkyType.NORMAL, false, false);
		}

		public static double[] layerWeights(double y) {
			double lower = smoothStep((y - LOWER_FADE_START) / (LOWER_FADE_END - LOWER_FADE_START));
			double upper = smoothStep((y - UPPER_FADE_START) / (UPPER_FADE_END - UPPER_FADE_START));
			return new double[]{1.0D - lower, lower - upper, upper};
		}

		public static void adjustFog(ViewportEvent.RenderFog event) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || !DemonRealmDimension.DEMON_REALM_KEY.equals(mc.level.dimension())) return;
			if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getType() != FogType.NONE) return;

			double[] weights = layerWeights(event.getCamera().getPosition().y);
			float near = event.getNearPlaneDistance();
			float lightHaze = Math.min(near, event.getFarPlaneDistance() * LIGHT_HAZE_START);
			float target = (float) (weights[0] * near + (weights[1] + weights[2]) * lightHaze);
			if (target >= near - 0.5F) return;

			event.setNearPlaneDistance(target);
			event.setCanceled(true);
		}

		@Override
		public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float daylight) {
			return mix(HORIZON, layerWeights(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y));
		}

		@Override
		public float[] getSunriseColor(float timeOfDay, float partialTicks) {
			return null;
		}

		@Override
		public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
			double[] weights = layerWeights(camera.getPosition().y);

			RenderSystem.depthMask(false);
			RenderSystem.disableCull();
			drawDome(poseStack.last().pose(), mix(HORIZON, weights), mix(ZENITH, weights));

			RenderSystem.enableBlend();
			if (weights[0] > 0.01D) {
				drawBody(poseStack, RED_SUN, 250.0F, 14.0F, 42.0F, (float) weights[0], true);
			}
			if (weights[1] > 0.01D) {
				float alpha = (float) weights[1];
				drawBody(poseStack, PALE_MOON, 110.0F, 14.0F, 32.0F, alpha * 0.55F, false);
				drawBody(poseStack, PALE_MOON, 150.0F, 30.0F, 9.0F, alpha * 0.45F, false);
				drawBody(poseStack, NAMEK_PLANET, 40.0F, 28.0F, 22.0F, alpha * 0.8F, false);
				drawBody(poseStack, GLIND_PLANET, 205.0F, 36.0F, 12.0F, alpha * 0.85F, false);
				drawBody(poseStack, MEGA_PLANET, 300.0F, 20.0F, 7.0F, alpha * 0.8F, false);
			}

			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.enableCull();
			RenderSystem.depthMask(true);
			return true;
		}

		@Override
		public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix) {
			double[] weights = layerWeights(camY);
			int layer = weights[0] >= weights[1] ? 0 : (weights[1] >= weights[2] ? 1 : 2);
			this.cloudRenderer.render(poseStack, projectionMatrix, partialTick, camX, camY, camZ, CLOUD_COLORS[layer], CLOUD_HEIGHTS[layer]);
			return true;
		}

		@Override
		public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ) {
			return true;
		}

		private static void drawDome(Matrix4f pose, Vec3 horizon, Vec3 zenith) {
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			BufferBuilder builder = Tesselator.getInstance().getBuilder();
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			for (int ring = -DOME_RINGS_BELOW; ring < DOME_RINGS; ring++) {
				float lowElevation = ring * 90.0F / DOME_RINGS;
				float highElevation = (ring + 1) * 90.0F / DOME_RINGS;
				Vec3 lowColor = domeColor(lowElevation, horizon, zenith);
				Vec3 highColor = domeColor(highElevation, horizon, zenith);
				for (int segment = 0; segment < DOME_SEGMENTS; segment++) {
					float fromAngle = segment * 360.0F / DOME_SEGMENTS;
					float toAngle = (segment + 1) * 360.0F / DOME_SEGMENTS;
					domeVertex(builder, pose, lowElevation, fromAngle, lowColor);
					domeVertex(builder, pose, lowElevation, toAngle, lowColor);
					domeVertex(builder, pose, highElevation, toAngle, highColor);
					domeVertex(builder, pose, highElevation, fromAngle, highColor);
				}
			}
			BufferUploader.drawWithShader(builder.end());
		}

		private static void domeVertex(BufferBuilder builder, Matrix4f pose, float elevation, float angle, Vec3 color) {
			float e = elevation * Mth.DEG_TO_RAD;
			float a = angle * Mth.DEG_TO_RAD;
			builder.vertex(pose, SKY_RADIUS * Mth.cos(e) * Mth.cos(a), SKY_RADIUS * Mth.sin(e), SKY_RADIUS * Mth.cos(e) * Mth.sin(a))
					.color((float) color.x, (float) color.y, (float) color.z, 1.0F).endVertex();
		}

		private static Vec3 domeColor(float elevation, Vec3 horizon, Vec3 zenith) {
			if (elevation <= 0.0F) return horizon;
			return horizon.lerp(zenith, Math.pow(Mth.sin(elevation * Mth.DEG_TO_RAD), 0.6D));
		}

		private static void drawBody(PoseStack poseStack, ResourceLocation texture, float yaw, float elevation, float size, float alpha, boolean additive) {
			if (additive) {
				RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			} else {
				RenderSystem.defaultBlendFunc();
			}
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, texture);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
			poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - elevation));
			Matrix4f pose = poseStack.last().pose();
			BufferBuilder builder = Tesselator.getInstance().getBuilder();
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			builder.vertex(pose, -size, SKY_RADIUS, -size).uv(0.0F, 0.0F).endVertex();
			builder.vertex(pose, size, SKY_RADIUS, -size).uv(1.0F, 0.0F).endVertex();
			builder.vertex(pose, size, SKY_RADIUS, size).uv(1.0F, 1.0F).endVertex();
			builder.vertex(pose, -size, SKY_RADIUS, size).uv(0.0F, 1.0F).endVertex();
			BufferUploader.drawWithShader(builder.end());
			poseStack.popPose();
		}

		private static Vec3 mix(Vec3[] colors, double[] weights) {
			return colors[0].scale(weights[0]).add(colors[1].scale(weights[1])).add(colors[2].scale(weights[2]));
		}

		private static double smoothStep(double t) {
			double clamped = Mth.clamp(t, 0.0D, 1.0D);
			return clamped * clamped * (3.0D - 2.0D * clamped);
		}

		private static ResourceLocation skyTexture(String name) {
			return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/environment/demon_realm/" + name + ".png");
		}
	}

	public static void registerSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
		event.register(NAMEK_EFFECTS, new NamekEffects());
		event.register(OTHERWORLD_EFFECTS, new OtherWorldEffects());
		event.register(HTC_EFFECT, new HTCEffects());
		event.register(SACREDKAI_EFFECTS, new SacredKaiEffects());
		event.register(DEMON_REALM_EFFECTS, new DemonRealmEffects());
	}

}

