package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.stats.techniques.Techniques;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.settings.KeyModifier;

import java.util.Locale;
import java.util.Objects;

public class TechniqueHotbarHUD {
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private static final int SLOTS = Techniques.SLOT_COUNT;
	private static final int BAR_SLOTS = 4;
	private static final int BADGE_SIZE = 11;
	private static final int GAP = 5;

	private static final float FADE_SECONDS = 0.06f;
	private static final float COOLDOWN_ALPHA = 0.55f;
	private static final float HIDDEN_PREVIEW_ALPHA = 0.35f;
	private static final long READY_LINGER_MS = 1500L;
	private static final long READY_BLINK_MS = 750L;
	private static final float READY_BLINKS = 3.0f;

	private static final int COLOR_NAME = 0xFFFFFF;
	private static final int COLOR_NAME_CD = 0x8A8A8A;
	private static final int COLOR_CD = 0xFFD200;
	private static final int COLOR_BADGE_BG = 0xB0000000;
	private static final int COLOR_BADGE_BORDER = 0x66FFFFFF;
	private static final int COLOR_BADGE_TEXT = 0xFFFFFF;

	private static final SlotView[] VIEWS = new SlotView[SLOTS];

	static {
		for (int i = 0; i < SLOTS; i++) VIEWS[i] = new SlotView();
	}

