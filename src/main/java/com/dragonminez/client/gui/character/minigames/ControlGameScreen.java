package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.training.KiControlLogic;
import com.dragonminez.common.training.MinigameEvent;
import com.dragonminez.common.training.MinigameOrigin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class ControlGameScreen extends BaseMinigameScreen {
	private static final ResourceLocation SHEET = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/minigame/control.png");
	private static final float FIELD_WIDTH = 140f;
	private static final float TRACK_X = 36f;
	private static final int TRACK_W = 40;
	private static final int TRACK_CAP = 6;
	private static final int TRACK_TILE = 8;
	private static final int KI_BAR_W = 26;
	private static final int ORB_SIZE = 16;
	private static final float METER_X = 94f;
	private static final float ICON_X = 12f;

	private final SmoothValue meter = new SmoothValue();
	private final SmoothValue insideGlow = new SmoothValue();
	private int heldCount;
	private int shownLevel;

	public ControlGameScreen() {
		this(MinigameOrigin.MENU);
	}

	public ControlGameScreen(MinigameOrigin origin) {
		super(KiControlLogic.ID, "gui.dragonminez.minigame.control", origin);
	}

	private KiControlLogic control() {
		return (KiControlLogic) logic;
	}

	@Override
	protected void init() {
		super.init();
		float trackHeight = ConfigManager.getTrainingConfig().getControl().getTrackHeight();
		layoutField(FIELD_WIDTH, Math.max(60, trackHeight) + 8, HUD_RESERVE + 10, 26, 1.4f);
	}

	@Override
	protected int remapLogicalKey(int logical) {
		return logical == MinigameEvent.UP || logical == MinigameEvent.ACTION ? MinigameEvent.ACTION : -1;
	}

	@Override
	protected void onLogicStarted() {
		meter.snap((float) control().progress());
		shownLevel = 0;
	}

	@Override
	protected void onInputApplied(MinigameEvent event) {
		if (event.key != MinigameEvent.ACTION) return;
		heldCount = event.type == MinigameEvent.KEY_DOWN ? 1 : 0;
	}

	@Override
	protected void renderGame(GuiGraphics graphics, float partialTick) {
		KiControlLogic c = control();
		float h = (float) c.trackHeight();
		float barH = (float) c.barHeight();
		float barY = (float) Math.max(0, Math.min(h - barH, c.barY() + (isPlaying() ? c.barVelocity() * partialTick : 0)));
		float orbY = (float) Math.max(0, Math.min(h, c.orbY() + (isPlaying() ? c.orbVelocity() * partialTick : 0)));
		float glow = insideGlow.update(c.isInside() ? 1f : 0f, 10f);

		pushField(graphics);
		graphics.pose().translate(0, 4, 0);
		blitRegion(graphics, SHEET, TRACK_X, -4, TRACK_W, TRACK_CAP, 0, 0, TRACK_W, TRACK_CAP);
		for (float y = -4 + TRACK_CAP; y < h + 4 - TRACK_CAP; y += TRACK_TILE) {
			float tile = Math.min(TRACK_TILE, h + 4 - TRACK_CAP - y);
			blitRegion(graphics, SHEET, TRACK_X, y, TRACK_W, tile, 0, TRACK_CAP, TRACK_W, tile);
		}
		blitRegion(graphics, SHEET, TRACK_X, h + 4 - TRACK_CAP, TRACK_W, TRACK_CAP, 0, TRACK_CAP + TRACK_TILE, TRACK_W, TRACK_CAP);

		float barX = TRACK_X + (TRACK_W - KI_BAR_W) / 2f;
		float barTop = h - barY - barH;
		int u = glow > 0.5f ? 0 : 28;
		if (glow > 0.05f) {
			rectF(graphics, barX - 2, barTop - 2, KI_BAR_W + 4, barH + 4, withAlpha(0x7CFDD6, 0.25f * glow));
			graphics.flush();
		}
		blitRegion(graphics, SHEET, barX, barTop, KI_BAR_W, 4, u, 24, KI_BAR_W, 4);
		blitRegion(graphics, SHEET, barX, barTop + 4, KI_BAR_W, barH - 8, u, 28, KI_BAR_W, 4);
		blitRegion(graphics, SHEET, barX, barTop + barH - 4, KI_BAR_W, 4, u, 32, KI_BAR_W, 4);

		float orbScreenY = h - orbY;
		float pulse = 1f + 0.08f * (float) Math.sin(System.nanoTime() / 90_000_000.0);
		float orbSize = ORB_SIZE * pulse;
		float orbCx = TRACK_X + TRACK_W / 2f;
		blitRegion(graphics, SHEET, orbCx - orbSize / 2f, orbScreenY - orbSize / 2f, orbSize, orbSize, c.isDarting() ? 16 : 0, 40, ORB_SIZE, ORB_SIZE);

		boolean held = heldCount > 0;
		float iconScale = held ? 1.6f : 1.4f;
		blitRegion(graphics, SHEET, ICON_X - 11 * iconScale / 2f + 5, h / 2f - 11 * iconScale / 2f + (held ? -2 : 0), 11 * iconScale, 11 * iconScale, held ? 14 : 0, 60, 11, 11);

		if (c.levelsCleared() != shownLevel) {
			shownLevel = c.levelsCleared();
			meter.snap(1f);
		}
		float shown = meter.update((float) Math.max(0, Math.min(1, c.progress())), 9f);
		int fill = shown > 0.6f ? FILL_GREEN : shown > 0.3f ? FILL_GOLD : FILL_RED;
		drawBarV(graphics, SHEET, METER_X, -4, h + 8, shown, fill);
		popField(graphics);
		drawLegend(graphics, "gui.dragonminez.minigame.legend.focus", fieldLeft + (METER_X + 4.5f) * fieldScale, fieldTop - 6);
	}
}
