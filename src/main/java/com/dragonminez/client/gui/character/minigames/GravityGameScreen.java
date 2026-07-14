package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TrainingConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class GravityGameScreen extends BaseMinigameScreen {

	private TrainingConfig.GravityConfig cfg;
	private final Random random = new Random();

	private int barX, barTop, barBottom;
	private int controlLineY;
	private float indicatorY;
	private double holdProgress;
	private int neededSide = -1;
	private int wrongFlashSide = 0;
	private int wrongFlashTicks = 0;

	public GravityGameScreen() {
		super("gravity", "gui.dragonminez.minigame.gravity");
	}

	@Override
	protected void init() {
		super.init();
		this.cfg = ConfigManager.getTrainingConfig().getGravity();
		this.barX = this.width / 2;
		this.barTop = this.height / 2 - cfg.getBarHeight() / 2;
		this.barBottom = this.height / 2 + cfg.getBarHeight() / 2;
		this.controlLineY = barTop + (int) (cfg.getBarHeight() * cfg.getControlLineFraction());
		this.indicatorY = barTop + 20;
		this.holdProgress = 0;
		this.neededSide = random.nextBoolean() ? -1 : 1;
		this.wrongFlashTicks = 0;
	}

	@Override
	protected void onStart() {
		neededSide = random.nextBoolean() ? -1 : 1;
	}

	private double gravity() {
		return cfg.getBaseGravity() + (levelsCleared * cfg.getGravityPerLevel());
	}

	@Override
	protected void tickGame() {
		if (wrongFlashTicks > 0) wrongFlashTicks--;

		indicatorY += (float) gravity();
		if (indicatorY >= barBottom) {
			indicatorY = barBottom;
			endGame();
			return;
		}

		if (indicatorY < controlLineY) {
			holdProgress++;
			if (holdProgress >= cfg.getHoldDurationTicks()) levelCleared();
		} else holdProgress = Math.max(0, holdProgress - cfg.getProgressLossPerTick());
	}

	@Override
	protected boolean onKey(int keyCode) {
		int side = sideForKey(keyCode);
		if (side == 0) return false;
		pushSide(side);
		return true;
	}

	@Override
	protected boolean onMouseClick(double mouseX, double mouseY, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			pushSide(-1);
			return true;
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			pushSide(1);
			return true;
		}
		return false;
	}

	private int sideForKey(int keyCode) {
		if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A || keyCode == KeyBinds.RHYTHM_LEFT.getKey().getValue()) return -1;
		if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D || keyCode == KeyBinds.RHYTHM_RIGHT.getKey().getValue()) return 1;
		return 0;
	}

	private void pushSide(int side) {
		if (side == neededSide) {
			indicatorY -= (float) cfg.getRisePerTap();
			if (indicatorY < barTop) indicatorY = barTop;
			playHit(false);
			neededSide = random.nextBoolean() ? -1 : 1;
		} else {
			indicatorY += (float) (cfg.getWrongPressDescentMultiplier() * gravity());
			wrongFlashSide = side;
			wrongFlashTicks = 6;
			playMiss();
		}
	}

	@Override
	protected void onLevelCleared() {
		holdProgress = 0;
		indicatorY = barTop + 20;
		neededSide = random.nextBoolean() ? -1 : 1;
	}

	@Override
	protected void renderGame(GuiGraphics graphics) {
		int cx = this.width / 2;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.training.level", level()), cx, barTop - 26, 0xFFFFD700);

		graphics.fill(barX - 10, barTop, barX + 10, barBottom, 0xFF202020);
		graphics.renderOutline(barX - 11, barTop - 1, 22, (barBottom - barTop) + 2, 0xFFFFFFFF);

		graphics.fill(barX - 16, controlLineY - 1, barX + 16, controlLineY + 1, 0xFFFFD700);
		graphics.fill(barX - 9, barTop + 1, barX + 9, controlLineY, 0x3033FF55);

		boolean above = indicatorY < controlLineY;
		int color = above ? 0xFF55FF55 : 0xFFFF5555;
		graphics.fill(barX - 9, (int) indicatorY - 3, barX + 9, (int) indicatorY + 3, color);

		drawSideArrow(graphics, barX - 34, (barTop + barBottom) / 2, "←", -1);
		drawSideArrow(graphics, barX + 34, (barTop + barBottom) / 2, "→", 1);

		int pbLeft = cx - 60, pbRight = cx + 60, pbY = barBottom + 18;
		graphics.fill(pbLeft, pbY, pbRight, pbY + 6, 0xFF333333);
		float pct = (float) Math.min(1.0, holdProgress / cfg.getHoldDurationTicks());
		graphics.fill(pbLeft, pbY, pbLeft + (int) ((pbRight - pbLeft) * pct), pbY + 6, 0xFF55FF55);
	}

	private void drawSideArrow(GuiGraphics graphics, int x, int y, String symbol, int side) {
		int color = 0xFF707070;
		if (wrongFlashTicks > 0 && wrongFlashSide == side) color = 0xFFFF3030;
		else if (neededSide == side) color = 0xFFFFE030;

		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		graphics.pose().scale(2.0f, 2.0f, 1.0f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, Component.literal(symbol), 0, -4, color);
		graphics.pose().popPose();
	}
}