	public static final IGuiOverlay HUD_TECHNIQUES = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	public static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;

		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, mc.player).orElse(null);
		if (data == null || !data.getStatus().isHasCreatedCharacter()) return;

		boolean preview = HudLayout.isPreview();
		int openBar = preview ? 0 : openBar();
		Techniques techniques = data.getTechniques();
		String[] slots = techniques.getEquippedSlots();
		long now = System.currentTimeMillis();

		for (int i = 0; i < SLOTS; i++) {
			SlotView view = VIEWS[i];
			String id = slots[i];
			TechniqueData tech = (id == null || id.isEmpty()) ? null : techniques.getUnlockedTechniques().get(id);
			int syncedCd = tech != null ? data.getCooldowns().getCooldown("TechniqueCooldown_" + id) : 0;
			boolean cooling = syncedCd > 0;

			if (!Objects.equals(id, view.id)) view.readyAtMs = -1L;
			else if (view.cooling && !cooling) view.readyAtMs = now;
			if (cooling) view.readyAtMs = -1L;
			if (view.readyAtMs >= 0L && now - view.readyAtMs >= READY_LINGER_MS) view.readyAtMs = -1L;

			view.tech = tech;
			view.cooling = cooling;
			view.seconds = interpolateCooldownSeconds(view, id, syncedCd, now);
			view.id = id;
		}

		for (int row = 0; row < BAR_SLOTS; row++) {
			HudLayout.Box box = HudLayout.resolve(HudElement.skill(row), width, height);
			int lingering = openBar < 0 ? lingeringSlot(row) : -1;

			for (int bar = 0; bar < SLOTS / BAR_SLOTS; bar++) {
				int slot = bar * BAR_SLOTS + row;
				SlotView view = VIEWS[slot];
				float target = openBar == bar ? 1.0f : slot == lingering ? COOLDOWN_ALPHA : 0.0f;
				if (!box.visible()) target = preview && openBar == bar ? HIDDEN_PREVIEW_ALPHA : 0.0f;

				float alpha = preview ? target : view.alpha.update(target);
				if (slot == lingering && view.readyAtMs >= 0L && box.visible()) {
					long elapsed = now - view.readyAtMs;
					if (elapsed < READY_BLINK_MS) {
						float pulse = Math.abs(Mth.sin((float) Math.PI * READY_BLINKS * elapsed / READY_BLINK_MS));
						alpha = Mth.lerp(pulse, alpha, 1.0f);
					}
				}
				if (alpha <= 0.02f) continue;
				drawRow(guiGraphics, mc.font, box, slot, view, alpha, preview);
			}
		}
	}

	private static int openBar() {
		KeyModifier bar1Mod = KeyBinds.TECHNIQUE_SLOTS[0].getKeyModifier();
		KeyModifier bar2Mod = KeyBinds.TECHNIQUE_SLOTS[BAR_SLOTS].getKeyModifier();
		boolean bar1Held = KeyBinds.isBarModifierActive(bar1Mod);
		boolean bar2Held = KeyBinds.isBarModifierActive(bar2Mod);

		if (bar2Held && bar2Mod != bar1Mod) return 1;
		if (bar1Held) return 0;
		if (bar2Held) return 1;
		return -1;
	}

	private static int lingeringSlot(int row) {
		int best = -1;
		float bestSeconds = Float.MAX_VALUE;
		for (int slot = row; slot < SLOTS; slot += BAR_SLOTS) {
			SlotView view = VIEWS[slot];
			if (view.tech == null || (!view.cooling && view.readyAtMs < 0L)) continue;
			float seconds = view.cooling ? view.seconds : 0.0f;
			if (seconds < bestSeconds) {
				best = slot;
				bestSeconds = seconds;
			}
		}
		return best;
	}

	private static void drawRow(GuiGraphics guiGraphics, Font font, HudLayout.Box box, int slot, SlotView view, float alpha, boolean preview) {
		float baseWidth = box.width() / box.scale();
		float baseHeight = box.height() / box.scale();
		int textY = Math.round((baseHeight - font.lineHeight) / 2.0f);
		int badgeY = Math.round((baseHeight - BADGE_SIZE) / 2.0f);
		boolean mirrored = box.mirrored();

		String keyLabel = KeyBinds.TECHNIQUE_SLOTS[slot].getKey().getDisplayName().getString();
		MutableComponent name = view.tech != null ? techniqueName(view.tech.getName())
				: preview ? Component.translatable("gui.dragonminez.hud_editor.skill_sample", slot % BAR_SLOTS + 1).withStyle(Style.EMPTY.withFont(DMZ_FONT)) : null;
		MutableComponent cd = (view.cooling && view.seconds > 0.05f) ? styled(String.format(Locale.US, "%.1fs", view.seconds)) : null;
		int nameColor = view.cooling ? COLOR_NAME_CD : COLOR_NAME;

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(box.x(), box.y(), 0.0f);
		guiGraphics.pose().scale(box.scale(), box.scale(), 1.0f);

		int badgeX = mirrored ? Math.round(baseWidth) - BADGE_SIZE : 0;
		drawBadge(guiGraphics, font, badgeX, badgeY, keyLabel, alpha);
		if (name != null) {
			int nameWidth = font.width(name);
			int nameX = mirrored ? badgeX - GAP - nameWidth : badgeX + BADGE_SIZE + GAP;
			drawText(guiGraphics, font, name, nameX, textY, nameColor, alpha);
			if (cd != null) {
				int cdX = mirrored ? nameX - GAP - font.width(cd) : nameX + nameWidth + GAP;
				drawText(guiGraphics, font, cd, cdX, textY, COLOR_CD, alpha);
			}
		}
		guiGraphics.pose().popPose();
	}

	private static void drawBadge(GuiGraphics guiGraphics, Font font, int x, int y, String label, float alpha) {
		guiGraphics.fill(x, y, x + BADGE_SIZE, y + BADGE_SIZE, fade(COLOR_BADGE_BG, alpha));
		guiGraphics.renderOutline(x, y, BADGE_SIZE, BADGE_SIZE, fade(COLOR_BADGE_BORDER, alpha));
		MutableComponent text = styled(label);
		int textX = x + (BADGE_SIZE - font.width(text)) / 2 + 1;
		int textY = y + (BADGE_SIZE - font.lineHeight) / 2 + 1;
		drawText(guiGraphics, font, text, textX, textY, COLOR_BADGE_TEXT, alpha);
	}

	private static void drawText(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int rgb, float alpha) {
		int alphaChannel = Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f);
		if (alphaChannel <= 3) return;
		guiGraphics.drawString(font, text, x, y, (alphaChannel << 24) | (rgb & 0xFFFFFF), false);
	}

	private static int fade(int argb, float alpha) {
		int alphaChannel = Math.round(((argb >>> 24) & 0xFF) * Mth.clamp(alpha, 0.0f, 1.0f));
		return (alphaChannel << 24) | (argb & 0xFFFFFF);
	}

	private static float interpolateCooldownSeconds(SlotView view, String id, int syncedCd, long now) {
		if (!Objects.equals(id, view.id) || syncedCd != view.lastTicks) {
			view.lastTicks = syncedCd;
			view.lastUpdateMs = now;
		}
		if (syncedCd <= 0) return 0.0f;
		float elapsedSec = (now - view.lastUpdateMs) / 1000.0f;
		return Math.max(0.0f, syncedCd / 20.0f - elapsedSec);
	}

	private static MutableComponent styled(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	private static MutableComponent techniqueName(String name) {
		if (name == null || name.isEmpty()) return styled("");
		MutableComponent base = name.contains(".") ? Component.translatable(name) : Component.literal(name);
		return base.withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	private static final class SlotView {
		private final HudSmoother alpha = new HudSmoother(FADE_SECONDS, 0.01f);
		private TechniqueData tech;
		private String id;
		private boolean cooling;
		private float seconds;
		private long readyAtMs = -1L;
		private int lastTicks;
		private long lastUpdateMs;
	}
}
