package com.dragonminez.common.network.S2C;

import com.dragonminez.client.systems.KiBurstShakeState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KiBurstVfxS2C {

	private final int entityId;
	private final boolean full;
	private final float radius;

	public KiBurstVfxS2C(int entityId, boolean full, float radius) {
		this.entityId = entityId;
		this.full = full;
		this.radius = radius;
	}

	public KiBurstVfxS2C(FriendlyByteBuf buf) {
		this.entityId = buf.readInt();
		this.full = buf.readBoolean();
		this.radius = buf.readFloat();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeInt(entityId);
		buf.writeBoolean(full);
		buf.writeFloat(radius);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> KiBurstShakeState.start(entityId, full, radius)));
		ctx.get().setPacketHandled(true);
	}
}
