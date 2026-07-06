package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TrainingConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class ControlGameScreen extends BaseMinigameScreen {

	private static final int DIR_CHANGE_INTERVAL_TICKS = 30;

	private final Random random = new Random();

	private TrainingConfig.ControlConfig cfg;

	private int barLeft, barRight, barY;
	private float markerX;
	private float zoneCenter;
	private int zoneDir = 1;
	private int dirChangeTimer = DIR_CHANGE_INTERVAL_TICKS;
	private int zoneWidth;
	private double holdProgress;
	private int levelTicksLeft;

	public ControlGameScreen() {
		super("control", "gui.dragonminez.minigame.control");
	}

	@Override
	protected void init() {
		super.init();
		this.cfg = ConfigManager.getTrainingConfig().getControl();
		int cx = this.width / 2;
		this.barLeft = cx - cfg.getBarWidth() / 2;
		this.barRight = cx + cfg.getBarWidth() / 2;
		this.barY = this.height / 2;
		this.markerX = cx;
		this.zoneCenter = cx;
		this.zoneWidth = cfg.getBaseZoneWidth();
		this.holdProgress = 0;
		this.levelTicksLeft = cfg.getLevelTimeLimitTicks();
	}

	private double zoneSpeed() {
		return cfg.getBaseZoneSpeed() + (levelsCleared * cfg.getZoneSpeedPerLevel());
	}

	@Override
	protected void tickGame() {
		levelTicksLeft--;
		if (levelTicksLeft <= 0) {
			endGame();
			return;
		}

		if (--dirChangeTimer <= 0) {
			dirChangeTimer = DIR_CHANGE_INTERVAL_TICKS;
			if (random.nextBoolean()) zoneDir = -zoneDir;
		}

		float half = zoneWidth / 2.0f;
		zoneCenter += (float) (zoneDir * zoneSpeed());
		if (zoneCenter - half <= barLeft) {
			zoneCenter = barLeft + half;
			zoneDir = 1;
		} else if (zoneCenter + half >= barRight) {
			zoneCenter = barRight - half;
			zoneDir = -1;
		}

		long window = Minecraft.getInstance().getWindow().getWindow();
		boolean left = isHeld(window, GLFW.GLFW_KEY_LEFT) || isHeld(window, GLFW.GLFW_KEY_A) || isHeld(window, KeyBinds.RHYTHM_LEFT.getKey().getValue());
		boolean right = isHeld(window, GLFW.GLFW_KEY_RIGHT) || isHeld(window, GLFW.GLFW_KEY_D) || isHeld(window, KeyBinds.RHYTHM_RIGHT.getKey().getValue());
		if (left) markerX -= (float) cfg.getMarkerSpeed();
		if (right) markerX += (float) cfg.getMarkerSpeed();
		markerX = Math.max(barLeft, Math.min(barRight, markerX));

		boolean inside = Math.abs(markerX - zoneCenter) <= half;
		if (inside) {
			holdProgress++;
			if (holdProgress >= cfg.getHoldDurationTicks()) levelCleared();
		} else {
			double loss = cfg.getBaseProgressLossPerTick() + (levelsCleared * cfg.getProgressLossPerLevel());
			holdProgress = Math.max(0, holdProgress - loss);
		}
	}

	private boolean isHeld(long window, int keyCode) {
		return keyCode > 0 && GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
	}

	@Override
	protected void onLevelCleared() {
		zoneWidth = Math.max(cfg.getMinZoneWidth(), cfg.getBaseZoneWidth() - levelsCleared * cfg.getZoneWidthDecreasePerLevel());
		holdProgress = 0;
		levelTicksLeft = cfg.getLevelTimeLimitTicks();
		zoneCenter = this.width / 2.0f;
		dirChangeTimer = DIR_CHANGE_INTERVAL_TICKS;
	}

	@Override
	protected void renderGame(GuiGraphics graphics) {
		int cx = this.width / 2;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.training.level", level()), cx, barY - 50, 0xFFFFD700);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.training.time", Math.max(0, levelTicksLeft) / 20 + 1), cx, barY - 38, 0xFFFFFFFF);

		graphics.fill(barLeft, barY - 8, barRight, barY + 8, 0xFF202020);
		graphics.renderOutline(barLeft - 1, barY - 9, (barRight - barLeft) + 2, 18, 0xFFFFFFFF);

		float half = zoneWidth / 2.0f;
		graphics.fill((int) (zoneCenter - half), barY - 8, (int) (zoneCenter + half), barY + 8, 0x9033FF55);

		boolean inside = Math.abs(markerX - zoneCenter) <= half;
		int markerColor = inside ? 0xFFFFFFFF : 0xFFFF5555;
		graphics.fill((int) markerX - 2, barY - 14, (int) markerX + 2, barY + 14, markerColor);

		int pbLeft = cx - 60, pbRight = cx + 60, pbY = barY + 30;
		graphics.fill(pbLeft, pbY, pbRight, pbY + 6, 0xFF333333);
		float pct = Math.min(1.0f, (float) holdProgress / cfg.getHoldDurationTicks());
		graphics.fill(pbLeft, pbY, pbLeft + (int) ((pbRight - pbLeft) * pct), pbY + 6, 0xFF55FF55);
	}
}
