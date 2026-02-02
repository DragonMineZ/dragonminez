package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class EffectsEvents {

	private static Field particlesField = null;
	private static Field particleXField = null;
	private static Field particleYField = null;
	private static Field particleZField = null;

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || mc.isPaused() || !player.hasEffect(MainEffects.STAGGER.get())) return;

        int amplifier = player.getEffect(MainEffects.STAGGER.get()).getAmplifier();
        float baseIntensity = 1.5F + (amplifier * 0.8F);

        double time = player.level().getGameTime() + event.getPartialTick();

        float yawShake = (float) (Math.sin(time * 0.5D) * baseIntensity * 0.8F + Math.sin(time * 1.2D) * baseIntensity * 0.4F);
        float pitchShake = (float) (Math.cos(time * 0.6D) * baseIntensity * 2.0F + Math.cos(time * 0.9D) * baseIntensity * 1.0F);
        float rollShake = (float) (Math.sin(time * 0.7D) * baseIntensity * 0.6F);

        event.setYaw(event.getYaw() + yawShake);
        event.setPitch(event.getPitch() + pitchShake);
        event.setRoll(event.getRoll() + rollShake);
    }

	@SubscribeEvent
	public static void renderStaggerOverlay(RenderGuiOverlayEvent.Post event) {
		if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
			Minecraft mc = Minecraft.getInstance();
			Player player = mc.player;

			if (player != null && player.hasEffect(MainEffects.STAGGER.get())) {
				int amplifier = player.getEffect(MainEffects.STAGGER.get()).getAmplifier();
				float blurStrength = 0.3F + (amplifier * 0.15F);

				int width = event.getWindow().getGuiScaledWidth();
				int height = event.getWindow().getGuiScaledHeight();

				GuiGraphics graphics = event.getGuiGraphics();

				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();

				int alpha = (int) (blurStrength * 80);
				int color = (alpha << 24) | 0xFFFFFF;

				graphics.fill(0, 0, width, height, color);

				RenderSystem.disableBlend();
			}
		}
	}

	@SubscribeEvent
	public static void renderStunOverlay(RenderGuiOverlayEvent.Post event) {
		if (event.getOverlay() == VanillaGuiOverlay.VIGNETTE.type()) {
			Minecraft mc = Minecraft.getInstance();
			Player player = mc.player;

			if (player != null && player.hasEffect(MainEffects.STUN.get())) {
				AtomicBoolean isDraining = new AtomicBoolean(false);
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
					if ("bioandroid".equals(data.getCharacter().getRaceName()) && data.getStatus().getDrainingTargetId() != -1) {
						isDraining.set(true);
					}
				});
				if (isDraining.get()) return;

				int width = event.getWindow().getGuiScaledWidth();
				int height = event.getWindow().getGuiScaledHeight();

				GuiGraphics graphics = event.getGuiGraphics();

				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();

				graphics.fill(0, 0, width, height, 0x99000000);

				RenderSystem.disableBlend();
			}
		}
	}
}