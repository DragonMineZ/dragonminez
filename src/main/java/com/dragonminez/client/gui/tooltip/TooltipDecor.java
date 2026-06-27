package com.dragonminez.client.gui.tooltip;

import com.dragonminez.Reference;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class TooltipDecor {
	public static final ResourceLocation DEFAULT_BORDERS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/tooltip_borders.png");

	public static int currentBorderStart = 0;
	public static int currentBorderEnd = 0;
	public static int currentBackgroundStart = 0;
	public static int currentBackgroundEnd = 0;

	public static boolean hasSpecialBorder = false;

	public static boolean forceCustomBorder = false;
	public static int forcedColor = 0xFFFFFF;
	public static boolean hasItemBox = false;

	public static int lastTooltipX = 0;
	public static int lastTooltipY = 0;
	public static int lastTooltipW = 0;
	public static int lastTooltipH = 0;

	private static float shineTimer = 2.5f;
	public static float rotationTimer = 0.0f;

	public static void updateTimer(float deltaTime) {
		if (shineTimer > 0.0f) shineTimer -= deltaTime;
		rotationTimer += deltaTime;
		if (rotationTimer > 100.0f) rotationTimer -= 100.0f;
	}

	public static void resetTimer() {
		shineTimer = 2.5f;
	}

	public static void drawShadow(PoseStack poseStack, int x, int y, int width, int height) {
		int shadowColor = 0x44000000;
		poseStack.pushPose();
		Matrix4f matrix = poseStack.last().pose();
		drawGradientRect(matrix, 390, x - 1, y + height + 4, x + width + 4, y + height + 5, shadowColor, shadowColor);
		drawGradientRect(matrix, 390, x + width + 4, y - 1, x + width + 5, y + height + 5, shadowColor, shadowColor);
		drawGradientRect(matrix, 390, x + width + 3, y + height + 3, x + width + 4, y + height + 4, shadowColor, shadowColor);
		drawGradientRect(matrix, 390, x, y + height + 5, x + width + 5, y + height + 6, shadowColor, shadowColor);
		drawGradientRect(matrix, 390, x + width + 5, y, x + width + 6, y + height + 5, shadowColor, shadowColor);
		poseStack.popPose();
	}

	public static void drawSeparator(PoseStack poseStack, int x, int y, int width, int color) {
		poseStack.pushPose();
		Matrix4f matrix = poseStack.last().pose();
		drawGradientRectHorizontal(matrix, 402, x, y, x + width / 2, y + 1, color & 0xFFFFFF, color);
		drawGradientRectHorizontal(matrix, 402, x + width / 2, y, x + width, y + 1, color, color & 0xFFFFFF);
		poseStack.popPose();
	}

	public static void drawBorder(PoseStack poseStack, int x, int y, int width, int height) {
		if (!hasSpecialBorder) return;

		poseStack.pushPose();
		Matrix4f matrix = poseStack.last().pose();

		if (shineTimer >= 0.5f && shineTimer <= 2.0f) {
			float interval = Mth.clamp(shineTimer - 0.5f, 0.0f, 1.0f);
			int alpha = (int)(0x99 * interval) << 24;
			int hMin = x - 3;
			int hMax = x + width + 3;
			int hInterval = (int)Mth.lerp(interval * interval, hMax, hMin);
			drawGradientRectHorizontal(matrix, 402, Math.max(hInterval - 36, hMin), y - 3, Math.min(hInterval, hMax), y - 3 + 1, 0x00FFFFFF, 0x00FFFFFF | alpha);
			drawGradientRectHorizontal(matrix, 402, Math.max(hInterval, hMin), y - 3, Math.min(hInterval + 36, hMax), y - 3 + 1, 0x00FFFFFF | alpha, 0x00FFFFFF);
		}

		if (shineTimer <= 1.0f) {
			float interval = Mth.clamp(shineTimer, 0.0f, 1.0f);
			int alpha = (int)(0x55 * interval) << 24;
			int vMin = y - 3 + 1;
			int vMax = y + height + 3 - 1;
			int vInterval = (int)Mth.lerp(interval * interval, vMax, vMin);
			drawGradientRect(matrix, 402, x - 3, Math.max(vInterval - 12, vMin), x - 3 + 1, Math.min(vInterval, vMax), 0x00FFFFFF, 0x00FFFFFF | alpha);
			drawGradientRect(matrix, 402, x - 3, Math.max(vInterval, vMin), x - 3 + 1, Math.min(vInterval + 12, vMax), 0x00FFFFFF | alpha, 0x00FFFFFF);
		}
		poseStack.popPose();

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.setShaderTexture(0, DEFAULT_BORDERS);

		poseStack.pushPose();
		poseStack.translate(0, 0, 410.0);

		int texW = 128;
		int texH = 128;
		int u = 0;
		int v = 0;

		blit(poseStack, x - 6, y - 6, 8, 8, u, v, 8, 8, texW, texH);
		blit(poseStack, x + width - 2, y - 6, 8, 8, 56 + u, v, 8, 8, texW, texH);
		blit(poseStack, x - 6, y + height - 2, 8, 8, u, v + 8, 8, 8, texW, texH);
		blit(poseStack, x + width - 2, y + height - 2, 8, 8, 56 + u, v + 8, 8, 8, texW, texH);

		if (width >= 48) {
			blit(poseStack, x + (width / 2) - 24, y - 9, 48, 8, 8 + u, v, 48, 8, texW, texH);
			blit(poseStack, x + (width / 2) - 24, y + height + 1, 48, 8, 8 + u, v + 8, 48, 8, texW, texH);
		}
		poseStack.popPose();
	}

	public static int combineARGB(int a, int r, int g, int b) {
		a = Mth.clamp(a, 0, 255);
		r = Mth.clamp(r, 0, 255);
		g = Mth.clamp(g, 0, 255);
		b = Mth.clamp(b, 0, 255);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static void blit(PoseStack poseStack, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int texW, int texH) {
		Matrix4f matrix4f = poseStack.last().pose();
		float minU = uOffset / texW;
		float maxU = (uOffset + uWidth) / texW;
		float minV = vOffset / texH;
		float maxV = (vOffset + vHeight) / texH;

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tesselator.getBuilder();
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferbuilder.vertex(matrix4f, (float)x, (float)y + height, 0.0F).uv(minU, maxV).endVertex();
		bufferbuilder.vertex(matrix4f, (float)x + width, (float)y + height, 0.0F).uv(maxU, maxV).endVertex();
		bufferbuilder.vertex(matrix4f, (float)x + width, (float)y, 0.0F).uv(maxU, minV).endVertex();
		bufferbuilder.vertex(matrix4f, (float)x, (float)y, 0.0F).uv(minU, minV).endVertex();
		tesselator.end();
	}

	public static void drawGradientRect(Matrix4f mat, int z, int left, int top, int right, int bottom, int startColor, int endColor) {
		float f  = (float)(startColor >> 24 & 255) / 255.0F;
		float f1 = (float)(startColor >> 16 & 255) / 255.0F;
		float f2 = (float)(startColor >> 8 & 255) / 255.0F;
		float f3 = (float)(startColor & 255) / 255.0F;
		float f4 = (float)(endColor >> 24 & 255) / 255.0F;
		float f5 = (float)(endColor >> 16 & 255) / 255.0F;
		float f6 = (float)(endColor >> 8 & 255) / 255.0F;
		float f7 = (float)(endColor & 255) / 255.0F;

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		Tesselator tess = Tesselator.getInstance();
		BufferBuilder buff = tess.getBuilder();
		buff.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		buff.vertex(mat, left, top, z).color(f1, f2, f3, f).endVertex();
		buff.vertex(mat, left, bottom, z).color(f5, f6, f7, f4).endVertex();
		buff.vertex(mat, right, bottom, z).color(f5, f6, f7, f4).endVertex();
		buff.vertex(mat, right, top, z).color(f1, f2, f3, f).endVertex();
		tess.end();
		RenderSystem.disableBlend();
	}

	public static void drawGradientRectHorizontal(Matrix4f mat, int z, int left, int top, int right, int bottom, int startColor, int endColor) {
		float f  = (float)(startColor >> 24 & 255) / 255.0F;
		float f1 = (float)(startColor >> 16 & 255) / 255.0F;
		float f2 = (float)(startColor >> 8 & 255) / 255.0F;
		float f3 = (float)(startColor & 255) / 255.0F;
		float f4 = (float)(endColor >> 24 & 255) / 255.0F;
		float f5 = (float)(endColor >> 16 & 255) / 255.0F;
		float f6 = (float)(endColor >> 8 & 255) / 255.0F;
		float f7 = (float)(endColor & 255) / 255.0F;

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		Tesselator tess = Tesselator.getInstance();
		BufferBuilder buff = tess.getBuilder();
		buff.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		buff.vertex(mat, right, top, z).color(f5, f6, f7, f4).endVertex();
		buff.vertex(mat, left, top, z).color(f1, f2, f3, f).endVertex();
		buff.vertex(mat, left, bottom, z).color(f1, f2, f3, f).endVertex();
		buff.vertex(mat, right, bottom, z).color(f5, f6, f7, f4).endVertex();
		tess.end();
		RenderSystem.disableBlend();
	}
}