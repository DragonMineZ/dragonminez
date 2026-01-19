package com.dragonminez.server.events.players;

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

import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class EffectsEvents {

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || mc.isPaused() || !player.hasEffect(MainEffects.STAGGER.get())) return;


        int amplifier = player.getEffect(MainEffects.STAGGER.get()).getAmplifier();
        float intensityDegrees = 2.0F + (amplifier * 1.5F);

        double shakeSpeed = 0.8D;
        double time = player.level().getGameTime() + event.getPartialTick();
        double baseWave = Math.sin(time * shakeSpeed);
        double crashingWave = baseWave * baseWave * baseWave;
        float yawOffset = (float) (crashingWave * intensityDegrees);

        event.setYaw(event.getYaw() + yawOffset);

        float pitchNoise = (float) (Math.cos(time * shakeSpeed * 1.3D) * (intensityDegrees * 0.1F));
        event.setPitch(event.getPitch() + pitchNoise);
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

				// Dibujar rectángulo negro con 60% de opacidad (alpha aprox 150)
				// Color ARGB: 0x99000000 (99 = alpha, 000000 = negro)
				graphics.fill(0, 0, width, height, 0x99000000);

				RenderSystem.disableBlend();
			}
		}
	}
}