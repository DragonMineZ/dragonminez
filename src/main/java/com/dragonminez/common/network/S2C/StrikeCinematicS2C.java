package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class StrikeCinematicS2C {
	private final boolean active;
	private final int attackerId;
	private final int victimId;

	public StrikeCinematicS2C(boolean active, int attackerId, int victimId) {
		this.active = active;
		this.attackerId = attackerId;
		this.victimId = victimId;
	}

	public static StrikeCinematicS2C start(int attackerId, int victimId) {
		return new StrikeCinematicS2C(true, attackerId, victimId);
	}

	public static StrikeCinematicS2C stop(int attackerId) {
		return new StrikeCinematicS2C(false, attackerId, -1);
	}

	public StrikeCinematicS2C(FriendlyByteBuf buf) {
		this.active = buf.readBoolean();
		this.attackerId = buf.readInt();
		this.victimId = buf.readInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeBoolean(active);
		buf.writeInt(attackerId);
		buf.writeInt(victimId);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleStrikeCinematic(active, attackerId, victimId)));
		context.setPacketHandled(true);
	}
}
