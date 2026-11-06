package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.Reference;
import com.dragonminez.common.config.TrainingConfig;
import com.dragonminez.common.training.GravityLogic;
import com.dragonminez.common.training.MinigameEvent;
import com.dragonminez.common.training.MinigameOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

public class GravityGameScreen extends BaseMinigameScreen {
	private static final ResourceLocation SHEET = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/minigame/gravity.png");

	private static final int BODY_V = 0;
	private static final int CAP_BOTTOM_V = 16;
	private static final int CAP_TOP_V = 34;
	private static final int GROUND_V = 52;
	private static final int GROUND_W = 64;
	private static final int POD_U = 80;
	private static final int FLAME_U = 104;
	private static final int FLAME_W = 10, FLAME_H = 8;
	private static final int FLAME_TICKS = 6;
	private static final int ROCKS_W = 256;
	private static final int FAR_V = 76, FAR_H = 72;
	private static final int MID_V = 150, MID_H = 60;
	private static final float FAR_PARALLAX = 0.15f;
	private static final float MID_PARALLAX = 0.35f;

	private static final int PANEL_COLOR = 0xB80A1428;
	private static final int PANEL_BORDER = 0xFF6F9AD8;
	private static final int PANEL_INNER_BORDER = 0x6030507A;

	private final SmoothValue levelBar = new SmoothValue();
	private int lastLives;
	private long lifeLostNanos;
	private int shownLevel = -1;
	private long levelFillNanos;

	public GravityGameScreen() {
		this(MinigameOrigin.MENU);
	}

	public GravityGameScreen(MinigameOrigin origin) {
		super(GravityLogic.ID, "gui.dragonminez.minigame.gravity", origin);
	}

	private GravityLogic gravity() {
		return (GravityLogic) logic;
	}

	@Override
	protected void init() {
		super.init();
		layoutField(GravityLogic.VIEW_WIDTH, GravityLogic.VIEW_HEIGHT, HUD_RESERVE + 16, 30, 4f);
		double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		double pixels = Math.floor(fieldScale * guiScale);
		if (pixels >= 1) {
			float snapped = (float) (pixels / guiScale);
			float top = HUD_RESERVE + 16f;
			float availH = Math.max(40f, this.height - top - 30f);
			fieldScale = snapped;
			fieldLeft = (float) (Math.round((this.width - GravityLogic.VIEW_WIDTH * snapped) / 2f * guiScale) / guiScale);
			fieldTop = (float) (Math.round((top + (availH - GravityLogic.VIEW_HEIGHT * snapped) / 2f) * guiScale) / guiScale);
		}
	}

	@Override
	protected int remapLogicalKey(int logical) {
		return logical == MinigameEvent.UP || logical == MinigameEvent.ACTION ? MinigameEvent.ACTION : -1;
	}

	@Override
	protected void onLogicStarted() {
		lastLives = gravity().lives();
		shownLevel = gravity().levelsCleared();
		levelBar.snap(0f);
	}

	@Override
	protected void onLogicTick() {
		GravityLogic g = gravity();
		if (g.lives() < lastLives) {
			playMiss();
			lifeLostNanos = System.nanoTime();
		}
		lastLives = g.lives();
	}

	@Override
	protected void onInputApplied(MinigameEvent event) {
		if (event.type == MinigameEvent.KEY_DOWN && event.key == MinigameEvent.ACTION) playUi(SoundEvents.FIRECHARGE_USE, 1.8f, 0.12f);
	}

	private float levelProgress(GravityLogic g, float scroll) {
		TrainingConfig.GravityConfig cfg = g.cfg();
		int perLevel = Math.max(1, cfg.getPillarsPerLevel());
		double podX = scroll + GravityLogic.PLAYER_X;
		GravityLogic.Pillar next = null;
		GravityLogic.Pillar previous = null;
		for (GravityLogic.Pillar pillar : g.pillars()) {
			if (pillar.passed) {
				if (previous == null || pillar.x > previous.x) previous = pillar;
			} else if (next == null || pillar.x < next.x) {
				next = pillar;
			}
		}
		float approach = 0f;
		if (next != null) {
			double from = previous != null ? previous.x : next.x - cfg.getBaseSpacing();
			double span = Math.max(1.0, next.x - from);
			approach = (float) Math.max(0.0, Math.min(0.9, (podX - from) / span));
		}
		return Math.min(1f, (g.passedThisLevel() + approach) / perLevel);
	}

