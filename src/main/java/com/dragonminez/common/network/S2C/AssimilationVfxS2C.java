package com.dragonminez.common.network.S2C;

import com.dragonminez.client.render.effects.NamekAssimilationEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class AssimilationVfxS2C {

	private final int targetId;
	private final int assimilatorId;

	public AssimilationVfxS2C(int targetId, int assimilatorId) {
		this.targetId = targetId;
		this.assimilatorId = assimilatorId;
	}

	public AssimilationVfxS2C(FriendlyByteBuf buf) {
		this.targetId = buf.readInt();
		this.assimilatorId = buf.readInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeInt(targetId);
		buf.writeInt(assimilatorId);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> NamekAssimilationEffect.consume(targetId, assimilatorId)));
		ctx.get().setPacketHandled(true);
	}
}
