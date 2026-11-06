package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.gui.hud.HudSmoother;
import com.dragonminez.client.render.shader.KnockoutShaderManager;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.systems.worldboss.ClientWorldBossState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
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

		float intensity = INTENSITY.update(ClientWorldBossState.Player.isKnockedOut() ? 1.0f : 0.0f);
		if (intensity <= 0.004f) {
			KnockoutShaderManager.reset();
			return;
		}
		if (KnockoutShaderManager.isLoadFailed()) return;

		if (iris) {
			mc.getMainRenderTarget().bindWrite(false);
			KnockoutShaderManager.process(event.getPartialTick().getGameTimeDeltaPartialTick(false), intensity, false);
		} else {
			KnockoutShaderManager.process(event.getPartialTick().getGameTimeDeltaPartialTick(false), intensity, true);
		}
	}

	@SubscribeEvent
	public static void onRenderOverlay(RenderGuiLayerEvent.Post event) {
		if (!VanillaGuiLayers.CAMERA_OVERLAYS.equals(event.getName())) return;
		if (!KnockoutShaderManager.isLoadFailed()) return;
		float intensity = INTENSITY.value();
		if (intensity <= 0.004f) return;

		GuiGraphics graphics = event.getGuiGraphics();
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
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
