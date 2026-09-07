package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.racial.impl.NamekAssimilation;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NamekRegenC2S {

	public NamekRegenC2S() {
	}

	public NamekRegenC2S(FriendlyByteBuf ignored) {
	}

	public void encode(FriendlyByteBuf ignored) {
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			if (!PacketRateLimiter.allow(player.getUUID(), "namek_regen", player.level().getGameTime(), 20L)) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!"namekian".equals(data.getCharacter().getRaceName())) return;
				NamekAssimilation.startRegen(player, data);
			});
		});
		context.setPacketHandled(true);
	}
}
