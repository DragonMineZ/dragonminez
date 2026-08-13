package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.clash.BeamClashCinematicCamera;
import com.dragonminez.client.clash.ClientBeamClashState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class BeamClashClientEvents {

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {

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
