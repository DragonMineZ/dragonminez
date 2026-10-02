package com.dragonminez.client.gui;

import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.util.ScrollbarState;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.ITTargetEntry;
import com.dragonminez.common.network.C2S.DeleteMasterC2S;
import com.dragonminez.common.network.C2S.InstantTransmissionTravelC2S;
import com.dragonminez.common.network.C2S.InstantTransmissionTravelToPlayerC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class InstantTransmissionScreen extends ScaledScreen {

	private static final ResourceLocation MENU_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");

	private static final int PANEL_WIDTH = 141;
	private static final int PANEL_HEIGHT = 213;
	private static final int ITEM_HEIGHT = 24;
	private static final int MAX_VISIBLE_ITEMS = 7;

	private final List<MasterEntry> destinations = new ArrayList<>();
	private int selectedIndex = -1;
	private final int skillLevel;
	private final String currentDimension;

	private int guiLeft, guiTop;
	private final ScrollbarState listScroll = new ScrollbarState().minThumb(20).step(ITEM_HEIGHT * 2);

	private TexturedTextButton travelButton;
	private CustomTextureButton deleteButton;

	public InstantTransmissionScreen(List<ITTargetEntry> entries, int skillLevel) {
		super(Component.literal("Instant Transmission").withStyle(Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth"))));
		this.skillLevel = skillLevel;
		this.currentDimension = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.level().dimension().location().toString() : "";
		loadDestinations(entries);
	}

	private void loadDestinations(List<ITTargetEntry> entries) {
		destinations.clear();
		for (ITTargetEntry entry : entries) destinations.add(new MasterEntry(entry.getType(), entry.getId(), entry.getName(), entry.getDimension(), entry.isReachable()));
		destinations.sort(Comparator.comparingInt((MasterEntry e) -> e.priority()).thenComparing(e -> e.name, String.CASE_INSENSITIVE_ORDER));
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (getUiWidth() - PANEL_WIDTH) / 2;
		this.guiTop = (getUiHeight() - PANEL_HEIGHT) / 2;

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

		this.deleteButton = new CustomTextureButton.Builder()
				.position(guiLeft + PANEL_WIDTH - 28, getUiHeight() - 25)
				.size(14, 11)
				.texture(BUTTON_TEXTURE)
				.textureCoords(10, 0, 10, 10)
				.textureSize(10, 10)
				.onPress(btn -> deleteSelectedMaster())
				.build();

		this.deleteButton.visible = false;
		this.addRenderableWidget(deleteButton);
	}

	private void deleteSelectedMaster() {
		if (selectedIndex < 0 || selectedIndex >= destinations.size()) return;
		MasterEntry dest = destinations.get(selectedIndex);
		if (dest.type != ITTargetEntry.Type.MASTER) return;

		NetworkHandler.sendToServer(new DeleteMasterC2S(dest.id));
		destinations.remove(selectedIndex);
		selectedIndex = -1;
		this.travelButton.visible = false;
		this.deleteButton.visible = false;
	}

	private void initiateTravel() {
		if (selectedIndex >= 0 && selectedIndex < destinations.size()) {
			MasterEntry dest = destinations.get(selectedIndex);
			if (dest.reachable) {
				if (dest.type == ITTargetEntry.Type.MASTER) NetworkHandler.sendToServer(new InstantTransmissionTravelC2S(dest.id));
				else NetworkHandler.sendToServer(new InstantTransmissionTravelToPlayerC2S(UUID.fromString(dest.id)));
				this.onClose();
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
		HudRender.blit(graphics, MENU_TEXTURE, guiLeft, guiTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.transmission.title"),
				getUiWidth() / 2, guiTop + 18, 0xFFFFD700);

		renderMasterList(graphics, uiMouseX, uiMouseY);
		super.render(graphics, uiMouseX, uiMouseY, partialTick);

		endUiScale(graphics);
	}

	private void renderMasterList(GuiGraphics graphics, int uiMouseX, int uiMouseY) {
		int listLeft = guiLeft + 10;
		int listTop = guiTop + 35;
		int listWidth = PANEL_WIDTH - 25;
		int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;

		listScroll.layout(listLeft, listTop, listWidth, viewHeight, destinations.size() * ITEM_HEIGHT).barAt(guiLeft + PANEL_WIDTH - 12);
		boolean overList = listScroll.isInView(uiMouseX, uiMouseY);
		double contentMouseY = listScroll.toContent(uiMouseY);
		listScroll.beginClip(graphics);

		for (int i = 0; i < destinations.size(); i++) {
			int itemY = listTop + (i * ITEM_HEIGHT);

			if (listScroll.isVisible(itemY, ITEM_HEIGHT)) {
				MasterEntry dest = destinations.get(i);
				boolean isSelected = (i == selectedIndex);

				if (dest.reachable) {
					boolean isHovered = overList && contentMouseY >= itemY && contentMouseY < itemY + ITEM_HEIGHT;

					int color = isSelected ? 0x80D4AF37 : (isHovered ? 0x80555555 : 0x00000000);
					graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, color);
					if (isSelected) graphics.renderOutline(listLeft, itemY, listWidth, ITEM_HEIGHT, 0xFFFFD700);
				} else {
					graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, 0x30000000);
				}

				Component textToDraw = txt(dest.name).copy().withStyle(ChatFormatting.BOLD);
				int textColor = dest.reachable ? colorForType(dest.type) : 0x747678;
				TextUtil.drawStringWithBorder(graphics, this.font, textToDraw, listLeft + 10, itemY + 8, textColor);
			}
		}

		listScroll.endClip(graphics);
		listScroll.renderBar(graphics, uiMouseX, uiMouseY);
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

		if (listScroll.mouseClicked(uiX, uiY, button)) return true;

		if (uiX >= listLeft && uiX < listLeft + listWidth && uiY >= listTop && uiY <= listTop + viewHeight) {
			int index = (int) (uiY - listTop + listScroll.scroll()) / ITEM_HEIGHT;
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
		this.deleteButton.visible = destinations.get(index).type == ITTargetEntry.Type.MASTER;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (listScroll.scrollWheel(delta)) return true;
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		listScroll.mouseReleased();
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (listScroll.mouseDragged(toUiX(mouseX), toUiY(mouseY))) return true;
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean isPauseScreen() { return false; }

	private static int colorForType(ITTargetEntry.Type type) {
		return switch (type) {
			case MASTER -> 0x20E0FF;
			case PARTY -> 0xFFD700;
			case EXTERNAL -> 0xFFFFFF;
		};
	}

	private static class MasterEntry {
		ITTargetEntry.Type type;
		String id;
		String name;
		String dimension;
		boolean reachable;

		public MasterEntry(ITTargetEntry.Type type, String id, String name, String dimension, boolean reachable) {
			this.type = type;
			this.id = id;
			this.name = name;
			this.dimension = dimension;
			this.reachable = reachable;
		}

		int priority() {
			return switch (type) {
				case MASTER -> 1;
				case PARTY -> 2;
				case EXTERNAL -> 3;
			};
		}
	}
}