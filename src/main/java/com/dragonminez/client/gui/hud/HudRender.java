package com.dragonminez.client.gui.hud;

import com.mojang.blaze3d.platform.GlStateManager;
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
import org.joml.Vector3f;

public final class HudRender {
	private static float alphaScale = 1.0f;
	private static boolean additive;

	private HudRender() {}

	public static void setAdditive(boolean value) {
		additive = value;
	}

	public static void setAlphaScale(float alpha) {
		alphaScale = Mth.clamp(alpha, 0.0f, 1.0f);
	}

	public static float currentAlphaScale() {
		return alphaScale;
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

	public static void spriteGradient(GuiGraphics graphics, HudSprites.Sprite sprite, float x, float y, float width, float height,
									  float from, float to, float alphaFrom, float alphaTo, int rgb, boolean flipX, boolean vertical) {
		spriteGradient(graphics, sprite, x, y, width, height, from, to, alphaFrom, alphaTo, alphaFrom, alphaTo, rgb, flipX, vertical);
	}

	public static void spriteGradient(GuiGraphics graphics, HudSprites.Sprite sprite, float x, float y, float width, float height,
									  float from, float to, float alphaFrom, float alphaTo, float alphaFromBottom, float alphaToBottom,
									  int rgb, boolean flipX, boolean vertical) {
		float startAlpha = Mth.clamp(alphaFrom, 0.0f, 1.0f) * alphaScale;
		float endAlpha = Mth.clamp(alphaTo, 0.0f, 1.0f) * alphaScale;
		float startBottomAlpha = vertical ? startAlpha : Mth.clamp(alphaFromBottom, 0.0f, 1.0f) * alphaScale;
		float endBottomAlpha = vertical ? endAlpha : Mth.clamp(alphaToBottom, 0.0f, 1.0f) * alphaScale;
		if (Math.max(Math.max(startAlpha, endAlpha), Math.max(startBottomAlpha, endBottomAlpha)) <= 0.004f || width <= 0.0f || height <= 0.0f || to <= from) return;

		float fromX = vertical ? 0.0f : from, toX = vertical ? 1.0f : to;
		float fromY = vertical ? 1.0f - to : 0.0f, toY = vertical ? 1.0f - from : 1.0f;
		boolean flip = flipX && !vertical;
		float x0 = flip ? x + width * (1.0f - toX) : x + width * fromX;
		float x1 = x0 + width * (toX - fromX);
		float y0 = y + height * fromY;
		float y1 = y + height * toY;
		float u0 = (sprite.u() + sprite.width() * (flip ? toX : fromX)) / sprite.sheetWidth();
		float u1 = (sprite.u() + sprite.width() * (flip ? fromX : toX)) / sprite.sheetWidth();
		float v0 = (sprite.v() + sprite.height() * fromY) / sprite.sheetHeight();
		float v1 = (sprite.v() + sprite.height() * toY) / sprite.sheetHeight();

		int topLeft, topRight, bottomLeft, bottomRight;
		int start = Math.round(startAlpha * 255.0f);
		int end = Math.round(endAlpha * 255.0f);
		int startBottom = Math.round(startBottomAlpha * 255.0f);
		int endBottom = Math.round(endBottomAlpha * 255.0f);
		if (vertical) {
			topLeft = topRight = end;
			bottomLeft = bottomRight = start;
		} else if (flip) {
			topLeft = end;
			bottomLeft = endBottom;
			topRight = start;
			bottomRight = startBottom;
		} else {
			topLeft = start;
			bottomLeft = startBottom;
			topRight = end;
			bottomRight = endBottom;
		}
		int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;

		graphics.flush();
		RenderSystem.setShaderTexture(0, sprite.sheet());
		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.enableBlend();
		if (additive) {
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		} else {
			RenderSystem.defaultBlendFunc();
		}
		Matrix4f matrix = graphics.pose().last().pose();
		BufferBuilder builder = Tesselator.getInstance().getBuilder();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		builder.vertex(matrix, x0, y0, 0.0f).color(r, g, b, topLeft).uv(u0, v0).endVertex();
		builder.vertex(matrix, x0, y1, 0.0f).color(r, g, b, bottomLeft).uv(u0, v1).endVertex();
		builder.vertex(matrix, x1, y1, 0.0f).color(r, g, b, bottomRight).uv(u1, v1).endVertex();
		builder.vertex(matrix, x1, y0, 0.0f).color(r, g, b, topRight).uv(u1, v0).endVertex();
		BufferUploader.drawWithShader(builder.end());
		if (additive) RenderSystem.defaultBlendFunc();
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
		if (additive) {
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		} else {
			RenderSystem.defaultBlendFunc();
		}
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
		if (additive) RenderSystem.defaultBlendFunc();
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

	public static final int PANEL_FILL_RGB = 0x141B25;
	public static final float PANEL_FILL_ALPHA = 0.78f;
	public static final int PANEL_FRAME_RGB = 0x070A0F;
	public static final float PANEL_FRAME_ALPHA = 0.92f;
	public static final float PANEL_FRAME_WIDTH = 1.0f;
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(com.dragonminez.Reference.MOD_ID, "smooth");

	public static void panel(GuiGraphics graphics, float x, float y, float width, float height, float alpha) {
		panel(graphics, x, y, width, height, PANEL_FILL_RGB, alpha);
	}

	public static void panel(GuiGraphics graphics, float x, float y, float width, float height, int fillRgb, float alpha) {
		float frame = PANEL_FRAME_WIDTH;
		rect(graphics, x + frame, y + frame, width - frame * 2.0f, height - frame * 2.0f, argb(PANEL_FILL_ALPHA * alpha, fillRgb));
		int frameColor = argb(PANEL_FRAME_ALPHA * alpha, PANEL_FRAME_RGB);
		rect(graphics, x, y, width, frame, frameColor);
		rect(graphics, x, y + height - frame, width, frame, frameColor);
		rect(graphics, x, y + frame, frame, height - frame * 2.0f, frameColor);
		rect(graphics, x + width - frame, y + frame, frame, height - frame * 2.0f, frameColor);
	}

	public static net.minecraft.network.chat.MutableComponent dmz(String text) {
		return net.minecraft.network.chat.Component.literal(text).withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(DMZ_FONT));
	}

	public static net.minecraft.network.chat.MutableComponent dmz(net.minecraft.network.chat.Component text) {
		return text.copy().withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(DMZ_FONT));
	}

