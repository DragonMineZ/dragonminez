package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.Reference;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.TrainingConfig;
import com.dragonminez.common.training.MinigameEvent;
import com.dragonminez.common.training.MinigameOrigin;
import com.dragonminez.common.training.PrecisionLogic;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PrecisionGameScreen extends BaseMinigameScreen {
	private static final ResourceLocation SHEET = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/minigame/precision.png");
	private static final int TARGET_SIZE = 32;
	private static final int PANEL_COLOR = 0xB80A1428;
	private static final int PANEL_BORDER = 0xFF6F9AD8;
	private static final int PANEL_INNER_BORDER = 0x6030507A;
	private static final float BURST_SECONDS = 0.3f;
	private static final float MISS_SECONDS = 0.5f;
	private static final int[][] RING_FRAMES = {
			{0, 48, 42}, {87, 48, 39}, {168, 48, 36},
			{0, 136, 33}, {69, 136, 30}, {132, 136, 27}, {189, 136, 24},
			{50, 206, 21}, {95, 206, 18}, {134, 206, 15}, {167, 206, 12}, {194, 206, 9}, {215, 206, 6}};

	private record Effect(float x, float y, boolean hit, long startNanos) {}

	private final Map<PrecisionLogic.Circle, Boolean> tracked = new IdentityHashMap<>();
	private final List<Effect> effects = new ArrayList<>();
	private final SmoothValue levelBar = new SmoothValue();
	private final SmoothValue scorePop = new SmoothValue();
	private int lastScore;
	private int shownLevel;
	private boolean spaceHeld;

	public PrecisionGameScreen() {
		this(MinigameOrigin.MENU);
	}

	public PrecisionGameScreen(MinigameOrigin origin) {
		super(PrecisionLogic.ID, "gui.dragonminez.minigame.precision", origin);
	}

	private PrecisionLogic precision() {
		return (PrecisionLogic) logic;
	}

	@Override
	protected void init() {
		super.init();
		layoutField(PrecisionLogic.FIELD_WIDTH, PrecisionLogic.FIELD_HEIGHT, HUD_RESERVE + 26, 22, 1.25f);
	}

	@Override
	protected boolean usesClicks() {
		return true;
	}

	@Override
	protected boolean onPlayingKey(int keyCode) {
		if (keyCode != GLFW.GLFW_KEY_SPACE) return false;
		if (!spaceHeld) {
			spaceHeld = true;
			emitClick((guiMouseX() - fieldLeft) / fieldScale, (guiMouseY() - fieldTop) / fieldScale);
		}
		return true;
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_SPACE) spaceHeld = false;
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	protected void onLogicStarted() {
		lastScore = precision().score();
		tracked.clear();
		effects.clear();
		levelBar.snap(0f);
		shownLevel = 0;
	}

	@Override
	protected void onLogicTick() {
		checkChanges();
	}

	@Override
	protected void onInputApplied(MinigameEvent event) {
		checkChanges();
	}

	private void checkChanges() {
		PrecisionLogic p = precision();
		int score = p.score();
		if (score > lastScore) playHit(score - lastScore >= p.cfg().getPerfectPoints());
		else if (score < lastScore) playMiss();
		if (score != lastScore) scorePop.snap(1f);
		lastScore = score;

		long now = System.nanoTime();
		Map<PrecisionLogic.Circle, Boolean> current = new IdentityHashMap<>();
		for (PrecisionLogic.Circle c : p.targets()) {
			current.put(c, c.fading);
			Boolean before = tracked.get(c);
			if (c.fading && (before == null || !before)) effects.add(new Effect(c.x, c.y, false, now));
		}
		for (Map.Entry<PrecisionLogic.Circle, Boolean> entry : tracked.entrySet()) {
			if (!current.containsKey(entry.getKey()) && !entry.getValue()) {
				effects.add(new Effect(entry.getKey().x, entry.getKey().y, true, now));
			}
		}
		tracked.clear();
		tracked.putAll(current);
	}

	private void drawApproachRing(GuiGraphics graphics, float x, float y, float radius, float closeness) {
		int[] best = RING_FRAMES[0];
		for (int[] frame : RING_FRAMES) if (Math.abs(frame[2] - radius) < Math.abs(best[2] - radius)) best = frame;
		int size = 2 * best[2] + 3;
		float scale = radius / best[2];
		float drawn = size * scale;
		if (closeness > 0.6f) RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		else RenderSystem.setShaderColor(1f, 1f, 1f, 0.7f + 0.3f * closeness);
		RenderSystem.enableBlend();
		blitRegion(graphics, SHEET, x - drawn / 2f, y - drawn / 2f, drawn, drawn, best[0], best[1], size, size);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
	}

	private void drawOrderNumber(GuiGraphics graphics, float x, float y, int order, boolean next) {
		MutableComponent number = dmz(String.valueOf(order));
		float scale = next ? 1.25f : 1f;
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		graphics.pose().scale(scale, scale, 1f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, number, 0, -3, next ? 0xFFFFE680 : 0xFFE0E0E0);
		graphics.pose().popPose();
	}

	@Override
	protected void renderGame(GuiGraphics graphics, float partialTick) {
		PrecisionLogic p = precision();
		TrainingConfig.PrecisionConfig cfg = p.cfg();
		int cx = this.width / 2;

		pushField(graphics);
		rectF(graphics, 0, 0, PrecisionLogic.FIELD_WIDTH, PrecisionLogic.FIELD_HEIGHT, PANEL_COLOR);
		outlineF(graphics, -2, -2, PrecisionLogic.FIELD_WIDTH + 4, PrecisionLogic.FIELD_HEIGHT + 4, 1f, PANEL_BORDER);
		outlineF(graphics, -1, -1, PrecisionLogic.FIELD_WIDTH + 2, PrecisionLogic.FIELD_HEIGHT + 2, 1f, PANEL_INNER_BORDER);
		graphics.flush();

		float speed = (float) p.ringSpeed();
		List<PrecisionLogic.Circle> active = new ArrayList<>();
		for (PrecisionLogic.Circle c : p.targets()) if (!c.fading) active.add(c);
		active.sort((a, b) -> Float.compare(a.ringRadius, b.ringRadius));
		for (int i = active.size() - 1; i >= 0; i--) {
			PrecisionLogic.Circle c = active.get(i);
			float ringR = Math.max(1f, c.ringRadius - (isPlaying() ? speed * partialTick : 0f));
			float closeness = 1f - Math.min(1f, Math.abs(ringR - cfg.getTargetRadius()) / (float) Math.max(1, cfg.getGoodWindow()));
			int variant = closeness > 0.6f ? 1 : 0;
			blitRegion(graphics, SHEET, c.x - TARGET_SIZE / 2f, c.y - TARGET_SIZE / 2f, TARGET_SIZE, TARGET_SIZE, variant * TARGET_SIZE, 0, TARGET_SIZE, TARGET_SIZE);
			drawApproachRing(graphics, c.x, c.y, ringR, closeness);
			drawOrderNumber(graphics, c.x, c.y, i + 1, i == 0);
		}

		long now = System.nanoTime();
		Iterator<Effect> it = effects.iterator();
		while (it.hasNext()) {
			Effect e = it.next();
			float age = (now - e.startNanos()) / 1_000_000_000f;
			if (e.hit()) {
				if (age >= BURST_SECONDS) {
					it.remove();
					continue;
				}
				int frame = Math.min(2, (int) (age / BURST_SECONDS * 3));
				blitRegion(graphics, SHEET, e.x() - 16, e.y() - 16, 32, 32, 64 + frame * 32, 0, 32, 32);
			} else {
				if (age >= MISS_SECONDS) {
					it.remove();
					continue;
				}
				float s = 1f + 0.4f * (float) Math.exp(-age * 12);
				float size = 14 * s;
				graphics.pose().pushPose();
				graphics.pose().translate(0, -age * 10f, 0);
				blitRegion(graphics, SHEET, e.x() - size / 2f, e.y() - size / 2f, size, size, 1, 33, 14, 14);
				graphics.pose().popPose();
			}
		}
		popField(graphics);

		float levelSpan = Math.max(1, p.nextThreshold() - p.levelStartScore());
		float target = Math.max(0f, Math.min(1f, (p.score() - p.levelStartScore()) / levelSpan));
		if (p.levelsCleared() != shownLevel) {
			shownLevel = p.levelsCleared();
			levelBar.snap(1f);
		}
		float shown = levelBar.update(target, 8f);
		float barW = 160;
		float barY = HUD_RESERVE;
		drawBarH(graphics, SHEET, cx - barW / 2f, barY, barW, shown, FILL_GOLD);
		float legendWidth = this.font.width(tr("gui.dragonminez.minigame.legend.level_progress")) * 0.75f;
		drawLegend(graphics, "gui.dragonminez.minigame.legend.level_progress", cx - barW / 2f - 6 - legendWidth / 2f, barY + 2);

		float pop = scorePop.update(0f, 9f);
		graphics.pose().pushPose();
		graphics.pose().translate(cx, HUD_RESERVE + 15, 0);
		graphics.pose().scale(1f + 0.3f * pop, 1f + 0.3f * pop, 1f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmz(p.score() + " / " + p.nextThreshold()), 0, -4, 0xFFFFFFFF);
		graphics.pose().popPose();
	}
}
