package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.gui.tutorial.TutorialManager;
import com.dragonminez.client.util.PanelSkin;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RhythmCalibrationScreen extends Screen {
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final int BUTTON_W = 74, BUTTON_H = 20;
	private static final int WARMUP_TICKS = 4;
	private static final int SAMPLES_NEEDED = 12;
	private static final long INTERVAL_NANOS = 500_000_000L;
	private static final long LEAD_NANOS = 1_200_000_000L;

	private final Screen parent;
	private final boolean mandatory;
	private final List<Integer> samples = new ArrayList<>();
	private long nextTickNanos;
	private long lastTickNanos;
	private int ticksPlayed;
	private boolean finished;
	private int suggested;
	private long finishedNanos;
	private float[] applyRect, retryRect, backRect;

	public RhythmCalibrationScreen(Screen parent) {
		this(parent, false);
	}

	public RhythmCalibrationScreen(Screen parent, boolean mandatory) {
		super(Component.translatable("gui.dragonminez.minigame.rhythm.calibration.title").withStyle(Style.EMPTY.withFont(DMZ_FONT)));
		this.parent = parent;
		this.mandatory = mandatory;
	}

	@Override
	protected void init() {
		super.init();
		restart();
	}

	private void restart() {
		samples.clear();
		ticksPlayed = 0;
		finished = false;
		lastTickNanos = 0;
		nextTickNanos = System.nanoTime() + LEAD_NANOS;
	}

	private static int currentOffset() {
		Integer value = ConfigManager.getUserConfig().getRhythmAudioOffsetMs();
		return value == null ? 0 : value;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		long now = System.nanoTime();
		if (!finished) {
			while (now >= nextTickNanos) {
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 1.0f, 1.0f));
				lastTickNanos = System.nanoTime();
				ticksPlayed++;
				nextTickNanos += INTERVAL_NANOS;
			}
		}

		int cx = this.width / 2, cy = this.height / 2;
		int contentW = 250, contentH = mandatory ? 132 : 118;
		drawPanel(graphics, cx, cy, contentW, contentH);
		int top = cy - contentH / 2;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, this.getTitle(), cx, top, 0xFFFFD700);
		int y = top + 14;
		if (mandatory) {
			for (FormattedCharSequence line : this.font.split(tr("gui.dragonminez.minigame.rhythm.calibration.first_time"), contentW)) {
				TextUtil.drawCenteredStringWithBorder(graphics, this.font, line, cx, y, 0xFF7CFDD6);
				y += 10;
			}
			y += 4;
		}
		for (FormattedCharSequence line : this.font.split(tr("gui.dragonminez.minigame.rhythm.calibration.howto"), contentW)) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, line, cx, y, 0xFFB0B0B0);
			y += 10;
		}
		y += 6;
		if (!finished) {
			boolean listening = ticksPlayed <= WARMUP_TICKS;
			Component status = listening ? tr("gui.dragonminez.minigame.rhythm.calibration.listen")
					: tr("gui.dragonminez.minigame.rhythm.calibration.progress", samples.size(), SAMPLES_NEEDED);
			float sinceTick = lastTickNanos == 0 ? 1f : (now - lastTickNanos) / 1_000_000_000f;
			float pulse = 1f + 0.25f * (float) Math.exp(-sinceTick * 10.0);
			graphics.pose().pushPose();
			graphics.pose().translate(cx, y + 4, 0);
			graphics.pose().scale(pulse, pulse, 1f);
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, status, 0, -4, listening ? 0xFFD2D7F1 : 0xFFFFFFFF);
			graphics.pose().popPose();
			y += 14;
			if (samples.size() >= 3) {
				TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.rhythm.calibration.live", median()), cx, y, 0xFF909AC3);
			}
			y += 12;
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.rhythm.calibration.current", currentOffset()), cx, y, 0xFF808898);
			applyRect = null;
			retryRect = null;
			backRect = null;
			if (!mandatory) {
				backRect = new float[]{cx - BUTTON_W / 2f, top + contentH - BUTTON_H, BUTTON_W, BUTTON_H};
				drawButton(graphics, backRect, tr("gui.dragonminez.minigame.rhythm.calibration.back"), mouseX, mouseY);
			}
		} else {
			float appear = Math.min(1f, (now - finishedNanos) / 350_000_000f);
			int color = suggested == currentOffset() ? 0xFFD2D7F1 : 0xFF7CFD8A;
			graphics.pose().pushPose();
			graphics.pose().translate(cx, y + 6, 0);
			float scale = 1.4f + 0.3f * (1f - appear);
			graphics.pose().scale(scale, scale, 1f);
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.rhythm.calibration.result", suggested), 0, -4, color);
			graphics.pose().popPose();
			y += 18;
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame.rhythm.calibration.current", currentOffset()), cx, y, 0xFF808898);
			float buttonY = top + contentH - BUTTON_H;
			if (mandatory) {
				applyRect = new float[]{cx - BUTTON_W - 3, buttonY, BUTTON_W, BUTTON_H};
				retryRect = new float[]{cx + 3, buttonY, BUTTON_W, BUTTON_H};
				backRect = null;
			} else {
				applyRect = new float[]{cx - BUTTON_W * 1.5f - 6, buttonY, BUTTON_W, BUTTON_H};
				retryRect = new float[]{cx - BUTTON_W / 2f, buttonY, BUTTON_W, BUTTON_H};
				backRect = new float[]{cx + BUTTON_W / 2f + 6, buttonY, BUTTON_W, BUTTON_H};
				drawButton(graphics, backRect, tr("gui.dragonminez.minigame.rhythm.calibration.back"), mouseX, mouseY);
			}
			drawButton(graphics, applyRect, tr("gui.dragonminez.minigame.rhythm.calibration.apply"), mouseX, mouseY);
			drawButton(graphics, retryRect, tr("gui.dragonminez.minigame.rhythm.calibration.retry"), mouseX, mouseY);
		}
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private int median() {
		List<Integer> sorted = new ArrayList<>(samples);
		Collections.sort(sorted);
		return sorted.get(sorted.size() / 2);
	}

	private void registerTap() {
		if (finished || lastTickNanos == 0) return;
		long now = System.nanoTime();
		long sinceLast = now - lastTickNanos;
		long untilNext = nextTickNanos - now;
		long delta = sinceLast <= untilNext ? sinceLast : -untilNext;
		if (ticksPlayed <= WARMUP_TICKS) return;
		samples.add((int) Math.max(-250, Math.min(400, delta / 1_000_000L)));
		if (samples.size() >= SAMPLES_NEEDED) {
			finished = true;
			finishedNanos = now;
			suggested = Math.max(-200, Math.min(400, median()));
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0f, 0.5f));
		}
	}

	private void apply() {
		ConfigManager.getUserConfig().setRhythmAudioOffsetMs(suggested);
		ConfigManager.saveGeneralUserConfig();
		TutorialManager.markSeen(TutorialManager.RHYTHM_CALIBRATION);
		close();
	}

	private void close() {
		Minecraft.getInstance().setScreen(parent);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			if (!mandatory) close();
			return true;
		}
		if (finished) {
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) apply();
			return true;
		}
		registerTap();
		return true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (inside(backRect, mouseX, mouseY)) {
			close();
			return true;
		}
		if (finished) {
			if (inside(applyRect, mouseX, mouseY)) apply();
			else if (inside(retryRect, mouseX, mouseY)) restart();
			return true;
		}
		registerTap();
		return true;
	}

	private static boolean inside(float[] rect, double x, double y) {
		return rect != null && x >= rect[0] && x <= rect[0] + rect[2] && y >= rect[1] && y <= rect[1] + rect[3];
	}

	private void drawPanel(GuiGraphics graphics, int cx, int cy, int contentW, int contentH) {
		int padding = 16;
		int panelW = contentW + padding * 2;
		int panelH = contentH + padding * 2;
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		PanelSkin.NPC_PANEL.draw(graphics, cx - panelW / 2f, cy - panelH / 2f, panelW, panelH);
	}

	private void drawButton(GuiGraphics graphics, float[] rect, Component label, int mouseX, int mouseY) {
		boolean hover = inside(rect, mouseX, mouseY);
		RenderSystem.enableBlend();
		HudRender.blit(graphics, BUTTONS, rect[0], rect[1], 0, hover ? 48 : 28, BUTTON_W, BUTTON_H, 256, 256);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, label, (int) (rect[0] + rect[2] / 2f), (int) (rect[1] + 6), hover ? 0xFFFFD700 : 0xFFFFFFFF);
	}

	private static MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return !mandatory;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
