package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ReviveTargetsS2C;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RequestReviveTargetsC2S {

	public RequestReviveTargetsC2S() {}

	public RequestReviveTargetsC2S(FriendlyByteBuf ignored) {}

	public void encode(FriendlyByteBuf ignored) {}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			List<ReviveTargetsS2C.Entry> entries = new ArrayList<>();
			for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
				if (!GrantWishC2S.isRevivable(other)) continue;
				entries.add(new ReviveTargetsS2C.Entry(other.getUUID(), other.getGameProfile().getName()));
			}
			entries.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
			NetworkHandler.sendToPlayer(new ReviveTargetsS2C(entries), player);
		});
		ctx.get().setPacketHandled(true);
	}
}
