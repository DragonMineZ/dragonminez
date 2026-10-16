package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.util.ScrollbarState;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.DeleteMasterC2S;
import com.dragonminez.common.network.C2S.InstantTransmissionTravelC2S;
import com.dragonminez.common.network.C2S.InstantTransmissionTravelToPlayerC2S;
import com.dragonminez.common.network.ITTargetEntry;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
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
	private static final ResourceLocation ICONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/spaceshipicons.png");
	private static final int PANEL_WIDTH = 141;
	private static final int PANEL_HEIGHT = 213;
	private static final int ITEM_HEIGHT = 24;
	private static final int MAX_VISIBLE_ITEMS = 5;
	private static final int ICON_SIZE = 11;
	private static final int ICON_Y_START = 3;
	private static final int ICON_Y_STEP = 14;

	private final List<MasterEntry> destinations = new ArrayList<>();
	private int selectedIndex = -1;
	private final int skillLevel;
	private boolean playersTab;
	private int guiLeft, guiTop;
	private final ScrollbarState listScroll = new ScrollbarState().minThumb(20).step(ITEM_HEIGHT * 2);
	private TexturedTextButton travelButton;
	private CustomTextureButton deleteButton;

	public InstantTransmissionScreen(List<ITTargetEntry> entries, int skillLevel) {
		super(Component.literal("Instant Transmission").withStyle(Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth"))));
		this.skillLevel = skillLevel;
		loadDestinations(entries);
	}

	private void loadDestinations(List<ITTargetEntry> entries) {
		destinations.clear();
		for (ITTargetEntry entry : entries) destinations.add(new MasterEntry(entry));
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

	private List<MasterEntry> visibleDestinations() {
		return destinations.stream().filter(entry -> (entry.type == ITTargetEntry.Type.MASTER) != playersTab).toList();
	}

	private MasterEntry selectedDestination() {
		List<MasterEntry> visible = visibleDestinations();
		return selectedIndex >= 0 && selectedIndex < visible.size() ? visible.get(selectedIndex) : null;
	}

	private void deleteSelectedMaster() {
		MasterEntry dest = selectedDestination();
		if (dest == null || dest.type != ITTargetEntry.Type.MASTER) return;

		NetworkHandler.sendToServer(new DeleteMasterC2S(dest.id));
		destinations.remove(dest);
		clearSelection();
	}

	private void initiateTravel() {
		MasterEntry dest = selectedDestination();
		if (dest == null || !dest.reachable || !hasEnoughKi(dest)) return;
		if (dest.type == ITTargetEntry.Type.MASTER) NetworkHandler.sendToServer(new InstantTransmissionTravelC2S(dest.id));
		else NetworkHandler.sendToServer(new InstantTransmissionTravelToPlayerC2S(UUID.fromString(dest.id)));
		this.onClose();
	}

	@Override
	public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		int uiMouseX = (int) toUiX(mouseX);
		int uiMouseY = (int) toUiY(mouseY);
		beginUiScale(graphics);

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		HudRender.blit(graphics, MENU_TEXTURE, guiLeft, guiTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.transmission.title"), getUiWidth() / 2, guiTop + 18, 0xFFFFD700);
		renderTabs(graphics);
		renderDestinationList(graphics, uiMouseX, uiMouseY);
		renderKiRequired(graphics);

		MasterEntry selected = selectedDestination();
		if (travelButton != null) travelButton.active = selected != null && selected.reachable && hasEnoughKi(selected);
		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		endUiScale(graphics);
	}

	private void renderTabs(GuiGraphics graphics) {
		int left = guiLeft + 10;
		int top = guiTop + 29;
		renderTab(graphics, left, top, 59, tr("gui.dragonminez.transmission.npcs"), !playersTab);
		if (skillLevel >= 10) renderTab(graphics, left + 61, top, 59, tr("gui.dragonminez.transmission.players"), playersTab);
	}

	private void renderTab(GuiGraphics graphics, int x, int y, int width, Component label, boolean selected) {
		graphics.fill(x, y, x + width, y + 15, selected ? 0x80555555 : 0x40000000);
		if (selected) graphics.renderOutline(x, y, width, 15, 0xFFFFD700);
		graphics.drawCenteredString(font, label, x + width / 2, y + 4, selected ? 0xFFFFD700 : 0xFFB0B0B0);
	}

	private void renderDestinationList(GuiGraphics graphics, int uiMouseX, int uiMouseY) {
		List<MasterEntry> visible = visibleDestinations();
		int listLeft = guiLeft + 10;
		int listTop = guiTop + 49;
		int listWidth = PANEL_WIDTH - 25;
		int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;
		listScroll.layout(listLeft, listTop, listWidth, viewHeight, visible.size() * ITEM_HEIGHT).barAt(guiLeft + PANEL_WIDTH - 12);
		boolean overList = listScroll.isInView(uiMouseX, uiMouseY);
		double contentMouseY = listScroll.toContent(uiMouseY);
		listScroll.beginClip(graphics);

		for (int i = 0; i < visible.size(); i++) {
			int itemY = listTop + i * ITEM_HEIGHT;
			if (!listScroll.isVisible(itemY, ITEM_HEIGHT)) continue;
			MasterEntry dest = visible.get(i);
			boolean isSelected = i == selectedIndex;
			boolean isHovered = overList && contentMouseY >= itemY && contentMouseY < itemY + ITEM_HEIGHT;
			if (!dest.reachable) graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, 0x30000000);
			else if (isSelected || isHovered) graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, isSelected ? 0x80D4AF37 : 0x80555555);
			if (isSelected) graphics.renderOutline(listLeft, itemY, listWidth, ITEM_HEIGHT, 0xFFFFD700);

			int iconIndex = iconIndexForDimension(dest.dimension);
			HudRender.blit(graphics, ICONS_TEXTURE, listLeft + 5, itemY + (ITEM_HEIGHT - ICON_SIZE) / 2,
					3, ICON_Y_START + iconIndex * ICON_Y_STEP, ICON_SIZE, ICON_SIZE, 256, 256);
			Component name = fitName(dest.name, listWidth - 31).copy().withStyle(net.minecraft.ChatFormatting.BOLD);
			TextUtil.drawStringWithBorder(graphics, font, name, listLeft + 21, itemY + 8, dest.reachable ? colorForType(dest.type) : 0x747678);
		}

		listScroll.endClip(graphics);
		listScroll.renderBar(graphics, uiMouseX, uiMouseY);
	}

	private Component fitName(String name, int maxWidth) {
		if (font.width(name) <= maxWidth) return txt(name);
		return txt(font.plainSubstrByWidth(name, Math.max(0, maxWidth - font.width("…"))) + "…");
	}

	private void renderKiRequired(GuiGraphics graphics) {
		MasterEntry selected = selectedDestination();
		if (selected == null) return;
		int color = hasEnoughKi(selected) ? 0xFFFFFFFF : 0xFFFF5555;
		TextUtil.drawCenteredStringWithBorder(graphics, font,
				tr("gui.dragonminez.transmission.ki_required", selected.kiCost), getUiWidth() / 2, guiTop + 171, color);
	}

	private boolean hasEnoughKi(MasterEntry entry) {
		var player = Minecraft.getInstance().player;
		if (player == null) return false;
		if (player.isCreative() || player.isSpectator()) return true;
		return StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(data -> data.getResources().getCurrentEnergy() >= entry.kiCost).orElse(false);
	}

	private void clearSelection() {
		selectedIndex = -1;
		listScroll.reset();
		travelButton.visible = false;
		deleteButton.visible = false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		double uiX = toUiX(mouseX);
		double uiY = toUiY(mouseY);
		int tabsTop = guiTop + 29;
		int tabsLeft = guiLeft + 10;
		if (uiY >= tabsTop && uiY < tabsTop + 15 && uiX >= tabsLeft && uiX < tabsLeft + 120) {
			boolean nextPlayersTab = uiX >= tabsLeft + 60;
			if (nextPlayersTab && skillLevel < 10) return true;
			if (nextPlayersTab != playersTab) {
				playersTab = nextPlayersTab;
				clearSelection();
			}
			return true;
		}

		int listLeft = guiLeft + 10;
		int listTop = guiTop + 49;
		int listWidth = PANEL_WIDTH - 25;
		int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;
		if (listScroll.mouseClicked(uiX, uiY, button)) return true;
		if (uiX >= listLeft && uiX < listLeft + listWidth && uiY >= listTop && uiY < listTop + viewHeight) {
			int index = (int) (uiY - listTop + listScroll.scroll()) / ITEM_HEIGHT;
			List<MasterEntry> visible = visibleDestinations();
			if (index >= 0 && index < visible.size()) {
				if (!visible.get(index).reachable) return true;
				selectDestination(index);
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MainSounds.UI_MENU_SWITCH.get(), 1.0F));
				return true;
			}
		}
		return false;
	}

	private void selectDestination(int index) {
		if (selectedIndex == index) return;
		selectedIndex = index;
		MasterEntry selected = selectedDestination();
		travelButton.visible = true;
		deleteButton.visible = selected != null && selected.type == ITTargetEntry.Type.MASTER;
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

	private static int iconIndexForDimension(String dimension) {
		return switch (dimension) {
			case "dragonminez:namek" -> 1;
			case "dragonminez:otherworld", "dragonminez:time_chamber", "minecraft:the_end" -> 2;
			case "dragonminez:sacredkaiplanet" -> 3;
			case "dragonminez:cereal_planet" -> 4;
			case "dmzsuper:beerus_planet" -> 5;
			case "dragonminez:demon_realm", "minecraft:the_nether" -> 6;
			default -> 0;
		};
	}

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
		int kiCost;

		MasterEntry(ITTargetEntry entry) {
			type = entry.getType();
			id = entry.getId();
			name = entry.getName();
			dimension = entry.getDimension();
			reachable = entry.isReachable();
			kiCost = entry.getKiCost();
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
