package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import com.dragonminez.compat.DistExecutor;
import net.minecraft.network.FriendlyByteBuf;
import com.dragonminez.compat.network.NetworkEvent;
import net.neoforged.api.distmarker.Dist;

import java.util.function.Supplier;

public class TaiyokenBlindS2C {

	private final int durationTicks;

	public TaiyokenBlindS2C(int durationTicks) {
		this.durationTicks = durationTicks;
	}

	public TaiyokenBlindS2C(FriendlyByteBuf buf) {
		this.durationTicks = buf.readInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeInt(durationTicks);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleTaiyokenBlind(durationTicks)));
		ctx.get().setPacketHandled(true);
	}
}
