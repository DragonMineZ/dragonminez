package com.dragonminez.common.network.S2C;

import com.dragonminez.client.render.shader.ClientGravityState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Tells the receiving client how much Gravity Device machine gravity is currently
 * affecting them. Drives the red distortion shader intensity. Sent once per second
 * only when the value changes.
 */
public class GravityZoneSyncS2C {

	private final float machineGravity;

	public GravityZoneSyncS2C(float machineGravity) {
		this.machineGravity = machineGravity;
	}

	public GravityZoneSyncS2C(FriendlyByteBuf buf) {
		this.machineGravity = buf.readFloat();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeFloat(machineGravity);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> ClientGravityState.setMachineGravity(machineGravity));
		ctx.get().setPacketHandled(true);
	}
}
