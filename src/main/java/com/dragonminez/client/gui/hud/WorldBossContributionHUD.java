package com.dragonminez.client.gui.hud;

import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.client.systems.worldboss.ClientWorldBossContribution;
import com.dragonminez.client.util.NumberFormattingUtil;
import com.dragonminez.common.network.S2C.WorldBossContributionS2C;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorldBossContributionHUD {
	public static final int MAX_ROWS = 8;
	public static final float WIDTH = 150.0f;
	public static final float HEADER_HEIGHT = 14.0f;
	public static final float ROW_HEIGHT = 15.0f;
	public static final float HEIGHT = HEADER_HEIGHT + MAX_ROWS * ROW_HEIGHT;

	private static final long ALTERNATE_MILLIS = 10_000L;
	private static final float TEXT_SCALE = 0.75f;
	private static final float BAR_HEIGHT = 4.0f;
	private static final float PADDING = 4.0f;
	private static final float FRAME = 1.5f;
	private static final float PANEL_BOTTOM_PADDING = 2.0f;
	private static final float HIDDEN_PREVIEW_ALPHA = 0.35f;
	private static final float FRAME_ALPHA = 0.92f;
	private static final float FILL_ALPHA = 0.62f;
	private static final int FRAME_COLOR = 0x000000;
	private static final int FILL_TOP_COLOR = 0x23262E;
	private static final int FILL_BOTTOM_COLOR = 0x191C22;
	private static final int TRACK_COLOR = 0x000000;
	private static final int LOCAL_NAME_COLOR = 0xFFE066;
	private static final int NAME_COLOR = 0xFFFFFF;
	private static final int KO_NAME_COLOR = 0xFF7A7A;
	private static final int VALUE_COLOR = 0xE8ECF2;
	private static final int HEADER_COLOR = 0xFFD54F;
	private static final int TIMER_COLOR = 0xB8C0CC;

	private static final List<WorldBossContributionS2C.Entry> PREVIEW_ENTRIES = List.of(
			new WorldBossContributionS2C.Entry(new UUID(0L, 11L), "Goku", 12_600_000.0f, 0.46f, 0x4FC3FF, false),
			new WorldBossContributionS2C.Entry(new UUID(0L, 12L), "Vegeta", 8_100_000.0f, 0.29f, 0xFFD54F, false),
			new WorldBossContributionS2C.Entry(new UUID(0L, 13L), "Piccolo", 4_400_000.0f, 0.16f, 0x7CFDD6, true),
			new WorldBossContributionS2C.Entry(new UUID(0L, 14L), "Krillin", 2_500_000.0f, 0.09f, 0xFF8A65, false));

	private static final Map<UUID, RowView> VIEWS = new HashMap<>();
	private static final HudSmoother PANEL_ALPHA = new HudSmoother(0.18f, 0.005f);
	private static final HudSmoother PANEL_ROWS = new HudSmoother(0.16f, 0.01f);

	public static final IGuiOverlay HUD_BOSS_CONTRIBUTION = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	public static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null || mc.level == null) return;

		boolean preview = HudLayout.isPreview();
		HudLayout.Box box = HudLayout.resolve(HudElement.BOSS_CONTRIBUTION, width, height);
		if (!box.visible() && !preview) return;

		boolean active = preview || ClientWorldBossContribution.isActive();
		float panelAlpha = preview ? 1.0f : PANEL_ALPHA.update(active ? 1.0f : 0.0f);
		if (panelAlpha <= 0.01f && !active) {
			VIEWS.clear();
			return;
		}
		if (!preview && ClientWorldBossContribution.isFinished()) {
			panelAlpha *= 1.0f - Mth.clamp((ClientWorldBossContribution.lingerFraction() - 0.85f) / 0.15f, 0.0f, 1.0f);
		}

		List<WorldBossContributionS2C.Entry> entries = preview && ClientWorldBossContribution.entries().isEmpty()
				? PREVIEW_ENTRIES : ClientWorldBossContribution.entries();
		List<WorldBossContributionS2C.Entry> visible = pickVisible(entries, mc.player.getUUID());
		float best = entries.isEmpty() ? 1.0f : Math.max(1.0e-6f, entries.get(0).points());
		boolean showPercent = (System.currentTimeMillis() / ALTERNATE_MILLIS) % 2L == 0L;

		for (RowView view : VIEWS.values()) view.present = false;
		for (int slot = 0; slot < visible.size(); slot++) {
			WorldBossContributionS2C.Entry entry = visible.get(slot);
			RowView view = VIEWS.computeIfAbsent(entry.id(), id -> new RowView());
			if (view.slot.value() < 0.0f || view.appear.value() <= 0.01f) {
				view.slot.snap(slot);
				view.fill.snap(entry.points() / best);
			}
			if (preview) view.appear.snap(1.0f);
			view.present = true;
			view.entry = entry;
			view.rank = indexOf(entries, entry.id()) + 1;
			view.targetSlot = slot;
		}

		float alpha = panelAlpha * (box.visible() ? 1.0f : HIDDEN_PREVIEW_ALPHA);
		float rows = preview ? MAX_ROWS : PANEL_ROWS.update(Math.max(1, visible.size()));
		float panelHeight = HEADER_HEIGHT + rows * ROW_HEIGHT + PANEL_BOTTOM_PADDING;
		float anchorShift = (HEIGHT - panelHeight) * HudLayout.anchorY(HudElement.BOSS_CONTRIBUTION);

		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(box.x(), box.y(), 0.0f);
		guiGraphics.pose().scale(box.scale(), box.scale(), 1.0f);
		guiGraphics.pose().translate(0.0f, anchorShift, 0.0f);
		HudRender.setAlphaScale(alpha);
		try {
			drawPanel(guiGraphics, panelHeight);
			drawHeader(guiGraphics, preview);
			for (Iterator<RowView> it = VIEWS.values().iterator(); it.hasNext(); ) {
				RowView view = it.next();
				float appear = view.appear.update(view.present ? 1.0f : 0.0f);
				if (!view.present && (appear <= 0.01f || preview)) {
					it.remove();
					continue;
				}
				float row = view.slot.update(view.targetSlot);
				float fill = view.fill.update(view.entry.points() / best);
				drawRow(guiGraphics, mc, view, row, fill, appear, showPercent);
			}
		} finally {
			HudRender.setAlphaScale(1.0f);
			guiGraphics.pose().popPose();
		}
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}

	private static List<WorldBossContributionS2C.Entry> pickVisible(List<WorldBossContributionS2C.Entry> entries, UUID local) {
		List<WorldBossContributionS2C.Entry> visible = new ArrayList<>(Math.min(entries.size(), MAX_ROWS));
		for (int i = 0; i < entries.size() && i < MAX_ROWS; i++) visible.add(entries.get(i));
		int localIndex = indexOf(entries, local);
		if (localIndex >= MAX_ROWS && !visible.isEmpty()) visible.set(visible.size() - 1, entries.get(localIndex));
		return visible;
	}

	private static int indexOf(List<WorldBossContributionS2C.Entry> entries, UUID id) {
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).id().equals(id)) return i;
		}
		return -1;
	}

	private static void drawPanel(GuiGraphics guiGraphics, float panelHeight) {
		HudRender.rect(guiGraphics, 0.0f, 0.0f, WIDTH, panelHeight, HudRender.argb(FRAME_ALPHA, FRAME_COLOR));
		HudRender.rectVertical(guiGraphics, FRAME, FRAME, WIDTH - FRAME * 2.0f, panelHeight - FRAME * 2.0f,
				HudRender.argb(FILL_ALPHA, FILL_TOP_COLOR), HudRender.argb(FILL_ALPHA, FILL_BOTTOM_COLOR));
		HudRender.rect(guiGraphics, FRAME, FRAME, WIDTH - FRAME * 2.0f, HEADER_HEIGHT - FRAME - 2.0f, HudRender.argb(0.3f, TRACK_COLOR));
		HudRender.rect(guiGraphics, FRAME, HEADER_HEIGHT - 2.0f, WIDTH - FRAME * 2.0f, 1.0f, HudRender.argb(0.75f, FRAME_COLOR));
	}

	private static void drawHeader(GuiGraphics guiGraphics, boolean preview) {
		String bossKey = ClientWorldBossContribution.bossNameKey();
		Component bossName = preview && bossKey.isEmpty()
				? Component.translatable("gui.dragonminez.hud_editor.sample.boss_name")
				: Component.translatable(bossKey);
		HudRender.text(guiGraphics, bossName.getString(), PADDING, 3.5f, TEXT_SCALE, 0.0f, HEADER_COLOR, 1.0f);
		long elapsedTicks = preview && bossKey.isEmpty() ? 20L * 330L : ClientWorldBossContribution.elapsedTicks();
		HudRender.text(guiGraphics, formatTicks(elapsedTicks), WIDTH - PADDING, 3.5f, TEXT_SCALE, 1.0f, TIMER_COLOR, 1.0f);
	}

	private static void drawRow(GuiGraphics guiGraphics, Minecraft mc, RowView view, float row, float fill, float appear, boolean showPercent) {
		WorldBossContributionS2C.Entry entry = view.entry;
		float y = HEADER_HEIGHT + row * ROW_HEIGHT;
		float slide = (1.0f - appear) * 24.0f;
		boolean local = mc.player != null && entry.id().equals(mc.player.getUUID());

		float baseAlpha = HudRender.currentAlphaScale();
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(-slide, y, 0.0f);
		HudRender.setAlphaScale(baseAlpha * appear);

		String rankLabel = "[" + view.rank + "]";
		int nameColor = entry.knockedOut() ? KO_NAME_COLOR : local ? LOCAL_NAME_COLOR : NAME_COLOR;
		String name = entry.knockedOut()
				? entry.name() + " " + Component.translatable("gui.dragonminez.worldboss.contribution.knocked_out").getString()
				: entry.name();
		HudRender.text(guiGraphics, rankLabel, PADDING, 1.0f, TEXT_SCALE, 0.0f, TIMER_COLOR, 1.0f);
		float rankWidth = mc.font.width(rankLabel) * TEXT_SCALE;
		HudRender.text(guiGraphics, name, PADDING + rankWidth + 3.0f, 1.0f, TEXT_SCALE, 0.0f, nameColor, 1.0f);

		String value = showPercent
				? Math.round(entry.share() * 100.0f) + "%"
				: NumberFormattingUtil.formatLargeNumber(entry.points());
		HudRender.text(guiGraphics, value, WIDTH - PADDING, 1.0f, TEXT_SCALE, 1.0f, VALUE_COLOR, 1.0f);

		float barY = ROW_HEIGHT - BAR_HEIGHT - 2.0f;
		float barWidth = WIDTH - PADDING * 2.0f;
		HudRender.rect(guiGraphics, PADDING, barY, barWidth, BAR_HEIGHT, HudRender.argb(0.5f, TRACK_COLOR));
		float fillWidth = barWidth * Mth.clamp(fill, 0.0f, 1.0f);
		int barColor = entry.knockedOut() ? HudRender.mix(entry.auraRgb(), 0x555555, 0.6f) : entry.auraRgb();
		HudRender.rectHorizontal(guiGraphics, PADDING, barY, fillWidth, BAR_HEIGHT,
				HudRender.argb(0.95f, HudRender.mix(barColor, 0x000000, 0.25f)), HudRender.argb(0.95f, barColor));
		if (fillWidth > 1.5f) HudRender.rect(guiGraphics, PADDING + fillWidth - 1.0f, barY, 1.0f, BAR_HEIGHT, HudRender.argb(0.7f, 0xFFFFFF));

		HudRender.setAlphaScale(baseAlpha);
		guiGraphics.pose().popPose();
	}

	private static String formatTicks(long ticks) {
		long totalSeconds = Math.max(0L, ticks / 20L);
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;
		return String.format("%d:%02d", minutes, seconds);
	}

	private static final class RowView {
		private final HudSmoother appear = new HudSmoother(0.14f, 0.01f);
		private final HudSmoother slot = new HudSmoother(0.12f, 0.01f);
		private final HudSmoother fill = new HudSmoother(0.2f, 0.001f);
		private WorldBossContributionS2C.Entry entry;
		private boolean present;
		private int targetSlot;
		private int rank;

		private RowView() {
			slot.snap(-1.0f);
			appear.snap(0.0f);
		}
	}
}
