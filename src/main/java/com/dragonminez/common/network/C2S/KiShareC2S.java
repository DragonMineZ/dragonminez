package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.server.events.players.KiShareService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KiShareC2S {
	private static final int NO_TARGET = -1;

	private final boolean start;
	private final int targetId;

	private KiShareC2S(boolean start, int targetId) {
		this.start = start;
		this.targetId = targetId;
	}

	public static KiShareC2S start(int targetId) {
		return new KiShareC2S(true, targetId);
	}

	public static KiShareC2S stop() {
		return new KiShareC2S(false, NO_TARGET);
	}

	public KiShareC2S(FriendlyByteBuf buffer) {
		this.start = buffer.readBoolean();
		this.targetId = buffer.readVarInt();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBoolean(start);
		buffer.writeVarInt(targetId);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			if (!start) {
				KiShareService.requestStop(player);
				return;
			}
			if (!PacketRateLimiter.allow(player.getUUID(), "kiShareStart", player.level().getGameTime(), 5L)) return;
			KiShareService.requestStart(player, targetId);
		});
		context.setPacketHandled(true);
	}
}
