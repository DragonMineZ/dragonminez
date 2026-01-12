package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class CameraShakeHandler {

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || mc.isPaused() || !player.hasEffect(MainEffects.STAGGER.get())) {
            return;
        }

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
}