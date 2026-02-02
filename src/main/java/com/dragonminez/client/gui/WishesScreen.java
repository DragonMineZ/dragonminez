package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.common.network.C2S.GrantWishC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.wish.Wish;
import com.dragonminez.common.wish.WishManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class WishesScreen extends Screen {

	private static final ResourceLocation MENU_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");

	private static final int PANEL_WIDTH = 141;
	private static final int PANEL_HEIGHT = 213;
	private static final int ITEM_HEIGHT = 20;
	private static final int MAX_VISIBLE_ITEMS = 8;

	private final String dragonType;
	private final int maxWishesToSelect;
	private final List<Wish> availableWishes;
	private final List<Integer> selectedIndices = new ArrayList<>();

	private int guiLeft, guiTop;
	private int scrollOffset = 0;
	private int maxScroll = 0;
	private boolean isScrolling = false;

	private TexturedTextButton confirmButton;

	public WishesScreen(String dragonType, int wishCount) {
		super(Component.literal("Wishes"));
		this.dragonType = dragonType;
		this.maxWishesToSelect = wishCount;
		this.availableWishes = WishManager.getClientWishes(dragonType);
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (this.width - PANEL_WIDTH) / 2;
		this.guiTop = (this.height - PANEL_HEIGHT) / 2;

		this.maxScroll = Math.max(0, this.availableWishes.size() - MAX_VISIBLE_ITEMS);

		this.confirmButton = new TexturedTextButton.Builder()
				.position(guiLeft + (PANEL_WIDTH - 80) / 2, guiTop + PANEL_HEIGHT - 35)
				.size(74, 20)
				.texture(BUTTON_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(Component.translatable("gui.dragonminez.customization.select"))
				.onPress(btn -> confirmWishes())
				.build();

		this.confirmButton.visible = false;
		this.addRenderableWidget(confirmButton);
	}

	private void confirmWishes() {
		NetworkHandler.sendToServer(new GrantWishC2S(dragonType, selectedIndices));
		this.onClose();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		graphics.blit(MENU_TEXTURE, guiLeft, guiTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);

		drawCenteredStringWithBorder(graphics,
				Component.translatable("gui.dragonminez.wishes_title", selectedIndices.size(), maxWishesToSelect),
				this.width / 2, guiTop + 12, 0xFFFFD700);

		renderWishesList(graphics, mouseX, mouseY);

		super.render(graphics, mouseX, mouseY, partialTick);

		renderTooltip(graphics, mouseX, mouseY);
	}

	private void renderWishesList(GuiGraphics graphics, int mouseX, int mouseY) {
		int listLeft = guiLeft + 10;
		int listTop = guiTop + 35;
		int listWidth = PANEL_WIDTH - 25;
		int listHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;

		graphics.enableScissor(listLeft, listTop, listLeft + listWidth, listTop + listHeight);

		for (int i = 0; i < availableWishes.size(); i++) {
			int itemY = listTop + (i * ITEM_HEIGHT) - (scrollOffset * ITEM_HEIGHT);

			if (itemY + ITEM_HEIGHT < listTop || itemY > listTop + listHeight) continue;

			Wish wish = availableWishes.get(i);
			boolean isSelected = selectedIndices.contains(i);
			boolean isHovered = mouseX >= listLeft && mouseX < listLeft + listWidth &&
					mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;

			int color;
			if (isSelected) color = 0x80D4AF37;
			else if (isHovered) color = 0x80555555;
			else color = 0x00000000;

			graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, color);

			drawStringWithBorder(graphics, Component.translatable(wish.getName()), listLeft + 5, itemY + 6, 0xFFFFFF);

			if (isSelected) {
				graphics.renderOutline(listLeft, itemY, listWidth, ITEM_HEIGHT, 0xFFFFD700);
			}
		}

		graphics.disableScissor();

		if (maxScroll > 0) {
			int scrollBarX = guiLeft + PANEL_WIDTH - 12;
			int scrollBarY = listTop;
			int scrollBarHeight = listHeight;

			graphics.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + scrollBarHeight, 0xFF333333);

			int totalItems = availableWishes.size();
			float scrollPercent = (float) scrollOffset / maxScroll;
			float visiblePercent = (float) MAX_VISIBLE_ITEMS / totalItems;

			int indicatorHeight = Math.max(20, (int) (scrollBarHeight * visiblePercent));
			int indicatorY = scrollBarY + (int) ((scrollBarHeight - indicatorHeight) * scrollPercent);

			graphics.fill(scrollBarX, indicatorY, scrollBarX + 3, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}
	}

	private void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		int listLeft = guiLeft + 10;
		int listTop = guiTop + 35;
		int listWidth = PANEL_WIDTH - 25;
		int listBottom = listTop + (MAX_VISIBLE_ITEMS * ITEM_HEIGHT);

		if (mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= listTop && mouseY < listBottom) {
			int relativeY = mouseY - listTop + (scrollOffset * ITEM_HEIGHT);
			int index = relativeY / ITEM_HEIGHT;

			if (index >= 0 && index < availableWishes.size()) {
				Wish wish = availableWishes.get(index);
				graphics.renderTooltip(this.font, Component.translatable(wish.getDescription()), mouseX, mouseY);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;

		int listLeft = guiLeft + 10;
		int listTop = guiTop + 35;
		int listWidth = PANEL_WIDTH - 25;
		int listBottom = listTop + (MAX_VISIBLE_ITEMS * ITEM_HEIGHT);

		if (mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= listTop && mouseY < listBottom) {
			int relativeY = (int) mouseY - listTop + (scrollOffset * ITEM_HEIGHT);
			int index = relativeY / ITEM_HEIGHT;

			if (index >= 0 && index < availableWishes.size()) {
				toggleSelection(index);
				Minecraft.getInstance().getSoundManager().play(
						net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F)
				);
				return true;
			}
		}

		if (maxScroll > 0 && mouseX >= listLeft + listWidth && mouseX <= guiLeft + PANEL_WIDTH) {
			this.isScrolling = true;
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (maxScroll > 0) {
			int scroll = (int) -Math.signum(delta);
			this.scrollOffset = Mth.clamp(this.scrollOffset + scroll, 0, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		this.isScrolling = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (isScrolling && maxScroll > 0) {
			int listHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;
			float scrollPerPixel = (float) maxScroll / listHeight;

			this.scrollOffset = Mth.clamp(this.scrollOffset + (int) (dragY * scrollPerPixel * 5), 0, maxScroll);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	private void toggleSelection(int index) {
		if (selectedIndices.contains(index)) {
			selectedIndices.remove(Integer.valueOf(index));
		} else {
			if (maxWishesToSelect == 1) {
				selectedIndices.clear();
				selectedIndices.add(index);
			} else {
				if (selectedIndices.size() < maxWishesToSelect) {
					selectedIndices.add(index);
				}
			}
		}
		confirmButton.visible = selectedIndices.size() == maxWishesToSelect;
	}

	private void drawStringWithBorder(GuiGraphics graphics, Component text, int x, int y, int textColor) {
		int borderColor = 0xFF000000;
		graphics.drawString(this.font, text, x + 1, y, borderColor, false);
		graphics.drawString(this.font, text, x - 1, y, borderColor, false);
		graphics.drawString(this.font, text, x, y + 1, borderColor, false);
		graphics.drawString(this.font, text, x, y - 1, borderColor, false);
		graphics.drawString(this.font, text, x, y, textColor, false);
	}

	private void drawCenteredStringWithBorder(GuiGraphics graphics, Component text, int centerX, int y, int textColor) {
		int textWidth = this.font.width(text);
		int x = centerX - (textWidth / 2);
		drawStringWithBorder(graphics, text, x, y, textColor);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}