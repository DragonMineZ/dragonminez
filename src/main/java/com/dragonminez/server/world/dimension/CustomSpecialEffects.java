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
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

	protected static final float SKY_RADIUS = 100.0F;

	@FunctionalInterface
	protected interface SkyColor {
		Vec3 at(float elevation, float angle);
	}

	protected record SkySprite(float yaw, float elevation, float size, float red, float green, float blue, float alpha,
							   float drift, float phase) {
		protected static SkySprite of(float yaw, float elevation, float size, int rgb, float alpha, float drift, float phase) {
			return new SkySprite(yaw, elevation, size, (rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F,
					(rgb & 0xFF) / 255.0F, alpha, drift, phase);
		}
	}

	protected static float[] uniformRings(int below, int above) {
		float[] rings = new float[below + above + 1];
		for (int i = 0; i < rings.length; i++) rings[i] = (i - below) * 90.0F / above;
		return rings;
	}

	protected static void drawDome(Matrix4f pose, float[] rings, int segments, SkyColor color) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder builder = Tesselator.getInstance().getBuilder();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int ring = 0; ring < rings.length - 1; ring++) {
			float low = rings[ring];
			float high = rings[ring + 1];
			for (int segment = 0; segment < segments; segment++) {
				float from = segment * 360.0F / segments;
				float to = (segment + 1) * 360.0F / segments;
				domeVertex(builder, pose, low, from, color.at(low, from));
				domeVertex(builder, pose, low, to, color.at(low, to));
				domeVertex(builder, pose, high, to, color.at(high, to));
				domeVertex(builder, pose, high, from, color.at(high, from));
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

	protected static Vec3 gradient(float elevation, float[] stops, Vec3[] colors) {
		if (elevation <= stops[0]) return colors[0];
		for (int i = 1; i < stops.length; i++) {
			if (elevation <= stops[i]) {
				return colors[i - 1].lerp(colors[i], (elevation - stops[i - 1]) / (stops[i] - stops[i - 1]));
			}
		}
		return colors[colors.length - 1];
	}

	protected static void drawSprites(PoseStack poseStack, ResourceLocation texture, List<SkySprite> sprites,
									  float time, float alphaScale, boolean tinted, boolean additive) {
		if (alphaScale <= 0.01F || sprites.isEmpty()) return;
		if (additive) {
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		} else {
			RenderSystem.defaultBlendFunc();
		}
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, texture);
		BufferBuilder builder = Tesselator.getInstance().getBuilder();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		for (SkySprite sprite : sprites) {
			float red = tinted ? sprite.red() : 1.0F;
			float green = tinted ? sprite.green() : 1.0F;
			float blue = tinted ? sprite.blue() : 1.0F;
			float alpha = sprite.alpha() * alphaScale;
			float bob = Mth.sin(time * 0.02F + sprite.phase()) * 0.6F;
			float size = sprite.size();
			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotationDegrees(sprite.yaw() + time * sprite.drift()));
			poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - sprite.elevation() - bob));
			Matrix4f pose = poseStack.last().pose();
			builder.vertex(pose, -size, SKY_RADIUS, -size).uv(0.0F, 0.0F).color(red, green, blue, alpha).endVertex();
			builder.vertex(pose, size, SKY_RADIUS, -size).uv(1.0F, 0.0F).color(red, green, blue, alpha).endVertex();
			builder.vertex(pose, size, SKY_RADIUS, size).uv(1.0F, 1.0F).color(red, green, blue, alpha).endVertex();
			builder.vertex(pose, -size, SKY_RADIUS, size).uv(0.0F, 1.0F).color(red, green, blue, alpha).endVertex();
			poseStack.popPose();
		}
		BufferUploader.drawWithShader(builder.end());
	}

	protected static double smoothStep(double t) {
		double clamped = Mth.clamp(t, 0.0D, 1.0D);
		return clamped * clamped * (3.0D - 2.0D * clamped);
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
		private static final int HELL_FADE_START = OtherworldGeneration.HELL_CEILING - 8;
		private static final int HELL_FADE_END = OtherworldGeneration.LOWER_CLOUD_DECK + 20;
		private static final int TOURNAMENT_FADE_START = OtherworldGeneration.TOURNAMENT_LEVEL + 2;
		private static final int TOURNAMENT_FADE_END = OtherworldGeneration.TOURNAMENT_LEVEL + 46;

		private static final ResourceLocation BUBBLE = skyTexture("bubble");
		private static final ResourceLocation SHINE = skyTexture("shine");
		private static final ResourceLocation GLOW = skyTexture("glow");

		private static final float[] RINGS = {-90.0F, -30.0F, -15.0F, -5.0F, 0.0F, 2.0F, 4.0F, 6.0F, 8.0F, 10.0F, 12.0F, 14.0F,
				16.0F, 19.0F, 23.0F, 28.0F, 35.0F, 45.0F, 60.0F, 75.0F, 90.0F};
		private static final int SEGMENTS = 64;

		private static final float[] HELL_STOPS = {0.0F, 5.0F, 11.0F, 18.0F, 35.0F, 90.0F};
		private static final Vec3[] HELL_COLORS = colors(0xE0552A, 0xF39A3C, 0xE8672E, 0xC8321C, 0x9A1414, 0x5A0A0A);
		private static final Vec3 HELL_STREAK = Vec3.fromRGB24(0xFFD98A);

		private static final float[] CLOUD_STOPS = {0.0F, 6.0F, 15.0F, 30.0F, 55.0F, 90.0F};
		private static final Vec3[] CLOUD_COLORS = colors(0xEBA9C2, 0xF2BFA8, 0xE39BC6, 0xB985C8, 0x8E6CBF, 0x6A55A8);

		private static final float[] TOURNAMENT_STOPS = {-90.0F, -20.0F, 0.0F, 8.0F, 20.0F, 40.0F, 65.0F, 90.0F};
		private static final Vec3[] TOURNAMENT_COLORS = colors(0x3F8F48, 0x8AD66C, 0xA9E67E, 0x86D46A, 0x55B452, 0x2E8A45, 0x17603A, 0x0C402A);

		private static final List<SkySprite> HELL_GLOWS = List.of(
				SkySprite.of(60.0F, 6.0F, 40.0F, 0xFFC060, 0.35F, 0.0F, 0.0F),
				SkySprite.of(250.0F, 4.0F, 35.0F, 0xFFB050, 0.30F, 0.0F, 1.7F));
		private static final List<SkySprite> CLOUD_BUBBLES = bubbles(0xC10D, 34, -4.0F, 24.0F, 1.0F, 3.5F,
				new int[]{0xFFFFFF, 0xFFE3F1, 0xFFF1D6, 0xE9DCFF}, 0.45F, 0.70F);
		private static final List<SkySprite> CLOUD_GLOWS = List.of(
				SkySprite.of(120.0F, 3.0F, 45.0F, 0xFFE0A8, 0.30F, 0.0F, 0.4F),
				SkySprite.of(300.0F, 5.0F, 38.0F, 0xFFD4E4, 0.25F, 0.0F, 2.1F));
		private static final List<SkySprite> TOURNAMENT_BUBBLES = bubbles(0x70E5L, 70, -20.0F, 80.0F, 1.2F, 7.0F,
				new int[]{0xC8FFD0, 0xB8F0FF, 0xFFFFFF, 0xE6FFB0}, 0.55F, 0.85F);
		private static final List<SkySprite> TOURNAMENT_GLOWS = List.of(
				SkySprite.of(200.0F, 10.0F, 45.0F, 0xD8FF9A, 0.55F, 0.0F, 0.0F),
				SkySprite.of(20.0F, 25.0F, 30.0F, 0xE8FFC0, 0.30F, 0.0F, 3.0F));

		public OtherWorldEffects() {
			super(192.0F, false, SkyType.NORMAL, true, false);
		}

		public static double[] layerWeights(double y) {
			double cloud = smoothStep((y - HELL_FADE_START) / (HELL_FADE_END - HELL_FADE_START));
			double tournament = smoothStep((y - TOURNAMENT_FADE_START) / (TOURNAMENT_FADE_END - TOURNAMENT_FADE_START));
			return new double[]{1.0D - cloud, cloud - tournament, tournament};
		}

		private static Vec3 horizonColor(double cameraY) {
			return skyColor(0.0F, 0.0F, layerWeights(cameraY));
		}

		private static Vec3 skyColor(float elevation, float angle, double[] weights) {
			Vec3 color = Vec3.ZERO;
			if (weights[0] > 0.001D) color = color.add(hellSky(elevation, angle).scale(weights[0]));
			if (weights[1] > 0.001D) color = color.add(gradient(elevation, CLOUD_STOPS, CLOUD_COLORS).scale(weights[1]));
			if (weights[2] > 0.001D) color = color.add(gradient(elevation, TOURNAMENT_STOPS, TOURNAMENT_COLORS).scale(weights[2]));
			return color;
		}

		private static Vec3 hellSky(float elevation, float angle) {
			Vec3 base = gradient(elevation, HELL_STOPS, HELL_COLORS);
			float band = (float) Math.exp(-Math.pow((elevation - 9.0F) / 4.0F, 2.0D));
			float a = angle * Mth.DEG_TO_RAD;
			float stroke = 0.5F + 0.5F * Mth.sin(a * 3.0F + 1.3F) * Mth.sin(a * 7.0F + 0.4F);
			return base.lerp(HELL_STREAK, band * (0.25F + 0.5F * stroke));
		}

		public static void adjustFogColor(ViewportEvent.ComputeFogColor event) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || !OtherworldDimension.OTHERWORLD_KEY.equals(mc.level.dimension())) return;
			if (event.getCamera().getFluidInCamera() != FogType.NONE) return;
			if (event.getCamera().getEntity() instanceof LivingEntity living
					&& (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) return;

			Vec3 color = horizonColor(event.getCamera().getPosition().y);
			event.setRed((float) color.x);
			event.setGreen((float) color.y);
			event.setBlue((float) color.z);
		}

		@Override
		public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float daylight) {
			return horizonColor(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y);
		}

		@Override
		public float[] getSunriseColor(float timeOfDay, float partialTicks) {
			return null;
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
			double[] weights = layerWeights(camera.getPosition().y);
			float hell = (float) weights[0];
			float cloud = (float) weights[1];
			float tournament = (float) weights[2];
			float time = ticks + partialTick;

			RenderSystem.depthMask(false);
			RenderSystem.disableCull();
			drawDome(poseStack.last().pose(), RINGS, SEGMENTS, (elevation, angle) -> skyColor(elevation, angle, weights));

			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			drawSprites(poseStack, GLOW, HELL_GLOWS, time, hell, true, true);
			drawSprites(poseStack, GLOW, CLOUD_GLOWS, time, cloud, true, true);
			drawSprites(poseStack, GLOW, TOURNAMENT_GLOWS, time, tournament, true, true);

			drawSprites(poseStack, BUBBLE, CLOUD_BUBBLES, time, cloud, true, false);
			drawSprites(poseStack, SHINE, CLOUD_BUBBLES, time, cloud * 0.6F, false, false);
			drawSprites(poseStack, BUBBLE, TOURNAMENT_BUBBLES, time, tournament, true, false);
			drawSprites(poseStack, SHINE, TOURNAMENT_BUBBLES, time, tournament * 0.8F, false, false);

			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.enableCull();
			RenderSystem.depthMask(true);
			return true;
		}

		private static List<SkySprite> bubbles(long seed, int count, float minElevation, float maxElevation, float minSize, float maxSize,
											   int[] palette, float minAlpha, float maxAlpha) {
			RandomSource random = RandomSource.create(seed);
			List<SkySprite> bubbles = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				float yaw = random.nextFloat() * 360.0F;
				float elevation = Mth.lerp(random.nextFloat(), minElevation, maxElevation);
				float size = Mth.lerp(random.nextFloat() * random.nextFloat(), minSize, maxSize);
				int color = palette[random.nextInt(palette.length)];
				float alpha = Mth.lerp(random.nextFloat(), minAlpha, maxAlpha);
				float drift = (random.nextBoolean() ? 1.0F : -1.0F) * (0.002F + random.nextFloat() * 0.006F);
				bubbles.add(SkySprite.of(yaw, elevation, size, color, alpha, drift, random.nextFloat() * Mth.TWO_PI));
			}
			bubbles.sort(Comparator.comparingDouble(SkySprite::size));
			return List.copyOf(bubbles);
		}

		private static Vec3[] colors(int... rgb) {
			Vec3[] colors = new Vec3[rgb.length];
			for (int i = 0; i < rgb.length; i++) colors[i] = Vec3.fromRGB24(rgb[i]);
			return colors;
		}

		private static ResourceLocation skyTexture(String name) {
			return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/environment/otherworld/" + name + ".png");
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

		private static final float[] DOME_RINGS = uniformRings(3, 12);
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
			Vec3 horizon = mix(HORIZON, weights);
			Vec3 zenith = mix(ZENITH, weights);
			drawDome(poseStack.last().pose(), DOME_RINGS, DOME_SEGMENTS, (elevation, angle) -> domeColor(elevation, horizon, zenith));

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

