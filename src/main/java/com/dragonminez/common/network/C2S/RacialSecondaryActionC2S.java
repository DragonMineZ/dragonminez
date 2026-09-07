package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.racial.RacialContext;
import com.dragonminez.common.racial.RacialRegistry;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RacialSecondaryActionC2S {

	public RacialSecondaryActionC2S() {
	}

	public RacialSecondaryActionC2S(FriendlyByteBuf ignored) {
	}

	public void encode(FriendlyByteBuf ignored) {
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			if (!PacketRateLimiter.allow(player.getUUID(), "racial_secondary_action", player.level().getGameTime(), 20L)) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data ->
					RacialRegistry.forPlayer(data).ifPresent(ability -> ability.onSecondaryActivate(new RacialContext(player, data))));
		});
		context.setPacketHandled(true);
	}
}