	@Override
	protected void renderGame(GuiGraphics graphics, float partialTick) {
		GravityLogic g = gravity();
		TrainingConfig.GravityConfig cfg = g.cfg();
		int w = GravityLogic.VIEW_WIDTH;
		int h = GravityLogic.VIEW_HEIGHT;
		int groundTop = h - GravityLogic.GROUND_HEIGHT;
		float scroll = (float) (g.worldX() + (isPlaying() ? g.scrollSpeed() * partialTick : 0));
		float podY = (float) Math.max(GravityLogic.POD_HITBOX_RADIUS,
				Math.min(groundTop - GravityLogic.POD_HITBOX_RADIUS, g.podY() + (isPlaying() ? g.velocity() * partialTick : 0)));

		graphics.enableScissor((int) Math.floor(fieldLeft), (int) Math.floor(fieldTop),
				(int) Math.ceil(fieldLeft + w * fieldScale), (int) Math.ceil(fieldTop + h * fieldScale));
		pushField(graphics);
		rectF(graphics, 0, 0, w, h, PANEL_COLOR);
		graphics.flush();
		tileRocks(graphics, FAR_V, FAR_H, groundTop - FAR_H + 10, scroll * FAR_PARALLAX, w);
		tileRocks(graphics, MID_V, MID_H, groundTop - MID_H + 6, scroll * MID_PARALLAX, w);

		for (GravityLogic.Pillar pillar : g.pillars()) {
			float cx = Math.round(pillar.x - scroll);
			if (cx + GravityLogic.CAP_WIDTH < 0 || cx - GravityLogic.CAP_WIDTH > w) continue;
			drawPillar(graphics, pillar, cx, groundTop);
		}

		int start = -Math.floorMod(Math.round(scroll), GROUND_W);
		for (int x = start; x < w; x += GROUND_W) {
			blitRegion(graphics, SHEET, x, groundTop, GROUND_W, GravityLogic.GROUND_HEIGHT, 0, GROUND_V, GROUND_W, GravityLogic.GROUND_HEIGHT);
		}

		boolean blink = g.invulnerableTicks() > 0 && (g.invulnerableTicks() / 3) % 2 == 0;
		if (!blink) {
			int size = GravityLogic.POD_SPRITE_SIZE;
			int podTop = Math.round(podY) - size / 2;
			int podLeft = GravityLogic.PLAYER_X - size / 2;
			if (g.ticksSinceFlap() < FLAME_TICKS) {
				int frame = g.ticksSinceFlap() < FLAME_TICKS / 2 ? 1 : 0;
				blitRegion(graphics, SHEET, GravityLogic.PLAYER_X - FLAME_W / 2f, podTop + size - 3, FLAME_W, FLAME_H, FLAME_U + frame * 12, 0, FLAME_W, FLAME_H);
			}
			blitRegion(graphics, SHEET, podLeft, podTop, size, size, POD_U, 0, size, size);
		}
		popField(graphics);
		graphics.disableScissor();

		pushField(graphics);
		outlineF(graphics, -2, -2, w + 4, h + 4, 1f, PANEL_BORDER);
		outlineF(graphics, -1, -1, w + 2, h + 2, 1f, PANEL_INNER_BORDER);
		popField(graphics);

		if (g.levelsCleared() != shownLevel) {
			shownLevel = g.levelsCleared();
			levelFillNanos = System.nanoTime();
		}
		float fillAge = (System.nanoTime() - levelFillNanos) / 1_000_000_000f;
		float target = fillAge < 0.25f ? 1f : levelProgress(g, scroll);
		if (fillAge >= 0.25f && fillAge < 0.3f) levelBar.snap(0f);
		float shown = levelBar.update(target, 7f);
		float barW = 160;
		float barY = fieldTop + h * fieldScale + 7;
		drawBarH(graphics, SHEET, this.width / 2f - barW / 2, barY, barW, shown, FILL_GOLD);
		drawLegend(graphics, "gui.dragonminez.minigame.legend.level_progress", this.width / 2f, barY + 11);

		drawHearts(graphics, SHEET, this.width / 2f, fieldTop - 17, g.lives(), Math.max(1, cfg.getLives()), 1.3f, lifeLostNanos);
	}

	private static void tileRocks(GuiGraphics graphics, int v, int height, float y, float offset, int width) {
		int start = -Math.floorMod(Math.round(offset), ROCKS_W);
		for (int x = start; x < width; x += ROCKS_W) blitRegion(graphics, SHEET, x, y, ROCKS_W, height, 0, v, ROCKS_W, height);
	}

	private void drawPillar(GuiGraphics graphics, GravityLogic.Pillar pillar, float cx, int groundTop) {
		int bodyVariant = pillar.variant & 1;
		int capVariant = (pillar.variant >> 1) & 1;
		int bodyW = GravityLogic.BODY_WIDTH;
		int tileH = GravityLogic.BODY_TILE_HEIGHT;
		int capW = GravityLogic.CAP_WIDTH;
		int capH = GravityLogic.CAP_HEIGHT;
		int gapTop = (int) Math.round(pillar.gapTop());
		int gapBottom = (int) Math.round(pillar.gapBottom());
		float bodyX = cx - bodyW / 2f;
		float capX = cx - capW / 2f;

		for (int y = gapBottom + capH; y < groundTop; y += tileH) {
			blitRegion(graphics, SHEET, bodyX, y, bodyW, tileH, bodyVariant * bodyW, BODY_V, bodyW, tileH);
		}
		for (int y = gapTop - capH - tileH; y > -tileH; y -= tileH) {
			blitRegion(graphics, SHEET, bodyX, y, bodyW, tileH, bodyVariant * bodyW, BODY_V, bodyW, tileH);
		}
		blitRegion(graphics, SHEET, capX, gapBottom, capW, capH, capVariant * capW, CAP_BOTTOM_V, capW, capH);
		blitRegion(graphics, SHEET, capX, gapTop - capH, capW, capH, capVariant * capW, CAP_TOP_V, capW, capH);
	}
}
