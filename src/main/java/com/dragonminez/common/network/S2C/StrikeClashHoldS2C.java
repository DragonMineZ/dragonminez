package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class StrikeClashHoldS2C {
	private final boolean active;
	private final double x;
	private final double y;
	private final double z;
	private final float yaw;

	public StrikeClashHoldS2C(boolean active, double x, double y, double z, float yaw) {
		this.active = active;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
	}

	public static StrikeClashHoldS2C release() {
		return new StrikeClashHoldS2C(false, 0.0, 0.0, 0.0, 0.0F);
	}

	public StrikeClashHoldS2C(FriendlyByteBuf buf) {
		this.active = buf.readBoolean();
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
		this.yaw = buf.readFloat();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeBoolean(active);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(yaw);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleStrikeClashHold(active, x, y, z, yaw)));
		context.setPacketHandled(true);
	}
}
