package com.dragonminez.common.network.S2C;

import com.dragonminez.client.render.effects.DimensionalFistEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DimensionalFistS2C {

	private final int entityId;
	private final double x;
	private final double y;
	private final double z;
	private final float yaw;
	private final float pitch;
	private final boolean leftArm;
	private final int openTicks;

	public DimensionalFistS2C(int entityId, double x, double y, double z, float yaw, float pitch, boolean leftArm, int openTicks) {
		this.entityId = entityId;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.leftArm = leftArm;
		this.openTicks = openTicks;
	}

	public DimensionalFistS2C(FriendlyByteBuf buf) {
		this.entityId = buf.readVarInt();
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
		this.yaw = buf.readFloat();
		this.pitch = buf.readFloat();
		this.leftArm = buf.readBoolean();
		this.openTicks = buf.readVarInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(entityId);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(yaw);
		buf.writeFloat(pitch);
		buf.writeBoolean(leftArm);
		buf.writeVarInt(openTicks);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> DimensionalFistEffect.spawn(entityId, x, y, z, yaw, pitch, leftArm, openTicks)));
		ctx.get().setPacketHandled(true);
	}
}
