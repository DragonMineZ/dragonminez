package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.TravelToPlanetC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.world.dimension.NamekDimension;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SpacePodScreen extends Screen {

	private static final ResourceLocation MENU_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation ICONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/spaceshipicons.png");

	private static final int PANEL_WIDTH = 141;
	private static final int PANEL_HEIGHT = 213;
	private static final int ITEM_HEIGHT = 24;
	private static final int MAX_VISIBLE_ITEMS = 7;

	private static final int ICON_SIZE = 11;
	private static final int ICON_X_COLOR = 3;
	private static final int ICON_X_GRAY = 20;
	private static final int ICON_Y_START = 3;
	private static final int ICON_Y_STEP = 14;

	private final List<PlanetDestination> destinations = new ArrayList<>();
	private int selectedIndex = -1;

	private int guiLeft, guiTop;
	private int scrollOffset = 0;
	private int maxScroll = 0;
	private boolean isScrolling = false;

	private TexturedTextButton travelButton;

	public SpacePodScreen() {
		super(Component.literal("Space Pod"));
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (this.width - PANEL_WIDTH) / 2;
		this.guiTop = (this.height - PANEL_HEIGHT) / 2;

		loadDestinations();
		this.maxScroll = Math.max(0, this.destinations.size() - MAX_VISIBLE_ITEMS);

		this.travelButton = new TexturedTextButton.Builder()
				.position(guiLeft + (PANEL_WIDTH - 80) / 2, guiTop + PANEL_HEIGHT - 35)
				.size(74, 20)
				.texture(BUTTON_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(Component.translatable("gui.dragonminez.travel"))
				.onPress(btn -> initiateTravel())
				.build();

		this.travelButton.visible = false;
		this.addRenderableWidget(travelButton);
	}

	private void loadDestinations() {
		destinations.clear();

		final boolean[] kaioUnlocked = {false};
		if (this.minecraft.player != null) {
			StatsProvider.get(StatsCapability.INSTANCE, this.minecraft.player).ifPresent(cap -> kaioUnlocked[0] = cap.getStatus().isInKaioPlanet());
		}

		destinations.add(new PlanetDestination("gui.dragonminez.spacepod.overworld", Level.OVERWORLD, 0, true));
		destinations.add(new PlanetDestination("gui.dragonminez.spacepod.namek", NamekDimension.NAMEK_KEY, 1, true));
		if (ConfigManager.getServerConfig().getWorldGen().getOtherworldActive()) {
			destinations.add(new PlanetDestination("gui.dragonminez.spacepod.otherworld", OtherworldDimension.OTHERWORLD_KEY, 2, kaioUnlocked[0]));
		} else {
			destinations.add(new PlanetDestination("gui.dragonminez.spacepod.otherworld", null, 2, false));
		}
		destinations.add(new PlanetDestination("gui.dragonminez.spacepod.supreme", null, 3, false));
		destinations.add(new PlanetDestination("gui.dragonminez.spacepod.cereal", null, 4, false));
		destinations.add(new PlanetDestination("gui.dragonminez.spacepod.beerus", null, 5, false));
	}

	private void initiateTravel() {
		if (selectedIndex >= 0 && selectedIndex < destinations.size()) {
			PlanetDestination dest = destinations.get(selectedIndex);
			if (dest.unlocked && dest.dimension != null && this.minecraft.player.level().dimension() != dest.dimension) {
				NetworkHandler.sendToServer(new TravelToPlanetC2S(dest.dimension));
				this.onClose();
			}
		}
	}

	@Override
	public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		graphics.blit(MENU_TEXTURE, guiLeft, guiTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);

		drawCenteredStringWithBorder(graphics,
				Component.translatable("gui.dragonminez.spacepod.title"),
				this.width / 2, guiTop + 18, 0xFFFFD700);

		renderPlanetList(graphics, mouseX, mouseY);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private void renderPlanetList(GuiGraphics graphics, int mouseX, int mouseY) {
		int listLeft = guiLeft + 10;
		int listTop = guiTop + 35;
		int listWidth = PANEL_WIDTH - 25;
		int listHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;

		graphics.enableScissor(listLeft, listTop, listLeft + listWidth, listTop + listHeight);

		for (int i = 0; i < destinations.size(); i++) {
			int itemY = listTop + (i * ITEM_HEIGHT) - (scrollOffset * ITEM_HEIGHT);

			if (itemY + ITEM_HEIGHT < listTop || itemY > listTop + listHeight) continue;

			PlanetDestination dest = destinations.get(i);
			boolean isSelected = (i == selectedIndex);

			if (dest.unlocked) {
				boolean isHovered = mouseX >= listLeft && mouseX < listLeft + listWidth &&
						mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;

				int color = isSelected ? 0x80D4AF37 : (isHovered ? 0x80555555 : 0x00000000);
				graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, color);

				if (isSelected) {
					graphics.renderOutline(listLeft, itemY, listWidth, ITEM_HEIGHT, 0xFFFFD700);
				}
			} else {
				graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, 0x30000000);
			}

			RenderSystem.setShaderTexture(0, ICONS_TEXTURE);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

			int u = dest.unlocked ? ICON_X_COLOR : ICON_X_GRAY;
			int v = ICON_Y_START + (dest.iconIndex * ICON_Y_STEP);

			int iconYCentered = itemY + (ITEM_HEIGHT - ICON_SIZE) / 2;

			graphics.blit(ICONS_TEXTURE, listLeft + 5, iconYCentered, u, v, ICON_SIZE, ICON_SIZE, 256, 256);

			Component textToDraw;
			int textColor;

			if (dest.unlocked) {
				textToDraw = Component.translatable(dest.nameKey).withStyle(ChatFormatting.BOLD);
				textColor = 0x20E0FF;
			} else {
				textToDraw = Component.literal("???").withStyle(ChatFormatting.BOLD);
				textColor = 0x747678;
			}

			drawStringWithBorder(graphics, textToDraw, listLeft + 25, itemY + 8, textColor);
		}

		graphics.disableScissor();

		if (maxScroll > 0) {
			renderScrollbar(graphics, listTop, listHeight);
		}
	}

	private void renderScrollbar(GuiGraphics graphics, int listTop, int listHeight) {
		int scrollBarX = guiLeft + PANEL_WIDTH - 12;
		int scrollBarHeight = listHeight;

		graphics.fill(scrollBarX, listTop, scrollBarX + 3, listTop + scrollBarHeight, 0xFF333333);

		int totalItems = destinations.size();
		float scrollPercent = (float) scrollOffset / maxScroll;
		float visiblePercent = (float) MAX_VISIBLE_ITEMS / totalItems;
		int indicatorHeight = Math.max(20, (int) (scrollBarHeight * visiblePercent));
		int indicatorY = listTop + (int) ((scrollBarHeight - indicatorHeight) * scrollPercent);

		graphics.fill(scrollBarX, indicatorY, scrollBarX + 3, indicatorY + indicatorHeight, 0xFFAAAAAA);
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

			if (index >= 0 && index < destinations.size()) {
				PlanetDestination dest = destinations.get(index);

				if (dest.unlocked) {
					selectDestination(index);
					Minecraft.getInstance().getSoundManager().play(
							SimpleSoundInstance.forUI(MainSounds.UI_MENU_SWITCH.get(), 1.0F)
					);
				}
				return true;
			}
		}

		if (maxScroll > 0 && mouseX >= listLeft + listWidth && mouseX <= guiLeft + PANEL_WIDTH) {
			this.isScrolling = true;
			return true;
		}

		return false;
	}

	private void selectDestination(int index) {
		if (this.selectedIndex == index) return;
		this.selectedIndex = index;
		this.travelButton.visible = true;
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

	private static class PlanetDestination {
		String nameKey;
		ResourceKey<Level> dimension;
		int iconIndex;
		boolean unlocked;

		public PlanetDestination(String nameKey, ResourceKey<Level> dimension, int iconIndex, boolean unlocked) {
			this.nameKey = nameKey;
			this.dimension = dimension;
			this.iconIndex = iconIndex;
			this.unlocked = unlocked;
		}
	}
}