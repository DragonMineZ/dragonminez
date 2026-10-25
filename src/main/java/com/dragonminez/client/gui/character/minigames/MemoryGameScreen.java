package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.Reference;
import com.dragonminez.common.training.MemoryLogic;
import com.dragonminez.common.training.MinigameEvent;
import com.dragonminez.common.training.MinigameOrigin;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public class MemoryGameScreen extends BaseMinigameScreen {
	private static final ResourceLocation SHEET = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/minigame/memory.png");
	private static final int CAP_W = 22, CAP_H = 24;
	private static final int PRESSED_U = 24;
	private static final int LETTER_IDLE = 0x2A2A34;
	private static final int LETTER_PRESSED = 0xE6E6EE;

	private final SmoothValue totalBar = new SmoothValue();
	private final SmoothValue keyBar = new SmoothValue();
	private MemoryLogic.Phase lastPhase;
	private int lastIndex;
	private int lastRemap = -1;
	private int lastLives;
	private long remapNanos;
	private long hitNanos;
	private long wrongNanos;
	private long lifeLostNanos;
	private long phaseNanos;
	private int lastWrongTick = -100;

	public MemoryGameScreen() {
		this(MinigameOrigin.MENU);
	}

	public MemoryGameScreen(MinigameOrigin origin) {
		super(MemoryLogic.ID, "gui.dragonminez.minigame.memory", origin);
	}

	private MemoryLogic memory() {
		return (MemoryLogic) logic;
	}

	@Override
	protected int mapPhysicalKey(int physical) {
		if (physical >= GLFW.GLFW_KEY_0 && physical <= GLFW.GLFW_KEY_9) return MinigameEvent.CHAR_BASE + 26 + (physical - GLFW.GLFW_KEY_0);
		if (physical >= GLFW.GLFW_KEY_KP_0 && physical <= GLFW.GLFW_KEY_KP_9) return MinigameEvent.CHAR_BASE + 26 + (physical - GLFW.GLFW_KEY_KP_0);
		int letter = layoutLetter(physical);
		return letter < 0 ? -1 : MinigameEvent.CHAR_BASE + letter;
	}

	private static int layoutLetter(int physical) {
		String name = layoutKeyName(physical);
		if (name != null && name.length() == 1) {
			char c = name.charAt(0);
			if (c >= 'A' && c <= 'Z') return c - 'A';
			if (!Character.isLetter(c)) return -1;
		}
		return physical >= GLFW.GLFW_KEY_A && physical <= GLFW.GLFW_KEY_Z ? physical - GLFW.GLFW_KEY_A : -1;
	}

	@Override
	protected void onLogicStarted() {
		MemoryLogic m = memory();
		lastPhase = m.phase();
		lastIndex = 0;
		lastLives = m.lives();
		lastRemap = -1;
		totalBar.snap(1f);
		keyBar.snap(1f);
	}

	@Override
	protected void onLogicTick() {
		checkFeedback();
	}

	@Override
	protected void onInputApplied(MinigameEvent event) {
		checkFeedback();
	}

	private void checkFeedback() {
		MemoryLogic m = memory();
		long now = System.nanoTime();
		if (m.remapCount() != lastRemap) {
			lastRemap = m.remapCount();
			remapNanos = now;
			lastIndex = 0;
			totalBar.snap(1f);
			keyBar.snap(1f);
		}
		if (m.index() > lastIndex) {
			playHit(true);
			hitNanos = now;
			lastIndex = m.index();
		}
		if (m.wrongTick() != lastWrongTick) {
			lastWrongTick = m.wrongTick();
			wrongNanos = now;
			playMiss();
		}
		if (m.phase() != lastPhase) {
			lastPhase = m.phase();
			phaseNanos = now;
		}
		if (m.lives() < lastLives) lifeLostNanos = now;
		lastLives = m.lives();
	}

	private static float seconds(long nanos) {
		return nanos == 0 ? 99f : (System.nanoTime() - nanos) / 1_000_000_000f;
	}

	@Override
	protected void renderGame(GuiGraphics graphics, float partialTick) {
		MemoryLogic m = memory();
		int[] sequence = m.sequence();
		int len = sequence.length;
		float cx = this.width / 2f;
		float cy = this.height / 2f + 6;
		float scale = Math.min(2.2f, this.width * 0.8f / (len * (CAP_W + 3)));
		float capW = CAP_W * scale, capH = CAP_H * scale;
		float gap = 3 * scale;
		float rowW = len * capW + (len - 1) * gap;
		float x0 = cx - rowW / 2f;

		float remapAge = seconds(remapNanos);
		float hitAge = seconds(hitNanos);
		float wrongAge = seconds(wrongNanos);
		float phaseAge = seconds(phaseNanos);
		boolean success = m.phase() == MemoryLogic.Phase.SUCCESS;
		boolean timeout = m.phase() == MemoryLogic.Phase.TIMEOUT;

		for (int i = 0; i < len; i++) {
			float slide = easeOutCubic((remapAge - i * 0.04f) / 0.25f);
			if (slide <= 0f) continue;
			float x = x0 + i * (capW + gap) + (1f - slide) * 40f;
			float y = cy - capH / 2f;
			boolean done = i < m.index();
			boolean current = i == m.index() && m.phase() == MemoryLogic.Phase.TYPING;
			float s = 1f;
			float dx = 0, dy = 0;
			if (current) {
				dy = -2f * scale * Math.abs((float) Math.sin(System.nanoTime() / 160_000_000.0));
				if (wrongAge < 0.3f) dx = 3f * (float) Math.exp(-wrongAge * 10) * (float) Math.sin(wrongAge * 70);
			}
			if (i == m.index() - 1 && hitAge < 0.35f) s = 1f + 0.25f * (float) Math.exp(-hitAge * 12);
			if (success) dy = -4f * scale * (float) Math.exp(-phaseAge * 6) * (float) Math.abs(Math.sin(phaseAge * 12 + i * 0.6));
			if (timeout && phaseAge < 0.5f) dx = 3f * (float) Math.exp(-phaseAge * 8) * (float) Math.sin(phaseAge * 60 + i);

			float r = 1f, g = 1f, b = 1f, a = Math.min(1f, slide * 1.5f);
			if (done || success) {
				r = 0.7f;
				b = 0.7f;
			} else if (timeout) {
				g = 0.5f;
				b = 0.5f;
			} else if (current && wrongAge < 0.3f) {
				g = 0.45f + wrongAge * 1.8f;
				b = g;
			} else if (!current) {
				r = g = b = 0.78f;
			}

			if (current) {
				float pulse = 0.55f + 0.45f * (float) Math.abs(Math.sin(System.nanoTime() / 220_000_000.0));
				outlineF(graphics, x + dx - 2, y + dy - 2, capW + 4, capH + 4 - 2, 1f, withAlpha(0xFFD700, pulse));
				graphics.flush();
			}
			boolean pressed = done || success;
			RenderSystem.setShaderColor(r, g, b, a);
			RenderSystem.enableBlend();
			float w = capW * s, h = capH * s;
			float keyX = x + dx - (w - capW) / 2f;
			float keyY = y + dy - (h - capH) / 2f;
			blitRegion(graphics, SHEET, keyX, keyY, w, h, pressed ? PRESSED_U : 0, 0, CAP_W, CAP_H);
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
			drawKeyLetter(graphics, sequence[i], keyX + w / 2f, keyY + (pressed ? 10f : 8f) * scale * s, scale * s, pressed ? LETTER_PRESSED : LETTER_IDLE, a);

			if (current && m.keyTicks() > 0) {
				float keyPct = keyBar.update(m.keyRemaining() / (float) m.keyTicks(), 18f);
				drawBarH(graphics, SHEET, x + dx, y + capH + 4, capW, keyPct, keyPct < 0.3f ? FILL_RED : FILL_GOLD);
				drawLegend(graphics, "gui.dragonminez.minigame.legend.key_time", x + dx + capW / 2f, y + capH + 15);
			}
		}

		if (m.wrongChar() >= 0 && wrongAge < 0.5f && m.phase() == MemoryLogic.Phase.TYPING) {
			int c = m.wrongChar();
			float size = 0.7f;
			float x = x0 + m.index() * (capW + gap) + capW * (1f - size) / 2f;
			float y = cy - capH / 2f - capH * size - 6 - wrongAge * 16f;
			float fade = Math.max(0.05f, 1f - wrongAge * 2f);
			RenderSystem.setShaderColor(1f, 0.4f, 0.4f, fade);
			RenderSystem.enableBlend();
			blitRegion(graphics, SHEET, x, y, capW * size, capH * size, 0, 0, CAP_W, CAP_H);
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
			drawKeyLetter(graphics, c, x + capW * size / 2f, y + 8f * scale * size, scale * size, 0xA02020, fade);
		}

		float barW = Math.max(160f, rowW);
		float totalPct = m.totalTicks() > 0 ? m.totalRemaining() / (float) m.totalTicks() : 0f;
		float shownTotal = totalBar.update(totalPct, m.isFastDraining() ? 25f : 12f);
		boolean flash = m.isFastDraining() && (System.nanoTime() / 80_000_000L) % 2 == 0;
		int fill = flash || shownTotal < 0.25f ? FILL_RED : FILL_CYAN;
		drawBarH(graphics, SHEET, cx - barW / 2f, cy + capH / 2f + 26, barW, shownTotal, fill);
		drawLegend(graphics, "gui.dragonminez.minigame.legend.time", cx, cy + capH / 2f + 38);

		drawHearts(graphics, SHEET, cx, cy - capH / 2f - 34, m.lives(), Math.max(1, m.cfg().getLives()), 1.5f, lifeLostNanos);
	}

	private void drawKeyLetter(GuiGraphics graphics, int character, float centerX, float centerY, float keyScale, int rgb, float alpha) {
		MutableComponent letter = dmz(String.valueOf(MemoryLogic.CHARACTERS.charAt(character)));
		float textScale = keyScale * 1.15f;
		int alphaBits = Math.max(8, Math.round(alpha * 255));
		graphics.pose().pushPose();
		graphics.pose().translate(centerX, centerY, 0);
		graphics.pose().scale(textScale, textScale, 1f);
		graphics.drawString(this.font, letter, -this.font.width(letter) / 2, -3, (alphaBits << 24) | rgb, false);
		graphics.pose().popPose();
	}

	private static float easeOutCubic(float t) {
		float u = 1f - Math.max(0f, Math.min(1f, t));
		return 1f - u * u * u;
	}
}
