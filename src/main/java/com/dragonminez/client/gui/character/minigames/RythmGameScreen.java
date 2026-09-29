package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TrainingConfig;
import com.dragonminez.common.network.S2C.MinigameStartS2C;
import com.dragonminez.common.training.MinigameEvent;
import com.dragonminez.common.training.MinigameLogic;
import com.dragonminez.common.training.MinigameLogics;
import com.dragonminez.common.training.MinigameOrigin;
import com.dragonminez.common.training.RhythmChart;
import com.dragonminez.common.training.RhythmLogic;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RythmGameScreen extends BaseMinigameScreen {
	private static final int[] LANE_COLORS = {0x5AD0E8, 0x6FD08A, 0xE8C75A, 0xB07CE8};
	private static final KeyMapping[] LANE_BINDS = {KeyBinds.RHYTHM_LEFT, KeyBinds.RHYTHM_DOWN, KeyBinds.RHYTHM_UP, KeyBinds.RHYTHM_RIGHT};
	private static final int[] LANE_DEFAULTS = {GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L};
	private static final int DROPDOWN_ROWS = 6;
	private static final int DROPDOWN_ROW_H = 13;
	private static final String[] JUDGEMENTS = {"gui.dragonminez.minigame.judge.miss", "gui.dragonminez.minigame.judge.good", "gui.dragonminez.minigame.judge.perfect"};
	private static final int[] JUDGEMENT_COLORS = {0xFFFF5555, 0xFF7CFDD6, 0xFFFFD700};
	private static final float TOP_SCALE = 0.5f;
	private static final float BOARD_HEIGHT_FRACTION = 0.75f;
	private static final int EFFECT_TICKS = 8;
	private static final ResourceLocation SHEET = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/minigame/rhythm.png");
	private static final int NOTES_U = 112;
	private static final int KEY_W = 36, KEY_H = 22;
	private static final int NOTE_W = 36, NOTE_H = 12;

	private static int rememberedTrack = -1;

	private record Track(String itemId, Component name) {}

	private final List<Track> tracks = new ArrayList<>();
	private final List<float[]> sparks = new ArrayList<>();
	private final Random sparkRandom = new Random();
	private int trackIndex;
	private Track activeTrack;
	private SoundInstance music;
	private boolean musicStarted;
	private long lastFrameNanos;
	private final int[] seenLaneJudge = {-100, -100, -100, -100};
	private int glowColor;
	private int glowTick = -100;

	private float cx, topY, hitY, vpY, bottomWidth;
	private float[] dropdownButton;
	private boolean dropdownOpen;
	private int dropdownScroll;
	private final long[] keyHitNanos = new long[RhythmLogic.LANES];
	private final long[] keyMissNanos = new long[RhythmLogic.LANES];
	private long boardEventNanos;
	private final SmoothValue healthShown = new SmoothValue();
	private final SmoothValue levelShown = new SmoothValue();
	private final SmoothValue accuracyShown = new SmoothValue();
	private final SmoothValue comboPop = new SmoothValue();
	private int shownLevel;
	private int shownCombo;
	private boolean boardMiss;
	private float boardStrength;

	public RythmGameScreen() {
		this(MinigameOrigin.MENU);
	}

	public RythmGameScreen(MinigameOrigin origin) {
		super(RhythmLogic.ID, "gui.dragonminez.minigame.rhythm", origin);
	}

	private RhythmLogic rhythm() {
		return (RhythmLogic) logic;
	}

	@Override
	protected void init() {
		super.init();
		buildTracks();
		cx = this.width / 2f;
		float boardBottom = this.height - 24f;
		topY = Math.max(HUD_RESERVE + 22f, boardBottom - this.height * BOARD_HEIGHT_FRACTION);
		bottomWidth = Math.min(Math.min(280f, this.width * 0.46f), (boardBottom - topY) * 0.95f);
		float keyScale = bottomWidth / RhythmLogic.LANES * 0.82f / KEY_W;
		hitY = boardBottom - 4f - KEY_H * keyScale;
		vpY = (topY - hitY * TOP_SCALE) / (1f - TOP_SCALE);
	}

	private void buildTracks() {
		tracks.clear();
		tracks.add(new Track("", tr("gui.dragonminez.minigame.rhythm.free")));
		Minecraft mc = Minecraft.getInstance();
		Map<String, Track> discs = new LinkedHashMap<>();
		boolean requireDisc = ConfigManager.getTrainingConfig().getRhythm().isRequireDiscInInventory();
		if (requireDisc && mc.player != null) {
			for (ItemStack stack : mc.player.getInventory().items) addDisc(discs, stack.getItem());
			for (ItemStack stack : mc.player.getInventory().offhand) addDisc(discs, stack.getItem());
		} else if (!requireDisc) {
			for (Item item : ForgeRegistries.ITEMS) addDisc(discs, item);
		}
		tracks.addAll(discs.values());
		trackIndex = rememberedTrack < tracks.size() ? rememberedTrack : -1;
		if (isChallenge()) trackIndex = 0;
		dropdownOpen = false;
		dropdownScroll = 0;
	}

	private static void addDisc(Map<String, Track> discs, Item item) {
		if (!(item instanceof RecordItem record)) return;
		ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
		if (id == null || discs.containsKey(id.toString())) return;
		discs.put(id.toString(), new Track(id.toString(), record.getDisplayName()));
	}

	@Override
	protected String startOption() {
		activeTrack = tracks.isEmpty() ? null : tracks.get(Math.max(0, trackIndex));
		return activeTrack == null ? "" : activeTrack.itemId();
	}

	@Override
	protected byte[] startPayload() {
		if (activeTrack == null || activeTrack.itemId().isEmpty()) return null;
		RhythmChart cached = RhythmClientCharts.cached(activeTrack.itemId());
		return cached == null ? null : cached.encode();
	}

	@Override
	protected boolean handleStartRejection(MinigameStartS2C message) {
		if (!"gui.dragonminez.minigame.reject.no_chart".equals(message.getRejectKey())) return false;
		if (activeTrack == null || activeTrack.itemId().isEmpty() || RhythmClientCharts.cached(activeTrack.itemId()) != null) return false;
		ResourceLocation id = ResourceLocation.tryParse(activeTrack.itemId());
		Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
		if (!(item instanceof RecordItem record)) return false;
		String itemId = activeTrack.itemId();
		RhythmClientCharts.analyze(itemId, record).thenAccept(chart -> Minecraft.getInstance().execute(() -> {
			if (Minecraft.getInstance().screen != this || !isPreparing()) return;
			if (chart == null) rejectWith("gui.dragonminez.minigame.reject.analysis_failed");
			else retryStart();
		}));
		return true;
	}

	@Override
	protected String preparingMessageKey() {
		return "gui.dragonminez.minigame.rhythm.analyzing";
	}

	@Override
	protected boolean onReadyKey(int keyCode) {
		if (isChallenge()) return false;
		int dir = 0;
		if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A || keyCode == GLFW.GLFW_KEY_UP) dir = -1;
		if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D || keyCode == GLFW.GLFW_KEY_DOWN) dir = 1;
		if (dir == 0) return false;
		if (tracks.size() <= 1) {
			trackIndex = 0;
			playMiss();
			return true;
		}
		trackIndex = trackIndex < 0 ? (dir > 0 ? 0 : tracks.size() - 1) : Math.floorMod(trackIndex + dir, tracks.size());
		rememberedTrack = trackIndex;
		ensureVisible();
		playHit(false);
		return true;
	}

	@Override
	protected boolean onReadyClick(double mouseX, double mouseY) {
		if (isChallenge()) return false;
		float x = readyToUiX(mouseX), y = readyToUiY(mouseY);
		if (insideRect(dropdownButton, x, y)) {
			dropdownOpen = !dropdownOpen;
			ensureVisible();
			playHit(false);
			return true;
		}
		if (dropdownOpen) {
			int row = rowAt(x, y);
			if (row >= 0) {
				trackIndex = row;
				rememberedTrack = row;
				playHit(true);
			}
			dropdownOpen = false;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (isReady() && dropdownOpen) {
			int max = Math.max(0, tracks.size() - DROPDOWN_ROWS);
			dropdownScroll = Math.max(0, Math.min(max, dropdownScroll - (int) Math.signum(delta)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	private void ensureVisible() {
		if (trackIndex < 0) return;
		if (trackIndex < dropdownScroll) dropdownScroll = trackIndex;
		if (trackIndex >= dropdownScroll + DROPDOWN_ROWS) dropdownScroll = trackIndex - DROPDOWN_ROWS + 1;
	}

	private int rowAt(float x, float y) {
		if (dropdownButton == null) return -1;
		float left = dropdownButton[0], width = dropdownButton[2];
		float top = dropdownButton[1] + dropdownButton[3] + 1;
		if (x < left || x > left + width || y < top) return -1;
		int row = (int) ((y - top) / DROPDOWN_ROW_H);
		if (row >= Math.min(DROPDOWN_ROWS, tracks.size())) return -1;
		int index = row + dropdownScroll;
		return index < tracks.size() ? index : -1;
	}

	private static boolean insideRect(float[] rect, double x, double y) {
		return rect != null && x >= rect[0] && x <= rect[0] + rect[2] && y >= rect[1] && y <= rect[1] + rect[3];
	}

	@Override
	protected int readyExtraHeight() {
		if (isChallenge()) return 0;
		return tracks.size() <= 1 ? 42 : 31;
	}

	@Override
	protected void renderReadyExtra(GuiGraphics graphics, int x, int y) {
		float width = 200, height = 16;
		dropdownButton = new float[]{x - width / 2f, y, width, height};
		float mx = readyToUiX(guiMouseX()), my = readyToUiY(guiMouseY());
		boolean hover = insideRect(dropdownButton, mx, my);
		drawDropdownBox(graphics, dropdownButton[0], dropdownButton[1], width, height, hover ? 0xF0183824 : 0xF0102818);
		Component label = trackIndex < 0 ? tr("gui.dragonminez.minigame.rhythm.select_song") : dmz("♪ ").append(tracks.get(trackIndex).name());
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, fit(label, (int) width - 26), x - 6, (int) y + 4, trackIndex < 0 ? 0xFFFFFFFF : 0xFFFFD700);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmz(dropdownOpen ? "▲" : "▼"), (int) (x + width / 2f - 9), (int) y + 4, 0xFFD2D7F1);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.rhythm.select"), x, (int) y + 20, 0xFF888888);
		if (tracks.size() <= 1) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.rhythm.no_discs"), x, (int) y + 31, 0xFFFFAA55);
		}
		if (dropdownOpen) drawDropdownList(graphics, mx, my);
	}

	private void drawDropdownList(GuiGraphics graphics, float mx, float my) {
		float left = dropdownButton[0], width = dropdownButton[2];
		float top = dropdownButton[1] + dropdownButton[3] + 1;
		int rows = Math.min(DROPDOWN_ROWS, tracks.size());
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 200);
		drawDropdownBox(graphics, left, top, width, rows * DROPDOWN_ROW_H + 2, 0xF8081410);
		int hoverRow = rowAt(mx, my);
		for (int i = 0; i < rows; i++) {
			int index = i + dropdownScroll;
			float rowY = top + 1 + i * DROPDOWN_ROW_H;
			if (index == hoverRow) {
				rectF(graphics, left + 2, rowY, width - 4, DROPDOWN_ROW_H, 0x40FFFFFF);
				graphics.flush();
			}
			Track track = tracks.get(index);
			Component name = index == 0 ? track.name() : dmz("♪ ").append(track.name());
			int color = index == trackIndex ? 0xFFFFD700 : 0xFFE0E4F4;
			TextUtil.drawStringWithBorder(graphics, this.font, fit(name, (int) width - 16), (int) left + 6, (int) rowY + 3, color);
		}
		if (tracks.size() > DROPDOWN_ROWS) {
			float trackH = rows * DROPDOWN_ROW_H;
			float thumbH = Math.max(8, trackH * DROPDOWN_ROWS / tracks.size());
			float thumbY = top + 1 + (trackH - thumbH) * dropdownScroll / Math.max(1, tracks.size() - DROPDOWN_ROWS);
			rectF(graphics, left + width - 4, thumbY, 2, thumbH, 0xFFD2D7F1);
			graphics.flush();
		}
		graphics.pose().popPose();
	}

	private void drawDropdownBox(GuiGraphics graphics, float x, float y, float w, float h, int fill) {
		rectF(graphics, x, y, w, h, fill);
		outlineF(graphics, x - 1, y - 1, w + 2, h + 2, 1f, 0xFF0C0C12);
		outlineF(graphics, x, y, w, h, 1f, 0xFF909AC3);
		rectF(graphics, x, y, w, 1, 0xFFD2D7F1);
		rectF(graphics, x, y, 1, h, 0xFFD2D7F1);
		graphics.flush();
	}

	private Component fit(Component text, int maxWidth) {
		if (this.font.width(text) <= maxWidth) return text;
		String plain = text.getString();
		while (plain.length() > 1 && this.font.width(dmz(plain + "...")) > maxWidth) plain = plain.substring(0, plain.length() - 1);
		return dmz(plain + "...");
	}

	@Override
	protected MinigameLogic createLogic(MinigameStartS2C message) {
		RhythmChart chart = message.getPayload().length > 0 ? RhythmChart.decode(message.getPayload()) : null;
		if (chart == null || message.getOption().isEmpty()) activeTrack = tracks.isEmpty() ? null : tracks.get(0);
		return MinigameLogics.create(minigameId, ConfigManager.getTrainingConfig(), message.getSeed(), chart);
	}

	@Override
	protected void onLogicStarted() {
		musicStarted = false;
		sparks.clear();
		for (int i = 0; i < seenLaneJudge.length; i++) seenLaneJudge[i] = -100;
		Minecraft.getInstance().getMusicManager().stopPlaying();
	}

	private static int laneKeyCode(int lane) {
		InputConstants.Key key = LANE_BINDS[lane].getKey();
		if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() != InputConstants.UNKNOWN.getValue()) return key.getValue();
		return LANE_DEFAULTS[lane];
	}

	private static String laneLabel(int lane) {
		int code = laneKeyCode(lane);
		switch (code) {
			case GLFW.GLFW_KEY_LEFT: return "←";
			case GLFW.GLFW_KEY_DOWN: return "↓";
			case GLFW.GLFW_KEY_UP: return "↑";
			case GLFW.GLFW_KEY_RIGHT: return "→";
			case GLFW.GLFW_KEY_SPACE: return "SPC";
			default: break;
		}
		if ((code >= GLFW.GLFW_KEY_A && code <= GLFW.GLFW_KEY_Z) || (code >= GLFW.GLFW_KEY_0 && code <= GLFW.GLFW_KEY_9)) return String.valueOf((char) code);
		String name = InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
		return name.length() > 3 ? name.substring(0, 3) : name;
	}

	@Override
	protected int mapPhysicalKey(int physical) {
		for (int lane = 0; lane < RhythmLogic.LANES; lane++) if (physical == laneKeyCode(lane)) return lane;
		return -1;
	}

	@Override
	protected int remapLogicalKey(int logical) {
		return logical == MinigameEvent.ACTION ? -1 : logical;
	}

	@Override
	public void tick() {
		super.tick();
		if (logic == null) return;
		RhythmLogic r = rhythm();
		if (isPlaying() && r.chart() != null && !musicStarted) {
			int offset = audioOffsetMs();
			if (r.songTimeMs() >= -offset - RhythmLogic.MS_PER_TICK / 2.0) startMusic(r.chart());
		}
		if (!isRunActive()) stopMusic();
	}

	private static int audioOffsetMs() {
		Integer offset = ConfigManager.getUserConfig().getRhythmAudioOffsetMs();
		return offset == null ? 0 : Math.max(-500, Math.min(500, offset));
	}

	private void startMusic(RhythmChart chart) {
		musicStarted = true;
		ResourceLocation soundId = ResourceLocation.tryParse(chart.sound);
		if (soundId == null) return;
		music = new SimpleSoundInstance(soundId, SoundSource.RECORDS, 1.0f, 1.0f, SoundInstance.createUnseededRandom(),
				false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
		Minecraft.getInstance().getSoundManager().play(music);
	}

	private void stopMusic() {
		if (music == null) return;
		Minecraft.getInstance().getSoundManager().stop(music);
		music = null;
	}

	@Override
	public void removed() {
		stopMusic();
		super.removed();
	}

	@Override
	protected void onLogicTick() {
		checkJudgements();
	}

	@Override
	protected void onInputApplied(MinigameEvent event) {
		checkJudgements();
	}

	private void checkJudgements() {
		RhythmLogic r = rhythm();
		long now = System.nanoTime();
		for (int lane = 0; lane < RhythmLogic.LANES; lane++) {
			int tick = r.laneJudgeTick(lane);
			if (tick == seenLaneJudge[lane]) continue;
			seenLaneJudge[lane] = tick;
			int judge = r.laneJudge(lane);
			if (judge == RhythmLogic.JUDGE_MISS) {
				playMiss();
				keyMissNanos[lane] = now;
				triggerBoard(true, 1f);
				continue;
			}
			boolean perfect = judge == RhythmLogic.JUDGE_PERFECT;
			playHit(perfect);
			keyHitNanos[lane] = now;
			spawnSparks(lane, perfect ? 14 : 8);
			triggerBoard(false, perfect ? 1f : 0.65f);
			if (perfect) {
				glowColor = LANE_COLORS[lane];
				glowTick = r.tickCount();
			}
		}
	}

	private void triggerBoard(boolean miss, float strength) {
		if (!miss && boardMiss && secondsSince(boardEventNanos) < 0.25f) return;
		boardEventNanos = System.nanoTime();
		boardMiss = miss;
		boardStrength = strength;
	}

	private static float secondsSince(long nanos) {
		return nanos == 0 ? 99f : (System.nanoTime() - nanos) / 1_000_000_000f;
	}

	private void spawnSparks(int lane, int count) {
		float x = laneX(lane, 1f);
		for (int i = 0; i < count; i++) {
			double angle = -Math.PI / 2 + (sparkRandom.nextDouble() - 0.5) * Math.PI * 0.9;
			float speed = 60f + sparkRandom.nextFloat() * 110f;
			sparks.add(new float[]{x, hitY, (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed, 0.35f + sparkRandom.nextFloat() * 0.25f, LANE_COLORS[lane]});
		}
	}

	private float laneX(float laneCenter, float scale) {
		float laneWidth = bottomWidth / RhythmLogic.LANES;
		return cx + (laneCenter + 0.5f - RhythmLogic.LANES / 2f) * laneWidth * scale;
	}

	private float edgeX(float boundary, float scale) {
		float laneWidth = bottomWidth / RhythmLogic.LANES;
		return cx + (boundary - RhythmLogic.LANES / 2f) * laneWidth * scale;
	}

	private float yFor(float scale) {
		return vpY + (hitY - vpY) * scale;
	}

	private static float scaleFor(double remainingMs, double travelMs) {
		double z = remainingMs / Math.max(1.0, travelMs);
		return (float) (1.0 / (1.0 + z * (1.0 / TOP_SCALE - 1.0)));
	}

	private void trapezoid(GuiGraphics graphics, float boundaryL, float boundaryR, float sTop, float sBottom, int topColor, int bottomColor) {
		float yTop = yFor(sTop);
		float yBottom = yFor(sBottom);
		quadStrip(graphics, edgeX(boundaryL, sTop), yTop, edgeX(boundaryR, sTop), yTop, edgeX(boundaryR, sBottom), yBottom, edgeX(boundaryL, sBottom), yBottom, topColor, bottomColor);
	}

	private void quadStrip(GuiGraphics graphics, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int topColor, int bottomColor) {
		trapezoidQuad(graphics, x0, y0, x1, y1, x2, y2, x3, y3, topColor, bottomColor);
	}

	@Override
	protected void renderGame(GuiGraphics graphics, float partialTick) {
		RhythmLogic r = rhythm();
		TrainingConfig.RhythmConfig cfg = r.cfg();
		double now = r.songTimeMs() + (isPlaying() ? partialTick * RhythmLogic.MS_PER_TICK : 0);

		int glowAge = r.tickCount() - glowTick;
		if (glowAge >= 0 && glowAge < 12) {
			float alpha = 0.35f * (1f - glowAge / 12f);
			for (int i = 0; i < 40; i++) {
				float a = alpha * (1f - i / 40f);
				rectF(graphics, i, 0, 1, this.height, withAlpha(glowColor, a));
				rectF(graphics, this.width - 1 - i, 0, 1, this.height, withAlpha(glowColor, a));
			}
			graphics.flush();
		}

		float t = secondsSince(boardEventNanos);
		float decay = (float) Math.exp(-t * (boardMiss ? 8.0 : 10.0));
		float dx = boardMiss ? 5f * decay * (float) Math.sin(t * 62.0) : 0f;
		float dy = boardMiss ? 2f * decay * (float) Math.sin(t * 47.0) : 3f * boardStrength * decay * (float) Math.sin(t * 38.0 + 0.4);
		float scale = 1f + (boardMiss ? 0.02f : 0.012f * boardStrength) * decay;
		graphics.pose().pushPose();
		graphics.pose().translate(cx + dx, hitY + dy, 0);
		graphics.pose().scale(scale, scale, 1f);
		graphics.pose().translate(-cx, -hitY, 0);

		trapezoid(graphics, 0, RhythmLogic.LANES, TOP_SCALE, 1.08f, 0xD00A1220, 0xE0162234);
		if (boardMiss && decay > 0.02f) {
			trapezoid(graphics, 0, RhythmLogic.LANES, TOP_SCALE, 1.08f, withAlpha(0xFF3030, 0.05f * decay), withAlpha(0xFF3030, 0.32f * decay));
		}
		for (int b = 1; b < RhythmLogic.LANES; b++) {
			quadStrip(graphics, edgeX(b, TOP_SCALE) - 0.3f, yFor(TOP_SCALE), edgeX(b, TOP_SCALE) + 0.3f, yFor(TOP_SCALE),
					edgeX(b, 1.08f) + 0.6f, yFor(1.08f), edgeX(b, 1.08f) - 0.6f, yFor(1.08f), 0x30FFFFFF, 0x50FFFFFF);
		}
		int rail = boardMiss && decay > 0.1f ? 0xFFFF8080 : 0xF0D8F4FF;
		for (int side = 0; side <= RhythmLogic.LANES; side += RhythmLogic.LANES) {
			quadStrip(graphics, edgeX(side, TOP_SCALE) - 1f, yFor(TOP_SCALE), edgeX(side, TOP_SCALE) + 1f, yFor(TOP_SCALE),
					edgeX(side, 1.08f) + 2f, yFor(1.08f), edgeX(side, 1.08f) - 2f, yFor(1.08f), 0x907CE8FF, rail);
		}

		double beatMs = 60000.0 / (r.chart() != null && r.chart().bpm > 0 ? r.chart().bpm : cfg.getFreeBaseBpm());
		int travel = r.travelMs();
		double firstBeat = Math.ceil(now / beatMs) * beatMs;
		for (double beat = firstBeat; beat < now + travel; beat += beatMs) {
			float s = scaleFor(beat - now, travel);
			rectF(graphics, edgeX(0, s), yFor(s), edgeX(RhythmLogic.LANES, s) - edgeX(0, s), Math.max(0.5f, s), 0x18FFFFFF);
		}

		for (int lane = 0; lane < RhythmLogic.LANES; lane++) {
			int age = r.tickCount() - r.laneJudgeTick(lane);
			if (age < 0 || age >= EFFECT_TICKS || r.laneJudge(lane) == RhythmLogic.JUDGE_MISS) continue;
			float fade = 1f - (age + (isPlaying() ? partialTick : 0)) / EFFECT_TICKS;
			trapezoid(graphics, lane + 0.1f, lane + 0.9f, 0.62f, 1f, withAlpha(LANE_COLORS[lane], 0f), withAlpha(LANE_COLORS[lane], 0.55f * fade));
		}

		for (RhythmLogic.Note note : r.notes()) {
			if (note.lengthMs <= 0 || note.state == RhythmLogic.NoteState.DONE) continue;
			double headRemaining = note.state == RhythmLogic.NoteState.HOLDING ? 0 : note.timeMs - now;
			double tailRemaining = note.timeMs + note.lengthMs - now;
			if (tailRemaining < 0) continue;
			float sHead = scaleFor(Math.max(headRemaining, -0.1 * note.travelMs), note.travelMs);
			float sTail = scaleFor(Math.min(tailRemaining, note.travelMs), note.travelMs);
			int base = LANE_COLORS[note.lane];
			float alpha = note.state == RhythmLogic.NoteState.HOLDING ? 0.85f : 0.55f;
			trapezoid(graphics, note.lane + 0.36f, note.lane + 0.64f, sTail, sHead, withAlpha(base, alpha * 0.7f), withAlpha(base, alpha));
		}
		graphics.flush();

		for (RhythmLogic.Note note : r.notes()) {
			if (note.state != RhythmLogic.NoteState.PENDING) continue;
			double remaining = note.timeMs - now;
			if (remaining > note.travelMs || remaining < -0.12 * note.travelMs) continue;
			drawGem(graphics, note.lane, scaleFor(remaining, note.travelMs));
		}

		rectGradient(graphics, edgeX(0, 1f), hitY - 2f, edgeX(RhythmLogic.LANES, 1f) - edgeX(0, 1f), 4f, 0xC0FFFFFF, 0x60A0D8FF);
		graphics.flush();
		for (int lane = 0; lane < RhythmLogic.LANES; lane++) drawKeyCap(graphics, lane, r);

		updateAndDrawSparks(graphics);
		graphics.flush();

		drawCenterTexts(graphics, r);
		drawHealth(graphics, r);
		if (now < 0) drawCountdown(graphics, now);
		graphics.pose().popPose();

		drawTopBar(graphics, r, now);
	}

	private void drawGem(GuiGraphics graphics, int lane, float s) {
		float laneWidth = bottomWidth / RhythmLogic.LANES;
		float k = laneWidth * 0.8f * s / NOTE_W;
		graphics.pose().pushPose();
		graphics.pose().translate(laneX(lane, s) - NOTE_W * k / 2f, yFor(s) - NOTE_H * k / 2f, 0);
		graphics.pose().scale(k, k, 1f);
		HudRender.blit(graphics, SHEET, 0, 0, NOTES_U, lane * NOTE_H, NOTE_W, NOTE_H, SHEET_SIZE, SHEET_SIZE);
		graphics.pose().popPose();
	}

	private void drawKeyCap(GuiGraphics graphics, int lane, RhythmLogic r) {
		float laneWidth = bottomWidth / RhythmLogic.LANES;
		float k = laneWidth * 0.82f / KEY_W;
		boolean held = r.isInputHeld(lane);
		float sinceHit = secondsSince(keyHitNanos[lane]);
		float sinceMiss = secondsSince(keyMissNanos[lane]);
		int state = sinceHit < 0.12f ? 2 : held ? 1 : 0;
		float pop = sinceHit < 0.5f ? 1f + 0.16f * (float) Math.exp(-sinceHit * 14.0) : 1f;
		float shake = sinceMiss < 0.4f ? 2f * (float) Math.exp(-sinceMiss * 12.0) * (float) Math.sin(sinceMiss * 70.0) : 0f;
		float centerX = laneX(lane, 1f) + shake;
		float centerY = hitY + 4 + KEY_H * k / 2f;

		if (sinceHit < 0.3f) {
			float p = sinceHit / 0.3f;
			float grow = 1f + p * 0.8f;
			float w = KEY_W * k * grow, h = KEY_H * k * grow;
			outlineF(graphics, centerX - w / 2f, centerY - h / 2f, w, h, 1f, withAlpha(LANE_COLORS[lane], 0.85f * (1f - p)));
			graphics.flush();
		}

		graphics.pose().pushPose();
		graphics.pose().translate(centerX, centerY, 0);
		graphics.pose().scale(k * pop, k * pop, 1f);
		graphics.pose().translate(-KEY_W / 2f, -KEY_H / 2f, 0);
		if (sinceMiss < 0.35f) {
			float red = sinceMiss / 0.35f;
			RenderSystem.setShaderColor(1f, 0.45f + 0.55f * red, 0.45f + 0.55f * red, 1f);
		}
		HudRender.blit(graphics, SHEET, 0, 0, state * KEY_W, lane * KEY_H, KEY_W, KEY_H, SHEET_SIZE, SHEET_SIZE);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		graphics.pose().popPose();

		float pressOffset = state == 0 ? 0 : 3;
		int letterColor = state == 2 ? 0xFF1A2230 : 0xFFFFFFFF;
		float textScale = Math.max(1f, k * 0.75f);
		graphics.pose().pushPose();
		graphics.pose().translate(centerX, centerY + (pressOffset - 3.5f) * k * pop, 0);
		graphics.pose().scale(textScale, textScale, 1f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmz(laneLabel(lane)), 0, -4, letterColor);
		graphics.pose().popPose();
	}

	private void updateAndDrawSparks(GuiGraphics graphics) {
		long nowNanos = System.nanoTime();
		float dt = lastFrameNanos == 0 ? 0f : Math.min(0.05f, (nowNanos - lastFrameNanos) / 1_000_000_000f);
		lastFrameNanos = nowNanos;
		Iterator<float[]> it = sparks.iterator();
		while (it.hasNext()) {
			float[] p = it.next();
			p[0] += p[2] * dt;
			p[1] += p[3] * dt;
			p[3] += 260f * dt;
			p[4] -= dt;
			if (p[4] <= 0) {
				it.remove();
				continue;
			}
			float alpha = Math.min(1f, p[4] * 3f);
			rectF(graphics, p[0] - 1, p[1] - 1, 2, 2, withAlpha((int) p[5], alpha));
		}
	}

	private void drawTopBar(GuiGraphics graphics, RhythmLogic r, double now) {
		Component title = activeTrack != null && r.chart() != null ? activeTrack.name() : tr("gui.dragonminez.minigame.rhythm.free");
		int barTop = (int) topY - 22;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmz("♪ ").append(title), (int) cx, barTop, 0xFFFFFFFF);
		float barW = 220f;
		float x0 = cx - barW / 2;
		float songPct = r.chart() != null && r.chart().durationMs > 0 ? (float) Math.max(0, Math.min(1, now / r.chart().durationMs)) : 0f;
		if (r.chart() != null) {
			drawBarH(graphics, SHEET, x0, barTop + 10, barW, songPct, FILL_CYAN);
			float legendWidth = this.font.width(tr("gui.dragonminez.minigame.legend.song")) * 0.75f;
			drawLegend(graphics, "gui.dragonminez.minigame.legend.song", x0 - 6 - legendWidth / 2f, barTop + 12);
		}
		float acc = accuracyShown.update((float) r.accuracy() * 100f, 6f);
		TextUtil.drawStringWithBorder(graphics, this.font, dmz(String.format("%.1f%%", acc)), (int) (x0 + barW + 6), barTop + 11, 0xFFB8E8FF);
	}

	private void drawCenterTexts(GuiGraphics graphics, RhythmLogic r) {
		int age = r.tickCount() - r.lastJudgementTick();
		float midY = yFor(0.8f);
		if (age >= 0 && age < 14 && r.lastJudgement() >= 0) {
			int j = Math.max(0, Math.min(2, r.lastJudgement()));
			float alpha = Math.max(0.1f, 1f - age / 14f);
			float judgeScale = 1f + 0.45f * (float) Math.exp(-age * 0.9f);
			graphics.pose().pushPose();
			graphics.pose().translate(cx, midY - 12, 0);
			graphics.pose().scale(judgeScale, judgeScale, 1f);
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(JUDGEMENTS[j]).withStyle(ChatFormatting.BOLD), 0, -4,
					withAlpha(JUDGEMENT_COLORS[j], alpha));
			graphics.pose().popPose();
		}
		if (r.combo() != shownCombo) {
			if (r.combo() > shownCombo) comboPop.snap(1f);
			shownCombo = r.combo();
		}
		float pop = comboPop.update(0f, 9f);
		if (r.combo() >= 2) {
			float comboScale = 1.8f * (1f + 0.35f * pop);
			graphics.pose().pushPose();
			graphics.pose().translate(cx, midY + 2, 0);
			graphics.pose().scale(comboScale, comboScale, 1f);
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmz("x" + r.combo()), 0, -4, r.combo() >= 20 ? 0xFFFFD700 : 0xFFFFFFFF);
			graphics.pose().popPose();
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.rhythm.combo"), (int) cx, (int) midY + 16, 0xFFAAAAAA);
		}
	}

	private void drawHealth(GuiGraphics graphics, RhythmLogic r) {
		float h = hitY - yFor(0.6f);
		float y = hitY - h;
		float health = healthShown.update((float) Math.max(0, Math.min(1, r.health())), 8f);
		int healthFill = health > 0.5f ? FILL_GREEN : health > 0.25f ? FILL_GOLD : FILL_RED;
		drawBarV(graphics, SHEET, edgeX(RhythmLogic.LANES, 1f) + 10, y, h, health, healthFill);
		drawLegend(graphics, "gui.dragonminez.minigame.legend.health", edgeX(RhythmLogic.LANES, 1f) + 14.5f, y - 9);
		float level = levelShown.update((float) Math.max(0, Math.min(1, r.progress() / r.cfg().getProgressMax())), 7f);
		if (r.levelsCleared() != shownLevel) {
			shownLevel = r.levelsCleared();
			levelShown.snap(0f);
			level = 0f;
		}
		drawBarV(graphics, SHEET, edgeX(0, 1f) - 19, y, h, level, FILL_GOLD);
		drawLegend(graphics, "gui.dragonminez.minigame.legend.level", edgeX(0, 1f) - 14.5f, y - 9);
	}

	private void drawCountdown(GuiGraphics graphics, double now) {
		int seconds = (int) Math.ceil(-now / 1000.0);
		float midY = yFor(0.7f);
		ring(graphics, cx, midY, 26, 2, 0xC07CE8FF);
		double frac = (-now / 1000.0) % 1.0;
		ring(graphics, cx, midY, 22, 1.5f, withAlpha(0xFFFFFF, (float) Math.max(0.2, frac)));
		graphics.flush();
		graphics.pose().pushPose();
		graphics.pose().translate(cx, midY, 0);
		graphics.pose().scale(2.5f, 2.5f, 1f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmz(String.valueOf(Math.max(1, seconds))), 0, -4, 0xFFFFFFFF);
		graphics.pose().popPose();
	}

	@Override
	protected void appendResultLines(List<Component> lines, List<Integer> colors) {
		if (logic == null) return;
		RhythmLogic r = rhythm();
		int hits = r.perfects() + r.goods();
		double acc = r.accuracy();
		int stars = acc >= 0.95 ? 3 : acc >= 0.85 ? 2 : acc >= 0.7 ? 1 : 0;
		lines.add(dmz("★".repeat(stars) + "☆".repeat(3 - stars)));
		colors.add(0xFFFFD700);
		lines.add(tr("gui.dragonminez.minigame.rhythm.stats", hits, r.judgedNotes(), r.misses(), String.format("%.1f%%", acc * 100), r.maxCombo()));
		colors.add(0xFFB8E8FF);
	}

	private static int brighten(int rgb, float amount) {
		int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
		r = (int) (r + (255 - r) * amount);
		g = (int) (g + (255 - g) * amount);
		b = (int) (b + (255 - b) * amount);
		return (r << 16) | (g << 8) | b;
	}

	private static int darken(int rgb, float amount) {
		int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
		r = (int) (r * (1 - amount));
		g = (int) (g * (1 - amount));
		b = (int) (b * (1 - amount));
		return (r << 16) | (g << 8) | b;
	}
}
