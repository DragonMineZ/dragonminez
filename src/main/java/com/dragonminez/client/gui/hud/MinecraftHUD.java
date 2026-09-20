package com.dragonminez.client.gui.hud;

import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class MinecraftHUD {
	private static final float BORDER = HudSprites.MINECRAFT_HP.border();
	private static final float BAR_HEIGHT = HudSprites.MINECRAFT_HP.height();
	private static final float ROW_STEP = BAR_HEIGHT + BORDER;
	private static final float CAPSULE = HudSprites.MINECRAFT_CAPSULE.guiWidth();
	private static final float MAX_BAR_WIDTH = HudSprites.MINECRAFT_HP.width();
	private static final float MIN_BAR_WIDTH = 36.0f;
	private static final float OFFHAND_SLOT = 29.0f;
	private static final float ATTACK_INDICATOR = 25.0f;
	private static final int STAMINA_COLOR = 0xF5A623;
	private static final int FORM_CHARGE_COLOR = 0xFFD84A;
	private static final int RELEASE_TEXT_COLOR = 0xFACAF7;

	private static final HudBar HP_BAR = new HudBar(HudStatNumberAnimator.StatKind.HEALTH);
	private static final HudBar KI_BAR = new HudBar(HudStatNumberAnimator.StatKind.KI);
	private static final HudBar STM_BAR = new HudBar(HudStatNumberAnimator.StatKind.STAMINA);
	private static final HudBar RELEASE_BAR = new HudBar(HudStatNumberAnimator.StatKind.KI);
	private static final HudSmoother RELEASE = new HudSmoother(0.10f, 0.05f);
	private static final HudSmoother FORM_CHARGE = new HudSmoother(0.08f);
	private static final HudSmoother CHARGE_GLOW = new HudSmoother(0.15f);
	private static final HudSmoother LEFT_SHIFT = new HudSmoother(0.10f, 0.05f);
	private static final HudSmoother RIGHT_SHIFT = new HudSmoother(0.10f, 0.05f);

	private record Frame(HudPlayerState state, float release, float formCharge, float chargeGlow, float surge, float lowHealth, float seconds, float tickTime) {}

	public static final IGuiOverlay HUD_MINECRAFT = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	public static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;
		if (HudStyle.current() != HudStyle.MINECRAFT) return;
		if (mc.player.isSpectator()) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;

			HudPlayerState state = HudPlayerState.of(mc.player, data);
			HP_BAR.update(state.health(), state.maxHealth());
			KI_BAR.update(state.energy(), state.maxEnergy());
			STM_BAR.update(state.stamina(), state.maxStamina());
			RELEASE_BAR.update(Math.min(state.powerRelease(), 100), 100.0f);

			float seconds = (System.nanoTime() / 1_000_000L % 3_600_000L) / 1000.0f;
			Frame frame = new Frame(state, RELEASE.update(state.powerRelease()), FORM_CHARGE.update(state.formCharge()),
					CHARGE_GLOW.update(state.chargingKi() ? 1.0f : 0.0f), SurgeBarState.fraction(data),
					HP_BAR.targetFraction() < 0.25f ? 0.5f + 0.5f * Mth.sin(seconds * 7.0f) : 0.0f,
					seconds, mc.player.tickCount + partialTicks);

			boolean offhandShown = !mc.player.getOffhandItem().isEmpty();
			boolean rightHanded = mc.player.getMainArm() == HumanoidArm.RIGHT;
			boolean indicatorShown = mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR;
			float leftShift = LEFT_SHIFT.update((offhandShown && rightHanded ? OFFHAND_SLOT : 0.0f) + (indicatorShown && !rightHanded ? ATTACK_INDICATOR : 0.0f));
			float rightShift = RIGHT_SHIFT.update((offhandShown && !rightHanded ? OFFHAND_SLOT : 0.0f) + (indicatorShown && rightHanded ? ATTACK_INDICATOR : 0.0f));

			float centerX = width / 2.0f;
			float available = centerX - HudLayout.HOTBAR_HALF_WIDTH - CAPSULE - BORDER - 2.0f - Math.max(OFFHAND_SLOT, ATTACK_INDICATOR);
			float hotbarWidth = Mth.clamp(available, MIN_BAR_WIDTH, MAX_BAR_WIDTH);
			float hotbarTop = height - HudLayout.HOTBAR_HEIGHT + BORDER;

			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			drawSide(guiGraphics, HudElement.MC_LEFT, width, height, frame,
					centerX - HudLayout.HOTBAR_HALF_WIDTH - BORDER - leftShift - hotbarWidth, hotbarTop, hotbarWidth);
			drawSide(guiGraphics, HudElement.MC_RIGHT, width, height, frame,
					centerX + HudLayout.HOTBAR_HALF_WIDTH + BORDER + rightShift, hotbarTop, hotbarWidth);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		});
	}

	private static void drawSide(GuiGraphics guiGraphics, HudElement element, int width, int height, Frame frame, float hotbarX, float hotbarTop, float hotbarWidth) {
		HudLayout.Box box = HudLayout.resolve(element, width, height);
		if (!box.visible() && !HudLayout.isPreview()) return;
		boolean left = element == HudElement.MC_LEFT;

		HudRender.setAlphaScale(box.visible() ? 1.0f : 0.35f);
		guiGraphics.pose().pushPose();
		try {
			float barX = hotbarX;
			float topY = hotbarTop;
			float barWidth = hotbarWidth;
			if (!box.hotbar()) {
				guiGraphics.pose().translate(box.x(), box.y(), 0.0f);
				guiGraphics.pose().scale(box.scale(), box.scale(), 1.0f);
				barX = left ? CAPSULE : BORDER;
				topY = BORDER;
				barWidth = MAX_BAR_WIDTH;
			}
			if (left) drawLeft(guiGraphics, frame, barX, topY, barWidth);
			else drawRight(guiGraphics, frame, barX, topY, barWidth);
		} finally {
			guiGraphics.pose().popPose();
			HudRender.setAlphaScale(1.0f);
		}
	}

	private static void drawLeft(GuiGraphics guiGraphics, Frame frame, float barX, float topY, float barWidth) {
		float bottomY = topY + ROW_STEP;
		float textY = (BAR_HEIGHT - 6.0f) / 2.0f;
		float capsuleX = barX - CAPSULE;

		HP_BAR.draw(guiGraphics, HudSprites.MINECRAFT_HP, barX, topY, barWidth, BAR_HEIGHT, HudPlayerState.healthColor(HP_BAR.fraction()), frame.lowHealth());
		drawCapsule(guiGraphics, HudSprites.MINECRAFT_ICON_HEART, capsuleX, topY - BORDER, HudRender.mix(0xFF4B4B, 0xFFFFFF, frame.lowHealth() * 0.6f));
		HP_BAR.drawValue(guiGraphics, barX + barWidth / 2.0f, topY + textY, 0.75f, 0.5f, frame.tickTime());

		STM_BAR.draw(guiGraphics, HudSprites.MINECRAFT_STAMINA, barX, bottomY, barWidth, BAR_HEIGHT, STAMINA_COLOR, 0.0f);
		drawCapsule(guiGraphics, HudSprites.MINECRAFT_ICON_STAMINA, capsuleX, bottomY - BORDER, 0xFFD34D);
		STM_BAR.drawValue(guiGraphics, barX + barWidth / 2.0f, bottomY + textY, 0.75f, 0.5f, frame.tickTime());
	}

	private static void drawRight(GuiGraphics guiGraphics, Frame frame, float barX, float topY, float barWidth) {
		float bottomY = topY + ROW_STEP;
		float textY = (BAR_HEIGHT - 6.0f) / 2.0f;
		float capsuleX = barX + barWidth;
		int aura = frame.state().auraColor();

		KI_BAR.draw(guiGraphics, HudSprites.MINECRAFT_KI, barX, topY, barWidth, BAR_HEIGHT, aura, 0.0f);
		KI_BAR.drawSweep(guiGraphics, barX, topY, barWidth, BAR_HEIGHT, frame.seconds() * 1.1f, frame.chargeGlow());
		if (frame.surge() > 0.0f) {
			HudRender.rect(guiGraphics, barX, topY + BAR_HEIGHT - 2.0f, frame.surge() * barWidth, 2.0f, HudRender.argb(0.95f, HudRender.mix(aura, 0xFFFFFF, 0.75f)));
		}
		drawCapsule(guiGraphics, HudSprites.MINECRAFT_ICON_KI, capsuleX, topY - BORDER, HudRender.mix(aura, 0xFFFFFF, 0.25f));
		KI_BAR.drawValue(guiGraphics, barX + barWidth / 2.0f, topY + textY, 0.75f, 0.5f, frame.tickTime());

		RELEASE_BAR.draw(guiGraphics, HudSprites.MINECRAFT_RELEASE, barX, bottomY, barWidth, BAR_HEIGHT, HudRender.mix(aura, 0xFFFFFF, 0.35f), 0.0f);
		if (frame.formCharge() > 0.005f) {
			HudRender.rect(guiGraphics, barX, bottomY + BAR_HEIGHT - 2.0f, frame.formCharge() * barWidth, 2.0f, HudRender.argb(1.0f, FORM_CHARGE_COLOR));
		}
		drawCapsule(guiGraphics, HudSprites.MINECRAFT_ICON_RELEASE, capsuleX, bottomY - BORDER, RELEASE_TEXT_COLOR);
		HudRender.text(guiGraphics, Math.round(frame.release()) + "%", barX + barWidth / 2.0f, bottomY + textY, 0.75f, 0.5f, RELEASE_TEXT_COLOR, 1.0f);
	}

	private static void drawCapsule(GuiGraphics guiGraphics, HudSprites.Sprite icon, float x, float y, int iconRgb) {
		HudRender.sprite(guiGraphics, HudSprites.MINECRAFT_CAPSULE, x, y, CAPSULE, CAPSULE, 0xFFFFFF, 1.0f);
		HudRender.sprite(guiGraphics, icon, x + BORDER, y + BORDER, icon.guiWidth(), icon.guiHeight(), iconRgb, 1.0f);
	}
}
