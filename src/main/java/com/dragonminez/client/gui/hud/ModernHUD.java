package com.dragonminez.client.gui.hud;

import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.client.render.HeadPortraitRenderer;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class ModernHUD {
	private static final float BORDER = HudSprites.DEFAULT_HP.border();
	private static final float PORTRAIT = HudSprites.DEFAULT_PORTRAIT_BACK.guiWidth() - BORDER * 2.0f;
	private static final float BARS_X = PORTRAIT + BORDER;
	private static final float HP_Y = 0.0f, HP_HEIGHT = HudSprites.DEFAULT_HP.height(), HP_WIDTH = HudSprites.DEFAULT_HP.width();
	private static final float KI_Y = HP_Y + HP_HEIGHT + BORDER, KI_HEIGHT = HudSprites.DEFAULT_KI.height(), KI_WIDTH = HudSprites.DEFAULT_KI.width();
	private static final float STM_Y = KI_Y + KI_HEIGHT + BORDER, STM_HEIGHT = HudSprites.DEFAULT_STAMINA.height(), STM_WIDTH = HudSprites.DEFAULT_STAMINA.width();
	private static final float RELEASE_HEIGHT = HudSprites.DEFAULT_RELEASE.height();
	private static final int STAMINA_COLOR = 0xF5A623;
	private static final int FORM_CHARGE_COLOR = 0xFFD84A;
	private static final int RELEASE_TEXT_COLOR = 0xFACAF7;

	private static final HudBar HP_BAR = new HudBar(HudStatNumberAnimator.StatKind.HEALTH);
	private static final HudBar KI_BAR = new HudBar(HudStatNumberAnimator.StatKind.KI);
	private static final HudBar STM_BAR = new HudBar(HudStatNumberAnimator.StatKind.STAMINA);
	private static final HudSmoother RELEASE = new HudSmoother(0.10f, 0.05f);
	private static final HudSmoother FORM_CHARGE = new HudSmoother(0.08f);
	private static final HudSmoother CHARGE_GLOW = new HudSmoother(0.15f);
	private static final HudSmoother SHAKE = new HudSmoother(0.12f);
	private static float lastHealth = -1.0f;

	public static final IGuiOverlay HUD_MODERN = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	public static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;
		if (HudStyle.current() != HudStyle.DEFAULT) return;

		HudLayout.Box box = HudLayout.resolve(HudElement.MAIN, width, height);
		if (!box.visible() && !HudLayout.isPreview()) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;

			HudPlayerState state = HudPlayerState.of(mc.player, data);
			HP_BAR.update(state.health(), state.maxHealth());
			KI_BAR.update(state.energy(), state.maxEnergy());
			STM_BAR.update(state.stamina(), state.maxStamina());
			float release = RELEASE.update(state.powerRelease());
			float formCharge = FORM_CHARGE.update(state.formCharge());
			float chargeGlow = CHARGE_GLOW.update(state.chargingKi() ? 1.0f : 0.0f);
			float surge = SurgeBarState.fraction(data);

			if (lastHealth >= 0.0f && state.health() < lastHealth) {
				float lost = (lastHealth - state.health()) / state.maxHealth();
				if (lost >= 0.02f) SHAKE.snap(Math.min(1.0f, SHAKE.value() + lost * 6.0f));
			}
			lastHealth = state.health();
			float shake = HudLayout.isPreview() ? 0.0f : SHAKE.update(0.0f);

			float seconds = (System.nanoTime() / 1_000_000L % 3_600_000L) / 1000.0f;
			float tickTime = mc.player.tickCount + partialTicks;
			float lowHealth = HP_BAR.targetFraction() < 0.25f ? 0.5f + 0.5f * Mth.sin(seconds * 7.0f) : 0.0f;

			float scale = box.scale();
			float anchorX = box.x() + BORDER * scale + Mth.sin(seconds * 61.0f) * 2.2f * shake;
			float anchorY = box.y() + BORDER * scale + Mth.cos(seconds * 47.0f) * 1.4f * shake;

			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			HudRender.setAlphaScale(box.visible() ? 1.0f : 0.35f);
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(anchorX, anchorY, 0.0f);
			guiGraphics.pose().scale(scale, scale, 1.0f);
			try {
				boolean mirrored = box.mirrored();
				float barsX = mirrored ? 0.0f : BARS_X;
				float portraitX = mirrored ? HP_WIDTH + BORDER : 0.0f;
				float valueX = mirrored ? 3.0f : BARS_X + HP_WIDTH - 3.0f;
				float valueAlign = mirrored ? 0.0f : 1.0f;
				HP_BAR.setMirrored(mirrored);
				KI_BAR.setMirrored(mirrored);
				STM_BAR.setMirrored(mirrored);

				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(portraitX, 0.0f, 0.0f);
				drawPortrait(guiGraphics, mc, state, partialTicks, release, formCharge, lowHealth, mirrored);
				guiGraphics.pose().popPose();

				HP_BAR.draw(guiGraphics, HudSprites.DEFAULT_HP, barsX, HP_Y, HP_WIDTH, HP_HEIGHT, HudPlayerState.healthColor(HP_BAR.fraction()), lowHealth);
				HP_BAR.drawValue(guiGraphics, valueX, HP_Y + 3.5f, 0.75f, valueAlign, tickTime);

				KI_BAR.draw(guiGraphics, HudSprites.DEFAULT_KI, barsX, KI_Y, KI_WIDTH, KI_HEIGHT, state.auraColor(), 0.0f);
				KI_BAR.drawSweep(guiGraphics, barsX, KI_Y, KI_WIDTH, KI_HEIGHT, seconds * 1.1f, chargeGlow);
				if (surge > 0.0f) {
					HudRender.rect(guiGraphics, mirrored ? barsX + KI_WIDTH * (1.0f - surge) : barsX, KI_Y + KI_HEIGHT * 0.70f, surge * KI_WIDTH, KI_HEIGHT * 0.30f, HudRender.argb(0.95f, HudRender.mix(state.auraColor(), 0xFFFFFF, 0.75f)));
				}
				KI_BAR.drawValue(guiGraphics, valueX, KI_Y + 2.0f, 0.75f, valueAlign, tickTime);

				STM_BAR.draw(guiGraphics, HudSprites.DEFAULT_STAMINA, barsX, STM_Y, STM_WIDTH, STM_HEIGHT, STAMINA_COLOR, 0.0f);
				STM_BAR.drawValue(guiGraphics, valueX, STM_Y + 1.5f, 0.625f, valueAlign, tickTime);
			} finally {
				guiGraphics.pose().popPose();
				HudRender.setAlphaScale(1.0f);
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			}
		});
	}

	private static void drawPortrait(GuiGraphics guiGraphics, Minecraft mc, HudPlayerState state, float partialTicks, float release, float formCharge, float lowHealth, boolean mirrored) {
		int frame = HudRender.mix(HudRender.mix(state.auraColor(), 0x000000, 0.35f), 0xFF3030, lowHealth * 0.8f);
		frame = HudRender.mix(frame, FORM_CHARGE_COLOR, formCharge);
		float outer = PORTRAIT + BORDER * 2.0f;
		HudRender.sprite(guiGraphics, HudSprites.DEFAULT_PORTRAIT_BACK, -BORDER, -BORDER, outer, outer, 0xFFFFFF, 1.0f);
		HudRender.sprite(guiGraphics, HudSprites.DEFAULT_PORTRAIT_TINT, -BORDER, -BORDER, outer, outer, frame, 1.0f);

		HeadPortraitRenderer.render(guiGraphics, mc.player, 1.0f, 1.0f, PORTRAIT - 2.0f, partialTicks, mirrored);

		float releaseY = PORTRAIT + BORDER;
		HudBar.drawStatic(guiGraphics, HudSprites.DEFAULT_RELEASE, 0.0f, releaseY, PORTRAIT, RELEASE_HEIGHT, release / 100.0f, HudRender.mix(state.auraColor(), 0xFFFFFF, 0.35f), mirrored);
		if (formCharge > 0.005f) {
			HudRender.rect(guiGraphics, mirrored ? PORTRAIT * (1.0f - formCharge) : 0.0f, releaseY + RELEASE_HEIGHT - 1.0f, formCharge * PORTRAIT, 1.0f, HudRender.argb(1.0f, FORM_CHARGE_COLOR));
		}
		HudRender.text(guiGraphics, Math.round(release) + "%", PORTRAIT / 2.0f, releaseY + RELEASE_HEIGHT + BORDER + 1.5f, 0.5f, 0.5f, RELEASE_TEXT_COLOR, 1.0f);
	}
}
