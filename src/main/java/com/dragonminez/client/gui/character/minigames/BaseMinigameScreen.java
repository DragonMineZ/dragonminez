package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.character.MinigamesScreen;
import com.dragonminez.client.util.TextUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TrainingConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.C2S.TrainingRewardC2S;
import com.dragonminez.common.network.C2S.TrainingAnimationC2S;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public abstract class BaseMinigameScreen extends Screen {
	protected static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	private static final ResourceLocation MENU_NPC_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menunpc.png");
	private static final int PANEL_TEX_W = 346;
	private static final int PANEL_TEX_H = 94;

	protected enum State { READY, PLAYING, FINISHED }

	protected final String minigameId;
	private final String howToKey;
	protected int levelsCleared = 0;
	protected State state = State.READY;

	private int finalRewardDisplay = 0;
	private boolean trainingAnimActive = false;

	protected BaseMinigameScreen(String minigameId, String titleKey) {
		super(Component.translatable(titleKey).withStyle(Style.EMPTY.withFont(DMZ_FONT)));
		this.minigameId = minigameId;
		this.howToKey = "gui.dragonminez.minigame." + minigameId + ".howto";
	}

	protected boolean isPlaying() {
		return state == State.PLAYING;
	}

	protected int level() {
		return levelsCleared + 1;
	}

	protected void levelCleared() {
		levelsCleared++;
		playUi(SoundEvents.PLAYER_LEVELUP, 1.2F, 0.5f);
		onLevelCleared();
	}

	protected void endGame() {
		if (state != State.PLAYING) return;
		this.state = State.FINISHED;
		if (levelsCleared > 0) {
			NetworkHandler.sendToServer(new TrainingRewardC2S(minigameId, levelsCleared));
		}
		this.finalRewardDisplay = computeRewardDisplay();
		stopTrainingAnimation();
	}

	private int computeRewardDisplay() {
		if (levelsCleared <= 0) return 0;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return 0;
		TrainingConfig cfg = ConfigManager.getTrainingConfig();
		TrainingConfig.MinigameSettings settings = cfg.getSettings(minigameId);
		return StatsProvider.get(StatsCapability.INSTANCE, mc.player).map(d -> {
			int tpc = d.getSingleStatCost(d.getStats().getTotalStats());
			float total = cfg.computeTpsPerLevel(tpc, settings) * levelsCleared;
			float limit = settings.getTpsLimitPerGame();
			if (limit > 0 && total > limit) total = limit;
			return (int) total;
		}).orElse(0);
	}

	private void quitToHub() {
		Minecraft.getInstance().setScreen(new MinigamesScreen());
	}

	protected void startGame() {
		this.state = State.PLAYING;
		startTrainingAnimation();
		onStart();
	}

	private void startTrainingAnimation() {
		if (trainingAnimActive) return;
		trainingAnimActive = true;
		NetworkHandler.sendToServer(new TrainingAnimationC2S(true));
	}

	private void stopTrainingAnimation() {
		if (!trainingAnimActive) return;
		trainingAnimActive = false;
		NetworkHandler.sendToServer(new TrainingAnimationC2S(false));
	}

	@Override
	public void removed() {
		stopTrainingAnimation();
		super.removed();
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
	public void tick() {
		super.tick();
		if (state == State.PLAYING) tickGame();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		renderGame(graphics);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(howToKey), this.width / 2, this.height - 16, 0xFFB0B0B0);
		drawBigTitle(graphics);

		if (state == State.PLAYING) drawRunningHud(graphics);
		if (state == State.READY) renderReadyOverlay(graphics);
		else if (state == State.FINISHED) renderFinishedOverlay(graphics);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private void drawBigTitle(GuiGraphics graphics) {
		MutableComponent title = this.getTitle().copy().withStyle(net.minecraft.ChatFormatting.BOLD);
		int cx = this.width / 2;
		graphics.pose().pushPose();
		graphics.pose().translate(cx, 14, 0);
		graphics.pose().scale(3.0f, 3.0f, 1.0f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, title, 0, 0, 0xFFFFD700);
		graphics.pose().popPose();
	}

	private void drawRunningHud(GuiGraphics graphics) {
		if (finalRewardDisplay == 0 && levelsCleared == 0) return;
		int tpsNow = computeRewardDisplay();
		if (tpsNow <= 0) return;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.tps_so_far", tpsNow), this.width / 2, 48, 0xFF55FF55);
	}

	private void drawNpcPanel(GuiGraphics graphics, int cx, int cy, int contentW, int contentH) {
		int padding = 16;
		int panelW = contentW + padding * 2;
		int panelH = contentH + padding * 2;
		float scaleX = (float) panelW / PANEL_TEX_W;
		float scaleY = (float) panelH / PANEL_TEX_H;
		int drawX = cx - panelW / 2;
		int drawY = cy - panelH / 2;

		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		graphics.pose().pushPose();
		graphics.pose().translate(drawX, drawY, 0);
		graphics.pose().scale(scaleX, scaleY, 1f);
		graphics.blit(MENU_NPC_TEXTURE, 0, 0, 0, 0, PANEL_TEX_W, PANEL_TEX_H, 512, 512);
		graphics.pose().popPose();
	}

	private void renderReadyOverlay(GuiGraphics graphics) {
		graphics.fill(0, 0, this.width, this.height, 0xBB000000);
		int cx = this.width / 2;
		int cy = this.height / 2;

		int wrapWidth = 260;
		List<FormattedCharSequence> lines = this.font.split(tr(howToKey), wrapWidth);
		int textHeight = lines.size() * 10;

		drawNpcPanel(graphics, cx, cy, 280, 20 + textHeight);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.start"), cx, cy - (textHeight / 2) - 5, 0xFFFFFFFF);

		int y = cy - (textHeight / 2) + 10;
		for (FormattedCharSequence line : lines) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, line, cx, y, 0xFFB0B0B0);
			y += 10;
		}
	}

	private void renderFinishedOverlay(GuiGraphics graphics) {
		graphics.fill(0, 0, this.width, this.height, 0xBB000000);
		int cx = this.width / 2;
		int cy = this.height / 2;

		drawNpcPanel(graphics, cx, cy, 280, 90);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.finished"), cx, cy - 38, 0xFFFFD700);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.rounds_won", levelsCleared), cx, cy - 20, 0xFFFFFFFF);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.tps_won", finalRewardDisplay), cx, cy - 4, 0xFF55FF55);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.good_luck"), cx, cy + 14, 0xFFB0B0B0);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.continue"), cx, cy + 30, 0xFF777777);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		switch (state) {
			case READY -> {
				if (keyCode == GLFW.GLFW_KEY_ESCAPE) quitToHub();
				else startGame();
				return true;
			}
			case FINISHED -> {
				quitToHub();
				return true;
			}
			case PLAYING -> {
				if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
					endGame();
					return true;
				}
				if (onKey(keyCode)) return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		switch (state) {
			case READY -> {
				startGame();
				return true;
			}
			case FINISHED -> {
				quitToHub();
				return true;
			}
			case PLAYING -> {
				if (onMouseClick(mouseX, mouseY, button)) return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	protected MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	protected abstract void tickGame();

	protected abstract void renderGame(GuiGraphics graphics);

	protected boolean onKey(int keyCode) {
		return false;
	}

	protected boolean onMouseClick(double mouseX, double mouseY, int button) {
		return false;
	}

	protected void onStart() {}

	protected void onLevelCleared() {}
}