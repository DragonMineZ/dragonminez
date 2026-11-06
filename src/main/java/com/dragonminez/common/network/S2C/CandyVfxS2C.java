package com.dragonminez.common.network.S2C;

import com.dragonminez.client.render.effects.CandyEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class CandyVfxS2C {

	private final int casterId;
	private final int targetId;
	private final int candyTicks;
	private final int boltTicks;

	public CandyVfxS2C(int casterId, int targetId, int candyTicks, int boltTicks) {
		this.casterId = casterId;
		this.targetId = targetId;
		this.candyTicks = candyTicks;
		this.boltTicks = boltTicks;
	}

	public CandyVfxS2C(FriendlyByteBuf buf) {
		this.casterId = buf.readInt();
		this.targetId = buf.readInt();
		this.candyTicks = buf.readInt();
		this.boltTicks = buf.readInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeInt(casterId);
		buf.writeInt(targetId);
		buf.writeInt(candyTicks);
		buf.writeInt(boltTicks);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> CandyEffect.apply(casterId, targetId, candyTicks, boltTicks)));
		ctx.get().setPacketHandled(true);
	}
}
