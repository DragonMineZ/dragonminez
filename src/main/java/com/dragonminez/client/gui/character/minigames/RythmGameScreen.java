package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.gui.tutorial.TutorialManager;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.client.util.ScrollbarState;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TrainingConfig;
import com.dragonminez.common.network.S2C.MinigameStartS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.training.BuiltinSongs;
import com.dragonminez.common.training.MinigameEvent;
import com.dragonminez.common.training.MinigameLogic;
import com.dragonminez.common.training.MinigameLogics;
import com.dragonminez.common.training.MinigameOrigin;
import com.dragonminez.common.training.RhythmAutoCharter;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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

	private static int rememberedTrack = 0;
	private static int rememberedDifficulty = RhythmLogic.DIFFICULTY_NORMAL;
	private static int rememberedVariant = RhythmChart.VARIANT_INSTRUMENTAL;
	private static final int[] DIFFICULTY_COLORS = {0x7CFD8A, 0xD2D7F1, 0xFFB347, 0xFF5E5E};
	private static final int LIST_ROW_H = 12, LIST_ROWS = 12, LIST_W = 172, DETAIL_W = 192, PANEL_GAP = 10;
	private static final int PANEL_W = LIST_W + PANEL_GAP + DETAIL_W;
	private static final int PANEL_H = 12 + LIST_ROWS * LIST_ROW_H + 2 + 4;
	private static final int DIFFICULTY_W = 45, DIFFICULTY_H = 14, DIFFICULTY_GAP = 2;
	private static final int VARIANT_W = 92, VARIANT_H = 14;
	private static final String[] TITLE_PREFIXES = {"dragon ball z kai", "dragon ball super", "dragon ball gt", "dragon ball z", "dragon ball daima", "dragon ball",
			"dbz kai", "dbgt", "dbz", "dbs", "db", "saint seiya", "one piece", "naruto shippuden", "naruto", "bleach", "jojo's bizarre adventure", "jojo",
			"ost", "opening", "ending", "op", "ed", "theme", "the"};

	private enum Kind { FREE, BUILTIN, DISC }

	private record Track(String key, Component name, Component subtitle, String sortKey, int durationMs, Kind kind, boolean learned) {}

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
	private final ScrollbarState listScroll = new ScrollbarState().noTrack().colors(0, 0xFFD2D7F1, 0xFFFFFFFF).barWidth(2).minThumb(8).step(LIST_ROW_H);
	private float[] listBox;
	private float[] playRect;
	private float[] calibrateRect;
	private final float[][] difficultyButtons = new float[RhythmLogic.DIFFICULTY_COUNT][];
	private final float[][] variantButtons = new float[2][];
	private int difficulty = rememberedDifficulty;
	private int variant = rememberedVariant;
	private String selectedItem = "";
	private RhythmChart selectedChart;
	private int recommended = -1;
	private String analyzingItem;
	private volatile int analysisPhase;
	private boolean analysisFailed;
	private long analysisStartNanos;
	private long analysisDoneNanos;
	private final SmoothValue analysisShown = new SmoothValue();
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
	private int restShownSeconds = -1;
	private long restPopNanos;
	private boolean calibrationPrompted;

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
		tracks.add(new Track("", tr("gui.dragonminez.minigame.rhythm.free"), tr("gui.dragonminez.minigame.rhythm.free_subtitle"), "", 0, Kind.FREE, false));
		for (BuiltinSongs.Song song : BuiltinSongs.ALL) {
			tracks.add(new Track(song.key(), dmz(song.title()), dmz(song.artist()), "", song.durationMs(), Kind.BUILTIN, false));
		}
		Minecraft mc = Minecraft.getInstance();
		Set<String> learned = mc.player == null ? Set.of()
				: StatsProvider.get(StatsCapability.INSTANCE, mc.player).map(d -> (Set<String>) new HashSet<>(d.getCharacter().getLearnedSongs())).orElse(Set.of());
		Map<String, Track> discs = new LinkedHashMap<>();
		boolean requireDisc = ConfigManager.getTrainingConfig().getRhythm().isRequireDiscInInventory();
		if (requireDisc && mc.player != null) {
			for (ItemStack stack : mc.player.getInventory().items) addDisc(discs, stack.getItem(), learned);
			for (ItemStack stack : mc.player.getInventory().offhand) addDisc(discs, stack.getItem(), learned);
			for (String key : learned) {
				ResourceLocation id = ResourceLocation.tryParse(key);
				if (id != null) addDisc(discs, ForgeRegistries.ITEMS.getValue(id), learned);
			}
		} else if (!requireDisc) {
			for (Item item : ForgeRegistries.ITEMS) addDisc(discs, item, learned);
		}
		List<Track> sorted = new ArrayList<>(discs.values());
		sorted.sort(Comparator.comparing(Track::sortKey).thenComparing(t -> t.name().getString().toLowerCase(Locale.ROOT)));
		tracks.addAll(sorted);
		trackIndex = rememberedTrack >= 0 && rememberedTrack < tracks.size() ? rememberedTrack : 0;
		if (isChallenge()) trackIndex = 0;
		ensureVisible();
		syncSelection(true);
	}

	private void addDisc(Map<String, Track> discs, Item item, Set<String> learned) {
		if (!(item instanceof RecordItem record)) return;
		ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
		if (id == null || discs.containsKey(id.toString())) return;
		String key = id.toString();
		boolean isLearned = learned.contains(key);
		String[] info = parseDiscInfo(id.getNamespace(), record.getDisplayName().getString());
		Component name = dmz(info[0]);
		String credit = info[1] != null && info[2] != null ? info[1] + " - " + info[2] : info[1] != null ? info[1] : info[2];
		Component subtitle = credit == null ? tr("gui.dragonminez.minigame.rhythm.music_disc") : dmz(credit);
		discs.put(key, new Track(key, name, subtitle, sortKey(info[0]), record.getLengthInTicks() * 50, Kind.DISC, isLearned));
	}

	static String[] parseDiscInfo(String namespace, String displayName) {
		String text = displayName.trim();
		if (text.regionMatches(true, 0, "Music Disc - ", 0, 13)) text = text.substring(13).trim();
		String artist = null;
		int by = text.lastIndexOf(" by ");
		if (by > 0) {
			artist = text.substring(by + 4).trim();
			text = text.substring(0, by).trim();
		}
		String source = null;
		String title = text;
		int sep = text.lastIndexOf(" - ");
		if (sep > 0) {
			source = text.substring(0, sep).trim();
			title = text.substring(sep + 3).trim();
		}
		if ("minecraft".equals(namespace) && source != null && artist == null) {
			artist = source;
			source = "Minecraft";
		}
		if (title.isEmpty()) title = displayName;
		return new String[]{title, artist, source};
	}

	static String sortKey(String displayName) {
		String text = displayName.toLowerCase(Locale.ROOT).trim();
		text = text.replaceAll("\\[[^\\]]*\\]", " ").replaceAll("\\([^)]*\\)", " ").replaceAll("\\s+", " ").trim();
		String[] parts = text.split("\\s+[-\u2013\u2014|:]\\s+");
		String core = text;
		for (int i = parts.length - 1; i >= 0; i--) {
			String part = parts[i].trim();
			if (part.length() >= 3) {
				core = part;
				break;
			}
		}
		int by = core.indexOf(" by ");
		if (by > 2) core = core.substring(0, by);
		boolean changed = true;
		while (changed) {
			changed = false;
			String stripped = core.replaceFirst("^\\d+[.)\\s-]+", "").replaceAll("^[^\\p{L}\\p{N}]+", "");
			if (!stripped.equals(core)) {
				core = stripped;
				changed = true;
			}
			for (String prefix : TITLE_PREFIXES) {
				if (core.startsWith(prefix + " ") && core.length() > prefix.length() + 1) {
					core = core.substring(prefix.length() + 1).trim();
					changed = true;
				}
			}
		}
		return core.isEmpty() ? text : core;
	}

	private String trackKey(int index) {
		return index < 0 || index >= tracks.size() ? "" : tracks.get(index).key();
	}

	private Track selectedTrack() {
		return trackIndex < 0 || trackIndex >= tracks.size() ? null : tracks.get(trackIndex);
	}

	private void selectTrack(int index) {
		if (index < 0 || index >= tracks.size()) return;
		trackIndex = index;
		rememberedTrack = index;
		ensureVisible();
		syncSelection(false);
	}

	private void syncSelection(boolean keepChoices) {
		String key = trackKey(trackIndex);
		if (key.equals(selectedItem) && (selectedChart != null || key.isEmpty() || analysisFailed || key.equals(analyzingItem))) return;
		selectedItem = key;
		selectedChart = null;
		recommended = -1;
		analysisFailed = false;
		if (key.isEmpty()) return;
		RhythmChart cached = RhythmClientCharts.cached(key);
		if (cached != null) {
			onChartReady(key, cached, keepChoices);
			return;
		}
		ResourceLocation soundId = soundFor(selectedTrack());
		if (soundId == null) {
			analysisFailed = true;
			return;
		}
		analyzingItem = key;
		analysisPhase = RhythmAutoCharter.PHASE_ANALYZING;
		analysisStartNanos = System.nanoTime();
		analysisShown.snap(0f);
		RhythmClientCharts.analyze(key, soundId, phase -> analysisPhase = phase).thenAccept(chart -> Minecraft.getInstance().execute(() -> {
			if (!key.equals(analyzingItem)) return;
			analyzingItem = null;
			if (!key.equals(selectedItem)) return;
			if (chart == null) {
				analysisFailed = true;
				playMiss();
			} else {
				onChartReady(key, chart, keepChoices);
				analysisDoneNanos = System.nanoTime();
				playHit(true);
			}
		}));
	}

	private static ResourceLocation soundFor(Track track) {
		if (track == null || track.kind() == Kind.FREE) return null;
		if (track.kind() == Kind.BUILTIN) {
			BuiltinSongs.Song song = BuiltinSongs.find(track.key());
			return song == null ? null : ResourceLocation.tryParse(song.sound());
		}
		ResourceLocation id = ResourceLocation.tryParse(track.key());
		Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
		return item instanceof RecordItem record ? record.getSound().getLocation() : null;
	}

	private void onChartReady(String key, RhythmChart chart, boolean keepChoices) {
		selectedItem = key;
		selectedChart = chart;
		if (!chart.hasVocals()) variant = RhythmChart.VARIANT_INSTRUMENTAL;
		else if (!keepChoices) variant = RhythmChart.VARIANT_INSTRUMENTAL;
		refreshRecommendation(!keepChoices);
	}

	private void refreshRecommendation(boolean adopt) {
		if (selectedChart == null) return;
		recommended = RhythmLogic.recommendedDifficulty(selectedChart, ConfigManager.getTrainingConfig().getRhythm(), variant);
		if (adopt) {
			difficulty = recommended;
			rememberedDifficulty = difficulty;
		}
	}

	private boolean isAnalyzing() {
		return trackIndex > 0 && analyzingItem != null && analyzingItem.equals(selectedItem);
	}

	@Override
	protected boolean canPlay() {
		return isChallenge() || trackIndex <= 0 || selectedChart != null;
	}

	private Component difficultyName(int index) {
		return tr("gui.dragonminez.minigame.rhythm.difficulty." + RhythmLogic.clampDifficulty(index));
	}

	private Component variantName(int index) {
		return tr("gui.dragonminez.minigame.rhythm.variant." + RhythmChart.clampVariant(index));
	}

	private static String multiplierText(double value) {
		String text = String.format(Locale.ROOT, "%.2f", value);
		while (text.endsWith("0")) text = text.substring(0, text.length() - 1);
		if (text.endsWith(".")) text = text.substring(0, text.length() - 1);
		return text;
	}

	private static String durationText(int durationMs) {
		int seconds = Math.max(0, durationMs / 1000);
		return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
	}

	@Override
	protected String startOption() {
		activeTrack = selectedTrack();
		if (activeTrack == null || activeTrack.kind() == Kind.FREE) return "";
		RhythmChart cached = RhythmClientCharts.cached(activeTrack.key());
		return RhythmLogic.buildOption(activeTrack.key(), difficulty, variant, cached == null ? "" : cached.audioHash);
	}

	@Override
	protected byte[] startPayload() {
		if (activeTrack == null || activeTrack.kind() == Kind.FREE) return null;
		RhythmChart cached = RhythmClientCharts.cached(activeTrack.key());
		return cached == null ? null : cached.encode();
	}

	@Override
	protected boolean handleStartRejection(MinigameStartS2C message) {
		if (!"gui.dragonminez.minigame.reject.no_chart".equals(message.getRejectKey())) return false;
		if (activeTrack == null || activeTrack.kind() == Kind.FREE || RhythmClientCharts.cached(activeTrack.key()) != null) return false;
		ResourceLocation soundId = soundFor(activeTrack);
		if (soundId == null) return false;
		String key = activeTrack.key();
		RhythmClientCharts.analyze(key, soundId, phase -> analysisPhase = phase).thenAccept(chart -> Minecraft.getInstance().execute(() -> {
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
		switch (keyCode) {
			case GLFW.GLFW_KEY_UP -> {
				if (tracks.size() > 1) selectTrack(Math.floorMod(trackIndex - 1, tracks.size()));
				playHit(false);
				return true;
			}
			case GLFW.GLFW_KEY_DOWN -> {
				if (tracks.size() > 1) selectTrack(Math.floorMod(trackIndex + 1, tracks.size()));
				playHit(false);
				return true;
			}
			case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> {
				if (selectedChart == null || trackIndex <= 0) return true;
				setDifficulty(Math.floorMod(difficulty + (keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : -1), RhythmLogic.DIFFICULTY_COUNT));
				return true;
			}
			case GLFW.GLFW_KEY_TAB -> {
				if (selectedChart != null && selectedChart.hasVocals()) setVariant(1 - variant);
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private void setDifficulty(int value) {
		if (difficulty == value) return;
		difficulty = value;
		rememberedDifficulty = value;
		playHit(value == recommended);
	}

	private void setVariant(int value) {
		value = RhythmChart.clampVariant(value);
		if (variant == value) return;
		variant = value;
		rememberedVariant = value;
		refreshRecommendation(false);
		playHit(false);
	}

	@Override
	protected boolean onReadyClick(double mouseX, double mouseY) {
		if (isChallenge()) return false;
		float x = readyToUiX(mouseX), y = readyToUiY(mouseY);
		if (listScroll.mouseClicked(x, y, 0)) return true;
		int row = rowAt(x, y);
		if (row >= 0) {
			if (row != trackIndex) {
				selectTrack(row);
				playHit(true);
			}
			return true;
		}
		if (insideRect(calibrateRect, x, y)) {
			playHit(false);
			Minecraft.getInstance().setScreen(new RhythmCalibrationScreen(this));
			return true;
		}
		if (insideRect(playRect, x, y)) {
			if (!canPlay()) {
				playMiss();
				return true;
			}
			playUi(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.5f);
			requestStart();
			return true;
		}
		if (selectedChart != null && trackIndex > 0) {
			for (int i = 0; i < difficultyButtons.length; i++) {
				if (insideRect(difficultyButtons[i], x, y)) {
					setDifficulty(i);
					return true;
				}
			}
			if (selectedChart.hasVocals()) {
				for (int i = 0; i < variantButtons.length; i++) {
					if (insideRect(variantButtons[i], x, y)) {
						setVariant(i);
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (isReady() && !isChallenge() && insideRect(listBox, readyToUiX(mouseX), readyToUiY(mouseY))) {
			listScroll.scrollWheel(delta);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (isReady() && listScroll.mouseDragged(readyToUiX(mouseX), readyToUiY(mouseY))) return true;
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (listScroll.mouseReleased() && isReady()) return true;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private void ensureVisible() {
		if (trackIndex < 0) return;
		if (listBox == null) {
			listScroll.jumpTo(Math.max(0, trackIndex - LIST_ROWS + 1) * LIST_ROW_H);
			return;
		}
		float rowTop = listBox[1] + 1 + trackIndex * LIST_ROW_H;
		listScroll.ensureVisible(rowTop, rowTop + LIST_ROW_H);
	}

	private int rowAt(float x, float y) {
		if (listBox == null) return -1;
		if (x < listBox[0] || x > listBox[0] + listBox[2] || y < listBox[1] || y > listBox[1] + listBox[3]) return -1;
		float rel = y - listBox[1] - 1;
		if (rel >= LIST_ROWS * LIST_ROW_H) return -1;
		int index = (int) (Math.max(0, rel) + listScroll.scroll()) / LIST_ROW_H;
		return index < tracks.size() ? index : -1;
	}

	private static boolean insideRect(float[] rect, double x, double y) {
		return rect != null && x >= rect[0] && x <= rect[0] + rect[2] && y >= rect[1] && y <= rect[1] + rect[3];
	}

	@Override
	protected boolean renderReadyCustom(GuiGraphics graphics) {
		if (isChallenge()) return false;
		int centerX = this.width / 2;
		int centerY = this.height / 2;
		float scale = Math.min(1f, Math.min((this.height - 44f) / (PANEL_H + 32f), (this.width - 16f) / (PANEL_W + 32f)));
		readyScale = scale;
		float mx = readyToUiX(guiMouseX()), my = readyToUiY(guiMouseY());
		graphics.pose().pushPose();
		graphics.pose().translate(centerX, centerY, 0);
		graphics.pose().scale(scale, scale, 1f);
		graphics.pose().translate(-centerX, -centerY, 0);
		drawNpcPanel(graphics, centerX, centerY, PANEL_W, PANEL_H);
		int left = centerX - PANEL_W / 2;
		int top = centerY - PANEL_H / 2;
		drawSongList(graphics, left, top, mx, my);
		drawDetails(graphics, left + LIST_W + PANEL_GAP, top, mx, my);
		graphics.pose().popPose();
		return true;
	}

	private void drawSongList(GuiGraphics graphics, int left, int top, float mx, float my) {
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.rhythm.songs"), left + 2, top, 0xFFFFD700);
		Integer offset = ConfigManager.getUserConfig().getRhythmAudioOffsetMs();
		Component hint = tr("gui.dragonminez.minigame.rhythm.calibrate_link", offset == null ? 0 : offset);
		float hintScale = 0.7f;
		float hintW = this.font.width(hint) * hintScale;
		calibrateRect = new float[]{left + LIST_W - 2 - hintW, top, hintW, 8};
		boolean hintHover = insideRect(calibrateRect, mx, my);
		graphics.pose().pushPose();
		graphics.pose().translate(left + LIST_W - 2, top + 2, 0);
		graphics.pose().scale(hintScale, hintScale, 1f);
		TextUtil.drawStringWithBorder(graphics, this.font, hint, -this.font.width(hint), 0, hintHover ? 0xFFFFD700 : 0xFF909AC3);
		graphics.pose().popPose();
		float boxTop = top + 12;
		float boxH = LIST_ROWS * LIST_ROW_H + 2;
		listBox = new float[]{left, boxTop, LIST_W, boxH};
		drawMenuBox(graphics, left, boxTop, LIST_W, boxH, 0xF0081410);
		listScroll.layout(left, top + 13, LIST_W, LIST_ROWS * LIST_ROW_H, tracks.size() * LIST_ROW_H).barAt(left + LIST_W - 4);
		int hoverRow = rowAt(mx, my);
		listScroll.beginClip(graphics);
		for (int index = 0; index < tracks.size(); index++) {
			float rowY = boxTop + 1 + index * LIST_ROW_H;
			if (!listScroll.isVisible(rowY, LIST_ROW_H)) continue;
			Track track = tracks.get(index);
			boolean selected = index == trackIndex;
			if (selected) {
				rectF(graphics, left + 2, rowY, LIST_W - 8, LIST_ROW_H, 0x50D2D7F1);
				graphics.flush();
			} else if (index == hoverRow) {
				rectF(graphics, left + 2, rowY, LIST_W - 8, LIST_ROW_H, 0x28FFFFFF);
				graphics.flush();
			}
			String prefix = track.kind() == Kind.BUILTIN ? "\u2605 " : track.kind() == Kind.DISC ? "\u266A " : "";
			int prefixColor = track.kind() == Kind.BUILTIN ? 0xFFFFD700 : track.learned() ? 0xFF7CFD8A : 0xFFB0B8D8;
			int textX = left + 6;
			if (!prefix.isEmpty()) {
				TextUtil.drawStringWithBorder(graphics, this.font, dmz(prefix), textX, (int) rowY + 2, prefixColor);
				textX += this.font.width(dmz(prefix));
			}
			String duration = track.kind() == Kind.FREE ? "" : durationText(track.durationMs());
			int durationW = duration.isEmpty() ? 0 : this.font.width(dmz(duration)) + 4;
			int color = selected ? 0xFFFFD700 : 0xFFE0E4F4;
			TextUtil.drawStringWithBorder(graphics, this.font, fit(track.name(), LIST_W - 16 - (textX - left) - durationW), textX, (int) rowY + 2, color);
			if (!duration.isEmpty()) {
				TextUtil.drawStringWithBorder(graphics, this.font, dmz(duration), left + LIST_W - 8 - (durationW - 4), (int) rowY + 2, selected ? 0xFFD2D7F1 : 0xFF808898);
			}
		}
		listScroll.endClip(graphics);
		listScroll.renderBar(graphics, mx, my);
	}

	private void drawDetails(GuiGraphics graphics, int x, int top, float mx, float my) {
		Track track = selectedTrack();
		if (track == null) return;
		Component title = track.kind() == Kind.BUILTIN ? dmz("\u2605 ").append(track.name()) : track.name();
		TextUtil.drawStringWithBorder(graphics, this.font, fit(title, DETAIL_W), x, top, 0xFFFFD700);
		TextUtil.drawStringWithBorder(graphics, this.font, fit(track.subtitle(), DETAIL_W), x, top + 11, 0xFFB0B8D8);
		int y = top + 22;
		if (track.kind() != Kind.FREE) {
			String info = durationText(track.durationMs());
			if (selectedChart != null && selectedChart.bpm > 0) info += "  \u2022  " + Math.round(selectedChart.bpm) + " BPM";
			Component infoLine = dmz(info);
			if (track.learned()) infoLine = infoLine.copy().append(dmz("  \u2022  ")).append(tr("gui.dragonminez.minigame.rhythm.learned_tag").copy().withStyle(style -> style.withColor(0x7CFD8A)));
			TextUtil.drawStringWithBorder(graphics, this.font, infoLine, x, y, 0xFFD2D7F1);
			y += 14;
		}
		TrainingConfig.RhythmConfig cfg = ConfigManager.getTrainingConfig().getRhythm();
		if (track.kind() == Kind.FREE) {
			for (var line : this.font.split(tr("gui.dragonminez.minigame.rhythm.free_desc"), DETAIL_W)) {
				TextUtil.drawStringWithBorder(graphics, this.font, line, x, y, 0xFFB0B0B0);
				y += 10;
			}
		} else if (selectedChart != null) {
			drawChartOptions(graphics, x, y, mx, my, cfg);
		} else if (analysisFailed) {
			for (var line : this.font.split(tr("gui.dragonminez.minigame.reject.analysis_failed"), DETAIL_W)) {
				TextUtil.drawStringWithBorder(graphics, this.font, line, x, y, 0xFFFF5555);
				y += 10;
			}
		} else {
			drawAnalysisStatus(graphics, x, y);
		}

		float buttonX = x + DETAIL_W - BUTTON_W;
		float buttonY = top + PANEL_H - BUTTON_H - 4;
		playRect = new float[]{buttonX, buttonY, BUTTON_W, BUTTON_H};
		Component label = isAnalyzing() ? phaseLabel(true) : tr("gui.dragonminez.minigame.play");
		drawMenuButton(graphics, playRect, label, canPlay() ? 1f : 0.45f);
		if (track.kind() == Kind.DISC && !track.learned()) {
			float hintScale = 0.7f;
			int hintWidth = (int) ((DETAIL_W - BUTTON_W - 8) / hintScale);
			var lines = this.font.split(tr("gui.dragonminez.minigame.rhythm.learn_hint", Math.round(cfg.getLearnSongAccuracy() * 100)), hintWidth);
			graphics.pose().pushPose();
			graphics.pose().translate(x, buttonY + 3, 0);
			graphics.pose().scale(hintScale, hintScale, 1f);
			int hy = 0;
			for (int i = 0; i < Math.min(2, lines.size()); i++) {
				TextUtil.drawStringWithBorder(graphics, this.font, lines.get(i), 0, hy, 0xFF909AC3);
				hy += 10;
			}
			graphics.pose().popPose();
		}
	}

	private Component phaseLabel(boolean shortForm) {
		int phase = Math.max(0, Math.min(RhythmAutoCharter.PHASE_COUNT - 1, analysisPhase));
		float elapsed = secondsSince(analysisStartNanos);
		String dots = ".".repeat(1 + (int) (elapsed * 3) % 3);
		return tr("gui.dragonminez.minigame.rhythm." + (shortForm ? "phase_short." : "phase.") + phase).copy().append(dmz(dots));
	}

	private void drawAnalysisStatus(GuiGraphics graphics, int x, int y) {
		int phase = Math.max(0, Math.min(RhythmAutoCharter.PHASE_COUNT - 1, analysisPhase));
		float elapsed = secondsSince(analysisStartNanos);
		float pulse = 0.85f + 0.15f * (float) Math.sin(elapsed * 6.0);
		TextUtil.drawStringWithBorder(graphics, this.font, phaseLabel(false), x, y, withAlpha(0xFFD700, pulse));
		float target = Math.min(0.97f, (phase + 0.6f) / RhythmAutoCharter.PHASE_COUNT + Math.min(0.12f, elapsed * 0.004f));
		float shown = analysisShown.update(target, 2.0f);
		drawBarH(graphics, SHEET, x, y + 13, DETAIL_W - 4, shown, FILL_CYAN);
		for (var line : this.font.split(tr("gui.dragonminez.minigame.rhythm.analysis_note"), DETAIL_W)) {
			y += 10;
			TextUtil.drawStringWithBorder(graphics, this.font, line, x, y + 18, 0xFF808898);
		}
	}

	private void drawChartOptions(GuiGraphics graphics, int x, int y, float mx, float my, TrainingConfig.RhythmConfig cfg) {
		float appear = analysisDoneNanos == 0 ? 1f : Math.min(1f, secondsSince(analysisDoneNanos) / 0.35f);
		float lift = (1f - appear) * 6f;
		Component recommendedLine = tr("gui.dragonminez.minigame.rhythm.recommended",
				difficultyName(recommended).copy().withStyle(style -> style.withColor(DIFFICULTY_COLORS[Math.max(0, recommended)])));
		TextUtil.drawStringWithBorder(graphics, this.font, recommendedLine, x, (int) (y - lift), withAlpha(0xFFFFFF, appear));
		float rowY = y + 12 + lift;
		for (int i = 0; i < RhythmLogic.DIFFICULTY_COUNT; i++) {
			float bx = x + i * (DIFFICULTY_W + DIFFICULTY_GAP);
			difficultyButtons[i] = new float[]{bx, rowY, DIFFICULTY_W, DIFFICULTY_H};
			drawChoice(graphics, difficultyButtons[i], difficultyName(i), DIFFICULTY_COLORS[i], i == difficulty, insideRect(difficultyButtons[i], mx, my), appear);
			if (i == recommended) {
				graphics.pose().pushPose();
				graphics.pose().translate(bx + DIFFICULTY_W - 3, rowY - 4, 0);
				graphics.pose().scale(0.7f, 0.7f, 1f);
				TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmz("\u2605"), 0, 0, withAlpha(0xFFD700, appear));
				graphics.pose().popPose();
			}
		}
		float nextY = rowY + DIFFICULTY_H + 6;
		if (selectedChart != null && selectedChart.hasVocals()) {
			for (int i = 0; i < 2; i++) {
				float bx = x + i * (VARIANT_W + DIFFICULTY_GAP);
				variantButtons[i] = new float[]{bx, nextY, VARIANT_W, VARIANT_H};
				drawChoice(graphics, variantButtons[i], variantName(i), i == RhythmChart.VARIANT_VOCAL ? 0xF2A6FF : 0x7CE8FF, i == variant, insideRect(variantButtons[i], mx, my), appear);
			}
			nextY += VARIANT_H + 6;
		} else {
			variantButtons[0] = null;
			variantButtons[1] = null;
		}
		double mult = cfg.getDifficulty(difficulty).getRewardMultiplier();
		Component multLine = tr("gui.dragonminez.minigame.rhythm.tp_mult", multiplierText(mult));
		int multColor = mult > 1.0 ? 0x7CFD8A : mult < 1.0 ? 0xFFB347 : 0xD2D7F1;
		TextUtil.drawStringWithBorder(graphics, this.font, multLine, x, (int) nextY, withAlpha(multColor, appear));
	}

	private void drawChoice(GuiGraphics graphics, float[] rect, Component label, int tint, boolean selected, boolean hover, float appear) {
		float bx = rect[0], by = rect[1], w = rect[2], h = rect[3];
		int fill = selected ? withAlpha(darken(tint, 0.55f), 0.95f * appear) : withAlpha(0x0C1A14, (hover ? 0.95f : 0.85f) * appear);
		rectF(graphics, bx, by, w, h, fill);
		outlineF(graphics, bx - 1, by - 1, w + 2, h + 2, 1f, withAlpha(0x0C0C12, appear));
		outlineF(graphics, bx, by, w, h, 1f, withAlpha(selected ? tint : hover ? 0xD2D7F1 : 0x909AC3, appear));
		graphics.flush();
		int color = selected ? withAlpha(0xFFFFFF, appear) : withAlpha(hover ? brighten(tint, 0.3f) : tint, 0.9f * appear);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, fit(label, (int) w - 4), (int) (bx + w / 2f), (int) by + 3, color);
	}

	private void drawMenuBox(GuiGraphics graphics, float x, float y, float w, float h, int fill) {
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
		return MinigameLogics.create(minigameId, ConfigManager.getTrainingConfig(), message.getSeed(), chart, RhythmLogic.optionDifficulty(message.getOption()), RhythmLogic.optionVariant(message.getOption()));
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
		if (code >= GLFW.GLFW_KEY_0 && code <= GLFW.GLFW_KEY_9) return String.valueOf((char) code);
		String name = layoutKeyName(code);
		if (name == null) name = InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
		return name.length() > 3 ? name.substring(0, 3) : name;
	}

	@Override
	protected int mapPhysicalKey(int physical) {
		for (int lane = 0; lane < RhythmLogic.LANES; lane++) if (physical == laneKeyCode(lane)) return lane;
		return switch (physical) {
			case GLFW.GLFW_KEY_LEFT -> MinigameEvent.LEFT;
			case GLFW.GLFW_KEY_DOWN -> MinigameEvent.DOWN;
			case GLFW.GLFW_KEY_UP -> MinigameEvent.UP;
			case GLFW.GLFW_KEY_RIGHT -> MinigameEvent.RIGHT;
			default -> -1;
		};
	}

	@Override
	protected int remapLogicalKey(int logical) {
		return logical == MinigameEvent.ACTION ? -1 : logical;
	}

	@Override
	public void tick() {
		super.tick();
		if (!calibrationPrompted && isReady() && !isChallenge() && !TutorialManager.hasSeen(TutorialManager.RHYTHM_CALIBRATION)) {
			calibrationPrompted = true;
			Minecraft.getInstance().setScreen(new RhythmCalibrationScreen(this, true));
			return;
		}
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
		else if (r.isResting()) drawRestCountdown(graphics, r);
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
		boolean disabled = r.isResting();
		boolean held = !disabled && r.isInputHeld(lane);
		float sinceHit = disabled ? 99f : secondsSince(keyHitNanos[lane]);
		float sinceMiss = disabled ? 99f : secondsSince(keyMissNanos[lane]);
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
		} else if (disabled) {
			RenderSystem.setShaderColor(0.42f, 0.44f, 0.5f, 1f);
		}
		HudRender.blit(graphics, SHEET, 0, 0, state * KEY_W, lane * KEY_H, KEY_W, KEY_H, SHEET_SIZE, SHEET_SIZE);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		graphics.pose().popPose();

		float pressOffset = state == 0 ? 0 : 3;
		int letterColor = disabled ? 0xFF6A6E7A : state == 2 ? 0xFF1A2230 : 0xFFFFFFFF;
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
		Component title = tr("gui.dragonminez.minigame.rhythm.free");
		if (activeTrack != null && r.chart() != null) {
			title = activeTrack.name().copy().append(dmz("  •  ")).append(difficultyName(r.difficultyIndex()).copy().withStyle(style -> style.withColor(DIFFICULTY_COLORS[r.difficultyIndex()])));
			if (r.variant() == RhythmChart.VARIANT_VOCAL) title = title.copy().append(dmz("  •  ")).append(variantName(r.variant()).copy().withStyle(style -> style.withColor(0xF2A6FF)));
		}
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

	private void drawRestCountdown(GuiGraphics graphics, RhythmLogic r) {
		int seconds = Math.max(1, (int) Math.ceil(r.restRemainingMs() / 1000.0));
		if (seconds != restShownSeconds) {
			restShownSeconds = seconds;
			restPopNanos = System.nanoTime();
		}
		float since = secondsSince(restPopNanos);
		float pop = 1f + 0.35f * (float) Math.exp(-since * 9.0);
		float midY = yFor(0.7f);
		rectF(graphics, cx - 30, midY - 30, 60, 60, 0x00000000);
		disc(graphics, cx, midY, 24, 0x90101828);
		ring(graphics, cx, midY, 24, 2, 0xC0D2D7F1);
		float frac = (float) Math.max(0.0, Math.min(1.0, (r.restRemainingMs() % 1000) / 1000.0));
		ring(graphics, cx, midY, 27, 1, withAlpha(0xFFFFFF, 0.25f + 0.5f * frac));
		graphics.flush();
		graphics.pose().pushPose();
		graphics.pose().translate(cx, midY, 0);
		graphics.pose().scale(2.2f * pop, 2.2f * pop, 1f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmz(String.valueOf(seconds)), 0, -4, 0xFFFFFFFF);
		graphics.pose().popPose();
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.rhythm.rest"), (int) cx, (int) midY - 40, 0xFFD2D7F1);
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
		if (r.chart() != null) {
			lines.add(tr("gui.dragonminez.minigame.rhythm.difficulty_result", difficultyName(r.difficultyIndex()), multiplierText(r.songDifficulty().getRewardMultiplier())));
			colors.add(0xFF000000 | DIFFICULTY_COLORS[r.difficultyIndex()]);
		}
		if (finalResult() != null && finalResult().isSongLearned()) {
			lines.add(tr("gui.dragonminez.minigame.rhythm.song_learned"));
			colors.add(0xFF7CFD8A);
		}
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
