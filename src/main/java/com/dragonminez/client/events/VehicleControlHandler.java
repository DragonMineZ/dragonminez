package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import com.dragonminez.common.init.entities.vehicle.VehicleProperties;
import com.dragonminez.common.init.sounds.VehicleEngineSound;
import com.dragonminez.common.network.C2S.VehicleSeatC2S;
import com.dragonminez.common.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class VehicleControlHandler {

	private static final double ENGINE_SOUND_RANGE = 48.0D;
	private static final int ENGINE_SCAN_INTERVAL = 5;
	private static final int SEAT_KEYS = 4;
	private static final Map<Integer, VehicleEngineSound> ENGINE_SOUNDS = new HashMap<>();
	private static final boolean[] SEAT_KEY_DOWN = new boolean[SEAT_KEYS];

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
			Arrays.fill(SEAT_KEY_DOWN, false);
			return;
		}
		boolean inGame = mc.screen == null;
		boolean seatChord = tickSeatKeys(vehicle, inGame);
		if (vehicle.getControllingPassenger() != player) return;
		vehicle.setDriverInput(
				inGame && mc.options.keyJump.isDown(),
				inGame && !seatChord && KeyBinds.isSecondFunctionDown(),
				inGame && KeyBinds.DASH_KEY.isDown());
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		tickEngineSounds(mc, mc.player);
	}

	private static boolean tickSeatKeys(VehicleEntity vehicle, boolean inGame) {
		boolean anyDown = false;
		int seats = Math.min(SEAT_KEYS, vehicle.getProperties().seatCount());
		for (int i = 0; i < SEAT_KEYS; i++) {
			boolean down = inGame && i < KeyBinds.TECHNIQUE_SLOTS.length && KeyBinds.isChordDown(KeyBinds.TECHNIQUE_SLOTS[i]);
			if (down && !SEAT_KEY_DOWN[i] && i < seats) NetworkHandler.sendToServer(new VehicleSeatC2S(i));
			SEAT_KEY_DOWN[i] = down;
			anyDown |= down;
		}
		return anyDown;
	}

	private static void tickEngineSounds(Minecraft mc, LocalPlayer player) {
		if (mc.level == null || player == null) {
			ENGINE_SOUNDS.clear();
			return;
		}
		if (mc.isPaused() || mc.level.getGameTime() % ENGINE_SCAN_INTERVAL != 0) return;
		ENGINE_SOUNDS.values().removeIf(sound -> sound.isStopped() || !mc.getSoundManager().isActive(sound));
		for (VehicleEntity vehicle : mc.level.getEntitiesOfClass(VehicleEntity.class, player.getBoundingBox().inflate(ENGINE_SOUND_RANGE))) {
			VehicleProperties.Engine engine = vehicle.getProperties().engine();
			if (engine == null || vehicle.getControllingPassenger() == null || ENGINE_SOUNDS.containsKey(vehicle.getId())) continue;
			VehicleEngineSound sound = new VehicleEngineSound(vehicle, engine);
			mc.getSoundManager().play(sound);
			ENGINE_SOUNDS.put(vehicle.getId(), sound);
		}
	}
}
