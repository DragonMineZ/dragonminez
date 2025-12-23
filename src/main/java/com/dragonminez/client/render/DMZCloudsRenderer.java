package com.dragonminez.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class DMZCloudsRenderer {
	private static final ResourceLocation CLOUDS_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");

	private VertexBuffer cloudBuffer;
	private int prevCloudX = Integer.MIN_VALUE;
	private int prevCloudY = Integer.MIN_VALUE;
	private int prevCloudZ = Integer.MIN_VALUE;
	private Vec3 prevCloudColor = Vec3.ZERO;
	private CloudStatus prevCloudsType;
	private boolean generateClouds = true;

	public void render(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, double camX, double camY, double camZ, Vec3 customColor) {
		Minecraft mc = Minecraft.getInstance();
		float cloudHeight = mc.level.effects().getCloudHeight();

		if (Float.isNaN(cloudHeight)) {
			return;
		}

		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.blendFuncSeparate(
				GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
		);
		RenderSystem.depthMask(true);

		double time = (double) ((float) mc.level.getGameTime() + partialTick) * 0.03F;
		double viewX = (camX + time) / 12.0D;
		double viewY = (double) (cloudHeight - (float) camY + 0.33F);
		double viewZ = camZ / 12.0D + 0.33D;

		viewX -= (double) (Mth.floor(viewX / 2048.0D) * 2048);
		viewZ -= (double) (Mth.floor(viewZ / 2048.0D) * 2048);

		float offsetX = (float) (viewX - (double) Mth.floor(viewX));
		float offsetY = (float) (viewY / 4.0D - (double) Mth.floor(viewY / 4.0D)) * 4.0F;
		float offsetZ = (float) (viewZ - (double) Mth.floor(viewZ));

		int cellX = (int) Math.floor(viewX);
		int cellY = (int) Math.floor(viewY / 4.0D);
		int cellZ = (int) Math.floor(viewZ);
		CloudStatus currentStatus = mc.options.getCloudsType();

		if (cellX != this.prevCloudX || cellY != this.prevCloudY || cellZ != this.prevCloudZ ||
				currentStatus != this.prevCloudsType || this.prevCloudColor.distanceToSqr(customColor) > 2.0E-4D) {

			this.prevCloudX = cellX;
			this.prevCloudY = cellY;
			this.prevCloudZ = cellZ;
			this.prevCloudColor = customColor;
			this.prevCloudsType = currentStatus;
			this.generateClouds = true;
		}

		if (this.generateClouds) {
			this.generateClouds = false;
			BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();

			if (this.cloudBuffer != null) {
				this.cloudBuffer.close();
			}

			this.cloudBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
			BufferBuilder.RenderedBuffer renderedBuffer = this.buildClouds(bufferbuilder, viewX, viewY, viewZ, customColor);
			this.cloudBuffer.bind();
			this.cloudBuffer.upload(renderedBuffer);
			VertexBuffer.unbind();
		}

		RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
		RenderSystem.setShaderTexture(0, CLOUDS_LOCATION);

		poseStack.pushPose();
		poseStack.scale(12.0F, 1.0F, 12.0F);
		poseStack.translate(-offsetX, offsetY, -offsetZ);

		if (this.cloudBuffer != null) {
			this.cloudBuffer.bind();
			int passes = this.prevCloudsType == CloudStatus.FANCY ? 0 : 1;

			for (int pass = passes; pass < 2; ++pass) {
				if (pass == 0) {
					RenderSystem.colorMask(false, false, false, false);
				} else {
					RenderSystem.colorMask(true, true, true, true);
				}

				this.cloudBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
			}

			VertexBuffer.unbind();
		}

		poseStack.popPose();
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.defaultBlendFunc();
	}

	private BufferBuilder.RenderedBuffer buildClouds(BufferBuilder builder, double x, double y, double z, Vec3 color) {
		float r = (float) color.x;
		float g = (float) color.y;
		float b = (float) color.z;

		float sideR = r * 0.9F;
		float sideG = g * 0.9F;
		float sideB = b * 0.9F;
		float bottomR = r * 0.7F;
		float bottomG = g * 0.7F;
		float bottomB = b * 0.7F;
		float shadowR = r * 0.6F;
		float shadowG = g * 0.6F;
		float shadowB = b * 0.6F;

		RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);

		float floorY = (float) Math.floor(y / 4.0D) * 4.0F;

		if (this.prevCloudsType == CloudStatus.FANCY) {
			for (int k = -3; k <= 4; ++k) {
				for (int l = -3; l <= 4; ++l) {
					float f18 = (float) (k * 8);
					float f19 = (float) (l * 8);

					if (floorY > -5.0F) {
						builder.vertex(f18 + 0.0F, floorY + 0.0F, f19 + 8.0F).uv((f18 + 0.0F) / 256.0F + (float) Mth.floor(x) / 256.0F, (f19 + 8.0F) / 256.0F + (float) Mth.floor(z) / 256.0F).color(bottomR, bottomG, bottomB, 0.6F).normal(0.0F, -1.0F, 0.0F).endVertex();
						builder.vertex(f18 + 8.0F, floorY + 0.0F, f19 + 8.0F).uv((f18 + 8.0F) / 256.0F + (float) Mth.floor(x) / 256.0F, (f19 + 8.0F) / 256.0F + (float) Mth.floor(z) / 256.0F).color(bottomR, bottomG, bottomB, 0.6F).normal(0.0F, -1.0F, 0.0F).endVertex();
						builder.vertex(f18 + 8.0F, floorY + 0.0F, f19 + 0.0F).uv((f18 + 8.0F) / 256.0F + (float) Mth.floor(x) / 256.0F, (f19 + 0.0F) / 256.0F + (float) Mth.floor(z) / 256.0F).color(bottomR, bottomG, bottomB, 0.6F).normal(0.0F, -1.0F, 0.0F).endVertex();
						builder.vertex(f18 + 0.0F, floorY + 0.0F, f19 + 0.0F).uv((f18 + 0.0F) / 256.0F + (float) Mth.floor(x) / 256.0F, (f19 + 0.0F) / 256.0F + (float) Mth.floor(z) / 256.0F).color(bottomR, bottomG, bottomB, 0.6F).normal(0.0F, -1.0F, 0.0F).endVertex();
					}

					if (floorY <= 5.0F) {
						builder.vertex(f18 + 0.0F, floorY + 4.0F - 9.765625E-4F, f19 + 8.0F).uv((f18 + 0.0F) / 256.0F + (float) Mth.floor(x) / 256.0F, (f19 + 8.0F) / 256.0F + (float) Mth.floor(z) / 256.0F).color(r, g, b, 0.6F).normal(0.0F, 1.0F, 0.0F).endVertex();
						builder.vertex(f18 + 8.0F, floorY + 4.0F - 9.765625E-4F, f19 + 8.0F).uv((f18 + 8.0F) / 256.0F + (float) Mth.floor(x) / 256.0F, (f19 + 8.0F) / 256.0F + (float) Mth.floor(z) / 256.0F).color(r, g, b, 0.6F).normal(0.0F, 1.0F, 0.0F).endVertex();
						builder.vertex(f18 + 8.0F, floorY + 4.0F - 9.765625E-4F, f19 + 0.0F).uv((f18 + 8.0F) / 256.0F + (float) Mth.floor(x) / 256.0F, (f19 + 0.0F) / 256.0F + (float) Mth.floor(z) / 256.0F).color(r, g, b, 0.6F).normal(0.0F, 1.0F, 0.0F).endVertex();
						builder.vertex(f18 + 0.0F, floorY + 4.0F - 9.765625E-4F, f19 + 0.0F).uv((f18 + 0.0F) / 256.0F + (float) Mth.floor(x) / 256.0F, (f19 + 0.0F) / 256.0F + (float) Mth.floor(z) / 256.0F).color(r, g, b, 0.6F).normal(0.0F, 1.0F, 0.0F).endVertex();
					}

					if (k > -1) {
						for(int i1 = 0; i1 < 8; ++i1) {
							builder.vertex(f18 + (float)i1 + 0.0F, floorY + 0.0F, f19 + 8.0F).uv((f18 + (float)i1 + 0.5F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + 8.0F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(sideR, sideG, sideB, 0.6F).normal(-1.0F, 0.0F, 0.0F).endVertex();
							builder.vertex(f18 + (float)i1 + 0.0F, floorY + 4.0F, f19 + 8.0F).uv((f18 + (float)i1 + 0.5F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + 8.0F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(sideR, sideG, sideB, 0.6F).normal(-1.0F, 0.0F, 0.0F).endVertex();
							builder.vertex(f18 + (float)i1 + 0.0F, floorY + 4.0F, f19 + 0.0F).uv((f18 + (float)i1 + 0.5F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + 0.0F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(sideR, sideG, sideB, 0.6F).normal(-1.0F, 0.0F, 0.0F).endVertex();
							builder.vertex(f18 + (float)i1 + 0.0F, floorY + 0.0F, f19 + 0.0F).uv((f18 + (float)i1 + 0.5F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + 0.0F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(sideR, sideG, sideB, 0.6F).normal(-1.0F, 0.0F, 0.0F).endVertex();
						}
					}
					if (k <= 1) {
						for(int j2 = 0; j2 < 8; ++j2) {
							builder.vertex(f18 + (float)j2 + 1.0F - 9.765625E-4F, floorY + 0.0F, f19 + 8.0F).uv((f18 + (float)j2 + 0.5F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + 8.0F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(sideR, sideG, sideB, 0.6F).normal(1.0F, 0.0F, 0.0F).endVertex();
							builder.vertex(f18 + (float)j2 + 1.0F - 9.765625E-4F, floorY + 4.0F, f19 + 8.0F).uv((f18 + (float)j2 + 0.5F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + 8.0F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(sideR, sideG, sideB, 0.6F).normal(1.0F, 0.0F, 0.0F).endVertex();
							builder.vertex(f18 + (float)j2 + 1.0F - 9.765625E-4F, floorY + 4.0F, f19 + 0.0F).uv((f18 + (float)j2 + 0.5F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + 0.0F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(sideR, sideG, sideB, 0.6F).normal(1.0F, 0.0F, 0.0F).endVertex();
							builder.vertex(f18 + (float)j2 + 1.0F - 9.765625E-4F, floorY + 0.0F, f19 + 0.0F).uv((f18 + (float)j2 + 0.5F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + 0.0F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(sideR, sideG, sideB, 0.6F).normal(1.0F, 0.0F, 0.0F).endVertex();
						}
					}
					if (l > -1) {
						for(int k2 = 0; k2 < 8; ++k2) {
							builder.vertex(f18 + 0.0F, floorY + 4.0F, f19 + (float)k2 + 0.0F).uv((f18 + 0.0F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + (float)k2 + 0.5F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(shadowR, shadowG, shadowB, 0.6F).normal(0.0F, 0.0F, -1.0F).endVertex();
							builder.vertex(f18 + 8.0F, floorY + 4.0F, f19 + (float)k2 + 0.0F).uv((f18 + 8.0F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + (float)k2 + 0.5F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(shadowR, shadowG, shadowB, 0.6F).normal(0.0F, 0.0F, -1.0F).endVertex();
							builder.vertex(f18 + 8.0F, floorY + 0.0F, f19 + (float)k2 + 0.0F).uv((f18 + 8.0F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + (float)k2 + 0.5F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(shadowR, shadowG, shadowB, 0.6F).normal(0.0F, 0.0F, -1.0F).endVertex();
							builder.vertex(f18 + 0.0F, floorY + 0.0F, f19 + (float)k2 + 0.0F).uv((f18 + 0.0F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + (float)k2 + 0.5F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(shadowR, shadowG, shadowB, 0.6F).normal(0.0F, 0.0F, -1.0F).endVertex();
						}
					}
					if (l <= 1) {
						for(int l2 = 0; l2 < 8; ++l2) {
							builder.vertex(f18 + 0.0F, floorY + 4.0F, f19 + (float)l2 + 1.0F - 9.765625E-4F).uv((f18 + 0.0F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + (float)l2 + 0.5F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(shadowR, shadowG, shadowB, 0.6F).normal(0.0F, 0.0F, 1.0F).endVertex();
							builder.vertex(f18 + 8.0F, floorY + 4.0F, f19 + (float)l2 + 1.0F - 9.765625E-4F).uv((f18 + 8.0F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + (float)l2 + 0.5F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(shadowR, shadowG, shadowB, 0.6F).normal(0.0F, 0.0F, 1.0F).endVertex();
							builder.vertex(f18 + 8.0F, floorY + 0.0F, f19 + (float)l2 + 1.0F - 9.765625E-4F).uv((f18 + 8.0F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + (float)l2 + 0.5F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(shadowR, shadowG, shadowB, 0.6F).normal(0.0F, 0.0F, 1.0F).endVertex();
							builder.vertex(f18 + 0.0F, floorY + 0.0F, f19 + (float)l2 + 1.0F - 9.765625E-4F).uv((f18 + 0.0F) / 256.0F + (float)Mth.floor(x) / 256.0F, (f19 + (float)l2 + 0.5F) / 256.0F + (float)Mth.floor(z) / 256.0F).color(shadowR, shadowG, shadowB, 0.6F).normal(0.0F, 0.0F, 1.0F).endVertex();
						}
					}
				}
			}
		} else {
			for (int l1 = -32; l1 < 32; l1 += 32) {
				for (int i2 = -32; i2 < 32; i2 += 32) {
					builder.vertex(l1 + 0, floorY, i2 + 32).uv((l1 + 0) / 256.0F + (float) Mth.floor(x) / 256.0F, (i2 + 32) / 256.0F + (float) Mth.floor(z) / 256.0F).color(r, g, b, 0.6F).normal(0.0F, -1.0F, 0.0F).endVertex();
					builder.vertex(l1 + 32, floorY, i2 + 32).uv((l1 + 32) / 256.0F + (float) Mth.floor(x) / 256.0F, (i2 + 32) / 256.0F + (float) Mth.floor(z) / 256.0F).color(r, g, b, 0.6F).normal(0.0F, -1.0F, 0.0F).endVertex();
					builder.vertex(l1 + 32, floorY, i2 + 0).uv((l1 + 32) / 256.0F + (float) Mth.floor(x) / 256.0F, (i2 + 0) / 256.0F + (float) Mth.floor(z) / 256.0F).color(r, g, b, 0.6F).normal(0.0F, -1.0F, 0.0F).endVertex();
					builder.vertex(l1 + 0, floorY, i2 + 0).uv((l1 + 0) / 256.0F + (float) Mth.floor(x) / 256.0F, (i2 + 0) / 256.0F + (float) Mth.floor(z) / 256.0F).color(r, g, b, 0.6F).normal(0.0F, -1.0F, 0.0F).endVertex();
				}
			}
		}

		return builder.end();
	}

	public void close() {
		if (this.cloudBuffer != null) {
			this.cloudBuffer.close();
			this.cloudBuffer = null;
		}
	}
}