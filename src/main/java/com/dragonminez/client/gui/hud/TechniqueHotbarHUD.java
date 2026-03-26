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
	private static final int HORIZONTAL_VISIBLE_HEIGHT = 132;
	private static final int VERTICAL_VISIBLE_WIDTH = 168;
	private static final int[] HORIZONTAL_SLOT_X = {64, 140, 224, 304, 384};
	private static final int[] HORIZONTAL_SLOT_Y = {56, 48, 40, 48, 56};
	private static final int VERTICAL_SLOT_START_X = 56;
	private static final int VERTICAL_SLOT_START_Y = 64;
	private static final int VERTICAL_SLOT_GAP = 16;

	public static final IGuiOverlay HUD_TECHNIQUES = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;

			Techniques techniques = data.getTechniques();
			String[] slots = techniques.getEquippedSlots();
			int selectedSlot = techniques.getSelectedSlot();
			int visibleSlots = Math.min(5, slots.length);
			boolean horizontal = ConfigManager.getUserConfig().getHud().getTechniqueHotbarHorizontal();
			ResourceLocation hotbarTexture = horizontal ? HORIZONTAL_TEXTURE : VERTICAL_TEXTURE;

			RenderSystem.enableBlend();
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			float hudScale = (float) (1.0d / Math.max(1.0d, mc.getWindow().getGuiScale()));

			int visibleWidth = horizontal ? HOTBAR_TEXTURE_SIZE : VERTICAL_VISIBLE_WIDTH;
			int visibleHeight = horizontal ? HORIZONTAL_VISIBLE_HEIGHT : HOTBAR_TEXTURE_SIZE;

			int hotbarScreenX = width - Math.round(visibleWidth * hudScale) - 8;
			int hotbarScreenY = height - Math.round(visibleHeight * hudScale) - 8;

			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(hotbarScreenX, hotbarScreenY, 0.0f);
			guiGraphics.pose().scale(hudScale, hudScale, 1.0f);

			int hotbarX = 0;
			int hotbarY = 0;

			for (int i = 0; i < visibleSlots; i++) {
				int slotX;
				int slotY;
				if (horizontal) {
					slotX = hotbarX + HORIZONTAL_SLOT_X[i];
					slotY = hotbarY + HORIZONTAL_SLOT_Y[i];
				} else {
					slotX = hotbarX + VERTICAL_SLOT_START_X;
					slotY = hotbarY + VERTICAL_SLOT_START_Y + i * (SLOT_SIZE + VERTICAL_SLOT_GAP);
				}

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
			guiGraphics.blit(hotbarTexture, hotbarX, hotbarY, 0, 0, HOTBAR_TEXTURE_SIZE, HOTBAR_TEXTURE_SIZE, HOTBAR_TEXTURE_SIZE, HOTBAR_TEXTURE_SIZE);

			guiGraphics.pose().popPose();
		});
	};

	private static ResourceLocation getTechniqueIconTexture(TechniqueData techniqueData) {
		if (!(techniqueData instanceof KiAttackData kiAttack) || kiAttack.getKiType() == null) return null;
		String iconName = kiAttack.getKiType().name().toLowerCase(Locale.ROOT);
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/iconski/" + iconName + ".png");
	}
}