package com.dragonminez.client.gui.hud;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class TechniqueChargeOverlay {
	private static final int BAR_WIDTH = 150;
	private static final int BAR_HEIGHT = 8;
	private static final float LERP_SPEED = 0.25f;
	private static volatile float currentChargePercent = 0.0f;

	public static final IGuiOverlay HUD_TECHNIQUE_CHARGE = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;

			float targetChargePercent = data.getTechniques().getTechniqueChargePercent();
			if (targetChargePercent <= 0.0f && !data.getTechniques().isTechniqueCharging()) {
				currentChargePercent = 0.0f;
				return;
			}

			TechniqueData selected = data.getTechniques().getSelectedTechnique();
			if (!(selected instanceof KiAttackData kiAttack)) return;

			int x = (width - BAR_WIDTH) / 2;
			int y = height - 72;
			int innerWidth = BAR_WIDTH - 2;
			float lerped = currentChargePercent + (targetChargePercent - currentChargePercent) * LERP_SPEED * partialTicks;
			if (Math.abs(lerped - targetChargePercent) <= 0.5f) {
				lerped = targetChargePercent;
			}
			currentChargePercent = Math.max(0.0f, Math.min(200.0f, lerped));
			float clampedCharge = currentChargePercent;
			int mainColor = 0xFF000000 | kiAttack.getColorExterior();
			int overchargeColor = 0xFF000000 | ColorUtils.darkenColor(kiAttack.getColorExterior(), 0.55f);

			guiGraphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xCC000000);
			guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x66101010);

			int halfMarkerX = x + 1 + Math.round(innerWidth * 0.5f);
			guiGraphics.fill(halfMarkerX, y - 2, halfMarkerX + 1, y + BAR_HEIGHT + 2, 0xCCFFFFFF);

			float normalFillRatio = Math.min(clampedCharge, 100.0f) / 100.0f;
			int normalFill = Math.round(innerWidth * normalFillRatio);
			if (normalFill > 0) {
				guiGraphics.fill(x + 1, y + 1, x + 1 + normalFill, y + BAR_HEIGHT - 1, mainColor);
			}

			if (clampedCharge > 100.0f) {
				float overchargeRatio = (clampedCharge - 100.0f) / 100.0f;
				int overchargeFill = Math.round(innerWidth * overchargeRatio);
				if (overchargeFill > 0) {
					guiGraphics.fill(x + 1, y + 2, x + 1 + overchargeFill, y + BAR_HEIGHT - 2, overchargeColor);
				}
			}
		});
	};
}

