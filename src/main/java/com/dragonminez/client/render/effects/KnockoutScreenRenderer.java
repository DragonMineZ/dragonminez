package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.gui.hud.HudSmoother;
import com.dragonminez.client.render.shader.KnockoutShaderManager;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.systems.worldboss.ClientWorldBossPlayerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class KnockoutScreenRenderer {
	private static final HudSmoother INTENSITY = new HudSmoother(0.35f, 0.004f);
	private static final int FALLBACK_GRAY = 0x8A8A8A;
	private static final int FALLBACK_RED = 0x7A0000;
	private static final float FALLBACK_BORDER_SHARE = 0.22f;

	private KnockoutScreenRenderer() {}

	public static float intensity() {
		return INTENSITY.value();
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		boolean iris = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		RenderLevelStageEvent.Stage targetStage = iris ? RenderLevelStageEvent.Stage.AFTER_LEVEL : RenderLevelStageEvent.Stage.AFTER_WEATHER;
		if (event.getStage() != targetStage) return;

		float intensity = INTENSITY.update(ClientWorldBossPlayerState.isKnockedOut() ? 1.0f : 0.0f);
		if (intensity <= 0.004f) {
			KnockoutShaderManager.reset();
			return;
		}
		if (KnockoutShaderManager.isLoadFailed()) return;

		if (iris) {
			mc.getMainRenderTarget().bindWrite(false);
			KnockoutShaderManager.process(event.getPartialTick(), intensity, false);
		} else {
			KnockoutShaderManager.process(event.getPartialTick(), intensity, true);
		}
	}

	@SubscribeEvent
	public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
		if (event.getOverlay() != VanillaGuiOverlay.VIGNETTE.type()) return;
		if (!KnockoutShaderManager.isLoadFailed()) return;
		float intensity = INTENSITY.value();
		if (intensity <= 0.004f) return;

		GuiGraphics graphics = event.getGuiGraphics();
		int width = event.getWindow().getGuiScaledWidth();
		int height = event.getWindow().getGuiScaledHeight();
		HudRender.rect(graphics, 0.0f, 0.0f, width, height, HudRender.argb(0.35f * intensity, FALLBACK_GRAY));

		float band = Math.min(width, height) * FALLBACK_BORDER_SHARE;
		int edge = HudRender.argb(0.75f * intensity, FALLBACK_RED);
		int clear = HudRender.argb(0.0f, FALLBACK_RED);
		HudRender.rectHorizontal(graphics, 0.0f, 0.0f, band, height, edge, clear);
		HudRender.rectHorizontal(graphics, width - band, 0.0f, band, height, clear, edge);
		HudRender.rectVertical(graphics, 0.0f, 0.0f, width, band, edge, clear);
		HudRender.rectVertical(graphics, 0.0f, height - band, width, band, clear, edge);
	}
}
