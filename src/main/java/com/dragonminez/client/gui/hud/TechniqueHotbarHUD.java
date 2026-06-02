package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
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
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class TechniqueHotbarHUD {
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private static final int SLOTS = 5;
	private static final int ROW_HEIGHT = 13;
	private static final int BADGE_SIZE = 11;
	private static final int GAP = 5;
	private static final int MARGIN_X = 12;
	private static final int MARGIN_BOTTOM = 34;

	private static final int COLOR_NAME = 0xFFFFFFFF;
	private static final int COLOR_NAME_CD = 0xFF8A8A8A;
	private static final int COLOR_CD = 0xFFFFD200;
	private static final int COLOR_BADGE_BG = 0xB0000000;
	private static final int COLOR_BADGE_BORDER = 0x66FFFFFF;
	private static final int COLOR_BADGE_TEXT = 0xFFFFFFFF;

	private static final int[] cdLastTicks = new int[SLOTS];
	private static final long[] cdLastUpdateMs = new long[SLOTS];
	private static final String[] cdLastId = new String[SLOTS];

	public static final IGuiOverlay HUD_TECHNIQUES = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;
		if (!KeyBinds.SECOND_FUNCTION_KEY.isDown()) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;

			Techniques techniques = data.getTechniques();
			String[] slots = techniques.getEquippedSlots();
			boolean rightSide = ConfigManager.getUserConfig().getTechniqueHotbarRightSide();
			Font font = mc.font;

			int totalHeight = ROW_HEIGHT * SLOTS;
			int startY = height - MARGIN_BOTTOM - totalHeight;

			for (int i = 0; i < SLOTS; i++) {
				int rowY = startY + i * ROW_HEIGHT;
				int textY = rowY + (ROW_HEIGHT - font.lineHeight) / 2;
				int badgeY = rowY + (ROW_HEIGHT - BADGE_SIZE) / 2;

				String keyLabel = KeyBinds.TECHNIQUE_SLOTS[i].getKey().getDisplayName().getString();

				String id = slots[i];
				TechniqueData tech = (id == null || id.isEmpty()) ? null : techniques.getUnlockedTechniques().get(id);

				int syncedCd = tech != null ? data.getCooldowns().getCooldown("TechniqueCooldown_" + id) : 0;
				boolean onCooldown = syncedCd > 0;
				float displaySeconds = interpolateCooldownSeconds(i, id, syncedCd);

				MutableComponent name = tech != null ? techniqueName(tech.getName()) : null;
				MutableComponent cd = (onCooldown && displaySeconds > 0.05f) ? styled(String.format(java.util.Locale.US, "%.1fs", displaySeconds)) : null;
				int nameWidth = name != null ? font.width(name) : 0;
				int cdWidth = cd != null ? font.width(cd) : 0;

				if (!rightSide) {
					int badgeX = MARGIN_X;
					drawBadge(guiGraphics, font, badgeX, badgeY, keyLabel);
					int textX = badgeX + BADGE_SIZE + GAP;
					if (name != null) {
						guiGraphics.drawString(font, name, textX, textY, onCooldown ? COLOR_NAME_CD : COLOR_NAME, false);
						if (cd != null) {
							guiGraphics.drawString(font, cd, textX + nameWidth + GAP, textY, COLOR_CD, false);
						}
					}
				} else {
					int badgeX = width - MARGIN_X - BADGE_SIZE;
					drawBadge(guiGraphics, font, badgeX, badgeY, keyLabel);
					if (name != null) {
						int nameX = badgeX - GAP - nameWidth;
						guiGraphics.drawString(font, name, nameX, textY, onCooldown ? COLOR_NAME_CD : COLOR_NAME, false);
						if (cd != null) {
							guiGraphics.drawString(font, cd, nameX - GAP - cdWidth, textY, COLOR_CD, false);
						}
					}
				}
			}
		});
	};

	private static void drawBadge(GuiGraphics guiGraphics, Font font, int x, int y, String label) {
		guiGraphics.fill(x, y, x + BADGE_SIZE, y + BADGE_SIZE, COLOR_BADGE_BG);
		guiGraphics.renderOutline(x, y, BADGE_SIZE, BADGE_SIZE, COLOR_BADGE_BORDER);
		MutableComponent text = styled(label);
		int textWidth = font.width(text);
		int textX = x + (BADGE_SIZE - textWidth) / 2 + 1;
		int textY = y + (BADGE_SIZE - font.lineHeight) / 2 + 1;
		guiGraphics.drawString(font, text, textX, textY, COLOR_BADGE_TEXT, false);
	}

	private static float interpolateCooldownSeconds(int slot, String id, int syncedCd) {
		long now = System.currentTimeMillis();
		if (!java.util.Objects.equals(id, cdLastId[slot]) || syncedCd != cdLastTicks[slot]) {
			cdLastId[slot] = id;
			cdLastTicks[slot] = syncedCd;
			cdLastUpdateMs[slot] = now;
		}
		if (syncedCd <= 0) return 0.0f;
		float elapsedSec = (now - cdLastUpdateMs[slot]) / 1000.0f;
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
}