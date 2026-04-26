package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.stats.extras.ActionMode;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class EffectsEvents {
	private static boolean isBioAndroidDrainingCache = false;
	private static boolean isChargingFormCache = false;

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null) return;

		if (player.hasEffect(MainEffects.STUN.get())) {
			isBioAndroidDrainingCache = StatsProvider.get(StatsCapability.INSTANCE, player)
					.map(data -> "bioandroid".equals(data.getCharacter().getRaceName()) && data.getStatus().getDrainingTargetId() != -1)
					.orElse(false);
		} else isBioAndroidDrainingCache = false;

		isChargingFormCache = StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(data -> (data.getStatus().isAuraActive() && !data.getStatus().isPermanentAura())).orElse(false);
	}

	@SubscribeEvent
	public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;

		if (player == null || mc.isPaused()) return;

		double time = player.level().getGameTime() + event.getPartialTick();

        double shakeRadius = 15.0D;
        var nearbyEntities = player.level().getEntitiesOfClass(DBSagasEntity.class, player.getBoundingBox().inflate(shakeRadius));

        for (DBSagasEntity entity : nearbyEntities) {
            if (entity.isAlive() && entity.isCasting() && entity.getSkillType() == 7) {

                double distance = player.distanceTo(entity);
                if (distance <= shakeRadius) {

                    float intensity = (float) (1.0D - (distance / shakeRadius));

                    float shakePitch = (player.getRandom().nextFloat() - 0.5F) * 2.5F * intensity;
                    float shakeYaw = (player.getRandom().nextFloat() - 0.5F) * 2.5F * intensity;
                    float shakeRoll = (player.getRandom().nextFloat() - 0.5F) * 1.5F * intensity;

                    event.setPitch(event.getPitch() + shakePitch);
                    event.setYaw(event.getYaw() + shakeYaw);
                    event.setRoll(event.getRoll() + shakeRoll);
                    break;
                }
            }
        }

		if (player.hasEffect(MainEffects.STAGGER.get())) {
			int amplifier = player.getEffect(MainEffects.STAGGER.get()).getAmplifier();
			float baseIntensity = 1.5F + (amplifier * 0.8F);

			float yawShake = (float) (Math.sin(time * 0.5D) * baseIntensity * 0.8F + Math.sin(time * 1.2D) * baseIntensity * 0.4F);
			float pitchShake = (float) (Math.cos(time * 0.6D) * baseIntensity * 2.0F + Math.cos(time * 0.9D) * baseIntensity * 1.0F);
			float rollShake = (float) (Math.sin(time * 0.7D) * baseIntensity * 0.6F);

			event.setYaw(event.getYaw() + yawShake);
			event.setPitch(event.getPitch() + pitchShake);
			event.setRoll(event.getRoll() + rollShake);
		}

		if (isChargingFormCache) {
			float globalshake = 0.05F;

			float sweepIntensity = 2.5F * globalshake;
			float sweepYaw = (float) (Math.sin(time * 1.5D) * sweepIntensity);
			float sweepPitch = (float) (Math.cos(time * 1.2D) * (sweepIntensity * 0.6F));

			float shakeIntensity = 0.2F * globalshake;
			float shakeYaw = (float) (Math.sin(time * 5.0D) * shakeIntensity);
			float shakePitch = (float) (Math.cos(time * 6.0D) * shakeIntensity);

			float rollIntensity = 1.5F * globalshake;
			float tfRoll = (float) (Math.sin(time * 2.0D) * rollIntensity);

			event.setYaw(event.getYaw() + sweepYaw + shakeYaw);
			event.setPitch(event.getPitch() + sweepPitch + shakePitch);
			event.setRoll(event.getRoll() + tfRoll);
		}
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
				if (isBioAndroidDrainingCache) return;

				int amplifier = player.getEffect(MainEffects.STUN.get()).getAmplifier();
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
}