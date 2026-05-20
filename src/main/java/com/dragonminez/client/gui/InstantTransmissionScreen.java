package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.InstantTransmissionTravelC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.character.MasterLocation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class InstantTransmissionScreen extends Screen {

	private static final ResourceLocation MENU_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private static final int PANEL_WIDTH = 141;
	private static final int PANEL_HEIGHT = 213;
	private static final int ITEM_HEIGHT = 24;
	private static final int MAX_VISIBLE_ITEMS = 7;

	private final List<MasterEntry> destinations = new ArrayList<>();
	private int selectedIndex = -1;
	private final int skillLevel;
	private final String currentDimension;

	private int guiLeft, guiTop;
	private float targetScroll = 0;
	private float currentScroll = 0;
	private float maxScroll = 0;
	private boolean isScrolling = false;

	private TexturedTextButton travelButton;

	public InstantTransmissionScreen(Map<String, MasterLocation> masters, int skillLevel) {
		super(Component.literal("Instant Transmission").withStyle(Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth"))));
		this.skillLevel = skillLevel;
		this.currentDimension = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.level().dimension().location().toString() : "";
		loadDestinations(masters);
	}

	private void loadDestinations(Map<String, MasterLocation> masters) {
		destinations.clear();
		for (MasterLocation master : masters.values()) {
			boolean isReachable = skillLevel >= 10 || master.getDimension().equals(currentDimension);
			destinations.add(new MasterEntry(master.getMasterId(), master.getDisplayName(), master.getDimension(), isReachable));
		}
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (this.width - PANEL_WIDTH) / 2;
		this.guiTop = (this.height - PANEL_HEIGHT) / 2;

		this.travelButton = new TexturedTextButton.Builder()
				.position(guiLeft + (PANEL_WIDTH - 80) / 2, guiTop + PANEL_HEIGHT - 35)
				.size(74, 20)
				.texture(BUTTON_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.travel"))
				.onPress(btn -> {
					if (Minecraft.getInstance().player != null)
						Minecraft.getInstance().player.playNotifySound(MainSounds.TP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
					initiateTravel();
				})
				.build();

		this.travelButton.visible = false;
		this.addRenderableWidget(travelButton);
	}

	private void initiateTravel() {
		if (selectedIndex >= 0 && selectedIndex < destinations.size()) {
			MasterEntry dest = destinations.get(selectedIndex);
			if (dest.reachable) {
				NetworkHandler.sendToServer(new InstantTransmissionTravelC2S(dest.id));
				this.onClose();
			}
		}
	}

	@Override
	public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		graphics.blit(MENU_TEXTURE, guiLeft, guiTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.transmission.title"),
				this.width / 2, guiTop + 18, 0xFFFFD700);

		renderMasterList(graphics, mouseX, mouseY);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private void renderMasterList(GuiGraphics graphics, int mouseX, int mouseY) {
		int listLeft = guiLeft + 10;
		int listTop = guiTop + 35;
		int listWidth = PANEL_WIDTH - 25;
		int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;
		int totalHeight = destinations.size() * ITEM_HEIGHT;

		maxScroll = Math.max(0, totalHeight - viewHeight);
		targetScroll = Mth.clamp(targetScroll, 0, maxScroll);
		float tickDelta = Minecraft.getInstance().getDeltaFrameTime();
		currentScroll = Mth.lerp(tickDelta * 0.4f, currentScroll, targetScroll);

		graphics.enableScissor(listLeft, listTop, listLeft + listWidth, listTop + viewHeight);
		graphics.pose().pushPose();
		graphics.pose().translate(0, -currentScroll, 0);

		for (int i = 0; i < destinations.size(); i++) {
			int itemY = listTop + (i * ITEM_HEIGHT);

			if (itemY + ITEM_HEIGHT >= listTop + currentScroll && itemY <= listTop + viewHeight + currentScroll) {
				MasterEntry dest = destinations.get(i);
				boolean isSelected = (i == selectedIndex);

				if (dest.reachable) {
					boolean isHovered = mouseX >= listLeft && mouseX < listLeft + listWidth &&
							mouseY >= itemY - currentScroll && mouseY < itemY + ITEM_HEIGHT - currentScroll;

					int color = isSelected ? 0x80D4AF37 : (isHovered ? 0x80555555 : 0x00000000);
					graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, color);
					if (isSelected) graphics.renderOutline(listLeft, itemY, listWidth, ITEM_HEIGHT, 0xFFFFD700);
				} else {
					graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, 0x30000000);
				}

				Component textToDraw = txt(dest.name).copy().withStyle(ChatFormatting.BOLD);
				int textColor = dest.reachable ? 0x20E0FF : 0x747678;
				TextUtil.drawStringWithBorder(graphics, this.font, textToDraw, listLeft + 10, itemY + 8, textColor);
			}
		}

		graphics.pose().popPose();
		graphics.disableScissor();

		if (maxScroll > 0) renderScrollbar(graphics, listTop, viewHeight, totalHeight);
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

	private float calculateScrollPercent(double mouseY, int startY, int viewHeight) {
		return Mth.clamp((float)(mouseY - startY) / viewHeight, 0.0f, 1.0f);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;

		int listLeft = guiLeft + 10;
		int listTop = guiTop + 35;
		int listWidth = PANEL_WIDTH - 25;
		int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;

		if (maxScroll > 0 && mouseX >= listLeft + listWidth && mouseX <= guiLeft + PANEL_WIDTH &&
				mouseY >= listTop && mouseY <= listTop + viewHeight) {
			this.isScrolling = true;
			targetScroll = calculateScrollPercent(mouseY, listTop, viewHeight) * maxScroll;
			return true;
		}

		if (mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= listTop && mouseY <= listTop + viewHeight) {
			int index = (int) (mouseY - listTop + currentScroll) / ITEM_HEIGHT;
			if (index >= 0 && index < destinations.size()) {
				MasterEntry dest = destinations.get(index);
				if (dest.reachable) {
					selectDestination(index);
					Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MainSounds.UI_MENU_SWITCH.get(), 1.0F));
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
			targetScroll = calculateScrollPercent(mouseY, guiTop + 35, MAX_VISIBLE_ITEMS * ITEM_HEIGHT) * maxScroll;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean isPauseScreen() { return false; }

	public MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	public MutableComponent txt(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	private static class MasterEntry {
		String id;
		String name;
		String dimension;
		boolean reachable;

		public MasterEntry(String id, String name, String dimension, boolean reachable) {
			this.id = id;
			this.name = name;
			this.dimension = dimension;
			this.reachable = reachable;
		}
	}
}