package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.stats.techniques.Techniques;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Locale;

public class TechniqueHotbarHUD {
	private static final ResourceLocation HORIZONTAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/skills_hotbar_horizontal.png");
	private static final ResourceLocation VERTICAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/skills_hotbar_vertical.png");

	private static final int SLOT_SIZE = 64;
	private static final int ICON_SIZE = 64;
	private static final int HOTBAR_TEXTURE_SIZE = 512;

	public static final IGuiOverlay HUD_TECHNIQUES = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;

			Techniques techniques = data.getTechniques();
			String[] slots = techniques.getEquippedSlots();
			int selectedSlot = techniques.getSelectedSlot();
			int visibleSlots = Math.min(5, slots.length);
			boolean horizontal = ConfigManager.getUserConfig().getTechniqueHotbarHorizontal();
			ResourceLocation hotbarTexture = horizontal ? HORIZONTAL_TEXTURE : VERTICAL_TEXTURE;

			int[] horizontalX = {64, 140, 224, 308, 384};
			int[] horizontalY = {56, 48, 40, 48, 56};
			int[] verticalX = {32, 24, 20, 24, 32};
			int[] verticalY = {64, 144, 224, 312, 388};

			int visibleWidth = horizontal ? 512 : 168;
			int visibleHeight = horizontal ? 132 : 448;

			RenderSystem.enableBlend();
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

			float baseScale = horizontal ? 0.4f : 0.45f;
			float maxAllowedWidth = width * (horizontal ? 0.45f : 0.25f);
			float maxAllowedHeight = height * (horizontal ? 0.40f : 0.65f);

			float scaleX = maxAllowedWidth / visibleWidth;
			float scaleY = maxAllowedHeight / visibleHeight;

			float hudScale = Math.min(baseScale, Math.min(scaleX, scaleY));

			int scaledWidth = Math.round(visibleWidth * hudScale);
			int scaledHeight = Math.round(visibleHeight * hudScale);

			int hotbarScreenX = width - scaledWidth - 8;
			int hotbarScreenY = horizontal ? height - scaledHeight - 8 : (height - scaledHeight) / 2;

			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(hotbarScreenX, hotbarScreenY, 0.0f);
			guiGraphics.pose().scale(hudScale, hudScale, 1.0f);

			for (int i = 0; i < visibleSlots; i++) {
				int slotX = horizontal ? horizontalX[i] : verticalX[i];
				int slotY = horizontal ? horizontalY[i] : verticalY[i];

				String techId = slots[i];
				if (techId != null && !techId.isEmpty()) {
					TechniqueData equippedTechnique = techniques.getUnlockedTechniques().get(techId);
					ResourceLocation iconTexture = getTechniqueIconTexture(equippedTechnique);

					if (iconTexture != null) {
						if (i == selectedSlot && equippedTechnique instanceof KiAttackData kiAttack) {
							int color = kiAttack.getColorExterior();
							RenderSystem.setShaderColor(((color >> 16) & 255) / 255.0f, ((color >> 8) & 255) / 255.0f, (color & 255) / 255.0f, 1.0f);
						}

						RenderSystem.setShaderTexture(0, iconTexture);
						int iconX = slotX + (SLOT_SIZE - ICON_SIZE) / 2;
						int iconY = slotY + (SLOT_SIZE - ICON_SIZE) / 2;
						guiGraphics.blit(iconTexture, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
						RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
					}
				}
			}

			RenderSystem.setShaderTexture(0, hotbarTexture);
			guiGraphics.blit(hotbarTexture, 0, 0, 0, 0, HOTBAR_TEXTURE_SIZE, HOTBAR_TEXTURE_SIZE, HOTBAR_TEXTURE_SIZE, HOTBAR_TEXTURE_SIZE);

			guiGraphics.pose().popPose();
		});
	};

	private static ResourceLocation getTechniqueIconTexture(TechniqueData techniqueData) {
		if (!(techniqueData instanceof KiAttackData kiAttack) || kiAttack.getKiType() == null) return null;
		String iconName = kiAttack.getKiType().name().toLowerCase(Locale.ROOT);
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/iconski/" + iconName + ".png");
	}
}