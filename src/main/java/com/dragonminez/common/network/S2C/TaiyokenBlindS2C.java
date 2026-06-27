package com.dragonminez.common.network.S2C;

import com.dragonminez.client.systems.taiyoken.TaiyokenBlindState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

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
		ctx.get().enqueueWork(() -> TaiyokenBlindState.startBlind(durationTicks));
		ctx.get().setPacketHandled(true);
	}
}
