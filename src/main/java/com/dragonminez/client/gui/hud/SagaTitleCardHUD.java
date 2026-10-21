package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.client.util.LocalizationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Cinematic title card shown when a saga begins: small header line, then the saga name
 * in large gold text, fading in, holding, and fading out.
 */
public class SagaTitleCardHUD {

	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private static final long FADE_IN_MS = 600;
	private static final long HOLD_MS = 2700;
	private static final long FADE_OUT_MS = 1200;
	private static final long TOTAL_MS = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;

	private static volatile String sagaName = null;
	private static volatile long showStartMs = 0;

	public static void show(String name) {
		if (name == null || name.isBlank()) return;
		sagaName = name;
		showStartMs = System.currentTimeMillis();
		Minecraft mc = Minecraft.getInstance();
		mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
	}

	public static final IGuiOverlay HUD_SAGA_TITLE_CARD = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		String name = sagaName;
		if (name == null) return;

		long elapsed = System.currentTimeMillis() - showStartMs;
		if (elapsed >= TOTAL_MS) {
			sagaName = null;
			return;
		}

		float alpha;
		if (elapsed < FADE_IN_MS) {
			alpha = elapsed / (float) FADE_IN_MS;
		} else if (elapsed < FADE_IN_MS + HOLD_MS) {
			alpha = 1.0f;
		} else {
			alpha = 1.0f - (elapsed - FADE_IN_MS - HOLD_MS) / (float) FADE_OUT_MS;
		}
		alpha = Mth.clamp(alpha, 0.0f, 1.0f);
		if (alpha < 0.05f) return;
		int alphaByte = (int) (alpha * 255.0f) << 24;

		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		Component header = Component.translatable("gui.dragonminez.saga_titlecard.header")
				.withStyle(Style.EMPTY.withFont(DMZ_FONT));
		Component title = LocalizationUtil.localizedOrReadable(name).copy()
				.withStyle(Style.EMPTY.withFont(DMZ_FONT));

		int centerX = width / 2;
		int baseY = height / 4;

		float headerScale = 1.3f;
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(centerX, baseY, 0);
		guiGraphics.pose().scale(headerScale, headerScale, 1.0f);
		guiGraphics.drawString(font, header, -font.width(header) / 2, 0, 0x00E8F0FF | alphaByte, true);
		guiGraphics.pose().popPose();

		float titleScale = 3.0f;
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(centerX, baseY + 18, 0);
		guiGraphics.pose().scale(titleScale, titleScale, 1.0f);
		guiGraphics.drawString(font, title, -font.width(title) / 2, 0, 0x00FFD700 | alphaByte, true);
		guiGraphics.pose().popPose();
	};

	private SagaTitleCardHUD() {
	}
}
