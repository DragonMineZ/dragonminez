package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.Techniques;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class TechniqueHotbarHUD {
	private static final ResourceLocation HUD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/technique_hotbar.png");

	public static final IGuiOverlay HUD_TECHNIQUES = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (Minecraft.getInstance().options.renderDebug || Minecraft.getInstance().player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;

			Techniques techniques = data.getTechniques();
			String[] slots = techniques.getEquippedSlots();
			int selectedSlot = techniques.getSelectedSlot();

			RenderSystem.enableBlend();
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			RenderSystem.setShaderTexture(0, HUD_TEXTURE);

			guiGraphics.pose().pushPose();

			int slotSize = 22; // Ajusta este valor al tamaño en píxeles de tu textura de slot
			int spacing = 2;
			int gridStartX = width - (4 * (slotSize + spacing)) - 10;
			int gridStartY = height - (2 * (slotSize + spacing)) - 10;

			for (int i = 0; i < 8; i++) {
				int col = i % 4;
				int row = i / 4;
				int slotX = gridStartX + (col * (slotSize + spacing));
				int slotY = gridStartY + (row * (slotSize + spacing));

				// Aquí dibujarías el fondo del slot desde tu HUD_TEXTURE
				// guiGraphics.blit(HUD_TEXTURE, slotX, slotY, u, v, slotSize, slotSize);

				if (i == selectedSlot) {
					guiGraphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0x88FFAA00);
				} else {
					guiGraphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0x88000000);
				}

				String techId = slots[i];
				if (techId != null && !techId.isEmpty()) {
					// Muestra las 3 primeras letras del ID como placeholder temporal hasta que tengas íconos
					String displayTxt = techId.length() > 3 ? techId.substring(0, 3).toUpperCase() : techId.toUpperCase();

					guiGraphics.pose().pushPose();
					float textScale = 0.5f;
					guiGraphics.pose().scale(textScale, textScale, 1.0f);

					int textX = (int) ((slotX + (slotSize / 2f)) / textScale);
					int textY = (int) ((slotY + (slotSize / 2f) - 4) / textScale);

					guiGraphics.drawCenteredString(Minecraft.getInstance().font, displayTxt, textX, textY, 0xFFFFFF);
					guiGraphics.pose().popPose();
				}
			}

			guiGraphics.pose().popPose();
		});
	};
}