package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.character.MinigamesScreen;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.C2S.MinigameInputC2S;
import com.dragonminez.common.network.C2S.MinigameStartC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.MinigameResultS2C;
import com.dragonminez.common.network.S2C.MinigameStartS2C;
import com.dragonminez.common.training.MinigameEvent;
import com.dragonminez.common.training.MinigameLogic;
import com.dragonminez.common.training.MinigameLogics;
import com.dragonminez.common.training.MinigameOrigin;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class BaseMinigameScreen extends Screen {
	protected static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	private static final ResourceLocation MENU_NPC_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menunpc.png");
	private static final int PANEL_TEX_W = 346;
	private static final int PANEL_TEX_H = 94;
	private static final int FLUSH_INTERVAL_TICKS = 20;
	private static final int CONNECT_TIMEOUT_TICKS = 100;
	private static final int VERIFY_TIMEOUT_TICKS = 100;
	private static final int MOUSE_KEY_BASE = -1000;
	private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final int BUTTON_W = 74, BUTTON_H = 20;
	private static final float PAUSE_FADE = 0.3f;
	private static final float PAUSE_CLOSE = 0.25f;
	private static final float COUNTDOWN_SECONDS = 3f;
	private static final int KEEP_ALIVE_TICKS = 100;
	protected static final int INFO_LINE_Y = 53;
	protected static final int HUD_RESERVE = 66;
	private static final float RESULTS_FADE = 0.35f;
	private static final float RESULTS_LINE_DELAY = 0.06f;
	private static final float RESULTS_INPUT_DELAY = 0.45f;

	protected enum Stage { READY, CONNECTING, PREPARING, PLAYING, PAUSED, RESUMING, VERIFYING, FINISHED, REJECTED }

	protected final String minigameId;
	private final String howToKey;
	private final MinigameOrigin origin;
	private UltimateChallenge challenge;
	private int challengeStage;

	protected Stage stage = Stage.READY;
	protected MinigameLogic logic;
	private int sessionId = -1;
	private int chunkStartTick;
	private int waitTicks;
	private final List<MinigameEvent> outbox = new ArrayList<>();
	private final int[] logicalDownCount = new int[MinigameEvent.KEY_COUNT];
	private final Map<Integer, Integer> physicalDown = new HashMap<>();

	private MinigameStartS2C startInfo;
	private MinigameResultS2C progressInfo;
	private MinigameResultS2C finalInfo;
	private String rejectKey = "";
	private int lastLevelsCleared;
	private final SmoothValue hudTp = new SmoothValue();
	private long levelPopNanos;
	private long gradePopNanos;
	private String lastGrade = "";
	private long finishedAtNanos;
	private long pausedAtNanos;
	private long pauseClosingNanos;
	private long resumeAtNanos;
	private boolean pauseClosing;
	private boolean soundsPaused;
	private int keepAliveTicks;
	private float[] playButton;
	private float[] resumeButton;
	private float[] finishButton;
	private float readyScale = 1f;

	protected float fieldScale = 1f;
	protected float fieldLeft;
	protected float fieldTop;

	protected BaseMinigameScreen(String minigameId, String titleKey, MinigameOrigin origin) {
		super(Component.translatable(titleKey).withStyle(Style.EMPTY.withFont(DMZ_FONT)));
		this.minigameId = minigameId;
		this.howToKey = "gui.dragonminez.minigame." + minigameId + ".howto";
		this.origin = origin == null ? MinigameOrigin.MENU : origin;
	}

	public static BaseMinigameScreen create(String minigameId, MinigameOrigin origin) {
		return switch (minigameId) {
			case "control" -> new ControlGameScreen(origin);
			case "memory" -> new MemoryGameScreen(origin);
			case "precision" -> new PrecisionGameScreen(origin);
			case "gravity" -> new GravityGameScreen(origin);
			default -> new RythmGameScreen(origin);
		};
	}

	public void setChallenge(UltimateChallenge challenge, int stageIndex) {
		this.challenge = challenge;
		this.challengeStage = stageIndex;
	}

	private MinigameOrigin effectiveOrigin() {
		return challenge != null ? MinigameOrigin.CHALLENGE : origin;
	}

	protected void layoutField(float fieldWidth, float fieldHeight, float topReserve, float bottomReserve, float maxScale) {
		float availW = this.width - 20f;
		float availH = Math.max(40f, this.height - topReserve - bottomReserve);
		fieldScale = Math.min(maxScale, Math.min(availW / fieldWidth, availH / fieldHeight));
		fieldLeft = (this.width - fieldWidth * fieldScale) / 2f;
		fieldTop = topReserve + (availH - fieldHeight * fieldScale) / 2f;
	}

	protected boolean isPlaying() {
		return stage == Stage.PLAYING;
	}

	protected boolean isRunActive() {
		return stage == Stage.PLAYING || stage == Stage.PAUSED || stage == Stage.RESUMING;
	}

	protected float partialTick() {
		return stage == Stage.PLAYING ? Minecraft.getInstance().getFrameTime() : 0f;
	}

	public boolean acceptsStart(MinigameStartS2C message) {
		return stage == Stage.CONNECTING && minigameId.equals(message.getMinigameId());
	}

	public void onServerStart(MinigameStartS2C message) {
		if (!message.isAccepted()) {
			if (handleStartRejection(message)) {
				stage = Stage.PREPARING;
				return;
			}
			stage = Stage.REJECTED;
			rejectKey = message.getRejectKey();
			playMiss();
			return;
		}
		startInfo = message;
		sessionId = message.getSessionId();
		logic = createLogic(message);
		if (message.getTargetLevels() > 0) logic.setTargetLevelsCleared(message.getTargetLevels());
		chunkStartTick = 0;
		lastLevelsCleared = 0;
		outbox.clear();
		physicalDown.clear();
		java.util.Arrays.fill(logicalDownCount, 0);
		stage = Stage.PLAYING;
		onLogicStarted();
	}

	public void onServerResult(MinigameResultS2C message) {
		if (message.getSessionId() != sessionId) return;
		if (!message.isFinalResult()) {
			progressInfo = message;
			return;
		}
		finalInfo = message;
		enterFinished();
		if (challenge != null) {
			switch (message.getChallengeOutcome()) {
				case MinigameResultS2C.CHALLENGE_STAGE_CLEARED -> challenge.onStageCleared();
				case MinigameResultS2C.CHALLENGE_COMPLETED -> challenge.onCompleted();
				default -> playMiss();
			}
		} else if (message.getTpEarned() > 0) {
			playUi(SoundEvents.PLAYER_LEVELUP, 1.0F, 0.6f);
		}
	}

	protected void retryStart() {
		requestStart();
	}

	protected void rejectWith(String key) {
		stage = Stage.REJECTED;
		rejectKey = key;
		playMiss();
	}

	private void requestStart() {
		stage = Stage.CONNECTING;
		waitTicks = 0;
		NetworkHandler.sendToServer(new MinigameStartC2S(minigameId, effectiveOrigin(), challenge != null ? challengeStage : 0, challenge != null ? "" : startOption(), challenge != null ? null : startPayload()));
	}

	private void flush(boolean finished) {
		flush(finished, false);
	}

	private void flush(boolean finished, boolean keepAlive) {
		if (logic == null || sessionId < 0) return;
		int now = logic.tickCount();
		List<MinigameEvent> chunk = new ArrayList<>();
		Iterator<MinigameEvent> it = outbox.iterator();
		while (it.hasNext()) {
			MinigameEvent event = it.next();
			if (event.tick < now) {
				chunk.add(event);
				it.remove();
			}
		}
		if (finished) outbox.clear();
		NetworkHandler.sendToServer(new MinigameInputC2S(sessionId, chunkStartTick, now, finished, keepAlive ? 0L : logic.checksum(), chunk));
		chunkStartTick = now;
	}

	private void endByPlayer() {
		if (!isRunActive()) return;
		resumeSounds();
		flush(true);
		logic.forceFinish();
		stage = Stage.VERIFYING;
		waitTicks = 0;
	}

	private void pauseGame() {
		if (stage != Stage.PLAYING) return;
		releaseAllHeld();
		stage = Stage.PAUSED;
		pausedAtNanos = System.nanoTime();
		pauseClosing = false;
		keepAliveTicks = 0;
		Minecraft.getInstance().getSoundManager().pause();
		soundsPaused = true;
		playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 0.4f);
	}

	private void requestResume() {
		if (stage == Stage.PAUSED && !pauseClosing) {
			pauseClosing = true;
			pauseClosingNanos = System.nanoTime();
			playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.4f);
		}
	}

	private void resumeSounds() {
		if (!soundsPaused) return;
		soundsPaused = false;
		Minecraft.getInstance().getSoundManager().resume();
	}

	private void releaseAllHeld() {
		for (int key = 0; key < logicalDownCount.length; key++) {
			if (logicalDownCount[key] > 0) emit(MinigameEvent.key(logic.tickCount(), false, key, partialTick()));
			logicalDownCount[key] = 0;
		}
		physicalDown.clear();
	}

	private static float seconds(long nanos) {
		return (System.nanoTime() - nanos) / 1_000_000_000f;
	}

	private void quitToHub() {
		if (challenge != null || origin != MinigameOrigin.MENU) {
			Minecraft.getInstance().setScreen(null);
			return;
		}
		Minecraft.getInstance().setScreen(new MinigamesScreen());
	}

	@Override
	public void removed() {
		if (isRunActive()) {
			flush(true);
			logic.forceFinish();
			stage = Stage.VERIFYING;
		}
		resumeSounds();
		super.removed();
	}

	@Override
	public void tick() {
		super.tick();
		switch (stage) {
			case CONNECTING -> {
				if (++waitTicks > CONNECT_TIMEOUT_TICKS) {
					stage = Stage.REJECTED;
					rejectKey = "gui.dragonminez.minigame.reject.timeout";
				}
			}
			case PLAYING -> {
				logic.tick();
				onLogicTick();
				if (logic.levelsCleared() > lastLevelsCleared) {
					lastLevelsCleared = logic.levelsCleared();
					levelPopNanos = System.nanoTime();
					playUi(SoundEvents.PLAYER_LEVELUP, 1.2F, 0.5f);
				}
				if (logic.isFinished()) {
					flush(true);
					stage = Stage.VERIFYING;
					waitTicks = 0;
				} else if (logic.tickCount() - chunkStartTick >= FLUSH_INTERVAL_TICKS) {
					flush(false);
				}
			}
			case PAUSED -> {
				keepAlive();
				if (pauseClosing && seconds(pauseClosingNanos) >= PAUSE_CLOSE) {
					stage = Stage.RESUMING;
					resumeAtNanos = System.nanoTime();
				}
			}
			case RESUMING -> {
				keepAlive();
				if (seconds(resumeAtNanos) >= COUNTDOWN_SECONDS) {
					stage = Stage.PLAYING;
					resumeSounds();
				}
			}
			case VERIFYING -> {
				if (++waitTicks > VERIFY_TIMEOUT_TICKS) enterFinished();
			}
			default -> {}
		}
	}

	private void keepAlive() {
		if (++keepAliveTicks >= KEEP_ALIVE_TICKS) {
			keepAliveTicks = 0;
			flush(false, true);
		}
	}

	protected int mapPhysicalKey(int physical) {
		if (physical == MOUSE_KEY_BASE - GLFW.GLFW_MOUSE_BUTTON_LEFT) return MinigameEvent.ACTION;
		if (physical == GLFW.GLFW_KEY_SPACE) return MinigameEvent.ACTION;
		if (physical == GLFW.GLFW_KEY_LEFT || physical == GLFW.GLFW_KEY_A) return MinigameEvent.LEFT;
		if (physical == GLFW.GLFW_KEY_RIGHT || physical == GLFW.GLFW_KEY_D) return MinigameEvent.RIGHT;
		if (physical == GLFW.GLFW_KEY_UP || physical == GLFW.GLFW_KEY_W) return MinigameEvent.UP;
		if (physical == GLFW.GLFW_KEY_DOWN || physical == GLFW.GLFW_KEY_S) return MinigameEvent.DOWN;
		return -1;
	}

	protected int remapLogicalKey(int logical) {
		return logical;
	}

	protected boolean usesClicks() {
		return false;
	}

	private boolean pressPhysical(int physical) {
		int mapped = mapPhysicalKey(physical);
		int logical = mapped < 0 ? -1 : remapLogicalKey(mapped);
		if (logical < 0) return false;
		if (physicalDown.containsKey(physical)) return true;
		physicalDown.put(physical, logical);
		if (logicalDownCount[logical]++ == 0) emit(MinigameEvent.key(logic.tickCount(), true, logical, partialTick()));
		return true;
	}

	private boolean releasePhysical(int physical) {
		Integer logical = physicalDown.remove(physical);
		if (logical == null) return false;
		logicalDownCount[logical] = Math.max(0, logicalDownCount[logical] - 1);
		if (logicalDownCount[logical] == 0) emit(MinigameEvent.key(logic.tickCount(), false, logical, partialTick()));
		return true;
	}

	protected void emitClick(double fieldX, double fieldY) {
		emit(MinigameEvent.click(logic.tickCount(), fieldX, fieldY, partialTick()));
	}

	private void emit(MinigameEvent event) {
		if (stage != Stage.PLAYING || logic == null || logic.isFinished()) return;
		logic.applyEvent(event);
		outbox.add(event);
		onInputApplied(event);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		switch (stage) {
			case READY -> {
				if (keyCode == GLFW.GLFW_KEY_ESCAPE) quitToHub();
				else if (!onReadyKey(keyCode) && isConfirmKey(keyCode)) requestStart();
				return true;
			}
			case CONNECTING, PREPARING, VERIFYING -> {
				if (keyCode == GLFW.GLFW_KEY_ESCAPE) quitToHub();
				return true;
			}
			case PAUSED -> {
				if (keyCode == GLFW.GLFW_KEY_ESCAPE || isConfirmKey(keyCode)) requestResume();
				return true;
			}
			case RESUMING -> {
				if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
					stage = Stage.PAUSED;
					pausedAtNanos = System.nanoTime();
					pauseClosing = false;
				}
				return true;
			}
			case PLAYING -> {
				if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
					pauseGame();
					return true;
				}
				if (onPlayingKey(keyCode)) return true;
				if (pressPhysical(keyCode)) return true;
			}
			case FINISHED, REJECTED -> {
				continueAfterFinish();
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (stage == Stage.PLAYING && releasePhysical(keyCode)) return true;
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		switch (stage) {
			case READY -> {
				if (onReadyClick(mouseX, mouseY)) return true;
				if (inside(playButton, readyToUiX(mouseX), readyToUiY(mouseY))) {
					playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.5f);
					requestStart();
				}
				return true;
			}
			case PAUSED -> {
				if (pauseClosing) return true;
				float x = readyToUiX(mouseX), y = readyToUiY(mouseY);
				if (inside(resumeButton, x, y)) requestResume();
				else if (inside(finishButton, x, y)) {
					playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.9f, 0.5f);
					endByPlayer();
				}
				return true;
			}
			case RESUMING -> {
				return true;
			}
			case FINISHED, REJECTED -> {
				continueAfterFinish();
				return true;
			}
			case PLAYING -> {
				if (usesClicks()) {
					if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
						emitClick((mouseX - fieldLeft) / fieldScale, (mouseY - fieldTop) / fieldScale);
						return true;
					}
				} else if (pressPhysical(MOUSE_KEY_BASE - button)) {
					return true;
				}
			}
			default -> {}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (stage == Stage.PLAYING && releasePhysical(MOUSE_KEY_BASE - button)) return true;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private static boolean isConfirmKey(int keyCode) {
		return keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE;
	}

	private static boolean inside(float[] rect, double x, double y) {
		return rect != null && x >= rect[0] && x <= rect[0] + rect[2] && y >= rect[1] && y <= rect[1] + rect[3];
	}

	private void enterFinished() {
		stage = Stage.FINISHED;
		finishedAtNanos = System.nanoTime();
	}

	private float finishedSeconds() {
		return (System.nanoTime() - finishedAtNanos) / 1_000_000_000f;
	}

	private void continueAfterFinish() {
		if (stage == Stage.FINISHED && finishedSeconds() < RESULTS_INPUT_DELAY) return;
		if (challenge != null && finalInfo != null && finalInfo.getChallengeOutcome() == MinigameResultS2C.CHALLENGE_FAILED) {
			challenge.restart();
			return;
		}
		if (challenge != null && stage == Stage.REJECTED) {
			Minecraft.getInstance().setScreen(null);
			return;
		}
		quitToHub();
	}

	protected void playUi(SoundEvent sound, float pitch, float volume) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}

	protected void playHit(boolean perfect) {
		playUi(SoundEvents.EXPERIENCE_ORB_PICKUP, perfect ? 1.5f : 1.0f, 0.3f);
	}

	protected void playMiss() {
		playUi(SoundEvents.NOTE_BLOCK_BASS.value(), 0.5F, 0.4f);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		if (logic != null) renderGame(graphics, partialTick());

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(howToKey), this.width / 2, this.height - 16, 0xFFB0B0B0);
		drawBigTitle(graphics);

		if (isRunActive() || stage == Stage.VERIFYING) drawRunningHud(graphics);
		switch (stage) {
			case READY -> renderReadyOverlay(graphics);
			case PAUSED -> renderPauseOverlay(graphics);
			case RESUMING -> renderCountdown(graphics);
			case CONNECTING -> renderMessageOverlay(graphics, tr("gui.dragonminez.minigame.connecting"), 0xFFFFFFFF);
			case PREPARING -> renderMessageOverlay(graphics, tr(preparingMessageKey()), 0xFFFFD700);
			case VERIFYING -> renderMessageOverlay(graphics, tr("gui.dragonminez.minigame.verifying"), 0xFFFFFFFF);
			case FINISHED -> renderFinishedOverlay(graphics);
			case REJECTED -> renderMessageOverlay(graphics, tr(rejectKey.isEmpty() ? "gui.dragonminez.minigame.reject.unavailable" : rejectKey), 0xFFFF7777);
			default -> {}
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private void drawBigTitle(GuiGraphics graphics) {
		MutableComponent title = this.getTitle().copy().withStyle(ChatFormatting.BOLD);
		graphics.pose().pushPose();
		graphics.pose().translate(this.width / 2f, 14, 0);
		graphics.pose().scale(3.0f, 3.0f, 1.0f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, title, 0, 0, 0xFFFFD700);
		graphics.pose().popPose();
	}

	protected static String gradeLetter(double performance) {
		if (performance >= 0.9) return "S";
		if (performance >= 0.75) return "A";
		if (performance >= 0.55) return "B";
		if (performance >= 0.35) return "C";
		return "D";
	}

	private static int gradeColor(String grade) {
		return switch (grade) {
			case "S" -> 0xFFFFD700;
			case "A" -> 0xFF55FF55;
			case "B" -> 0xFF7CFDD6;
			case "C" -> 0xFFFFAA55;
			default -> 0xFFFF5555;
		};
	}

	private void drawRunningHud(GuiGraphics graphics) {
		if (logic == null) return;
		int cx = this.width / 2;
		String grade = gradeLetter(logic.performance());
		if (!grade.equals(lastGrade)) {
			if (!lastGrade.isEmpty()) gradePopNanos = System.nanoTime();
			lastGrade = grade;
		}
		float levelAge = (System.nanoTime() - levelPopNanos) / 1_000_000_000f;
		float gradeAge = (System.nanoTime() - gradePopNanos) / 1_000_000_000f;
		float levelScale = levelAge < 0.45f ? 1f + 0.6f * (float) Math.exp(-levelAge * 9) : 1f;
		float gradeScale = gradeAge < 0.45f ? 1f + 0.5f * (float) Math.exp(-gradeAge * 9) : 1f;
		MutableComponent levelText = tr("gui.dragonminez.training.level", logic.level());
		MutableComponent gradeText = tr("gui.dragonminez.minigame.grade", grade);
		int levelWidth = this.font.width(levelText);
		int gradeWidth = this.font.width(gradeText);
		int gap = 16;
		float lx = cx - (levelWidth + gap + gradeWidth) / 2f + levelWidth / 2f;
		float gx = lx + levelWidth / 2f + gap + gradeWidth / 2f;
		int levelColor = levelAge < 0.45f ? 0xFFFFFFFF : 0xFFFFD700;
		drawScaledCentered(graphics, levelText, lx, 40, levelScale, levelColor);
		drawScaledCentered(graphics, gradeText, gx, 40, gradeScale, gradeColor(grade));

		MutableComponent info = Component.empty().withStyle(Style.EMPTY.withFont(DMZ_FONT));
		boolean any = false;
		if (progressInfo != null && progressInfo.getTpEarned() > 0) {
			int shown = Math.round(hudTp.update(progressInfo.getTpEarned(), 5f));
			any = appendInfo(info, false, tr("gui.dragonminez.minigame.tps_so_far", shown), 0x55FF55);
		}
		if (startInfo != null && startInfo.isMentorBonus()) {
			int pct = (int) Math.round(ConfigManager.getTrainingConfig().getMentorBonus() * 100);
			any = appendInfo(info, any, tr("gui.dragonminez.minigame.mentor_bonus", pct), 0x7CFDD6);
		}
		if (startInfo != null && !startInfo.isKnown() && startInfo.getOrigin() == MinigameOrigin.MASTER) {
			boolean reached = logic.level() >= startInfo.getLearnRequiredLevel();
			any = appendInfo(info, any, tr("gui.dragonminez.minigame.learning", startInfo.getLearnRuns(), startInfo.getLearnRequiredRuns(),
					startInfo.getLearnRequiredLevel()), reached ? 0x55FF55 : 0xFFAA55);
		}
		if (startInfo != null && startInfo.getOrigin() == MinigameOrigin.CHALLENGE && startInfo.getTargetLevels() > 0) {
			any = appendInfo(info, any, tr("gui.dragonminez.minigame.challenge_stage", challengeStage + 1, MinigameLogics.CHALLENGE_STAGES.size(),
					Math.min(logic.levelsCleared(), startInfo.getTargetLevels()), startInfo.getTargetLevels()), 0xFFD700);
		}
		if (any) TextUtil.drawCenteredStringWithBorder(graphics, this.font, info, cx, INFO_LINE_Y, 0xFFFFFFFF);
	}

	private boolean appendInfo(MutableComponent line, boolean separator, MutableComponent part, int rgb) {
		if (separator) line.append(dmz("  •  ").withStyle(style -> style.withColor(0x8890A8)));
		line.append(part.withStyle(style -> style.withColor(rgb)));
		return true;
	}

	private void drawScaledCentered(GuiGraphics graphics, Component text, float x, float y, float scale, int color) {
		graphics.pose().pushPose();
		graphics.pose().translate(x, y + 4, 0);
		graphics.pose().scale(scale, scale, 1f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, text, 0, -4, color);
		graphics.pose().popPose();
	}

	private void drawNpcPanel(GuiGraphics graphics, int cx, int cy, int contentW, int contentH) {
		int padding = 16;
		int panelW = contentW + padding * 2;
		int panelH = contentH + padding * 2;
		float scaleX = (float) panelW / PANEL_TEX_W;
		float scaleY = (float) panelH / PANEL_TEX_H;
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		graphics.pose().pushPose();
		graphics.pose().translate(cx - panelW / 2f, cy - panelH / 2f, 0);
		graphics.pose().scale(scaleX, scaleY, 1f);
		HudRender.blit(graphics, MENU_NPC_TEXTURE, 0, 0, 0, 0, PANEL_TEX_W, PANEL_TEX_H, 512, 512);
		graphics.pose().popPose();
	}

	private void renderReadyOverlay(GuiGraphics graphics) {
		graphics.fill(0, 0, this.width, this.height, 0xBB000000);
		int cx = this.width / 2;
		int cy = this.height / 2;
		int wrapWidth = Math.max(120, Math.min(260, this.width - 90));
		List<FormattedCharSequence> lines = this.font.split(tr(howToKey), wrapWidth);
		int extra = readyExtraHeight();
		int panelContentH = 16 + lines.size() * 10 + (extra > 0 ? extra + 2 : 0) + 6 + BUTTON_H;
		float available = this.height - 40f;
		float scale = Math.min(1f, available / (panelContentH + 32f));
		graphics.pose().pushPose();
		graphics.pose().translate(cx, cy, 0);
		graphics.pose().scale(scale, scale, 1f);
		graphics.pose().translate(-cx, -cy, 0);
		drawNpcPanel(graphics, cx, cy, wrapWidth + 20, panelContentH);
		int top = cy - panelContentH / 2;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.start"), cx, top, 0xFFFFD700);
		int y = top + 16;
		for (FormattedCharSequence line : lines) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, line, cx, y, 0xFFB0B0B0);
			y += 10;
		}
		readyScale = scale;
		float buttonY = y + (extra > 0 ? extra + 2 : 0) + 6;
		playButton = new float[]{cx - BUTTON_W / 2f, buttonY, BUTTON_W, BUTTON_H};
		drawMenuButton(graphics, playButton, "gui.dragonminez.minigame.play", 1f);
		if (extra > 0) renderReadyExtra(graphics, cx, y + 2);
		graphics.pose().popPose();
	}

	private void drawMenuButton(GuiGraphics graphics, float[] rect, String key, float alpha) {
		boolean hover = inside(rect, readyToUiX(guiMouseX()), readyToUiY(guiMouseY()));
		RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
		RenderSystem.enableBlend();
		HudRender.blit(graphics, BUTTONS, rect[0], rect[1], 0, hover ? 48 : 28, BUTTON_W, BUTTON_H, 256, 256);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		int color = fadeColor(hover ? 0xFFFFD700 : 0xFFFFFFFF, alpha);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(key), (int) (rect[0] + rect[2] / 2f), (int) (rect[1] + 6), color);
	}

	private void renderPauseOverlay(GuiGraphics graphics) {
		float appear = pauseClosing ? 1f - easeOut(seconds(pauseClosingNanos) / PAUSE_CLOSE) : easeOut(seconds(pausedAtNanos) / PAUSE_FADE);
		graphics.fill(0, 0, this.width, this.height, (Math.round(0xC8 * appear) << 24));
		if (appear <= 0.02f) return;
		int cx = this.width / 2;
		int cy = this.height / 2;
		int wrapWidth = Math.max(120, Math.min(260, this.width - 90));
		List<FormattedCharSequence> rules = this.font.split(tr(howToKey), wrapWidth);
		int seconds = logic == null ? 0 : logic.tickCount() / 20;
		String grade = logic == null ? "-" : gradeLetter(logic.performance());
		Component progress = tr("gui.dragonminez.minigame.summary", logic == null ? 1 : logic.level(), String.format("%d:%02d", seconds / 60, seconds % 60), grade);
		Component tpLine = progressInfo != null && progressInfo.getTpEarned() > 0 ? tr("gui.dragonminez.minigame.tps_so_far", Math.round(hudTp.get())) : null;
		int contentH = 18 + 12 + (tpLine != null ? 12 : 0) + 8 + rules.size() * 10 + 10 + BUTTON_H;
		float fit = Math.min(1f, (this.height - 40f) / (contentH + 32f));
		float scale = fit * (0.9f + 0.1f * easeOutBack(Math.min(1f, seconds(pausedAtNanos) / PAUSE_FADE)));
		readyScale = scale;
		graphics.pose().pushPose();
		graphics.pose().translate(cx, cy + (1f - appear) * 14f, 0);
		graphics.pose().scale(scale, scale, 1f);
		graphics.pose().translate(-cx, -cy, 0);
		RenderSystem.setShaderColor(1f, 1f, 1f, Math.max(0.02f, appear));
		RenderSystem.enableBlend();
		drawNpcPanel(graphics, cx, cy, wrapWidth + 20, contentH);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		int y = cy - contentH / 2;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.paused"), cx, y, fadeColor(0xFFFFD700, appear));
		y += 18;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, progress, cx, y, fadeColor(0xFFFFFFFF, appear));
		y += 12;
		if (tpLine != null) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tpLine, cx, y, fadeColor(0xFF55FF55, appear));
			y += 12;
		}
		y += 8;
		for (FormattedCharSequence line : rules) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, line, cx, y, fadeColor(0xFFB0B0B0, appear));
			y += 10;
		}
		y += 10;
		resumeButton = new float[]{cx - BUTTON_W - 6, y, BUTTON_W, BUTTON_H};
		finishButton = new float[]{cx + 6, y, BUTTON_W, BUTTON_H};
		drawMenuButton(graphics, resumeButton, "gui.dragonminez.minigame.resume", appear);
		drawMenuButton(graphics, finishButton, "gui.dragonminez.minigame.finish", appear);
		graphics.pose().popPose();
	}

	private void renderCountdown(GuiGraphics graphics) {
		float elapsed = seconds(resumeAtNanos);
		float dim = 1f - Math.min(1f, elapsed / COUNTDOWN_SECONDS);
		graphics.fill(0, 0, this.width, this.height, (Math.round(0x70 * dim) << 24));
		int number = Math.max(1, (int) Math.ceil(COUNTDOWN_SECONDS - elapsed));
		float within = elapsed - (float) Math.floor(elapsed);
		float pop = 1f + 0.8f * (float) Math.exp(-within * 7);
		float alpha = within > 0.75f ? 1f - (within - 0.75f) / 0.25f : 1f;
		graphics.pose().pushPose();
		graphics.pose().translate(this.width / 2f, this.height / 2f, 0);
		graphics.pose().scale(3.5f * pop, 3.5f * pop, 1f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmz(String.valueOf(number)), 0, -4, fadeColor(0xFFFFD700, alpha));
		graphics.pose().popPose();
	}

	protected float readyToUiX(double mouseX) {
		float cx = this.width / 2f;
		return (float) (cx + (mouseX - cx) / readyScale);
	}

	protected float readyToUiY(double mouseY) {
		float cy = this.height / 2f;
		return (float) (cy + (mouseY - cy) / readyScale);
	}

	private void renderMessageOverlay(GuiGraphics graphics, Component message, int color) {
		graphics.fill(0, 0, this.width, this.height, 0x99000000);
		int cx = this.width / 2;
		int cy = this.height / 2;
		List<FormattedCharSequence> lines = this.font.split(message, 260);
		drawNpcPanel(graphics, cx, cy, 280, lines.size() * 10 + 4);
		int y = cy - lines.size() * 5;
		for (FormattedCharSequence line : lines) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, line, cx, y, color);
			y += 10;
		}
	}

	private static float easeOut(float t) {
		float u = 1f - Math.max(0f, Math.min(1f, t));
		return 1f - u * u * u;
	}

	private static float easeOutBack(float t) {
		t = Math.max(0f, Math.min(1f, t));
		float c1 = 1.70158f, c3 = c1 + 1f;
		float u = t - 1f;
		return 1f + c3 * u * u * u + c1 * u * u;
	}

	private static int fadeColor(int argb, float alpha) {
		int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0.05f, Math.min(1f, alpha)));
		return (Math.max(8, a) << 24) | (argb & 0xFFFFFF);
	}

	private void renderFinishedOverlay(GuiGraphics graphics) {
		float elapsed = finishedSeconds();
		float appear = easeOut(elapsed / RESULTS_FADE);
		graphics.fill(0, 0, this.width, this.height, (Math.round(0xBB * appear) << 24));
		int cx = this.width / 2;
		int cy = this.height / 2;
		List<Component> lines = new ArrayList<>();
		List<Integer> colors = new ArrayList<>();
		boolean challengeFailed = finalInfo != null && finalInfo.getChallengeOutcome() == MinigameResultS2C.CHALLENGE_FAILED;

		if (challengeFailed) {
			lines.add(tr("gui.dragonminez.minigame.challenge_failed"));
			colors.add(0xFFFF7777);
		} else if (finalInfo == null) {
			lines.add(tr("gui.dragonminez.minigame.unverified"));
			colors.add(0xFFFFAA55);
		} else if (finalInfo.isInvalid()) {
			lines.add(tr("gui.dragonminez.minigame.invalid"));
			colors.add(0xFFFF7777);
		} else {
			int levels = finalInfo.getLevelsCleared();
			int seconds = finalInfo.getTicksPlayed() / 20;
			String grade = gradeLetter(finalInfo.getPerformance());
			lines.add(tr("gui.dragonminez.minigame.summary", levels + 1, String.format("%d:%02d", seconds / 60, seconds % 60), grade));
			colors.add(0xFFFFFFFF);
			lines.add(tr("gui.dragonminez.minigame.tps_won", finalInfo.getTpEarned()));
			colors.add(0xFF55FF55);
			appendResultLines(lines, colors);
			if (finalInfo.isMentorBonus()) {
				int pct = (int) Math.round(ConfigManager.getTrainingConfig().getMentorBonus() * 100);
				lines.add(tr("gui.dragonminez.minigame.mentor_bonus", pct));
				colors.add(0xFF7CFDD6);
			}
			if (finalInfo.isLearnedNow()) {
				lines.add(tr("gui.dragonminez.minigame.learned", tr("gui.dragonminez.minigame." + minigameId)));
				colors.add(0xFFFFD700);
			} else if (startInfo != null && !startInfo.isKnown() && startInfo.getOrigin() == MinigameOrigin.MASTER) {
				lines.add(tr(finalInfo.isLearnRunCounted() ? "gui.dragonminez.minigame.learn_counted" : "gui.dragonminez.minigame.learn_not_counted",
						finalInfo.getLearnRuns(), finalInfo.getLearnRequiredRuns(), startInfo.getLearnRequiredLevel()));
				colors.add(finalInfo.isLearnRunCounted() ? 0xFF55FF55 : 0xFFFFAA55);
			}
		}

		int contentH = 30 + lines.size() * 14;
		float scale = 0.85f + 0.15f * easeOutBack(elapsed / RESULTS_FADE);
		graphics.pose().pushPose();
		graphics.pose().translate(cx, cy + (1f - appear) * 18f, 0);
		graphics.pose().scale(scale, scale, 1f);
		graphics.pose().translate(-cx, -cy, 0);
		RenderSystem.setShaderColor(1f, 1f, 1f, Math.max(0.02f, appear));
		RenderSystem.enableBlend();
		drawNpcPanel(graphics, cx, cy, 280, contentH);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		int y = cy - contentH / 2 + 2;
		if (appear > 0.05f) TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.finished"), cx, y, fadeColor(0xFFFFD700, appear));
		y += 16;
		for (int i = 0; i < lines.size(); i++) {
			float lineAppear = easeOut((elapsed - RESULTS_FADE * 0.5f - i * RESULTS_LINE_DELAY) / 0.25f);
			if (lineAppear > 0.05f) {
				graphics.pose().pushPose();
				graphics.pose().translate((1f - lineAppear) * -10f, 0, 0);
				TextUtil.drawCenteredStringWithBorder(graphics, this.font, lines.get(i), cx, y, fadeColor(colors.get(i), lineAppear));
				graphics.pose().popPose();
			}
			y += 14;
		}
		float promptAppear = easeOut((elapsed - RESULTS_INPUT_DELAY) / 0.3f);
		if (promptAppear > 0.05f) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font,
					tr(challengeFailed ? "gui.dragonminez.minigame.challenge_retry" : "gui.dragonminez.minigame.continue"), cx, y + 2, fadeColor(0xFF777777, promptAppear));
		}
		graphics.pose().popPose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	protected static MutableComponent dmz(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	protected MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	protected static final int SHEET_SIZE = 256;
	protected static final int UI_V = 240;
	protected static final int FILL_GOLD = 0;
	protected static final int FILL_GREEN = 1;
	protected static final int FILL_CYAN = 2;
	protected static final int FILL_RED = 3;
	protected static final int FILL_PURPLE = 4;
	protected static final int BAR_THICKNESS = 9;

	protected static void blitRegion(GuiGraphics graphics, ResourceLocation texture, float x, float y, float w, float h, float u, float v, float uw, float vh) {
		if (w <= 0 || h <= 0 || uw <= 0 || vh <= 0) return;
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		graphics.pose().scale(w / uw, h / vh, 1f);
		HudRender.blit(graphics, texture, 0, 0, u, v, uw, vh, SHEET_SIZE, SHEET_SIZE);
		graphics.pose().popPose();
	}

	protected static void drawBarH(GuiGraphics graphics, ResourceLocation sheet, float x, float y, float w, float pct, int fill) {
		pct = Math.max(0f, Math.min(1f, pct));
		blitRegion(graphics, sheet, x, y, 4, 9, 0, UI_V, 4, 9);
		blitRegion(graphics, sheet, x + 4, y, w - 8, 9, 4, UI_V, 1, 9);
		blitRegion(graphics, sheet, x + w - 4, y, 4, 9, 5, UI_V, 4, 9);
		float inner = (w - 4) * pct;
		if (inner > 0) {
			blitRegion(graphics, sheet, x + 2, y + 2, inner, 5, 18 + fill, UI_V, 1, 5);
			if (pct < 1f) {
				rectF(graphics, x + 2 + inner - 1, y + 2, 1, 5, 0xB0FFFFFF);
				graphics.flush();
			}
		}
	}

	protected static void drawBarV(GuiGraphics graphics, ResourceLocation sheet, float x, float y, float h, float pct, int fill) {
		pct = Math.max(0f, Math.min(1f, pct));
		blitRegion(graphics, sheet, x, y, 9, 4, 9, UI_V, 9, 4);
		blitRegion(graphics, sheet, x, y + 4, 9, h - 8, 9, UI_V + 4, 9, 1);
		blitRegion(graphics, sheet, x, y + h - 4, 9, 4, 9, UI_V + 5, 9, 4);
		float inner = (h - 4) * pct;
		if (inner > 0) {
			blitRegion(graphics, sheet, x + 2, y + 2 + (h - 4) - inner, 5, inner, 24, UI_V + fill, 5, 1);
			if (pct < 1f) {
				rectF(graphics, x + 2, y + 2 + (h - 4) - inner, 5, 1, 0xB0FFFFFF);
				graphics.flush();
			}
		}
	}

	protected static void drawHearts(GuiGraphics graphics, ResourceLocation sheet, float cx, float y, int lives, int max, float scale, long lostNanos) {
		float step = 9 * scale;
		float x0 = cx - (max * step - 2 * scale) / 2f;
		float lostAge = (System.nanoTime() - lostNanos) / 1_000_000_000f;
		for (int i = 0; i < max; i++) {
			boolean full = i < lives;
			float s = scale;
			float dy = 0;
			if (i == lives && lostAge < 0.5f) {
				s *= 1f + 0.5f * (1f - lostAge / 0.5f);
				dy = -4f * (1f - lostAge / 0.5f);
			}
			float size = 7 * s;
			blitRegion(graphics, sheet, x0 + i * step + (7 * scale - size) / 2f, y + dy + (7 * scale - size) / 2f, size, size, full ? 32 : 40, UI_V, 7, 7);
		}
	}

	protected void pushField(GuiGraphics graphics) {
		graphics.pose().pushPose();
		graphics.pose().translate(fieldLeft, fieldTop, 0);
		graphics.pose().scale(fieldScale, fieldScale, 1f);
	}

	protected void popField(GuiGraphics graphics) {
		graphics.flush();
		graphics.pose().popPose();
	}

	protected static void rectF(GuiGraphics graphics, float x, float y, float w, float h, int argb) {
		if (w <= 0 || h <= 0) return;
		quad(graphics, x, y, x, y + h, x + w, y + h, x + w, y, argb, argb);
	}

	protected static void rectGradient(GuiGraphics graphics, float x, float y, float w, float h, int top, int bottom) {
		if (w <= 0 || h <= 0) return;
		quad(graphics, x, y, x, y + h, x + w, y + h, x + w, y, top, bottom);
	}

	protected static void outlineF(GuiGraphics graphics, float x, float y, float w, float h, float t, int argb) {
		rectF(graphics, x, y, w, t, argb);
		rectF(graphics, x, y + h - t, w, t, argb);
		rectF(graphics, x, y + t, t, h - 2 * t, argb);
		rectF(graphics, x + w - t, y + t, t, h - 2 * t, argb);
	}

	protected static void triangle(GuiGraphics graphics, float ax, float ay, float bx, float by, float cx, float cy, int argb) {
		quad(graphics, ax, ay, bx, by, cx, cy, cx, cy, argb, argb);
	}

	protected static void ring(GuiGraphics graphics, float cx, float cy, float radius, float thickness, int argb) {
		if (radius <= 0) return;
		int segments = Math.max(20, Math.min(96, (int) (radius * 2.5f)));
		float inner = Math.max(0, radius - thickness);
		for (int i = 0; i < segments; i++) {
			double a0 = Math.PI * 2 * i / segments;
			double a1 = Math.PI * 2 * (i + 1) / segments;
			float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
			float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
			quad(graphics, cx + c0 * inner, cy + s0 * inner, cx + c0 * radius, cy + s0 * radius,
					cx + c1 * radius, cy + s1 * radius, cx + c1 * inner, cy + s1 * inner, argb, argb);
		}
	}

	protected static void trapezoidQuad(GuiGraphics graphics, float tlx, float tly, float trx, float try_, float brx, float bry, float blx, float bly, int top, int bottom) {
		quad(graphics, tlx, tly, blx, bly, brx, bry, trx, try_, top, bottom);
	}

	protected static void disc(GuiGraphics graphics, float cx, float cy, float radius, int argb) {
		ring(graphics, cx, cy, radius, radius, argb);
	}

	private static void quad(GuiGraphics graphics, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int top, int bottom) {
		Matrix4f matrix = graphics.pose().last().pose();
		VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
		float area = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0);
		if (area > 0) {
			vertex(buffer, matrix, x0, y0, top);
			vertex(buffer, matrix, x3, y3, top);
			vertex(buffer, matrix, x2, y2, bottom);
			vertex(buffer, matrix, x1, y1, bottom);
		} else {
			vertex(buffer, matrix, x0, y0, top);
			vertex(buffer, matrix, x1, y1, bottom);
			vertex(buffer, matrix, x2, y2, bottom);
			vertex(buffer, matrix, x3, y3, top);
		}
	}

	private static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, int argb) {
		buffer.vertex(matrix, x, y, 0.0f).color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF).endVertex();
	}

	protected static int withAlpha(int rgb, float alpha) {
		return (Math.max(0, Math.min(255, Math.round(alpha * 255))) << 24) | (rgb & 0xFFFFFF);
	}

	protected void drawSymbol(GuiGraphics graphics, String symbol, float x, float y, float scale, int color) {
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		graphics.pose().scale(scale, scale, 1.0f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmz(symbol), 0, -4, color);
		graphics.pose().popPose();
	}

	protected abstract void renderGame(GuiGraphics graphics, float partialTick);

	protected MinigameLogic createLogic(MinigameStartS2C message) {
		return MinigameLogics.create(minigameId, ConfigManager.getTrainingConfig(), message.getSeed());
	}

	protected String startOption() {
		return "";
	}

	protected byte[] startPayload() {
		return null;
	}

	protected boolean handleStartRejection(MinigameStartS2C message) {
		return false;
	}

	protected String preparingMessageKey() {
		return "gui.dragonminez.minigame.connecting";
	}

	protected boolean isPreparing() {
		return stage == Stage.PREPARING;
	}

	protected boolean onReadyKey(int keyCode) {
		return false;
	}

	protected boolean onReadyClick(double mouseX, double mouseY) {
		return false;
	}

	protected boolean onPlayingKey(int keyCode) {
		return false;
	}

	protected double guiMouseX() {
		return Minecraft.getInstance().mouseHandler.xpos() * this.width / Minecraft.getInstance().getWindow().getScreenWidth();
	}

	protected double guiMouseY() {
		return Minecraft.getInstance().mouseHandler.ypos() * this.height / Minecraft.getInstance().getWindow().getScreenHeight();
	}

	protected void drawLegend(GuiGraphics graphics, String key, float centerX, float y) {
		graphics.pose().pushPose();
		graphics.pose().translate(centerX, y, 0);
		graphics.pose().scale(0.75f, 0.75f, 1f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(key), 0, 0, 0xFFB8C0DC);
		graphics.pose().popPose();
	}

	protected int readyExtraHeight() {
		return 0;
	}

	protected void renderReadyExtra(GuiGraphics graphics, int cx, int y) {}

	protected void appendResultLines(List<Component> lines, List<Integer> colors) {}

	protected boolean isReady() {
		return stage == Stage.READY;
	}

	protected boolean isChallenge() {
		return challenge != null;
	}

	protected void onLogicStarted() {}

	protected void onLogicTick() {}

	protected void onInputApplied(MinigameEvent event) {}
}
