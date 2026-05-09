package com.dragonminez.client.gui.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.locale.Language;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class CustomTooltipRenderers {

	public static class HeaderRenderer implements ClientTooltipComponent {
		private final ItemStack stack;
		private final FormattedCharSequence title;

		public HeaderRenderer(CustomTooltipNodes.HeaderNode node) {
			this.stack = node.stack();
			this.title = Language.getInstance().getVisualOrder(node.title());
		}

		@Override
		public int getHeight() {
			return 24;
		}

		@Override
		public int getWidth(Font font) {
			return 26 + font.width(title);
		}

		@Override
		public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
			if (!TooltipDecor.hasSpecialBorder) return;

			graphics.flush();
			int margin = 2;
			int renderWidth = 22;
			int renderHeight = 22;

			int borderStart = TooltipDecor.combineARGB((int) (((TooltipDecor.currentBorderStart >> 24) & 0xFF) * 0.35f), (TooltipDecor.currentBorderStart >> 16) & 0xFF, (TooltipDecor.currentBorderStart >> 8) & 0xFF, TooltipDecor.currentBorderStart & 0xFF);
			int bgStart = TooltipDecor.combineARGB((int) (((TooltipDecor.currentBackgroundStart >> 24) & 0xFF) * 0.15f), (TooltipDecor.currentBackgroundStart >> 16) & 0xFF, (TooltipDecor.currentBackgroundStart >> 8) & 0xFF, TooltipDecor.currentBackgroundStart & 0xFF);
			int bgEnd = TooltipDecor.combineARGB((int) (((TooltipDecor.currentBackgroundEnd >> 24) & 0xFF) * 0.60f), (TooltipDecor.currentBackgroundEnd >> 16) & 0xFF, (TooltipDecor.currentBackgroundEnd >> 8) & 0xFF, TooltipDecor.currentBackgroundEnd & 0xFF);

			PoseStack pose = graphics.pose();
			Matrix4f matrix = pose.last().pose();

			TooltipDecor.drawGradientRect(matrix, 0, x + margin + 1, y + margin + 1, x + renderWidth - margin - 1, y + renderHeight - margin - 1, bgStart, bgEnd);
			TooltipDecor.drawGradientRectHorizontal(matrix, 0, x + margin + 1, y + margin + 1, x + renderWidth - margin - 1, y + renderHeight - margin - 1, bgStart, bgEnd);

			TooltipDecor.drawGradientRect(matrix, 0, x + margin + 1, y + margin, x + renderWidth - margin - 1, y + margin + 1, borderStart, borderStart);
			TooltipDecor.drawGradientRect(matrix, 0, x + margin + 1, y + renderHeight - margin - 1, x + renderWidth - margin - 1, y + renderHeight - margin, borderStart, borderStart);
			TooltipDecor.drawGradientRect(matrix, 0, x + margin, y + margin + 1, x + margin + 1, y + renderHeight - margin - 1, borderStart, borderStart);
			TooltipDecor.drawGradientRect(matrix, 0, x + renderWidth - margin - 1, y + margin + 1, x + renderWidth - margin, y + renderHeight - margin - 1, borderStart, borderStart);

			pose.pushPose();

			float offsetX = -0.75f;
			float offsetY = -0.75f;

			float centerX = x + margin - 1 + 11 + offsetX;
			float centerY = y + margin - 1 + 11 + offsetY;
			pose.translate(centerX, centerY, 150);

			float rotationAngle = ((System.currentTimeMillis() % 4000L) / 4000.0f) * 360.0f;
			pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotationAngle));

			float scale = 0.9f;
			pose.scale(scale, scale, scale);
			pose.translate(-8, -8, -150);

			graphics.renderItem(stack, 0, 0);
			pose.popPose();
		}

		@Override
		public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource bufferSource) {
			int textY = y + (22 - font.lineHeight) / 2;
			font.drawInBatch(title, x + 26, textY, -1, true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
		}
	}

	public static class SeparatorRenderer implements ClientTooltipComponent {
		public SeparatorRenderer(CustomTooltipNodes.SeparatorNode node) {}

		@Override
		public int getHeight() { return 0; }

		@Override
		public int getWidth(Font font) { return 0; }

		@Override
		public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
			if (!TooltipDecor.hasSpecialBorder) return;
			TooltipDecor.drawSeparator(graphics.pose(), TooltipDecor.lastTooltipX, y - 8, TooltipDecor.lastTooltipW, TooltipDecor.currentBorderStart);
		}
	}

	public static class PaddingRenderer implements ClientTooltipComponent {
		private final int height;
		public PaddingRenderer(CustomTooltipNodes.PaddingNode node) { this.height = node.height(); }
		@Override public int getHeight() { return height; }
		@Override public int getWidth(Font font) { return 0; }
		@Override public void renderImage(Font font, int x, int y, GuiGraphics graphics) { }
	}
}