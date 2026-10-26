package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.MainEffects;
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

				int newFlightSpeed = flightSpeed <= 0 ? maxFlightSpeed : Math.max(5, Math.min(maxFlightSpeed, (flightSpeed / 5) * 5));
				data.getResources().setFlightSpeedLimit(newFlightSpeed);

				StatsSyncS2C.sendRequested(player);
			});
		});
		context.setPacketHandled(true);
	}
}
