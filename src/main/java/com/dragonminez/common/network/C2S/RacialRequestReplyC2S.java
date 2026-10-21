package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.racial.capture.CaptureRequest;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RacialRequestReplyC2S {
	private final boolean accepted;

	public RacialRequestReplyC2S(boolean accepted) {
		this.accepted = accepted;
	}

	public RacialRequestReplyC2S(FriendlyByteBuf buf) {
		this.accepted = buf.readBoolean();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeBoolean(accepted);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			if (!PacketRateLimiter.allow(player.getUUID(), "racial_request_reply", player.level().getGameTime(), 20L)) return;

			CaptureRequest.reply(player, accepted);
		});
		context.setPacketHandled(true);
	}
}
