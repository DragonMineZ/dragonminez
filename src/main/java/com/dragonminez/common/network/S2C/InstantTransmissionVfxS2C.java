package com.dragonminez.common.network.S2C;

import com.dragonminez.client.render.effects.InstantTransmissionEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class InstantTransmissionVfxS2C {

	private final int entityId;
	private final boolean arrival;

	public InstantTransmissionVfxS2C(int entityId, boolean arrival) {
		this.entityId = entityId;
		this.arrival = arrival;
	}

	public InstantTransmissionVfxS2C(FriendlyByteBuf buf) {
		this.entityId = buf.readInt();
		this.arrival = buf.readBoolean();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeInt(entityId);
		buf.writeBoolean(arrival);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> InstantTransmissionEffect.handle(entityId, arrival)));
		ctx.get().setPacketHandled(true);
	}
}
