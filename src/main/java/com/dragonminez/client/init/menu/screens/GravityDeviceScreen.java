package com.dragonminez.client.init.menu.screens;

import com.dragonminez.Reference;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.menu.menutypes.GravityDeviceMenu;
import com.dragonminez.common.network.C2S.GravityDeviceUpdateC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GravityDeviceScreen extends AbstractContainerScreen<GravityDeviceMenu> {
	protected static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/screen/gravity_device_gui.png");

	private EditBox gravityInput;
	private Button toggleButton;

	public GravityDeviceScreen(GravityDeviceMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
		super(pMenu, pPlayerInventory, pTitle);
	}

	@Override
	protected void init() {
		super.init();
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;

		gravityInput = new EditBox(this.font, x + 40, y + 20, 50, 16, Component.translatable("gui.dragonminez.gravity_device.gravity"));
		gravityInput.setMaxLength(4);
		gravityInput.setValue(String.valueOf(Math.max(1, menu.getTargetGravity())));
		gravityInput.setFilter(s -> s.isEmpty() || s.matches("\\d{1,4}"));
		addRenderableWidget(gravityInput);

		addRenderableWidget(Button.builder(Component.translatable("gui.dragonminez.gravity_device.set").withStyle(Style.EMPTY.withFont(DMZ_FONT)), b -> sendUpdate(menu.isActive()))
				.bounds(x + 95, y + 19, 40, 18).build());

		toggleButton = Button.builder(toggleLabel(menu.isActive()), b -> sendUpdate(!menu.isActive()))
				.bounds(x + 40, y + 42, 95, 18).build();
		addRenderableWidget(toggleButton);
	}

	private Component toggleLabel(boolean active) {
		return active
				? Component.translatable("gui.dragonminez.gravity_device.turn_off").withStyle(Style.EMPTY.withFont(DMZ_FONT))
				: Component.translatable("gui.dragonminez.gravity_device.turn_on").withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	private void sendUpdate(boolean active) {
		int gravity = parseGravity();
		NetworkHandler.sendToServer(new GravityDeviceUpdateC2S(menu.getBlockPos(), active, gravity));
	}

	private int parseGravity() {
		int max = ConfigManager.getServerConfig().getGravity().getDeviceMaxGravity();
		int value;
		try {
			value = Integer.parseInt(gravityInput.getValue());
		} catch (NumberFormatException e) {
			value = 1;
		}
		return Math.max(1, Math.min(value, max));
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, TEXTURE);
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;

		guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

		int energyHeight = menu.getScaledEnergy();
		guiGraphics.blit(TEXTURE, x + 154, y + 21 + (52 - energyHeight), 177, 21 + (60 - energyHeight), 12, energyHeight);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float delta) {
		renderBackground(guiGraphics);
		if (toggleButton != null) toggleButton.setMessage(toggleLabel(menu.isActive()));
		super.render(guiGraphics, pMouseX, pMouseY, delta);

		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;

		Component status;
		if (!menu.isRoomValid()) {
			status = Component.translatable("gui.dragonminez.gravity_device.no_room").withStyle(ChatFormatting.RED);
		} else if (menu.isRunning()) {
			status = Component.translatable("gui.dragonminez.gravity_device.running", menu.getTargetGravity()).withStyle(ChatFormatting.GREEN);
		} else if (menu.isActive()) {
			status = Component.translatable("gui.dragonminez.gravity_device.no_energy").withStyle(ChatFormatting.GOLD);
		} else {
			status = Component.translatable("gui.dragonminez.gravity_device.idle").withStyle(ChatFormatting.GRAY);
		}
		status = status.copy().withStyle(Style.EMPTY.withFont(DMZ_FONT));
		TextUtil.drawCenteredStringWithBorder(guiGraphics, this.font, status, x + imageWidth / 2, y + 64, 0xFFFFFF);

		renderTooltip(guiGraphics, pMouseX, pMouseY);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
		Component title = this.title.copy().withStyle(Style.EMPTY.withFont(DMZ_FONT));
		TextUtil.drawCenteredStringWithBorder(guiGraphics, this.font, title, this.imageWidth / 2, this.titleLabelY, 0xFFFFFF);
		guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
	}

	@Override
	protected void renderTooltip(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
		super.renderTooltip(guiGraphics, pMouseX, pMouseY);
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;
		if (pMouseX >= x + 154 && pMouseX <= x + 166 && pMouseY >= y + 16 && pMouseY <= y + 68) {
			guiGraphics.renderTooltip(this.font,
					Component.literal(menu.getEnergy() + " / " + menu.getMaxEnergy() + " Star Energy"),
					pMouseX, pMouseY);
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (gravityInput.isFocused() && (keyCode == 257 || keyCode == 335)) {
			sendUpdate(menu.isActive());
			return true;
		}
		if (gravityInput.isFocused() && keyCode != 256) {
			return gravityInput.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
