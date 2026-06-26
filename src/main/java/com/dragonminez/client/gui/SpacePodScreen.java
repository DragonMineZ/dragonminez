package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.TravelToPlanetC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.spacepod.SpacePodDestinationDefinition;
import com.dragonminez.common.spacepod.SpacePodDestinationRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SpacePodScreen extends ScaledScreen {

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
	private float targetScroll = 0;
	private float currentScroll = 0;
	private float maxScroll = 0;
	private boolean isScrolling = false;

	private TexturedTextButton travelButton;

	public SpacePodScreen() {
		super(Component.literal("Space Pod").withStyle(Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth"))));
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (getUiWidth() - PANEL_WIDTH) / 2;
		this.guiTop = (getUiHeight() - PANEL_HEIGHT) / 2;

		loadDestinations();

		this.travelButton = new TexturedTextButton.Builder()
				.position(guiLeft + (PANEL_WIDTH - 80) / 2, getUiHeight() - 30)
				.size(74, 20)
				.texture(BUTTON_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.travel"))
				.onPress(btn -> initiateTravel())
				.build();

		this.travelButton.visible = false;
		this.addRenderableWidget(travelButton);
	}

	private void loadDestinations() {
		destinations.clear();
		if (this.minecraft == null || this.minecraft.player == null) {
			return;
		}

		for (SpacePodDestinationDefinition definition : SpacePodDestinationRegistry.getClientDestinations()) {
			boolean unlocked = definition.unlockRules().test(this.minecraft.player);
			if (!unlocked && !definition.showWhenLocked()) {
				continue;
			}

			ResourceLocation iconTexture = definition.iconTexture() != null ? ResourceLocation.tryParse(definition.iconTexture()) : null;
			destinations.add(new PlanetDestination(
					definition.id(),
					definition.name(),
					definition.translate(),
					definition.dimension(),
					definition.iconIndex(),
					iconTexture,
					unlocked
			));
		}
	}

	private void initiateTravel() {
		if (selectedIndex >= 0 && selectedIndex < destinations.size()) {
			PlanetDestination dest = destinations.get(selectedIndex);
			if (dest.unlocked && dest.dimensionId != null && !dest.dimensionId.isBlank() && dest.id != null && !dest.id.isBlank()) {
				String currentDimensionId = this.minecraft.player.level().dimension().location().toString();
				if (!currentDimensionId.equals(dest.dimensionId)) {
					NetworkHandler.sendToServer(new TravelToPlanetC2S(dest.id));
					this.onClose();
				}
			}
		}
	}

	@Override
	public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);

		int uiMouseX = (int) toUiX(mouseX);
		int uiMouseY = (int) toUiY(mouseY);

		beginUiScale(graphics);

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		graphics.blit(MENU_TEXTURE, guiLeft, guiTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.spacepod.title"),
				getUiWidth() / 2, guiTop + 18, 0xFFFFD700);

		renderPlanetList(graphics, uiMouseX, uiMouseY);

		super.render(graphics, uiMouseX, uiMouseY, partialTick);

		endUiScale(graphics);
	}

	private void renderPlanetList(GuiGraphics graphics, int uiMouseX, int uiMouseY) {
		int listLeft = guiLeft + 10;
		int listTop = guiTop + 35;
		int listWidth = PANEL_WIDTH - 25;
		int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;
		int totalHeight = destinations.size() * ITEM_HEIGHT;

		maxScroll = Math.max(0, totalHeight - viewHeight);
		targetScroll = Mth.clamp(targetScroll, 0, maxScroll);
		float tickDelta = Minecraft.getInstance().getDeltaFrameTime();
		currentScroll = Mth.lerp(tickDelta * 0.4f, currentScroll, targetScroll);

		int scLeft = toScreenCoord(listLeft);
		int scTop = toScreenCoord(listTop);
		int scRight = toScreenCoord(listLeft + listWidth);
		int scBottom = toScreenCoord(listTop + viewHeight);

		graphics.enableScissor(scLeft, scTop, scRight, scBottom);
		graphics.pose().pushPose();
		graphics.pose().translate(0, -currentScroll, 0);

		for (int i = 0; i < destinations.size(); i++) {
			int itemY = listTop + (i * ITEM_HEIGHT);

			if (itemY + ITEM_HEIGHT >= listTop + currentScroll && itemY <= listTop + viewHeight + currentScroll) {
				PlanetDestination dest = destinations.get(i);
				boolean isSelected = (i == selectedIndex);

				if (dest.unlocked) {
					boolean isHovered = uiMouseX >= listLeft && uiMouseX < listLeft + listWidth &&
							uiMouseY >= itemY - currentScroll && uiMouseY < itemY + ITEM_HEIGHT - currentScroll;

					int color = isSelected ? 0x80D4AF37 : (isHovered ? 0x80555555 : 0x00000000);
					graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, color);

					if (isSelected) {
						graphics.renderOutline(listLeft, itemY, listWidth, ITEM_HEIGHT, 0xFFFFD700);
					}
				} else {
					graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, 0x30000000);
				}

				int iconYCentered = itemY + (ITEM_HEIGHT - ICON_SIZE) / 2;
				renderDestinationIcon(graphics, dest, listLeft + 5, iconYCentered);

				Component textToDraw;
				int textColor;

				if (dest.unlocked) {
					textToDraw = destinationName(dest).copy().withStyle(ChatFormatting.BOLD);
					textColor = 0x20E0FF;
				} else {
					textToDraw = txt("???").withStyle(ChatFormatting.BOLD);
					textColor = 0x747678;
				}

				TextUtil.drawStringWithBorder(graphics, this.font, textToDraw, listLeft + 25, itemY + 8, textColor);
			}
		}

		graphics.pose().popPose();
		graphics.disableScissor();

		if (maxScroll > 0) {
			renderScrollbar(graphics, listTop, viewHeight, totalHeight);
		}
	}

	private void renderDestinationIcon(GuiGraphics graphics, PlanetDestination dest, int x, int y) {
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		if (dest.iconTexture != null) {
			graphics.blit(dest.iconTexture, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
			return;
		}

		int iconIndex = dest.iconIndex != null ? dest.iconIndex : 0;
		int u = dest.unlocked ? ICON_X_COLOR : ICON_X_GRAY;
		int v = ICON_Y_START + (iconIndex * ICON_Y_STEP);
		graphics.blit(ICONS_TEXTURE, x, y, u, v, ICON_SIZE, ICON_SIZE, 256, 256);
	}

	private Component destinationName(PlanetDestination destination) {
		return destination.translate ? tr(destination.name) : txt(destination.name);
	}

	private void renderScrollbar(GuiGraphics graphics, int listTop, int viewHeight, int totalHeight) {
		int scrollBarX = guiLeft + PANEL_WIDTH - 12;

		graphics.fill(scrollBarX, listTop, scrollBarX + 3, listTop + viewHeight, 0xFF333333);

		float scrollPercent = currentScroll / maxScroll;
		float visiblePercent = (float) viewHeight / totalHeight;
		int indicatorHeight = Math.max(20, (int) (viewHeight * visiblePercent));
		int indicatorY = listTop + (int) ((viewHeight - indicatorHeight) * scrollPercent);

		graphics.fill(scrollBarX, indicatorY, scrollBarX + 3, indicatorY + indicatorHeight, 0xFFAAAAAA);
	}

	private float calculateScrollPercent(double uiY, int startY, int viewHeight) {
		float percent = (float)(uiY - startY) / viewHeight;
		return Mth.clamp(percent, 0.0f, 1.0f);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;

		double uiX = toUiX(mouseX);
		double uiY = toUiY(mouseY);

		int listLeft = guiLeft + 10;
		int listTop = guiTop + 35;
		int listWidth = PANEL_WIDTH - 25;
		int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;

		if (maxScroll > 0 && uiX >= listLeft + listWidth && uiX <= guiLeft + PANEL_WIDTH &&
				uiY >= listTop && uiY <= listTop + viewHeight) {
			this.isScrolling = true;
			targetScroll = calculateScrollPercent(uiY, listTop, viewHeight) * maxScroll;
			return true;
		}

		if (uiX >= listLeft && uiX < listLeft + listWidth && uiY >= listTop && uiY <= listTop + viewHeight) {
			int relativeY = (int) (uiY - listTop + currentScroll);
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
			targetScroll = (float) Mth.clamp(targetScroll - (Math.signum(delta) * ITEM_HEIGHT * 2), 0, maxScroll);
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
			double uiY = toUiY(mouseY);
			int listTop = guiTop + 35;
			int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;
			targetScroll = calculateScrollPercent(uiY, listTop, viewHeight) * maxScroll;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static class PlanetDestination {
		String id;
		String name;
		boolean translate;
		String dimensionId;
		Integer iconIndex;
		ResourceLocation iconTexture;
		boolean unlocked;

		public PlanetDestination(String id, String name, boolean translate, String dimensionId, Integer iconIndex, ResourceLocation iconTexture, boolean unlocked) {
			this.id = id;
			this.name = name;
			this.translate = translate;
			this.dimensionId = dimensionId;
			this.iconIndex = iconIndex;
			this.iconTexture = iconTexture;
			this.unlocked = unlocked;
		}
	}
}