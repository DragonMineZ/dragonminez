package com.dragonminez.client.gui.hud;

import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.client.render.CameraProjectionCapture;
import com.dragonminez.client.systems.kisense.CombatIndicators;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import com.dragonminez.client.systems.kisense.KiSenseState;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KiSenseOverlay {
	private static final float BORDER = HudSprites.DEFAULT_HP.border();
	private static final float WIDTH = HudSprites.DEFAULT_HP.width();
	private static final float HP_HEIGHT = HudSprites.DEFAULT_HP.height();
	private static final float KI_Y = HP_HEIGHT + BORDER, KI_HEIGHT = HudSprites.DEFAULT_KI.height();
	private static final float STM_Y = KI_Y + KI_HEIGHT + BORDER, STM_HEIGHT = HudSprites.DEFAULT_STAMINA.height();
	private static final float PLAYER_BOTTOM = STM_Y + STM_HEIGHT + BORDER;
	private static final float NPC_BOTTOM = HP_HEIGHT + BORDER;
	private static final float PANEL_WIDTH = WIDTH + BORDER * 2.0f;
	private static final float WORLD_WIDTH = 2.2f;
	private static final float MIN_SCALE = 0.42f;
	private static final float MAX_SCALE = 0.7f;
	private static final double ANCHOR_ABOVE = 0.6;
	private static final float SCREEN_PAD = 40.0f;
	private static final float BP_SCALE = 0.85f;
	private static final float INDICATOR_SCALE = 1.25f;
	private static final float INDICATOR_SPREAD = 1.5f;
	private static final float POPUP_RISE = 15.0f;
	private static final int STAMINA_COLOR = 0xF5A623;
	private static final int BP_COLOR = 0xFFFFFF;
	private static final int DAMAGE_COLOR = 0xFF5555;
	private static final int HEAL_COLOR = 0x55FF55;

	private static final Map<Integer, View> VIEWS = new HashMap<>();

	public static final IGuiOverlay HUD_KI_SENSE = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	private static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			VIEWS.clear();
			return;
		}
		if (mc.options.renderDebug || mc.options.hideGui || !CameraProjectionCapture.isAvailable()) return;

		Set<Integer> combat = KiSenseState.isCombat() ? KiSenseScan.getCombatEntities() : Collections.emptySet();
		for (View view : VIEWS.values()) view.present = false;
		for (int id : combat) VIEWS.computeIfAbsent(id, View::new).present = true;
		if (VIEWS.isEmpty()) return;

		float unit = HudLayout.unit(null, height);
		float pad = SCREEN_PAD * unit;
		float[] anchor = new float[4];
		float[] above = new float[4];
		List<View> visible = new ArrayList<>();

		for (Iterator<View> it = VIEWS.values().iterator(); it.hasNext(); ) {
			View view = it.next();
			view.appearValue = view.appear.update(view.present ? 1.0f : 0.0f);
			Entity entity = mc.level.getEntity(view.id);
			if (!(entity instanceof LivingEntity living) || entity == mc.player || entity.isRemoved() || (!view.present && view.appearValue <= 0.01f)) {
				it.remove();
				continue;
			}

			Vec3 position = living.getPosition(partialTicks);
			Vec3 top = new Vec3(position.x, position.y + living.getBbHeight() + ANCHOR_ABOVE, position.z);
			if (!CameraProjectionCapture.project(top, width, height, anchor)) continue;
			if (!CameraProjectionCapture.project(top.add(0.0, 1.0, 0.0), width, height, above)) continue;
			if (anchor[0] < -pad || anchor[0] > width + pad || anchor[1] < -pad || anchor[1] > height + pad) continue;

			float pixelsPerBlock = Math.abs(anchor[1] - above[1]);
			view.entity = living;
			view.screenX = anchor[0];
			view.screenY = anchor[1];
			view.scale = Mth.clamp(WORLD_WIDTH * pixelsPerBlock / PANEL_WIDTH, MIN_SCALE * unit, MAX_SCALE * unit);
			view.distance = mc.player.distanceToSqr(living);
			visible.add(view);
		}
		if (visible.isEmpty()) return;
		visible.sort((a, b) -> Double.compare(b.distance, a.distance));

		float tickTime = mc.player.tickCount + partialTicks;
		float seconds = HudBar.time();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		for (View view : visible) drawView(guiGraphics, mc, view, tickTime, seconds);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}

	private static void drawView(GuiGraphics guiGraphics, Minecraft mc, View view, float tickTime, float seconds) {
		LivingEntity entity = view.entity;
		StatsData data = entity instanceof Player target ? StatsProvider.get(StatsCapability.INSTANCE, target).orElse(null) : null;
		boolean full = data != null && data.getStatus().isHasCreatedCharacter();
		float bottom = full ? PLAYER_BOTTOM : NPC_BOTTOM;

		view.health.update(entity.getHealth(), entity.getMaxHealth());
		float lowHealth = HudBar.heartbeat(view.health.targetFraction());

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(view.screenX, view.screenY, 0.0f);
		guiGraphics.pose().scale(view.scale, view.scale, 1.0f);
		guiGraphics.pose().translate(-WIDTH / 2.0f, -bottom, 0.0f);
		HudRender.setAlphaScale(view.appearValue);
		try {
			view.health.draw(guiGraphics, full ? HudSprites.DEFAULT_HP : HudSprites.DEFAULT_HP_SOLO, 0.0f, 0.0f, WIDTH, HP_HEIGHT,
					HudPlayerState.healthColor(view.health.fraction()), lowHealth);
			view.health.drawValue(guiGraphics, WIDTH - 3.0f, 3.5f, 0.75f, 1.0f, tickTime, true);

			if (full) {
				HudPlayerState state = HudPlayerState.of((Player) entity, data);
				view.energy.update(state.energy(), state.maxEnergy());
				view.stamina.update(state.stamina(), state.maxStamina());
				float chargeGlow = view.chargeGlow.update(state.chargingKi() ? 1.0f : 0.0f);
				view.energy.setFlowBoost(chargeGlow);

				view.energy.draw(guiGraphics, HudSprites.DEFAULT_KI, 0.0f, KI_Y, WIDTH, KI_HEIGHT, state.auraColor(), 0.0f);
				view.energy.drawSweep(guiGraphics, 0.0f, KI_Y, WIDTH, KI_HEIGHT, seconds * 1.1f, chargeGlow);
				view.energy.drawValue(guiGraphics, WIDTH - 3.0f, KI_Y + 2.0f, 0.75f, 1.0f, tickTime);

				view.stamina.draw(guiGraphics, HudSprites.DEFAULT_STAMINA, 0.0f, STM_Y, WIDTH, STM_HEIGHT, STAMINA_COLOR, 0.0f);
				view.stamina.drawValue(guiGraphics, WIDTH - 3.0f, STM_Y + 1.5f, 0.625f, 1.0f, tickTime);
			}

			drawBattlePower(guiGraphics, mc, view.id, entity instanceof Player);
			drawIndicators(guiGraphics, mc, view.id, entity.getMaxHealth());
		} finally {
			HudRender.setAlphaScale(1.0f);
			guiGraphics.pose().popPose();
		}
	}

	private static void drawBattlePower(GuiGraphics guiGraphics, Minecraft mc, int id, boolean player) {
		float bp = KiSenseScan.getCachedBP(id);
		boolean maxed = player ? bp >= Float.MAX_VALUE : bp >= Integer.MAX_VALUE;
		String text = maxed ? "BP: ???" : "BP: " + String.format("%,.0f", bp).replace(",", ".");
		float y = -BORDER - 1.5f - mc.font.lineHeight * BP_SCALE;
		HudRender.dmzText(guiGraphics, text, WIDTH / 2.0f, y, BP_SCALE, 0.5f, BP_COLOR, 1.0f);
	}

	private static void drawIndicators(GuiGraphics guiGraphics, Minecraft mc, int id, float maxHealth) {
		float time = mc.level.getGameTime() + mc.getFrameTime();
		float originX = WIDTH / 2.0f;
		float originY = -BORDER;

		if (ConfigManager.getUserConfig().getShowAccumulativeDamage()) {
			float damage = CombatIndicators.getAccumDamage(id);
			float heal = CombatIndicators.getAccumHeal(id);
			if (damage > 0.0f) {
				drawIndicator(guiGraphics, formatDelta(damage, false, maxHealth), originX + CombatIndicators.getDamageOffX(id) * INDICATOR_SPREAD,
						originY + CombatIndicators.getDamageOffY(id) * INDICATOR_SPREAD, DAMAGE_COLOR, accumAlpha(time - CombatIndicators.getDamageTick(id)));
			}
			if (heal > 0.0f) {
				drawIndicator(guiGraphics, formatDelta(heal, true, maxHealth), originX + CombatIndicators.getHealOffX(id) * INDICATOR_SPREAD,
						originY + CombatIndicators.getHealOffY(id) * INDICATOR_SPREAD, HEAL_COLOR, accumAlpha(time - CombatIndicators.getHealTick(id)));
			}
			return;
		}

		for (CombatIndicators.DamagePopup popup : CombatIndicators.getPopups(id)) {
			float life = Mth.clamp((time - popup.bornTick()) / CombatIndicators.POPUP_LIFETIME, 0.0f, 1.0f);
			drawIndicator(guiGraphics, formatDelta(popup.value(), popup.heal(), maxHealth), originX + popup.offX() * INDICATOR_SPREAD,
					originY + popup.offY() * INDICATOR_SPREAD - life * POPUP_RISE, popup.heal() ? HEAL_COLOR : DAMAGE_COLOR, 1.0f - life);
		}
	}

	private static void drawIndicator(GuiGraphics guiGraphics, String text, float centerX, float y, int rgb, float alpha) {
		if (alpha <= 0.01f) return;
		HudRender.dmzText(guiGraphics, text, centerX, y, INDICATOR_SCALE, 0.5f, rgb, alpha);
	}

	private static String formatDelta(float amount, boolean heal, float maxHealth) {
		String sign = heal ? "+" : "-";
		if (ConfigManager.getUserConfig().getAdvancedDescriptionPercentage() && maxHealth > 0.0f) {
			return sign + Math.round(amount / maxHealth * 100.0f) + "%";
		}
		return sign + Math.round(amount);
	}

	private static float accumAlpha(float ticksSinceChange) {
		if (ticksSinceChange <= 25.0f) return 1.0f;
		if (ticksSinceChange >= 40.0f) return 0.0f;
		return 1.0f - (ticksSinceChange - 25.0f) / 15.0f;
	}

	private static final class View {
		private final int id;
		private final HudBar health = new HudBar(HudStatNumberAnimator.StatKind.KISENSE_HEALTH);
		private final HudBar energy = new HudBar(HudStatNumberAnimator.StatKind.KI).flowing();
		private final HudBar stamina = new HudBar(HudStatNumberAnimator.StatKind.STAMINA);
		private final HudSmoother appear = new HudSmoother(0.15f, 0.01f);
		private final HudSmoother chargeGlow = new HudSmoother(0.15f);
		private boolean present;
		private float appearValue;
		private LivingEntity entity;
		private float screenX;
		private float screenY;
		private float scale;
		private double distance;

		private View(int id) {
			this.id = id;
			appear.snap(0.0f);
		}
	}
}
