package com.dragonminez.client.gui.tournament;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.TournamentPackets;
import com.dragonminez.common.quest.QuestUnlocks;
import com.dragonminez.server.world.tournament.Tournament;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.pipeline.VertexConsumerWrapper;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class TournamentBracketScreen extends ScaledScreen {

	private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation SLOT_FRAMES = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/tournament/slots.png");

	private static final float PANEL_FRACTION = 0.8f;
	private static final int PADDING = 10;
	private static final int HEADER_HEIGHT = 30;
	private static final int MEMBERS_HEIGHT = 16;
	private static final int FOOTER_HEIGHT = 26;
	private static final int BUTTON_WIDTH = 74;
	private static final int BUTTON_HEIGHT = 20;
	private static final int SCROLLBAR = 4;

	private static final int SLOT = 34;
	private static final int FRAME_SOURCE = 48;
	private static final int TREE_PITCH = 42;
	private static final int ROW_PITCH = 54;
	private static final int LABEL_MARGIN = 68;
	private static final int GAUNTLET_PITCH = 58;
	private static final int GAUNTLET_MARGIN = 36;
	private static final float SCROLL_STEP = 28.0f;
	private static final float MAX_UPSCALE = 1.5f;
	private static final long DECLINE_ARM_MS = 3000L;

	private static final int TITLE_COLOR = 0xFFD54F;
	private static final int STARS_COLOR = 0xFFAA00;
	private static final int TEXT_COLOR = 0xE8ECF2;
	private static final int MUTED_COLOR = 0x9FB8AE;
	private static final int ACCENT_COLOR = 0x7CFDD6;
	private static final int WARN_COLOR = 0xFF6D6D;
	private static final int LINE_COLOR = 0xFF6D8CFF;
	private static final int LINE_DONE_COLOR = 0xFFFFD54F;
	private static final int LINE_NOW_COLOR = 0xFFFF5555;
	private static final int PLATE_COLOR = 0x1CFFFFFF;
	private static final int HOVER_COLOR = 0x30FFFFFF;
	private static final int TRACK_COLOR = 0x50000000;
	private static final int THUMB_COLOR = 0xC0A9C7FF;

	private static final float PORTRAIT_SPAN = 0.80F;
	private static final float PORTRAIT_HEADROOM = 0.30F;
	private static final float PORTRAIT_PAD = 2.0F;

	private enum View { SIGNUP, LOCKED, RULES, BRACKET }

	private enum SlotState {
		UPCOMING(0, 0), PLAYER(FRAME_SOURCE, 0), CURRENT(0, FRAME_SOURCE), DEFEATED(FRAME_SOURCE, 0), HIDDEN(0, 0), EMPTY(0, 0),
		PENDING(0, 0), CROWN(FRAME_SOURCE, 0);

		private final int u;
		private final int v;

		SlotState(int u, int v) {
			this.u = u;
			this.v = v;
		}
	}

	private record SlotHit(float x, float y, String id, SlotState state) {}

	private record MemberHit(int x, int y, int width, TournamentPackets.OpenBracketS2C.Member member) {}

	private record RuleLine(int number, List<FormattedCharSequence> lines, int color) {}

	private static final Map<String, LivingEntity> PORTRAIT_CACHE = new HashMap<>();

	private TournamentPackets.OpenBracketS2C data;
	private int npcEntityId;
	private int phaseTicks;
	private int cooldownTicks;

	private int guiLeft;
	private int guiTop;
	private int guiWidth;
	private int guiHeight;
	private int contentX;
	private int contentY;
	private int contentWidth;
	private int contentHeight;

	private float targetScroll;
	private float currentScroll;
	private float maxScroll;
	private boolean verticalScroll = true;
	private boolean draggingThumb;
	private float contentScale = 1.0f;
	private float originX;
	private float originY;

	private final List<SlotHit> slotHits = new ArrayList<>();
	private final List<MemberHit> memberHits = new ArrayList<>();
	private SlotHit hoveredSlot;
	private MemberHit hoveredMember;

	private TexturedTextButton declineButton;
	private long declineArmedUntil;
	private boolean pendingFocus = true;

	private TournamentBracketScreen(TournamentPackets.OpenBracketS2C data) {
		super(Component.translatable("tournament.dragonminez.title"));
		this.data = data;
		this.npcEntityId = data.getNpcEntityId();
		this.phaseTicks = data.getPhaseSeconds() * 20;
		this.cooldownTicks = data.getCooldownSeconds() * 20;
	}

	public static void open(TournamentPackets.OpenBracketS2C data) {
		Minecraft mc = Minecraft.getInstance();

		if (mc.screen instanceof TournamentBracketScreen open
				&& open.data.getTournamentId().equals(data.getTournamentId())) {
			open.refresh(data);
			return;
		}
		if (data.isPush()) return;

		mc.setScreen(new TournamentBracketScreen(data));
	}

	private void refresh(TournamentPackets.OpenBracketS2C update) {
		boolean previewOver = this.data.getPhase() == TournamentPackets.OpenBracketS2C.Phase.PREVIEW
				&& update.getPhase() == TournamentPackets.OpenBracketS2C.Phase.BOUT;
		View before = view();
		this.data = update;
		if (update.getNpcEntityId() >= 0) this.npcEntityId = update.getNpcEntityId();
		this.phaseTicks = update.getPhaseSeconds() * 20;
		this.cooldownTicks = update.getCooldownSeconds() * 20;
		if (previewOver) {
			onClose();
			return;
		}
		if (before != view()) {
			targetScroll = 0.0f;
			currentScroll = 0.0f;
			pendingFocus = true;
		}
		rebuildWidgets();
	}

	private View view() {
		if (data.isLockedByOther()) return View.LOCKED;
		if (data.isSignUp()) return View.SIGNUP;
		if (data.getPhase() == TournamentPackets.OpenBracketS2C.Phase.RULES && !data.isEliminated()) return View.RULES;
		return View.BRACKET;
	}

	private TournamentPackets.OpenBracketS2C.Member me() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return null;
		String name = mc.player.getGameProfile().getName();
		for (TournamentPackets.OpenBracketS2C.Member member : data.getMembers()) {
			if (member.name().equals(name)) return member;
		}
		return null;
	}

	private boolean isMyBout() {
		TournamentPackets.OpenBracketS2C.Member me = me();
		return me != null && me.active() && !data.isEliminated() && !data.isCompleted();
	}

	private boolean awaitingMyReady() {
		return data.getPhase() == TournamentPackets.OpenBracketS2C.Phase.INTERMISSION && isMyBout() && !data.isMyReady();
	}

	@Override
	public void tick() {
		super.tick();
		if (phaseTicks > 0) {
			phaseTicks--;
			if (phaseTicks == 0 && data.getPhase() == TournamentPackets.OpenBracketS2C.Phase.PREVIEW) onClose();
		}
		if (cooldownTicks > 0) {
			cooldownTicks--;
			if (cooldownTicks == 0) rebuildWidgets();
		}
		if (declineArmedUntil != 0L && System.currentTimeMillis() > declineArmedUntil) {
			declineArmedUntil = 0L;
			if (declineButton != null) declineButton.setMessage(tr("tournament.dragonminez.decline"));
		}
	}

	private int phaseSecondsLeft() {
		return (phaseTicks + 19) / 20;
	}

	@Override
	protected int getMinGuiWidth() {
		return 420;
	}

	@Override
	protected void init() {
		super.init();
		guiWidth = Math.round(getUiWidth() * PANEL_FRACTION);
		guiHeight = Math.round(getUiHeight() * PANEL_FRACTION);
		guiLeft = (getUiWidth() - guiWidth) / 2;
		guiTop = (getUiHeight() - guiHeight) / 2;

		contentX = guiLeft + PADDING;
		contentY = guiTop + PADDING + HEADER_HEIGHT + MEMBERS_HEIGHT;
		contentWidth = guiWidth - PADDING * 2;
		contentHeight = guiHeight - PADDING * 2 - HEADER_HEIGHT - MEMBERS_HEIGHT - FOOTER_HEIGHT;

		List<TexturedTextButton> buttons = new ArrayList<>();
		declineButton = null;
		switch (view()) {
			case SIGNUP -> {
				if (cooldownTicks <= 0 && data.isPartyLeader()) {
					buttons.add(button("tournament.dragonminez.enter", b -> send(TournamentPackets.ActionC2S.Action.SIGN_UP)));
				}
			}
			case RULES -> {
				if (data.getMyState() == TournamentPackets.OpenBracketS2C.MemberState.PENDING) {
					buttons.add(button("tournament.dragonminez.accept", b -> send(TournamentPackets.ActionC2S.Action.ACCEPT_RULES)));
					declineButton = button("tournament.dragonminez.decline", b -> onDeclinePressed());
					buttons.add(declineButton);
				}
			}
			case BRACKET -> {
				if (awaitingMyReady()) {
					buttons.add(button("tournament.dragonminez.ready", b -> send(TournamentPackets.ActionC2S.Action.READY)));
				}
			}
			default -> {
			}
		}
		buttons.add(button("gui.dragonminez.close", b -> onClose()));

		int total = buttons.size() * BUTTON_WIDTH + (buttons.size() - 1) * 6;
		int x = guiLeft + (guiWidth - total) / 2;
		int y = guiTop + guiHeight - PADDING - BUTTON_HEIGHT;
		for (TexturedTextButton button : buttons) {
			button.setX(x);
			button.setY(y);
			addRenderableWidget(button);
			x += BUTTON_WIDTH + 6;
		}
	}

	private TexturedTextButton button(String key, net.minecraft.client.gui.components.Button.OnPress onPress) {
		return new TexturedTextButton.Builder()
				.position(0, 0)
				.size(BUTTON_WIDTH, BUTTON_HEIGHT)
				.texture(BUTTONS)
				.textureCoords(0, 28, 0, 48)
				.textureSize(BUTTON_WIDTH, BUTTON_HEIGHT)
				.message(tr(key))
				.onPress(onPress)
				.build();
	}

	private void onDeclinePressed() {
		long now = System.currentTimeMillis();
		if (declineArmedUntil != 0L && now <= declineArmedUntil) {
			declineArmedUntil = 0L;
			send(TournamentPackets.ActionC2S.Action.DECLINE_RULES);
			return;
		}
		declineArmedUntil = now + DECLINE_ARM_MS;
		if (declineButton != null) declineButton.setMessage(tr("tournament.dragonminez.decline.confirm"));
	}

	private void send(TournamentPackets.ActionC2S.Action action) {
		NetworkHandler.sendToServer(new TournamentPackets.ActionC2S(action, npcEntityId));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
		int uiMouseX = (int) toUiX(mouseX);
		int uiMouseY = (int) toUiY(mouseY);

		beginUiScale(graphics);
		slotHits.clear();
		memberHits.clear();
		hoveredSlot = null;
		hoveredMember = null;

		HudRender.nineSlice(graphics, PANEL, guiLeft, guiTop, guiWidth, guiHeight, 0.0f, 0.0f, 141.0f, 213.0f, 8.0f, 256, 256);
		renderHeader(graphics);
		renderMembers(graphics, uiMouseX, uiMouseY);

		switch (view()) {
			case RULES -> renderRules(graphics);
			default -> renderLadder(graphics, uiMouseX, uiMouseY);
		}
		renderScrollbar(graphics);
		renderFooter(graphics);

		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		renderTooltips(graphics, uiMouseX, uiMouseY);
		endUiScale(graphics);
	}

	private void renderHeader(GuiGraphics graphics) {
		int centreX = guiLeft + guiWidth / 2;
		int y = guiTop + PADDING;

		MutableComponent title = tr(data.getDisplayName());
		StringBuilder stars = new StringBuilder();
		for (int i = 0; i < 5; i++) stars.append(i < data.getDifficultyStars() ? "★" : "☆");
		MutableComponent starsText = txt(stars.toString());
		int titleWidth = font.width(title);
		int totalWidth = titleWidth + 6 + font.width(starsText);
		int x = centreX - totalWidth / 2;
		TextUtil.drawStringWithBorder(graphics, font, title, x, y, TITLE_COLOR);
		TextUtil.drawStringWithBorder(graphics, font, starsText, x + titleWidth + 6, y, STARS_COLOR);

		List<MutableComponent> chips = new ArrayList<>();
		chips.add(tr(data.isGauntlet() ? "tournament.dragonminez.format.gauntlet" : "tournament.dragonminez.format.bracket"));
		chips.add(tr(data.isLethal() ? "tournament.dragonminez.lethal" : "tournament.dragonminez.non_lethal"));
		int chipX = guiLeft + guiWidth - PADDING;
		for (int i = chips.size() - 1; i >= 0; i--) {
			MutableComponent chip = chips.get(i);
			int width = font.width(chip) + 8;
			chipX -= width;
			boolean warn = i == 1 && data.isLethal();
			HudRender.rect(graphics, chipX, y - 2, width, font.lineHeight + 4, warn ? 0x40FF6D6D : PLATE_COLOR);
			TextUtil.drawStringWithBorder(graphics, font, chip, chipX + 4, y, warn ? WARN_COLOR : ACCENT_COLOR);
			chipX -= 4;
		}

		MutableComponent status = statusLine();
		int statusColor = data.isEliminated() ? WARN_COLOR
				: data.isCompleted() ? TITLE_COLOR
				: awaitingMyReady() ? ACCENT_COLOR : TEXT_COLOR;
		TextUtil.drawCenteredStringWithBorder(graphics, font, status, centreX, y + 14, statusColor);
	}

	private MutableComponent statusLine() {
		if (data.isLockedByOther()) return tr("tournament.dragonminez.in_progress");
		if (data.isCompleted()) return tr("tournament.dragonminez.status.champion");
		if (data.isEliminated()) return tr("tournament.dragonminez.status.eliminated");
		if (data.isSignUp()) {
			if (cooldownTicks > 0) return tr("tournament.dragonminez.status.cooldown", formatDuration((cooldownTicks + 19) / 20));
			return tr("tournament.dragonminez.status.open");
		}

		int seconds = phaseSecondsLeft();
		return switch (data.getPhase()) {
			case RULES -> {
				if (data.getMyState() == TournamentPackets.OpenBracketS2C.MemberState.PENDING) {
					yield tr("tournament.dragonminez.status.rules", seconds);
				}
				yield tr("tournament.dragonminez.status.rules_waiting", pendingNames(), seconds);
			}
			case INTERMISSION -> {
				if (isMyBout()) {
					if (!data.isMyReady()) yield tr("tournament.dragonminez.status.accept", seconds);
					yield tr("tournament.dragonminez.status.accepted", opponentName());
				}
				yield tr("tournament.dragonminez.status.waiting_accept", notReadyNames(), seconds);
			}
			case PREVIEW -> tr("tournament.dragonminez.status.preview", boutLine());
			case BOUT -> tr("tournament.dragonminez.status.bout", boutLine());
			default -> tr("tournament.dragonminez.status.open");
		};
	}

	private String pendingNames() {
		List<String> names = new ArrayList<>();
		for (TournamentPackets.OpenBracketS2C.Member member : data.getMembers()) {
			if (member.state() == TournamentPackets.OpenBracketS2C.MemberState.PENDING) names.add(member.name());
		}
		return names.isEmpty() ? "-" : String.join(", ", names);
	}

	private String notReadyNames() {
		List<String> names = new ArrayList<>();
		for (TournamentPackets.OpenBracketS2C.Member member : data.getMembers()) {
			if (member.active() && !member.ready()) names.add(member.name());
		}
		return names.isEmpty() ? "-" : String.join(", ", names);
	}

	private Component opponentName() {
		List<Component> fighters = fighterNames();
		Minecraft mc = Minecraft.getInstance();
		String myName = mc.player != null ? mc.player.getGameProfile().getName() : "";
		for (Component fighter : fighters) {
			if (!fighter.getString().equals(myName)) return fighter;
		}
		return fighters.isEmpty() ? tr("tournament.dragonminez.slot.hidden") : fighters.get(0);
	}

	private List<Component> fighterNames() {
		List<Component> fighters = new ArrayList<>();
		for (TournamentPackets.OpenBracketS2C.Member member : data.getMembers()) {
			if (member.active()) fighters.add(Component.literal(member.name()));
		}
		if (fighters.size() == 1) {
			Component opponent = data.getRivalSlot().isEmpty() ? null : slotName(data.getRivalSlot());
			if (opponent == null && data.isGauntlet()) opponent = gauntletOpponentName();
			if (opponent != null) fighters.add(opponent);
		}
		return fighters;
	}

	private Component boutLine() {
		List<Component> fighters = fighterNames();
		if (fighters.isEmpty()) return tr("tournament.dragonminez.you");
		if (fighters.size() < 2) return fighters.get(0);
		return tr("tournament.dragonminez.title.versus", fighters.get(0), fighters.get(1));
	}

	private Component gauntletOpponentName() {
		List<String> card = gauntletCard();
		int round = data.getRound();
		return round >= 0 && round < card.size() ? slotName(card.get(round)) : null;
	}

	private void renderMembers(GuiGraphics graphics, int mouseX, int mouseY) {
		int y = guiTop + PADDING + HEADER_HEIGHT;
		List<MutableComponent> names = new ArrayList<>();
		int total = 0;
		for (TournamentPackets.OpenBracketS2C.Member member : data.getMembers()) {
			String glyph = data.isSignUp() && member.state() != TournamentPackets.OpenBracketS2C.MemberState.OUT
					? "" : stateGlyph(member);
			MutableComponent name = txt((member.leader() ? "★ " : "") + member.name() + (glyph.isEmpty() ? "" : " " + glyph));
			names.add(name);
			total += font.width(name) + 8 + 4;
		}
		total = Math.max(0, total - 4);

		int x = guiLeft + guiWidth / 2 - total / 2;
		for (int i = 0; i < names.size(); i++) {
			TournamentPackets.OpenBracketS2C.Member member = data.getMembers().get(i);
			MutableComponent name = names.get(i);
			int width = font.width(name) + 8;
			boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + font.lineHeight + 4;
			HudRender.rect(graphics, x, y, width, font.lineHeight + 4, hovered ? HOVER_COLOR : PLATE_COLOR);
			TextUtil.drawStringWithBorder(graphics, font, name, x + 4, y + 2, memberColor(member));
			memberHits.add(new MemberHit(x, y, width, member));
			if (hovered) hoveredMember = memberHits.get(memberHits.size() - 1);
			x += width + 4;
		}
	}

	private String stateGlyph(TournamentPackets.OpenBracketS2C.Member member) {
		if (member.active()) return member.ready() ? "✓" : "⚔";
		return switch (member.state()) {
			case PENDING -> "…";
			case ACCEPTED -> "✓";
			case DECLINED, OUT -> "✗";
			case ALIVE -> "";
		};
	}

	private static int memberColor(TournamentPackets.OpenBracketS2C.Member member) {
		if (member.active()) return TITLE_COLOR;
		return switch (member.state()) {
			case PENDING -> TEXT_COLOR;
			case ACCEPTED, ALIVE -> 0x9FFF9F;
			case DECLINED, OUT -> 0x7A7A85;
		};
	}

	private void renderRules(GuiGraphics graphics) {
		List<RuleLine> rules = buildRules(contentWidth - 24 - SCROLLBAR);
		int lineHeight = font.lineHeight + 1;
		int total = 0;
		for (RuleLine rule : rules) total += rule.lines().size() * lineHeight + 4;
		total += font.lineHeight + 6;

		verticalScroll = true;
		contentScale = 1.0f;
		maxScroll = Math.max(0.0f, total - contentHeight);
		updateScroll();
		originX = contentX;
		originY = contentY - currentScroll;

		graphics.enableScissor(toScreenCoord(contentX), toScreenCoord(contentY),
				toScreenCoord(contentX + contentWidth), toScreenCoord(contentY + contentHeight));
		graphics.pose().pushPose();
		graphics.pose().translate(0.0f, -currentScroll, 0.0f);

		int y = contentY + 2;
		TextUtil.drawStringWithBorder(graphics, font, tr("tournament.dragonminez.rules.title"), contentX + 4, y, TITLE_COLOR);
		y += font.lineHeight + 6;
		for (RuleLine rule : rules) {
			TextUtil.drawStringWithBorder(graphics, font, txt(rule.number() + "."), contentX + 4, y, ACCENT_COLOR);
			for (FormattedCharSequence line : rule.lines()) {
				TextUtil.drawStringWithBorder(graphics, font, line, contentX + 20, y, rule.color());
				y += lineHeight;
			}
			y += 4;
		}

		graphics.pose().popPose();
		graphics.disableScissor();
	}

	private List<RuleLine> buildRules(int width) {
		TournamentPackets.OpenBracketS2C.Rules rules = data.getRules();
		List<RuleLine> out = new ArrayList<>();
		List<MutableComponent> texts = new ArrayList<>();
		List<Integer> colors = new ArrayList<>();

		texts.add(tr("tournament.dragonminez.rules.accept_bout", rules.nextRoundSeconds()));
		colors.add(TEXT_COLOR);
		texts.add(tr("tournament.dragonminez.rules.lose", formatDuration(rules.reentryCooldownSeconds())));
		colors.add(TEXT_COLOR);
		texts.add(tr("tournament.dragonminez.rules.leave", rules.forfeitDistance(), rules.returnSeconds()));
		colors.add(TEXT_COLOR);
		texts.add(tr("tournament.dragonminez.rules.timeout", formatDuration(rules.matchTimeoutSeconds())));
		colors.add(TEXT_COLOR);
		texts.add(tr("tournament.dragonminez.rules.interference", rules.exclusionRadius()));
		colors.add(TEXT_COLOR);
		texts.add(tr("tournament.dragonminez.rules.countdown", Tournament.Manager.COUNTDOWN_SECONDS));
		colors.add(TEXT_COLOR);
		texts.add(tr(data.isLethal() ? "tournament.dragonminez.rules.lethal" : "tournament.dragonminez.rules.knockout"));
		colors.add(data.isLethal() ? WARN_COLOR : TEXT_COLOR);
		texts.add(tr(data.isGauntlet() ? "tournament.dragonminez.rules.party.gauntlet" : "tournament.dragonminez.rules.party.bracket",
				rules.rulesAcceptSeconds()));
		colors.add(TEXT_COLOR);

		for (int i = 0; i < texts.size(); i++) {
			out.add(new RuleLine(i + 1, font.split(texts.get(i), width), colors.get(i)));
		}
		return out;
	}

	private void renderLadder(GuiGraphics graphics, int mouseX, int mouseY) {
		float[] size = data.isGauntlet() ? gauntletSize() : treeSize();
		float fitBoth = Math.min(contentWidth / size[0], contentHeight / size[1]);
		float scale = fitBoth >= 1.0f ? Math.min(MAX_UPSCALE, fitBoth)
				: Math.min(1.0f, Math.max(contentWidth / size[0], contentHeight / size[1]));
		contentScale = scale;
		float scaledWidth = size[0] * scale;
		float scaledHeight = size[1] * scale;

		verticalScroll = scaledWidth <= contentWidth + 0.5f;
		maxScroll = verticalScroll ? Math.max(0.0f, scaledHeight - contentHeight) : Math.max(0.0f, scaledWidth - contentWidth);
		if (pendingFocus) {
			pendingFocus = false;
			if (!data.isGauntlet() && verticalScroll && maxScroll > 0.0f) {
				int focusRow = data.isCompleted() ? qualifierRounds() + 2 : data.getRound();
				float rowCentre = (rowY(focusRow, size[1]) + SLOT / 2.0f) * scale;
				targetScroll = Mth.clamp(rowCentre - contentHeight / 2.0f, 0.0f, maxScroll);
				if (!data.isRevealed()) targetScroll = 0.0f;
				currentScroll = targetScroll;
			}
		}
		updateScroll();

		if (verticalScroll) {
			originX = contentX + (contentWidth - scaledWidth) / 2.0f;
			originY = maxScroll > 0.0f ? contentY - currentScroll : contentY + (contentHeight - scaledHeight) / 2.0f;
		} else {
			originX = contentX - currentScroll;
			originY = contentY + (contentHeight - scaledHeight) / 2.0f;
		}

		float localMouseX = (mouseX - originX) / scale;
		float localMouseY = (mouseY - originY) / scale;
		boolean mouseInside = mouseX >= contentX && mouseX < contentX + contentWidth
				&& mouseY >= contentY && mouseY < contentY + contentHeight;

		graphics.enableScissor(toScreenCoord(contentX), toScreenCoord(contentY),
				toScreenCoord(contentX + contentWidth), toScreenCoord(contentY + contentHeight));
		graphics.pose().pushPose();
		graphics.pose().translate(originX, originY, 0.0f);
		graphics.pose().scale(scale, scale, 1.0f);

		if (data.isGauntlet()) renderGauntlet(graphics);
		else renderTree(graphics, size);

		graphics.pose().popPose();
		graphics.disableScissor();

		if (mouseInside) {
			for (SlotHit hit : slotHits) {
				if (localMouseX >= hit.x() && localMouseX < hit.x() + SLOT && localMouseY >= hit.y() && localMouseY < hit.y() + SLOT) {
					hoveredSlot = hit;
				}
			}
		}
	}

	private void updateScroll() {
		targetScroll = Mth.clamp(targetScroll, 0.0f, maxScroll);
		currentScroll = Mth.lerp(frameEase(), currentScroll, targetScroll);
		if (Math.abs(currentScroll - targetScroll) < 0.05f) currentScroll = targetScroll;
	}

	private int qualifierRounds() {
		int rounds = 0;
		for (int remaining = data.getSeeds().size(); remaining > 1; remaining /= 2) rounds++;
		return rounds;
	}

	private float[] treeSize() {
		int seeds = Math.max(2, data.getSeeds().size());
		int rows = qualifierRounds() + 3;
		float width = LABEL_MARGIN + seeds * TREE_PITCH + TREE_PITCH * 1.5f + SLOT;
		float height = rows * ROW_PITCH + 6;
		return new float[]{width, height};
	}

	private float rowY(int row, float height) {
		return height - 4 - (row + 1) * ROW_PITCH + (ROW_PITCH - SLOT) / 2.0f;
	}

	private float treeCentreX(int row, int index) {
		return LABEL_MARGIN + (index + 0.5f) * TREE_PITCH * (1 << row);
	}

	private String entrantAt(int round, int position) {
		List<String> source = round == 0
				? data.getSeeds()
				: (round - 1 < data.getWinners().size() ? data.getWinners().get(round - 1) : List.of());
		return position >= 0 && position < source.size() ? source.get(position) : "";
	}

	private String winnerOf(int round, int index) {
		if (round >= data.getWinners().size()) return "";
		List<String> winners = data.getWinners().get(round);
		return index < winners.size() ? winners.get(index) : "";
	}

	private void renderTree(GuiGraphics graphics, float[] size) {
		int q = qualifierRounds();
		int seeds = Math.max(2, data.getSeeds().size());
		int currentRound = data.isSignUp() || !data.isRevealed() ? -1 : data.getRound();
		boolean playing = data.isRevealed() && !data.isCompleted() && !data.isEliminated()
				&& data.getPhase() != TournamentPackets.OpenBracketS2C.Phase.RULES;
		float height = size[1];

		for (int row = 0; row < q; row++) {
			int entries = seeds >> row;
			float y = rowY(row, height);
			float parentY = rowY(row + 1, height);
			for (int i = 0; i < entries; i++) {
				String id = entrantAt(row, i);
				String winner = winnerOf(row, i / 2);
				boolean decided = !winner.isEmpty();
				boolean current = playing && row == currentRound && isCurrentPair(row, i);
				SlotState state = slotState(id, decided && !winner.equals(id), current, row == 0);
				float cx = treeCentreX(row, i);
				float x = cx - SLOT / 2.0f;
				float busY = parentY + SLOT + (y - parentY - SLOT) / 2.0f;
				int color = current ? LINE_NOW_COLOR : decided && winner.equals(id) ? LINE_DONE_COLOR : LINE_COLOR;
				HudRender.rect(graphics, cx - 0.5f, busY, 1.0f, y - busY, color);
				float parentCx = treeCentreX(row + 1, i / 2);
				float left = Math.min(cx, parentCx);
				float right = Math.max(cx, parentCx);
				HudRender.rect(graphics, left - 0.5f, busY, right - left + 1.0f, 1.0f, color);
				if (i % 2 == 0) HudRender.rect(graphics, parentCx - 0.5f, parentY + SLOT, 1.0f, busY - parentY - SLOT, decided ? LINE_DONE_COLOR : color);
				drawSlot(graphics, x, y, id, state);
			}
			drawRowLabel(graphics, tr("tournament.dragonminez.round.qualifier", row + 1), y, row == currentRound);
		}

		float trunkCx = treeCentreX(q, 0);
		float challengerCx = trunkCx + TREE_PITCH * 1.5f + SLOT / 2.0f;

		String qualified = q == 0 ? data.getSeeds().isEmpty() ? "" : data.getSeeds().get(0) : winnerOf(q - 1, 0);
		String semiWinner = winnerOf(q, 0);
		String finalWinner = winnerOf(q + 1, 0);

		float semiY = rowY(q, height);
		float finalY = rowY(q + 1, height);
		float topY = rowY(q + 2, height);

		boolean semiNow = playing && currentRound == q;
		boolean finalNow = playing && currentRound == q + 1;

		drawChallengerRow(graphics, semiY, trunkCx, challengerCx, qualified, data.getSemifinalist(), semiWinner, semiNow, finalY);
		drawRowLabel(graphics, tr("tournament.dragonminez.round.semi"), semiY, semiNow);

		drawChallengerRow(graphics, finalY, trunkCx, challengerCx, semiWinner, data.getChampion(), finalWinner, finalNow, topY);
		drawRowLabel(graphics, tr("tournament.dragonminez.round.final"), finalY, finalNow);

		String crown = data.isCompleted() ? finalWinner : "";
		drawSlot(graphics, trunkCx - SLOT / 2.0f, topY, crown, crown.isEmpty() ? SlotState.CROWN : SlotState.PLAYER);
		TextUtil.drawCenteredStringWithBorder(graphics, font, txt("★"), Math.round(trunkCx), Math.round(topY) - 11, TITLE_COLOR);
		drawRowLabel(graphics, tr("tournament.dragonminez.round.champion"), topY, data.isCompleted());
	}

	private void drawChallengerRow(GuiGraphics graphics, float y, float trunkCx, float challengerCx, String trunkId,
								   String challengerId, String winner, boolean now, float parentY) {
		boolean decided = !winner.isEmpty();
		int color = now ? LINE_NOW_COLOR : decided ? LINE_DONE_COLOR : LINE_COLOR;
		float midY = y + SLOT / 2.0f;
		HudRender.rect(graphics, trunkCx + SLOT / 2.0f, midY - 0.5f, challengerCx - SLOT / 2.0f - trunkCx - SLOT / 2.0f, 1.0f, color);
		TextUtil.drawCenteredStringWithBorder(graphics, font, tr("tournament.dragonminez.vs"),
				Math.round((trunkCx + challengerCx) / 2.0f), Math.round(midY) - 9, now ? 0xFF5555 : MUTED_COLOR);
		HudRender.rect(graphics, trunkCx - 0.5f, parentY + SLOT, 1.0f, y - parentY - SLOT, decided ? LINE_DONE_COLOR : color);

		drawSlot(graphics, trunkCx - SLOT / 2.0f, y, trunkId, slotState(trunkId, decided && !winner.equals(trunkId), now, false));
		drawSlot(graphics, challengerCx - SLOT / 2.0f, y, challengerId, slotState(challengerId, decided && !winner.equals(challengerId), now, false));
	}

	private void drawRowLabel(GuiGraphics graphics, MutableComponent label, float y, boolean highlight) {
		int width = font.width(label);
		TextUtil.drawStringWithBorder(graphics, font, label, LABEL_MARGIN - 10 - width, Math.round(y + SLOT / 2.0f - 4),
				highlight ? TITLE_COLOR : MUTED_COLOR);
	}

	private boolean isCurrentPair(int row, int index) {
		int active = seedPosition(data.getActiveSlot(), row);
		int rival = seedPosition(data.getRivalSlot(), row);
		if (active < 0 && rival < 0) return false;
		if (active >= 0 && (index == active || index == (active ^ 1))) return true;
		return rival >= 0 && (index == rival || index == (rival ^ 1));
	}

	private int seedPosition(String slot, int row) {
		if (slot == null || slot.isEmpty()) return -1;
		int seed = data.getSeeds().indexOf(slot);
		return seed < 0 ? -1 : seed >> row;
	}

	private SlotState slotState(String id, boolean defeated, boolean current, boolean seedRow) {
		if (Tournament.Bracket.HIDDEN_SLOT.equals(id)) return SlotState.HIDDEN;
		if (id == null || id.isEmpty()) {
			if (seedRow) return SlotState.EMPTY;
			return data.isRevealed() ? SlotState.PENDING : SlotState.HIDDEN;
		}
		if (defeated) return SlotState.DEFEATED;
		if (current) return SlotState.CURRENT;
		if (Tournament.Bracket.isPlayerSlot(id)) return SlotState.PLAYER;
		return SlotState.UPCOMING;
	}

	private List<String> gauntletCard() {
		List<String> card = new ArrayList<>(data.getSeeds());
		card.add(data.getSemifinalist());
		card.add(data.getChampion());
		return card;
	}

	private float[] gauntletSize() {
		int cells = gauntletCard().size() + (showGauntletPlayer() ? 1 : 0);
		return new float[]{cells * GAUNTLET_PITCH - (GAUNTLET_PITCH - SLOT) + GAUNTLET_MARGIN * 2, SLOT + 34};
	}

	private boolean showGauntletPlayer() {
		return data.isRevealed() && !data.isEliminated() && data.getPhase() != TournamentPackets.OpenBracketS2C.Phase.RULES;
	}

	private void renderGauntlet(GuiGraphics graphics) {
		List<String> card = gauntletCard();
		int currentRound = data.isRevealed() ? data.getRound() : -1;
		boolean showPlayer = showGauntletPlayer();

		List<String> cells = new ArrayList<>();
		List<SlotState> states = new ArrayList<>();
		List<Integer> rounds = new ArrayList<>();
		int versusAfter = -1;

		for (int i = 0; i < card.size(); i++) {
			if (showPlayer && i == currentRound && !data.isCompleted()) {
				versusAfter = cells.size();
				cells.add(Tournament.Bracket.PLAYER_SLOT);
				states.add(SlotState.PLAYER);
				rounds.add(-1);
			}
			String id = card.get(i);
			cells.add(id);
			rounds.add(i);
			if (Tournament.Bracket.HIDDEN_SLOT.equals(id)) states.add(SlotState.HIDDEN);
			else if (currentRound >= 0 && i < currentRound) states.add(SlotState.DEFEATED);
			else if (i == currentRound && showPlayer && !data.isCompleted()) states.add(SlotState.CURRENT);
			else states.add(SlotState.UPCOMING);
		}
		if (showPlayer && data.isCompleted()) {
			cells.add(Tournament.Bracket.PLAYER_SLOT);
			states.add(SlotState.PLAYER);
			rounds.add(-1);
		}

		float top = 22;
		float left = GAUNTLET_MARGIN;
		for (int i = 0; i < cells.size(); i++) {
			float x = left + i * GAUNTLET_PITCH;
			String id = cells.get(i);
			SlotState state = states.get(i);

			if (i < cells.size() - 1 && i != versusAfter) {
				int color = state == SlotState.DEFEATED ? LINE_DONE_COLOR : LINE_COLOR;
				HudRender.rect(graphics, x + SLOT + 3, top + SLOT / 2.0f, GAUNTLET_PITCH - SLOT - 6, 1.0f, color);
			}
			drawSlot(graphics, x, top, id, state);

			int round = rounds.get(i);
			if (round >= 0) {
				MutableComponent label = round >= card.size() - 1 ? tr("tournament.dragonminez.round.final")
						: round == card.size() - 2 ? tr("tournament.dragonminez.round.semi")
						: tr("tournament.dragonminez.round.qualifier", round + 1);
				TextUtil.drawCenteredStringWithBorder(graphics, font, label, Math.round(x + SLOT / 2.0f), Math.round(top) - 12,
						state == SlotState.CURRENT ? TITLE_COLOR : MUTED_COLOR);
			}
		}

		if (versusAfter >= 0) {
			float vsX = left + versusAfter * GAUNTLET_PITCH + SLOT + (GAUNTLET_PITCH - SLOT) / 2.0f;
			TextUtil.drawCenteredStringWithBorder(graphics, font, tr("tournament.dragonminez.vs"), Math.round(vsX), Math.round(top + SLOT / 2.0f) - 4, 0xFF5555);
		}
	}

	private void drawSlot(GuiGraphics graphics, float x, float y, String id, SlotState state) {
		slotHits.add(new SlotHit(x, y, id, state));
		HudRender.rect(graphics, x, y, SLOT, SLOT, state == SlotState.DEFEATED ? 0xD0080810 : 0xD0101018);

		if (state == SlotState.HIDDEN || state == SlotState.CROWN) {
			graphics.pose().pushPose();
			graphics.pose().translate(x + SLOT / 2.0f, y + SLOT / 2.0f, 0.0f);
			graphics.pose().scale(2.0f, 2.0f, 1.0f);
			TextUtil.drawCenteredStringWithBorder(graphics, font, txt(state == SlotState.CROWN ? "★" : "?"), 0, -4,
					state == SlotState.CROWN ? TITLE_COLOR : MUTED_COLOR);
			graphics.pose().popPose();
		} else if (state != SlotState.EMPTY && state != SlotState.PENDING) {
			boolean playerSlot = Tournament.Bracket.isPlayerSlot(id);
			renderPortrait(graphics, id, x, y, playerSlot, state == SlotState.DEFEATED);
		}

		graphics.pose().pushPose();
		graphics.pose().translate(0.0f, 0.0f, 300.0f);
		boolean dim = state == SlotState.DEFEATED || state == SlotState.PENDING || state == SlotState.EMPTY;
		if (dim) graphics.setColor(0.45F, 0.45F, 0.50F, 1.0F);
		HudRender.blit(graphics, SLOT_FRAMES, x, y, state.u, state.v, SLOT, SLOT, FRAME_SOURCE, FRAME_SOURCE, 256, 256);
		if (dim) graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		graphics.pose().popPose();
	}

	private void renderPortrait(GuiGraphics graphics, String id, float boxX, float boxY, boolean isPlayer, boolean greyed) {
		LivingEntity entity = isPlayer ? playerPortrait(id) : portraitFor(id);
		if (entity == null) {
			if (isPlayer) {
				TextUtil.drawCenteredStringWithBorder(graphics, font, slotName(id).copy(), Math.round(boxX + SLOT / 2.0f),
						Math.round(boxY + SLOT / 2.0f) - 4, greyed ? 0x7A7A85 : 0xFFFFFF);
			}
			return;
		}

		float height = Math.max(0.6F, entity.getBbHeight() * renderScale(entity));
		int scale = Mth.clamp(Math.round((SLOT - PORTRAIT_PAD * 2) / (height * PORTRAIT_SPAN)), 6, 140);
		int feetY = Math.round(boxY + SLOT * PORTRAIT_HEADROOM + height * scale);

		graphics.enableScissor(screenX(boxX + 1), screenY(boxY + 1), screenX(boxX + SLOT - 1), screenY(boxY + SLOT - 1));
		if (greyed) {
			renderGreyedEntity(graphics, Math.round(boxX + SLOT / 2.0f), feetY, scale, entity);
		} else {
			InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, Math.round(boxX + SLOT / 2.0f), feetY,
					scale, 0.0F, 0.0F, entity);
		}
		graphics.disableScissor();
	}

	private int screenX(float localX) {
		return toScreenCoord(originX + localX * contentScale);
	}

	private int screenY(float localY) {
		return toScreenCoord(originY + localY * contentScale);
	}

	private static LivingEntity playerPortrait(String slot) {
		Minecraft mc = Minecraft.getInstance();
		if (Tournament.Bracket.PLAYER_SLOT.equals(slot)) return mc.player;

		UUID id = Tournament.Bracket.playerOf(slot);
		if (id == null || mc.level == null) return mc.player;
		if (mc.player != null && mc.player.getUUID().equals(id)) return mc.player;
		return mc.level.getPlayerByUUID(id);
	}

	private static float renderScale(LivingEntity entity) {
		return entity instanceof DBSagasEntity saga ? Math.max(0.1F, saga.getScale()) : 1.0F;
	}

	private static void renderGreyedEntity(GuiGraphics graphics, int x, int y, int scale, LivingEntity entity) {
		Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);

		float bodyRot = entity.yBodyRot;
		float yRot = entity.getYRot();
		float xRot = entity.getXRot();
		float headRotO = entity.yHeadRotO;
		float headRot = entity.yHeadRot;

		entity.yBodyRot = 180.0F;
		entity.setYRot(180.0F);
		entity.setXRot(0.0F);
		entity.yHeadRot = 180.0F;
		entity.yHeadRotO = 180.0F;

		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 50.0D);
		graphics.pose().mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
		graphics.pose().mulPose(pose);

		Lighting.setupForEntityInInventory();
		EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		dispatcher.setRenderShadow(false);

		MultiBufferSource greyed = renderType -> new GreyingVertexConsumer(graphics.bufferSource().getBuffer(renderType));
		RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F,
				graphics.pose(), greyed, 15728880));

		graphics.flush();
		dispatcher.setRenderShadow(true);
		graphics.pose().popPose();
		Lighting.setupFor3DItems();

		entity.yBodyRot = bodyRot;
		entity.setYRot(yRot);
		entity.setXRot(xRot);
		entity.yHeadRotO = headRotO;
		entity.yHeadRot = headRot;
	}

	private static final class GreyingVertexConsumer extends VertexConsumerWrapper {
		private static final float TINT = 0.32F;

		private GreyingVertexConsumer(VertexConsumer parent) {
			super(parent);
		}

		@Override
		public VertexConsumer color(int r, int g, int b, int a) {
			int grey = (int) ((r + g + b) / 3.0F * TINT);
			parent.color(grey, grey, Math.min(255, grey + 8), a);
			return this;
		}
	}

	private static LivingEntity portraitFor(String id) {
		if (id == null || id.isEmpty()) return null;
		if (PORTRAIT_CACHE.containsKey(id)) return PORTRAIT_CACHE.get(id);

		LivingEntity created = null;
		ResourceLocation location = ResourceLocation.tryParse(id);
		Minecraft mc = Minecraft.getInstance();
		if (location != null && mc.level != null) {
			EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(location);
			if (type != null) {
				Entity entity = type.create(mc.level);
				if (entity instanceof LivingEntity living) created = living;
				else if (entity != null) entity.discard();
			}
		}
		PORTRAIT_CACHE.put(id, created);
		return created;
	}

	private void renderScrollbar(GuiGraphics graphics) {
		if (maxScroll <= 0.0f) return;
		float fraction = currentScroll / maxScroll;
		if (verticalScroll) {
			int x = contentX + contentWidth - SCROLLBAR;
			HudRender.rect(graphics, x, contentY, SCROLLBAR, contentHeight, TRACK_COLOR);
			float thumb = Math.max(12.0f, contentHeight * contentHeight / (contentHeight + maxScroll));
			HudRender.rect(graphics, x, contentY + (contentHeight - thumb) * fraction, SCROLLBAR, thumb, THUMB_COLOR);
		} else {
			int y = contentY + contentHeight - SCROLLBAR;
			HudRender.rect(graphics, contentX, y, contentWidth, SCROLLBAR, TRACK_COLOR);
			float thumb = Math.max(12.0f, contentWidth * contentWidth / (contentWidth + maxScroll));
			HudRender.rect(graphics, contentX + (contentWidth - thumb) * fraction, y, thumb, SCROLLBAR, THUMB_COLOR);
		}
	}

	private void renderFooter(GuiGraphics graphics) {
		int y = guiTop + guiHeight - PADDING - BUTTON_HEIGHT + 6;
		MutableComponent hint = hintLine();
		int buttons = 0;
		for (var widget : this.renderables) if (widget instanceof TexturedTextButton) buttons++;
		int buttonsWidth = buttons * BUTTON_WIDTH + Math.max(0, buttons - 1) * 6;
		int maxWidth = Math.max(60, (guiWidth - buttonsWidth) / 2 - PADDING - 8);
		List<FormattedCharSequence> lines = font.split(hint, maxWidth);
		for (int i = 0; i < Math.min(2, lines.size()); i++) {
			TextUtil.drawStringWithBorder(graphics, font, lines.get(i), guiLeft + PADDING, y - 4 + i * font.lineHeight, MUTED_COLOR);
		}

		if (data.getPhase() == TournamentPackets.OpenBracketS2C.Phase.PREVIEW && !data.isSignUp()) {
			MutableComponent countdown = tr("tournament.dragonminez.preview_countdown", Math.max(1, phaseSecondsLeft()));
			graphics.pose().pushPose();
			graphics.pose().translate(guiLeft + guiWidth - PADDING, y + 4, 0.0f);
			graphics.pose().scale(1.3f, 1.3f, 1.0f);
			TextUtil.drawStringWithBorder(graphics, font, countdown, -font.width(countdown), -4, TITLE_COLOR);
			graphics.pose().popPose();
		}
	}

	private MutableComponent hintLine() {
		return switch (view()) {
			case SIGNUP -> tr("tournament.dragonminez.hint.signup");
			case LOCKED -> tr("tournament.dragonminez.hint.locked");
			case RULES -> data.getMyState() == TournamentPackets.OpenBracketS2C.MemberState.PENDING
					? tr("tournament.dragonminez.hint.rules") : tr("tournament.dragonminez.hint.answered");
			case BRACKET -> awaitingMyReady() ? tr("tournament.dragonminez.hint.accept")
					: data.isGauntlet() ? tr("tournament.dragonminez.hint.gauntlet") : tr("tournament.dragonminez.hint.bracket");
		};
	}

	private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
		if (hoveredSlot != null) {
			List<Component> description = new ArrayList<>();
			List<Component> extras = null;
			Component title;
			String id = hoveredSlot.id();
			if (hoveredSlot.state() == SlotState.HIDDEN) {
				title = tr("tournament.dragonminez.slot.hidden");
				description.add(tr("tournament.dragonminez.slot.hidden_hint").withStyle(ChatFormatting.GRAY));
			} else if (hoveredSlot.state() == SlotState.EMPTY) {
				title = tr("tournament.dragonminez.slot.empty");
			} else if (hoveredSlot.state() == SlotState.PENDING) {
				title = tr("tournament.dragonminez.slot.pending");
			} else if (hoveredSlot.state() == SlotState.CROWN) {
				title = tr("tournament.dragonminez.slot.champion_seat");
				description.add(tr("tournament.dragonminez.slot.pending").withStyle(ChatFormatting.GRAY));
			} else {
				title = slotName(id).copy();
				LivingEntity dummy = Tournament.Bracket.isPlayerSlot(id) ? null : portraitFor(id);
				Minecraft mc = Minecraft.getInstance();
				TournamentPackets.OpenBracketS2C.FighterStats stats = data.getStats().get(id);
				if (stats != null) {
					description.add(tr("tournament.dragonminez.stat.health", stats.health()).withStyle(ChatFormatting.RED));
					description.add(tr("tournament.dragonminez.stat.melee", stats.melee()).withStyle(ChatFormatting.GOLD));
					description.add(tr("tournament.dragonminez.stat.ki", stats.ki()).withStyle(ChatFormatting.AQUA));
				}
				if (isLocalPlayerSlot(id)) description.add(tr("tournament.dragonminez.slot.you").withStyle(ChatFormatting.GREEN));
				List<Component> threats = dummy != null ? collectThreats(dummy) : List.of();
				if (!threats.isEmpty()) {
					extras = new ArrayList<>();
					extras.add(tr("tournament.dragonminez.stat.threats").withStyle(ChatFormatting.BOLD));
					if (QuestUnlocks.isCompleted(mc.player, QuestUnlocks.SCOUTER_THREAT_DB)) extras.addAll(threats);
					else extras.add(tr("tournament.dragonminez.stat.scan_required").withStyle(ChatFormatting.DARK_GRAY));
				}
			}
			TextUtil.renderAdvancedTooltip(graphics, font, mouseX, mouseY, getUiWidth(), getUiHeight(), title, description, extras, 0x6D8CFF);
			return;
		}
		if (hoveredMember != null) {
			TournamentPackets.OpenBracketS2C.Member member = hoveredMember.member();
			List<Component> description = new ArrayList<>();
			if (member.leader()) description.add(tr(data.getMembers().size() > 1
					? "tournament.dragonminez.member.leader" : "tournament.dragonminez.member.solo").withStyle(ChatFormatting.GOLD));
			if (member.active() && member.ready()) description.add(tr("tournament.dragonminez.member.ready"));
			else description.add(tr(member.active() ? "tournament.dragonminez.member.active"
					: "tournament.dragonminez.member." + member.state().name().toLowerCase(Locale.ROOT)));
			TextUtil.renderAdvancedTooltip(graphics, font, mouseX, mouseY, getUiWidth(), getUiHeight(),
					txt(member.name()), description, null, 0x6D8CFF);
		}
	}

	private List<Component> collectThreats(LivingEntity entity) {
		List<Component> out = new ArrayList<>();
		if (!(entity instanceof DBSagasEntity dbz)) return out;

		Set<String> seen = new HashSet<>();
		for (DBSagasEntity.KiSkill skill : dbz.getSkillPool()) {
			DBSagasEntity.KiSkillType type = DBSagasEntity.KiSkillType.fromId(skill.id);
			if (type == null) continue;
			String key = "gui.dragonminez.quest_tree.preview.skill." + type.name().toLowerCase(Locale.ROOT);
			if (seen.add(key)) out.add(txt("• ").append(tr(key)).withStyle(s -> s.withColor(0xFF8AB4F8)));
		}
		int[] combos = dbz.getAllowedCombos();
		if (combos != null) {
			for (int id : combos) {
				DBSagasEntity.ComboType type = DBSagasEntity.ComboType.fromId(id);
				if (type == null) continue;
				String key = "gui.dragonminez.quest_tree.preview.combo." + type.name().toLowerCase(Locale.ROOT);
				if (seen.add(key)) out.add(txt("• ").append(tr(key)).withStyle(s -> s.withColor(0xFFF08A8A)));
			}
		}
		return out;
	}

	private boolean isLocalPlayerSlot(String id) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return false;
		if (Tournament.Bracket.PLAYER_SLOT.equals(id)) return true;
		UUID uuid = Tournament.Bracket.playerOf(id);
		return uuid != null && uuid.equals(mc.player.getUUID());
	}

	private MutableComponent slotName(String id) {
		if (Tournament.Bracket.PLAYER_SLOT.equals(id)) {
			return Minecraft.getInstance().player != null
					? txt(Minecraft.getInstance().player.getName().getString())
					: tr("tournament.dragonminez.you");
		}
		if (Tournament.Bracket.isPlayerSlot(id)) {
			String name = data.getSlotNames().get(id);
			return name != null ? txt(name) : tr("tournament.dragonminez.you");
		}
		if (Tournament.Bracket.HIDDEN_SLOT.equals(id)) return tr("tournament.dragonminez.slot.hidden");
		if (id == null || id.isEmpty()) return txt("-");

		ResourceLocation location = ResourceLocation.tryParse(id);
		if (location == null) return txt(id);
		EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(location);
		return type != null ? tr(type.getDescriptionId()) : txt(id);
	}

	private boolean overThumb(double uiX, double uiY) {
		if (maxScroll <= 0.0f) return false;
		if (verticalScroll) {
			int x = contentX + contentWidth - SCROLLBAR;
			return uiX >= x - 3 && uiX <= x + SCROLLBAR + 3 && uiY >= contentY && uiY <= contentY + contentHeight;
		}
		int y = contentY + contentHeight - SCROLLBAR;
		return uiY >= y - 3 && uiY <= y + SCROLLBAR + 3 && uiX >= contentX && uiX <= contentX + contentWidth;
	}

	private void scrollFromMouse(double uiX, double uiY) {
		if (verticalScroll) {
			float thumb = Math.max(12.0f, contentHeight * contentHeight / (contentHeight + maxScroll));
			float track = contentHeight - thumb;
			float fraction = track <= 0.0f ? 0.0f : (float) (uiY - contentY - thumb / 2.0f) / track;
			targetScroll = Mth.clamp(fraction, 0.0f, 1.0f) * maxScroll;
		} else {
			float thumb = Math.max(12.0f, contentWidth * contentWidth / (contentWidth + maxScroll));
			float track = contentWidth - thumb;
			float fraction = track <= 0.0f ? 0.0f : (float) (uiX - contentX - thumb / 2.0f) / track;
			targetScroll = Mth.clamp(fraction, 0.0f, 1.0f) * maxScroll;
		}
		currentScroll = targetScroll;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && overThumb(toUiX(mouseX), toUiY(mouseY))) {
			draggingThumb = true;
			scrollFromMouse(toUiX(mouseX), toUiY(mouseY));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (draggingThumb && button == 0) {
			scrollFromMouse(toUiX(mouseX), toUiY(mouseY));
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) draggingThumb = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (maxScroll > 0.0f) {
			targetScroll = Mth.clamp(targetScroll - (float) delta * SCROLL_STEP, 0.0f, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public void onClose() {
		PORTRAIT_CACHE.clear();
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static String formatDuration(long seconds) {
		long minutes = seconds / 60L;
		long rest = seconds % 60L;
		return minutes > 0L ? minutes + "m " + rest + "s" : rest + "s";
	}
}
