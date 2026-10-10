package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.BaseMenuScreen;
import com.dragonminez.client.util.PanelSkin;
import com.dragonminez.client.util.ScrollbarState;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.PartyPackets;
import com.dragonminez.common.quest.Difficulty;
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
import net.minecraft.util.FormattedCharSequence;
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

	private static final int WELCOME_WIDTH = 300;
	private static final int WELCOME_HEIGHT = 94;
	private static final int WELCOME_TEXT_TOP = 26;
	private static final int WELCOME_TEXT_PAD = 14;
	private static final int WELCOME_TEXT_BOTTOM = 8;
	private static final int PANEL_TEXT_WIDTH = 122;
	private static final int STAT_VALUE_WIDTH = 66;

	private enum View { WELCOME, CREATE, JOIN, PARTY }
	private View currentView = View.WELCOME;

	private record PartyEntry(UUID id, String name, boolean isOnline, boolean isLeader, UUID partyId) {}
	private List<PartyEntry> displayList = new ArrayList<>();
	private int selectedIndex = -1;

	private final ScrollbarState listScroll = new ScrollbarState().barWidth(2).minThumb(10).step(ITEM_HEIGHT * 2);
	private final ScrollbarState welcomeScroll = new ScrollbarState().barWidth(2).minThumb(10).step(22);

	private TexturedTextButton actionBtn;
	private TexturedTextButton altBtn;
	private TexturedTextButton backBtn;
	private TexturedTextButton inviteMoreBtn;
	private UUID pendingConfirmParty;
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

	@Override
	protected boolean hasCenterModel() {
		return currentView != View.WELCOME || isInParty();
	}

	private boolean isInParty() {
		if (Minecraft.getInstance().player == null) return false;
		return StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player)
				.map(data -> data.getPlayerQuestData().getActivePartyId() != null)
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
		pendingConfirmParty = null;
		listScroll.reset();
		welcomeScroll.reset();
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
			List<UUID> currentMembers = StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player)
					.map(data -> data.getPlayerQuestData().getPartyMemberIds())
					.orElse(List.of());
			onlinePlayers.sort((p1, p2) -> p1.getProfile().getName().compareToIgnoreCase(p2.getProfile().getName()));

			for (PlayerInfo p : onlinePlayers) {
				if (p.getProfile().getId().equals(localId) || currentMembers.contains(p.getProfile().getId())) continue;
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
		int rightPanelX = getUiWidth() - 158 + Math.round(getRightPanelSwitchOffset(1.0f));
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
		backBtn = menuButton(12 + Math.round(getLeftPanelSwitchOffset(1.0f)) + 35, rightPanelY + 180,
				"gui.dragonminez.party.back", btn -> goBack());
		inviteMoreBtn = menuButton(12 + Math.round(getLeftPanelSwitchOffset(1.0f)) + 35, rightPanelY + 180,
				"gui.dragonminez.party.invite_players", btn -> setView(View.CREATE));

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
		this.addRenderableWidget(inviteMoreBtn);
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
		setView(isInParty() ? View.PARTY : View.WELCOME);
	}

	private Difficulty ownDifficulty() {
		if (Minecraft.getInstance().player == null) return Difficulty.NORMAL;
		return StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player)
				.map(data -> data.getPlayerQuestData().getDifficulty())
				.orElse(Difficulty.NORMAL);
	}

	private PlayerQuestData.PartyInviteData inviteFor(UUID partyId) {
		if (partyId == null) return null;
		for (PlayerQuestData.PartyInviteData pending : pendingInvites()) {
			if (partyId.equals(pending.getPartyId())) return pending;
		}
		return null;
	}

	private void answerInvite(boolean accept) {
		if (selectedIndex < 0 || selectedIndex >= displayList.size()) return;

		UUID partyId = displayList.get(selectedIndex).partyId();
		if (accept) {
			PlayerQuestData.PartyInviteData invite = inviteFor(partyId);
			int ownRank = ownDifficulty().ordinal();
			int partyRank = invite != null ? invite.getPartyDifficulty().ordinal() : ownRank;
			if (partyRank < ownRank) return;
			if (partyRank > ownRank && (partyId == null || !partyId.equals(pendingConfirmParty))) {
				pendingConfirmParty = partyId;
				refreshActionButtons();
				return;
			}
			com.dragonminez.common.network.NetworkHandler.sendToServer(
					new com.dragonminez.common.network.C2S.AcceptPartyInviteC2S(partyRank > ownRank, partyId));
		} else {
			com.dragonminez.common.network.NetworkHandler.sendToServer(
					new com.dragonminez.common.network.C2S.RejectPartyInviteC2S(partyId));
		}
		selectedIndex = -1;
		pendingConfirmParty = null;
		refreshPlayerList();
	}

	private void updatePanelWidgetOffsets(float rightOffset) {
		int rightPanelX = getUiWidth() - 158;

		slideX(prevBtn, rightPanelX + 20, rightOffset);
		slideX(nextBtn, rightPanelX + 116, rightOffset);
		slideX(actionBtn, rightPanelX + 35, rightOffset);
	}

	private void shiftSelection(int direction) {
		if (displayList.isEmpty()) return;
		selectedIndex += direction;
		if (selectedIndex < 0) selectedIndex = displayList.size() - 1;
		if (selectedIndex >= displayList.size()) selectedIndex = 0;
		pendingConfirmParty = null;
		refreshActionButtons();
	}

	private void refreshActionButtons() {
		boolean welcome = currentView == View.WELCOME;
		boolean validSelection = selectedIndex >= 0 && selectedIndex < displayList.size();

		createBtn.visible = welcome;
		joinBtn.visible = welcome;
		joinBtn.active = !pendingInvites().isEmpty();

		backBtn.visible = currentView == View.CREATE || currentView == View.JOIN;
		inviteMoreBtn.visible = currentView == View.PARTY && isInParty();
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
				PlayerQuestData.PartyInviteData invite = inviteFor(targetEntry.partyId());
				int ownRank = ownDifficulty().ordinal();
				int partyRank = invite != null ? invite.getPartyDifficulty().ordinal() : ownRank;
				boolean confirming = targetEntry.partyId() != null && targetEntry.partyId().equals(pendingConfirmParty);
				actionBtn.visible = true;
				actionBtn.active = partyRank >= ownRank;
				actionBtn.setMessage(tr(confirming ? "quest.dmz.party.invite.difficulty_confirm.button" : "gui.dragonminez.party.invite.accept"));
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
					com.dragonminez.common.network.NetworkHandler.sendToServer(
							new com.dragonminez.common.network.C2S.InvitePartyMemberC2S(target.id()));
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
		renderMenuBackground(graphics, partialTick);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);

		if (currentView == View.WELCOME) {
			listScroll.clear();
			renderWelcome(graphics, uiMouseX, uiMouseY);
			super.render(graphics, uiMouseX, uiMouseY, partialTick);
			endUiScale(graphics);
			return;
		}
		welcomeScroll.clear();

		float leftOffset = getLeftPanelSwitchOffset(partialTick);
		float rightOffset = getRightPanelSwitchOffset(partialTick);

		updatePanelWidgetOffsets(rightOffset);

		int leftPanelX = 12;
		int rightPanelX = getUiWidth() - 158;
		int centerY = getUiHeight() / 2;
		int panelY = centerY - 105;

		graphics.pose().pushPose();
		graphics.pose().translate(leftOffset, 0.0f, 0.0f);
		renderLeftPanelFrame(graphics, leftPanelX, panelY);
		renderPlayerList(graphics, leftPanelX, panelY, uiMouseX - Math.round(leftOffset), uiMouseY);
		graphics.pose().popPose();

		graphics.pose().pushPose();
		graphics.pose().translate(rightOffset, 0.0f, 0.0f);
		renderRightPanelFrame(graphics, rightPanelX, panelY);
		renderRightPanelDetails(graphics, rightPanelX, panelY);
		graphics.pose().popPose();

		graphics.pose().pushPose();
		graphics.pose().translate(0.0f, getCenterPanelSwitchOffset(partialTick), 0.0f);
		renderCentralModel(graphics, getUiWidth() / 2 + 5, getUiHeight() / 2 + 70, 75, uiMouseX, uiMouseY);
		graphics.pose().popPose();

		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		endUiScale(graphics);
	}

	private void renderWelcome(GuiGraphics graphics, int mouseX, int mouseY) {
		int centreX = getUiWidth() / 2;
		int x = centreX - WELCOME_WIDTH / 2;
		int y = welcomeTop();

		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		PanelSkin.NPC_PANEL.draw(graphics, x, y, WELCOME_WIDTH, WELCOME_HEIGHT);
		RenderSystem.disableBlend();

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.party.welcome.title").copy().withStyle(ChatFormatting.BOLD),
				centreX, y + 10, 0xFFFFD700, 0x000000);

		List<Component> paragraphs = new ArrayList<>();
		for (String key : new String[]{"gui.dragonminez.party.welcome.quests", "gui.dragonminez.party.welcome.tournaments",
				"gui.dragonminez.party.welcome.explore", "gui.dragonminez.party.welcome.stats"}) {
			paragraphs.add(tr(key));
		}
		int invites = pendingInvites().size();
		if (invites > 0) {
			paragraphs.add(tr("gui.dragonminez.party.welcome.pending", invites).withStyle(style -> style.withColor(0x9FFF9F)));
		}

		int textX = x + WELCOME_TEXT_PAD;
		int textY = y + WELCOME_TEXT_TOP;
		int textWidth = WELCOME_WIDTH - WELCOME_TEXT_PAD * 2;
		int textHeight = WELCOME_HEIGHT - WELCOME_TEXT_TOP - WELCOME_TEXT_BOTTOM;
		int lineHeight = this.font.lineHeight + 2;
		List<FormattedCharSequence> wrapped = TextUtil.wrapScrollable(this.font, paragraphs, textWidth, textHeight, lineHeight, welcomeScroll);
		TextUtil.renderScrollableText(graphics, this.font, welcomeScroll, wrapped, textX, textY, textWidth, textHeight, lineHeight,
				0xFFE8F0FF, true, mouseX, mouseY);
	}

	private int drawWrappedCentered(GuiGraphics graphics, Component text, int centreX, int y, int color) {
		for (FormattedCharSequence line : this.font.split(text, PANEL_TEXT_WIDTH)) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, line, centreX, y, color, 0x000000);
			y += this.font.lineHeight + 1;
		}
		return y;
	}

	private void drawFitted(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
		int width = this.font.width(text);
		if (width <= maxWidth) {
			TextUtil.drawStringWithBorder(graphics, this.font, text, x, y, color, 0x000000);
			return;
		}
		float scale = maxWidth / (float) width;
		graphics.pose().pushPose();
		graphics.pose().translate(x, y + this.font.lineHeight * (1.0f - scale) / 2.0f, 0.0f);
		graphics.pose().scale(scale, scale, 1.0f);
		TextUtil.drawStringWithBorder(graphics, this.font, text, 0, 0, color, 0x000000);
		graphics.pose().popPose();
	}

	private int welcomeTop() {
		return getUiHeight() / 2 - WELCOME_HEIGHT / 2 - 14;
	}

	private void renderLeftPanelFrame(GuiGraphics graphics, int leftX, int panelY) {
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		blit(graphics, MENU_BIG, leftX, panelY, 0, 0, 141, 213);
		blit(graphics, MENU_BIG, leftX + 17, panelY + 10, 142, 22, 107, 21);
	}

	private void renderRightPanelFrame(GuiGraphics graphics, int rightX, int panelY) {
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		blit(graphics, MENU_BIG, rightX, panelY, 0, 0, 141, 213);
		blit(graphics, MENU_BIG, rightX + 17, panelY + 10, 142, 22, 107, 21);
		if (currentView != View.JOIN) blit(graphics, MENU_BIG, rightX + 31, panelY + 77, 142, 0, 79, 21);
	}

	private void renderPlayerList(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
		Component tabText = tr(switch (currentView) {
			case CREATE -> "gui.dragonminez.party.server";
			case JOIN -> "gui.dragonminez.party.invites";
			default -> "gui.dragonminez.party.party";
		});
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tabText.copy().withStyle(ChatFormatting.BOLD), panelX + 70, panelY + 16, 0xFFFFD700, 0x000000);

		if (displayList.isEmpty() && currentView == View.JOIN) {
			drawWrappedCentered(graphics, tr("gui.dragonminez.party.invites.empty").withStyle(ChatFormatting.GRAY),
					panelX + 70, panelY + 60, 0xFFAAAAAA);
		}

		int startY = panelY + 35;
		int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;

		listScroll.layout(panelX + 5, startY, 130, viewHeight, displayList.size() * ITEM_HEIGHT).barAt(panelX + 130);
		boolean overList = listScroll.isInView(mouseX, mouseY);
		double contentMouseY = listScroll.toContent(mouseY);
		listScroll.beginClip(graphics);

		for (int i = 0; i < displayList.size(); i++) {
			PartyEntry entry = displayList.get(i);
			int itemY = startY + (i * ITEM_HEIGHT);

			if (listScroll.isVisible(itemY, ITEM_HEIGHT)) {
				boolean isSelected = (i == selectedIndex);
				boolean isHovered = overList && mouseX >= panelX + 10 && mouseX <= panelX + 120 && contentMouseY >= itemY && contentMouseY <= itemY + ITEM_HEIGHT;

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

		listScroll.endClip(graphics);
		listScroll.renderBar(graphics, mouseX, mouseY);
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

		int reasonY = drawWrappedCentered(graphics, tr("gui.dragonminez.party.unavailable").withStyle(ChatFormatting.RED), panelX + 70, startY + 20, 0xFF5555);
		drawWrappedCentered(graphics, tr("gui.dragonminez.party.out_of_range").withStyle(ChatFormatting.GRAY), panelX + 70, reasonY + 1, 0xAAAAAA);
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
				difficultyLabel(invite.getPartyDifficulty()), 0xFFFFFF);
		y = inviteRow(graphics, centreX, y, tr("gui.dragonminez.party.invite.expires"),
				txt(secondsLeft + "s"), secondsLeft <= 10 ? 0xFFFF5555 : 0xFFFFFF);

		Difficulty own = ownDifficulty();
		if (invite.getPartyDifficulty().ordinal() < own.ordinal()) {
			drawWrappedCentered(graphics, tr("gui.dragonminez.party.invite.difficulty_low", difficultyLabel(own)), centreX, y, 0xFF5555);
		} else if (invite.getPartyDifficulty().ordinal() > own.ordinal()) {
			drawWrappedCentered(graphics, tr("gui.dragonminez.party.invite.difficulty_change", difficultyLabel(invite.getPartyDifficulty())), centreX, y, 0xFFE066);
		}
	}

	private Component difficultyLabel(Difficulty difficulty) {
		return tr("gui.dragonminez.quest_tree.difficulty." + difficulty.name().toLowerCase(java.util.Locale.ROOT));
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
		drawFitted(graphics, tr("race.dragonminez." + race), valueX, startY, STAT_VALUE_WIDTH, 0xFFFFFF);

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.class").withStyle(style -> style.withBold(true)), labelX, startY + 11, 0xD7FEF5, 0x000000);
		drawFitted(graphics, tr("class.dragonminez." + characterClass), valueX, startY + 11, STAT_VALUE_WIDTH, 0xFFFFFF);

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
		if (listScroll.scrollWheel(delta) || welcomeScroll.scrollWheel(delta)) return true;
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		if (currentView == View.WELCOME) return welcomeScroll.mouseClicked(toUiX(mouseX), toUiY(mouseY), button);

		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int listSlide = Math.round(getLeftPanelSwitchOffset(1.0f));
		int leftPanelX = 12 + listSlide;
		int centerY = getUiHeight() / 2;
		int panelY = centerY - 105;

		int startY = panelY + 35;
		int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;

		if (listScroll.mouseClicked(uiMouseX - listSlide, uiMouseY, button)) return true;

		if (uiMouseX >= leftPanelX + 10 && uiMouseX <= leftPanelX + 120 && uiMouseY >= startY && uiMouseY <= startY + viewHeight) {
			int index = (int) ((uiMouseY - startY + listScroll.scroll()) / ITEM_HEIGHT);
			if (index >= 0 && index < displayList.size()) {
				if (index != selectedIndex) pendingConfirmParty = null;
				selectedIndex = index;
				refreshActionButtons();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (listScroll.mouseDragged(toUiX(mouseX) - Math.round(getLeftPanelSwitchOffset(1.0f)), toUiY(mouseY))) return true;
		if (welcomeScroll.mouseDragged(toUiX(mouseX), toUiY(mouseY))) return true;
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (listScroll.mouseReleased() | welcomeScroll.mouseReleased()) return true;
		return super.mouseReleased(mouseX, mouseY, button);
	}
}