package com.dragonminez.common.network.C2S;

import com.dragonminez.common.combat.clash.BeamClashManager;
import com.dragonminez.common.network.PacketRateLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BeamClashInputC2S {

	public BeamClashInputC2S() {
	}

	public BeamClashInputC2S(FriendlyByteBuf buf) {
	}

	public void toBytes(FriendlyByteBuf buf) {
	}

	public static void handle(BeamClashInputC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			if (!PacketRateLimiter.allow(player.getUUID(), "beam_clash_press", player.level().getGameTime(), 1L)) return;
			BeamClashManager.handlePlayerPress(player);
		});
		ctx.get().setPacketHandled(true);
	}
}
