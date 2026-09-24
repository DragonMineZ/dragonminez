package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.clash.ClientBeamClashState;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.gui.hud.HudSmoother;
import com.dragonminez.client.render.shader.FullscreenPostEffect;
import com.dragonminez.client.render.util.IrisCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class BeamClashScreenRenderer {
	private static final FullscreenPostEffect EFFECT = new FullscreenPostEffect(
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "shaders/post/beam_clash.json"), "beam clash speed lines");
	private static final HudSmoother INTENSITY = new HudSmoother(0.22f, 0.004f);
	private static final float FALLBACK_BORDER_SHARE = 0.15f;
	private static final int FALLBACK_INK = 0x050505;

	private BeamClashScreenRenderer() {}

	public static void onResourceReload() {
		EFFECT.onResourceReload();
	}

	public static void reset() {
		EFFECT.reset();
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		float intensity = INTENSITY.update(ClientBeamClashState.isActive() ? 1.0f : 0.0f);
		if (intensity <= 0.004f) {
			EFFECT.reset();
			return;
		}
		if (EFFECT.isLoadFailed()) return;

		boolean iris = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		float seconds = ((mc.level.getGameTime() % 24000L) + event.getPartialTick()) / 20.0f;

		if (iris) mc.getMainRenderTarget().bindWrite(false);
		EFFECT.process(event.getPartialTick(), !iris, effect -> {
			effect.safeGetUniform("Intensity").set(intensity);
			effect.safeGetUniform("Seconds").set(seconds);
		});
	}

	@SubscribeEvent
	public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
		if (event.getOverlay() != VanillaGuiOverlay.VIGNETTE.type()) return;
		if (!EFFECT.isLoadFailed()) return;
		float intensity = INTENSITY.value();
		if (intensity <= 0.004f) return;

		GuiGraphics graphics = event.getGuiGraphics();
		int width = event.getWindow().getGuiScaledWidth();
		int height = event.getWindow().getGuiScaledHeight();
		float band = Math.min(width, height) * FALLBACK_BORDER_SHARE;
		int edge = HudRender.argb(0.6f * intensity, FALLBACK_INK);
		int clear = HudRender.argb(0.0f, FALLBACK_INK);
		HudRender.rectHorizontal(graphics, 0.0f, 0.0f, band, height, edge, clear);
		HudRender.rectHorizontal(graphics, width - band, 0.0f, band, height, clear, edge);
		HudRender.rectVertical(graphics, 0.0f, 0.0f, width, band, edge, clear);
		HudRender.rectVertical(graphics, 0.0f, height - band, width, band, clear, edge);
	}
}
