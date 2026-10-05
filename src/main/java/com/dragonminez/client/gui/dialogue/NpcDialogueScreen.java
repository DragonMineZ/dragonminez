package com.dragonminez.client.gui.dialogue;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.util.ScrollbarState;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import com.dragonminez.common.network.S2C.DialogueResultS2C;
import com.dragonminez.common.network.S2C.OpenDialogueNodeS2C;
import com.dragonminez.common.network.S2C.OpenQuestNPCDialogueS2C;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class NpcDialogueScreen extends ScaledScreen {
	static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private static final int MARGIN = 16;
	private static final int GAP = 14;
	private static final int TEXT_MAX_W = 420;
	private static final int TEXT_TOP = 14;
	private static final int LINE_H = 11;
	private static final int TAB_RISE = 15;
	private static final int FOOTER_W = 88;
	private static final int FOOTER_H = 20;
	private static final int FOOTER_BOTTOM = 8;
	private static final int FOOTER_GAP = 6;
	private static final int OPTIONS_TOP = 12;
	private static final int INPUT_H = 18;
	private static final int INPUT_W = 150;
	private static final int MIN_BAND_H = 118;
	private static final int SHADOW_H = 28;
	private static final int PORTRAIT_MIN_W = 120;
	private static final int PORTRAIT_MAX_W = 200;
	private static final float BAND_SHARE = 0.35f;
	private static final float PORTRAIT_SHARE = 0.22f;
	private static final float HEAD_SHARE = 0.24f;
	private static final float HEAD_LIFT = 0.40f;
	private static final int PORTRAIT_FADE = 26;
	private static final float OVERLAY_Z = 450.0f;
	private static final float NAME_SCALE = 1.15f;
	private static final long OPEN_MS = 380L;
	private static final long CLOSE_MS = 200L;
	private static final float DIM_ALPHA = 0.32f;
	private static final long CARET_BLINK_MS = 480L;
	private static final int MAX_INPUT_DIGITS = 9;

	private final String npcId;
	private final int entityId;
	private final Deque<DialoguePage> history = new ArrayDeque<>();
	private final Typewriter typewriter = new Typewriter();
	private final OptionColumn column = new OptionColumn();
	private final QuestSheet questSheet = new QuestSheet();
	private final ScrollbarState textBar = new ScrollbarState().step(LINE_H * 2).minThumb(10)
			.colors(DialogueSkin.SCROLL_TRACK, DialogueSkin.SCROLL_THUMB, DialogueSkin.SCROLL_ACTIVE);
	private final StringBuilder input = new StringBuilder();

	private NpcDialogueContent content;
	private boolean treeMode;
	private boolean awaitingServer;
	private DialoguePage page;
	private Component override;
	private Component shownLine = Component.empty();
	private boolean lineDirty = true;
	private int wrappedWidth = -1;
	private boolean followTyping = true;
	private long openedAt;
	private long closingAt;
	private int ticks;

	private int bandTop;
	private int bandH;
	private int portraitW;
	private int optionsX;
	private int textX;
	private int textY;
	private int textW;
	private int textH;
	private int footerY;

	private NpcDialogueScreen(String npcId, int entityId, boolean masterNpc, List<DialogueQuest> quests) {
		super(Component.translatable("entity.dragonminez.questnpc." + npcId).withStyle(Style.EMPTY.withFont(FONT)));
		this.npcId = npcId;
		this.entityId = entityId;
		this.content = new NpcDialogueContent(this, npcId, masterNpc, quests);
		setPage(content.root());
	}

	public static void open(OpenQuestNPCDialogueS2C msg) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) return;
		List<DialogueQuest> quests = DialogueQuest.from(msg);
		if (minecraft.screen instanceof NpcDialogueScreen current && current.treeMode && current.entityId == msg.getEntityId()) {
			current.treeMode = false;
			current.awaitingServer = false;
			current.content = new NpcDialogueContent(current, msg.getNpcId(), msg.isMasterNpc(), quests);
			current.history.clear();
			current.setPage(current.content.questsAsRoot());
			return;
		}
		minecraft.setScreen(new NpcDialogueScreen(msg.getNpcId(), msg.getEntityId(), msg.isMasterNpc(), quests));
	}

	public static void handleResult(DialogueResultS2C msg) {
		if (Minecraft.getInstance().screen instanceof NpcDialogueScreen current && current.closingAt == 0L
				&& current.content.npcId().equals(msg.getNpcId())) {
			current.content.onResult(msg);
		}
	}

	public static void handleNode(OpenDialogueNodeS2C msg) {
		Minecraft minecraft = Minecraft.getInstance();
		if (msg.isClose()) {
			if (minecraft.screen instanceof NpcDialogueScreen current && current.treeMode) current.onClose();
			return;
		}
		if (minecraft.screen instanceof NpcDialogueScreen current && current.treeMode
				&& current.npcId.equals(msg.getNpcId()) && current.entityId == msg.getEntityId()) {
			current.showNode(msg);
			return;
		}
		NpcDialogueScreen screen = new NpcDialogueScreen(msg.getNpcId(), msg.getEntityId(), true, List.of());
		screen.showNode(msg);
		minecraft.setScreen(screen);
	}

	@Override
	public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		long now = Util.getMillis();
		if (openedAt == 0L) openedAt = now;
		float open = ease((now - openedAt) / (float) OPEN_MS);
		if (closingAt > 0L) {
			float closing = Mth.clamp((now - closingAt) / (float) CLOSE_MS, 0.0f, 1.0f);
			open = Math.min(open, 1.0f - closing * closing);
		}

		beginUiScale(graphics);
		layout();
		double mx = toUiX(mouseX);
		double my = toUiY(mouseY);
		typewriter.update(now);
		HudRender.rect(graphics, 0, 0, getUiWidth(), getUiHeight(), HudRender.argb(DIM_ALPHA * open, 0x000000));

		float slide = (1.0f - open) * (bandH + SHADOW_H);
		double bandMouseY = my - slide;
		graphics.pose().pushPose();
		graphics.pose().translate(0.0f, slide, 0.0f);
		renderBand(graphics, open);
		renderPortrait(graphics, slide);
		ItemStack hoveredItem = renderSpeech(graphics, mx, bandMouseY, open, now);
		renderFooter(graphics, mx, bandMouseY, open, now);
		column.render(graphics, this.font, mx, bandMouseY, open, frameEase(0.06f), now, page.isQuest() ? page.id : null,
				treeMode && awaitingServer, () -> playClick(0.7f));
		graphics.pose().popPose();

		column.renderTooltip(graphics, this.font, mx, my, getUiWidth(), getUiHeight(), now);
		if (hoveredItem != null && !hoveredItem.isEmpty()) graphics.renderTooltip(this.font, hoveredItem, (int) mx, (int) my);
		endUiScale(graphics);
	}

	@Override
	public void tick() {
		super.tick();
		if (closingAt > 0L) {
			if (Util.getMillis() - closingAt >= CLOSE_MS) Minecraft.getInstance().setScreen(null);
			return;
		}
		if (++ticks % 10 == 0) refresh(false);
	}

	@Override
	public void onClose() {
		if (closingAt == 0L) closingAt = Util.getMillis();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (closingAt > 0L) return true;
		double x = toUiX(mouseX);
		double y = toUiY(mouseY);
		if (textBar.mouseClicked(x, y, button)) {
			followTyping = false;
			return true;
		}
		if (column.mouseClicked(x, y, button)) return true;
		if (button == 1) {
			if (page.root) return false;
			playClick(0.8f);
			pop();
			return true;
		}
		if (button != 0) return false;

		for (FooterButton footerButton : footer()) {
			if (inside(x, y, footerButton.x(), footerY, FOOTER_W, FOOTER_H)) {
				playClick(1.0f);
				footerButton.action().run();
				return true;
			}
		}

		int index = column.rowAt(x, y);
		if (index >= 0) {
			activate(column.options().get(index));
			return true;
		}

		if (inside(x, y, textX, textY, textW, textH) && !typewriter.finished()) {
			typewriter.finish();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		double x = toUiX(mouseX);
		double y = toUiY(mouseY);
		return textBar.mouseDragged(x, y) || column.mouseDragged(x, y);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean text = textBar.mouseReleased();
		return column.mouseReleased() || text;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (closingAt > 0L) return true;
		double x = toUiX(mouseX);
		double y = toUiY(mouseY);
		if (inside(x, y, textX, textY, textW, textH) && textBar.scrollWheel(delta)) {
			followTyping = false;
			return true;
		}
		return column.mouseScrolled(x, y, delta);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		if (column.rowAt(toUiX(mouseX), toUiY(mouseY)) >= 0) column.clearFocus();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (closingAt > 0L) return true;
		long now = Util.getMillis();
		if (page.hasInput() && handleInputKey(keyCode, now)) return true;

		switch (keyCode) {
			case GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
				if (!typewriter.finished()) {
					typewriter.finish();
					return true;
				}
				DialogueOption focusedOption = column.focusedOption();
				if (focusedOption != null) {
					activate(focusedOption);
					return true;
				}
			}
			case GLFW.GLFW_KEY_UP -> {
				column.moveFocus(-1, now);
				return true;
			}
			case GLFW.GLFW_KEY_DOWN -> {
				column.moveFocus(1, now);
				return true;
			}
			case GLFW.GLFW_KEY_BACKSPACE -> {
				if (!page.root) {
					playClick(0.8f);
					pop();
					return true;
				}
			}
			default -> {
				if (!page.hasInput() && keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
					int index = keyCode - GLFW.GLFW_KEY_1;
					if (index < column.options().size()) activate(column.options().get(index));
					return true;
				}
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (closingAt > 0L) return true;
		if (!page.hasInput() || !Character.isDigit(codePoint)) return super.charTyped(codePoint, modifiers);
		int limit = Math.min(page.inputDigits, MAX_INPUT_DIGITS);
		if (input.length() < limit && !(input.isEmpty() && codePoint == '0')) {
			input.append(codePoint);
			refresh(false);
		}
		return true;
	}

	void push(DialoguePage next) {
		history.push(page);
		setPage(next);
	}

	void pop() {
		if (history.isEmpty()) {
			onClose();
			return;
		}
		setPage(history.pop());
	}

	void popToRoot() {
		DialoguePage first = page;
		while (!history.isEmpty()) first = history.pop();
		setPage(first);
	}

	void showQuest(DialoguePage questPage) {
		if (page != null && page.isQuest()) setPage(questPage);
		else push(questPage);
	}

	void say(Component line) {
		override = line;
		lineDirty = true;
		followTyping = true;
		textBar.reset();
		refresh(false);
	}

	void close() {
		onClose();
	}

	void openScreen(Screen screen) {
		Minecraft.getInstance().setScreen(screen);
	}

	Screen screen() {
		return this;
	}

	int entityId() {
		return entityId;
	}

	LivingEntity npcEntity() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || entityId < 0) return null;
		Entity entity = minecraft.level.getEntity(entityId);
		return entity instanceof LivingEntity living && entity.isAlive() ? living : null;
	}

	int inputValue() {
		return input.isEmpty() ? 0 : Integer.parseInt(input.toString());
	}

	void awaitServer() {
		awaitingServer = true;
		refresh(false);
	}

	void refreshOptions() {
		refresh(false);
	}

	boolean awaitingServer() {
		return awaitingServer;
	}

	static float ease(float t) {
		float inverse = 1.0f - Mth.clamp(t, 0.0f, 1.0f);
		return 1.0f - inverse * inverse * inverse;
	}

	@Override
	protected int getMinGuiWidth() {
		return 560;
	}

	@Override
	protected int getMinGuiHeight() {
		return 260;
	}

	private void showNode(OpenDialogueNodeS2C msg) {
		treeMode = true;
		awaitingServer = false;
		history.clear();
		setPage(content.tree(msg));
	}

	private void setPage(DialoguePage next) {
		page = next;
		override = null;
		lineDirty = true;
		followTyping = true;
		input.setLength(0);
		column.clearFocus();
		questSheet.invalidate();
		textBar.reset();
		if (next.isQuest()) typewriter.finish();
		refresh(true);
	}

	private void refresh(boolean pageChanged) {
		Component next = override != null ? override : page.line.get();
		if (pageChanged || lineDirty || !next.getString().equals(shownLine.getString())) {
			shownLine = next;
			wrappedWidth = -1;
		}
		long now = Util.getMillis();
		column.sync(page.options.get(), now, openedAt == 0L || now - openedAt < OPEN_MS);
	}

	private boolean handleInputKey(int keyCode, long now) {
		if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
			if (input.isEmpty()) {
				pop();
			} else {
				input.deleteCharAt(input.length() - 1);
				refresh(false);
			}
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			column.options().stream().filter(option -> option.available(now)).findFirst().ifPresent(this::activate);
			return true;
		}
		return false;
	}

	private void layout() {
		int width = getUiWidth();
		int height = getUiHeight();
		bandH = Math.max(MIN_BAND_H, Math.round(height * BAND_SHARE));
		bandTop = height - bandH;
		portraitW = Mth.clamp(Math.round(width * PORTRAIT_SHARE), PORTRAIT_MIN_W, PORTRAIT_MAX_W);
		optionsX = width - MARGIN - OptionColumn.WIDTH;
		textX = MARGIN + portraitW + GAP;
		textW = Math.min(TEXT_MAX_W, optionsX - GAP - textX);
		textY = bandTop + TEXT_TOP;
		footerY = height - FOOTER_BOTTOM - FOOTER_H;
		textH = footerY - FOOTER_GAP - textY;
		column.place(optionsX, bandTop + OPTIONS_TOP, height - FOOTER_BOTTOM - bandTop - OPTIONS_TOP);
	}

	private void renderBand(GuiGraphics graphics, float open) {
		int width = getUiWidth();
		DialogueSkin.shadow(graphics, 0, bandTop - SHADOW_H, width, SHADOW_H, open);
		DialogueSkin.band(graphics, 0, bandTop, width, bandH + SHADOW_H, 1.0f);
		DialogueSkin.line(graphics, 0, bandTop - 2, width, 1.0f);
	}

	private void renderPortrait(GuiGraphics graphics, float slide) {
		LivingEntity entity = npcEntity();
		if (entity == null) return;
		float headSize = getUiHeight() * HEAD_SHARE;
		float headX = MARGIN + portraitW / 2.0f;
		float headY = bandTop - headSize * HEAD_LIFT;
		float bandClip = textX - GAP / 2.0f;
		NpcPortrait.render(graphics, entity, 0, -slide, optionsX - GAP, bandTop, headX, headY, headSize);
		NpcPortrait.render(graphics, entity, 0, bandTop, bandClip, bandTop + bandH, headX, headY, headSize);
		graphics.pose().pushPose();
		graphics.pose().translate(0.0f, 0.0f, OVERLAY_Z);
		HudRender.rectHorizontal(graphics, bandClip - PORTRAIT_FADE, bandTop + DialogueSkin.LINE_H - 2, PORTRAIT_FADE, bandH,
				HudRender.argb(0.0f, DialogueSkin.BAND_RGB), HudRender.argb(0.94f, DialogueSkin.BAND_RGB));
		graphics.pose().popPose();
	}

	private ItemStack renderSpeech(GuiGraphics graphics, double mx, double my, float open, long now) {
		Component name = npcName();
		float tabWidth = this.font.width(name) * NAME_SCALE + 34.0f;
		float tabX = textX - 4;
		float tabY = bandTop - TAB_RISE;
		graphics.pose().pushPose();
		graphics.pose().translate(0.0f, 0.0f, OVERLAY_Z);
		DialogueSkin.tab(graphics, tabX, tabY, Math.min(tabWidth, textW), 1.0f);
		HudRender.dmzText(graphics, name, tabX + 10, tabY + 4, NAME_SCALE, 0.0f, DialogueSkin.TEXT, 1.0f);
		graphics.pose().popPose();

		int areaH = textH - (page.hasInput() ? INPUT_H + FOOTER_GAP : 0);
		int barX = textX + textW - ScrollbarState.DEFAULT_BAR_WIDTH;
		ItemStack hovered = null;
		if (page.isQuest()) hovered = questSheet.render(graphics, this.font, textBar, page.quest, textX, textY, textW, areaH, barX, mx, my, open);
		else renderLine(graphics, textX, textY, textW, areaH, barX, mx, my, open, now);

		if (page.hasInput()) renderInput(graphics, textX + 2, textY + areaH + FOOTER_GAP, Math.min(INPUT_W, textW), open, now);
		return hovered;
	}

	private void renderLine(GuiGraphics graphics, int x, int y, int width, int height, int barX, double mx, double my, float open, long now) {
		if (lineDirty || wrappedWidth != width) {
			List<FormattedCharSequence> lines = TextUtil.wrapScrollable(this.font, shownLine, width, height, LINE_H, textBar);
			if (lineDirty) typewriter.start(lines, now);
			else typewriter.replace(lines);
			lineDirty = false;
			wrappedWidth = width;
		}

		List<FormattedCharSequence> lines = typewriter.lines();
		textBar.layout(x, y, width, height, lines.size() * LINE_H).barAt(barX);
		if (followTyping && !typewriter.finished()) {
			int cursor = typewriter.cursorLine();
			textBar.ensureVisible(y + cursor * LINE_H, y + (cursor + 1) * LINE_H);
		}

		textBar.beginClip(graphics);
		for (int i = 0; i < lines.size(); i++) {
			int lineY = y + i * LINE_H;
			if (textBar.isVisible(lineY, LINE_H)) {
				HudRender.dmzText(graphics, typewriter.visible(i), x, lineY, 1.0f, 0.0f, DialogueSkin.TEXT, open);
			}
		}
		textBar.endClip(graphics);
		textBar.renderBar(graphics, mx, my);

		if (typewriter.finished() && textBar.canScroll() && textBar.scroll() < textBar.maxScroll() - 1.0f) {
			float bounce = (float) Math.abs(Math.sin(now / 260.0)) * 2.5f;
			DialogueSkin.icon(graphics, DialogueSkin.Icon.DOWN, x + width - 16, y + height - 8 + bounce, DialogueSkin.ACCENT, open);
		}
	}

	private void renderInput(GuiGraphics graphics, int x, int y, int width, float open, long now) {
		DialogueSkin.input(graphics, x - 2, y, width, INPUT_H, open);
		if (input.isEmpty()) {
			if (page.inputHint != null) HudRender.dmzText(graphics, page.inputHint, x + 4, y + 5, 1.0f, 0.0f, DialogueSkin.MUTED, open);
		} else {
			HudRender.dmzText(graphics, input.toString(), x + 4, y + 5, 1.0f, 0.0f, DialogueSkin.TEXT, open);
		}
		if ((now / CARET_BLINK_MS) % 2L == 0L) {
			float caretX = x + 4 + (input.isEmpty() ? 0.0f : HudRender.dmzWidth(input.toString(), 1.0f)) + 1;
			HudRender.rect(graphics, caretX, y + 4, 1, 10, HudRender.argb(open, DialogueSkin.ACCENT));
		}
	}

	private void renderFooter(GuiGraphics graphics, double mx, double my, float open, long now) {
		for (FooterButton button : footer()) {
			boolean hovered = inside(mx, my, button.x(), footerY, FOOTER_W, FOOTER_H);
			float x = button.x();
			float y = footerY;
			if (button.row() == DialogueSkin.Row.PRIMARY) {
				DialogueSkin.row(graphics, DialogueSkin.Row.PRIMARY, x, y, FOOTER_W, FOOTER_H, open);
				if (hovered) {
					float glow = 0.10f + 0.06f * (float) Math.sin(now / 140.0);
					HudRender.rect(graphics, x + 3, y + 2, FOOTER_W - 6, FOOTER_H - 4, HudRender.argb(glow * open, 0xFFFFFF));
				}
			} else {
				DialogueSkin.row(graphics, hovered ? DialogueSkin.Row.HOVER : DialogueSkin.Row.NORMAL, x, y, FOOTER_W, FOOTER_H, open);
			}
			HudRender.dmzText(graphics, button.label(), x + FOOTER_W / 2.0f, y + 6, 1.0f, 0.5f, DialogueSkin.TEXT, open);
		}
	}

	private List<FooterButton> footer() {
		List<FooterButton> buttons = new ArrayList<>();
		int left = textX;
		int right = textX + textW - FOOTER_W;
		if (page.isQuest()) {
			DialogueQuest quest = page.quest;
			switch (quest.status(Util.getMillis())) {
				case OFFER -> {
					buttons.add(new FooterButton(NpcDialogueContent.tr("gui.dragonminez.story.sidequests.accept"), DialogueSkin.Row.PRIMARY,
							left, () -> content.accept(quest)));
					buttons.add(new FooterButton(NpcDialogueContent.tr("gui.dragonminez.dialogue.reject"), DialogueSkin.Row.NORMAL, right, this::pop));
				}
				case TURN_IN -> {
					buttons.add(new FooterButton(NpcDialogueContent.tr("gui.dragonminez.sidequest.turn_in"), DialogueSkin.Row.PRIMARY,
							left, () -> content.turnIn(quest)));
					buttons.add(new FooterButton(NpcDialogueContent.tr("gui.dragonminez.customization.back"), DialogueSkin.Row.NORMAL, right, this::pop));
				}
				default -> buttons.add(new FooterButton(NpcDialogueContent.tr("gui.dragonminez.customization.back"), DialogueSkin.Row.NORMAL,
						left, this::pop));
			}
		} else if (page.root) {
			buttons.add(new FooterButton(NpcDialogueContent.tr("gui.dragonminez.close"), DialogueSkin.Row.NORMAL, left, this::onClose));
		} else {
			buttons.add(new FooterButton(NpcDialogueContent.tr("gui.dragonminez.customization.back"), DialogueSkin.Row.NORMAL, left, this::pop));
		}
		return buttons;
	}

	private Component npcName() {
		LivingEntity entity = npcEntity();
		if (entity != null && !(entity instanceof QuestNPCEntity) && I18n.exists(entity.getType().getDescriptionId())) {
			return NpcDialogueContent.tr(entity.getType().getDescriptionId());
		}
		return NpcDialogueContent.npcDisplayName(npcId);
	}

	private void activate(DialogueOption option) {
		if (treeMode && awaitingServer) return;
		long now = Util.getMillis();
		if (!option.available(now)) {
			if (option.lockedAction != null && !option.onCooldown(now)) {
				playClick(0.8f);
				option.lockedAction.run();
			}
			return;
		}
		playClick(1.0f);
		option.action.run();
	}

	private void playClick(float volume) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MainSounds.PIP_MENU.get(), 1.0f, volume));
	}

	private static boolean inside(double mx, double my, float x, float y, float width, float height) {
		return mx >= x && mx < x + width && my >= y && my < y + height;
	}

	private record FooterButton(Component label, DialogueSkin.Row row, int x, Runnable action) {
	}

	@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
	public static final class HudVisibility {
		private HudVisibility() {
		}

		@SubscribeEvent
		public static void onRenderGui(RenderGuiEvent.Pre event) {
			if (Minecraft.getInstance().screen instanceof NpcDialogueScreen) event.setCanceled(true);
		}

		@SubscribeEvent
		public static void onRenderHand(RenderHandEvent event) {
			if (Minecraft.getInstance().screen instanceof NpcDialogueScreen) event.setCanceled(true);
		}
	}
}
