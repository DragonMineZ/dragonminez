package com.dragonminez.client.init.menu.screens;

import com.dragonminez.Reference;
import com.dragonminez.common.init.menu.menutypes.PatternStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PatternStationScreen extends AbstractContainerScreen<PatternStationMenu> {
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/screen/pattern_station_gui.png");
	private static final int TEXTURE_SIZE = 512;

	public static final int ARROW_X = 182;
	public static final int ARROW_Y = 25;
	public static final int ARROW_W = 22;
	public static final int ARROW_H = 15;

	public PatternStationScreen(PatternStationMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 298;
		this.imageHeight = 146;
	}

	@Override
	protected void init() {
		super.init();
		this.titleLabelX = 8;
		this.titleLabelY = 6;
		this.inventoryLabelX = PatternStationMenu.INV_X;
		this.inventoryLabelY = PatternStationMenu.INV_Y - 11;
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}
}
