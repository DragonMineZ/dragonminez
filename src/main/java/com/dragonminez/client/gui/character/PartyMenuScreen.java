package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.BaseMenuScreen;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.PartyPackets;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

	private static final ResourceLocation CARD_BG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menunpc.png");
	private static final int CARD_SOURCE_WIDTH = 345;
	private static final int CARD_SOURCE_HEIGHT = 94;
	private static final int CARD_SHEET = 512;
	private static final int WELCOME_WIDTH = 300;
	private static final int WELCOME_HEIGHT = 94;

	private enum View { WELCOME, CREATE, JOIN, PARTY }
	private View currentView = View.WELCOME;

	private record PartyEntry(UUID id, String name, boolean isOnline, boolean isLeader, UUID partyId) {}
	private List<PartyEntry> displayList = new ArrayList<>();
	private int selectedIndex = -1;

	private float targetScroll = 0;
	private float currentScroll = 0;
	private float maxScroll = 0;
	private boolean isDraggingScroll = false;

	private TexturedTextButton actionBtn;
	private TexturedTextButton altBtn;
	private TexturedTextButton backBtn;
	private TexturedTextButton createBtn;
	private TexturedTextButton joinBtn;
	private CustomTextureButton prevBtn, nextBtn;

	public PartyMenuScreen() {
		super(Component.translatable("gui.dragonminez.party.title"));
	}

	@Override
	protected void init() {
		super.init();
		requestPartyStats();
		if (currentView == View.WELCOME && isInParty()) currentView = View.PARTY;
		else if (currentView == View.PARTY && !isInParty()) currentView = View.WELCOME;
		refreshPlayerList();
		initActionButtons();
	}

	private boolean isInParty() {
		if (Minecraft.getInstance().player == null) return false;
		return StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player)
				.map(data -> data.getPlayerQuestData().getActivePartyId() != null)
				.orElse(false);
	}

	private boolean isPartyLeader() {
		if (Minecraft.getInstance().player == null) return false;
		UUID self = Minecraft.getInstance().player.getUUID();
		return StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player)
				.map(data -> self.equals(data.getPlayerQuestData().getPartyLeaderId()))
				.orElse(false);
	}

	private List<PlayerQuestData.PartyInviteData> pendingInvites() {
		if (Minecraft.getInstance().player == null) return List.of();
		return StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player)
				.map(data -> data.getPlayerQuestData().getPendingPartyInvites())
				.orElse(List.of());
	}

	private void setView(View view) {
		currentView = view;
		selectedIndex = -1;
		targetScroll = 0;
		currentScroll = 0;
		refreshPlayerList();
		rebuildWidgets();
		if (Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.playSound(MainSounds.UI_MENU_SWITCH.get());
		}
	}

	@Override
	public void tick() {
		super.tick();

		if (currentView == View.JOIN && isInParty()) {
			setView(View.PARTY);
			return;
		}
		if (currentView == View.PARTY && !isInParty()) {
			setView(View.WELCOME);
			return;
		}

		if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getGameTime() % 40 == 0) {
			requestPartyStats();
			refreshPlayerList();
		}
	}

	private static void requestPartyStats() {
		com.dragonminez.common.network.NetworkHandler.sendToServer(
				new com.dragonminez.common.network.PartyPackets.RequestStatsC2S());
	}

	private void refreshPlayerList() {
		if (Minecraft.getInstance().getConnection() == null || Minecraft.getInstance().player == null) return;

		UUID selectedId = null;
		if (selectedIndex >= 0 && selectedIndex < displayList.size()) selectedId = displayList.get(selectedIndex).id();
		List<PlayerInfo> onlinePlayers = new ArrayList<>(Minecraft.getInstance().getConnection().getOnlinePlayers());
		UUID localId = Minecraft.getInstance().player.getUUID();
		displayList.clear();

		if (currentView == View.WELCOME) {
			selectedIndex = -1;
			if (actionBtn != null) refreshActionButtons();
			return;
		}

		if (currentView == View.JOIN) {
			for (PlayerQuestData.PartyInviteData invite : pendingInvites()) {
				displayList.add(new PartyEntry(invite.getPartyLeaderId(), invite.getInviterName(),
						true, true, invite.getPartyId()));
			}
		} else if (currentView == View.CREATE) {
			// Only other people: you are already in the party you are building.
			onlinePlayers.sort((p1, p2) -> p1.getProfile().getName().compareToIgnoreCase(p2.getProfile().getName()));

			for (PlayerInfo p : onlinePlayers) {
				if (p.getProfile().getId().equals(localId)) continue;
				displayList.add(new PartyEntry(p.getProfile().getId(), p.getProfile().getName(), true, false, null));
			}
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
					PartyPackets.MemberStats known = PartyStatsCache.get(memberId);
					boolean isOnline = info != null;

					String name;
					if (isOnline) name = info.getProfile().getName();
					else if (known != null && !known.name().isEmpty()) name = known.name();
					else name = "Offline (" + memberId.toString().substring(0, 4) + ")";

					displayList.add(new PartyEntry(memberId, name, isOnline, memberId.equals(leaderId), null));
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

		actionBtn = menuButton(rightPanelX + 35, rightPanelY + 180, "gui.dragonminez.party.invite",
				btn -> executePlayerAction());
		altBtn = menuButton(rightPanelX + 35, rightPanelY + 155, "gui.dragonminez.party.invite.reject",
				btn -> answerInvite(false));
		backBtn = menuButton(12 + getLeftPanelSwitchOffset(1.0f) + 35, rightPanelY + 180,
				"gui.dragonminez.party.back", btn -> goBack());

		int centreX = getUiWidth() / 2;
		int welcomeY = welcomeTop() + WELCOME_HEIGHT + 8;
		createBtn = menuButton(centreX - 78, welcomeY, "gui.dragonminez.party.create",
				btn -> setView(View.CREATE));
		joinBtn = menuButton(centreX + 4, welcomeY, "gui.dragonminez.party.join",
				btn -> setView(View.JOIN));

		this.addRenderableWidget(prevBtn);
		this.addRenderableWidget(nextBtn);
		this.addRenderableWidget(actionBtn);
		this.addRenderableWidget(altBtn);
		this.addRenderableWidget(backBtn);
		this.addRenderableWidget(createBtn);
		this.addRenderableWidget(joinBtn);

		refreshActionButtons();
	}

	private TexturedTextButton menuButton(int x, int y, String translationKey, Button.OnPress onPress) {
		return new TexturedTextButton.Builder()
				.position(x, y)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr(translationKey))
				.onPress(onPress)
				.build();
	}

	private void goBack() {
		if (currentView == View.CREATE && isInParty() && isPartyLeader()) {
			if (partyMemberCount() <= 1) {
				if (Minecraft.getInstance().player != null) {
					Minecraft.getInstance().player.connection.sendCommand("dmzparty disband");
				}
				setView(View.WELCOME);
				return;
			}
			setView(View.PARTY);
			return;
		}
		setView(isInParty() ? View.PARTY : View.WELCOME);
	}

	private int partyMemberCount() {
		if (Minecraft.getInstance().player == null) return 0;
		return StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player)
				.map(data -> data.getPlayerQuestData().getPartyMemberIds().size())
				.orElse(0);
	}

	private void answerInvite(boolean accept) {
		if (selectedIndex < 0 || selectedIndex >= displayList.size()) return;

		UUID partyId = displayList.get(selectedIndex).partyId();
		if (accept) {
			com.dragonminez.common.network.NetworkHandler.sendToServer(
					new com.dragonminez.common.network.C2S.AcceptPartyInviteC2S(false, partyId));
		} else {
			com.dragonminez.common.network.NetworkHandler.sendToServer(
					new com.dragonminez.common.network.C2S.RejectPartyInviteC2S(partyId));
		}
		selectedIndex = -1;
		refreshPlayerList();
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
		boolean welcome = currentView == View.WELCOME;
		boolean validSelection = selectedIndex >= 0 && selectedIndex < displayList.size();

		createBtn.visible = welcome;
		joinBtn.visible = welcome;
		joinBtn.active = !pendingInvites().isEmpty();

		backBtn.visible = currentView == View.CREATE || currentView == View.JOIN;
		prevBtn.visible = !welcome;
		nextBtn.visible = !welcome;
		prevBtn.active = validSelection && displayList.size() > 1;
		nextBtn.active = validSelection && displayList.size() > 1;

		altBtn.visible = currentView == View.JOIN && validSelection;
		altBtn.active = altBtn.visible;

		if (welcome || !validSelection || Minecraft.getInstance().player == null) {
			actionBtn.visible = false;
			actionBtn.active = false;
			return;
		}

		PartyEntry targetEntry = displayList.get(selectedIndex);
		boolean isSelf = targetEntry.id().equals(Minecraft.getInstance().player.getUUID());

		switch (currentView) {
			case CREATE -> {
				actionBtn.visible = !isSelf;
				actionBtn.active = !isSelf && targetEntry.isOnline();
				actionBtn.setMessage(tr("gui.dragonminez.party.invite"));
			}
			case JOIN -> {
				actionBtn.visible = true;
				actionBtn.active = true;
				actionBtn.setMessage(tr("gui.dragonminez.party.invite.accept"));
			}
			default -> {
				actionBtn.visible = true;
				actionBtn.active = true;
				actionBtn.setMessage(tr(isSelf ? "gui.dragonminez.party.leave" : "gui.dragonminez.party.kick"));
			}
		}
	}

	private void executePlayerAction() {
		if (selectedIndex < 0 || selectedIndex >= displayList.size() || Minecraft.getInstance().player == null) return;

		PartyEntry target = displayList.get(selectedIndex);
		boolean isSelf = target.id().equals(Minecraft.getInstance().player.getUUID());
		String name = target.name();

		switch (currentView) {
			case CREATE -> {
				if (!isSelf && target.isOnline()) {
					Minecraft.getInstance().player.connection.sendCommand("dmzparty invite " + name);
				}
			}
			case JOIN -> answerInvite(true);
			default -> {
				if (isSelf) Minecraft.getInstance().player.connection.sendCommand("dmzparty leave");
				else Minecraft.getInstance().player.connection.sendCommand("dmzparty kick " + name);
			}
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (isNotAnimating()) this.renderBackground(graphics);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);
		applyZoom(graphics, partialTick);

		if (currentView == View.WELCOME) {
			renderWelcome(graphics);
			super.render(graphics, uiMouseX, uiMouseY, partialTick);
			endUiScale(graphics);
			return;
		}

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

	private void renderWelcome(GuiGraphics graphics) {
		int centreX = getUiWidth() / 2;
		int x = centreX - WELCOME_WIDTH / 2;
		int y = welcomeTop();

		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(CARD_BG, x, y, WELCOME_WIDTH, WELCOME_HEIGHT,
				0.0F, 0.0F, CARD_SOURCE_WIDTH, CARD_SOURCE_HEIGHT, CARD_SHEET, CARD_SHEET);
		RenderSystem.disableBlend();

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.party.welcome.title").copy().withStyle(ChatFormatting.BOLD),
				centreX, y + 10, 0xFFFFD700, 0x000000);

		String[] lines = {
				"gui.dragonminez.party.welcome.quests",
				"gui.dragonminez.party.welcome.tournaments",
				"gui.dragonminez.party.welcome.explore",
				"gui.dragonminez.party.welcome.stats"
		};

		int lineY = y + 26;
		for (String key : lines) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(key), centreX, lineY, 0xFFE8F0FF, 0x000000);
			lineY += this.font.lineHeight + 2;
		}

		int invites = pendingInvites().size();
		if (invites > 0) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font,
					tr("gui.dragonminez.party.welcome.pending", invites), centreX, lineY + 2, 0xFF9FFF9F, 0x000000);
		}
	}

	private int welcomeTop() {
		return getUiHeight() / 2 - WELCOME_HEIGHT / 2 - 14;
	}

	private void renderPanels(GuiGraphics graphics, int leftX, int rightX, int panelY) {
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		graphics.blit(MENU_BIG, leftX, panelY, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, leftX + 17, panelY + 10, 142, 22, 107, 21, 256, 256);

		graphics.blit(MENU_BIG, rightX, panelY, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, rightX + 17, panelY + 10, 142, 22, 107, 21, 256, 256);
		if (currentView != View.JOIN) {
			graphics.blit(MENU_BIG, rightX + 31, panelY + 77, 142, 0, 79, 21, 256, 256);
		}

		RenderSystem.disableBlend();
	}

	private void renderPlayerList(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
		Component tabText = tr(switch (currentView) {
			case CREATE -> "gui.dragonminez.party.server";
			case JOIN -> "gui.dragonminez.party.invites";
			default -> "gui.dragonminez.party.party";
		});
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tabText.copy().withStyle(ChatFormatting.BOLD), panelX + 70, panelY + 16, 0xFFFFD700, 0x000000);

		if (displayList.isEmpty() && currentView == View.JOIN) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font,
					tr("gui.dragonminez.party.invites.empty").withStyle(ChatFormatting.GRAY),
					panelX + 70, panelY + 60, 0xFFAAAAAA, 0x000000);
		}

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
				if (currentView == View.CREATE) {
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
			if (currentView != View.JOIN) {
				TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.stats").withStyle(ChatFormatting.BOLD), panelX + 70, panelY + 84, 0x68CCFF, 0x000000);
			}
			return;
		}

		PartyEntry targetEntry = displayList.get(selectedIndex);
		UUID targetId = targetEntry.id();
		String displayName = targetEntry.name() + (targetEntry.isLeader() ? " ⭐" : "");

		int headerColor = targetEntry.isOnline() ? 0xFFFFD700 : 0xFFAAAAAA;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(displayName).withStyle(ChatFormatting.BOLD), panelX + 70, panelY + 16, headerColor, 0x000000);

		Player targetPlayer = Minecraft.getInstance().level.getPlayerByUUID(targetId);
		int startY = panelY + 36;

		if (currentView == View.JOIN) {
			renderInviteDetails(graphics, panelX, startY, targetEntry.partyId());
			return;
		}

		PartyPackets.MemberStats snapshot = currentView == View.PARTY ? PartyStatsCache.get(targetId) : null;

		if (snapshot != null && snapshot.online()) {
			renderStatBlock(graphics, panelX, startY, snapshot.race(), snapshot.characterClass(), snapshot.level(),
					snapshot.strength(), snapshot.strikePower(), snapshot.resistance(), snapshot.vitality(),
					snapshot.kiPower(), snapshot.energy());
			return;
		}

		if (targetPlayer != null && targetEntry.isOnline()) {
			StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).ifPresent(data ->
					renderStatBlock(graphics, panelX, startY,
							data.getCharacter().getRaceName(), data.getCharacter().getCharacterClass(), data.getLevel(),
							data.getStats().getStrength(), data.getStats().getStrikePower(),
							data.getStats().getResistance(), data.getStats().getVitality(),
							data.getStats().getKiPower(), data.getStats().getEnergy()));
			return;
		}

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.party.unavailable").withStyle(ChatFormatting.RED), panelX + 70, startY + 20, 0xFF5555, 0x000000);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.party.out_of_range").withStyle(ChatFormatting.GRAY), panelX + 70, startY + 32, 0xAAAAAA, 0x000000);
	}

	/** Who is asking, on what difficulty, and how long the offer stands — label over value. */
	private void renderInviteDetails(GuiGraphics graphics, int panelX, int startY, UUID partyId) {
		PlayerQuestData.PartyInviteData invite = null;
		for (PlayerQuestData.PartyInviteData pending : pendingInvites()) {
			if (partyId != null && partyId.equals(pending.getPartyId())) invite = pending;
		}
		if (invite == null) return;

		int centreX = panelX + 70;
		long secondsLeft = Math.max(0L, (invite.getExpiresAtMs() - System.currentTimeMillis() + 999L) / 1000L);

		int y = startY;
		y = inviteRow(graphics, centreX, y, tr("gui.dragonminez.party.invite.from"),
				txt(invite.getInviterName()), 0xFFFFFF);
		y = inviteRow(graphics, centreX, y, tr("gui.dragonminez.party.invite.difficulty"),
				tr("gui.dragonminez.quest_tree.difficulty." + invite.getPartyDifficulty().name().toLowerCase()),
				0xFFFFFF);
		inviteRow(graphics, centreX, y, tr("gui.dragonminez.party.invite.expires"),
				txt(secondsLeft + "s"), secondsLeft <= 10 ? 0xFFFF5555 : 0xFFFFFF);
	}

	private int inviteRow(GuiGraphics graphics, int centreX, int y, Component label, Component value, int valueColour) {
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, label.copy().withStyle(ChatFormatting.BOLD),
				centreX, y, 0xD7FEF5, 0x000000);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, value, centreX, y + 11, valueColour, 0x000000);
		return y + 26;
	}

	private void renderStatBlock(GuiGraphics graphics, int panelX, int startY,
								 String race, String characterClass, int level,
								 int strength, int strikePower, int resistance, int vitality,
								 int kiPower, int energyStat) {
		int labelX = panelX + 20;
		int valueX = panelX + 65;

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.race").withStyle(style -> style.withBold(true)), labelX, startY, 0xD7FEF5, 0x000000);
		TextUtil.drawStringWithBorder(graphics, this.font, tr("race.dragonminez." + race), valueX, startY, 0xFFFFFF, 0x000000);

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.class").withStyle(style -> style.withBold(true)), labelX, startY + 11, 0xD7FEF5, 0x000000);
		TextUtil.drawStringWithBorder(graphics, this.font, tr("class.dragonminez." + characterClass), valueX, startY + 11, 0xFFFFFF, 0x000000);

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.level").withStyle(style -> style.withBold(true)), labelX, startY + 22, 0xD7FEF5, 0x000000);
		TextUtil.drawStringWithBorder(graphics, this.font, txt(String.valueOf(level)), valueX, startY + 22, 0xFFFFFF, 0x000000);

		int statsY = startY + 48;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.stats").withStyle(ChatFormatting.BOLD), panelX + 70, statsY, 0x68CCFF, 0x000000);

		int statLabelX = panelX + 30;
		int statValueX = panelX + 60;

		String[] keys = {"str", "skp", "res", "vit", "pwr", "ene"};
		int[] values = {strength, strikePower, resistance, vitality, kiPower, energyStat};

		for (int i = 0; i < keys.length; i++) {
			int rowY = statsY + 18 + i * 12;
			final String key = keys[i];
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats." + key).withStyle(style -> style.withBold(true)), statLabelX, rowY, 0xD71432, 0x000000);
			TextUtil.drawStringWithBorder(graphics, this.font, txt(String.valueOf(values[i])), statValueX, rowY, 0xFFD7AB, 0x000000);
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
		if (currentView == View.WELCOME) return false;

		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int leftPanelX = 12 + getLeftPanelSwitchOffset(1.0f);
		int centerY = getUiHeight() / 2;
		int panelY = centerY - 105;

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