package com.dragonminez.client.gui.hair;

import com.dragonminez.Reference;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

final class HairEditorTextures {
	static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	static final ResourceLocation MENU_SMALL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menusmall.png");
	static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");

	static final Sprite PANEL = new Sprite(MENU_BIG, 0, 0, 141, 213, 8);
	static final Sprite SMALL_PANEL = new Sprite(MENU_SMALL, 0, 0, 141, 94, 8);
	static final Sprite TITLE_PLATE = new Sprite(MENU_BIG, 142, 22, 107, 21, 5);
	static final Sprite BUTTON = new Sprite(BUTTONS, 0, 28, 74, 20, 4);
	static final Sprite BUTTON_HOVER = new Sprite(BUTTONS, 0, 48, 74, 20, 4);
	static final Sprite BUTTON_ACTIVE = new Sprite(BUTTONS, 0, 68, 150, 20, 3);
	static final Sprite BUTTON_ACCENT = new Sprite(BUTTONS, 0, 88, 150, 20, 3);
	static final Sprite FIELD = new Sprite(BUTTONS, 0, 108, 107, 18, 3);
	static final Sprite FIELD_EDITING = new Sprite(BUTTONS, 0, 126, 107, 18, 3);

	private static final float ATLAS_SIZE = 256.0f;

	record Sprite(ResourceLocation texture, int u, int v, int width, int height, int border) {}

	private HairEditorTextures() {}

	static void draw(GuiGraphics graphics, Sprite sprite, int x, int y, int width, int height) {
		draw(graphics, sprite, x, y, width, height, 1.0f, 1.0f, 1.0f, 1.0f);
	}

	static void draw(GuiGraphics graphics, Sprite sprite, int x, int y, int width, int height, float red, float green, float blue, float alpha) {
		if (width <= 0 || height <= 0) return;
		int borderX = Math.min(sprite.border(), width / 2);
		int borderY = Math.min(sprite.border(), height / 2);
		int sourceBorderX = Math.min(sprite.border(), sprite.width() / 2);
		int sourceBorderY = Math.min(sprite.border(), sprite.height() / 2);

		float[] xs = {x, x + borderX, x + width - borderX, x + width};
		float[] ys = {y, y + borderY, y + height - borderY, y + height};
		float[] us = {sprite.u(), sprite.u() + sourceBorderX, sprite.u() + sprite.width() - sourceBorderX, sprite.u() + sprite.width()};
		float[] vs = {sprite.v(), sprite.v() + sourceBorderY, sprite.v() + sprite.height() - sourceBorderY, sprite.v() + sprite.height()};

		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, sprite.texture());
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		Matrix4f matrix = graphics.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		for (int column = 0; column < 3; column++) {
			if (xs[column + 1] <= xs[column]) continue;
			for (int row = 0; row < 3; row++) {
				if (ys[row + 1] <= ys[row]) continue;
				quad(buffer, matrix, xs[column], ys[row], xs[column + 1], ys[row + 1],
						us[column] / ATLAS_SIZE, vs[row] / ATLAS_SIZE, us[column + 1] / ATLAS_SIZE, vs[row + 1] / ATLAS_SIZE,
						red, green, blue, alpha);
			}
		}
		Tesselator.getInstance().end();
		RenderSystem.disableBlend();
	}

	private static void quad(BufferBuilder buffer, Matrix4f matrix, float x0, float y0, float x1, float y1,
							 float u0, float v0, float u1, float v1, float red, float green, float blue, float alpha) {
		buffer.vertex(matrix, x0, y0, 0.0f).uv(u0, v0).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x0, y1, 0.0f).uv(u0, v1).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x1, y1, 0.0f).uv(u1, v1).color(red, green, blue, alpha).endVertex();
		buffer.vertex(matrix, x1, y0, 0.0f).uv(u1, v0).color(red, green, blue, alpha).endVertex();
	}
}
