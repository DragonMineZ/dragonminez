package com.dragonminez.client.gui;

import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.network.C2S.GrantWishC2S;
import com.dragonminez.common.network.C2S.RequestReviveTargetsC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ReviveTargetsS2C;
import com.dragonminez.common.wish.Wish;
import com.dragonminez.common.wish.WishManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class WishesScreen extends ScaledScreen {

	private static final ResourceLocation MENU_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");

	private static final int PANEL_WIDTH = 141;
	private static final int PANEL_HEIGHT = 213;
	private static final int ITEM_HEIGHT = 20;
	private static final int MAX_VISIBLE_ITEMS = 8;
	private static final int SELECTED_FILL = 0x80D4AF37;
	private static final int HOVER_FILL = 0x80555555;
	private static final int SELECTED_OUTLINE = 0xFFFFD700;
	private static final int BADGE_COLOR = 0xFFFFE680;
	private static final int DISABLED_TEXT = 0x8A8A8A;

	private final String dragonType;
	private final int maxWishesToSelect;
	private final List<Wish> availableWishes;
	private final List<Integer> selectedIndices = new ArrayList<>();
	private final List<List<UUID>> selectedTargets = new ArrayList<>();
	private final List<ReviveTargetsS2C.Entry> reviveTargets = new ArrayList<>();

	private boolean pickerOpen;
	private int pickerSelection = -1;
	private final List<UUID> pickerPicks = new ArrayList<>();

	private int guiLeft, guiTop;
	private float targetScroll = 0;
	private float currentScroll = 0;
	private float maxScroll = 0;
	private boolean isScrolling = false;

	private TexturedTextButton confirmButton;

	public WishesScreen(String dragonType, int wishCount) {
		super(Component.literal("Wishes").withStyle(Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth"))));
		this.dragonType = dragonType;
		this.maxWishesToSelect = wishCount;
		this.availableWishes = WishManager.getClientWishes(dragonType);
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (getUiWidth() - PANEL_WIDTH) / 2;
		this.guiTop = (getUiHeight() - PANEL_HEIGHT) / 2;

		this.confirmButton = new TexturedTextButton.Builder()
				.position(guiLeft + (PANEL_WIDTH - 80) / 2, getUiHeight() - 30)
				.size(74, 20)
				.texture(BUTTON_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.customization.select"))
				.onPress(btn -> onConfirmPressed())
				.build();

		this.addRenderableWidget(confirmButton);
		refreshConfirmButton();

		if (availableWishes.stream().anyMatch(wish -> wish.getMaxTargets() > 0)) {
			NetworkHandler.sendToServer(new RequestReviveTargetsC2S());
		}
	}

	public void setReviveTargets(List<ReviveTargetsS2C.Entry> entries) {
		reviveTargets.clear();
		reviveTargets.addAll(entries);
		for (int i = selectedIndices.size() - 1; i >= 0; i--) {
			if (needsTargets(availableWishes.get(selectedIndices.get(i))) && reviveTargets.isEmpty()) removeSelectionAt(i);
		}
		if (pickerOpen) {
			pickerPicks.removeIf(id -> reviveTargets.stream().noneMatch(entry -> entry.id().equals(id)));
			if (reviveTargets.isEmpty()) closePicker();
		}
		refreshConfirmButton();
	}

	private boolean needsTargets(Wish wish) {
		return wish.getMaxTargets() > 0;
	}

	private void onConfirmPressed() {
		if (pickerOpen) {
			if (pickerPicks.isEmpty()) return;
			selectedTargets.set(pickerSelection, new ArrayList<>(pickerPicks));
			closePicker();
			refreshConfirmButton();
			openNextPickerOrConfirm();
			return;
		}
		if (selectedIndices.size() != maxWishesToSelect) return;
		openNextPickerOrConfirm();
	}

	private void openNextPickerOrConfirm() {
		for (int i = 0; i < selectedIndices.size(); i++) {
			Wish wish = availableWishes.get(selectedIndices.get(i));
			if (needsTargets(wish) && selectedTargets.get(i).isEmpty()) {
				openPicker(i);
				return;
			}
		}
		NetworkHandler.sendToServer(new GrantWishC2S(dragonType, new ArrayList<>(selectedIndices), new ArrayList<>(selectedTargets)));
		this.onClose();
	}

	private void openPicker(int selection) {
		pickerOpen = true;
		pickerSelection = selection;
		pickerPicks.clear();
		targetScroll = 0;
		currentScroll = 0;
		refreshConfirmButton();
	}

	private void closePicker() {
		pickerOpen = false;
		pickerSelection = -1;
		pickerPicks.clear();
		targetScroll = 0;
		currentScroll = 0;
	}

	private void refreshConfirmButton() {
		if (pickerOpen) {
			confirmButton.visible = true;
			confirmButton.active = !pickerPicks.isEmpty();
			confirmButton.setMessage(tr("gui.dragonminez.wishes.revive_confirm"));
		} else {
			confirmButton.visible = selectedIndices.size() == maxWishesToSelect;
			confirmButton.active = true;
			confirmButton.setMessage(tr("gui.dragonminez.customization.select"));
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);

		int uiMouseX = (int) toUiX(mouseX);
		int uiMouseY = (int) toUiY(mouseY);

		beginUiScale(graphics);

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		HudRender.blit(graphics, MENU_TEXTURE, guiLeft, guiTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);

		if (pickerOpen) {
			Wish wish = availableWishes.get(selectedIndices.get(pickerSelection));
			TextUtil.drawCenteredStringWithBorder(graphics, this.font,
					tr("gui.dragonminez.wishes.revive_title", pickerPicks.size(), wish.getMaxTargets()),
					getUiWidth() / 2, guiTop + 18, 0xFFFFD700);
			renderTargetList(graphics, uiMouseX, uiMouseY);
		} else {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font,
					tr("gui.dragonminez.wishes_title", selectedIndices.size(), maxWishesToSelect),
					getUiWidth() / 2, guiTop + 18, 0xFFFFD700);
			renderWishesList(graphics, uiMouseX, uiMouseY);
		}

		super.render(graphics, uiMouseX, uiMouseY, partialTick);

		if (!pickerOpen) renderTooltip(graphics, uiMouseX, uiMouseY);

		endUiScale(graphics);
	}

	private int listLeft() { return guiLeft + 10; }
	private int listTop() { return guiTop + 35; }
	private int listWidth() { return PANEL_WIDTH - 25; }
	private int viewHeight() { return MAX_VISIBLE_ITEMS * ITEM_HEIGHT; }

	private void updateScroll(int rowCount) {
		int totalHeight = rowCount * ITEM_HEIGHT;
		maxScroll = Math.max(0, totalHeight - viewHeight());
		targetScroll = Mth.clamp(targetScroll, 0, maxScroll);
		currentScroll = Mth.lerp(frameEase(), currentScroll, targetScroll);
	}

	private void renderWishesList(GuiGraphics graphics, int uiMouseX, int uiMouseY) {
		int listLeft = listLeft();
		int listTop = listTop();
		int listWidth = listWidth();
		int viewHeight = viewHeight();
		updateScroll(availableWishes.size());

		graphics.enableScissor(toScreenCoord(listLeft), toScreenCoord(listTop), toScreenCoord(listLeft + listWidth), toScreenCoord(listTop + viewHeight));
		graphics.pose().pushPose();
		graphics.pose().translate(0, -currentScroll, 0);

		for (int i = 0; i < availableWishes.size(); i++) {
			int itemY = listTop + (i * ITEM_HEIGHT);
			if (itemY + ITEM_HEIGHT < listTop + currentScroll || itemY > listTop + viewHeight + currentScroll) continue;

			Wish wish = availableWishes.get(i);
			String badge = badgeFor(i);
			boolean isSelected = !badge.isEmpty();
			boolean selectable = !needsTargets(wish) || !reviveTargets.isEmpty();
			boolean isHovered = uiMouseX >= listLeft && uiMouseX < listLeft + listWidth &&
					uiMouseY >= itemY - currentScroll && uiMouseY < itemY + ITEM_HEIGHT - currentScroll;

			int color = isSelected ? SELECTED_FILL : (isHovered ? HOVER_FILL : 0);
			graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, color);

			TextUtil.drawStringWithBorder(graphics, this.font, tr(wish.getName()), listLeft + 5, itemY + 6, selectable ? 0xFFFFFF : DISABLED_TEXT);

			if (isSelected) {
				graphics.renderOutline(listLeft, itemY, listWidth, ITEM_HEIGHT, SELECTED_OUTLINE);
				drawBadge(graphics, badge, listLeft + listWidth - 3, itemY + 2);
			}
		}

		graphics.pose().popPose();
		graphics.disableScissor();
		renderScrollbar(graphics, availableWishes.size());
	}

	private void renderTargetList(GuiGraphics graphics, int uiMouseX, int uiMouseY) {
		int listLeft = listLeft();
		int listTop = listTop();
		int listWidth = listWidth();
		int viewHeight = viewHeight();
		updateScroll(reviveTargets.size());

		if (reviveTargets.isEmpty()) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.wishes.revive_nobody"),
					getUiWidth() / 2, listTop + 10, 0xFFAAAAAA);
			return;
		}

		graphics.enableScissor(toScreenCoord(listLeft), toScreenCoord(listTop), toScreenCoord(listLeft + listWidth), toScreenCoord(listTop + viewHeight));
		graphics.pose().pushPose();
		graphics.pose().translate(0, -currentScroll, 0);

		for (int i = 0; i < reviveTargets.size(); i++) {
			int itemY = listTop + (i * ITEM_HEIGHT);
			if (itemY + ITEM_HEIGHT < listTop + currentScroll || itemY > listTop + viewHeight + currentScroll) continue;

			ReviveTargetsS2C.Entry entry = reviveTargets.get(i);
			boolean isSelected = pickerPicks.contains(entry.id());
			boolean isHovered = uiMouseX >= listLeft && uiMouseX < listLeft + listWidth &&
					uiMouseY >= itemY - currentScroll && uiMouseY < itemY + ITEM_HEIGHT - currentScroll;

			int color = isSelected ? SELECTED_FILL : (isHovered ? HOVER_FILL : 0);
			graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, color);
			TextUtil.drawStringWithBorder(graphics, this.font, txt(entry.name()), listLeft + 5, itemY + 6, 0xFFFFFF);
			if (isSelected) graphics.renderOutline(listLeft, itemY, listWidth, ITEM_HEIGHT, SELECTED_OUTLINE);
		}

		graphics.pose().popPose();
		graphics.disableScissor();
		renderScrollbar(graphics, reviveTargets.size());
	}

	private void drawBadge(GuiGraphics graphics, String badge, int right, int top) {
		graphics.pose().pushPose();
		graphics.pose().translate(right, top, 0);
		graphics.pose().scale(0.75f, 0.75f, 1.0f);
		int width = this.font.width(badge);
		TextUtil.drawStringWithBorder(graphics, this.font, txt(badge), -width, 0, BADGE_COLOR);
		graphics.pose().popPose();
	}

	private String badgeFor(int wishIndex) {
		StringBuilder badge = new StringBuilder();
		for (int slot = 0; slot < selectedIndices.size(); slot++) {
			if (selectedIndices.get(slot) != wishIndex) continue;
			if (badge.length() > 0) badge.append(" - ");
			badge.append(slot + 1);
		}
		return badge.toString();
	}

	private void renderScrollbar(GuiGraphics graphics, int rowCount) {
		if (maxScroll <= 0) return;
		int viewHeight = viewHeight();
		int totalHeight = rowCount * ITEM_HEIGHT;
		int scrollBarX = guiLeft + PANEL_WIDTH - 12;
		int scrollBarY = listTop();

		graphics.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + viewHeight, 0xFF333333);

		float scrollPercent = currentScroll / maxScroll;
		float visiblePercent = (float) viewHeight / totalHeight;
		int indicatorHeight = Math.max(20, (int) (viewHeight * visiblePercent));
		int indicatorY = scrollBarY + (int) ((viewHeight - indicatorHeight) * scrollPercent);

		graphics.fill(scrollBarX, indicatorY, scrollBarX + 3, indicatorY + indicatorHeight, 0xFFAAAAAA);
	}

	private void renderTooltip(GuiGraphics graphics, int uiMouseX, int uiMouseY) {
		int index = rowAt(uiMouseX, uiMouseY, availableWishes.size());
		if (index < 0) return;
		Wish wish = availableWishes.get(index);
		List<Component> description = List.of(tr(wish.getDescription()));
		List<Component> extras = needsTargets(wish) && reviveTargets.isEmpty()
				? List.of(tr("gui.dragonminez.wishes.revive_nobody").withStyle(net.minecraft.ChatFormatting.RED))
				: null;
		TextUtil.renderAdvancedTooltip(graphics, this.font, uiMouseX, uiMouseY, getUiWidth(), getUiHeight(),
				tr(wish.getName()), description, extras, 0xFFD700);
	}

	private int rowAt(double uiX, double uiY, int rowCount) {
		int listLeft = listLeft();
		int listTop = listTop();
		if (uiX < listLeft || uiX >= listLeft + listWidth() || uiY < listTop || uiY > listTop + viewHeight()) return -1;
		int index = (int) ((uiY - listTop + currentScroll) / ITEM_HEIGHT);
		return index >= 0 && index < rowCount ? index : -1;
	}

	private float calculateScrollPercent(double uiY, int startY, int viewHeight) {
		float percent = (float) (uiY - startY) / viewHeight;
		return Mth.clamp(percent, 0.0f, 1.0f);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;

		double uiX = toUiX(mouseX);
		double uiY = toUiY(mouseY);

		int listLeft = listLeft();
		int listTop = listTop();
		int listWidth = listWidth();
		int viewHeight = viewHeight();

		if (maxScroll > 0 && uiX >= listLeft + listWidth && uiX <= guiLeft + PANEL_WIDTH &&
				uiY >= listTop && uiY <= listTop + viewHeight) {
			this.isScrolling = true;
			targetScroll = calculateScrollPercent(uiY, listTop, viewHeight) * maxScroll;
			return true;
		}

		if (pickerOpen) {
			int index = rowAt(uiX, uiY, reviveTargets.size());
			if (index < 0) return false;
			togglePick(reviveTargets.get(index).id());
			playClick();
			return true;
		}

		int index = rowAt(uiX, uiY, availableWishes.size());
		if (index < 0) return false;
		if (button == 1) removeLastSelectionOf(index);
		else addSelection(index);
		playClick();
		return true;
	}

	private void playClick() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
			targetScroll = calculateScrollPercent(uiY, listTop(), viewHeight()) * maxScroll;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (pickerOpen && keyCode == 256) {
			closePicker();
			refreshConfirmButton();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void addSelection(int index) {
		Wish wish = availableWishes.get(index);
		if (needsTargets(wish) && reviveTargets.isEmpty()) return;
		boolean alreadyPicked = selectedIndices.contains(index);
		if (maxWishesToSelect == 1) {
			if (alreadyPicked) {
				removeLastSelectionOf(index);
				return;
			}
			selectedIndices.clear();
			selectedTargets.clear();
		} else if (alreadyPicked && !wish.isRepeatable()) {
			removeLastSelectionOf(index);
			return;
		}
		if (selectedIndices.size() >= maxWishesToSelect) return;
		selectedIndices.add(index);
		selectedTargets.add(new ArrayList<>());
		refreshConfirmButton();
	}

	private void removeLastSelectionOf(int index) {
		for (int slot = selectedIndices.size() - 1; slot >= 0; slot--) {
			if (selectedIndices.get(slot) == index) {
				removeSelectionAt(slot);
				break;
			}
		}
		refreshConfirmButton();
	}

	private void removeSelectionAt(int slot) {
		selectedIndices.remove(slot);
		selectedTargets.remove(slot);
	}

	private void togglePick(UUID id) {
		if (pickerPicks.remove(id)) {
			refreshConfirmButton();
			return;
		}
		Wish wish = availableWishes.get(selectedIndices.get(pickerSelection));
		if (pickerPicks.size() >= wish.getMaxTargets()) {
			if (wish.getMaxTargets() == 1) pickerPicks.clear();
			else return;
		}
		pickerPicks.add(id);
		refreshConfirmButton();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
