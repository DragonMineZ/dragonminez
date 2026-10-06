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

public class AlternativeHUD {
	private static final float HP_X = 9.0f, HP_Y = 3.0f, HP_TEXT_X = 45.0f;
	private static final float KI_X = 10.0f, KI_Y = 3.0f, KI_TEXT_X = 45.5f, KI_ICON_X = 2.0f;
	private static final float STM_X = 2.0f, STM_Y = 3.0f, STM_TEXT_X = 37.5f;
	private static final float ICON_Y = 2.0f;
	private static final float HEART_X = 2.0f, BOLT_X = 76.0f;
	private static final float TEXT_Y = 3.0f;
	private static final float KI_FRAME_X = 42.0f;
	private static final float RADAR_SCALE = 1.5f;
	private static final float RADAR_ICON_Y = 4.0f;
	private static final float RELEASE_TEXT_X = 15.0f, RELEASE_TEXT_Y = 11.0f;
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

	public static final IGuiOverlay HUD_ALTERNATIVE = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	public static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;
		if (HudStyle.current() != HudStyle.COMPACT) return;
		HudSprites.CompactSkin skin = HudSprites.COMPACT;
		HudLayout.Box hpBox = HudLayout.resolve(HudElement.L2_HEALTH, width, height);
		HudLayout.Box kiBox = HudLayout.resolve(HudElement.L2_KI, width, height);
		HudLayout.Box stmBox = HudLayout.resolve(HudElement.L2_STAMINA, width, height);
		boolean preview = HudLayout.isPreview();

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
			float shake = preview ? 0.0f : SHAKE.update(0.0f);

			float seconds = HudBar.time();
			float tickTime = mc.player.tickCount + partialTicks;
			float lowHealth = HudBar.heartbeat(HP_BAR.targetFraction());
			KI_BAR.setFlowBoost(chargeGlow);

			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			try {
				if (hpBox.visible() || preview) {
					begin(guiGraphics, hpBox, Mth.sin(seconds * 61.0f) * 1.4f * shake, Mth.cos(seconds * 47.0f) * 0.9f * shake);
					int hpColor = HudPlayerState.healthColor(HP_BAR.fraction());
					sprite(guiGraphics, skin.hpFrame(), 0.0f, 0.0f, 0xFFFFFF, 1.0f);
					if (lowHealth > 0.0f) sprite(guiGraphics, skin.hpFrame(), 0.0f, 0.0f, ALERT_COLOR, lowHealth * 0.6f);
					drawBar(guiGraphics, HP_BAR, skin.hpFill(), HP_X, HP_Y, hpColor, lowHealth);
					sprite(guiGraphics, skin.heart(), HEART_X, ICON_Y, HudRender.mix(hpColor, 0xFFFFFF, lowHealth * 0.35f), 1.0f);
					HP_BAR.drawValue(guiGraphics, HP_TEXT_X, TEXT_Y, 0.5f, 0.5f, tickTime);
					guiGraphics.pose().popPose();
				}

				if (kiBox.visible() || preview) {
					begin(guiGraphics, kiBox, 0.0f, 0.0f);
					float kiFrameX = KI_FRAME_X;
					int aura = state.auraColor();
					sprite(guiGraphics, skin.kiFrame(), kiFrameX, 0.0f, 0xFFFFFF, 1.0f);
					drawBar(guiGraphics, KI_BAR, skin.kiFill(), kiFrameX + KI_X, KI_Y, aura, 0.0f);
					HudSprites.Sprite kiFill = skin.kiFill();
					KI_BAR.drawFillSweep(guiGraphics, kiFill, kiFrameX + KI_X, KI_Y, kiFill.guiWidth(), kiFill.guiHeight(), seconds * 1.1f, chargeGlow);
					if (surge > 0.0f) {
						HudRender.spritePart(guiGraphics, kiFill, kiFrameX + KI_X, KI_Y, kiFill.guiWidth(), kiFill.guiHeight(), 0.0f, SURGE_TOP, surge, 1.0f,
								HudRender.mix(aura, 0xFFFFFF, 0.75f), 0.95f, false);
					}
					sprite(guiGraphics, skin.kiIcon(), kiFrameX + KI_ICON_X, ICON_Y, HudRender.mix(aura, 0xFFFFFF, 0.2f), 1.0f);
					KI_BAR.drawValue(guiGraphics, kiFrameX + KI_TEXT_X, TEXT_Y, 0.5f, 0.5f, tickTime);

					guiGraphics.pose().pushPose();
					guiGraphics.pose().scale(RADAR_SCALE, RADAR_SCALE, 1.0f);
					HudRadar.draw(guiGraphics, HudSprites.RADAR, 0.0f, 0.0f, -1.0f, data.getCharacter().getRaceName(), RADAR_ICON_Y, release, formCharge);
					HudRender.text(guiGraphics, Math.round(release) + "%", RELEASE_TEXT_X, RELEASE_TEXT_Y, 0.5f, 0.5f, RELEASE_TEXT_COLOR, 1.0f);
					guiGraphics.pose().popPose();
					guiGraphics.pose().popPose();
				}

				if (stmBox.visible() || preview) {
					begin(guiGraphics, stmBox, 0.0f, 0.0f);
					sprite(guiGraphics, skin.stmFrame(), 0.0f, 0.0f, 0xFFFFFF, 1.0f);
					drawBar(guiGraphics, STM_BAR, skin.stmFill(), STM_X, STM_Y, STAMINA_COLOR, 0.0f);
					sprite(guiGraphics, skin.bolt(), BOLT_X, ICON_Y, STAMINA_COLOR, 1.0f);
					STM_BAR.drawValue(guiGraphics, STM_TEXT_X, TEXT_Y, 0.5f, 0.5f, tickTime);
					guiGraphics.pose().popPose();
				}
			} finally {
				HudRender.setAlphaScale(1.0f);
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			}
		});
	}

	private static void begin(GuiGraphics guiGraphics, HudLayout.Box box, float offsetX, float offsetY) {
		HudRender.setAlphaScale(box.visible() ? 1.0f : 0.35f);
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(box.x() + offsetX, box.y() + offsetY, 0.0f);
		guiGraphics.pose().scale(box.scale(), box.scale(), 1.0f);
	}

	private static void drawBar(GuiGraphics guiGraphics, HudBar bar, HudSprites.Sprite fill, float x, float y, int rgb, float pulse) {
		bar.drawFill(guiGraphics, fill, null, x, y, fill.guiWidth(), fill.guiHeight(), rgb, pulse);
	}

	private static void sprite(GuiGraphics guiGraphics, HudSprites.Sprite sprite, float x, float y, int rgb, float alpha) {
		HudRender.sprite(guiGraphics, sprite, x, y, sprite.guiWidth(), sprite.guiHeight(), rgb, alpha);
	}
}
