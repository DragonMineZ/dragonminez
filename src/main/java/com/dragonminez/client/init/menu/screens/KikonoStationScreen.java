package com.dragonminez.client.init.menu.screens;

import com.dragonminez.Reference;
import com.dragonminez.common.init.menu.menutypes.KikonoStationMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class KikonoStationScreen extends AbstractContainerScreen<KikonoStationMenu> {
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/screen/kikono_station_gui.png");

	public KikonoStationScreen(KikonoStationMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
		super(pMenu, pPlayerInventory, pTitle);
	}

	@Override
	protected void init() {
		super.init();
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, TEXTURE);
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;

		guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

		if (menu.isCrafting()) {
			guiGraphics.blit(TEXTURE, x + 111, y + 35, 177, 2, menu.getScaledProgress(), 17);
		}

		int energyHeight = menu.getScaledEnergy();
		guiGraphics.blit(TEXTURE, x + 9, y + 21 + (52 - energyHeight), 177, 21 + (60 - energyHeight), 12, energyHeight);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float delta) {
		renderBackground(guiGraphics);
		super.render(guiGraphics, pMouseX, pMouseY, delta);
		renderTooltip(guiGraphics, pMouseX, pMouseY);
	}

	@Override
	protected void renderTooltip(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
		super.renderTooltip(guiGraphics, pMouseX, pMouseY);

		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;

		if(pMouseX >= x + 10 && pMouseX <= x + 18 && pMouseY >= y + 16 && pMouseY <= y + 68) {
			guiGraphics.renderTooltip(this.font,
					Component.literal(menu.getEnergy() + " / " + menu.getMaxEnergy() + " Star Energy"),
					pMouseX, pMouseY);
		}
	}
}