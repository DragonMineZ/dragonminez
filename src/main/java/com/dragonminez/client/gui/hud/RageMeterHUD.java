package com.dragonminez.client.gui.hud;

import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class RageMeterHUD {
	private static final int RAGE_COLOR = 0x1F8F3A;
	private static final int RAGE_READY_COLOR = 0x6BFF4A;

	private static final HudBar RAGE_BAR = new HudBar(HudStatNumberAnimator.StatKind.KI);
	private static final HudSmoother VISIBILITY = new HudSmoother(0.18f, 0.01f);
	private static final HudSmoother READY = new HudSmoother(0.15f);

	public static final IGuiOverlay HUD_RAGE_METER = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	public static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;
			boolean applicable = data.getEffects().hasEffect("mutant");

			float rage = applicable ? data.getResources().getRage() : 0.0f;
			boolean active = applicable && data.getStatus().isRageActive();
			boolean full = applicable && data.getResources().isRageFull();

			RAGE_BAR.update(rage, 100.0f);
			float visibility = HudSideMeters.update(HudElement.RAGE, VISIBILITY, applicable, rage > 0.0f || active, width, height);
			float ready = READY.update(full || active ? 1.0f : 0.0f);

			float seconds = (System.nanoTime() / 1_000_000L % 3_600_000L) / 1000.0f;
			float pulse = ready * (0.5f + 0.5f * Mth.sin(seconds * (active ? 11.0f : 6.0f)));
			int color = HudRender.mix(RAGE_COLOR, RAGE_READY_COLOR, ready * (0.35f + 0.35f * pulse));

			HudSideMeters.draw(guiGraphics, HudElement.RAGE, width, height, RAGE_BAR, HudSprites.RAGE_ICON, visibility, color, pulse);
		});
	}
}
