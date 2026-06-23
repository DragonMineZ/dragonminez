package com.dragonminez.common.network.C2S;

import com.dragonminez.server.events.players.combat.TaiyokenHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TaiyokenCastC2S {

	public TaiyokenCastC2S() {
	}

	public TaiyokenCastC2S(FriendlyByteBuf buf) {
	}

	public void toBytes(FriendlyByteBuf buf) {
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player != null) TaiyokenHandler.cast(player);
		});
		ctx.get().setPacketHandled(true);
	}
}
