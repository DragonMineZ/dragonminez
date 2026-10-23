package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.entities.ki.GumBindEntity;
import com.dragonminez.common.network.PacketRateLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class GumStruggleC2S {

	public GumStruggleC2S() {
	}

	public GumStruggleC2S(FriendlyByteBuf buffer) {
	}

	public void encode(FriendlyByteBuf buffer) {
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null || !player.isAlive()) return;
			if (!PacketRateLimiter.allow(player.getUUID(), "gum_struggle", player.level().getGameTime(), 2L)) return;
			GumBindEntity.struggle(player);
		});
		context.setPacketHandled(true);
	}
}
