package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class VehicleControlHandler {

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle) || vehicle.getControllingPassenger() != player) return;
		boolean inGame = mc.screen == null;
		vehicle.setDriverInput(
				inGame && mc.options.keyJump.isDown(),
				inGame && KeyBinds.isSecondFunctionDown(),
				inGame && KeyBinds.DASH_KEY.isDown());
	}
}
