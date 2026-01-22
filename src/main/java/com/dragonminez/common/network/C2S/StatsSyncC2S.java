package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StatsSyncC2S {

	public StatsSyncC2S() {
	}

	public static void encode(StatsSyncC2S msg, FriendlyByteBuf buf) {}

	public static StatsSyncC2S decode(FriendlyByteBuf buf) {
		return new StatsSyncC2S();
	}

	public static void handle(StatsSyncC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
		});
		ctx.get().setPacketHandled(true);
	}
}
