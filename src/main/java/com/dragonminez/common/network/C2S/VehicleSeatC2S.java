package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import com.dragonminez.common.network.PacketRateLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class VehicleSeatC2S {
	private final int seat;

	public VehicleSeatC2S(int seat) {
		this.seat = seat;
	}

	public VehicleSeatC2S(FriendlyByteBuf buffer) {
		this.seat = buffer.readVarInt();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeVarInt(seat);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null || !player.isAlive()) return;
			if (!(player.getVehicle() instanceof VehicleEntity vehicle)) return;
			if (!PacketRateLimiter.allow(player.getUUID(), "vehicle_seat", player.level().getGameTime(), 5L)) return;
			if (seat < 0 || seat >= vehicle.getProperties().seatCount() || vehicle.getSeatIndex(player) == seat) return;
			if (!vehicle.moveToSeat(player, seat)) {
				player.displayClientMessage(Component.translatable("message.dragonminez.vehicle.seat_taken", seat + 1), true);
			}
		});
		context.setPacketHandled(true);
	}
}
