package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class StrikeRushS2C {
	private final int playerId;
	private final int ticks;
	private final int targetId;
	private final boolean connected;

	public StrikeRushS2C(int playerId, int ticks, int targetId, boolean connected) {
		this.playerId = playerId;
		this.ticks = ticks;
		this.targetId = targetId;
		this.connected = connected;
	}

	public StrikeRushS2C(FriendlyByteBuf buf) {
		this.playerId = buf.readInt();
		this.ticks = buf.readVarInt();
		this.targetId = buf.readInt();
		this.connected = buf.readBoolean();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeInt(playerId);
		buf.writeVarInt(ticks);
		buf.writeInt(targetId);
		buf.writeBoolean(connected);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleStrikeRush(playerId, ticks, targetId, connected)));
		context.setPacketHandled(true);
	}
}
