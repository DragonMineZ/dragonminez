package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Injects a server-applied knockback velocity into the local player's active flight handler.
 * Without this the client flight loop overwrites {@code setDeltaMovement} every tick, so a flying
 * player would ignore all knockback. Only sent to a flying player who is being launched.
 */
public class KnockbackFlightS2C {

	private final double x;
	private final double y;
	private final double z;

	public KnockbackFlightS2C(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public KnockbackFlightS2C(FriendlyByteBuf buf) {
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleKnockbackFlightPacket(x, y, z)));
		ctx.get().setPacketHandled(true);
	}
}
