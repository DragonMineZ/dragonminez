package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.BaseMenuScreen;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class PartyMenuScreen extends BaseMenuScreen {

	private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final int ITEM_HEIGHT = 16;
	private static final int MAX_VISIBLE_ITEMS = 10;

	private enum Tab { SERVER, PARTY }
	private Tab currentTab = Tab.SERVER;

	private record PartyEntry(UUID id, String name, boolean isOnline, boolean isLeader) {}
	private List<PartyEntry> displayList = new ArrayList<>();
	private int selectedIndex = -1;

	private float targetScroll = 0;
	private float currentScroll = 0;
	private float maxScroll = 0;
	private boolean isDraggingScroll = false;

	private TexturedTextButton actionBtn;
	private CustomTextureButton prevBtn, nextBtn;

	public PartyMenuScreen() {
		super(Component.translatable("gui.dragonminez.party.title"));
	}

	@Override
	protected void init() {
		super.init();
		refreshPlayerList();
		initActionButtons();
	}

	@Override
	public void tick() {
		super.tick();
		if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getGameTime() % 40 == 0) refreshPlayerList();
	}

	private void refreshPlayerList() {
		if (Minecraft.getInstance().getConnection() == null || Minecraft.getInstance().player == null) return;

		UUID selectedId = null;
		if (selectedIndex >= 0 && selectedIndex < displayList.size()) selectedId = displayList.get(selectedIndex).id();
		List<PlayerInfo> onlinePlayers = new ArrayList<>(Minecraft.getInstance().getConnection().getOnlinePlayers());
		UUID localId = Minecraft.getInstance().player.getUUID();
		displayList.clear();

		if (currentTab == Tab.SERVER) {
			onlinePlayers.sort((p1, p2) -> {
				boolean isP1Local = p1.getProfile().getId().equals(localId);
				boolean isP2Local = p2.getProfile().getId().equals(localId);
				if (isP1Local && !isP2Local) return -1;
				if (!isP1Local && isP2Local) return 1;
				return p1.getProfile().getName().compareToIgnoreCase(p2.getProfile().getName());
			});

			for (PlayerInfo p : onlinePlayers) displayList.add(new PartyEntry(p.getProfile().getId(), p.getProfile().getName(), true, false));
		} else {
			StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(data -> {
				List<UUID> partyIds = data.getPlayerQuestData().getPartyMemberIds();
				UUID leaderId = data.getPlayerQuestData().getPartyLeaderId();

				if (partyIds == null || partyIds.isEmpty()) {
					partyIds = List.of(localId);
					leaderId = localId;
				}

				for (UUID memberId : partyIds) {
					PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(memberId);
					boolean isOnline = info != null;
					String name = isOnline ? info.getProfile().getName() : "Offline (" + memberId.toString().substring(0, 4) + ")";
					displayList.add(new PartyEntry(memberId, name, isOnline, memberId.equals(leaderId)));
				}

				displayList.sort((e1, e2) -> {
					if (e1.id().equals(localId)) return -1;
					if (e2.id().equals(localId)) return 1;
					return e1.name().compareToIgnoreCase(e2.name());
				});
			});
		}

		selectedIndex = -1;
		if (selectedId != null) {
			for (int i = 0; i < displayList.size(); i++) {
				PartyEntry entry = displayList.get(i);
				if (entry.id().equals(selectedId)) {
					if (entry.isOnline()) selectedIndex = i;
					break;
				}
			}
		}

		if (actionBtn != null) refreshActionButtons();
	}

	private void initActionButtons() {
		int rightPanelX = getUiWidth() - 158 + getRightPanelSwitchOffset(1.0f);
		int centerY = getUiHeight() / 2;
		int rightPanelY = centerY - 105;

		prevBtn = new CustomTextureButton.Builder()
				.position(rightPanelX + 20, rightPanelY + 183)
				.size(15, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(32, 0, 32, 14)
				.textureSize(8, 14)
				.onPress(btn -> shiftSelection(-1))
				.build();

		nextBtn = new CustomTextureButton.Builder()
				.position(rightPanelX + 116, rightPanelY + 183)
				.size(15, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(20, 0, 20, 14)
				.textureSize(8, 14)
				.onPress(btn -> shiftSelection(1))
				.build();

		actionBtn = new TexturedTextButton.Builder()
				.position(rightPanelX + 35, rightPanelY + 180)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.party.invite"))
				.onPress(btn -> executePlayerAction())
				.build();

		this.addRenderableWidget(prevBtn);
		this.addRenderableWidget(nextBtn);
		this.addRenderableWidget(actionBtn);

		refreshActionButtons();
	}

	private void updatePanelWidgetOffsets(int rightOffset) {
		int rightPanelX = getUiWidth() - 158 + rightOffset;

		if (prevBtn != null) prevBtn.setX(rightPanelX + 20);
		if (nextBtn != null) nextBtn.setX(rightPanelX + 116);
		if (actionBtn != null) actionBtn.setX(rightPanelX + 35);
	}

	private void shiftSelection(int direction) {
		if (displayList.isEmpty()) return;
		selectedIndex += direction;
		if (selectedIndex < 0) selectedIndex = displayList.size() - 1;
		if (selectedIndex >= displayList.size()) selectedIndex = 0;
		refreshActionButtons();
	}

	private void refreshActionButtons() {
		boolean validSelection = selectedIndex >= 0 && selectedIndex < displayList.size();
		prevBtn.active = validSelection && displayList.size() > 1;
		nextBtn.active = validSelection && displayList.size() > 1;

		if (validSelection && Minecraft.getInstance().player != null) {
			PartyEntry targetEntry = displayList.get(selectedIndex);
			boolean isSelf = targetEntry.id().equals(Minecraft.getInstance().player.getUUID());

			if (currentTab == Tab.SERVER) {
				actionBtn.visible = !isSelf;
				actionBtn.active = !isSelf && targetEntry.isOnline();
				actionBtn.setMessage(tr("gui.dragonminez.party.invite"));
			} else {
				actionBtn.visible = true;
				actionBtn.active = true;
				if (isSelf) actionBtn.setMessage(tr("gui.dragonminez.party.leave"));
				else actionBtn.setMessage(tr("gui.dragonminez.party.kick"));
			}
		} else {
			actionBtn.visible = false;
			actionBtn.active = false;
		}
	}

	private void executePlayerAction() {
		if (selectedIndex < 0 || selectedIndex >= displayList.size() || Minecraft.getInstance().player == null) return;

		PartyEntry target = displayList.get(selectedIndex);
		boolean isSelf = target.id().equals(Minecraft.getInstance().player.getUUID());
		String name = target.name();

		if (currentTab == Tab.SERVER) {
			if (!isSelf && target.isOnline()) Minecraft.getInstance().player.connection.sendCommand("dmzparty invite " + name);
		} else {
			if (isSelf) Minecraft.getInstance().player.connection.sendCommand("dmzparty leave");
			else Minecraft.getInstance().player.connection.sendCommand("dmzparty kick " + name);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (isNotAnimating()) this.renderBackground(graphics);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);
		applyZoom(graphics, partialTick);

		int leftOffset = getLeftPanelSwitchOffset(partialTick);
		int rightOffset = getRightPanelSwitchOffset(partialTick);

		updatePanelWidgetOffsets(rightOffset);

		int leftPanelX = 12 + leftOffset;
		int rightPanelX = getUiWidth() - 158 + rightOffset;
		int centerY = getUiHeight() / 2;
		int panelY = centerY - 105;

		renderPanels(graphics, leftPanelX, rightPanelX, panelY);
		renderPlayerList(graphics, leftPanelX, panelY, uiMouseX, uiMouseY);
		renderRightPanelDetails(graphics, rightPanelX, panelY);

		renderCentralModel(graphics, getUiWidth() / 2 + 5, getUiHeight() / 2 + 70, 75, uiMouseX, uiMouseY);

		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		endUiScale(graphics);
	}

	private void renderPanels(GuiGraphics graphics, int leftX, int rightX, int panelY) {
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		graphics.blit(MENU_BIG, leftX, panelY, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, leftX + 17, panelY + 10, 142, 22, 107, 21, 256, 256);

		graphics.blit(MENU_BIG, rightX, panelY, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, rightX + 17, panelY + 10, 142, 22, 107, 21, 256, 256);
		graphics.blit(MENU_BIG, rightX + 31, panelY + 77, 142, 0, 79, 21, 256, 256);

		RenderSystem.disableBlend();
	}

	private void renderPlayerList(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
		boolean isTabHovered = mouseX >= panelX + 17 && mouseX <= panelX + 124 && mouseY >= panelY + 10 && mouseY <= panelY + 31;
		int tabColor = isTabHovered ? 0xFFFFFF : 0xFFFFD700;

		Component tabText = Component.literal("< ").append(tr(currentTab == Tab.SERVER ? "gui.dragonminez.party.server" : "gui.dragonminez.party.party")).append(" >");
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tabText.copy().withStyle(ChatFormatting.BOLD), panelX + 70, panelY + 16, tabColor, 0x000000);

		int startY = panelY + 35;
		int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;
		int totalHeight = displayList.size() * ITEM_HEIGHT;

		maxScroll = Math.max(0, totalHeight - viewHeight);
		targetScroll = Mth.clamp(targetScroll, 0, maxScroll);
		currentScroll = Mth.lerp(Minecraft.getInstance().getDeltaFrameTime() * 0.4f, currentScroll, targetScroll);

		graphics.enableScissor(toScreenCoord(panelX + 5), toScreenCoord(startY), toScreenCoord(panelX + 135), toScreenCoord(startY + viewHeight));
		graphics.pose().pushPose();
		graphics.pose().translate(0, -currentScroll, 0);

		for (int i = 0; i < displayList.size(); i++) {
			PartyEntry entry = displayList.get(i);
			int itemY = startY + (i * ITEM_HEIGHT);

			if (itemY + ITEM_HEIGHT >= startY + currentScroll && itemY <= startY + viewHeight + currentScroll) {
				boolean isSelected = (i == selectedIndex);
				boolean isHovered = mouseX >= panelX + 10 && mouseX <= panelX + 120 && mouseY >= itemY - currentScroll && mouseY <= itemY + ITEM_HEIGHT - currentScroll;

				int color;
				if (currentTab == Tab.SERVER) {
					color = isSelected ? 0xFFFFAA00 : (isHovered ? 0xFFAAAAAA : 0xFFFFFFFF);
				} else {
					if (!entry.isOnline()) color = isSelected ? 0xFFCCCCCC : (isHovered ? 0xFFBBBBBB : 0xFFAAAAAA); // GRAY
					else if (entry.isLeader()) color = isSelected ? 0xFFFFEEAA : (isHovered ? 0xFFFFE066 : 0xFFFFD700); // GOLD
					else color = isSelected ? 0xFFFFFFCC : (isHovered ? 0xFFFFFFAA : 0xFFFFFF55); // YELLOW
				}

				String displayText = entry.name() + (entry.isLeader() ? " ⭐" : "");
				TextUtil.drawStringWithBorder(graphics, this.font, txt(displayText), panelX + 15, itemY + 4, color);
			}
		}

		graphics.pose().popPose();
		graphics.disableScissor();

		if (maxScroll > 0) {
			int scrollBarX = panelX + 130;
			graphics.fill(scrollBarX, startY, scrollBarX + 2, startY + viewHeight, 0xFF333333);
			float scrollPercent = currentScroll / maxScroll;
			float visiblePercent = (float) viewHeight / totalHeight;
			int indicatorHeight = Math.max(10, (int) (viewHeight * visiblePercent));
			int indicatorY = startY + (int) ((viewHeight - indicatorHeight) * scrollPercent);
			graphics.fill(scrollBarX, indicatorY, scrollBarX + 2, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}
	}

	private void renderRightPanelDetails(GuiGraphics graphics, int panelX, int panelY) {
		if (selectedIndex < 0 || selectedIndex >= displayList.size()) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt("???").withStyle(ChatFormatting.BOLD), panelX + 70, panelY + 16, 0xFFFFD700, 0x000000);
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.stats").withStyle(ChatFormatting.BOLD), panelX + 70, panelY + 84, 0x68CCFF, 0x000000);
			return;
		}

		PartyEntry targetEntry = displayList.get(selectedIndex);
		UUID targetId = targetEntry.id();
		String displayName = targetEntry.name() + (targetEntry.isLeader() ? " ⭐" : "");

		int headerColor = targetEntry.isOnline() ? 0xFFFFD700 : 0xFFAAAAAA;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(displayName).withStyle(ChatFormatting.BOLD), panelX + 70, panelY + 16, headerColor, 0x000000);

		Player targetPlayer = Minecraft.getInstance().level.getPlayerByUUID(targetId);
		int startY = panelY + 36;

		if (targetPlayer != null && targetEntry.isOnline()) {
			StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).ifPresent(data -> {
				int labelX = panelX + 20;
				int valueX = panelX + 65;

				TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.race").withStyle(style -> style.withBold(true)), labelX, startY, 0xD7FEF5, 0x000000);
				TextUtil.drawStringWithBorder(graphics, this.font, tr("race.dragonminez." + data.getCharacter().getRaceName()), valueX, startY, 0xFFFFFF, 0x000000);

				TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.class").withStyle(style -> style.withBold(true)), labelX, startY + 11, 0xD7FEF5, 0x000000);
				TextUtil.drawStringWithBorder(graphics, this.font, tr("class.dragonminez." + data.getCharacter().getCharacterClass()), valueX, startY + 11, 0xFFFFFF, 0x000000);

				TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.level").withStyle(style -> style.withBold(true)), labelX, startY + 22, 0xD7FEF5, 0x000000);
				TextUtil.drawStringWithBorder(graphics, this.font, txt(String.valueOf(data.getLevel())), valueX, startY + 22, 0xFFFFFF, 0x000000);

				int statsY = startY + 48;
				TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.stats").withStyle(ChatFormatting.BOLD), panelX + 70, statsY, 0x68CCFF, 0x000000);

				int r1 = statsY + 18;
				int r2 = statsY + 30;
				int r3 = statsY + 42;
				int r4 = statsY + 54;
				int r5 = statsY + 66;
				int r6 = statsY + 78;

				int statLabelX = panelX + 30;
				int statValueX = panelX + 60;

				TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.str").withStyle(style -> style.withBold(true)), statLabelX, r1, 0xD71432, 0x000000);
				TextUtil.drawStringWithBorder(graphics, this.font, txt(String.valueOf(data.getStats().getStrength())), statValueX, r1, 0xFFD7AB, 0x000000);

				TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.skp").withStyle(style -> style.withBold(true)), statLabelX, r2, 0xD71432, 0x000000);
				TextUtil.drawStringWithBorder(graphics, this.font, txt(String.valueOf(data.getStats().getStrikePower())), statValueX, r2, 0xFFD7AB, 0x000000);

				TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.res").withStyle(style -> style.withBold(true)), statLabelX, r3, 0xD71432, 0x000000);
				TextUtil.drawStringWithBorder(graphics, this.font, txt(String.valueOf(data.getStats().getResistance())), statValueX, r3, 0xFFD7AB, 0x000000);

				TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.vit").withStyle(style -> style.withBold(true)), statLabelX, r4, 0xD71432, 0x000000);
				TextUtil.drawStringWithBorder(graphics, this.font, txt(String.valueOf(data.getStats().getVitality())), statValueX, r4, 0xFFD7AB, 0x000000);

				TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.pwr").withStyle(style -> style.withBold(true)), statLabelX, r5, 0xD71432, 0x000000);
				TextUtil.drawStringWithBorder(graphics, this.font, txt(String.valueOf(data.getStats().getKiPower())), statValueX, r5, 0xFFD7AB, 0x000000);

				TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.ene").withStyle(style -> style.withBold(true)), statLabelX, r6, 0xD71432, 0x000000);
				TextUtil.drawStringWithBorder(graphics, this.font, txt(String.valueOf(data.getStats().getEnergy())), statValueX, r6, 0xFFD7AB, 0x000000);
			});
		} else {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.party.unavailable").withStyle(ChatFormatting.RED), panelX + 70, startY + 20, 0xFF5555, 0x000000);
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.party.out_of_range").withStyle(ChatFormatting.GRAY), panelX + 70, startY + 32, 0xAAAAAA, 0x000000);
		}
	}

	private void renderCentralModel(GuiGraphics graphics, int x, int y, int scale, float mouseX, float mouseY) {
		LivingEntity renderEntity = Minecraft.getInstance().player;

		if (selectedIndex >= 0 && selectedIndex < displayList.size()) {
			PartyEntry targetEntry = displayList.get(selectedIndex);
			if (targetEntry.isOnline()) {
				AbstractClientPlayer targetPlayer = (AbstractClientPlayer) Minecraft.getInstance().level.getPlayerByUUID(targetEntry.id());
				if (targetPlayer != null) renderEntity = targetPlayer;
			} else renderEntity = null;
		}

		if (renderEntity == null) return;

		int adjustedScale = getAdjustedModelScale(scale);

		float xRotation = (float) Math.atan((double) ((float) y - mouseY) / 40.0F);
		float yRotation = (float) Math.atan((double) ((float) x - mouseX) / 40.0F);

		Quaternionf pose = (new Quaternionf()).rotateZ((float) Math.PI);
		Quaternionf cameraOrientation = (new Quaternionf()).rotateX(xRotation * 20.0F * ((float) Math.PI / 180F));
		pose.mul(cameraOrientation);

		float yBodyRotO = renderEntity.yBodyRot;
		float yRotO = renderEntity.getYRot();
		float xRotO = renderEntity.getXRot();
		float yHeadRotO = renderEntity.yHeadRotO;
		float yHeadRot = renderEntity.yHeadRot;

		renderEntity.yBodyRot = 180.0F + yRotation * 20.0F;
		renderEntity.setYRot(180.0F + yRotation * 40.0F);
		renderEntity.setXRot(-xRotation * 20.0F);
		renderEntity.yHeadRot = renderEntity.getYRot();
		renderEntity.yHeadRotO = renderEntity.getYRot();

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 150.0D);
		InventoryScreen.renderEntityInInventory(graphics, x, y, adjustedScale, pose, cameraOrientation, renderEntity);
		graphics.pose().popPose();

		renderEntity.yBodyRot = yBodyRotO;
		renderEntity.setYRot(yRotO);
		renderEntity.setXRot(xRotO);
		renderEntity.yHeadRotO = yHeadRotO;
		renderEntity.yHeadRot = yHeadRot;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (maxScroll > 0) {
			targetScroll = Mth.clamp(targetScroll - ((float) Math.signum(delta) * ITEM_HEIGHT * 2), 0, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;

		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int leftPanelX = 12 + getLeftPanelSwitchOffset(1.0f);
		int centerY = getUiHeight() / 2;
		int panelY = centerY - 105;

		if (uiMouseX >= leftPanelX + 17 && uiMouseX <= leftPanelX + 124 && uiMouseY >= panelY + 10 && uiMouseY <= panelY + 31) {
			currentTab = currentTab == Tab.SERVER ? Tab.PARTY : Tab.SERVER;
			selectedIndex = -1;
			targetScroll = 0;
			refreshPlayerList();
			refreshActionButtons();
			if (Minecraft.getInstance().player != null) {
				Minecraft.getInstance().player.playSound(MainSounds.UI_MENU_SWITCH.get());
			}
			return true;
		}

		int startY = panelY + 35;
		int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;

		if (maxScroll > 0 && TextUtil.overScrollBar(uiMouseX, uiMouseY, leftPanelX + 130, 2, startY, viewHeight)) {
			isDraggingScroll = true;
			targetScroll = TextUtil.scrollFromBar(uiMouseY, startY, viewHeight, maxScroll);
			return true;
		}

		if (uiMouseX >= leftPanelX + 10 && uiMouseX <= leftPanelX + 120 && uiMouseY >= startY && uiMouseY <= startY + viewHeight) {
			int index = (int) ((uiMouseY - startY + currentScroll) / ITEM_HEIGHT);
			if (index >= 0 && index < displayList.size()) {
				selectedIndex = index;
				refreshActionButtons();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (isDraggingScroll && maxScroll > 0) {
			int panelY = (getUiHeight() / 2) - 105;
			int startY = panelY + 35;
			targetScroll = TextUtil.scrollFromBar(toUiY(mouseY), startY, MAX_VISIBLE_ITEMS * ITEM_HEIGHT, maxScroll);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (isDraggingScroll) {
			isDraggingScroll = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}
}