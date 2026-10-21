package com.dragonminez.client.gui.hud;

import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class XenoverseHUD {
	private static final float HP_FRAME_X = 31.0f, HP_FRAME_Y = 13.0f;
	private static final float HP_X = 32.0f, HP_Y = 15.0f;
	private static final float KI_FRAME_X = 28.0f, KI_FRAME_Y = 21.0f;
	private static final float KI_X = 29.0f, KI_Y = 23.0f;
	private static final float STM_FRAME_X = 28.0f, STM_FRAME_Y = 28.0f;
	private static final float STM_X = 43.0f, STM_Y = 29.0f;
	private static final float RADAR_X = 8.0f, RADAR_Y = 8.0f;
	private static final float ICON_Y = 5.0f;
	private static final float SURGE_TOP = 0.55f;
	private static final int STAMINA_COLOR = 0xF5A623;
	private static final int RELEASE_TEXT_COLOR = 0xFACAF7;
	private static final int ALERT_COLOR = 0xFF3B3B;

	private static final HudBar HP_BAR = new HudBar(HudStatNumberAnimator.StatKind.HEALTH);
	private static final HudBar KI_BAR = new HudBar(HudStatNumberAnimator.StatKind.KI).flowing();
	private static final HudBar STM_BAR = new HudBar(HudStatNumberAnimator.StatKind.STAMINA);
	private static final HudSmoother RELEASE = new HudSmoother(0.10f, 0.05f);
	private static final HudSmoother FORM_CHARGE = new HudSmoother(0.08f);
	private static final HudSmoother CHARGE_GLOW = new HudSmoother(0.15f);
	private static final HudSmoother SHAKE = new HudSmoother(0.12f);
	private static float lastHealth = -1.0f;
	private static float mirrorWidth = -1.0f;

	public static final IGuiOverlay HUD_XENOVERSE = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	public static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;
		if (HudStyle.current() != HudStyle.XENOVERSE) return;
		HudLayout.Box box = HudLayout.resolve(HudElement.MAIN, width, height);
		if (!box.visible() && !HudLayout.isPreview()) return;
		HudSprites.XenoverseSkin skin = HudSprites.XENOVERSE;

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

			float seconds = HudBar.time();
			float tickTime = mc.player.tickCount + partialTicks;
			float lowHealth = HudBar.heartbeat(HP_BAR.targetFraction());
			KI_BAR.setFlowBoost(chargeGlow);

			float scale = box.scale();
			mirrorWidth = box.mirrored() ? box.width() / scale : -1.0f;
			boolean mirrored = mirrorWidth >= 0.0f;
			HP_BAR.setMirrored(mirrored);
			KI_BAR.setMirrored(mirrored);
			STM_BAR.setMirrored(mirrored);

			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			HudRender.setAlphaScale(box.visible() ? 1.0f : 0.35f);
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(box.x() + Mth.sin(seconds * 61.0f) * 1.6f * shake, box.y() + Mth.cos(seconds * 47.0f) * 1.0f * shake, 0.0f);
			guiGraphics.pose().scale(scale, scale, 1.0f);
			try {
				part(guiGraphics, skin.cloud(), 0.0f, 0.0f, 0xFFFFFF, 1.0f);

				part(guiGraphics, skin.hpFrame(), HP_FRAME_X, HP_FRAME_Y, 0xFFFFFF, 1.0f);
				if (lowHealth > 0.0f) part(guiGraphics, skin.hpFrame(), HP_FRAME_X, HP_FRAME_Y, ALERT_COLOR, lowHealth * 0.6f);
				drawBar(guiGraphics, HP_BAR, skin.hpFill(), HP_X, HP_Y, HudPlayerState.healthColor(HP_BAR.fraction()), lowHealth);

				part(guiGraphics, skin.kiFrame(), KI_FRAME_X, KI_FRAME_Y, 0xFFFFFF, 1.0f);
				drawBar(guiGraphics, KI_BAR, skin.kiFill(), KI_X, KI_Y, state.auraColor(), 0.0f);
				HudSprites.Sprite kiFill = skin.kiFill();
				float kiX = mx(KI_X, kiFill.guiWidth());
				KI_BAR.drawFillSweep(guiGraphics, kiFill, kiX, KI_Y, kiFill.guiWidth(), kiFill.guiHeight(), seconds * 1.1f, chargeGlow);
				if (surge > 0.0f) {
					HudRender.spritePart(guiGraphics, kiFill, kiX, KI_Y, kiFill.guiWidth(), kiFill.guiHeight(), 0.0f, SURGE_TOP, surge, 1.0f,
							HudRender.mix(state.auraColor(), 0xFFFFFF, 0.75f), 0.95f, mirrored);
				}

				HudSprites.Sprite label = skin.stmLabel();
				HudRender.sprite(guiGraphics, label, mx(STM_FRAME_X, label.guiWidth()), STM_FRAME_Y, label.guiWidth(), label.guiHeight(), 0xFFFFFF, 1.0f, false);
				part(guiGraphics, skin.stmFrame(), STM_FRAME_X + label.guiWidth(), STM_FRAME_Y, 0xFFFFFF, 1.0f);
				drawBar(guiGraphics, STM_BAR, skin.stmFill(), STM_X, STM_Y, STAMINA_COLOR, 0.0f);

				HudRadar.draw(guiGraphics, HudSprites.RADAR, RADAR_X, RADAR_Y, mirrorWidth, data.getCharacter().getRaceName(),
						"majin".equalsIgnoreCase(data.getCharacter().getRaceName()) ? ICON_Y - 1.0f : ICON_Y, release, formCharge);
				HudRender.text(guiGraphics, Math.round(release) + "%", mirrorX(7.0f), 32.0f, 0.5f, 0.5f, RELEASE_TEXT_COLOR, 1.0f);

				HP_BAR.drawValue(guiGraphics, mirrorX(100.0f), 15.5f, 0.5f, 0.5f, tickTime);
				KI_BAR.drawValue(guiGraphics, mirrorX(90.0f), 23.0f, 0.5f, 0.5f, tickTime);
				STM_BAR.drawValue(guiGraphics, mirrorX(85.0f), 29.5f, 0.5f, 0.5f, tickTime);
			} finally {
				guiGraphics.pose().popPose();
				HudRender.setAlphaScale(1.0f);
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
				mirrorWidth = -1.0f;
			}
		});
	}

	private static void drawBar(GuiGraphics guiGraphics, HudBar bar, HudSprites.Sprite fill, float x, float y, int rgb, float pulse) {
		bar.drawFill(guiGraphics, fill, null, mx(x, fill.guiWidth()), y, fill.guiWidth(), fill.guiHeight(), rgb, pulse);
	}

	private static void part(GuiGraphics guiGraphics, HudSprites.Sprite sprite, float x, float y, int rgb, float alpha) {
		float width = sprite.guiWidth();
		HudRender.sprite(guiGraphics, sprite, mx(x, width), y, width, sprite.guiHeight(), rgb, alpha, mirrorWidth >= 0.0f);
	}

	private static float mx(float x, float width) {
		return mirrorWidth < 0.0f ? x : mirrorWidth - x - width;
	}

	private static float mirrorX(float x) {
		return mirrorWidth < 0.0f ? x : mirrorWidth - x;
	}
}
