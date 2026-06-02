package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.clash.BeamClashCinematicCamera;
import com.dragonminez.client.clash.ClientBeamClashState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BeamClashClientEvents {

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;

		if (ClientBeamClashState.isActive() && !BeamClashCinematicCamera.isActive()) {
			BeamClashCinematicCamera.activate();
		} else if (!ClientBeamClashState.isActive() && BeamClashCinematicCamera.isActive()) {
			BeamClashCinematicCamera.deactivate();
		}

		BeamClashCinematicCamera.tickFov();
	}

	@SubscribeEvent
	public static void onComputeFov(ViewportEvent.ComputeFov event) {
		event.setFOV(BeamClashCinematicCamera.applyFov(event.getFOV()));
	}
}
