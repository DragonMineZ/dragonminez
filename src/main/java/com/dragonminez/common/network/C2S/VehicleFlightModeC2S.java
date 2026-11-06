package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import com.dragonminez.common.network.PacketRateLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleFlightModeC2S {
	private final boolean flying;

	public VehicleFlightModeC2S(boolean flying) {
		this.flying = flying;
	}

	public VehicleFlightModeC2S(FriendlyByteBuf buffer) {
		this.flying = buffer.readBoolean();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBoolean(flying);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null || !player.isAlive()) return;
			if (!(player.getVehicle() instanceof VehicleEntity vehicle)) return;
			if (!PacketRateLimiter.allow(player.getUUID(), "vehicle_flight_mode", player.level().getGameTime(), 2L)) return;
			vehicle.receiveDriverFlightMode(player, flying);
		});
		context.setPacketHandled(true);
	}
}
