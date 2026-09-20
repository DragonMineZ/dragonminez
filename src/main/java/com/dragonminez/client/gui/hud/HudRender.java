package com.dragonminez.client.gui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class HudRender {
	private static float alphaScale = 1.0f;

	private HudRender() {}

	public static void setAlphaScale(float alpha) {
		alphaScale = Mth.clamp(alpha, 0.0f, 1.0f);
	}

	public static void sprite(GuiGraphics graphics, HudSprites.Sprite sprite, float x, float y, float width, float height, int rgb, float alpha) {
		spritePart(graphics, sprite, x, y, width, height, 0.0f, 0.0f, 1.0f, 1.0f, rgb, alpha);
	}

	public static void spritePart(GuiGraphics graphics, HudSprites.Sprite sprite, float x, float y, float width, float height,
								  float fromX, float fromY, float toX, float toY, int rgb, float alpha) {
		spritePart(graphics, sprite, x, y, width, height, fromX, fromY, toX, toY, rgb, alpha, false);
	}

	public static void sprite(GuiGraphics graphics, HudSprites.Sprite sprite, float x, float y, float width, float height, int rgb, float alpha, boolean flipX) {
		spritePart(graphics, sprite, x, y, width, height, 0.0f, 0.0f, 1.0f, 1.0f, rgb, alpha, flipX);
	}

	public static void spritePart(GuiGraphics graphics, HudSprites.Sprite sprite, float x, float y, float width, float height,
								  float fromX, float fromY, float toX, float toY, int rgb, float alpha, boolean flipX) {
		float a = Mth.clamp(alpha, 0.0f, 1.0f) * alphaScale;
		if (a <= 0.004f || width <= 0.0f || height <= 0.0f || toX <= fromX || toY <= fromY) return;

		RenderSystem.setShaderColor(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f, a);
		float destX = flipX ? x + width * (1.0f - toX) : x + width * fromX;
		float regionU = flipX ? sprite.u() + sprite.width() * toX : sprite.u() + sprite.width() * fromX;
		float regionWidth = sprite.width() * (toX - fromX) * (flipX ? -1.0f : 1.0f);
		blit(graphics, sprite.sheet(), destX, y + height * fromY,
				regionU, sprite.v() + sprite.height() * fromY,
				width * (toX - fromX), height * (toY - fromY),
				regionWidth, sprite.height() * (toY - fromY),
				sprite.sheetWidth(), sprite.sheetHeight());
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}

	public static void blit(GuiGraphics graphics, ResourceLocation texture, float x, float y, float u, float v, float width, float height, int textureWidth, int textureHeight) {
		blit(graphics, texture, x, y, u, v, width, height, width, height, textureWidth, textureHeight);
	}

	public static void blit(GuiGraphics graphics, ResourceLocation texture, float x, float y, float u, float v, float width, float height,
							float regionWidth, float regionHeight, int textureWidth, int textureHeight) {
		if (width <= 0.0f || height <= 0.0f) return;
		graphics.flush();
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		Matrix4f matrix = graphics.pose().last().pose();
		float u0 = u / textureWidth, u1 = (u + regionWidth) / textureWidth;
		float v0 = v / textureHeight, v1 = (v + regionHeight) / textureHeight;
		BufferBuilder builder = Tesselator.getInstance().getBuilder();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		builder.vertex(matrix, x, y, 0.0f).uv(u0, v0).endVertex();
		builder.vertex(matrix, x, y + height, 0.0f).uv(u0, v1).endVertex();
		builder.vertex(matrix, x + width, y + height, 0.0f).uv(u1, v1).endVertex();
		builder.vertex(matrix, x + width, y, 0.0f).uv(u1, v0).endVertex();
		BufferUploader.drawWithShader(builder.end());
	}

	public static void nineSlice(GuiGraphics graphics, ResourceLocation texture, float x, float y, float width, float height,
								 float u, float v, float regionWidth, float regionHeight, float border, int textureWidth, int textureHeight) {
		if (width <= 0.0f || height <= 0.0f) return;
		float destBorder = Math.min(border, Math.min(width, height) / 2.0f);
		float sourceBorder = Math.min(border, Math.min(regionWidth, regionHeight) / 2.0f);
		float[] destX = {x, x + destBorder, x + width - destBorder, x + width};
		float[] destY = {y, y + destBorder, y + height - destBorder, y + height};
		float[] sourceX = {u, u + sourceBorder, u + regionWidth - sourceBorder, u + regionWidth};
		float[] sourceY = {v, v + sourceBorder, v + regionHeight - sourceBorder, v + regionHeight};

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				blit(graphics, texture, destX[column], destY[row], sourceX[column], sourceY[row],
						destX[column + 1] - destX[column], destY[row + 1] - destY[row],
						sourceX[column + 1] - sourceX[column], sourceY[row + 1] - sourceY[row], textureWidth, textureHeight);
			}
		}
	}

	public static void rect(GuiGraphics graphics, float x, float y, float width, float height, int color) {
		gradient(graphics, x, y, width, height, color, color, color, color);
	}

	public static void rectHorizontal(GuiGraphics graphics, float x, float y, float width, float height, int colorLeft, int colorRight) {
		gradient(graphics, x, y, width, height, colorLeft, colorLeft, colorRight, colorRight);
	}

	private static void gradient(GuiGraphics graphics, float x, float y, float width, float height, int topLeft, int bottomLeft, int bottomRight, int topRight) {
		if (width <= 0.0f || height <= 0.0f) return;
		Matrix4f matrix = graphics.pose().last().pose();
		VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
		vertex(buffer, matrix, x, y, topLeft);
		vertex(buffer, matrix, x, y + height, bottomLeft);
		vertex(buffer, matrix, x + width, y + height, bottomRight);
		vertex(buffer, matrix, x + width, y, topRight);
		graphics.flush();
	}

	private static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, int argb) {
		int alpha = Math.round(((argb >>> 24) & 0xFF) * alphaScale);
		buffer.vertex(matrix, x, y, 0.0f).color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, alpha).endVertex();
	}

	public static void text(GuiGraphics graphics, String text, float x, float y, float scale, float align, int rgb, float alpha) {
		int alphaChannel = Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * alphaScale * 255.0f);
		if (alphaChannel <= 3 || text == null || text.isEmpty()) return;

		Font font = Minecraft.getInstance().font;
		int color = (alphaChannel << 24) | (rgb & 0xFFFFFF);
		int border = alphaChannel << 24;
		float offset = -font.width(text) * align;

		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0.0f);
		graphics.pose().scale(scale, scale, 1.0f);
		graphics.pose().translate(offset, 0.0f, 0.0f);
		graphics.drawString(font, text, -1, 0, border, false);
		graphics.drawString(font, text, 1, 0, border, false);
		graphics.drawString(font, text, 0, -1, border, false);
		graphics.drawString(font, text, 0, 1, border, false);
		graphics.drawString(font, text, 0, 0, color, false);
		graphics.pose().popPose();
	}

	public static int argb(float alpha, int rgb) {
		return (Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f) << 24) | (rgb & 0xFFFFFF);
	}

	public static int rgb(float[] rgb, float brightness) {
		int r = Math.round(Mth.clamp(rgb[0] * brightness, 0.0f, 1.0f) * 255.0f);
		int g = Math.round(Mth.clamp(rgb[1] * brightness, 0.0f, 1.0f) * 255.0f);
		int b = Math.round(Mth.clamp(rgb[2] * brightness, 0.0f, 1.0f) * 255.0f);
		return (r << 16) | (g << 8) | b;
	}

	public static int mix(int rgbFrom, int rgbTo, float amount) {
		float t = Mth.clamp(amount, 0.0f, 1.0f);
		int r = Math.round(((rgbFrom >> 16) & 0xFF) + (((rgbTo >> 16) & 0xFF) - ((rgbFrom >> 16) & 0xFF)) * t);
		int g = Math.round(((rgbFrom >> 8) & 0xFF) + (((rgbTo >> 8) & 0xFF) - ((rgbFrom >> 8) & 0xFF)) * t);
		int b = Math.round((rgbFrom & 0xFF) + ((rgbTo & 0xFF) - (rgbFrom & 0xFF)) * t);
		return (r << 16) | (g << 8) | b;
	}
}
