package com.dragonminez.client.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Marks entity renders performed by DMZ menus so first-person-only layer rules do
 * not accidentally hide parts of the character preview.
 */
public final class EntityPreviewRenderContext {
	private static int renderDepth;

	private EntityPreviewRenderContext() {
	}

	public static boolean isRendering() {
		return renderDepth > 0;
	}

	public static void renderEntityInInventory(GuiGraphics graphics, int x, int y, int scale,
			Vector3f translation, Quaternionf pose, Quaternionf cameraOrientation, LivingEntity entity) {
		renderDepth++;
		try {
			InventoryScreen.renderEntityInInventory(graphics, x, y, scale, translation, pose, cameraOrientation, entity);
		} finally {
			renderDepth--;
		}
	}
}