	public static float dmzWidth(String text, float scale) {
		return Minecraft.getInstance().font.width(dmz(text)) * scale;
	}

	public static void dmzText(GuiGraphics graphics, String text, float x, float y, float scale, float align, int rgb, float alpha) {
		if (text == null || text.isEmpty()) return;
		dmzText(graphics, dmz(text), x, y, scale, align, rgb, alpha);
	}

	private static net.minecraft.util.FormattedCharSequence uncolored(net.minecraft.util.FormattedCharSequence sequence) {
		return sink -> sequence.accept((index, style, codePoint) ->
				sink.accept(index, style.withColor((net.minecraft.network.chat.TextColor) null), codePoint));
	}

	public static void dmzText(GuiGraphics graphics, net.minecraft.network.chat.Component component, float x, float y, float scale, float align, int rgb, float alpha) {
		int alphaChannel = Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * alphaScale * 255.0f);
		if (alphaChannel <= 3 || component == null) return;

		Font font = Minecraft.getInstance().font;
		int color = (alphaChannel << 24) | (rgb & 0xFFFFFF);
		int border = alphaChannel << 24;
		float offset = -font.width(component) * align;

		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0.0f);
		graphics.pose().scale(scale, scale, 1.0f);
		graphics.pose().translate(offset, 0.0f, 0.0f);
		net.minecraft.util.FormattedCharSequence outline = uncolored(component.getVisualOrderText());
		graphics.drawString(font, outline, -1, 0, border, false);
		graphics.drawString(font, outline, 1, 0, border, false);
		graphics.drawString(font, outline, 0, -1, border, false);
		graphics.drawString(font, outline, 0, 1, border, false);
		graphics.drawString(font, component, 0, 0, color, false);
		graphics.pose().popPose();
	}

	public static void dmzText(GuiGraphics graphics, net.minecraft.util.FormattedCharSequence sequence, float x, float y, float scale, float align, int rgb, float alpha) {
		int alphaChannel = Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * alphaScale * 255.0f);
		if (alphaChannel <= 3 || sequence == null) return;

		Font font = Minecraft.getInstance().font;
		int color = (alphaChannel << 24) | (rgb & 0xFFFFFF);
		int border = alphaChannel << 24;
		float offset = -font.width(sequence) * align;

		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0.0f);
		graphics.pose().scale(scale, scale, 1.0f);
		graphics.pose().translate(offset, 0.0f, 0.0f);
		graphics.drawString(font, uncolored(sequence), -1, 0, border, false);
		graphics.drawString(font, uncolored(sequence), 1, 0, border, false);
		graphics.drawString(font, uncolored(sequence), 0, -1, border, false);
		graphics.drawString(font, uncolored(sequence), 0, 1, border, false);
		graphics.drawString(font, sequence, 0, 0, color, false);
		graphics.pose().popPose();
	}

	public static void scissor(GuiGraphics graphics, float minX, float minY, float maxX, float maxY) {
		Matrix4f pose = graphics.pose().last().pose();
		Vector3f min = pose.transformPosition(new Vector3f(minX, minY, 0.0f));
		Vector3f max = pose.transformPosition(new Vector3f(maxX, maxY, 0.0f));
		graphics.enableScissor(Math.round(Math.min(min.x, max.x)), Math.round(Math.min(min.y, max.y)),
				Math.round(Math.max(min.x, max.x)), Math.round(Math.max(min.y, max.y)));
	}

	public static void rect(GuiGraphics graphics, float x, float y, float width, float height, int color) {
		gradient(graphics, x, y, width, height, color, color, color, color);
	}

	public static void rectHorizontal(GuiGraphics graphics, float x, float y, float width, float height, int colorLeft, int colorRight) {
		gradient(graphics, x, y, width, height, colorLeft, colorLeft, colorRight, colorRight);
	}

	public static void rectVertical(GuiGraphics graphics, float x, float y, float width, float height, int colorTop, int colorBottom) {
		gradient(graphics, x, y, width, height, colorTop, colorBottom, colorBottom, colorTop);
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
		dmzText(graphics, text, x, y, scale, align, rgb, alpha);
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
