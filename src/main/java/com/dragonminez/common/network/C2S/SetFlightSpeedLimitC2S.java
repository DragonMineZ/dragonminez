package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetFlightSpeedLimitC2S {

	private final int flightSpeed;

	public SetFlightSpeedLimitC2S(int flightSpeed) {
		this.flightSpeed = flightSpeed;
	}

	public SetFlightSpeedLimitC2S(FriendlyByteBuf buffer) {
		this.flightSpeed = buffer.readVarInt();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeVarInt(flightSpeed);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			if (player.hasEffect(MainEffects.STUN.get())) return;
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				int maxFlightSpeed = 100;

				int newFlightSpeed;
				if (flightSpeed <= 0) {
					newFlightSpeed = 0;
				} else {
					int snapped = (flightSpeed / 5) * 5;
					newFlightSpeed = Math.max(5, Math.min(maxFlightSpeed, snapped));
				}

				data.getResources().setFlightSpeedLimit(newFlightSpeed);
				if (newFlightSpeed > 0 && data.getResources().getFlightSpeedLimit() > newFlightSpeed) {
					data.getResources().setFlightSpeedLimit(newFlightSpeed);
				}

				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
	}
}
